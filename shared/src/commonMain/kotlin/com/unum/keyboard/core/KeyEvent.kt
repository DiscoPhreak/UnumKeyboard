package com.unum.keyboard.core

data class KeyEvent(
    val keyCode: Int,
    val character: Char? = null,
    val isRepeat: Boolean = false,
    val timestamp: Long
)
