package com.unum.keyboard.layout

enum class KeyType {
    CHARACTER,
    SHIFT,
    BACKSPACE,
    SPACE,
    ENTER,
    SYMBOL_TOGGLE,
    CTRL
}

data class Key(
    val id: String,
    val primary: String,
    val secondary: String? = null,
    val width: Float = 1.0f,
    val type: KeyType = KeyType.CHARACTER
)

data class KeyRow(val keys: List<Key>)

data class KeyboardLayout(
    val id: String,
    val rows: List<KeyRow>,
    val isRTL: Boolean = false
)
