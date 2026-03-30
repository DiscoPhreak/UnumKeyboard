package com.unum.keyboard.stats

/**
 * Tracks per-word usage frequency from user typing behavior.
 *
 * Uses exponential recency weighting: recent words score higher than
 * words typed long ago, preventing stale patterns from dominating.
 *
 * Serialization format: "word|count|lastUsed\n" per entry.
 */
class UserFrequencyModel(
    val maxEntries: Int = MAX_ENTRIES
) {
    private val entries = mutableMapOf<String, FrequencyEntry>()
    private var maxCount: Int = 1

    data class FrequencyEntry(
        var count: Int = 0,
        var lastUsedTimestamp: Long = 0L
    )

    fun recordUsage(word: String, timestamp: Long) {
        val key = word.lowercase()
        val entry = entries.getOrPut(key) { FrequencyEntry() }
        entry.count++
        entry.lastUsedTimestamp = timestamp
        if (entry.count > maxCount) maxCount = entry.count

        if (entries.size > maxEntries) {
            evictLeastUsed(timestamp)
        }
    }

    /**
     * Get a normalized score for a word, in [0, 1].
     * Combines raw count with recency weighting.
     */
    fun getScore(word: String, currentTime: Long): Float {
        val entry = entries[word.lowercase()] ?: return 0f
        val countScore = entry.count.toFloat() / maxCount

        val age = (currentTime - entry.lastUsedTimestamp).toFloat()
        val recencyScore = if (age <= 0) 1f else {
            val decay = (age / HALF_LIFE_MS).coerceAtMost(10f)
            1f / (1f + decay)
        }

        return FREQUENCY_BLEND * countScore + RECENCY_BLEND * recencyScore
    }

    fun getCount(word: String): Int =
        entries[word.lowercase()]?.count ?: 0

    fun hasWord(word: String): Boolean =
        entries.containsKey(word.lowercase())

    fun topWords(n: Int = 10): List<Pair<String, Int>> =
        entries.entries
            .sortedByDescending { it.value.count }
            .take(n)
            .map { it.key to it.value.count }

    val size: Int get() = entries.size

    private fun evictLeastUsed(now: Long) {
        val sorted = entries.entries.map { it.key to it.value }.sortedBy { (_, e) ->
            val age = (now - e.lastUsedTimestamp).toFloat()
            val recency = 1f / (1f + age / HALF_LIFE_MS)
            e.count * recency
        }
        val keysToRemove = sorted.take(entries.size - maxEntries + EVICT_BATCH).map { it.first }
        for (key in keysToRemove) {
            entries.remove(key)
        }
        recalcMax()
    }

    private fun recalcMax() {
        maxCount = entries.values.maxOfOrNull { it.count } ?: 1
    }

    fun serialize(): String {
        return entries.entries.joinToString("\n") { (word, entry) ->
            "${word.replace("|", "\\|")}|${entry.count}|${entry.lastUsedTimestamp}"
        }
    }

    fun deserialize(data: String) {
        entries.clear()
        maxCount = 1
        if (data.isBlank()) return

        for (line in data.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val parts = splitOnPipe(trimmed)
            if (parts.size == 3) {
                val word = parts[0].replace("\\|", "|")
                val count = parts[1].toIntOrNull() ?: continue
                val timestamp = parts[2].toLongOrNull() ?: continue
                entries[word] = FrequencyEntry(count, timestamp)
                if (count > maxCount) maxCount = count
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
        entries.clear()
        maxCount = 1
    }

    companion object {
        const val MAX_ENTRIES = 5000
        const val EVICT_BATCH = 100
        const val HALF_LIFE_MS = 604_800_000f // 7 days
        const val FREQUENCY_BLEND = 0.6f
        const val RECENCY_BLEND = 0.4f
    }
}
