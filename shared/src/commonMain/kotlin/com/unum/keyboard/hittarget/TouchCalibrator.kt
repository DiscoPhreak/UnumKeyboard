package com.unum.keyboard.hittarget

import com.unum.keyboard.layout.KeyGeometry

/**
 * Learns per-user touch offset bias over time.
 *
 * Users systematically hit slightly left/right/up/down of key centers.
 * This calibrator tracks the average offset and adjusts the Gaussian
 * means to account for this bias.
 *
 * Uses exponential moving average to adapt to changing habits.
 */
class TouchCalibrator {
    /** Learned horizontal offset (positive = user tends to tap right of center) */
    var offsetX: Float = 0f
        private set

    /** Learned vertical offset (positive = user tends to tap below center) */
    var offsetY: Float = 0f
        private set

    private var sampleCount: Int = 0

    /**
     * Record a touch event and the key it was resolved to.
     * Updates the running average of touch offset.
     *
     * @param touchX Actual touch X coordinate
     * @param touchY Actual touch Y coordinate
     * @param key The key that was hit (after resolution)
     */
    fun recordTouch(touchX: Float, touchY: Float, key: KeyGeometry) {
        val errorX = touchX - key.center.x
        val errorY = touchY - key.center.y

        // Only learn from touches reasonably close to the key center
        // (distant touches might be actual different-key intentions)
        if (kotlin.math.abs(errorX) > key.bounds.width * 0.8f ||
            kotlin.math.abs(errorY) > key.bounds.height * 0.8f) {
            return
        }

        sampleCount++

        if (sampleCount < MIN_SAMPLES) {
            // Simple average for initial calibration
            offsetX = offsetX + (errorX - offsetX) / sampleCount
            offsetY = offsetY + (errorY - offsetY) / sampleCount
        } else {
            // Exponential moving average for ongoing calibration
            offsetX = EMA_ALPHA * errorX + (1f - EMA_ALPHA) * offsetX
            offsetY = EMA_ALPHA * errorY + (1f - EMA_ALPHA) * offsetY
        }

        // Clamp to reasonable bounds
        offsetX = offsetX.coerceIn(-MAX_OFFSET, MAX_OFFSET)
        offsetY = offsetY.coerceIn(-MAX_OFFSET, MAX_OFFSET)
    }

    /** Reset calibration (e.g., when user changes keyboard size) */
    fun reset() {
        offsetX = 0f
        offsetY = 0f
        sampleCount = 0
    }

    /** Number of touch samples recorded */
    val samples: Int get() = sampleCount

    companion object {
        /** Minimum samples before switching to EMA */
        const val MIN_SAMPLES = 20

        /** Exponential moving average alpha (smaller = more stable) */
        const val EMA_ALPHA = 0.02f

        /** Maximum allowed offset in pixels */
        const val MAX_OFFSET = 30f
    }
}
