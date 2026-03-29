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
        if (prevWordId < 0) return emptyList()

        val bigramContinuations = ngramModel.getBigramContinuations(prevWordId, maxResults * 2)
        return bigramContinuations.mapNotNull { (wordId, _) ->
            val word = dictionary.getWordById(wordId) ?: return@mapNotNull null
            val freq = dictionary.getFrequency(word)
            val candidate = WordCandidate(word, freq, wordId)
            val score = ngramModel.scoreCandidate(candidate, prevWordId, prev2WordId, maxUnigramFreq)
            Prediction(word, score, PredictionSource.NGRAM)
        }.sortedByDescending { it.score }
            .take(maxResults)
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
            val score = ngramModel.scoreCandidate(candidate, prevWordId, prev2WordId, maxUnigramFreq)
            val boostedScore = score + (candidate.frequency.toFloat() / maxUnigramFreq) * 0.1f
            results.add(Prediction(candidate.word, boostedScore, PredictionSource.TRIE))
        }

        // 2. Autocorrect if few trie results
        if (results.size < maxResults && prefix.length >= 2) {
            val corrections = autoCorrect.corrections(prefix, maxResults)
            for (correction in corrections) {
                if (results.any { it.word == correction.word }) continue
                val wordId = dictionary.getWordId(correction.word)
                val candidate = WordCandidate(correction.word, correction.frequency, wordId)
                val score = ngramModel.scoreCandidate(candidate, prevWordId, prev2WordId, maxUnigramFreq)
                // Penalize corrections by edit distance
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

    companion object {
        private const val MAX_CONTEXT_WORDS = 5
        private const val EXACT_MATCH_BOOST = 0.2f
    }
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
