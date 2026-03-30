package com.unum.keyboard.prediction

import kotlin.math.min

/**
 * Autocorrect using Damerau-Levenshtein distance weighted by keyboard adjacency.
 * Adjacent key typos (e.g., 'g' instead of 'f') cost less than distant ones.
 */
class AutoCorrect(
    private val dictionary: TrieDictionary,
    private val adjacencyMap: Map<String, List<String>> = emptyMap()
) {
    /**
     * Find corrections for a mistyped word.
     * Returns candidates within edit distance 1-2, weighted by adjacency.
     */
    fun corrections(
        input: String,
        maxResults: Int = 5,
        maxEditDistance: Int = 2
    ): List<CorrectionCandidate> {
        val inputLower = input.lowercase()
        if (dictionary.contains(inputLower)) return emptyList()

        val candidates = mutableListOf<CorrectionCandidate>()
        val edits1 = generateEdits(inputLower)

        for (edit in edits1) {
            if (dictionary.contains(edit)) {
                val cost = adjacencyWeightedCost(inputLower, edit)
                val freq = dictionary.getFrequency(edit)
                candidates.add(CorrectionCandidate(edit, 1, cost, freq))
            }
        }

        if (candidates.size < maxResults && maxEditDistance >= 2) {
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

        candidates.sortWith(compareBy<CorrectionCandidate> { it.editDistance }
            .thenBy { it.weightedCost }
            .thenByDescending { it.frequency })

        return candidates.take(maxResults)
    }

    /**
     * Generate all strings within edit distance 1 of the input.
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
     * Compute adjacency-weighted edit cost between two strings.
     * Substitutions of adjacent keys cost less (0.5) than distant keys (1.0).
     */
    private fun adjacencyWeightedCost(original: String, corrected: String): Float {
        val len = min(original.length, corrected.length)
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
        cost += kotlin.math.abs(original.length - corrected.length) * DISTANT_KEY_COST

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
    }
}

data class CorrectionCandidate(
    val word: String,
    val editDistance: Int,
    val weightedCost: Float,
    val frequency: Int
)
