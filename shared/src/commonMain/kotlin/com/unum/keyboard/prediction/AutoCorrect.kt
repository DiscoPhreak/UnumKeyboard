package com.unum.keyboard.prediction

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min

/**
 * Autocorrect engine using symmetric delete distance for fast lookups,
 * Damerau-Levenshtein distance weighted by keyboard adjacency for scoring,
 * confidence-based auto-apply decisions, capitalization preservation, and a user block list.
 */
class AutoCorrect(
    private val dictionary: TrieDictionary,
    private val adjacencyMap: Map<String, List<String>> = emptyMap()
) {
    /** Symmetric delete index: delete variant → list of source dictionary words */
    private var deleteIndex: Map<String, List<String>> = emptyMap()
    private var indexed = false
    private var maxFrequency = 1

    /** Words the user has rejected corrections for */
    private val blockList = mutableSetOf<String>()

    /**
     * Build the symmetric delete index for fast correction lookups.
     * Call once after dictionary is loaded. Indexes all delete variants
     * within edit distance 2 for O(n) query-time lookups.
     */
    fun buildIndex() {
        val index = mutableMapOf<String, MutableList<String>>()
        var maxFreq = 1

        dictionary.forEachWord { word, frequency ->
            if (frequency > maxFreq) maxFreq = frequency

            // Index the word itself (catches insertion errors in input)
            index.getOrPut(word) { mutableListOf() }.add(word)

            // Index all delete-1 variants
            val deletes1 = generateDeletes(word)
            for (d1 in deletes1) {
                index.getOrPut(d1) { mutableListOf() }.add(word)
            }

            // Index all delete-2 variants
            for (d1 in deletes1) {
                for (d2 in generateDeletes(d1)) {
                    index.getOrPut(d2) { mutableListOf() }.add(word)
                }
            }
        }

        deleteIndex = index
        maxFrequency = maxFreq
        indexed = true
    }

    /**
     * Find corrections for a mistyped word (used for suggestion bar during typing).
     * Returns candidates within edit distance 1-2, weighted by adjacency.
     */
    fun corrections(
        input: String,
        maxResults: Int = 5,
        maxEditDistance: Int = 2
    ): List<CorrectionCandidate> {
        val inputLower = input.lowercase()
        if (dictionary.contains(inputLower)) return emptyList()

        val candidates = if (indexed) {
            correctionsFromIndex(inputLower, maxEditDistance)
        } else {
            correctionsFromBruteForce(inputLower, maxEditDistance)
        }

        candidates.sortWith(
            compareBy<CorrectionCandidate> { it.editDistance }
                .thenBy { it.weightedCost }
                .thenByDescending { it.frequency }
        )

        return candidates.take(maxResults)
    }

    /**
     * Evaluate a correction for commit-time auto-apply decisions.
     * Returns an AutoCorrectionResult with confidence scoring and auto-apply recommendation.
     */
    fun evaluateCorrection(
        input: String,
        userDictionaryContains: (String) -> Boolean = { false }
    ): AutoCorrectionResult {
        val inputLower = input.lowercase()

        // Too short to correct
        if (input.length <= 1) {
            return AutoCorrectionResult(input, input, 0f, false, emptyList())
        }

        // Blocked by user
        if (inputLower in blockList) {
            return AutoCorrectionResult(input, input, 0f, false, emptyList())
        }

        // Already a valid word — no auto-apply (still return candidates for suggestion bar)
        val isValidWord = dictionary.contains(inputLower) || userDictionaryContains(inputLower)

        val candidates = corrections(input, maxResults = 5)
        if (candidates.isEmpty()) {
            return AutoCorrectionResult(input, input, 0f, false, emptyList())
        }

        val topCandidate = candidates[0]
        val confidence = computeConfidence(topCandidate, candidates)
        val correctedWithCasing = applyCapitalization(topCandidate.word, input)

        val shouldAutoApply = !isValidWord
            && confidence >= AUTO_APPLY_THRESHOLD
            && topCandidate.editDistance <= 1

        return AutoCorrectionResult(
            original = input,
            corrected = correctedWithCasing,
            confidence = confidence,
            shouldAutoApply = shouldAutoApply,
            candidates = candidates
        )
    }

    // ---- Block list ----

    fun addToBlockList(word: String) { blockList.add(word.lowercase()) }
    fun removeFromBlockList(word: String) { blockList.remove(word.lowercase()) }
    fun isBlocked(word: String): Boolean = word.lowercase() in blockList

    fun serializeBlockList(): String = blockList.joinToString("\n")
    fun deserializeBlockList(data: String) {
        blockList.clear()
        data.lineSequence().filter { it.isNotBlank() }.forEach { blockList.add(it.trim()) }
    }

    // ---- Private implementation ----

    /**
     * Fast correction lookup using the symmetric delete index.
     */
    private fun correctionsFromIndex(
        inputLower: String,
        maxEditDistance: Int
    ): MutableList<CorrectionCandidate> {
        val candidates = mutableListOf<CorrectionCandidate>()
        val seen = mutableSetOf<String>()

        // Look up the input itself in the index (catches insertion errors)
        checkIndexCandidates(inputLower, inputLower, seen, candidates, maxEditDistance)

        // Look up all delete-1 variants of the input
        val deletes1 = generateDeletes(inputLower)
        for (d1 in deletes1) {
            checkIndexCandidates(d1, inputLower, seen, candidates, maxEditDistance)
        }

        // Look up all delete-2 variants
        if (maxEditDistance >= 2) {
            for (d1 in deletes1) {
                for (d2 in generateDeletes(d1)) {
                    checkIndexCandidates(d2, inputLower, seen, candidates, maxEditDistance)
                }
            }
        }

        return candidates
    }

    private fun checkIndexCandidates(
        lookupKey: String,
        inputLower: String,
        seen: MutableSet<String>,
        candidates: MutableList<CorrectionCandidate>,
        maxEditDistance: Int
    ) {
        val words = deleteIndex[lookupKey] ?: return
        for (word in words) {
            if (word == inputLower || word in seen) continue
            seen.add(word)

            val distance = damerauLevenshteinDistance(inputLower, word)
            if (distance in 1..maxEditDistance) {
                val cost = adjacencyWeightedCost(inputLower, word)
                val freq = dictionary.getFrequency(word)
                candidates.add(CorrectionCandidate(word, distance, cost, freq))
            }
        }
    }

    /**
     * Brute-force correction (fallback when index not built).
     */
    private fun correctionsFromBruteForce(
        inputLower: String,
        maxEditDistance: Int
    ): MutableList<CorrectionCandidate> {
        val candidates = mutableListOf<CorrectionCandidate>()
        val edits1 = generateEdits(inputLower)

        for (edit in edits1) {
            if (dictionary.contains(edit)) {
                val cost = adjacencyWeightedCost(inputLower, edit)
                val freq = dictionary.getFrequency(edit)
                candidates.add(CorrectionCandidate(edit, 1, cost, freq))
            }
        }

        if (candidates.size < 10 && maxEditDistance >= 2) {
            val seen = edits1.toMutableSet()
            for (edit1 in edits1) {
                for (edit2 in generateEdits(edit1)) {
                    if (edit2 in seen) continue
                    seen.add(edit2)
                    if (dictionary.contains(edit2)) {
                        val cost = adjacencyWeightedCost(inputLower, edit2)
                        val freq = dictionary.getFrequency(edit2)
                        candidates.add(CorrectionCandidate(edit2, 2, cost, freq))
                    }
                }
            }
        }

        return candidates
    }

    /**
     * Compute confidence score (0.0-1.0) for the top correction candidate.
     */
    private fun computeConfidence(
        top: CorrectionCandidate,
        allCandidates: List<CorrectionCandidate>
    ): Float {
        // Factor 1: Edit distance closeness (0.30 weight)
        val distanceScore = if (top.editDistance == 1) 1.0f else 0.3f

        // Factor 2: Adjacency score (0.25 weight) — how many errors are adjacent key typos
        val adjacencyScore = if (top.editDistance > 0) {
            ((top.editDistance.toFloat() - top.weightedCost) / top.editDistance.toFloat())
                .coerceIn(0f, 1f)
        } else {
            1f
        }

        // Factor 3: Frequency strength (0.25 weight)
        val freqScore = if (maxFrequency > 1) {
            (ln((top.frequency + 1).toDouble()) / ln((maxFrequency + 1).toDouble())).toFloat()
        } else {
            0.5f
        }

        // Factor 4: Uniqueness (0.20 weight) — fewer candidates at same distance = more confident
        val sameDistCount = allCandidates.count { it.editDistance == top.editDistance }
        val uniquenessScore = when {
            sameDistCount == 1 -> 1.0f
            sameDistCount == 2 -> 0.5f
            else -> 0.3f
        }

        return WEIGHT_DISTANCE * distanceScore +
                WEIGHT_ADJACENCY * adjacencyScore +
                WEIGHT_FREQUENCY * freqScore +
                WEIGHT_UNIQUENESS * uniquenessScore
    }

    /**
     * Preserve the capitalization pattern of the original input on the corrected word.
     */
    private fun applyCapitalization(corrected: String, original: String): String {
        return when {
            original.length >= 2 && original.all { it.isUpperCase() || !it.isLetter() } ->
                corrected.uppercase()
            original.firstOrNull()?.isUpperCase() == true ->
                corrected.replaceFirstChar { it.uppercaseChar() }
            else -> corrected
        }
    }

    /**
     * Generate only deletion variants of a word (for symmetric delete indexing).
     */
    private fun generateDeletes(word: String): Set<String> {
        val results = mutableSetOf<String>()
        for (i in word.indices) {
            results.add(word.removeRange(i, i + 1))
        }
        return results
    }

    /**
     * Generate all strings within edit distance 1 of the input (brute-force fallback).
     * Operations: deletion, insertion, substitution, transposition (Damerau)
     */
    private fun generateEdits(word: String): Set<String> {
        val results = mutableSetOf<String>()
        val chars = ALPHABET

        // Deletions
        for (i in word.indices) {
            results.add(word.removeRange(i, i + 1))
        }
        // Insertions
        for (i in 0..word.length) {
            for (c in chars) {
                results.add(word.substring(0, i) + c + word.substring(i))
            }
        }
        // Substitutions
        for (i in word.indices) {
            for (c in chars) {
                if (c != word[i]) {
                    results.add(word.substring(0, i) + c + word.substring(i + 1))
                }
            }
        }
        // Transpositions
        for (i in 0 until word.length - 1) {
            results.add(
                word.substring(0, i) + word[i + 1] + word[i] + word.substring(i + 2)
            )
        }

        return results
    }

    /**
     * Compute true Damerau-Levenshtein distance between two strings.
     */
    private fun damerauLevenshteinDistance(a: String, b: String): Int {
        val lenA = a.length
        val lenB = b.length

        if (lenA == 0) return lenB
        if (lenB == 0) return lenA

        val d = Array(lenA + 1) { IntArray(lenB + 1) }
        for (i in 0..lenA) d[i][0] = i
        for (j in 0..lenB) d[0][j] = j

        for (i in 1..lenA) {
            for (j in 1..lenB) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(
                    d[i - 1][j] + 1,       // deletion
                    d[i][j - 1] + 1,       // insertion
                    d[i - 1][j - 1] + cost  // substitution
                )
                // Transposition
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = min(d[i][j], d[i - 2][j - 2] + cost)
                }
            }
        }
        return d[lenA][lenB]
    }

    /**
     * Compute adjacency-weighted edit cost between two strings.
     * Substitutions of adjacent keys cost less (0.5) than distant keys (1.0).
     */
    private fun adjacencyWeightedCost(original: String, corrected: String): Float {
        var cost = 0f
        var i = 0
        var j = 0

        while (i < original.length && j < corrected.length) {
            if (original[i] != corrected[j]) {
                cost += if (areAdjacent(original[i], corrected[j])) {
                    ADJACENT_KEY_COST
                } else {
                    DISTANT_KEY_COST
                }
            }
            i++
            j++
        }
        // Length difference
        cost += abs(original.length - corrected.length) * DISTANT_KEY_COST

        return cost
    }

    private fun areAdjacent(a: Char, b: Char): Boolean {
        val neighbors = adjacencyMap[a.lowercase()] ?: return false
        return b.lowercase() in neighbors
    }

    companion object {
        private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz"
        private const val ADJACENT_KEY_COST = 0.5f
        private const val DISTANT_KEY_COST = 1.0f

        // Confidence weights
        private const val WEIGHT_DISTANCE = 0.30f
        private const val WEIGHT_ADJACENCY = 0.25f
        private const val WEIGHT_FREQUENCY = 0.25f
        private const val WEIGHT_UNIQUENESS = 0.20f

        // Auto-apply threshold
        private const val AUTO_APPLY_THRESHOLD = 0.65f
    }
}

data class CorrectionCandidate(
    val word: String,
    val editDistance: Int,
    val weightedCost: Float,
    val frequency: Int
)

data class AutoCorrectionResult(
    val original: String,
    val corrected: String,
    val confidence: Float,
    val shouldAutoApply: Boolean,
    val candidates: List<CorrectionCandidate>
)
