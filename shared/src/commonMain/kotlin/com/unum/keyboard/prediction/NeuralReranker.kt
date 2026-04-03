package com.unum.keyboard.prediction

/**
 * Neural reranker that scores candidate words using a transformer model.
 *
 * Architecture: Small encoder-only transformer (~20-30M params, INT8 quantized to ~25-30MB).
 * Input: [CLS] context_tokens [SEP] candidate_word [SEP] → score (0-1)
 * Scores each candidate from the Stage 1 trie results in a single batched inference.
 *
 * This is the interface + stub. Real ONNX Runtime integration requires:
 * - Android: onnxruntime-android dependency
 * - iOS: onnxruntime-c via Kotlin/Native cinterop
 *
 * The stub always returns uniform scores so Stage 1 results pass through unchanged.
 */
interface NeuralReranker {
    /** Whether the model is loaded and ready for inference. */
    val isReady: Boolean

    /** Load the model. Call once at keyboard startup (async, ~500ms). */
    suspend fun load()

    /** Release model resources. Call when keyboard is destroyed. */
    fun release()

    /**
     * Score candidates given context. Returns a map of word → neural score (0-1).
     * @param context The last N tokens of text before the current word.
     * @param candidates The Stage 1 candidates to rerank.
     * @return Map of word to neural probability score.
     */
    suspend fun score(context: List<String>, candidates: List<String>): Map<String, Float>
}

/**
 * Stub reranker that returns uniform scores.
 * Used as fallback when the ONNX model is unavailable.
 */
class StubNeuralReranker : NeuralReranker {
    override val isReady: Boolean = true

    override suspend fun load() {
        // No-op: stub is always ready
    }

    override fun release() {
        // No-op
    }

    override suspend fun score(context: List<String>, candidates: List<String>): Map<String, Float> {
        // Return uniform scores — Stage 1 ranking passes through unchanged
        return candidates.associateWith { 0.5f }
    }
}

/**
 * ONNX Runtime-backed reranker. Reserved for future use when a trained
 * transformer model is available. See ContextReranker for the current
 * feature-based implementation that replaces the stub.
 */
