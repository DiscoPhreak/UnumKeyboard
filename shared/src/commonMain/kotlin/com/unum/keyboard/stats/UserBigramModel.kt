package com.unum.keyboard.stats

/**
 * Tracks user-specific word pair (bigram) patterns.
 *
 * When the user frequently types "thank you" or "on my way",
 * this model boosts those continuations in next-word prediction.
 *
 * Serialization format: "prevWord|nextWord|count\n" per entry.
 */
class UserBigramModel(
    val maxEntries: Int = MAX_ENTRIES
) {
    // prevWord -> (nextWord -> count)
    private val bigrams = mutableMapOf<String, MutableMap<String, Int>>()
    private var totalEntries: Int = 0

    fun recordBigram(prevWord: String, nextWord: String) {
        val prev = prevWord.lowercase()
        val next = nextWord.lowercase()
        val map = bigrams.getOrPut(prev) { mutableMapOf() }
        map[next] = (map[next] ?: 0) + 1
        totalEntries++

        if (totalEntries > maxEntries) {
            evictLowFrequency()
        }
    }

    /**
     * Get P(nextWord | prevWord) based on user history.
     * Returns a value in [0, 1].
     */
    fun getScore(prevWord: String, nextWord: String): Float {
        val map = bigrams[prevWord.lowercase()] ?: return 0f
        val count = map[nextWord.lowercase()] ?: return 0f
        val total = map.values.sum()
        return if (total > 0) count.toFloat() / total else 0f
    }

    /**
     * Get top continuations after a given word, sorted by frequency.
     */
    fun getContinuations(prevWord: String, maxResults: Int = 5): List<Pair<String, Int>> {
        val map = bigrams[prevWord.lowercase()] ?: return emptyList()
        return map.entries
            .sortedByDescending { it.value }
            .take(maxResults)
            .map { it.key to it.value }
    }

    val size: Int get() = totalEntries

    private fun evictLowFrequency() {
        val allEntries = mutableListOf<Triple<String, String, Int>>()
        for ((prev, map) in bigrams) {
            for ((next, count) in map) {
                allEntries.add(Triple(prev, next, count))
            }
        }
        allEntries.sortBy { it.third }
        val toRemove = allEntries.take(totalEntries - maxEntries + EVICT_BATCH)
        for ((prev, next, _) in toRemove) {
            bigrams[prev]?.remove(next)
            if (bigrams[prev]?.isEmpty() == true) bigrams.remove(prev)
            totalEntries--
        }
    }

    fun serialize(): String {
        val sb = StringBuilder()
        for ((prev, map) in bigrams) {
            for ((next, count) in map) {
                sb.append("${prev.replace("|", "\\|")}|${next.replace("|", "\\|")}|$count\n")
            }
        }
        return sb.toString().trimEnd('\n')
    }

    fun deserialize(data: String) {
        bigrams.clear()
        totalEntries = 0
        if (data.isBlank()) return

        for (line in data.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val parts = splitOnPipe(trimmed)
            if (parts.size == 3) {
                val prev = parts[0].replace("\\|", "|")
                val next = parts[1].replace("\\|", "|")
                val count = parts[2].toIntOrNull() ?: continue
                bigrams.getOrPut(prev) { mutableMapOf() }[next] = count
                totalEntries++
            }
        }
    }

    private fun splitOnPipe(s: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < s.length) {
            if (s[i] == '\\' && i + 1 < s.length && s[i + 1] == '|') {
                current.append("\\|")
                i += 2
            } else if (s[i] == '|') {
                parts.add(current.toString())
                current.clear()
                i++
            } else {
                current.append(s[i])
                i++
            }
        }
        parts.add(current.toString())
        return parts
    }

    fun clear() {
        bigrams.clear()
        totalEntries = 0
    }

    companion object {
        const val MAX_ENTRIES = 10000
        const val EVICT_BATCH = 500
    }
}
