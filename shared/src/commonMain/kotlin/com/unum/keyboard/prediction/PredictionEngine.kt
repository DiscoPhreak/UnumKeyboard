package com.unum.keyboard.prediction

/**
 * Orchestrates trie prefix search + n-gram scoring + autocorrect
 * to produce ranked word predictions.
 *
 * Pipeline:
 * 1. Trie prefix search → raw candidates
 * 2. N-gram score each candidate using context (previous 1-2 words)
 * 3. If no exact prefix match, try autocorrect (edit-distance-1 variants)
 * 4. Return top results ranked by combined score
 */
class PredictionEngine(
    private val dictionary: TrieDictionary,
    private val ngramModel: NGramModel,
    private val autoCorrect: AutoCorrect
) {
    private var contextWords: MutableList<String> = mutableListOf()
    private var currentPrefix: String = ""
    private var maxUnigramFreq: Int = 1

    /** User learning integration (set by PredictionService when learning is enabled) */
    var learningScorer: LearningScorer? = null

    fun setMaxUnigramFreq(freq: Int) {
        maxUnigramFreq = maxOf(freq, 1)
    }

    /**
     * Called when a word is committed (space pressed, prediction accepted).
     */
    fun commitWord(word: String) {
        contextWords.add(word.lowercase())
        if (contextWords.size > MAX_CONTEXT_WORDS) {
            contextWords.removeAt(0)
        }
        currentPrefix = ""
    }

    /**
     * Called on sentence boundary (period, !, ?, newline).
     */
    fun resetContext() {
        contextWords.clear()
        currentPrefix = ""
    }

    /**
     * Update the current prefix as user types characters.
     */
    fun updatePrefix(prefix: String) {
        currentPrefix = prefix
    }

    /**
     * Main prediction method. Returns ranked predictions.
     */
    fun predict(maxResults: Int = 5): List<Prediction> {
        val results = mutableListOf<Prediction>()

        val prevWordId = getContextWordId(0)
        val prev2WordId = getContextWordId(1)

        if (currentPrefix.isEmpty()) {
            // Next-word prediction from context
            results.addAll(predictNextWord(prevWordId, prev2WordId, maxResults))
        } else {
            // Prefix completion + scoring
            results.addAll(predictCompletion(currentPrefix, prevWordId, prev2WordId, maxResults))
        }

        return results.take(maxResults)
    }

    private fun predictNextWord(
        prevWordId: Int,
        prev2WordId: Int,
        maxResults: Int
    ): List<Prediction> {
        val results = mutableListOf<Prediction>()

        // System bigram continuations
        if (prevWordId >= 0) {
            val bigramContinuations = ngramModel.getBigramContinuations(prevWordId, maxResults * 2)
            for ((wordId, _) in bigramContinuations) {
                val word = dictionary.getWordById(wordId) ?: continue
                val freq = dictionary.getFrequency(word)
                val candidate = WordCandidate(word, freq, wordId)
                val userScore = learningScorer?.getUserFrequencyScore(word) ?: 0f
                val score = ngramModel.scoreCandidate(candidate, prevWordId, prev2WordId, maxUnigramFreq, userScore)
                results.add(Prediction(word, score, PredictionSource.NGRAM))
            }
        }

        // User bigram continuations (may suggest words not in system bigrams)
        val prevWord = if (contextWords.isNotEmpty()) contextWords.last() else null
        if (prevWord != null) {
            val userContinuations = learningScorer?.getUserContinuations(prevWord, maxResults) ?: emptyList()
            for ((word, _) in userContinuations) {
                if (results.any { it.word == word }) continue
                val freq = dictionary.getFrequency(word)
                if (freq <= 0) continue // only suggest dictionary words
                val wordId = dictionary.getWordId(word)
                val candidate = WordCandidate(word, freq, wordId)
                val userScore = learningScorer?.getUserFrequencyScore(word) ?: 0f
                val score = ngramModel.scoreCandidate(candidate, prevWordId, prev2WordId, maxUnigramFreq, userScore)
                results.add(Prediction(word, score + USER_BIGRAM_BOOST, PredictionSource.USER_DICTIONARY))
            }
        }

        results.sortByDescending { it.score }
        return results.take(maxResults)
    }

    private fun predictCompletion(
        prefix: String,
        prevWordId: Int,
        prev2WordId: Int,
        maxResults: Int
    ): List<Prediction> {
        val results = mutableListOf<Prediction>()

        // 1. Trie prefix search
        val trieCandidates = dictionary.prefixSearch(prefix, maxResults * 2)
        for (candidate in trieCandidates) {
            val userScore = learningScorer?.getUserFrequencyScore(candidate.word) ?: 0f
            val score = ngramModel.scoreCandidate(candidate, prevWordId, prev2WordId, maxUnigramFreq, userScore)
            val boostedScore = score + (candidate.frequency.toFloat() / maxUnigramFreq) * 0.1f
            results.add(Prediction(candidate.word, boostedScore, PredictionSource.TRIE))
        }

        // 2. Always run autocorrect for suggestion candidates
        if (prefix.length >= 2) {
            val corrections = autoCorrect.corrections(prefix, maxResults)
            for (correction in corrections) {
                if (results.any { it.word == correction.word }) continue
                val wordId = dictionary.getWordId(correction.word)
                val candidate = WordCandidate(correction.word, correction.frequency, wordId)
                val userScore = learningScorer?.getUserFrequencyScore(correction.word) ?: 0f
                val score = ngramModel.scoreCandidate(candidate, prevWordId, prev2WordId, maxUnigramFreq, userScore)
                val correctionPenalty = correction.weightedCost * 0.1f
                results.add(Prediction(
                    correction.word,
                    score - correctionPenalty,
                    PredictionSource.AUTOCORRECT
                ))
            }
        }

        // 3. Exact match boost — if the prefix itself is a word, boost it
        if (dictionary.contains(prefix)) {
            val existing = results.find { it.word == prefix.lowercase() }
            if (existing != null) {
                val idx = results.indexOf(existing)
                results[idx] = existing.copy(score = existing.score + EXACT_MATCH_BOOST)
            }
        }

        results.sortByDescending { it.score }
        return results.take(maxResults)
    }

    private fun getContextWordId(wordsBack: Int): Int {
        val index = contextWords.size - 1 - wordsBack
        if (index < 0) return -1
        return dictionary.getWordId(contextWords[index])
    }

    /**
     * Evaluate auto-correction for a completed word (called at commit time).
     */
    fun evaluateAutoCorrection(
        word: String,
        userDictionaryContains: (String) -> Boolean
    ): AutoCorrectionResult {
        return autoCorrect.evaluateCorrection(word, userDictionaryContains)
    }

    companion object {
        private const val MAX_CONTEXT_WORDS = 5
        private const val EXACT_MATCH_BOOST = 0.2f
        private const val USER_BIGRAM_BOOST = 0.15f
    }
}

/**
 * Interface for providing user learning scores to the prediction engine.
 * Implemented by LearningManager to decouple prediction from learning storage.
 */
interface LearningScorer {
    fun getUserFrequencyScore(word: String): Float
    fun getUserBigramScore(prevWord: String, word: String): Float
    fun getUserContinuations(prevWord: String, maxResults: Int): List<Pair<String, Int>>
}

data class Prediction(
    val word: String,
    val score: Float,
    val source: PredictionSource
)

enum class PredictionSource {
    TRIE,
    NGRAM,
    AUTOCORRECT,
    USER_DICTIONARY
}
