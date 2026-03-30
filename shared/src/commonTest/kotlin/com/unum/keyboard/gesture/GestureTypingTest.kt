package com.unum.keyboard.gesture

import com.unum.keyboard.layout.*
import com.unum.keyboard.prediction.TrieDictionary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GestureTypingTest {

    // Helper to create a key geometry
    private fun makeKey(
        id: String,
        primary: String,
        left: Float,
        top: Float,
        width: Float = 100f,
        height: Float = 80f
    ): KeyGeometry {
        val bounds = Rect(left, top, left + width, top + height)
        val center = Point(bounds.centerX, bounds.centerY)
        val key = Key(id = id, primary = primary, width = 1f, type = KeyType.CHARACTER)
        return KeyGeometry(key, bounds, center, emptyList())
    }

    // Build a simple single-row keyboard: h(0-100), e(100-200), l(200-300), o(300-400)
    private fun makeHelloKeys(): List<KeyGeometry> = listOf(
        makeKey("h", "h", 0f, 0f),
        makeKey("e", "e", 100f, 0f),
        makeKey("l", "l", 200f, 0f),
        makeKey("o", "o", 300f, 0f)
    )

    // Build a dictionary with test words
    private fun makeTestDictionary(): TrieDictionary {
        val dict = TrieDictionary()
        dict.insert("hello", 50000)
        dict.insert("help", 30000)
        dict.insert("hero", 20000)
        dict.insert("helo", 5000)
        dict.insert("the", 100000)
        dict.insert("there", 40000)
        dict.insert("them", 35000)
        dict.insert("world", 25000)
        dict.insert("word", 15000)
        return dict
    }

    // ---- GesturePathTracker Tests ----

    @Test
    fun tracker_start_initializesState() {
        val tracker = GesturePathTracker()
        tracker.start(50f, 50f, 0L)

        assertTrue(tracker.isActive)
        assertEquals(1, tracker.pathPoints.size)
        assertEquals(0f, tracker.pathLength)
    }

    @Test
    fun tracker_addPoints_buildsPath() {
        val tracker = GesturePathTracker()
        tracker.start(50f, 40f, 0L)
        tracker.addPoint(150f, 40f, 50L)  // 100px right
        tracker.addPoint(250f, 40f, 100L) // another 100px right

        assertEquals(3, tracker.pathPoints.size)
        assertTrue(tracker.pathLength > 150f, "Path length should be ~200px, got ${tracker.pathLength}")
    }

    @Test
    fun tracker_closePoints_areFiltered() {
        val tracker = GesturePathTracker()
        tracker.start(50f, 40f, 0L)
        tracker.addPoint(51f, 40f, 10L)  // only 1px — below MIN_SAMPLE_DISTANCE
        tracker.addPoint(52f, 40f, 20L)  // still too close

        assertEquals(1, tracker.pathPoints.size, "Close points should be filtered out")
    }

    @Test
    fun tracker_updateCurrentKey_buildsSequence() {
        val tracker = GesturePathTracker()
        val keys = makeHelloKeys()

        tracker.start(50f, 40f, 0L)
        tracker.updateCurrentKey(keys[0]) // h
        tracker.updateCurrentKey(keys[1]) // e
        tracker.updateCurrentKey(keys[2]) // l
        tracker.updateCurrentKey(keys[3]) // o

        assertEquals(4, tracker.keySequence.size)
        assertEquals("h", tracker.keySequence[0].key.primary)
        assertEquals("o", tracker.keySequence[3].key.primary)
    }

    @Test
    fun tracker_consecutiveSameKey_deduplicated() {
        val tracker = GesturePathTracker()
        val keys = makeHelloKeys()

        tracker.start(50f, 40f, 0L)
        tracker.updateCurrentKey(keys[0]) // h
        tracker.updateCurrentKey(keys[0]) // h again — should be ignored
        tracker.updateCurrentKey(keys[0]) // h again — should be ignored
        tracker.updateCurrentKey(keys[1]) // e

        assertEquals(2, tracker.keySequence.size, "Consecutive same key should be deduplicated")
    }

    @Test
    fun tracker_finish_returnsGesturePath() {
        val tracker = GesturePathTracker()
        val keys = makeHelloKeys()

        tracker.start(50f, 40f, 0L)
        tracker.addPoint(150f, 40f, 50L)
        tracker.addPoint(250f, 40f, 100L)
        tracker.addPoint(350f, 40f, 150L)

        tracker.updateCurrentKey(keys[0]) // h
        tracker.updateCurrentKey(keys[1]) // e
        tracker.updateCurrentKey(keys[2]) // l
        tracker.updateCurrentKey(keys[3]) // o

        val path = tracker.finish()

        assertFalse(tracker.isActive)
        assertEquals(listOf('h', 'e', 'l', 'o'), path.characters)
        assertEquals("helo", path.characterString)
        assertEquals(4, path.keySequence.size)
        assertTrue(path.totalLength > 200f)
        assertEquals(150L, path.duration)
    }

    @Test
    fun tracker_isValidGesture_requiresMinKeysAndLength() {
        val tracker = GesturePathTracker()
        val keys = makeHelloKeys()

        tracker.start(50f, 40f, 0L)
        tracker.updateCurrentKey(keys[0])
        assertFalse(tracker.isValidGesture(), "1 key should not be valid")

        tracker.addPoint(150f, 40f, 50L)
        tracker.updateCurrentKey(keys[1])
        assertTrue(tracker.isValidGesture(), "2 keys + 100px should be valid")
    }

    @Test
    fun tracker_reset_clearsAll() {
        val tracker = GesturePathTracker()
        val keys = makeHelloKeys()

        tracker.start(50f, 40f, 0L)
        tracker.addPoint(150f, 40f, 50L)
        tracker.updateCurrentKey(keys[0])
        tracker.updateCurrentKey(keys[1])

        tracker.reset()

        assertFalse(tracker.isActive)
        assertEquals(0, tracker.pathPoints.size)
        assertEquals(0, tracker.keySequence.size)
        assertEquals(0f, tracker.pathLength)
    }

    @Test
    fun tracker_nonCharacterKeys_ignored() {
        val tracker = GesturePathTracker()
        val charKey = makeKey("a", "a", 0f, 0f)
        val bounds = Rect(100f, 0f, 200f, 80f)
        val shiftKey = KeyGeometry(
            Key("shift", "⇧", type = KeyType.SHIFT, width = 1.5f),
            bounds, Point(150f, 40f), emptyList()
        )

        tracker.start(50f, 40f, 0L)
        tracker.updateCurrentKey(charKey)
        tracker.updateCurrentKey(shiftKey) // should be ignored

        assertEquals(1, tracker.keySequence.size, "Non-character keys should be ignored")
    }

    // ---- GestureDecoder Tests ----

    @Test
    fun decoder_exactPath_findsWord() {
        val dict = makeTestDictionary()
        val decoder = GestureDecoder(dict)
        val keys = makeHelloKeys()

        // Simulate a path through h, e, l, o
        val path = GesturePath(
            characters = listOf('h', 'e', 'l', 'o'),
            points = listOf(
                PathPoint(50f, 40f, 0L),
                PathPoint(150f, 40f, 50L),
                PathPoint(250f, 40f, 100L),
                PathPoint(350f, 40f, 150L)
            ),
            keySequence = keys,
            totalLength = 300f,
            duration = 150L
        )

        val candidates = decoder.decode(path, keys)

        assertTrue(candidates.isNotEmpty(), "Should find candidates for h-e-l-o path")
        // "hello" or "helo" should be in the candidates
        val words = candidates.map { it.word }
        assertTrue(
            words.any { it == "hello" || it == "helo" || it == "help" || it == "hero" },
            "Should find hello/helo/help/hero, got: $words"
        )
    }

    @Test
    fun decoder_emptyPath_returnsEmpty() {
        val dict = makeTestDictionary()
        val decoder = GestureDecoder(dict)
        val keys = makeHelloKeys()

        val path = GesturePath(
            characters = emptyList(),
            points = emptyList(),
            keySequence = emptyList(),
            totalLength = 0f,
            duration = 0L
        )

        val candidates = decoder.decode(path, keys)
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun decoder_candidatesAreRanked() {
        val dict = makeTestDictionary()
        val decoder = GestureDecoder(dict)
        val keys = makeHelloKeys()

        val path = GesturePath(
            characters = listOf('h', 'e', 'l', 'o'),
            points = listOf(
                PathPoint(50f, 40f, 0L),
                PathPoint(150f, 40f, 50L),
                PathPoint(250f, 40f, 100L),
                PathPoint(350f, 40f, 150L)
            ),
            keySequence = keys,
            totalLength = 300f,
            duration = 150L
        )

        val candidates = decoder.decode(path, keys)
        if (candidates.size >= 2) {
            assertTrue(candidates[0].score >= candidates[1].score,
                "Candidates should be ranked by score descending")
        }
    }

    @Test
    fun decoder_limitsResults() {
        val dict = makeTestDictionary()
        val decoder = GestureDecoder(dict)
        val keys = makeHelloKeys()

        val path = GesturePath(
            characters = listOf('h', 'e'),
            points = listOf(
                PathPoint(50f, 40f, 0L),
                PathPoint(150f, 40f, 50L)
            ),
            keySequence = keys.take(2),
            totalLength = 100f,
            duration = 50L
        )

        val candidates = decoder.decode(path, keys, maxResults = 3)
        assertTrue(candidates.size <= 3, "Should limit to maxResults")
    }

    @Test
    fun decoder_scoringWeightsSum() {
        val totalWeight = GestureDecoder.SPATIAL_WEIGHT +
            GestureDecoder.SEQUENCE_WEIGHT +
            GestureDecoder.FREQUENCY_WEIGHT +
            GestureDecoder.LENGTH_WEIGHT
        assertEquals(1.0f, totalWeight, "Scoring weights should sum to 1.0")
    }

    // ---- Integration: Tracker → Decoder ----

    @Test
    fun integration_trackerToDecoder_flow() {
        val dict = makeTestDictionary()
        val decoder = GestureDecoder(dict)
        val keys = makeHelloKeys()
        val tracker = GesturePathTracker()

        // Simulate a swipe from h → e → l → o
        tracker.start(50f, 40f, 0L)
        tracker.updateCurrentKey(keys[0])

        tracker.addPoint(150f, 40f, 50L)
        tracker.updateCurrentKey(keys[1])

        tracker.addPoint(250f, 40f, 100L)
        tracker.updateCurrentKey(keys[2])

        tracker.addPoint(350f, 40f, 150L)
        tracker.updateCurrentKey(keys[3])

        assertTrue(tracker.isValidGesture())

        val path = tracker.finish()
        val candidates = decoder.decode(path, keys)

        assertTrue(candidates.isNotEmpty(), "End-to-end gesture should produce candidates")
    }
}
