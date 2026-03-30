package com.unum.keyboard.gesture

import com.unum.keyboard.layout.FlickDirection
import com.unum.keyboard.layout.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlickGestureDetectorTest {

    // ---- Direction Resolution Tests ----

    @Test
    fun resolveDirection_upFlick() {
        val detector = FlickGestureDetector()
        // Negative dy = upward movement
        assertEquals(FlickDirection.UP, detector.resolveDirection(0f, -50f))
    }

    @Test
    fun resolveDirection_downFlick() {
        val detector = FlickGestureDetector()
        assertEquals(FlickDirection.DOWN, detector.resolveDirection(0f, 50f))
    }

    @Test
    fun resolveDirection_leftFlick() {
        val detector = FlickGestureDetector()
        assertEquals(FlickDirection.LEFT, detector.resolveDirection(-50f, 0f))
    }

    @Test
    fun resolveDirection_rightFlick() {
        val detector = FlickGestureDetector()
        assertEquals(FlickDirection.RIGHT, detector.resolveDirection(50f, 0f))
    }

    @Test
    fun resolveDirection_diagonal_returnsNone() {
        val detector = FlickGestureDetector()
        // Equal dx and dy — too diagonal to be a clear direction
        assertEquals(FlickDirection.NONE, detector.resolveDirection(50f, 50f))
    }

    @Test
    fun resolveDirection_slightlyDiagonal_resolves() {
        val detector = FlickGestureDetector()
        // dx=50, dy=20 → dx/dy = 2.5 > 1.5 threshold → RIGHT
        assertEquals(FlickDirection.RIGHT, detector.resolveDirection(50f, 20f))
    }

    @Test
    fun resolveDirection_zero_returnsNone() {
        val detector = FlickGestureDetector()
        assertEquals(FlickDirection.NONE, detector.resolveDirection(0f, 0f))
    }

    // ---- Tap Detection Tests ----

    @Test
    fun tapGesture_shortDistance_isTap() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)
        val result = detector.onTouchEnd(102f, 101f, 50L) // 2px movement
        assertTrue(result.isTap, "Small movement should be detected as tap")
        assertEquals(FlickDirection.NONE, result.direction)
    }

    @Test
    fun tapGesture_noMovement_isTap() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)
        val result = detector.onTouchEnd(100f, 100f, 100L)
        assertTrue(result.isTap)
    }

    // ---- Flick Detection Tests ----

    @Test
    fun flickGesture_upFlick_detected() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)
        val result = detector.onTouchEnd(100f, 60f, 100L) // 40px upward in 100ms
        assertFalse(result.isTap)
        assertEquals(FlickDirection.UP, result.direction)
    }

    @Test
    fun flickGesture_rightFlick_detected() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)
        val result = detector.onTouchEnd(140f, 100f, 150L) // 40px rightward
        assertEquals(FlickDirection.RIGHT, result.direction)
        assertFalse(result.isTap)
    }

    @Test
    fun flickGesture_tooSlow_notAFlick() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)
        // 40px movement but took 500ms (over 300ms limit)
        val result = detector.onTouchEnd(100f, 60f, 500L)
        // Should not be a flick, and not a tap (too far from start)
        assertEquals(FlickDirection.NONE, result.direction)
        assertFalse(result.isTap)
    }

    @Test
    fun flickGesture_tooFar_notAFlick() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)
        // 200px movement (over 150px max distance)
        val result = detector.onTouchEnd(100f, -100f, 100L)
        assertEquals(FlickDirection.NONE, result.direction)
    }

    @Test
    fun flickGesture_exactMinDistance_isFlick() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)
        // Exactly 20px upward (min flick distance)
        val result = detector.onTouchEnd(100f, 80f, 100L)
        assertEquals(FlickDirection.UP, result.direction)
    }

    // ---- Mid-Gesture Flick Detection ----

    @Test
    fun flickDuringMove_detectedImmediately() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)

        // Simulate gradual upward movement
        assertEquals(FlickDirection.NONE, detector.onTouchMove(100f, 95f, 20L))  // 5px - too short
        assertEquals(FlickDirection.NONE, detector.onTouchMove(100f, 90f, 40L))  // 10px - still short
        val dir = detector.onTouchMove(100f, 75f, 60L) // 25px - should trigger
        assertEquals(FlickDirection.UP, dir)
    }

    @Test
    fun flickDuringMove_onlyFiresOnce() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)

        // First detection
        detector.onTouchMove(100f, 75f, 60L) // triggers flick

        // Subsequent moves should return NONE (already fired)
        assertEquals(FlickDirection.NONE, detector.onTouchMove(100f, 65f, 80L))
        assertTrue(detector.fired)
    }

    // ---- Cancel and State Tests ----

    @Test
    fun cancel_stopsTracking() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)
        assertTrue(detector.tracking)

        detector.cancel()
        assertFalse(detector.tracking)
        assertFalse(detector.fired)
    }

    @Test
    fun multipleGestures_stateResets() {
        val detector = FlickGestureDetector()

        // First gesture — flick up
        detector.onTouchStart(100f, 100f, 0L)
        val result1 = detector.onTouchEnd(100f, 60f, 100L)
        assertEquals(FlickDirection.UP, result1.direction)

        // Second gesture — tap
        detector.onTouchStart(200f, 200f, 500L)
        val result2 = detector.onTouchEnd(201f, 201f, 550L)
        assertTrue(result2.isTap)
    }

    @Test
    fun moveAfterTimeout_stopsTracking() {
        val detector = FlickGestureDetector()
        detector.onTouchStart(100f, 100f, 0L)

        // Move after timeout (400ms > 300ms)
        val dir = detector.onTouchMove(100f, 60f, 400L)
        assertEquals(FlickDirection.NONE, dir)
        assertFalse(detector.tracking, "Should stop tracking after timeout")
    }

    // ---- Key FlickChar Integration Tests ----

    @Test
    fun keyFlickChar_up_returnsMapping() {
        val key = Key("q", "q", flickUp = "1", flickRight = "!")
        assertEquals("1", key.flickChar(FlickDirection.UP))
        assertEquals("!", key.flickChar(FlickDirection.RIGHT))
        assertEquals(null, key.flickChar(FlickDirection.DOWN))
        assertEquals(null, key.flickChar(FlickDirection.LEFT))
        assertEquals(null, key.flickChar(FlickDirection.NONE))
    }

    @Test
    fun keyFlickChar_noMappings_returnsNull() {
        val key = Key("a", "a")
        assertEquals(null, key.flickChar(FlickDirection.UP))
        assertEquals(null, key.flickChar(FlickDirection.DOWN))
    }

    // ---- Custom Threshold Tests ----

    @Test
    fun customThresholds_work() {
        val detector = FlickGestureDetector(
            minFlickDistance = 10f,
            maxFlickDistance = 50f,
            maxFlickDuration = 200L,
            directionalityThreshold = 2.0f
        )

        detector.onTouchStart(100f, 100f, 0L)
        // 15px up in 50ms with custom min of 10px
        val result = detector.onTouchEnd(100f, 85f, 50L)
        assertEquals(FlickDirection.UP, result.direction)
    }

    @Test
    fun strictDirectionality_rejectsMildDiagonal() {
        val detector = FlickGestureDetector(directionalityThreshold = 2.0f)
        // dx=50, dy=30 → ratio = 1.67 < 2.0 threshold → NONE
        assertEquals(FlickDirection.NONE, detector.resolveDirection(50f, 30f))
    }
}
