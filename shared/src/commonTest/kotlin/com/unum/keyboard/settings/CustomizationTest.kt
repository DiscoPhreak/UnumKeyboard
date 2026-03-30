package com.unum.keyboard.settings

import kotlin.test.*

class KeyboardThemeTest {

    @Test
    fun defaultThemeIsAmoledDark() {
        val theme = KeyboardTheme.fromId("amoled_dark")
        assertEquals("AMOLED Dark", theme.name)
        assertEquals(0xFF000000, theme.backgroundColor)
    }

    @Test
    fun allBuiltInThemesHaveUniqueIds() {
        val ids = KeyboardTheme.builtInThemes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun fourBuiltInThemes() {
        assertEquals(4, KeyboardTheme.builtInThemes.size)
    }

    @Test
    fun fromIdReturnsCorrectTheme() {
        assertEquals("Classic Dark", KeyboardTheme.fromId("classic_dark").name)
        assertEquals("Light", KeyboardTheme.fromId("light").name)
        assertEquals("Midnight Blue", KeyboardTheme.fromId("midnight_blue").name)
    }

    @Test
    fun unknownIdFallsBackToAmoled() {
        val theme = KeyboardTheme.fromId("nonexistent")
        assertEquals("amoled_dark", theme.id)
    }

    @Test
    fun lightThemeHasWhiteKeys() {
        val light = KeyboardTheme.LIGHT
        assertEquals(0xFFFFFFFF, light.keyBackgroundColor)
    }

    @Test
    fun allThemesHaveNonZeroColors() {
        for (theme in KeyboardTheme.builtInThemes) {
            assertTrue(theme.backgroundColor != 0L, "${theme.name} background is zero")
            assertTrue(theme.keyBackgroundColor != 0L, "${theme.name} key bg is zero")
            assertTrue(theme.keyTextColor != 0L, "${theme.name} text is zero")
        }
    }

    @Test
    fun allThemesHaveNonEmptyNames() {
        KeyboardTheme.builtInThemes.forEach {
            assertTrue(it.name.isNotBlank())
            assertTrue(it.id.isNotBlank())
        }
    }
}

class KeyboardConfigTest {

    @Test
    fun defaultValues() {
        val config = KeyboardConfig()
        assertEquals(260f, config.keyboardHeight)
        assertEquals(44f, config.suggestionBarHeight)
        assertEquals(22f, config.keyTextSize)
        assertEquals(300L, config.longPressDelay)
        assertTrue(config.keyPreviewEnabled)
        assertFalse(config.showSpacebarLanguage)
    }

    @Test
    fun serializeAndDeserialize() {
        val config = KeyboardConfig(
            keyboardHeight = 280f,
            keyTextSize = 24f,
            keySpacing = 5f,
            longPressDelay = 400L,
            keyPreviewEnabled = false
        )

        val serialized = config.serialize()
        val restored = KeyboardConfig.deserialize(serialized)

        assertEquals(280f, restored.keyboardHeight)
        assertEquals(24f, restored.keyTextSize)
        assertEquals(5f, restored.keySpacing)
        assertEquals(400L, restored.longPressDelay)
        assertFalse(restored.keyPreviewEnabled)
    }

    @Test
    fun deserializeEmptyReturnsDefaults() {
        val config = KeyboardConfig.deserialize("")
        assertEquals(KeyboardConfig(), config)
    }

    @Test
    fun deserializePartialFillsDefaults() {
        val config = KeyboardConfig.deserialize("keyboardHeight=300")
        assertEquals(300f, config.keyboardHeight)
        assertEquals(22f, config.keyTextSize) // default
    }

    @Test
    fun tallPreset() {
        val tall = KeyboardConfig.TALL
        assertEquals(300f, tall.keyboardHeight)
        assertEquals(24f, tall.keyTextSize)
    }

    @Test
    fun compactPreset() {
        val compact = KeyboardConfig.COMPACT
        assertEquals(220f, compact.keyboardHeight)
        assertEquals(20f, compact.keyTextSize)
        assertEquals(3f, compact.keySpacing)
    }

    @Test
    fun serializeRoundTripsAllFields() {
        val original = KeyboardConfig(
            keyboardHeight = 240f,
            suggestionBarHeight = 40f,
            horizontalPadding = 5f,
            verticalPadding = 8f,
            keySpacing = 6f,
            keyCornerRadius = 12f,
            keyTextSize = 20f,
            specialKeyTextSize = 12f,
            flickHintTextSize = 8f,
            longPressDelay = 500L,
            backspaceRepeatDelay = 300L,
            backspaceRepeatInterval = 30L,
            keyPreviewEnabled = false,
            showSpacebarLanguage = true
        )

        val restored = KeyboardConfig.deserialize(original.serialize())
        assertEquals(original, restored)
    }

    @Test
    fun deserializeMalformedLinesIgnored() {
        val config = KeyboardConfig.deserialize("garbage\nkeyboardHeight=300\n=bad\nnoeq")
        assertEquals(300f, config.keyboardHeight)
        // Other fields should be defaults
        assertEquals(22f, config.keyTextSize)
    }
}
