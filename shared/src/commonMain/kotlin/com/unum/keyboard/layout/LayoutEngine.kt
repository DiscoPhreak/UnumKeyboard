package com.unum.keyboard.layout

import kotlin.math.sqrt

class LayoutEngine {

    data class ComputedLayout(
        val layout: KeyboardLayout,
        val keys: List<KeyGeometry>,
        val adjacencyMap: Map<String, List<String>>,
        val screenWidth: Float,
        val screenHeight: Float
    ) {
        private val keyById: Map<String, KeyGeometry> = keys.associateBy { it.key.id }

        fun findKeyAt(x: Float, y: Float): KeyGeometry? =
            keys.find { it.bounds.contains(x, y) }

        fun getGeometry(keyId: String): KeyGeometry? = keyById[keyId]
    }

    fun computeLayout(
        layout: KeyboardLayout,
        screenWidth: Float,
        keyboardHeight: Float,
        horizontalPadding: Float = 0f,
        verticalPadding: Float = 0f,
        keySpacing: Float = 4f
    ): ComputedLayout {
        val geometries = mutableListOf<KeyGeometry>()
        val rowCount = layout.rows.size
        val availableHeight = keyboardHeight - verticalPadding * 2 - keySpacing * (rowCount - 1)
        val rowHeight = availableHeight / rowCount

        for ((rowIndex, row) in layout.rows.withIndex()) {
            val totalWeight = row.keys.sumOf { it.width.toDouble() }.toFloat()
            val availableWidth = screenWidth - horizontalPadding * 2 - keySpacing * (row.keys.size - 1)
            val unitWidth = availableWidth / totalWeight

            val rowTop = verticalPadding + rowIndex * (rowHeight + keySpacing)
            var keyLeft = horizontalPadding

            // Center rows with fewer keys (like the 9-key second row)
            val rowTotalWidth = row.keys.sumOf { (it.width * unitWidth + keySpacing).toDouble() }.toFloat() - keySpacing
            if (rowTotalWidth < screenWidth - horizontalPadding * 2) {
                keyLeft = (screenWidth - rowTotalWidth) / 2f
            }

            for (key in row.keys) {
                val keyWidth = key.width * unitWidth
                val bounds = Rect(
                    left = keyLeft,
                    top = rowTop,
                    right = keyLeft + keyWidth,
                    bottom = rowTop + rowHeight
                )
                val center = Point(bounds.centerX, bounds.centerY)

                // Neighbors computed after all keys are placed
                geometries.add(KeyGeometry(key, bounds, center, emptyList()))
                keyLeft += keyWidth + keySpacing
            }
        }

        val adjacencyMap = computeAdjacency(geometries)

        val finalGeometries = geometries.map { geo ->
            geo.copy(neighbors = adjacencyMap[geo.key.id] ?: emptyList())
        }

        return ComputedLayout(
            layout = layout,
            keys = finalGeometries,
            adjacencyMap = adjacencyMap,
            screenWidth = screenWidth,
            screenHeight = keyboardHeight
        )
    }

    /**
     * Computes neighbor adjacency based on distance between key centers.
     * Two keys are neighbors if the distance between their centers is less than
     * the neighbor threshold (1.8x the average of their widths).
     */
    fun computeAdjacency(
        geometries: List<KeyGeometry>,
        thresholdMultiplier: Float = 1.8f
    ): Map<String, List<String>> {
        val adjacency = mutableMapOf<String, MutableList<String>>()

        for (i in geometries.indices) {
            val a = geometries[i]
            adjacency.getOrPut(a.key.id) { mutableListOf() }

            for (j in i + 1 until geometries.size) {
                val b = geometries[j]
                val dx = a.center.x - b.center.x
                val dy = a.center.y - b.center.y
                val distance = sqrt(dx * dx + dy * dy)
                val avgWidth = (a.bounds.width + b.bounds.width) / 2f
                val threshold = avgWidth * thresholdMultiplier

                if (distance < threshold) {
                    adjacency.getOrPut(a.key.id) { mutableListOf() }.add(b.key.id)
                    adjacency.getOrPut(b.key.id) { mutableListOf() }.add(a.key.id)
                }
            }
        }

        return adjacency
    }
}
