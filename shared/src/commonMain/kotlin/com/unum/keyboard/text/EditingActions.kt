package com.unum.keyboard.text

/**
 * Defines the editing toolbar actions available in text editing mode.
 *
 * The editing toolbar appears when the user activates the CTRL/editing mode,
 * providing quick access to cursor navigation, selection, and clipboard operations.
 */
enum class EditingAction(
    val id: String,
    val label: String,
    val icon: String
) {
    // Cursor navigation
    CURSOR_LEFT("cursor_left", "←", "←"),
    CURSOR_RIGHT("cursor_right", "→", "→"),
    CURSOR_WORD_LEFT("cursor_word_left", "⇤", "⇤"),
    CURSOR_WORD_RIGHT("cursor_word_right", "⇥", "⇥"),
    CURSOR_LINE_START("cursor_line_start", "⇱", "⇱"),
    CURSOR_LINE_END("cursor_line_end", "⇲", "⇲"),

    // Selection
    SELECT_ALL("select_all", "Sel All", "▣"),
    SELECT_LEFT("select_left", "◁", "◁"),
    SELECT_RIGHT("select_right", "▷", "▷"),

    // Clipboard
    CUT("cut", "Cut", "✂"),
    COPY("copy", "Copy", "⧉"),
    PASTE("paste", "Paste", "📋"),

    // Undo/Redo
    UNDO("undo", "Undo", "↩"),
    REDO("redo", "Redo", "↪");

    companion object {
        /**
         * Default toolbar layout — two rows of actions.
         */
        val toolbarRow1 = listOf(
            CURSOR_LEFT, CURSOR_RIGHT, CURSOR_WORD_LEFT, CURSOR_WORD_RIGHT,
            CURSOR_LINE_START, CURSOR_LINE_END
        )

        val toolbarRow2 = listOf(
            SELECT_ALL, CUT, COPY, PASTE, UNDO, REDO
        )

        fun fromId(id: String): EditingAction? = entries.find { it.id == id }
    }
}
