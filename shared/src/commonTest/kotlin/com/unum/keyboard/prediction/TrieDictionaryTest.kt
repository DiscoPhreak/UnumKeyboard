package com.unum.keyboard.prediction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrieDictionaryTest {

    @Test
    fun insert_and_contains() {
        val trie = TrieDictionary()
        trie.insert("hello", 100)
        assertTrue(trie.contains("hello"))
        assertFalse(trie.contains("hell"))
        assertFalse(trie.contains("helloo"))
    }

    @Test
    fun contains_is_case_insensitive() {
        val trie = TrieDictionary()
        trie.insert("Hello", 100)
        assertTrue(trie.contains("hello"))
        assertTrue(trie.contains("HELLO"))
    }

    @Test
    fun getFrequency_returns_correct_value() {
        val trie = TrieDictionary()
        trie.insert("the", 100000)
        trie.insert("there", 5000)
        assertEquals(100000, trie.getFrequency("the"))
        assertEquals(5000, trie.getFrequency("there"))
        assertEquals(0, trie.getFrequency("them"))
    }

    @Test
    fun prefixSearch_returns_matching_words() {
        val trie = TrieDictionary()
        trie.insert("the", 100000)
        trie.insert("there", 5000)
        trie.insert("their", 4000)
        trie.insert("them", 6000)
        trie.insert("then", 7000)
        trie.insert("apple", 1000)

        val results = trie.prefixSearch("the")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.word.startsWith("the") })
        assertFalse(results.any { it.word == "apple" })
    }

    @Test
    fun prefixSearch_sorted_by_frequency() {
        val trie = TrieDictionary()
        trie.insert("the", 100000)
        trie.insert("then", 7000)
        trie.insert("them", 6000)
        trie.insert("there", 5000)

        val results = trie.prefixSearch("the")
        assertEquals("the", results[0].word)
        assertEquals("then", results[1].word)
    }

    @Test
    fun prefixSearch_respects_maxResults() {
        val trie = TrieDictionary()
        for (i in 0 until 20) {
            trie.insert("test$i", 100 + i)
        }
        val results = trie.prefixSearch("test", maxResults = 5)
        assertEquals(5, results.size)
    }

    @Test
    fun loadFromUnigrams_parses_correctly() {
        val text = """
            the 100000
            of 80000
            and 70000
            # this is a comment

            to 60000
        """.trimIndent()

        val trie = TrieDictionary()
        trie.loadFromUnigrams(text)

        assertTrue(trie.contains("the"))
        assertTrue(trie.contains("of"))
        assertTrue(trie.contains("and"))
        assertTrue(trie.contains("to"))
        assertEquals(4, trie.wordCount)
    }

    @Test
    fun wordId_roundtrip() {
        val trie = TrieDictionary()
        val id = trie.insert("hello", 100)
        assertEquals("hello", trie.getWordById(id))
        assertEquals(id, trie.getWordId("hello"))
    }
}
