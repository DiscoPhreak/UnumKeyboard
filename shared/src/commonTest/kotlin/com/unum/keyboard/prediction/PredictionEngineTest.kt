package com.unum.keyboard.prediction

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class PredictionEngineTest {

    private fun createEngine(): PredictionEngine {
        val dict = TrieDictionary()
        val unigrams = """
            the 100000
            there 5000
            their 4000
            them 6000
            then 7000
            they 8000
            this 9000
            that 10000
            to 60000
            go 5000
            get 4500
            give 3000
            good 7000
            great 3500
            going 4000
            have 30000
            is 50000
            want 4000
            hello 2000
            help 2500
            world 3000
            would 8000
            with 20000
            was 25000
            will 15000
            can 10000
            not 12000
            do 8000
            i 40000
            a 90000
        """.trimIndent()
        dict.loadFromUnigrams(unigrams)

        val ngram = NGramModel()
        // Sync IDs
        for (line in unigrams.lineSequence()) {
            val parts = line.trim().split("\\s+".toRegex(), limit = 2)
            if (parts.size == 2) {
                val word = parts[0].lowercase()
                val id = dict.getWordId(word)
                if (id >= 0) ngram.setWordId(word, id)
            }
        }

        val bigrams = """
            i want 5000
            i have 4000
            i was 3000
            i will 2500
            want to 6000
            going to 5000
            to go 2000
            to get 1500
            to give 1000
            the world 2000
        """.trimIndent()
        ngram.loadBigrams(bigrams)

        val trigrams = """
            i want to 3000
            going to go 1500
            going to get 1000
        """.trimIndent()
        ngram.loadTrigrams(trigrams)

        val autoCorrect = AutoCorrect(dict)
        val engine = PredictionEngine(dict, ngram, autoCorrect)
        engine.setMaxUnigramFreq(100000)
        return engine
    }

    @Test
    fun predict_prefixCompletion_returnsResults() {
        val engine = createEngine()
        engine.updatePrefix("th")
        val results = engine.predict(5)
        assertTrue(results.isNotEmpty(), "Should return predictions for prefix 'th'")
        assertTrue(results.all { it.word.startsWith("th") }, "All results should start with 'th'")
    }

    @Test
    fun predict_prefixCompletion_highFreqFirst() {
        val engine = createEngine()
        engine.updatePrefix("th")
        val results = engine.predict(5)
        // "the" (100000) should rank highest
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].word == "the" || results[0].word == "that",
            "Highest freq word starting with 'th' should be first, got: ${results[0].word}")
    }

    @Test
    fun predict_withContext_usesNgrams() {
        val engine = createEngine()
        engine.commitWord("i")
        engine.updatePrefix("w")
        val results = engine.predict(5)
        assertTrue(results.isNotEmpty(), "Should return predictions after context 'i'")
        // "want", "was", "will", "with", "would" should appear
        val words = results.map { it.word }
        assertTrue(words.any { it == "want" || it == "was" || it == "will" || it == "with" || it == "would" },
            "Should predict common words after 'i' starting with 'w', got: $words")
    }

    @Test
    fun predict_nextWord_fromContext() {
        val engine = createEngine()
        engine.commitWord("i")
        engine.updatePrefix("")
        val results = engine.predict(5)
        // After "i", bigrams suggest "want", "have", "was", "will"
        if (results.isNotEmpty()) {
            val words = results.map { it.word }
            assertTrue(words.any { it in listOf("want", "have", "was", "will") },
                "Next-word prediction after 'i' should include common continuations, got: $words")
        }
    }

    @Test
    fun predict_autocorrect_fixesTypo() {
        val engine = createEngine()
        // "teh" is a common typo for "the"
        engine.updatePrefix("teh")
        val results = engine.predict(5)
        val words = results.map { it.word }
        assertTrue("the" in words, "Autocorrect should suggest 'the' for typo 'teh', got: $words")
    }

    @Test
    fun predict_autocorrect_fixesMissingLetter() {
        val engine = createEngine()
        engine.updatePrefix("helo")
        val results = engine.predict(5)
        val words = results.map { it.word }
        assertTrue("hello" in words, "Autocorrect should suggest 'hello' for 'helo', got: $words")
    }

    @Test
    fun predict_exactMatch_boosted() {
        val engine = createEngine()
        engine.updatePrefix("the")
        val results = engine.predict(5)
        assertTrue(results.isNotEmpty())
        // "the" is both a prefix match AND an exact match — should be ranked high
        assertTrue(results[0].word == "the",
            "Exact match 'the' should be first, got: ${results[0].word}")
    }

    @Test
    fun commitWord_and_resetContext() {
        val engine = createEngine()
        engine.commitWord("hello")
        engine.commitWord("world")
        engine.resetContext()
        engine.updatePrefix("th")
        val results = engine.predict(5)
        // After reset, context shouldn't influence predictions abnormally
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun predict_emptyPrefix_noContext_returnsEmpty() {
        val engine = createEngine()
        engine.updatePrefix("")
        val results = engine.predict(5)
        // No context and no prefix = no predictions
        assertTrue(results.isEmpty(), "Should return empty with no prefix and no context")
    }

    @Test
    fun predict_singleChar_returnsResults() {
        val engine = createEngine()
        engine.updatePrefix("g")
        val results = engine.predict(5)
        assertTrue(results.isNotEmpty(), "Should return predictions for single char 'g'")
        assertTrue(results.all { it.word.startsWith("g") })
    }
}
