package com.unum.keyboard.prediction

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextRerankerTest {

    private fun createReranker(): Pair<ContextReranker, TrieDictionary> {
        val dict = TrieDictionary()
        val words = listOf(
            "the" to 100000, "there" to 5000, "their" to 4000,
            "them" to 6000, "then" to 7000, "they" to 8000,
            "to" to 60000, "go" to 5000, "get" to 4500,
            "good" to 7000, "have" to 30000, "is" to 50000,
            "want" to 4000, "i" to 40000, "a" to 90000,
            "happy" to 2000, "help" to 2500, "world" to 3000
        )
        for ((word, freq) in words) {
            dict.insert(word, freq)
        }

        val ngram = NGramModel()
        for ((word, _) in words) {
            val id = dict.getWordId(word)
            if (id >= 0) ngram.setWordId(word, id)
        }
        ngram.loadBigrams("""
            i want 5000
            i have 4000
            want to 6000
            to go 2000
            to get 1500
            they have 3000
            they want 2000
        """.trimIndent())
        ngram.loadTrigrams("""
            i want to 3000
        """.trimIndent())

        val reranker = ContextReranker(dict, ngram, 100000)
        runBlocking { reranker.load() }
        return reranker to dict
    }

    @Test
    fun isReady_afterLoad() {
        val (reranker, _) = createReranker()
        assertTrue(reranker.isReady)
    }

    @Test
    fun isNotReady_afterRelease() {
        val (reranker, _) = createReranker()
        reranker.release()
        assertTrue(!reranker.isReady)
    }

    @Test
    fun score_returnsAllCandidates() = runBlocking {
        val (reranker, _) = createReranker()
        val candidates = listOf("the", "there", "their", "them")
        val scores = reranker.score(listOf("i"), candidates)
        assertEquals(candidates.size, scores.size, "Should return score for every candidate")
        for (word in candidates) {
            assertTrue(word in scores, "Missing score for '$word'")
        }
    }

    @Test
    fun score_sumsToOne() = runBlocking {
        val (reranker, _) = createReranker()
        val candidates = listOf("the", "there", "their", "them", "then")
        val scores = reranker.score(listOf("i"), candidates)
        val total = scores.values.sum()
        assertTrue(total in 0.99f..1.01f, "Softmax scores should sum to ~1.0, got: $total")
    }

    @Test
    fun score_emptyContext_stillWorks() = runBlocking {
        val (reranker, _) = createReranker()
        val candidates = listOf("the", "is", "a")
        val scores = reranker.score(emptyList(), candidates)
        assertEquals(3, scores.size)
        assertTrue(scores.values.all { it > 0f }, "All scores should be positive")
    }

    @Test
    fun score_emptyCandidate_returnsEmpty() = runBlocking {
        val (reranker, _) = createReranker()
        val scores = reranker.score(listOf("i"), emptyList())
        assertTrue(scores.isEmpty())
    }

    @Test
    fun score_bigramContext_boostsContinuation() = runBlocking {
        val (reranker, _) = createReranker()
        // After "i", "want" and "have" should score higher than "world"
        val candidates = listOf("want", "have", "world")
        val scores = reranker.score(listOf("i"), candidates)
        val wantScore = scores["want"] ?: 0f
        val worldScore = scores["world"] ?: 0f
        assertTrue(wantScore > worldScore,
            "After 'i', 'want' ($wantScore) should score higher than 'world' ($worldScore)")
    }

    @Test
    fun score_highFreqWord_scoresHigherThanLowFreq() = runBlocking {
        val (reranker, _) = createReranker()
        // Without context, "the" (100000) should beat "world" (3000)
        val candidates = listOf("the", "world")
        val scores = reranker.score(emptyList(), candidates)
        val theScore = scores["the"] ?: 0f
        val worldScore = scores["world"] ?: 0f
        assertTrue(theScore > worldScore,
            "'the' ($theScore) should score higher than 'world' ($worldScore) by frequency")
    }

    @Test
    fun score_trigramContext_boostsCandidate() = runBlocking {
        val (reranker, _) = createReranker()
        // After "i want", "to" should score very high (trigram match)
        val candidates = listOf("to", "world", "help")
        val scores = reranker.score(listOf("i", "want"), candidates)
        val toScore = scores["to"] ?: 0f
        val helpScore = scores["help"] ?: 0f
        assertTrue(toScore > helpScore,
            "After 'i want', 'to' ($toScore) should score higher than 'help' ($helpScore)")
    }

    @Test
    fun pipeline_withContextReranker_emitsResults() {
        val service = PredictionService()
        val unigrams = """
            the 100000
            there 5000
            their 4000
            to 60000
            i 40000
            want 4000
            have 30000
        """.trimIndent()
        val bigrams = """
            i want 5000
            i have 4000
            want to 6000
        """.trimIndent()
        val trigrams = """
            i want to 3000
        """.trimIndent()
        service.initialize(unigrams, bigrams, trigrams)

        val reranker = service.createReranker()
        assertTrue(reranker is ContextReranker, "createReranker should return ContextReranker")

        val pipeline = TwoStagePipeline(service, reranker, enabled = true)
        var received: List<Prediction>? = null
        pipeline.onPredictionsUpdated = { received = it }

        pipeline.onKeystroke("th")
        assertTrue(received != null && received!!.isNotEmpty(),
            "Pipeline with ContextReranker should emit predictions")
        pipeline.destroy()
    }

    @Test
    fun weights_sumToOne() {
        val total = ContextReranker.UNIGRAM_WEIGHT +
            ContextReranker.BIGRAM_WEIGHT +
            ContextReranker.TRIGRAM_WEIGHT +
            ContextReranker.PREFIX_WEIGHT +
            ContextReranker.LENGTH_WEIGHT +
            ContextReranker.COHERENCE_WEIGHT
        assertTrue(total in 0.99f..1.01f, "Feature weights should sum to 1.0, got: $total")
    }
}
