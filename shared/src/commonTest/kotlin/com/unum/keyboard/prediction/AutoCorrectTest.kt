package com.unum.keyboard.prediction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoCorrectTest {

    private fun createAutoCorrect(withIndex: Boolean = false): AutoCorrect {
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
        val ac = AutoCorrect(dict)
        if (withIndex) ac.buildIndex()
        return ac
    }

    // ---- Original tests (corrections) ----

    @Test
    fun corrections_findsTransposition() {
        val ac = createAutoCorrect()
        val results = ac.corrections("teh")
        val words = results.map { it.word }
        assertTrue("the" in words, "Should correct 'teh' to 'the', got: $words")
    }

    @Test
    fun corrections_findsSubstitution() {
        val ac = createAutoCorrect()
        val results = ac.corrections("thd")
        val words = results.map { it.word }
        assertTrue("the" in words, "Should correct 'thd' to 'the', got: $words")
    }

    @Test
    fun corrections_findsDeletion() {
        val ac = createAutoCorrect()
        val results = ac.corrections("helo")
        val words = results.map { it.word }
        assertTrue("hello" in words, "Should correct 'helo' to 'hello', got: $words")
    }

    @Test
    fun corrections_findsInsertion() {
        val ac = createAutoCorrect()
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
        val dict = TrieDictionary()
        dict.insert("the", 100000)
        dict.insert("she", 8000)

        val adjacency = mapOf(
            "t" to listOf("r", "y"),
            "r" to listOf("t", "e")
        )
        val ac = AutoCorrect(dict, adjacency)
        val results = ac.corrections("rhe")
        val words = results.map { it.word }
        assertTrue("the" in words, "Adjacent key correction should suggest 'the' for 'rhe', got: $words")
    }

    // ---- Symmetric delete index tests ----

    @Test
    fun indexedCorrections_findsTransposition() {
        val ac = createAutoCorrect(withIndex = true)
        val results = ac.corrections("teh")
        val words = results.map { it.word }
        assertTrue("the" in words, "Indexed: Should correct 'teh' to 'the', got: $words")
    }

    @Test
    fun indexedCorrections_findsSubstitution() {
        val ac = createAutoCorrect(withIndex = true)
        val results = ac.corrections("thd")
        val words = results.map { it.word }
        assertTrue("the" in words, "Indexed: Should correct 'thd' to 'the', got: $words")
    }

    @Test
    fun indexedCorrections_findsDeletion() {
        val ac = createAutoCorrect(withIndex = true)
        val results = ac.corrections("helo")
        val words = results.map { it.word }
        assertTrue("hello" in words, "Indexed: Should correct 'helo' to 'hello', got: $words")
    }

    @Test
    fun indexedCorrections_findsInsertion() {
        val ac = createAutoCorrect(withIndex = true)
        val results = ac.corrections("thee")
        val words = results.map { it.word }
        assertTrue("the" in words, "Indexed: Should correct 'thee' to 'the', got: $words")
    }

    @Test
    fun indexedCorrections_returnsEmptyForCorrectWord() {
        val ac = createAutoCorrect(withIndex = true)
        val results = ac.corrections("the")
        assertTrue(results.isEmpty(), "Indexed: Should return empty for correct word")
    }

    // ---- Confidence / evaluateCorrection tests ----

    @Test
    fun evaluateCorrection_highConfidenceForTransposition() {
        val ac = createAutoCorrect(withIndex = true)
        val result = ac.evaluateCorrection("teh")
        assertTrue(result.confidence > 0.5f,
            "Transposition of high-freq word should have high confidence, got: ${result.confidence}")
        assertEquals("the", result.corrected)
        assertTrue(result.shouldAutoApply, "Should auto-apply for clear transposition")
    }

    @Test
    fun evaluateCorrection_noAutoApplyForDictionaryWord() {
        val ac = createAutoCorrect(withIndex = true)
        // "have" is a valid dictionary word — should not auto-correct even though "gave" etc. exist
        val result = ac.evaluateCorrection("have")
        assertFalse(result.shouldAutoApply, "Should not auto-apply for valid dictionary word")
    }

    @Test
    fun evaluateCorrection_noAutoApplyForUserDictionaryWord() {
        val ac = createAutoCorrect(withIndex = true)
        val result = ac.evaluateCorrection("xyz123") { word -> word == "xyz123" }
        assertFalse(result.shouldAutoApply, "Should not auto-apply for user dictionary word")
    }

    @Test
    fun evaluateCorrection_noAutoApplyForBlockedWord() {
        val ac = createAutoCorrect(withIndex = true)
        ac.addToBlockList("teh")
        val result = ac.evaluateCorrection("teh")
        assertFalse(result.shouldAutoApply, "Should not auto-apply for blocked word")
        assertEquals(0f, result.confidence)
    }

    @Test
    fun evaluateCorrection_noAutoApplyForSingleChar() {
        val ac = createAutoCorrect(withIndex = true)
        val result = ac.evaluateCorrection("x")
        assertFalse(result.shouldAutoApply, "Should not auto-apply for single character")
    }

    // ---- Capitalization tests ----

    @Test
    fun evaluateCorrection_preservesInitialCap() {
        val ac = createAutoCorrect(withIndex = true)
        val result = ac.evaluateCorrection("Teh")
        assertEquals("The", result.corrected, "Should preserve initial capitalization")
    }

    @Test
    fun evaluateCorrection_preservesAllCaps() {
        val ac = createAutoCorrect(withIndex = true)
        val result = ac.evaluateCorrection("TEH")
        assertEquals("THE", result.corrected, "Should preserve all-caps")
    }

    @Test
    fun evaluateCorrection_preservesLowercase() {
        val ac = createAutoCorrect(withIndex = true)
        val result = ac.evaluateCorrection("teh")
        assertEquals("the", result.corrected, "Should preserve lowercase")
    }

    // ---- Block list tests ----

    @Test
    fun blockList_preventsAutoApply() {
        val ac = createAutoCorrect(withIndex = true)
        ac.addToBlockList("teh")
        assertTrue(ac.isBlocked("teh"))
        val result = ac.evaluateCorrection("teh")
        assertFalse(result.shouldAutoApply)
    }

    @Test
    fun blockList_serializeDeserializeRoundTrip() {
        val ac = createAutoCorrect(withIndex = true)
        ac.addToBlockList("teh")
        ac.addToBlockList("wrld")
        val serialized = ac.serializeBlockList()

        val ac2 = createAutoCorrect(withIndex = true)
        ac2.deserializeBlockList(serialized)
        assertTrue(ac2.isBlocked("teh"))
        assertTrue(ac2.isBlocked("wrld"))
    }

    @Test
    fun blockList_addAndRemove() {
        val ac = createAutoCorrect(withIndex = true)
        ac.addToBlockList("teh")
        assertTrue(ac.isBlocked("teh"))
        ac.removeFromBlockList("teh")
        assertFalse(ac.isBlocked("teh"))
    }

    @Test
    fun blockList_caseInsensitive() {
        val ac = createAutoCorrect(withIndex = true)
        ac.addToBlockList("Teh")
        assertTrue(ac.isBlocked("teh"))
        assertTrue(ac.isBlocked("TEH"))
    }
}
