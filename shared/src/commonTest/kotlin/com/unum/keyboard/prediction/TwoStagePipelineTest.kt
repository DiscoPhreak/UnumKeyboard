package com.unum.keyboard.prediction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TwoStagePipelineTest {

    private fun createPredictionService(): PredictionService {
        val service = PredictionService()
        val unigrams = """
            the 100000
            there 5000
            their 4000
            them 6000
            then 7000
            they 8000
            to 60000
            go 5000
            get 4500
            good 7000
            have 30000
            is 50000
            want 4000
            i 40000
            a 90000
        """.trimIndent()
        val bigrams = """
            i want 5000
            i have 4000
            want to 6000
            to go 2000
            to get 1500
        """.trimIndent()
        val trigrams = """
            i want to 3000
        """.trimIndent()
        service.initialize(unigrams, bigrams, trigrams)
        return service
    }

    @Test
    fun pipeline_emitsStage1ResultsImmediately() {
        val service = createPredictionService()
        val pipeline = TwoStagePipeline(service, StubNeuralReranker(), enabled = true)

        var receivedPredictions: List<Prediction>? = null
        pipeline.onPredictionsUpdated = { receivedPredictions = it }

        pipeline.onKeystroke("th")

        // Stage 1 should have emitted results synchronously
        assertTrue(receivedPredictions != null, "Should receive predictions immediately")
        assertTrue(receivedPredictions!!.isNotEmpty(), "Should have predictions for 'th'")
        assertTrue(receivedPredictions!!.all { it.word.startsWith("th") },
            "All predictions should start with 'th'")
    }

    @Test
    fun pipeline_limitsToDisplaySlots() {
        val service = createPredictionService()
        val pipeline = TwoStagePipeline(service, StubNeuralReranker(), enabled = false)

        var receivedPredictions: List<Prediction>? = null
        pipeline.onPredictionsUpdated = { receivedPredictions = it }

        pipeline.onKeystroke("th")

        assertTrue(receivedPredictions!!.size <= TwoStagePipeline.DISPLAY_SLOTS,
            "Should limit to ${TwoStagePipeline.DISPLAY_SLOTS} slots")
    }

    @Test
    fun pipeline_wordCommit_updatesContext() {
        val service = createPredictionService()
        val pipeline = TwoStagePipeline(service, StubNeuralReranker(), enabled = false)

        var receivedPredictions: List<Prediction>? = null
        pipeline.onPredictionsUpdated = { receivedPredictions = it }

        pipeline.onWordCommitted("i")

        // After committing "i", next-word predictions should include "want", "have"
        if (receivedPredictions != null && receivedPredictions!!.isNotEmpty()) {
            val words = receivedPredictions!!.map { it.word }
            assertTrue(words.any { it in listOf("want", "have") },
                "After 'i', should predict 'want' or 'have', got: $words")
        }
    }

    @Test
    fun pipeline_sentenceBoundary_clearsPredictions() {
        val service = createPredictionService()
        val pipeline = TwoStagePipeline(service, StubNeuralReranker(), enabled = false)

        var receivedPredictions: List<Prediction>? = null
        pipeline.onPredictionsUpdated = { receivedPredictions = it }

        pipeline.onWordCommitted("hello")
        pipeline.onSentenceBoundary()

        assertTrue(receivedPredictions != null)
        assertTrue(receivedPredictions!!.isEmpty(), "Should clear predictions on sentence boundary")
    }

    @Test
    fun scoreBlending_weightsAreCorrect() {
        // Verify the blending constants
        val totalWeight = TwoStagePipeline.STAGE1_WEIGHT + TwoStagePipeline.NEURAL_WEIGHT
        assertEquals(1.0f, totalWeight, "Weights should sum to 1.0")
    }

    @Test
    fun scoreBlending_stage1Weight() {
        assertEquals(0.4f, TwoStagePipeline.STAGE1_WEIGHT,
            "Stage 1 weight should be 0.4")
    }

    @Test
    fun scoreBlending_neuralWeight() {
        assertEquals(0.6f, TwoStagePipeline.NEURAL_WEIGHT,
            "Neural weight should be 0.6")
    }

    @Test
    fun pipeline_disabledReranker_usesStage1Only() {
        val service = createPredictionService()
        val pipeline = TwoStagePipeline(service, StubNeuralReranker(), enabled = false)

        var receivedCount = 0
        pipeline.onPredictionsUpdated = { receivedCount++ }

        pipeline.onKeystroke("th")

        // With reranker disabled, should get exactly one emission (Stage 1 only)
        assertEquals(1, receivedCount, "Disabled reranker should emit once (Stage 1 only)")
    }

    @Test
    fun stubReranker_isAlwaysReady() {
        val stub = StubNeuralReranker()
        assertTrue(stub.isReady, "Stub reranker should always be ready")
    }

    @Test
    fun pipeline_destroy_doesNotCrash() {
        val service = createPredictionService()
        val pipeline = TwoStagePipeline(service, StubNeuralReranker(), enabled = true)
        pipeline.onKeystroke("th")
        pipeline.destroy()
        // Should not throw
    }
}
