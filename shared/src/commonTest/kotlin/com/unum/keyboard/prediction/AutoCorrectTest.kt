package com.unum.keyboard.prediction

import kotlin.test.Test
import kotlin.test.assertTrue

class AutoCorrectTest {

    private fun createAutoCorrect(): AutoCorrect {
        val dict = TrieDictionary()
        val words = listOf(
            "the" to 100000, "there" to 5000, "their" to 4000,
            "hello" to 2000, "help" to 2500, "world" to 3000,
            "would" to 8000, "with" to 20000, "this" to 9000,
            "that" to 10000, "have" to 30000, "from" to 15000,
            "good" to 7000, "great" to 3500, "going" to 4000
        )
        for ((word, freq) in words) {
            dict.insert(word, freq)
        }
        return AutoCorrect(dict)
    }

    @Test
    fun corrections_findsTransposition() {
        val ac = createAutoCorrect()
        // "teh" → "the" (transposition of h and e)
        val results = ac.corrections("teh")
        val words = results.map { it.word }
        assertTrue("the" in words, "Should correct 'teh' to 'the', got: $words")
    }

    @Test
    fun corrections_findsSubstitution() {
        val ac = createAutoCorrect()
        // "thd" → "the" (substitution of d for e)
        val results = ac.corrections("thd")
        val words = results.map { it.word }
        assertTrue("the" in words, "Should correct 'thd' to 'the', got: $words")
    }

    @Test
    fun corrections_findsDeletion() {
        val ac = createAutoCorrect()
        // "helo" → "hello" (missing l)
        val results = ac.corrections("helo")
        val words = results.map { it.word }
        assertTrue("hello" in words, "Should correct 'helo' to 'hello', got: $words")
    }

    @Test
    fun corrections_findsInsertion() {
        val ac = createAutoCorrect()
        // "thee" → "the" (extra e)
        val results = ac.corrections("thee")
        val words = results.map { it.word }
        assertTrue("the" in words, "Should correct 'thee' to 'the', got: $words")
    }

    @Test
    fun corrections_returnsEmptyForCorrectWord() {
        val ac = createAutoCorrect()
        val results = ac.corrections("the")
        assertTrue(results.isEmpty(), "Should return empty for correct word, got: ${results.map { it.word }}")
    }

    @Test
    fun corrections_sortedByEditDistanceThenFrequency() {
        val ac = createAutoCorrect()
        val results = ac.corrections("thr")
        // Edit distance 1 corrections should come before distance 2
        if (results.size >= 2) {
            assertTrue(results[0].editDistance <= results[1].editDistance,
                "Results should be sorted by edit distance")
        }
    }

    @Test
    fun corrections_respectsMaxResults() {
        val ac = createAutoCorrect()
        val results = ac.corrections("t", maxResults = 3)
        assertTrue(results.size <= 3, "Should respect maxResults")
    }

    @Test
    fun corrections_adjacencyWeighting() {
        // With adjacency info, adjacent key typos should have lower cost
        val dict = TrieDictionary()
        dict.insert("the", 100000)
        dict.insert("she", 8000)

        val adjacency = mapOf(
            "t" to listOf("r", "y"),
            "r" to listOf("t", "e")
        )
        val ac = AutoCorrect(dict, adjacency)
        // "rhe" is NOT in dictionary, so it will be corrected
        // r→t is adjacent, so "the" should have lower weighted cost than "she" (s→r not adjacent)
        val results = ac.corrections("rhe")
        val words = results.map { it.word }
        assertTrue("the" in words, "Adjacent key correction should suggest 'the' for 'rhe', got: $words")
    }
}
