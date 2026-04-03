package com.unum.keyboard.settings

/**
 * Defines the visual theme for the keyboard.
 *
 * Colors are stored as ARGB Long values (e.g., 0xFF000000 for opaque black).
 * Platform renderers convert these to native color types.
 */
data class KeyboardTheme(
    val id: String,
    val name: String,
    // Background
    val backgroundColor: Long,
    // Key colors
    val keyBackgroundColor: Long,
    val keyPressedColor: Long,
    val specialKeyBackgroundColor: Long,
    // Text colors
    val keyTextColor: Long,
    val specialKeyTextColor: Long,
    val flickHintColor: Long,
    // Suggestion bar
    val suggestionBarBackground: Long,
    val suggestionTextColor: Long,
    val suggestionDividerColor: Long,
    // Accent
    val accentColor: Long,
    val trackpadActiveColor: Long
) {
    companion object {
        val AMOLED_DARK = KeyboardTheme(
            id = "amoled_dark",
            name = "AMOLED Dark",
            backgroundColor = 0xFF000000,
            keyBackgroundColor = 0xFF1A1A1A,
            keyPressedColor = 0xFF333333,
            specialKeyBackgroundColor = 0xFF292929,
            keyTextColor = 0xFFFFFFFF,
            specialKeyTextColor = 0xFFBBBBBB,
            flickHintColor = 0xFF666666,
            suggestionBarBackground = 0xFF0A0A0A,
            suggestionTextColor = 0xFFDDDDDD,
            suggestionDividerColor = 0xFF333333,
            accentColor = 0xFF2196F3,
            trackpadActiveColor = 0xFF0D47A1
        )

        val CLASSIC_DARK = KeyboardTheme(
            id = "classic_dark",
            name = "Classic Dark",
            backgroundColor = 0xFF212121,
            keyBackgroundColor = 0xFF424242,
            keyPressedColor = 0xFF616161,
            specialKeyBackgroundColor = 0xFF373737,
            keyTextColor = 0xFFFFFFFF,
            specialKeyTextColor = 0xFFBDBDBD,
            flickHintColor = 0xFF757575,
            suggestionBarBackground = 0xFF1A1A1A,
            suggestionTextColor = 0xFFE0E0E0,
            suggestionDividerColor = 0xFF424242,
            accentColor = 0xFF64B5F6,
            trackpadActiveColor = 0xFF1565C0
        )

        val LIGHT = KeyboardTheme(
            id = "light",
            name = "Light",
            backgroundColor = 0xFFD2D5DB,
            keyBackgroundColor = 0xFFFFFFFF,
            keyPressedColor = 0xFFBDBDBD,
            specialKeyBackgroundColor = 0xFFADB5BD,
            keyTextColor = 0xFF212121,
            specialKeyTextColor = 0xFF616161,
            flickHintColor = 0xFF9E9E9E,
            suggestionBarBackground = 0xFFC8CBD1,
            suggestionTextColor = 0xFF212121,
            suggestionDividerColor = 0xFFBDBDBD,
            accentColor = 0xFF1976D2,
            trackpadActiveColor = 0xFF0D47A1
        )

        val MIDNIGHT_BLUE = KeyboardTheme(
            id = "midnight_blue",
            name = "Midnight Blue",
            backgroundColor = 0xFF0D1B2A,
            keyBackgroundColor = 0xFF1B2838,
            keyPressedColor = 0xFF2C3E50,
            specialKeyBackgroundColor = 0xFF162231,
            keyTextColor = 0xFFE0E6ED,
            specialKeyTextColor = 0xFF8899AA,
            flickHintColor = 0xFF4A5568,
            suggestionBarBackground = 0xFF0A1520,
            suggestionTextColor = 0xFFCCD6E0,
            suggestionDividerColor = 0xFF1B2838,
            accentColor = 0xFF48BB78,
            trackpadActiveColor = 0xFF276749
        )

        val builtInThemes = listOf(AMOLED_DARK, CLASSIC_DARK, LIGHT, MIDNIGHT_BLUE)

        fun fromId(id: String): KeyboardTheme =
            builtInThemes.find { it.id == id } ?: AMOLED_DARK
    }
}
