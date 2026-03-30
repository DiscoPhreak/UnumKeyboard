package com.unum.keyboard.core

sealed interface TextAction {
    data class Insert(val text: String) : TextAction
    data class Delete(val count: Int = 1) : TextAction
    data object MoveCursorLeft : TextAction
    data object MoveCursorRight : TextAction
    data class SetComposing(val text: String) : TextAction
    data object FinishComposing : TextAction

    // M10: Text editing actions
    /** Move cursor by the given offset (negative = left, positive = right) */
    data class MoveCursor(val offset: Int) : TextAction
    /** Select text by extending selection (negative = left, positive = right) */
    data class ExtendSelection(val offset: Int) : TextAction
    /** Select all text */
    data object SelectAll : TextAction
    /** Cut selected text to clipboard */
    data object Cut : TextAction
    /** Copy selected text to clipboard */
    data object Copy : TextAction
    /** Paste from clipboard */
    data object Paste : TextAction
    /** Undo last action */
    data object Undo : TextAction
    /** Redo last undone action */
    data object Redo : TextAction
}
