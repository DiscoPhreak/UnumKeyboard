package com.unum.keyboard.prediction

/**
 * High-level prediction service that manages the prediction engine lifecycle
 * and provides a simple API for the keyboard UI.
 */
class PredictionService {
    private var dictionary: TrieDictionary? = null
    private var ngramModel: NGramModel? = null
    private var autoCorrect: AutoCorrect? = null
    private var engine: PredictionEngine? = null

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

        dictionary = dict
        ngramModel = ngram
        autoCorrect = ac
        engine = pe
        isLoaded = true
    }

    /**
     * Get predictions for the current typing state.
     */
    fun predict(maxResults: Int = 3): List<Prediction> {
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
    }
}
