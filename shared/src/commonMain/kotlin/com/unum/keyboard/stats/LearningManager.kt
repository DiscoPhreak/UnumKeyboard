package com.unum.keyboard.stats

import com.unum.keyboard.prediction.TrieDictionary

/**
 * Orchestrates user learning across frequency, bigram, and dictionary models.
 *
 * Integrates with the prediction pipeline to:
 * 1. Boost words the user types frequently (UserFrequencyModel)
 * 2. Learn word pair patterns for next-word prediction (UserBigramModel)
 * 3. Auto-learn unknown words after repeated use (UserDictionary)
 */
class LearningManager(
    private val dictionary: TrieDictionary? = null
) {
    val frequencyModel = UserFrequencyModel()
    val bigramModel = UserBigramModel()
    val userDictionary = UserDictionary()

    private var lastCommittedWord: String? = null
    private var unknownWordCounts = mutableMapOf<String, Int>()

    /**
     * Called when a word is committed (space pressed or suggestion accepted).
     */
    fun onWordCommitted(word: String, timestamp: Long) {
        val lower = word.lowercase()
        if (lower.isBlank() || lower.length < 2) return

        // 1. Record frequency
        frequencyModel.recordUsage(lower, timestamp)

        // 2. Record bigram with previous word
        lastCommittedWord?.let { prev ->
            bigramModel.recordBigram(prev, lower)
        }
        lastCommittedWord = lower

        // 3. Auto-learn unknown words
        if (dictionary != null && !dictionary.contains(lower) && !userDictionary.contains(lower)) {
            val count = (unknownWordCounts[lower] ?: 0) + 1
            unknownWordCounts[lower] = count
            if (count >= AUTO_LEARN_THRESHOLD) {
                userDictionary.addWord(lower, timestamp)
                dictionary.insert(lower, USER_WORD_BASE_FREQUENCY)
                unknownWordCounts.remove(lower)
            }
        }
    }

    /**
     * Called on sentence boundary (period, question mark, etc.)
     */
    fun onSentenceBoundary() {
        lastCommittedWord = null
    }

    /**
     * Get the user frequency score for a word (for PredictionEngine integration).
     */
    fun getUserScore(word: String, currentTime: Long): Float {
        return frequencyModel.getScore(word, currentTime)
    }

    /**
     * Get the user bigram score for a word given the previous word.
     */
    fun getUserBigramScore(prevWord: String, word: String): Float {
        return bigramModel.getScore(prevWord, word)
    }

    /**
     * Get user bigram continuations for next-word prediction.
     */
    fun getUserContinuations(prevWord: String, maxResults: Int = 5): List<Pair<String, Int>> {
        return bigramModel.getContinuations(prevWord, maxResults)
    }

    /**
     * Manually add a word to user dictionary.
     */
    fun addUserWord(word: String, timestamp: Long) {
        userDictionary.addWord(word, timestamp)
        dictionary?.insert(word.lowercase(), USER_WORD_BASE_FREQUENCY)
    }

    /**
     * Remove a word from user dictionary.
     */
    fun removeUserWord(word: String) {
        userDictionary.removeWord(word)
    }

    /**
     * Serialize all learning data for persistence.
     * Format: three sections separated by SECTION_DELIMITER.
     */
    fun serialize(): String {
        return buildString {
            append(frequencyModel.serialize())
            append(SECTION_DELIMITER)
            append(bigramModel.serialize())
            append(SECTION_DELIMITER)
            append(userDictionary.serialize())
        }
    }

    /**
     * Deserialize learning data from persistence.
     */
    fun deserialize(data: String) {
        if (data.isBlank()) return

        val sections = data.split(SECTION_DELIMITER)
        if (sections.isNotEmpty()) frequencyModel.deserialize(sections[0])
        if (sections.size > 1) bigramModel.deserialize(sections[1])
        if (sections.size > 2) {
            userDictionary.deserialize(sections[2])
            // Re-insert user dictionary words into trie
            if (dictionary != null) {
                for (word in userDictionary.allWords()) {
                    if (!dictionary.contains(word)) {
                        dictionary.insert(word, USER_WORD_BASE_FREQUENCY)
                    }
                }
            }
        }
    }

    fun clear() {
        frequencyModel.clear()
        bigramModel.clear()
        userDictionary.clear()
        unknownWordCounts.clear()
        lastCommittedWord = null
    }

    companion object {
        const val SECTION_DELIMITER = "\n===SECTION===\n"
        const val AUTO_LEARN_THRESHOLD = 3
        const val USER_WORD_BASE_FREQUENCY = 50
        const val USER_FREQUENCY_WEIGHT = 0.2f
    }
}
