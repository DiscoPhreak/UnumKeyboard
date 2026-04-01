package com.unum.keyboard.prediction

class TrieNode {
    val children: MutableMap<Char, TrieNode> = mutableMapOf()
    var isWord: Boolean = false
    var frequency: Int = 0
    var wordId: Int = -1
}

class TrieDictionary {
    private val root = TrieNode()
    private val wordById = mutableMapOf<Int, String>()
    private var nextId = 0

    val wordCount: Int get() = wordById.size

    fun insert(word: String, frequency: Int = 1): Int {
        var node = root
        for (ch in word.lowercase()) {
            node = node.children.getOrPut(ch) { TrieNode() }
        }
        if (!node.isWord) {
            node.wordId = nextId++
            wordById[node.wordId] = word.lowercase()
        }
        node.isWord = true
        node.frequency = maxOf(node.frequency, frequency)
        return node.wordId
    }

    fun contains(word: String): Boolean {
        val node = findNode(word.lowercase())
        return node?.isWord == true
    }

    fun getFrequency(word: String): Int {
        val node = findNode(word.lowercase())
        return if (node?.isWord == true) node.frequency else 0
    }

    fun getWordId(word: String): Int {
        val node = findNode(word.lowercase())
        return if (node?.isWord == true) node.wordId else -1
    }

    fun getWordById(id: Int): String? = wordById[id]

    /**
     * Find all words starting with the given prefix, sorted by frequency descending.
     */
    fun prefixSearch(prefix: String, maxResults: Int = 10): List<WordCandidate> {
        val node = findNode(prefix.lowercase()) ?: return emptyList()
        val results = mutableListOf<WordCandidate>()
        collectWords(node, StringBuilder(prefix.lowercase()), results, maxResults * 3)
        results.sortByDescending { it.frequency }
        return results.take(maxResults)
    }

    private fun collectWords(
        node: TrieNode,
        prefix: StringBuilder,
        results: MutableList<WordCandidate>,
        limit: Int
    ) {
        if (results.size >= limit) return
        if (node.isWord) {
            results.add(WordCandidate(prefix.toString(), node.frequency, node.wordId))
        }
        for ((ch, child) in node.children) {
            prefix.append(ch)
            collectWords(child, prefix, results, limit)
            prefix.deleteAt(prefix.length - 1)
        }
    }

    /**
     * Iterate all words in the dictionary with their frequencies.
     */
    fun forEachWord(callback: (word: String, frequency: Int) -> Unit) {
        for ((id, word) in wordById) {
            val freq = getFrequency(word)
            callback(word, freq)
        }
    }

    private fun findNode(key: String): TrieNode? {
        var node = root
        for (ch in key) {
            node = node.children[ch] ?: return null
        }
        return node
    }

    /**
     * Load from unigrams text format: "word frequency" per line
     */
    fun loadFromUnigrams(text: String) {
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val parts = trimmed.split(WHITESPACE_REGEX, limit = 2)
            if (parts.size == 2) {
                val word = parts[0]
                val freq = parts[1].toIntOrNull() ?: continue
                insert(word, freq)
            }
        }
    }

    companion object {
        private val WHITESPACE_REGEX = "\\s+".toRegex()
    }
}

data class WordCandidate(
    val word: String,
    val frequency: Int,
    val wordId: Int = -1
)
