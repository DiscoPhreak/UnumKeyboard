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
    /** Characters accessible by flicking in cardinal directions */
    val flickUp: String? = null,
    val flickDown: String? = null,
    val flickLeft: String? = null,
    val flickRight: String? = null,
    val width: Float = 1.0f,
    val type: KeyType = KeyType.CHARACTER
) {
    /** Get the flick character for a given direction, or null if none mapped */
    fun flickChar(direction: FlickDirection): String? = when (direction) {
        FlickDirection.UP -> flickUp
        FlickDirection.DOWN -> flickDown
        FlickDirection.LEFT -> flickLeft
        FlickDirection.RIGHT -> flickRight
        FlickDirection.NONE -> null
    }
}

/** Cardinal flick directions */
enum class FlickDirection {
    NONE, UP, DOWN, LEFT, RIGHT
}

data class KeyRow(val keys: List<Key>)

data class KeyboardLayout(
    val id: String,
    val rows: List<KeyRow>,
    val isRTL: Boolean = false
)
