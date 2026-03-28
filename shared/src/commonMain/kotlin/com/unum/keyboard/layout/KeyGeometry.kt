package com.unum.keyboard.layout

data class Point(val x: Float, val y: Float)

data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean =
        x in left..right && y in top..bottom
}

data class KeyGeometry(
    val key: Key,
    val bounds: Rect,
    val center: Point,
    val neighbors: List<String>
)
