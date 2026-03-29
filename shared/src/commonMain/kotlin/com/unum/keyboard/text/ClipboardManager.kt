package com.unum.keyboard.text

/**
 * Manages clipboard history for the Unum Keyboard.
 *
 * Stores recent clipboard entries (text clips) and allows quick access
 * for pasting. Supports pinned clips that persist across sessions.
 *
 * The history is stored in-memory with persistence via PlatformSettings
 * using a simple serialization format.
 */
class ClipboardManager(
    private val maxHistory: Int = DEFAULT_MAX_HISTORY
) {
    private val _history = mutableListOf<ClipEntry>()

    /** Recent clipboard entries, newest first */
    val history: List<ClipEntry> get() = _history.toList()

    /** Pinned clips that persist at the top */
    val pinnedClips: List<ClipEntry> get() = _history.filter { it.isPinned }

    /** Unpinned (regular) clips */
    val recentClips: List<ClipEntry> get() = _history.filter { !it.isPinned }

    /**
     * Add a new clip to the history.
     * If the same text already exists, it's moved to the top.
     * Pinned items are not affected by new additions.
     */
    fun addClip(text: String, timestamp: Long = currentTimeMillis()) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // Remove duplicate (if exists and not pinned)
        _history.removeAll { it.text == trimmed && !it.isPinned }

        // Add to front (after pinned items)
        val pinnedCount = _history.count { it.isPinned }
        _history.add(pinnedCount, ClipEntry(
            text = trimmed,
            timestamp = timestamp,
            isPinned = false
        ))

        // Trim unpinned entries to max history
        trimHistory()
    }

    /**
     * Pin a clip so it persists and stays at the top.
     */
    fun pinClip(index: Int) {
        if (index !in _history.indices) return
        val entry = _history[index]
        if (entry.isPinned) return

        _history.removeAt(index)
        val pinnedCount = _history.count { it.isPinned }
        _history.add(pinnedCount, entry.copy(isPinned = true))
    }

    /**
     * Unpin a clip, returning it to the regular history.
     */
    fun unpinClip(index: Int) {
        if (index !in _history.indices) return
        val entry = _history[index]
        if (!entry.isPinned) return

        _history.removeAt(index)
        val pinnedCount = _history.count { it.isPinned }
        _history.add(pinnedCount, entry.copy(isPinned = false))
    }

    /**
     * Remove a clip from history.
     */
    fun removeClip(index: Int) {
        if (index in _history.indices) {
            _history.removeAt(index)
        }
    }

    /**
     * Get a clip by index.
     */
    fun getClip(index: Int): ClipEntry? = _history.getOrNull(index)

    /**
     * Clear all unpinned clips.
     */
    fun clearHistory() {
        _history.removeAll { !it.isPinned }
    }

    /**
     * Clear everything including pinned clips.
     */
    fun clearAll() {
        _history.clear()
    }

    /** Number of entries */
    val size: Int get() = _history.size

    /**
     * Serialize history to a string for persistent storage.
     * Format: each entry is "P|timestamp|text" or "U|timestamp|text" per line.
     * Text is escaped: newlines → \\n, pipes → \\p
     */
    fun serialize(): String {
        return _history.joinToString("\n") { entry ->
            val prefix = if (entry.isPinned) "P" else "U"
            val escaped = entry.text
                .replace("\\", "\\\\")
                .replace("|", "\\p")
                .replace("\n", "\\n")
            "$prefix|${entry.timestamp}|$escaped"
        }
    }

    /**
     * Restore history from serialized string.
     */
    fun deserialize(data: String) {
        _history.clear()
        if (data.isBlank()) return

        for (line in data.lines()) {
            if (line.isBlank()) continue
            val parts = line.split("|", limit = 3)
            if (parts.size != 3) continue

            val isPinned = parts[0] == "P"
            val timestamp = parts[1].toLongOrNull() ?: continue
            val text = parts[2]
                .replace("\\n", "\n")
                .replace("\\p", "|")
                .replace("\\\\", "\\")

            if (text.isNotEmpty()) {
                _history.add(ClipEntry(text, timestamp, isPinned))
            }
        }

        trimHistory()
    }

    /**
     * Search clips containing the given query.
     */
    fun search(query: String): List<ClipEntry> {
        if (query.isBlank()) return history
        val lower = query.lowercase()
        return _history.filter { it.text.lowercase().contains(lower) }
    }

    private fun trimHistory() {
        val pinnedCount = _history.count { it.isPinned }
        val maxUnpinned = maxHistory - pinnedCount
        if (maxUnpinned <= 0) return

        val unpinnedEntries = _history.filter { !it.isPinned }
        if (unpinnedEntries.size > maxUnpinned) {
            val toRemove = unpinnedEntries.drop(maxUnpinned)
            _history.removeAll(toRemove.toSet())
        }
    }

    companion object {
        const val DEFAULT_MAX_HISTORY = 25

        // Simple time function — will be overridden per platform if needed
        private fun currentTimeMillis(): Long {
            // In KMP, we use kotlin.system or platform-specific time
            // For simplicity, default to 0 — callers should pass timestamp
            return 0L
        }
    }
}

/**
 * A single clipboard entry.
 */
data class ClipEntry(
    /** The clipped text content */
    val text: String,
    /** When the clip was captured (Unix millis) */
    val timestamp: Long,
    /** Whether this clip is pinned (persists and stays at top) */
    val isPinned: Boolean = false
) {
    /** Preview text (first line, truncated) for display */
    val preview: String get() {
        val firstLine = text.lines().firstOrNull() ?: text
        return if (firstLine.length > MAX_PREVIEW_LENGTH) {
            firstLine.take(MAX_PREVIEW_LENGTH) + "…"
        } else {
            firstLine
        }
    }

    companion object {
        const val MAX_PREVIEW_LENGTH = 50
    }
}
