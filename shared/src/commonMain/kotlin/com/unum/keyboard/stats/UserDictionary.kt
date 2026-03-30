package com.unum.keyboard.stats

/**
 * User-added custom words that aren't in the system dictionary.
 *
 * Words can be added explicitly (via settings) or auto-learned when
 * the user types the same unknown word multiple times.
 *
 * Serialization format: "word|addedTimestamp\n" per entry.
 */
class UserDictionary(
    val maxEntries: Int = MAX_ENTRIES
) {
    private val words = mutableMapOf<String, Long>() // word -> addedTimestamp

    fun addWord(word: String, timestamp: Long) {
        val key = word.lowercase()
        if (words.containsKey(key)) return
        words[key] = timestamp

        if (words.size > maxEntries) {
            val keysToRemove = words.entries
                .sortedBy { it.value }
                .take(words.size - maxEntries + 50)
                .map { it.key }
            for (key in keysToRemove) {
                words.remove(key)
            }
        }
    }

    fun removeWord(word: String) {
        words.remove(word.lowercase())
    }

    fun contains(word: String): Boolean =
        words.containsKey(word.lowercase())

    fun allWords(): List<String> =
        words.keys.toList()

    val size: Int get() = words.size

    fun serialize(): String {
        return words.entries.joinToString("\n") { (word, ts) ->
            "${word.replace("|", "\\|")}|$ts"
        }
    }

    fun deserialize(data: String) {
        words.clear()
        if (data.isBlank()) return

        for (line in data.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val pipeIdx = trimmed.lastIndexOf('|')
            if (pipeIdx > 0) {
                val word = trimmed.substring(0, pipeIdx).replace("\\|", "|")
                val ts = trimmed.substring(pipeIdx + 1).toLongOrNull() ?: continue
                words[word] = ts
            }
        }
    }

    fun clear() {
        words.clear()
    }

    companion object {
        const val MAX_ENTRIES = 2000
    }
}
