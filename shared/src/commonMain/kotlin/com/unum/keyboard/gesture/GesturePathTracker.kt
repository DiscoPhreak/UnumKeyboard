package com.unum.keyboard.gesture

import com.unum.keyboard.layout.KeyGeometry
import com.unum.keyboard.layout.KeyType
import com.unum.keyboard.layout.Point
import kotlin.math.sqrt

/**
 * Tracks the touch path during a swipe/gesture typing session.
 *
 * Records sampled points along the swipe path, identifies which keys
 * the path passes through, and deduplicates consecutive visits to the
 * same key to produce a key sequence.
 *
 * Example: swiping from H → E → L → L → O produces the key sequence [h, e, l, o]
 * (note: the duplicate 'l' is kept because the path left and returned to 'l')
 */
class GesturePathTracker {

    /** Raw sampled points along the swipe path */
    private val _pathPoints = mutableListOf<PathPoint>()
    val pathPoints: List<PathPoint> get() = _pathPoints

    /** Sequence of keys the path has crossed through (deduplicated consecutive) */
    private val _keySequence = mutableListOf<KeyGeometry>()
    val keySequence: List<KeyGeometry> get() = _keySequence

    /** The last key the path was on (for deduplication) */
    private var lastKeyId: String? = null

    /** Whether the tracker is actively recording */
    var isActive: Boolean = false
        private set

    /** Total path length in pixels */
    var pathLength: Float = 0f
        private set

    /** Timestamp when tracking started */
    var startTime: Long = 0L
        private set

    /**
     * Start tracking a new gesture.
     */
    fun start(x: Float, y: Float, timestamp: Long) {
        reset()
        isActive = true
        startTime = timestamp
        addPoint(x, y, timestamp)
    }

    /**
     * Add a point to the path during the swipe.
     * Samples are filtered to avoid redundant close points.
     */
    fun addPoint(x: Float, y: Float, timestamp: Long) {
        if (!isActive) return

        val last = _pathPoints.lastOrNull()
        if (last != null) {
            val dx = x - last.x
            val dy = y - last.y
            val dist = sqrt(dx * dx + dy * dy)

            // Skip points that are too close together (reduce noise)
            if (dist < MIN_SAMPLE_DISTANCE) return

            pathLength += dist
        }

        _pathPoints.add(PathPoint(x, y, timestamp))
    }

    /**
     * Update the key that the current touch point is over.
     * Builds up the key sequence as the path crosses different keys.
     */
    fun updateCurrentKey(key: KeyGeometry?) {
        if (!isActive || key == null) return
        if (key.key.type != KeyType.CHARACTER) return

        if (key.key.id != lastKeyId) {
            _keySequence.add(key)
            lastKeyId = key.key.id
        }
    }

    /**
     * End the gesture and return the final key sequence as characters.
     */
    fun finish(): GesturePath {
        isActive = false
        val chars = _keySequence.mapNotNull { geo ->
            geo.key.primary.firstOrNull()?.lowercaseChar()
        }
        return GesturePath(
            characters = chars,
            points = _pathPoints.toList(),
            keySequence = _keySequence.toList(),
            totalLength = pathLength,
            duration = if (_pathPoints.size >= 2) {
                _pathPoints.last().timestamp - _pathPoints.first().timestamp
            } else 0L
        )
    }

    /**
     * Reset the tracker for a new gesture.
     */
    fun reset() {
        _pathPoints.clear()
        _keySequence.clear()
        lastKeyId = null
        isActive = false
        pathLength = 0f
        startTime = 0L
    }

    /**
     * Check if the gesture has crossed enough keys to be a valid swipe.
     */
    fun isValidGesture(): Boolean {
        return _keySequence.size >= MIN_KEYS_FOR_GESTURE && pathLength >= MIN_PATH_LENGTH
    }

    companion object {
        /** Minimum pixel distance between sampled points */
        const val MIN_SAMPLE_DISTANCE = 5f

        /** Minimum number of distinct keys crossed for a valid gesture */
        const val MIN_KEYS_FOR_GESTURE = 2

        /** Minimum path length in pixels for a valid gesture */
        const val MIN_PATH_LENGTH = 50f
    }
}

/**
 * A single sampled point on the gesture path.
 */
data class PathPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long
)

/**
 * The completed gesture path data.
 */
data class GesturePath(
    /** Characters from the key sequence (deduplicated consecutive) */
    val characters: List<Char>,
    /** Raw sampled path points */
    val points: List<PathPoint>,
    /** Key geometries crossed (in order) */
    val keySequence: List<KeyGeometry>,
    /** Total path length in pixels */
    val totalLength: Float,
    /** Duration of the gesture in milliseconds */
    val duration: Long
) {
    /** The key sequence as a string */
    val characterString: String get() = characters.toCharArray().concatToString()
}
