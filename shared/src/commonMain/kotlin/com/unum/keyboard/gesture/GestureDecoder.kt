package com.unum.keyboard.gesture

import com.unum.keyboard.layout.KeyGeometry
import com.unum.keyboard.layout.Point
import com.unum.keyboard.prediction.TrieDictionary
import com.unum.keyboard.prediction.WordCandidate
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Decodes a gesture swipe path into candidate words.
 *
 * Algorithm:
 * 1. Extract the key sequence from the gesture path (e.g., [h, e, l, o])
 * 2. Use the key sequence as a "template" to search the dictionary
 * 3. Score candidates based on:
 *    - How well the word's ideal path matches the actual swipe path
 *    - Word frequency from the dictionary
 *    - Sequence similarity (edit distance between path chars and word chars)
 *
 * The decoder handles:
 * - Skipped keys (user swipes past a key without dwelling)
 * - Extra keys (user's path wanders through nearby keys)
 * - The path characters are a LOSSY representation of intent
 */
class GestureDecoder(
    private val dictionary: TrieDictionary
) {
    /**
     * Decode a completed gesture path into ranked word candidates.
     *
     * @param path The completed gesture path
     * @param keys All key geometries on the current layout (for spatial scoring)
     * @param maxResults Maximum number of candidates to return
     * @return Ranked list of word candidates
     */
    fun decode(
        path: GesturePath,
        keys: List<KeyGeometry>,
        maxResults: Int = MAX_CANDIDATES
    ): List<GestureCandidate> {
        if (path.characters.isEmpty()) return emptyList()

        val keyMap = buildKeyMap(keys)
        val pathChars = path.characters

        // Strategy 1: Direct prefix search from the path's character sequence
        val directCandidates = searchFromSequence(pathChars, maxResults * 2)

        // Strategy 2: Search using first + last character anchors
        val anchorCandidates = if (pathChars.size >= 2) {
            searchWithAnchors(pathChars.first(), pathChars.last(), pathChars.size, maxResults * 2)
        } else {
            emptyList()
        }

        // Merge and score all candidates
        val allCandidates = (directCandidates + anchorCandidates)
            .distinctBy { it.word }
            .map { candidate ->
                val spatialScore = computeSpatialScore(candidate.word, path, keyMap)
                val sequenceScore = computeSequenceScore(candidate.word, pathChars)
                val frequencyScore = computeFrequencyScore(candidate.frequency)
                val lengthPenalty = computeLengthPenalty(candidate.word.length, pathChars.size)

                val totalScore = SPATIAL_WEIGHT * spatialScore +
                    SEQUENCE_WEIGHT * sequenceScore +
                    FREQUENCY_WEIGHT * frequencyScore +
                    LENGTH_WEIGHT * lengthPenalty

                GestureCandidate(
                    word = candidate.word,
                    score = totalScore,
                    frequency = candidate.frequency
                )
            }
            .sortedByDescending { it.score }
            .take(maxResults)

        return allCandidates
    }

    /**
     * Search dictionary using the path's character sequence as prefix variants.
     */
    private fun searchFromSequence(pathChars: List<Char>, maxResults: Int): List<WordCandidate> {
        val results = mutableListOf<WordCandidate>()

        // Try the full sequence as a prefix
        val prefix = pathChars.toCharArray().concatToString()
        results.addAll(dictionary.prefixSearch(prefix, maxResults))

        // Try subsequences (in case of extra keys in the path)
        if (pathChars.size >= 3) {
            // Try removing each character to handle stray path deviations
            for (i in 1 until pathChars.size - 1) {
                val shortened = pathChars.toMutableList().apply { removeAt(i) }
                val subPrefix = shortened.toCharArray().concatToString()
                results.addAll(dictionary.prefixSearch(subPrefix, maxResults / 2))
            }
        }

        return results.distinctBy { it.word }
    }

    /**
     * Search dictionary using first and last character as anchors.
     * Users almost always start and end on the correct keys.
     */
    private fun searchWithAnchors(
        firstChar: Char,
        lastChar: Char,
        approxLength: Int,
        maxResults: Int
    ): List<WordCandidate> {
        // Search words starting with the first character
        val prefix = firstChar.toString()
        val candidates = dictionary.prefixSearch(prefix, maxResults * 3)

        // Filter to words that end with the last character and are approximately the right length
        return candidates.filter { candidate ->
            candidate.word.isNotEmpty() &&
                candidate.word.last() == lastChar &&
                candidate.word.length in (approxLength - 2)..(approxLength + 3)
        }.take(maxResults)
    }

    /**
     * Compute spatial score: how well does the swipe path pass through the
     * ideal key centers for this word?
     */
    private fun computeSpatialScore(
        word: String,
        path: GesturePath,
        keyMap: Map<Char, Point>
    ): Float {
        if (path.points.isEmpty() || word.isEmpty()) return 0f

        // Build the ideal path (key centers for each letter in the word)
        val idealPoints = word.mapNotNull { keyMap[it] }
        if (idealPoints.isEmpty()) return 0f

        // For each ideal point, find the minimum distance to any path point
        var totalScore = 0f
        for (ideal in idealPoints) {
            var minDist = Float.MAX_VALUE
            for (pathPoint in path.points) {
                val dx = pathPoint.x - ideal.x
                val dy = pathPoint.y - ideal.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < minDist) minDist = dist
            }
            // Convert distance to a score using Gaussian falloff
            totalScore += exp(-minDist * minDist / (2f * SPATIAL_SIGMA * SPATIAL_SIGMA))
        }

        return totalScore / idealPoints.size
    }

    /**
     * Compute sequence similarity score between the path's key sequence
     * and the candidate word. Uses longest common subsequence.
     */
    private fun computeSequenceScore(word: String, pathChars: List<Char>): Float {
        if (word.isEmpty() || pathChars.isEmpty()) return 0f

        val lcsLength = longestCommonSubsequence(word.toList(), pathChars)
        val maxLen = maxOf(word.length, pathChars.size)
        return lcsLength.toFloat() / maxLen
    }

    /**
     * Score based on word frequency (log scale, normalized).
     */
    private fun computeFrequencyScore(frequency: Int): Float {
        if (frequency <= 0) return 0f
        // Log scale with normalization (assuming max freq ~100000)
        return (kotlin.math.ln(frequency.toFloat() + 1f) / kotlin.math.ln(100001f))
            .coerceIn(0f, 1f)
    }

    /**
     * Penalize candidates whose length is very different from the path key count.
     */
    private fun computeLengthPenalty(wordLength: Int, pathKeyCount: Int): Float {
        val diff = abs(wordLength - pathKeyCount)
        return when {
            diff == 0 -> 1f
            diff == 1 -> 0.8f
            diff == 2 -> 0.5f
            diff == 3 -> 0.2f
            else -> 0f
        }
    }

    /**
     * Build a map from character to key center point for spatial scoring.
     */
    private fun buildKeyMap(keys: List<KeyGeometry>): Map<Char, Point> {
        val map = mutableMapOf<Char, Point>()
        for (key in keys) {
            val char = key.key.primary.firstOrNull()?.lowercaseChar() ?: continue
            map[char] = key.center
        }
        return map
    }

    /**
     * Longest common subsequence length.
     */
    private fun longestCommonSubsequence(a: List<Char>, b: List<Char>): Int {
        val m = a.size
        val n = b.size
        // Space-optimized: only keep two rows
        var prev = IntArray(n + 1)
        var curr = IntArray(n + 1)

        for (i in 1..m) {
            for (j in 1..n) {
                curr[j] = if (a[i - 1] == b[j - 1]) {
                    prev[j - 1] + 1
                } else {
                    maxOf(prev[j], curr[j - 1])
                }
            }
            val temp = prev
            prev = curr
            curr = temp
            curr.fill(0)
        }

        return prev[n]
    }

    companion object {
        const val MAX_CANDIDATES = 5

        // Scoring weights (sum to 1.0)
        const val SPATIAL_WEIGHT = 0.35f
        const val SEQUENCE_WEIGHT = 0.25f
        const val FREQUENCY_WEIGHT = 0.25f
        const val LENGTH_WEIGHT = 0.15f

        /** Spatial Gaussian sigma in pixels — how forgiving the path matching is */
        const val SPATIAL_SIGMA = 60f
    }
}

/**
 * A gesture typing word candidate with its score.
 */
data class GestureCandidate(
    val word: String,
    val score: Float,
    val frequency: Int
)
