package com.unum.keyboard.prediction

/**
 * N-gram language model for word prediction scoring.
 * Stores bigram and trigram frequency tables indexed by word IDs.
 */
class NGramModel {
    // bigrams: prevWordId -> list of (nextWordId, frequency)
    private val bigrams = mutableMapOf<Int, MutableMap<Int, Int>>()
    // trigrams: (word1Id, word2Id) -> list of (nextWordId, frequency)
    private val trigrams = mutableMapOf<Long, MutableMap<Int, Int>>()

    // Word-to-ID lookup (shared with TrieDictionary)
    private val wordToId = mutableMapOf<String, Int>()

    fun setWordId(word: String, id: Int) {
        wordToId[word.lowercase()] = id
    }

    fun addBigram(prevWord: String, nextWord: String, frequency: Int) {
        val prevId = wordToId[prevWord.lowercase()] ?: return
        val nextId = wordToId[nextWord.lowercase()] ?: return
        bigrams.getOrPut(prevId) { mutableMapOf() }[nextId] = frequency
    }

    fun addTrigram(word1: String, word2: String, nextWord: String, frequency: Int) {
        val id1 = wordToId[word1.lowercase()] ?: return
        val id2 = wordToId[word2.lowercase()] ?: return
        val nextId = wordToId[nextWord.lowercase()] ?: return
        val key = packTrigramKey(id1, id2)
        trigrams.getOrPut(key) { mutableMapOf() }[nextId] = frequency
    }

    fun getBigramFrequency(prevWordId: Int, nextWordId: Int): Int {
        return bigrams[prevWordId]?.get(nextWordId) ?: 0
    }

    fun getTrigramFrequency(word1Id: Int, word2Id: Int, nextWordId: Int): Int {
        val key = packTrigramKey(word1Id, word2Id)
        return trigrams[key]?.get(nextWordId) ?: 0
    }

    /**
     * Get top bigram continuations for a given previous word.
     */
    fun getBigramContinuations(prevWordId: Int, maxResults: Int = 10): List<Pair<Int, Int>> {
        return bigrams[prevWordId]
            ?.entries
            ?.sortedByDescending { it.value }
            ?.take(maxResults)
            ?.map { it.key to it.value }
            ?: emptyList()
    }

    /**
     * Score a candidate word given context.
     *
     * score = w1*unigram + w2*P(word|prev) + w3*P(word|prev2,prev)
     */
    fun scoreCandidate(
        candidate: WordCandidate,
        prevWordId: Int,
        prev2WordId: Int,
        maxUnigramFreq: Int
    ): Float {
        val unigramScore = if (maxUnigramFreq > 0) {
            candidate.frequency.toFloat() / maxUnigramFreq
        } else 0f

        val bigramFreq = getBigramFrequency(prevWordId, candidate.wordId).toFloat()
        val bigramTotal = bigrams[prevWordId]?.values?.sum()?.toFloat() ?: 1f
        val bigramScore = if (bigramTotal > 0) bigramFreq / bigramTotal else 0f

        val trigramFreq = getTrigramFrequency(prev2WordId, prevWordId, candidate.wordId).toFloat()
        val trigramKey = packTrigramKey(prev2WordId, prevWordId)
        val trigramTotal = trigrams[trigramKey]?.values?.sum()?.toFloat() ?: 1f
        val trigramScore = if (trigramTotal > 0) trigramFreq / trigramTotal else 0f

        return UNIGRAM_WEIGHT * unigramScore +
               BIGRAM_WEIGHT * bigramScore +
               TRIGRAM_WEIGHT * trigramScore
    }

    /**
     * Load bigrams from text format: "word1 word2 frequency" per line
     */
    fun loadBigrams(text: String) {
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val parts = trimmed.split(WHITESPACE_REGEX, limit = 3)
            if (parts.size == 3) {
                val freq = parts[2].toIntOrNull() ?: continue
                addBigram(parts[0], parts[1], freq)
            }
        }
    }

    /**
     * Load trigrams from text format: "word1 word2 word3 frequency" per line
     */
    fun loadTrigrams(text: String) {
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val parts = trimmed.split(WHITESPACE_REGEX, limit = 4)
            if (parts.size == 4) {
                val freq = parts[3].toIntOrNull() ?: continue
                addTrigram(parts[0], parts[1], parts[2], freq)
            }
        }
    }

    private fun packTrigramKey(id1: Int, id2: Int): Long =
        (id1.toLong() shl 32) or id2.toLong().and(0xFFFFFFFFL)

    companion object {
        private val WHITESPACE_REGEX = "\\s+".toRegex()
        const val UNIGRAM_WEIGHT = 0.1f
        const val BIGRAM_WEIGHT = 0.3f
        const val TRIGRAM_WEIGHT = 0.4f
        // Remaining 0.2 reserved for user frequency (M11)
    }
}
