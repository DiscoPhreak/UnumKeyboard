package com.unum.keyboard.prediction

import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.min

/**
 * Feature-based context reranker that scores candidates using
 * n-gram continuity, word frequency, prefix quality, and length.
 *
 * This replaces the uniform-score stub with real context-aware scoring
 * while maintaining the NeuralReranker interface so it plugs directly
 * into the TwoStagePipeline.
 *
 * Features (6 signals, weighted):
 *   1. Bigram P(word|prev)       — strongest context signal
 *   2. Trigram P(word|prev2,prev) — even stronger when available
 *   3. Unigram frequency          — prior word probability
 *   4. Prefix match bonus         — exact prefix match quality
 *   5. Word length penalty        — penalize very short or very long
 *   6. Context coherence          — semantic similarity via shared n-gram neighbors
 */
class ContextReranker(
    private val dictionary: TrieDictionary,
    private val ngramModel: NGramModel,
    private var maxUnigramFreq: Int = 1
) : NeuralReranker {

    private var _isReady = false
    override val isReady: Boolean get() = _isReady

    override suspend fun load() {
        _isReady = true
    }

    override fun release() {
        _isReady = false
    }

    fun updateMaxUnigramFreq(freq: Int) {
        maxUnigramFreq = maxOf(freq, 1)
    }

    override suspend fun score(
        context: List<String>,
        candidates: List<String>
    ): Map<String, Float> {
        if (candidates.isEmpty()) return emptyMap()

        val prevWord = context.lastOrNull()?.lowercase()
        val prev2Word = if (context.size >= 2) context[context.size - 2].lowercase() else null

        val prevWordId = if (prevWord != null) dictionary.getWordId(prevWord) else -1
        val prev2WordId = if (prev2Word != null) dictionary.getWordId(prev2Word) else -1

        // Compute raw feature vectors for each candidate
        val rawScores = candidates.map { word ->
            val lower = word.lowercase()
            val wordId = dictionary.getWordId(lower)
            val freq = dictionary.getFrequency(lower)

            val features = computeFeatures(
                word = lower,
                wordId = wordId,
                frequency = freq,
                prevWordId = prevWordId,
                prev2WordId = prev2WordId,
                prefix = context.lastOrNull() ?: ""
            )
            word to features
        }

        // Apply softmax normalization to produce 0-1 scores
        val logits = rawScores.map { (_, features) -> features }
        val maxLogit = logits.maxOrNull() ?: 0f
        val expScores = logits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sumExp = expScores.sum().coerceAtLeast(1e-8f)

        return rawScores.zip(expScores).associate { (pair, expScore) ->
            pair.first to expScore / sumExp
        }
    }

    private fun computeFeatures(
        word: String,
        wordId: Int,
        frequency: Int,
        prevWordId: Int,
        prev2WordId: Int,
        prefix: String
    ): Float {
        var score = 0f

        // Feature 1: Unigram log-frequency (normalized)
        if (frequency > 0 && maxUnigramFreq > 0) {
            val logFreq = ln(frequency.toFloat() + 1f) / ln(maxUnigramFreq.toFloat() + 1f)
            score += UNIGRAM_WEIGHT * logFreq
        }

        // Feature 2: Bigram P(word|prev)
        if (prevWordId >= 0 && wordId >= 0) {
            val bigramFreq = ngramModel.getBigramFrequency(prevWordId, wordId)
            if (bigramFreq > 0) {
                // Use log-probability scaled to roughly 0-1
                val bigramScore = ln(bigramFreq.toFloat() + 1f) / 10f
                score += BIGRAM_WEIGHT * min(bigramScore, 1f)
            }
        }

        // Feature 3: Trigram P(word|prev2,prev)
        if (prev2WordId >= 0 && prevWordId >= 0 && wordId >= 0) {
            val trigramFreq = ngramModel.getTrigramFrequency(prev2WordId, prevWordId, wordId)
            if (trigramFreq > 0) {
                val trigramScore = ln(trigramFreq.toFloat() + 1f) / 8f
                score += TRIGRAM_WEIGHT * min(trigramScore, 1f)
            }
        }

        // Feature 4: Prefix match quality
        if (prefix.isNotEmpty()) {
            val lowerPrefix = prefix.lowercase()
            if (word.startsWith(lowerPrefix)) {
                // Reward longer prefix matches (more of the word already typed)
                val matchRatio = lowerPrefix.length.toFloat() / word.length.coerceAtLeast(1)
                score += PREFIX_WEIGHT * matchRatio
            }
        }

        // Feature 5: Word length appropriateness
        // Penalize very short (1-2 char) and very long (>12 char) completions
        val lenScore = when (word.length) {
            1 -> 0.2f
            2 -> 0.5f
            in 3..8 -> 1.0f
            in 9..12 -> 0.7f
            else -> 0.4f
        }
        score += LENGTH_WEIGHT * lenScore

        // Feature 6: Context coherence — bonus if candidate shares bigram neighbors with context
        if (prevWordId >= 0 && wordId >= 0) {
            val prevContinuations = ngramModel.getBigramContinuations(prevWordId, 20)
            val candidateRank = prevContinuations.indexOfFirst { it.first == wordId }
            if (candidateRank >= 0) {
                // Higher bonus for top-ranked continuations
                val rankBonus = 1f - (candidateRank.toFloat() / 20f)
                score += COHERENCE_WEIGHT * rankBonus
            }
        }

        return score
    }

    companion object {
        const val UNIGRAM_WEIGHT = 0.15f
        const val BIGRAM_WEIGHT = 0.25f
        const val TRIGRAM_WEIGHT = 0.25f
        const val PREFIX_WEIGHT = 0.10f
        const val LENGTH_WEIGHT = 0.05f
        const val COHERENCE_WEIGHT = 0.20f
    }
}
