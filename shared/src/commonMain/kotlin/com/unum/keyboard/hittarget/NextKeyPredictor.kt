package com.unum.keyboard.hittarget

/**
 * Predicts the probability of each key being typed next, based on context.
 *
 * Uses character-level n-gram statistics derived from the word prediction engine.
 * For example, after typing "th", 'e' has very high probability (the, there, them, etc).
 */
class NextKeyPredictor {

    // Character bigram frequencies: P(next_char | prev_char)
    // Built from English character statistics
    private val charBigrams: Map<Char, Map<Char, Float>> = buildCharBigrams()

    // Character unigram frequencies (overall letter frequency in English)
    private val charUnigrams: Map<Char, Float> = buildCharUnigrams()

    /**
     * Predict probability of each character key being typed next.
     *
     * @param currentPrefix The characters typed so far in the current word
     * @return Map of character to probability (0-1), normalized to sum to ~1
     */
    fun predictNextKey(currentPrefix: String): Map<Char, Float> {
        if (currentPrefix.isEmpty()) {
            // No context — use unigram frequencies (common word-start letters)
            return WORD_START_FREQUENCIES
        }

        val lastChar = currentPrefix.last().lowercaseChar()
        val bigramProbs = charBigrams[lastChar]

        return if (bigramProbs != null && bigramProbs.isNotEmpty()) {
            // Blend bigram prediction with unigram fallback
            val blended = mutableMapOf<Char, Float>()
            for (c in 'a'..'z') {
                val bigramP = bigramProbs[c] ?: 0.001f
                val unigramP = charUnigrams[c] ?: 0.01f
                blended[c] = BIGRAM_WEIGHT * bigramP + UNIGRAM_WEIGHT * unigramP
            }
            normalize(blended)
        } else {
            charUnigrams.toMutableMap()
        }
    }

    private fun normalize(probs: MutableMap<Char, Float>): Map<Char, Float> {
        val sum = probs.values.sum()
        if (sum > 0) {
            for (key in probs.keys) {
                probs[key] = probs[key]!! / sum
            }
        }
        return probs
    }

    companion object {
        private const val BIGRAM_WEIGHT = 0.7f
        private const val UNIGRAM_WEIGHT = 0.3f

        // Common word-start letter frequencies in English
        private val WORD_START_FREQUENCIES = mapOf(
            't' to 0.16f, 'a' to 0.11f, 'o' to 0.07f, 's' to 0.07f,
            'w' to 0.06f, 'c' to 0.05f, 'b' to 0.04f, 'h' to 0.05f,
            'i' to 0.06f, 'd' to 0.04f, 'f' to 0.04f, 'm' to 0.04f,
            'p' to 0.03f, 'n' to 0.02f, 'e' to 0.03f, 'r' to 0.02f,
            'l' to 0.02f, 'g' to 0.02f, 'u' to 0.01f, 'v' to 0.01f,
            'j' to 0.01f, 'k' to 0.01f, 'y' to 0.01f, 'q' to 0.005f,
            'x' to 0.002f, 'z' to 0.002f
        )

        /**
         * English character bigram frequencies.
         * P(next | previous) — most common character transitions.
         */
        private fun buildCharBigrams(): Map<Char, Map<Char, Float>> {
            return mapOf(
                't' to mapOf('h' to 0.35f, 'o' to 0.12f, 'i' to 0.10f, 'e' to 0.08f, 'a' to 0.07f, 'r' to 0.05f, 's' to 0.04f, 'u' to 0.03f, 't' to 0.02f, 'y' to 0.02f),
                'h' to mapOf('e' to 0.35f, 'a' to 0.15f, 'i' to 0.12f, 'o' to 0.10f, 'u' to 0.03f, 'r' to 0.02f),
                'a' to mapOf('n' to 0.15f, 't' to 0.12f, 'l' to 0.10f, 'r' to 0.08f, 's' to 0.07f, 'd' to 0.05f, 'c' to 0.04f, 'i' to 0.04f, 'b' to 0.03f, 'y' to 0.03f, 'm' to 0.03f),
                'e' to mapOf('r' to 0.15f, 'n' to 0.12f, 'd' to 0.10f, 's' to 0.08f, 'a' to 0.06f, 'l' to 0.05f, 'e' to 0.04f, 'x' to 0.03f, 'c' to 0.03f, 't' to 0.03f),
                'i' to mapOf('n' to 0.20f, 't' to 0.12f, 'o' to 0.08f, 's' to 0.07f, 'c' to 0.05f, 'l' to 0.05f, 'd' to 0.04f, 'e' to 0.04f, 'a' to 0.03f, 'r' to 0.03f),
                'o' to mapOf('n' to 0.15f, 'r' to 0.10f, 'f' to 0.08f, 'u' to 0.07f, 't' to 0.06f, 'w' to 0.04f, 'o' to 0.03f, 'l' to 0.03f, 'm' to 0.03f),
                'n' to mapOf('g' to 0.15f, 'd' to 0.12f, 'e' to 0.10f, 't' to 0.08f, 'o' to 0.07f, 'i' to 0.05f, 's' to 0.04f, 'a' to 0.03f, 'c' to 0.03f),
                's' to mapOf('t' to 0.15f, 'e' to 0.10f, 'h' to 0.08f, 'o' to 0.07f, 's' to 0.05f, 'i' to 0.05f, 'a' to 0.04f, 'u' to 0.04f, 'p' to 0.03f),
                'r' to mapOf('e' to 0.18f, 'o' to 0.10f, 'a' to 0.08f, 'i' to 0.07f, 's' to 0.05f, 'u' to 0.04f, 'n' to 0.03f, 'y' to 0.03f),
                'l' to mapOf('e' to 0.15f, 'l' to 0.10f, 'i' to 0.10f, 'y' to 0.08f, 'a' to 0.07f, 'o' to 0.05f, 'u' to 0.03f),
                'd' to mapOf('e' to 0.15f, 'i' to 0.10f, 'o' to 0.08f, 'a' to 0.07f, 's' to 0.06f, 'u' to 0.03f),
                'c' to mapOf('o' to 0.15f, 'a' to 0.12f, 'e' to 0.10f, 'h' to 0.10f, 'k' to 0.08f, 't' to 0.05f, 'i' to 0.04f, 'u' to 0.03f),
                'u' to mapOf('r' to 0.12f, 's' to 0.10f, 'n' to 0.10f, 't' to 0.08f, 'l' to 0.07f, 'p' to 0.05f, 'c' to 0.04f),
                'p' to mapOf('e' to 0.12f, 'r' to 0.10f, 'a' to 0.10f, 'o' to 0.08f, 'l' to 0.06f, 'p' to 0.04f, 'i' to 0.04f),
                'm' to mapOf('e' to 0.15f, 'a' to 0.12f, 'o' to 0.08f, 'i' to 0.07f, 'u' to 0.05f, 'y' to 0.03f),
                'f' to mapOf('o' to 0.15f, 'r' to 0.12f, 'i' to 0.08f, 'e' to 0.08f, 'u' to 0.05f, 'a' to 0.05f),
                'g' to mapOf('e' to 0.12f, 'o' to 0.10f, 'r' to 0.08f, 'h' to 0.06f, 'a' to 0.06f, 'i' to 0.05f, 'u' to 0.04f),
                'w' to mapOf('a' to 0.15f, 'i' to 0.12f, 'o' to 0.10f, 'h' to 0.08f, 'e' to 0.08f, 'r' to 0.03f),
                'b' to mapOf('e' to 0.15f, 'a' to 0.10f, 'u' to 0.08f, 'l' to 0.06f, 'o' to 0.06f, 'r' to 0.05f, 'i' to 0.04f),
                'y' to mapOf('o' to 0.15f, 's' to 0.08f, 'e' to 0.06f, 'i' to 0.05f),
                'v' to mapOf('e' to 0.25f, 'i' to 0.10f, 'a' to 0.08f, 'o' to 0.05f),
                'k' to mapOf('e' to 0.15f, 'i' to 0.10f, 'n' to 0.08f, 's' to 0.05f),
                'j' to mapOf('u' to 0.15f, 'o' to 0.10f, 'a' to 0.08f, 'e' to 0.05f),
                'x' to mapOf('t' to 0.15f, 'p' to 0.10f, 'i' to 0.08f, 'a' to 0.05f),
                'q' to mapOf('u' to 0.95f),
                'z' to mapOf('e' to 0.15f, 'a' to 0.10f, 'o' to 0.08f, 'i' to 0.05f)
            )
        }

        /** Overall English letter frequency */
        private fun buildCharUnigrams(): Map<Char, Float> {
            return mapOf(
                'e' to 0.127f, 't' to 0.091f, 'a' to 0.082f, 'o' to 0.075f,
                'i' to 0.070f, 'n' to 0.067f, 's' to 0.063f, 'h' to 0.061f,
                'r' to 0.060f, 'd' to 0.043f, 'l' to 0.040f, 'c' to 0.028f,
                'u' to 0.028f, 'm' to 0.024f, 'w' to 0.024f, 'f' to 0.022f,
                'g' to 0.020f, 'y' to 0.020f, 'p' to 0.019f, 'b' to 0.015f,
                'v' to 0.010f, 'k' to 0.008f, 'j' to 0.002f, 'x' to 0.002f,
                'q' to 0.001f, 'z' to 0.001f
            )
        }
    }
}
