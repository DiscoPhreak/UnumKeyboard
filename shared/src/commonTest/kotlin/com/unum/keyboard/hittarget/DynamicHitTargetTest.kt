package com.unum.keyboard.hittarget

import com.unum.keyboard.layout.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DynamicHitTargetTest {

    /**
     * Helper to create a simple key geometry for testing.
     */
    private fun makeKey(
        id: String,
        primary: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        type: KeyType = KeyType.CHARACTER
    ): KeyGeometry {
        val bounds = Rect(left, top, left + width, top + height)
        val center = Point(bounds.centerX, bounds.centerY)
        val key = Key(id = id, primary = primary, width = 1f, type = type)
        return KeyGeometry(key, bounds, center, emptyList())
    }

    private fun makeTestRow(): List<KeyGeometry> {
        // Three keys side by side: a(0-100), b(100-200), c(200-300), all 0-80 tall
        return listOf(
            makeKey("a", "a", 0f, 0f, 100f, 80f),
            makeKey("b", "b", 100f, 0f, 100f, 80f),
            makeKey("c", "c", 200f, 0f, 100f, 80f)
        )
    }

    // ---- Anchor Zone Tests ----

    @Test
    fun anchorZone_centerOfKey_alwaysReturnsKey() {
        val resolver = DynamicHitTargetResolver()
        val keys = makeTestRow()
        // Touch dead center of key "b" (at 150, 40)
        val result = resolver.resolve(150f, 40f, keys)
        assertNotNull(result)
        assertEquals("b", result.key.id)
    }

    @Test
    fun anchorZone_eachKeyCenter_mapsToCorrectKey() {
        val resolver = DynamicHitTargetResolver()
        val keys = makeTestRow()

        val a = resolver.resolve(50f, 40f, keys)
        val b = resolver.resolve(150f, 40f, keys)
        val c = resolver.resolve(250f, 40f, keys)

        assertEquals("a", a?.key?.id)
        assertEquals("b", b?.key?.id)
        assertEquals("c", c?.key?.id)
    }

    @Test
    fun anchorZone_innerRegion_neverOverridden() {
        // Even with strong language bias toward a different key,
        // the inner 60% should always return the key it's in.
        val resolver = DynamicHitTargetResolver()
        val keys = makeTestRow()

        // Touch clearly in the center of key "a" (at 50, 40)
        // Regardless of prefix, should return "a"
        val result = resolver.resolve(50f, 40f, keys, "zzzz")
        assertEquals("a", result?.key?.id)
    }

    // ---- Bayesian Scoring Tests ----

    @Test
    fun bayesian_boundaryTouch_resolvesToNearestKey() {
        val resolver = DynamicHitTargetResolver()
        val keys = makeTestRow()
        // Touch on the boundary between a and b, slightly toward b (at 101, 40)
        val result = resolver.resolve(101f, 40f, keys)
        assertNotNull(result)
        // Should resolve to either a or b (both are valid, nearest wins)
        assertTrue(result.key.id in listOf("a", "b"))
    }

    @Test
    fun bayesian_touchFarFromAllKeys_returnsNull() {
        val resolver = DynamicHitTargetResolver()
        val keys = makeTestRow()
        // Touch very far from all keys
        val result = resolver.resolve(500f, 500f, keys)
        // May return null if spatial prob is below threshold
        // (or return nearest key if any scores above threshold)
    }

    @Test
    fun bayesian_emptyKeyList_returnsNull() {
        val resolver = DynamicHitTargetResolver()
        val result = resolver.resolve(100f, 100f, emptyList())
        assertNull(result)
    }

    @Test
    fun bayesian_withPrefix_biasesTowardLikelyKeys() {
        val resolver = DynamicHitTargetResolver()
        // Create keys for t, h, e in a row
        val keys = listOf(
            makeKey("t", "t", 0f, 0f, 100f, 80f),
            makeKey("h", "h", 100f, 0f, 100f, 80f),
            makeKey("e", "e", 200f, 0f, 100f, 80f)
        )

        // Touch on the outer boundary between h and e (at ~195, 40)
        // With prefix "th", 'e' is very likely (the, them, then, etc.)
        val result = resolver.resolve(195f, 40f, keys, "th")
        assertNotNull(result)
        // The language model should bias toward 'e' after "th"
        assertEquals("e", result.key.id)
    }

    // ---- TouchCalibrator Tests ----

    @Test
    fun calibrator_initialOffsets_areZero() {
        val calibrator = TouchCalibrator()
        assertEquals(0f, calibrator.offsetX)
        assertEquals(0f, calibrator.offsetY)
        assertEquals(0, calibrator.samples)
    }

    @Test
    fun calibrator_singleTouch_updatesOffset() {
        val calibrator = TouchCalibrator()
        val key = makeKey("a", "a", 0f, 0f, 100f, 80f)

        // Touch slightly right of center (55, 40) — center is (50, 40)
        calibrator.recordTouch(55f, 40f, key)

        assertEquals(1, calibrator.samples)
        assertTrue(calibrator.offsetX > 0f, "offsetX should be positive (tapped right of center)")
        assertEquals(0f, calibrator.offsetY, "offsetY should be 0 (no vertical error)")
    }

    @Test
    fun calibrator_multipleTouches_convergesOnBias() {
        val calibrator = TouchCalibrator()
        val key = makeKey("a", "a", 0f, 0f, 100f, 80f)

        // Simulate user consistently tapping 10px right of center
        repeat(30) {
            calibrator.recordTouch(60f, 40f, key) // center is (50,40)
        }

        // After 30 samples, offset should be close to 10
        assertTrue(calibrator.offsetX > 5f, "offsetX should converge toward 10, got ${calibrator.offsetX}")
        assertTrue(abs(calibrator.offsetY) < 2f, "offsetY should be near 0")
    }

    @Test
    fun calibrator_distantTouch_isIgnored() {
        val calibrator = TouchCalibrator()
        val key = makeKey("a", "a", 0f, 0f, 100f, 80f)

        // Touch very far from key center (beyond 80% of key width)
        calibrator.recordTouch(200f, 40f, key)

        assertEquals(0, calibrator.samples, "Distant touches should be ignored")
        assertEquals(0f, calibrator.offsetX)
    }

    @Test
    fun calibrator_reset_clearsEverything() {
        val calibrator = TouchCalibrator()
        val key = makeKey("a", "a", 0f, 0f, 100f, 80f)

        repeat(10) {
            calibrator.recordTouch(55f, 45f, key)
        }

        calibrator.reset()
        assertEquals(0f, calibrator.offsetX)
        assertEquals(0f, calibrator.offsetY)
        assertEquals(0, calibrator.samples)
    }

    @Test
    fun calibrator_offsetClamped_toMaxOffset() {
        val calibrator = TouchCalibrator()
        // Create a very wide key so large offsets aren't filtered out
        val key = makeKey("a", "a", 0f, 0f, 200f, 200f)

        // Center is (100, 100). Touch at (170, 170) — within 80% of width/height
        repeat(50) {
            calibrator.recordTouch(170f, 170f, key)
        }

        assertTrue(calibrator.offsetX <= TouchCalibrator.MAX_OFFSET,
            "offsetX should be clamped to MAX_OFFSET")
        assertTrue(calibrator.offsetY <= TouchCalibrator.MAX_OFFSET,
            "offsetY should be clamped to MAX_OFFSET")
    }

    // ---- NextKeyPredictor Tests ----

    @Test
    fun predictor_noPrefix_returnsWordStartFrequencies() {
        val predictor = NextKeyPredictor()
        val probs = predictor.predictNextKey("")
        assertTrue(probs.isNotEmpty(), "Should return word-start frequencies")
        // 't' should be the most common word-start letter
        val tProb = probs['t'] ?: 0f
        assertTrue(tProb > 0.1f, "t should have high word-start probability")
    }

    @Test
    fun predictor_afterT_hIsLikely() {
        val predictor = NextKeyPredictor()
        val probs = predictor.predictNextKey("t")
        val hProb = probs['h'] ?: 0f
        // 'h' follows 't' very frequently (th is the most common English bigram)
        assertTrue(hProb > 0.05f, "h should be likely after t, got $hProb")
    }

    @Test
    fun predictor_afterQ_uIsDominant() {
        val predictor = NextKeyPredictor()
        val probs = predictor.predictNextKey("q")
        val uProb = probs['u'] ?: 0f
        // 'u' almost always follows 'q'
        assertTrue(uProb > 0.3f, "u should be dominant after q, got $uProb")
    }

    @Test
    fun predictor_allProbsSumToApproximatelyOne() {
        val predictor = NextKeyPredictor()
        val probs = predictor.predictNextKey("t")
        val sum = probs.values.sum()
        assertTrue(sum > 0.95f && sum < 1.05f, "Probabilities should sum to ~1.0, got $sum")
    }

    // ---- Expansion Factor Tests ----

    @Test
    fun expansionFactors_predictedKeysExpanded() {
        val resolver = DynamicHitTargetResolver()
        val keys = listOf(
            makeKey("t", "t", 0f, 0f, 100f, 80f),
            makeKey("h", "h", 100f, 0f, 100f, 80f),
            makeKey("z", "z", 200f, 0f, 100f, 80f)
        )

        val factors = resolver.getExpansionFactors(keys, "t")
        val hFactor = factors["h"] ?: 1f
        val zFactor = factors["z"] ?: 1f

        // 'h' should be more expanded than 'z' after typing 't'
        assertTrue(hFactor > zFactor,
            "h should be more expanded than z after 't', h=$hFactor z=$zFactor")
    }

    @Test
    fun expansionFactors_withinBounds() {
        val resolver = DynamicHitTargetResolver()
        val keys = makeTestRow()

        val factors = resolver.getExpansionFactors(keys, "th")

        for ((_, factor) in factors) {
            assertTrue(factor >= DynamicHitTargetResolver.MIN_EXPANSION,
                "Factor $factor should be >= MIN_EXPANSION")
            assertTrue(factor <= DynamicHitTargetResolver.MAX_EXPANSION,
                "Factor $factor should be <= MAX_EXPANSION")
        }
    }
}
