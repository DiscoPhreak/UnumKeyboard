package com.unum.keyboard.stats

import kotlin.test.*

class UserFrequencyModelTest {

    private lateinit var model: UserFrequencyModel

    @BeforeTest
    fun setUp() {
        model = UserFrequencyModel()
    }

    @Test
    fun emptyModelReturnsZeroScore() {
        assertEquals(0f, model.getScore("hello", 1000L))
    }

    @Test
    fun recordUsageIncrementsCount() {
        model.recordUsage("hello", 1000L)
        assertEquals(1, model.getCount("hello"))
        model.recordUsage("hello", 2000L)
        assertEquals(2, model.getCount("hello"))
    }

    @Test
    fun caseInsensitive() {
        model.recordUsage("Hello", 1000L)
        assertEquals(1, model.getCount("hello"))
        assertEquals(1, model.getCount("HELLO"))
        assertTrue(model.hasWord("Hello"))
    }

    @Test
    fun scorePositiveAfterUsage() {
        model.recordUsage("hello", 1000L)
        val score = model.getScore("hello", 1000L)
        assertTrue(score > 0f)
    }

    @Test
    fun recentWordScoresHigher() {
        model.recordUsage("old", 0L)
        model.recordUsage("new", 1000L)
        // Both have count=1, but "new" is more recent
        val oldScore = model.getScore("old", 1000L)
        val newScore = model.getScore("new", 1000L)
        assertTrue(newScore > oldScore, "Recent word should score higher: new=$newScore, old=$oldScore")
    }

    @Test
    fun frequentWordScoresHigher() {
        model.recordUsage("rare", 1000L)
        model.recordUsage("common", 1000L)
        model.recordUsage("common", 1000L)
        model.recordUsage("common", 1000L)
        val rareScore = model.getScore("rare", 1000L)
        val commonScore = model.getScore("common", 1000L)
        assertTrue(commonScore > rareScore)
    }

    @Test
    fun topWordsReturnsSorted() {
        model.recordUsage("a", 100L)
        model.recordUsage("b", 100L)
        model.recordUsage("b", 200L)
        model.recordUsage("c", 100L)
        model.recordUsage("c", 200L)
        model.recordUsage("c", 300L)

        val top = model.topWords(2)
        assertEquals(2, top.size)
        assertEquals("c", top[0].first)
        assertEquals(3, top[0].second)
        assertEquals("b", top[1].first)
    }

    @Test
    fun serializeAndDeserialize() {
        model.recordUsage("hello", 1000L)
        model.recordUsage("world", 2000L)
        model.recordUsage("hello", 3000L)

        val serialized = model.serialize()
        val restored = UserFrequencyModel()
        restored.deserialize(serialized)

        assertEquals(2, restored.getCount("hello"))
        assertEquals(1, restored.getCount("world"))
        assertEquals(model.size, restored.size)
    }

    @Test
    fun serializePipeInWord() {
        model.recordUsage("test|word", 1000L)
        val serialized = model.serialize()
        val restored = UserFrequencyModel()
        restored.deserialize(serialized)
        assertEquals(1, restored.getCount("test|word"))
    }

    @Test
    fun clearResetsModel() {
        model.recordUsage("hello", 1000L)
        model.clear()
        assertEquals(0, model.size)
        assertEquals(0, model.getCount("hello"))
    }

    @Test
    fun evictsWhenOverCapacity() {
        val small = UserFrequencyModel(maxEntries = 10)
        for (i in 0..15) {
            small.recordUsage("word$i", i.toLong() * 1000)
        }
        assertTrue(small.size <= 10)
    }

    @Test
    fun deserializeEmptyString() {
        model.deserialize("")
        assertEquals(0, model.size)
    }

    @Test
    fun scoreDecaysWithAge() {
        model.recordUsage("hello", 0L)
        val score1 = model.getScore("hello", 0L)
        val score2 = model.getScore("hello", UserFrequencyModel.HALF_LIFE_MS.toLong())
        assertTrue(score1 > score2, "Score should decay with age")
    }
}

class UserBigramModelTest {

    private lateinit var model: UserBigramModel

    @BeforeTest
    fun setUp() {
        model = UserBigramModel()
    }

    @Test
    fun emptyModelReturnsZeroScore() {
        assertEquals(0f, model.getScore("hello", "world"))
    }

    @Test
    fun recordAndScore() {
        model.recordBigram("thank", "you")
        val score = model.getScore("thank", "you")
        assertTrue(score > 0f)
        assertEquals(1f, score) // only one continuation = 100%
    }

    @Test
    fun multipleContinuationsNormalize() {
        model.recordBigram("i", "am")
        model.recordBigram("i", "am")
        model.recordBigram("i", "have")

        val amScore = model.getScore("i", "am")
        val haveScore = model.getScore("i", "have")
        // "am" = 2/3, "have" = 1/3
        assertTrue(amScore > haveScore)
        assertEquals(2f / 3f, amScore, 0.01f)
    }

    @Test
    fun caseInsensitive() {
        model.recordBigram("Thank", "You")
        assertTrue(model.getScore("thank", "you") > 0f)
    }

    @Test
    fun getContinuationsReturnsSorted() {
        model.recordBigram("i", "am")
        model.recordBigram("i", "am")
        model.recordBigram("i", "have")
        model.recordBigram("i", "was")

        val cont = model.getContinuations("i", 2)
        assertEquals(2, cont.size)
        assertEquals("am", cont[0].first)
        assertEquals(2, cont[0].second)
    }

    @Test
    fun serializeAndDeserialize() {
        model.recordBigram("hello", "world")
        model.recordBigram("hello", "world")
        model.recordBigram("good", "morning")

        val serialized = model.serialize()
        val restored = UserBigramModel()
        restored.deserialize(serialized)

        assertEquals(model.getScore("hello", "world"), restored.getScore("hello", "world"))
        assertEquals(model.getScore("good", "morning"), restored.getScore("good", "morning"))
    }

    @Test
    fun clearResetsModel() {
        model.recordBigram("a", "b")
        model.clear()
        assertEquals(0, model.size)
        assertEquals(0f, model.getScore("a", "b"))
    }

    @Test
    fun deserializeEmptyString() {
        model.deserialize("")
        assertEquals(0, model.size)
    }

    @Test
    fun emptyContinuationsForUnknownWord() {
        val cont = model.getContinuations("unknown")
        assertTrue(cont.isEmpty())
    }
}

class UserDictionaryTest {

    private lateinit var dict: UserDictionary

    @BeforeTest
    fun setUp() {
        dict = UserDictionary()
    }

    @Test
    fun addAndContains() {
        dict.addWord("kotlin", 1000L)
        assertTrue(dict.contains("kotlin"))
        assertTrue(dict.contains("Kotlin"))
    }

    @Test
    fun removeWord() {
        dict.addWord("test", 1000L)
        dict.removeWord("test")
        assertFalse(dict.contains("test"))
    }

    @Test
    fun duplicateAddIgnored() {
        dict.addWord("test", 1000L)
        dict.addWord("test", 2000L)
        assertEquals(1, dict.size)
    }

    @Test
    fun allWords() {
        dict.addWord("apple", 100L)
        dict.addWord("banana", 200L)
        val words = dict.allWords()
        assertEquals(2, words.size)
        assertTrue(words.contains("apple"))
        assertTrue(words.contains("banana"))
    }

    @Test
    fun serializeAndDeserialize() {
        dict.addWord("hello", 1000L)
        dict.addWord("world", 2000L)

        val serialized = dict.serialize()
        val restored = UserDictionary()
        restored.deserialize(serialized)

        assertTrue(restored.contains("hello"))
        assertTrue(restored.contains("world"))
        assertEquals(dict.size, restored.size)
    }

    @Test
    fun clearResets() {
        dict.addWord("test", 1000L)
        dict.clear()
        assertEquals(0, dict.size)
        assertFalse(dict.contains("test"))
    }

    @Test
    fun evictsOldestWhenOverCapacity() {
        val small = UserDictionary(maxEntries = 5)
        for (i in 0..9) {
            small.addWord("word$i", i.toLong() * 1000)
        }
        assertTrue(small.size <= 5)
    }
}

class LearningManagerTest {

    private lateinit var manager: LearningManager

    @BeforeTest
    fun setUp() {
        manager = LearningManager()
    }

    @Test
    fun wordCommitRecordsFrequency() {
        manager.onWordCommitted("hello", 1000L)
        assertTrue(manager.frequencyModel.hasWord("hello"))
        assertEquals(1, manager.frequencyModel.getCount("hello"))
    }

    @Test
    fun wordCommitRecordsBigram() {
        manager.onWordCommitted("hello", 1000L)
        manager.onWordCommitted("world", 2000L)
        assertTrue(manager.bigramModel.getScore("hello", "world") > 0f)
    }

    @Test
    fun sentenceBoundaryResetsBigramContext() {
        manager.onWordCommitted("hello", 1000L)
        manager.onSentenceBoundary()
        manager.onWordCommitted("world", 2000L)
        // No bigram between "hello" and "world" across sentence boundary
        assertEquals(0f, manager.bigramModel.getScore("hello", "world"))
    }

    @Test
    fun ignoresShortWords() {
        manager.onWordCommitted("a", 1000L)
        assertEquals(0, manager.frequencyModel.size)
    }

    @Test
    fun ignoresBlankWords() {
        manager.onWordCommitted("", 1000L)
        manager.onWordCommitted("  ", 2000L)
        assertEquals(0, manager.frequencyModel.size)
    }

    @Test
    fun getUserScoreReturnsScore() {
        manager.onWordCommitted("hello", 1000L)
        val score = manager.getUserScore("hello", 1000L)
        assertTrue(score > 0f)
    }

    @Test
    fun getUserBigramScoreReturnsScore() {
        manager.onWordCommitted("good", 1000L)
        manager.onWordCommitted("morning", 2000L)
        val score = manager.getUserBigramScore("good", "morning")
        assertTrue(score > 0f)
    }

    @Test
    fun serializeAndDeserialize() {
        manager.onWordCommitted("hello", 1000L)
        manager.onWordCommitted("world", 2000L)
        manager.addUserWord("kotlin", 3000L)

        val serialized = manager.serialize()
        val restored = LearningManager()
        restored.deserialize(serialized)

        assertEquals(
            manager.frequencyModel.getCount("hello"),
            restored.frequencyModel.getCount("hello")
        )
        assertTrue(restored.userDictionary.contains("kotlin"))
    }

    @Test
    fun clearResetsEverything() {
        manager.onWordCommitted("hello", 1000L)
        manager.addUserWord("test", 2000L)
        manager.clear()
        assertEquals(0, manager.frequencyModel.size)
        assertEquals(0, manager.bigramModel.size)
        assertEquals(0, manager.userDictionary.size)
    }

    @Test
    fun addAndRemoveUserWord() {
        manager.addUserWord("kotlin", 1000L)
        assertTrue(manager.userDictionary.contains("kotlin"))
        manager.removeUserWord("kotlin")
        assertFalse(manager.userDictionary.contains("kotlin"))
    }

    @Test
    fun getUserContinuationsReturnsResults() {
        manager.onWordCommitted("on", 1000L)
        manager.onWordCommitted("my", 2000L)
        manager.onWordCommitted("on", 3000L)
        manager.onWordCommitted("my", 4000L)

        val continuations = manager.getUserContinuations("on", 5)
        assertTrue(continuations.isNotEmpty())
        assertEquals("my", continuations[0].first)
    }
}
