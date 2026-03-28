package com.unum.keyboard.core

sealed interface TextAction {
    data class Insert(val text: String) : TextAction
    data class Delete(val count: Int = 1) : TextAction
    data object MoveCursorLeft : TextAction
    data object MoveCursorRight : TextAction
    data class SetComposing(val text: String) : TextAction
    data object FinishComposing : TextAction
}
