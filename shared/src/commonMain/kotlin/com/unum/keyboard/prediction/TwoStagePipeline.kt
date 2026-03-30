package com.unum.keyboard.prediction

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Two-stage prediction pipeline:
 *
 * Stage 1 (synchronous, <5ms): Trie + N-gram from PredictionEngine
 *   → Show predictions immediately
 *
 * Stage 2 (async, ~20-50ms): Neural reranker scores candidates
 *   → When done, blend scores and update displayed predictions
 *   → If user typed another char, cancel stale result
 *
 * Debounce: Stage 2 waits 50ms after last keystroke before running.
 * This prevents wasting compute during fast burst typing.
 */
class TwoStagePipeline(
    private val predictionService: PredictionService,
    private val reranker: NeuralReranker = StubNeuralReranker(),
    private val enabled: Boolean = true
) {
    /**
     * Callback for when predictions are updated (either Stage 1 or Stage 2).
     */
    var onPredictionsUpdated: ((List<Prediction>) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var stage2Job: Job? = null
    private var currentRequestId = 0L
    private val mutex = Mutex()

    // Latest Stage 1 results (always available)
    private var stage1Results: List<Prediction> = emptyList()

    // Context for Stage 2
    private var contextWords: List<String> = emptyList()

    /**
     * Called on each keystroke. Runs Stage 1 synchronously and kicks off Stage 2.
     */
    fun onKeystroke(prefix: String, context: List<String> = emptyList()) {
        currentRequestId++
        val requestId = currentRequestId
        contextWords = context

        // Stage 1: synchronous trie + n-gram
        predictionService.updatePrefix(prefix)
        stage1Results = predictionService.predict(MAX_CANDIDATES)

        // Emit Stage 1 results immediately
        onPredictionsUpdated?.invoke(stage1Results.take(DISPLAY_SLOTS))

        // Stage 2: async neural reranking (if enabled and reranker is ready)
        if (enabled && reranker.isReady && stage1Results.isNotEmpty()) {
            stage2Job?.cancel()
            stage2Job = scope.launch {
                // Debounce: wait before running inference
                delay(DEBOUNCE_MS)

                // Check if this request is still current
                if (requestId != currentRequestId) return@launch

                try {
                    val candidates = stage1Results.map { it.word }
                    val neuralScores = reranker.score(contextWords, candidates)

                    // Check again after inference completes
                    if (requestId != currentRequestId) return@launch

                    // Blend scores
                    val blended = blendScores(stage1Results, neuralScores)

                    mutex.withLock {
                        if (requestId == currentRequestId) {
                            onPredictionsUpdated?.invoke(blended.take(DISPLAY_SLOTS))
                        }
                    }
                } catch (e: Exception) {
                    // Neural reranking failed — Stage 1 results remain displayed
                    // Log for diagnostics but don't show error to user
                }
            }
        }
    }

    /**
     * Called when a word is committed (space pressed).
     */
    fun onWordCommitted(word: String) {
        stage2Job?.cancel()
        predictionService.commitWord(word)
        currentRequestId++

        // Get next-word predictions
        predictionService.updatePrefix("")
        stage1Results = predictionService.predict(MAX_CANDIDATES)
        onPredictionsUpdated?.invoke(stage1Results.take(DISPLAY_SLOTS))
    }

    /**
     * Called on sentence boundary.
     */
    fun onSentenceBoundary() {
        stage2Job?.cancel()
        predictionService.resetContext()
        currentRequestId++
        stage1Results = emptyList()
        onPredictionsUpdated?.invoke(emptyList())
    }

    /**
     * Blend Stage 1 and Stage 2 scores.
     * final_score = 0.4 × stage1_score + 0.6 × neural_score
     */
    private fun blendScores(
        stage1: List<Prediction>,
        neuralScores: Map<String, Float>
    ): List<Prediction> {
        if (neuralScores.isEmpty()) return stage1

        // Normalize Stage 1 scores to 0-1 range
        val maxStage1 = stage1.maxOfOrNull { it.score } ?: 1f
        val minStage1 = stage1.minOfOrNull { it.score } ?: 0f
        val range = if (maxStage1 > minStage1) maxStage1 - minStage1 else 1f

        return stage1.map { prediction ->
            val normalizedStage1 = (prediction.score - minStage1) / range
            val neuralScore = neuralScores[prediction.word] ?: 0.5f
            val blendedScore = STAGE1_WEIGHT * normalizedStage1 + NEURAL_WEIGHT * neuralScore

            prediction.copy(score = blendedScore)
        }.sortedByDescending { it.score }
    }

    /**
     * Load the neural model. Call during keyboard startup.
     */
    fun loadModel() {
        scope.launch {
            try {
                reranker.load()
            } catch (e: Exception) {
                // Model failed to load — Stage 1 only mode
            }
        }
    }

    /**
     * Release resources. Call when keyboard is destroyed.
     */
    fun destroy() {
        stage2Job?.cancel()
        reranker.release()
        scope.cancel()
    }

    companion object {
        const val DEBOUNCE_MS = 50L
        const val STAGE1_WEIGHT = 0.4f
        const val NEURAL_WEIGHT = 0.6f
        const val MAX_CANDIDATES = 10  // Stage 1 generates up to 10 for reranking
        const val DISPLAY_SLOTS = 3    // Show top 3 to user
    }
}
