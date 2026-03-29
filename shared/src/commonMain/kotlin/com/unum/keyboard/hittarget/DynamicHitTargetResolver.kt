package com.unum.keyboard.hittarget

import com.unum.keyboard.layout.KeyGeometry
import com.unum.keyboard.layout.KeyType
import kotlin.math.exp
import kotlin.math.max

/**
 * Resolves which key was tapped using Bayesian spatial + language model scoring.
 *
 * For each key k:
 *   score(k) = P_spatial(touch | key_k) × P_language(key_k | context)
 *
 * Where P_spatial is a bivariate Gaussian centered on the key, and P_language
 * comes from the NextKeyPredictor.
 *
 * ANCHORED TARGETS: The center region of each key (inner 60%) ALWAYS maps to
 * that key regardless of probabilities. Only the outer boundary zones are
 * probabilistically reassigned. This prevents the frustrating case where a user
 * clearly aims for a key but gets a different one.
 */
class DynamicHitTargetResolver(
    private val nextKeyPredictor: NextKeyPredictor = NextKeyPredictor(),
    val calibrator: TouchCalibrator = TouchCalibrator()
) {
    /**
     * Resolve which key was tapped at the given touch point.
     *
     * @param touchX X coordinate of touch
     * @param touchY Y coordinate of touch
     * @param keys All key geometries on the current layout
     * @param currentPrefix Current word prefix being typed
     * @return The resolved key, or null if touch is outside all keys
     */
    fun resolve(
        touchX: Float,
        touchY: Float,
        keys: List<KeyGeometry>,
        currentPrefix: String = ""
    ): KeyGeometry? {
        if (keys.isEmpty()) return null

        // Apply calibration offset
        val calibratedX = touchX + calibrator.offsetX
        val calibratedY = touchY + calibrator.offsetY

        // 1. Check anchored zones first — center 60% of each key always wins
        for (key in keys) {
            if (isInAnchorZone(calibratedX, calibratedY, key)) {
                return key
            }
        }

        // 2. Bayesian scoring for outer zones
        val keyProbs = if (currentPrefix.isNotEmpty() || true) {
            nextKeyPredictor.predictNextKey(currentPrefix)
        } else {
            emptyMap()
        }

        var bestKey: KeyGeometry? = null
        var bestScore = Float.MIN_VALUE

        for (key in keys) {
            val spatialProb = gaussianPdf(calibratedX, calibratedY, key)
            if (spatialProb < MIN_SPATIAL_PROB) continue

            val languageProb = getLanguageProb(key, keyProbs)
            val score = spatialProb * languageProb

            if (score > bestScore) {
                bestScore = score
                bestKey = key
            }
        }

        return bestKey
    }

    /**
     * Check if a touch point is in the anchored center zone of a key.
     * The anchor zone is the inner ANCHOR_RATIO (60%) of the key bounds.
     */
    private fun isInAnchorZone(x: Float, y: Float, key: KeyGeometry): Boolean {
        val shrinkX = key.bounds.width * (1f - ANCHOR_RATIO) / 2f
        val shrinkY = key.bounds.height * (1f - ANCHOR_RATIO) / 2f

        return x >= key.bounds.left + shrinkX &&
               x <= key.bounds.right - shrinkX &&
               y >= key.bounds.top + shrinkY &&
               y <= key.bounds.bottom - shrinkY
    }

    /**
     * Bivariate Gaussian PDF for spatial probability.
     * Models the probability that a touch at (x,y) was intended for this key.
     */
    private fun gaussianPdf(x: Float, y: Float, key: KeyGeometry): Float {
        val dx = x - key.center.x
        val dy = y - key.center.y

        // Standard deviation proportional to key size
        val sigmaX = key.bounds.width * SIGMA_RATIO
        val sigmaY = key.bounds.height * SIGMA_RATIO

        if (sigmaX <= 0f || sigmaY <= 0f) return 0f

        val exponent = -0.5f * ((dx * dx) / (sigmaX * sigmaX) + (dy * dy) / (sigmaY * sigmaY))
        return exp(exponent)
    }

    /**
     * Get language model probability for a key.
     * Non-character keys get a flat base probability.
     */
    private fun getLanguageProb(key: KeyGeometry, keyProbs: Map<Char, Float>): Float {
        if (key.key.type != KeyType.CHARACTER) {
            return BASE_LANGUAGE_PROB
        }

        val char = key.key.primary.firstOrNull()?.lowercaseChar() ?: return BASE_LANGUAGE_PROB
        val prob = keyProbs[char] ?: BASE_LANGUAGE_PROB

        // Blend with base probability so no key becomes completely unreachable
        return max(prob, MIN_LANGUAGE_PROB)
    }

    /**
     * Get the expanded/contracted hit zone sizes for debug visualization.
     * Returns a map of key ID to expansion factor (1.0 = normal, >1 = expanded, <1 = contracted).
     */
    fun getExpansionFactors(
        keys: List<KeyGeometry>,
        currentPrefix: String = ""
    ): Map<String, Float> {
        val keyProbs = nextKeyPredictor.predictNextKey(currentPrefix)
        val avgProb = if (keyProbs.isNotEmpty()) keyProbs.values.average().toFloat() else 1f / 26f

        return keys.associate { key ->
            val prob = if (key.key.type == KeyType.CHARACTER) {
                val char = key.key.primary.firstOrNull()?.lowercaseChar()
                char?.let { keyProbs[it] } ?: avgProb
            } else {
                avgProb
            }
            key.key.id to (prob / avgProb).coerceIn(MIN_EXPANSION, MAX_EXPANSION)
        }
    }

    companion object {
        /** Inner ratio of key that always maps to that key */
        const val ANCHOR_RATIO = 0.6f

        /** Gaussian sigma as ratio of key dimension */
        const val SIGMA_RATIO = 0.5f

        /** Base language probability for non-character keys */
        const val BASE_LANGUAGE_PROB = 0.04f

        /** Minimum language probability (prevents keys from being unreachable) */
        const val MIN_LANGUAGE_PROB = 0.005f

        /** Minimum spatial probability threshold */
        const val MIN_SPATIAL_PROB = 0.001f

        /** Min/max expansion factors for visualization */
        const val MIN_EXPANSION = 0.5f
        const val MAX_EXPANSION = 2.0f
    }
}
