package com.unum.keyboard.layout

import com.unum.keyboard.core.KeyboardState
import com.unum.keyboard.core.ShiftState
import kotlin.test.*

class LayoutRegistryTest {

    @Test
    fun hasEnglishLocale() {
        assertTrue(LayoutRegistry.hasLocale("en-US"))
    }

    @Test
    fun hasSpanishLocale() {
        assertTrue(LayoutRegistry.hasLocale("es-ES"))
    }

    @Test
    fun hasFrenchLocale() {
        assertTrue(LayoutRegistry.hasLocale("fr-FR"))
    }

    @Test
    fun hasGermanLocale() {
        assertTrue(LayoutRegistry.hasLocale("de-DE"))
    }

    @Test
    fun hasPortugueseLocale() {
        assertTrue(LayoutRegistry.hasLocale("pt-BR"))
    }

    @Test
    fun fiveLocalesAvailable() {
        assertEquals(5, LayoutRegistry.availableLocales().size)
    }

    @Test
    fun unknownLocaleFallsBackToEnUs() {
        val layouts = LayoutRegistry.getLayouts("xx-XX")
        assertEquals("en-US", layouts.locale)
    }

    @Test
    fun eachLocaleHasFourLayouts() {
        for (locale in LayoutRegistry.availableLocales()) {
            val layouts = LayoutRegistry.getLayouts(locale)
            assertNotNull(layouts.lowercase)
            assertNotNull(layouts.uppercase)
            assertNotNull(layouts.symbols)
            assertNotNull(layouts.symbols2)
        }
    }

    @Test
    fun eachLocaleHasDisplayName() {
        val names = LayoutRegistry.localeDisplayNames()
        assertEquals(5, names.size)
        assertTrue(names.any { it.second == "English (US)" })
        assertTrue(names.any { it.second == "Español" })
        assertTrue(names.any { it.second == "Français" })
        assertTrue(names.any { it.second == "Deutsch" })
    }

    @Test
    fun frenchLayoutIsAzerty() {
        val fr = LayoutRegistry.getLayouts("fr-FR")
        val firstRow = fr.lowercase.rows[0].keys
        // AZERTY: first row starts with a, z, e, r, t, y
        assertEquals("a", firstRow[0].primary)
        assertEquals("z", firstRow[1].primary)
    }

    @Test
    fun germanLayoutIsQwertz() {
        val de = LayoutRegistry.getLayouts("de-DE")
        val firstRow = de.lowercase.rows[0].keys
        // QWERTZ: z and y are swapped compared to QWERTY
        assertEquals("z", firstRow[5].primary)
        val thirdRow = de.lowercase.rows[2].keys
        assertEquals("y", thirdRow[1].primary)
    }

    @Test
    fun spanishLayoutHasEne() {
        val es = LayoutRegistry.getLayouts("es-ES")
        val secondRow = es.lowercase.rows[1].keys
        assertTrue(secondRow.any { it.primary == "ñ" }, "Spanish layout should have ñ")
    }

    @Test
    fun portugueseLayoutHasCedilla() {
        val pt = LayoutRegistry.getLayouts("pt-BR")
        val secondRow = pt.lowercase.rows[1].keys
        assertTrue(secondRow.any { it.primary == "ç" }, "Portuguese layout should have ç")
    }

    @Test
    fun germanHasUmlautFlicks() {
        val de = LayoutRegistry.getLayouts("de-DE")
        val firstRow = de.lowercase.rows[0].keys
        val uKey = firstRow.find { it.primary == "u" }
        assertEquals("ü", uKey?.flickRight)
        val oKey = firstRow.find { it.primary == "o" }
        assertEquals("ö", oKey?.flickRight)
    }

    @Test
    fun frenchHasAccentFlicks() {
        val fr = LayoutRegistry.getLayouts("fr-FR")
        val firstRow = fr.lowercase.rows[0].keys
        val eKey = firstRow.find { it.primary == "e" }
        assertEquals("é", eKey?.flickRight)
        val aKey = firstRow.find { it.primary == "a" }
        assertEquals("à", aKey?.flickRight)
    }

    @Test
    fun allLayoutsHaveFourRows() {
        for (locale in LayoutRegistry.availableLocales()) {
            val layouts = LayoutRegistry.getLayouts(locale)
            assertEquals(4, layouts.lowercase.rows.size, "$locale lowercase")
            assertEquals(4, layouts.uppercase.rows.size, "$locale uppercase")
        }
    }

    @Test
    fun allLayoutsHaveSpaceKey() {
        for (locale in LayoutRegistry.availableLocales()) {
            val layouts = LayoutRegistry.getLayouts(locale)
            val lastRow = layouts.lowercase.rows.last().keys
            assertTrue(lastRow.any { it.type == KeyType.SPACE }, "$locale missing space key")
        }
    }

    @Test
    fun uppercaseMatchesLowercaseStructure() {
        for (locale in LayoutRegistry.availableLocales()) {
            val layouts = LayoutRegistry.getLayouts(locale)
            assertEquals(
                layouts.lowercase.rows.size,
                layouts.uppercase.rows.size,
                "$locale row count mismatch"
            )
        }
    }
}

class KeyboardStateLocaleTest {

    @Test
    fun defaultLocaleIsEnUs() {
        val state = KeyboardState()
        assertEquals("en-US", state.locale)
    }

    @Test
    fun setLocaleChangesLayout() {
        val state = KeyboardState()
        state.setLocale("fr-FR")
        assertEquals("fr-FR", state.locale)
        // French AZERTY starts with 'a'
        val firstKey = state.currentLayout.rows[0].keys[0]
        assertEquals("a", firstKey.primary)
    }

    @Test
    fun setLocaleResetsToLetters() {
        val state = KeyboardState()
        state.toggleSymbols()
        state.setLocale("es-ES")
        val layout = state.currentLayout
        // Should be in letters mode, not symbols
        assertTrue(layout.id.contains("es-ES"))
    }

    @Test
    fun shiftWorksAcrossLocales() {
        val state = KeyboardState()
        state.setLocale("de-DE")
        state.toggleShift()
        val layout = state.currentLayout
        assertTrue(layout.id.contains("upper"))
    }

    @Test
    fun symbolsSharedAcrossLocales() {
        val state = KeyboardState()
        state.setLocale("es-ES")
        state.toggleSymbols()
        val layout = state.currentLayout
        // Symbols layout is shared (en-US based)
        assertTrue(layout.id.contains("symbols"))
    }

    @Test
    fun unknownLocaleFallsBack() {
        val state = KeyboardState()
        state.setLocale("xx-XX")
        // Should fall back to en-US
        val firstKey = state.currentLayout.rows[0].keys[0]
        assertEquals("q", firstKey.primary) // QWERTY
    }
}
