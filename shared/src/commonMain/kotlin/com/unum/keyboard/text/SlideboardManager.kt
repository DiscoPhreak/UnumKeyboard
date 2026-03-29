package com.unum.keyboard.text

/**
 * Manages text snippets (slideboard) for quick insertion.
 *
 * Users can create named snippets like email addresses, phone numbers,
 * common phrases, etc. and insert them with a tap from the slideboard panel.
 */
class SlideboardManager(
    private val maxSnippets: Int = DEFAULT_MAX_SNIPPETS
) {
    private val _snippets = mutableListOf<TextSnippet>()

    /** All snippets, ordered by usage count (most used first) */
    val snippets: List<TextSnippet> get() = _snippets.sortedByDescending { it.usageCount }

    /**
     * Add a new snippet.
     * @return true if added, false if at max capacity
     */
    fun addSnippet(label: String, text: String, category: SnippetCategory = SnippetCategory.GENERAL): Boolean {
        if (_snippets.size >= maxSnippets) return false
        if (label.isBlank() || text.isBlank()) return false

        // Check for duplicate label
        if (_snippets.any { it.label.equals(label, ignoreCase = true) }) return false

        _snippets.add(TextSnippet(
            label = label.trim(),
            text = text,
            category = category,
            usageCount = 0
        ))
        return true
    }

    /**
     * Remove a snippet by index.
     */
    fun removeSnippet(index: Int) {
        val sorted = snippets
        if (index in sorted.indices) {
            val snippet = sorted[index]
            _snippets.remove(snippet)
        }
    }

    /**
     * Record usage of a snippet (increments usage counter for sorting).
     */
    fun recordUsage(label: String) {
        val snippet = _snippets.find { it.label == label } ?: return
        val index = _snippets.indexOf(snippet)
        _snippets[index] = snippet.copy(usageCount = snippet.usageCount + 1)
    }

    /**
     * Get snippets filtered by category.
     */
    fun getByCategory(category: SnippetCategory): List<TextSnippet> {
        return snippets.filter { it.category == category }
    }

    /**
     * Find a snippet by label.
     */
    fun findByLabel(label: String): TextSnippet? {
        return _snippets.find { it.label.equals(label, ignoreCase = true) }
    }

    /** Number of snippets */
    val size: Int get() = _snippets.size

    /**
     * Serialize snippets for persistent storage.
     * Format: "category|usageCount|label|text" per line.
     */
    fun serialize(): String {
        return _snippets.joinToString("\n") { snippet ->
            val escapedLabel = snippet.label.replace("|", "\\p").replace("\n", "\\n")
            val escapedText = snippet.text
                .replace("\\", "\\\\")
                .replace("|", "\\p")
                .replace("\n", "\\n")
            "${snippet.category.name}|${snippet.usageCount}|$escapedLabel|$escapedText"
        }
    }

    /**
     * Restore snippets from serialized string.
     */
    fun deserialize(data: String) {
        _snippets.clear()
        if (data.isBlank()) return

        for (line in data.lines()) {
            if (line.isBlank()) continue
            val parts = line.split("|", limit = 4)
            if (parts.size != 4) continue

            val category = try { SnippetCategory.valueOf(parts[0]) } catch (_: Exception) { SnippetCategory.GENERAL }
            val usageCount = parts[1].toIntOrNull() ?: 0
            val label = parts[2].replace("\\p", "|").replace("\\n", "\n")
            val text = parts[3]
                .replace("\\n", "\n")
                .replace("\\p", "|")
                .replace("\\\\", "\\")

            if (label.isNotBlank() && text.isNotBlank()) {
                _snippets.add(TextSnippet(label, text, category, usageCount))
            }
        }
    }

    /**
     * Clear all snippets.
     */
    fun clearAll() {
        _snippets.clear()
    }

    companion object {
        const val DEFAULT_MAX_SNIPPETS = 50
    }
}

/**
 * A saved text snippet for quick insertion.
 */
data class TextSnippet(
    /** User-visible label (e.g., "My Email", "Home Address") */
    val label: String,
    /** The full text content to insert */
    val text: String,
    /** Category for organization */
    val category: SnippetCategory = SnippetCategory.GENERAL,
    /** Number of times this snippet has been used */
    val usageCount: Int = 0
)

/**
 * Categories for organizing snippets.
 */
enum class SnippetCategory {
    GENERAL,
    EMAIL,
    ADDRESS,
    PHONE,
    URL,
    EMOJI,
    CUSTOM
}
