package com.unum.keyboard.core

data class TouchPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val timestamp: Long
)
