package com.unum.keyboard.settings

/**
 * Configurable keyboard dimensions and behavior settings.
 * All dimension values are in density-independent pixels (dp).
 */
data class KeyboardConfig(
    // Dimensions
    val keyboardHeight: Float = DEFAULT_KEYBOARD_HEIGHT,
    val suggestionBarHeight: Float = DEFAULT_SUGGESTION_BAR_HEIGHT,
    val horizontalPadding: Float = DEFAULT_HORIZONTAL_PADDING,
    val verticalPadding: Float = DEFAULT_VERTICAL_PADDING,
    val keySpacing: Float = DEFAULT_KEY_SPACING,
    val keyCornerRadius: Float = DEFAULT_KEY_CORNER_RADIUS,

    // Text sizes
    val keyTextSize: Float = DEFAULT_KEY_TEXT_SIZE,
    val specialKeyTextSize: Float = DEFAULT_SPECIAL_KEY_TEXT_SIZE,
    val flickHintTextSize: Float = DEFAULT_FLICK_HINT_TEXT_SIZE,

    // Long-press behavior
    val longPressDelay: Long = DEFAULT_LONG_PRESS_DELAY,
    val backspaceRepeatDelay: Long = DEFAULT_BACKSPACE_REPEAT_DELAY,
    val backspaceRepeatInterval: Long = DEFAULT_BACKSPACE_REPEAT_INTERVAL,

    // Popup / key preview
    val keyPreviewEnabled: Boolean = true,

    // Spacebar label
    val showSpacebarLanguage: Boolean = false
) {
    /**
     * Serialize to "key=value" pairs for persistence.
     */
    fun serialize(): String = buildString {
        append("keyboardHeight=$keyboardHeight\n")
        append("suggestionBarHeight=$suggestionBarHeight\n")
        append("horizontalPadding=$horizontalPadding\n")
        append("verticalPadding=$verticalPadding\n")
        append("keySpacing=$keySpacing\n")
        append("keyCornerRadius=$keyCornerRadius\n")
        append("keyTextSize=$keyTextSize\n")
        append("specialKeyTextSize=$specialKeyTextSize\n")
        append("flickHintTextSize=$flickHintTextSize\n")
        append("longPressDelay=$longPressDelay\n")
        append("backspaceRepeatDelay=$backspaceRepeatDelay\n")
        append("backspaceRepeatInterval=$backspaceRepeatInterval\n")
        append("keyPreviewEnabled=$keyPreviewEnabled\n")
        append("showSpacebarLanguage=$showSpacebarLanguage")
    }

    companion object {
        const val DEFAULT_KEYBOARD_HEIGHT = 260f
        const val DEFAULT_SUGGESTION_BAR_HEIGHT = 44f
        const val DEFAULT_HORIZONTAL_PADDING = 3f
        const val DEFAULT_VERTICAL_PADDING = 6f
        const val DEFAULT_KEY_SPACING = 4f
        const val DEFAULT_KEY_CORNER_RADIUS = 8f
        const val DEFAULT_KEY_TEXT_SIZE = 22f
        const val DEFAULT_SPECIAL_KEY_TEXT_SIZE = 14f
        const val DEFAULT_FLICK_HINT_TEXT_SIZE = 10f
        const val DEFAULT_LONG_PRESS_DELAY = 300L
        const val DEFAULT_BACKSPACE_REPEAT_DELAY = 400L
        const val DEFAULT_BACKSPACE_REPEAT_INTERVAL = 50L

        /** Preset for taller keyboards */
        val TALL = KeyboardConfig(keyboardHeight = 300f, keyTextSize = 24f)

        /** Preset for compact keyboards */
        val COMPACT = KeyboardConfig(
            keyboardHeight = 220f,
            keyTextSize = 20f,
            specialKeyTextSize = 12f,
            keySpacing = 3f,
            horizontalPadding = 2f,
            verticalPadding = 4f
        )

        /**
         * Deserialize from "key=value" pairs.
         */
        fun deserialize(data: String): KeyboardConfig {
            if (data.isBlank()) return KeyboardConfig()

            val map = mutableMapOf<String, String>()
            for (line in data.lineSequence()) {
                val trimmed = line.trim()
                val eq = trimmed.indexOf('=')
                if (eq > 0) {
                    map[trimmed.substring(0, eq)] = trimmed.substring(eq + 1)
                }
            }

            return KeyboardConfig(
                keyboardHeight = map["keyboardHeight"]?.toFloatOrNull() ?: DEFAULT_KEYBOARD_HEIGHT,
                suggestionBarHeight = map["suggestionBarHeight"]?.toFloatOrNull() ?: DEFAULT_SUGGESTION_BAR_HEIGHT,
                horizontalPadding = map["horizontalPadding"]?.toFloatOrNull() ?: DEFAULT_HORIZONTAL_PADDING,
                verticalPadding = map["verticalPadding"]?.toFloatOrNull() ?: DEFAULT_VERTICAL_PADDING,
                keySpacing = map["keySpacing"]?.toFloatOrNull() ?: DEFAULT_KEY_SPACING,
                keyCornerRadius = map["keyCornerRadius"]?.toFloatOrNull() ?: DEFAULT_KEY_CORNER_RADIUS,
                keyTextSize = map["keyTextSize"]?.toFloatOrNull() ?: DEFAULT_KEY_TEXT_SIZE,
                specialKeyTextSize = map["specialKeyTextSize"]?.toFloatOrNull() ?: DEFAULT_SPECIAL_KEY_TEXT_SIZE,
                flickHintTextSize = map["flickHintTextSize"]?.toFloatOrNull() ?: DEFAULT_FLICK_HINT_TEXT_SIZE,
                longPressDelay = map["longPressDelay"]?.toLongOrNull() ?: DEFAULT_LONG_PRESS_DELAY,
                backspaceRepeatDelay = map["backspaceRepeatDelay"]?.toLongOrNull() ?: DEFAULT_BACKSPACE_REPEAT_DELAY,
                backspaceRepeatInterval = map["backspaceRepeatInterval"]?.toLongOrNull() ?: DEFAULT_BACKSPACE_REPEAT_INTERVAL,
                keyPreviewEnabled = map["keyPreviewEnabled"]?.toBooleanStrictOrNull() ?: true,
                showSpacebarLanguage = map["showSpacebarLanguage"]?.toBooleanStrictOrNull() ?: false
            )
        }
    }
}
