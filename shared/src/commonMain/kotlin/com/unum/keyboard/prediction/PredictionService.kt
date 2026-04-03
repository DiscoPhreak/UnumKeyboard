package com.unum.keyboard.prediction

import com.unum.keyboard.stats.LearningManager

/**
 * High-level prediction service that manages the prediction engine lifecycle
 * and provides a simple API for the keyboard UI.
 */
class PredictionService {
    /** The loaded dictionary — exposed for gesture typing decoder */
    var dictionary: TrieDictionary? = null
        private set
    private var ngramModel: NGramModel? = null
    private var autoCorrect: AutoCorrect? = null
    private var engine: PredictionEngine? = null

    /** User learning manager — exposed for IME to call onWordCommitted */
    var learningManager: LearningManager? = null
        private set

    var isLoaded: Boolean = false
        private set

    /**
     * Initialize the prediction engine from frequency data text.
     * Call this once when the keyboard starts up.
     */
    fun initialize(
        unigramsText: String,
        bigramsText: String,
        trigramsText: String,
        adjacencyMap: Map<String, List<String>> = emptyMap()
    ) {
        val dict = TrieDictionary()
        dict.loadFromUnigrams(unigramsText)

        val ngram = NGramModel()
        // Sync word IDs between dictionary and n-gram model
        for (line in unigramsText.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val parts = trimmed.split("\\s+".toRegex(), limit = 2)
            if (parts.size == 2) {
                val word = parts[0].lowercase()
                val id = dict.getWordId(word)
                if (id >= 0) ngram.setWordId(word, id)
            }
        }

        ngram.loadBigrams(bigramsText)
        ngram.loadTrigrams(trigramsText)

        val ac = AutoCorrect(dict, adjacencyMap)
        ac.buildIndex()
        val pe = PredictionEngine(dict, ngram, ac)

        // Find max unigram frequency for normalization
        var maxFreq = 1
        for (line in unigramsText.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val parts = trimmed.split("\\s+".toRegex(), limit = 2)
            if (parts.size == 2) {
                val freq = parts[1].toIntOrNull() ?: continue
                if (freq > maxFreq) maxFreq = freq
            }
        }
        pe.setMaxUnigramFreq(maxFreq)
        maxUnigramFreq = maxFreq

        // Initialize user learning
        val lm = LearningManager(dict)
        pe.learningScorer = LearningManagerScorer(lm)

        dictionary = dict
        ngramModel = ngram
        autoCorrect = ac
        engine = pe
        learningManager = lm
        isLoaded = true
    }

    /**
     * Get predictions for the current typing state.
     * @param currentTime current time in millis for user frequency recency scoring
     */
    fun predict(maxResults: Int = 3, currentTime: Long = 0L): List<Prediction> {
        // Update scorer time before prediction
        val scorer = engine?.learningScorer
        if (scorer is LearningManagerScorer) {
            scorer.currentTime = currentTime
        }
        return engine?.predict(maxResults) ?: emptyList()
    }

    /**
     * Update the prefix being typed.
     */
    fun updatePrefix(prefix: String) {
        engine?.updatePrefix(prefix)
    }

    /**
     * Commit a word (space pressed or prediction accepted).
     */
    fun commitWord(word: String) {
        engine?.commitWord(word)
    }

    /**
     * Reset context (sentence boundary).
     */
    fun resetContext() {
        engine?.resetContext()
        learningManager?.onSentenceBoundary()
    }

    /**
     * Record that a word was committed by the user (for learning).
     */
    fun learnWord(word: String, timestamp: Long) {
        learningManager?.onWordCommitted(word, timestamp)
    }

    /**
     * Load persisted learning data.
     */
    fun loadLearningData(data: String) {
        learningManager?.deserialize(data)
    }

    /**
     * Serialize learning data for persistence.
     */
    fun saveLearningData(): String {
        return learningManager?.serialize() ?: ""
    }

    // ---- Reranker ----

    /**
     * Create a ContextReranker backed by this service's dictionary and n-gram model.
     * Returns StubNeuralReranker if the service is not yet initialized.
     */
    fun createReranker(): NeuralReranker {
        val dict = dictionary ?: return StubNeuralReranker()
        val ngram = ngramModel ?: return StubNeuralReranker()
        return ContextReranker(dict, ngram, maxUnigramFreq)
    }

    private var maxUnigramFreq: Int = 1

    // ---- Autocorrect ----

    /**
     * Evaluate a word for auto-correction at commit time.
     * Returns null if autocorrect is not initialized.
     */
    fun getAutoCorrection(word: String): AutoCorrectionResult? {
        val eng = engine ?: return null
        val lm = learningManager
        return eng.evaluateAutoCorrection(
            word = word,
            userDictionaryContains = { w -> lm?.userDictionary?.contains(w) == true }
        )
    }

    /**
     * Add a word to the autocorrect block list (user rejected this correction).
     */
    fun addToBlockList(word: String) {
        autoCorrect?.addToBlockList(word)
    }

    fun serializeBlockList(): String = autoCorrect?.serializeBlockList() ?: ""

    fun loadBlockList(data: String) {
        autoCorrect?.deserializeBlockList(data)
    }
}

/**
 * Adapts LearningManager to the LearningScorer interface used by PredictionEngine.
 * The currentTime is set by the platform layer before each prediction cycle.
 */
private class LearningManagerScorer(
    private val manager: LearningManager
) : LearningScorer {
    var currentTime: Long = 0L

    override fun getUserFrequencyScore(word: String): Float =
        manager.getUserScore(word, currentTime)

    override fun getUserBigramScore(prevWord: String, word: String): Float =
        manager.getUserBigramScore(prevWord, word)

    override fun getUserContinuations(prevWord: String, maxResults: Int): List<Pair<String, Int>> =
        manager.getUserContinuations(prevWord, maxResults)
}
