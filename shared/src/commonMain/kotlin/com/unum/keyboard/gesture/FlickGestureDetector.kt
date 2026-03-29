package com.unum.keyboard.gesture

import com.unum.keyboard.layout.FlickDirection
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects flick gestures on keyboard keys.
 *
 * A flick is a short, fast swipe starting from a key press.
 * The detector tracks touch movement from the initial press point
 * and determines the cardinal direction of the flick.
 *
 * Design inspired by BlackBerry's press-and-flick for secondary characters.
 */
class FlickGestureDetector(
    /** Minimum distance in pixels to qualify as a flick */
    val minFlickDistance: Float = DEFAULT_MIN_FLICK_DISTANCE,
    /** Maximum distance before the gesture is considered a drag, not a flick */
    val maxFlickDistance: Float = DEFAULT_MAX_FLICK_DISTANCE,
    /** Maximum time in ms for a valid flick (too slow = not a flick) */
    val maxFlickDuration: Long = DEFAULT_MAX_FLICK_DURATION,
    /**
     * Minimum ratio of primary axis distance to secondary axis distance.
     * Higher = more strict directional requirement.
     * A value of 1.5 means the primary axis must be at least 1.5× the secondary.
     */
    val directionalityThreshold: Float = DEFAULT_DIRECTIONALITY_THRESHOLD
) {
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var startTime: Long = 0L
    private var isTracking: Boolean = false
    private var hasFired: Boolean = false

    /**
     * Call when a touch begins on a key.
     */
    fun onTouchStart(x: Float, y: Float, timestamp: Long) {
        startX = x
        startY = y
        startTime = timestamp
        isTracking = true
        hasFired = false
    }

    /**
     * Call on touch move to check for an in-progress flick.
     * Returns the detected direction once the threshold is crossed,
     * or NONE if no flick yet.
     */
    fun onTouchMove(x: Float, y: Float, timestamp: Long): FlickDirection {
        if (!isTracking || hasFired) return FlickDirection.NONE

        val elapsed = timestamp - startTime
        if (elapsed > maxFlickDuration) {
            // Took too long — this is a long press or drag, not a flick
            isTracking = false
            return FlickDirection.NONE
        }

        val dx = x - startX
        val dy = y - startY
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > maxFlickDistance) {
            isTracking = false
            return FlickDirection.NONE
        }

        if (distance >= minFlickDistance) {
            val direction = resolveDirection(dx, dy)
            if (direction != FlickDirection.NONE) {
                hasFired = true
                return direction
            }
        }

        return FlickDirection.NONE
    }

    /**
     * Call when touch ends. Returns the flick direction if one was detected
     * during the gesture, or NONE for a regular tap.
     */
    fun onTouchEnd(x: Float, y: Float, timestamp: Long): FlickResult {
        if (!isTracking) return FlickResult(FlickDirection.NONE, isTap = false)

        isTracking = false

        if (hasFired) {
            // Already detected during move
            return FlickResult(resolveDirection(x - startX, y - startY), isTap = false)
        }

        val elapsed = timestamp - startTime
        val dx = x - startX
        val dy = y - startY
        val distance = sqrt(dx * dx + dy * dy)

        // Check if this qualifies as a flick on release
        if (distance >= minFlickDistance && distance <= maxFlickDistance && elapsed <= maxFlickDuration) {
            val direction = resolveDirection(dx, dy)
            if (direction != FlickDirection.NONE) {
                return FlickResult(direction, isTap = false)
            }
        }

        // Not a flick — treat as a regular tap
        return FlickResult(FlickDirection.NONE, isTap = distance < minFlickDistance)
    }

    /**
     * Cancel tracking (e.g., on touch cancel event).
     */
    fun cancel() {
        isTracking = false
        hasFired = false
    }

    /**
     * Determine the cardinal direction from a displacement vector.
     * Returns NONE if the direction is ambiguous (diagonal).
     */
    fun resolveDirection(dx: Float, dy: Float): FlickDirection {
        val absDx = abs(dx)
        val absDy = abs(dy)

        // Require clear directionality — the primary axis must dominate
        return if (absDx > absDy * directionalityThreshold) {
            if (dx > 0) FlickDirection.RIGHT else FlickDirection.LEFT
        } else if (absDy > absDx * directionalityThreshold) {
            if (dy > 0) FlickDirection.DOWN else FlickDirection.UP
        } else {
            // Too diagonal — ambiguous
            FlickDirection.NONE
        }
    }

    /** Whether a flick gesture is currently being tracked */
    val tracking: Boolean get() = isTracking

    /** Whether a flick has already been detected in the current gesture */
    val fired: Boolean get() = hasFired

    companion object {
        /** Default minimum flick distance in dp-independent pixels */
        const val DEFAULT_MIN_FLICK_DISTANCE = 20f
        /** Default maximum flick distance */
        const val DEFAULT_MAX_FLICK_DISTANCE = 150f
        /** Default maximum flick duration in milliseconds */
        const val DEFAULT_MAX_FLICK_DURATION = 300L
        /** Default directionality threshold (1.5× = 56° cone) */
        const val DEFAULT_DIRECTIONALITY_THRESHOLD = 1.5f
    }
}

/**
 * Result of a flick gesture detection.
 */
data class FlickResult(
    val direction: FlickDirection,
    /** True if the gesture was a simple tap (no significant movement) */
    val isTap: Boolean
)
