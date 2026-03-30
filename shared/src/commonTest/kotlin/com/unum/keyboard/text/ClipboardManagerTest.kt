package com.unum.keyboard.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClipboardManagerTest {

    // ---- Basic Operations ----

    @Test
    fun addClip_addsToHistory() {
        val mgr = ClipboardManager()
        mgr.addClip("hello world", 1000L)
        assertEquals(1, mgr.size)
        assertEquals("hello world", mgr.history[0].text)
    }

    @Test
    fun addClip_newestFirst() {
        val mgr = ClipboardManager()
        mgr.addClip("first", 1000L)
        mgr.addClip("second", 2000L)
        assertEquals("second", mgr.history[0].text)
        assertEquals("first", mgr.history[1].text)
    }

    @Test
    fun addClip_emptyText_ignored() {
        val mgr = ClipboardManager()
        mgr.addClip("", 1000L)
        mgr.addClip("   ", 2000L)
        assertEquals(0, mgr.size)
    }

    @Test
    fun addClip_duplicate_movesToTop() {
        val mgr = ClipboardManager()
        mgr.addClip("hello", 1000L)
        mgr.addClip("world", 2000L)
        mgr.addClip("hello", 3000L)
        assertEquals(2, mgr.size)
        assertEquals("hello", mgr.history[0].text)
    }

    @Test
    fun addClip_trimsToMaxHistory() {
        val mgr = ClipboardManager(maxHistory = 3)
        mgr.addClip("one", 1000L)
        mgr.addClip("two", 2000L)
        mgr.addClip("three", 3000L)
        mgr.addClip("four", 4000L)
        assertEquals(3, mgr.size)
        assertFalse(mgr.history.any { it.text == "one" }, "Oldest should be removed")
    }

    // ---- Pin/Unpin ----

    @Test
    fun pinClip_movesToTop() {
        val mgr = ClipboardManager()
        mgr.addClip("first", 1000L)
        mgr.addClip("second", 2000L)
        mgr.pinClip(1) // pin "first"
        assertTrue(mgr.history[0].isPinned)
        assertEquals("first", mgr.history[0].text)
    }

    @Test
    fun pinnedClip_notRemovedByNewEntries() {
        val mgr = ClipboardManager(maxHistory = 3)
        mgr.addClip("pinned", 1000L)
        mgr.pinClip(0)
        mgr.addClip("a", 2000L)
        mgr.addClip("b", 3000L)
        mgr.addClip("c", 4000L)
        assertTrue(mgr.history.any { it.text == "pinned" && it.isPinned })
    }

    @Test
    fun unpinClip_works() {
        val mgr = ClipboardManager()
        mgr.addClip("test", 1000L)
        mgr.pinClip(0)
        assertTrue(mgr.history[0].isPinned)
        mgr.unpinClip(0)
        assertFalse(mgr.history[0].isPinned)
    }

    @Test
    fun pinnedClips_listedSeparately() {
        val mgr = ClipboardManager()
        mgr.addClip("a", 1000L)
        mgr.addClip("b", 2000L)
        mgr.pinClip(0) // pin "b"
        assertEquals(1, mgr.pinnedClips.size)
        assertEquals(1, mgr.recentClips.size)
    }

    // ---- Remove / Clear ----

    @Test
    fun removeClip_works() {
        val mgr = ClipboardManager()
        mgr.addClip("hello", 1000L)
        mgr.addClip("world", 2000L)
        mgr.removeClip(0) // remove "world" (newest)
        assertEquals(1, mgr.size)
        assertEquals("hello", mgr.history[0].text)
    }

    @Test
    fun clearHistory_keepsOnlyPinned() {
        val mgr = ClipboardManager()
        mgr.addClip("a", 1000L)
        mgr.addClip("b", 2000L)
        mgr.pinClip(0) // pin "b"
        mgr.clearHistory()
        assertEquals(1, mgr.size)
        assertEquals("b", mgr.history[0].text)
    }

    @Test
    fun clearAll_removesEverything() {
        val mgr = ClipboardManager()
        mgr.addClip("a", 1000L)
        mgr.pinClip(0)
        mgr.addClip("b", 2000L)
        mgr.clearAll()
        assertEquals(0, mgr.size)
    }

    // ---- Search ----

    @Test
    fun search_findsMatchingClips() {
        val mgr = ClipboardManager()
        mgr.addClip("hello world", 1000L)
        mgr.addClip("goodbye world", 2000L)
        mgr.addClip("hello there", 3000L)
        val results = mgr.search("hello")
        assertEquals(2, results.size)
    }

    @Test
    fun search_caseInsensitive() {
        val mgr = ClipboardManager()
        mgr.addClip("Hello World", 1000L)
        val results = mgr.search("hello")
        assertEquals(1, results.size)
    }

    @Test
    fun search_emptyQuery_returnsAll() {
        val mgr = ClipboardManager()
        mgr.addClip("a", 1000L)
        mgr.addClip("b", 2000L)
        val results = mgr.search("")
        assertEquals(2, results.size)
    }

    // ---- Serialization ----

    @Test
    fun serialize_roundTrip() {
        val mgr = ClipboardManager()
        mgr.addClip("hello world", 1000L)
        mgr.addClip("line1\nline2", 2000L)
        mgr.addClip("pipe|test", 3000L)
        mgr.pinClip(1) // pin "line1\nline2"

        val data = mgr.serialize()

        val mgr2 = ClipboardManager()
        mgr2.deserialize(data)

        assertEquals(mgr.size, mgr2.size)
        assertEquals(mgr.history.map { it.text }, mgr2.history.map { it.text })
        assertEquals(mgr.history.map { it.isPinned }, mgr2.history.map { it.isPinned })
    }

    @Test
    fun deserialize_emptyString_noError() {
        val mgr = ClipboardManager()
        mgr.deserialize("")
        assertEquals(0, mgr.size)
    }

    // ---- ClipEntry ----

    @Test
    fun clipEntry_preview_truncates() {
        val longText = "a".repeat(100)
        val entry = ClipEntry(longText, 1000L)
        assertTrue(entry.preview.length <= ClipEntry.MAX_PREVIEW_LENGTH + 1) // +1 for …
        assertTrue(entry.preview.endsWith("…"))
    }

    @Test
    fun clipEntry_preview_firstLine() {
        val entry = ClipEntry("first line\nsecond line", 1000L)
        assertEquals("first line", entry.preview)
    }

    @Test
    fun clipEntry_preview_short() {
        val entry = ClipEntry("short", 1000L)
        assertEquals("short", entry.preview)
    }
}

class SlideboardManagerTest {

    @Test
    fun addSnippet_works() {
        val mgr = SlideboardManager()
        assertTrue(mgr.addSnippet("Email", "test@example.com"))
        assertEquals(1, mgr.size)
    }

    @Test
    fun addSnippet_duplicateLabel_rejected() {
        val mgr = SlideboardManager()
        mgr.addSnippet("Email", "test@example.com")
        assertFalse(mgr.addSnippet("Email", "other@example.com"))
    }

    @Test
    fun addSnippet_blank_rejected() {
        val mgr = SlideboardManager()
        assertFalse(mgr.addSnippet("", "text"))
        assertFalse(mgr.addSnippet("label", ""))
    }

    @Test
    fun addSnippet_maxCapacity() {
        val mgr = SlideboardManager(maxSnippets = 2)
        assertTrue(mgr.addSnippet("a", "text1"))
        assertTrue(mgr.addSnippet("b", "text2"))
        assertFalse(mgr.addSnippet("c", "text3"))
    }

    @Test
    fun recordUsage_sortsByUsage() {
        val mgr = SlideboardManager()
        mgr.addSnippet("rarely", "text1")
        mgr.addSnippet("often", "text2")
        mgr.recordUsage("often")
        mgr.recordUsage("often")
        mgr.recordUsage("often")
        assertEquals("often", mgr.snippets[0].label)
    }

    @Test
    fun findByLabel_caseInsensitive() {
        val mgr = SlideboardManager()
        mgr.addSnippet("My Email", "test@example.com")
        val found = mgr.findByLabel("my email")
        assertEquals("My Email", found?.label)
    }

    @Test
    fun getByCategory_filters() {
        val mgr = SlideboardManager()
        mgr.addSnippet("Email", "test@example.com", SnippetCategory.EMAIL)
        mgr.addSnippet("Home", "123 Main St", SnippetCategory.ADDRESS)
        mgr.addSnippet("Work", "work@example.com", SnippetCategory.EMAIL)
        val emails = mgr.getByCategory(SnippetCategory.EMAIL)
        assertEquals(2, emails.size)
    }

    @Test
    fun serialize_roundTrip() {
        val mgr = SlideboardManager()
        mgr.addSnippet("Email", "test@example.com", SnippetCategory.EMAIL)
        mgr.addSnippet("Note", "line1\nline2", SnippetCategory.GENERAL)
        mgr.recordUsage("Email")

        val data = mgr.serialize()

        val mgr2 = SlideboardManager()
        mgr2.deserialize(data)

        assertEquals(mgr.size, mgr2.size)
        val orig = mgr.snippets.sortedBy { it.label }
        val restored = mgr2.snippets.sortedBy { it.label }
        assertEquals(orig.map { it.label }, restored.map { it.label })
        assertEquals(orig.map { it.text }, restored.map { it.text })
    }

    @Test
    fun clearAll_works() {
        val mgr = SlideboardManager()
        mgr.addSnippet("a", "text1")
        mgr.addSnippet("b", "text2")
        mgr.clearAll()
        assertEquals(0, mgr.size)
    }

    @Test
    fun removeSnippet_works() {
        val mgr = SlideboardManager()
        mgr.addSnippet("a", "text1")
        mgr.addSnippet("b", "text2")
        mgr.removeSnippet(0) // remove first in sorted order
        assertEquals(1, mgr.size)
    }
}
