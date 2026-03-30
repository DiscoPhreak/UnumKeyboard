package com.unum.keyboard.settings

import com.unum.keyboard.platform.PlatformSettings

/**
 * Central preferences for the Unum Keyboard.
 * Wraps platform-specific settings storage.
 */
class KeyboardPreferences(private val settings: PlatformSettings) {

    /** Enable neural reranker for enhanced predictions (uses more battery). Default: ON */
    var enhancedPredictions: Boolean
        get() = settings.getBoolean(KEY_ENHANCED_PREDICTIONS, true)
        set(value) = settings.putBoolean(KEY_ENHANCED_PREDICTIONS, value)

    /** Show prediction suggestions above keyboard. Default: ON */
    var showPredictions: Boolean
        get() = settings.getBoolean(KEY_SHOW_PREDICTIONS, true)
        set(value) = settings.putBoolean(KEY_SHOW_PREDICTIONS, value)

    /** Enable autocorrect. Default: ON */
    var autoCorrectEnabled: Boolean
        get() = settings.getBoolean(KEY_AUTOCORRECT, true)
        set(value) = settings.putBoolean(KEY_AUTOCORRECT, value)

    /** Enable haptic feedback on key press. Default: ON */
    var hapticFeedback: Boolean
        get() = settings.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = settings.putBoolean(KEY_HAPTIC_FEEDBACK, value)

    /** Haptic feedback intensity (0-100). Default: 50 */
    var hapticIntensity: Int
        get() = settings.getInt(KEY_HAPTIC_INTENSITY, 50)
        set(value) = settings.putInt(KEY_HAPTIC_INTENSITY, value.coerceIn(0, 100))

    /** Enable key press sound. Default: OFF */
    var soundFeedback: Boolean
        get() = settings.getBoolean(KEY_SOUND_FEEDBACK, false)
        set(value) = settings.putBoolean(KEY_SOUND_FEEDBACK, value)

    /** Enable gesture/swipe typing. Default: OFF */
    var gestureTypingEnabled: Boolean
        get() = settings.getBoolean(KEY_GESTURE_TYPING, false)
        set(value) = settings.putBoolean(KEY_GESTURE_TYPING, value)

    /** Enable clipboard history tracking. Default: ON */
    var clipboardHistoryEnabled: Boolean
        get() = settings.getBoolean(KEY_CLIPBOARD_HISTORY, true)
        set(value) = settings.putBoolean(KEY_CLIPBOARD_HISTORY, value)

    /** Serialized clipboard history data */
    var clipboardData: String
        get() = settings.getString(KEY_CLIPBOARD_DATA, "")
        set(value) = settings.putString(KEY_CLIPBOARD_DATA, value)

    /** Serialized slideboard snippets data */
    var slideboardData: String
        get() = settings.getString(KEY_SLIDEBOARD_DATA, "")
        set(value) = settings.putString(KEY_SLIDEBOARD_DATA, value)

    /** Enable user learning (frequency, bigrams, auto-dictionary). Default: ON */
    var learningEnabled: Boolean
        get() = settings.getBoolean(KEY_LEARNING_ENABLED, true)
        set(value) = settings.putBoolean(KEY_LEARNING_ENABLED, value)

    /** Serialized user learning data (frequency + bigrams + dictionary) */
    var learningData: String
        get() = settings.getString(KEY_LEARNING_DATA, "")
        set(value) = settings.putString(KEY_LEARNING_DATA, value)

    /** Selected theme ID. Default: amoled_dark */
    var themeId: String
        get() = settings.getString(KEY_THEME_ID, "amoled_dark")
        set(value) = settings.putString(KEY_THEME_ID, value)

    /** Get the current theme object */
    val theme: KeyboardTheme get() = KeyboardTheme.fromId(themeId)

    /** Serialized keyboard config (dimensions, timing) */
    var configData: String
        get() = settings.getString(KEY_CONFIG_DATA, "")
        set(value) = settings.putString(KEY_CONFIG_DATA, value)

    /** Get/set the keyboard config */
    var config: KeyboardConfig
        get() = KeyboardConfig.deserialize(configData)
        set(value) { configData = value.serialize() }

    /** Active input locale. Default: en-US */
    var locale: String
        get() = settings.getString(KEY_LOCALE, "en-US")
        set(value) = settings.putString(KEY_LOCALE, value)

    /** Enabled locales list (comma-separated). Default: en-US */
    var enabledLocales: String
        get() = settings.getString(KEY_ENABLED_LOCALES, "en-US")
        set(value) = settings.putString(KEY_ENABLED_LOCALES, value)

    fun getEnabledLocaleList(): List<String> =
        enabledLocales.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun setEnabledLocaleList(locales: List<String>) {
        enabledLocales = locales.joinToString(",")
    }

    companion object {
        private const val KEY_ENHANCED_PREDICTIONS = "enhanced_predictions"
        private const val KEY_SHOW_PREDICTIONS = "show_predictions"
        private const val KEY_AUTOCORRECT = "autocorrect"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
        private const val KEY_SOUND_FEEDBACK = "sound_feedback"
        private const val KEY_GESTURE_TYPING = "gesture_typing"
        private const val KEY_CLIPBOARD_HISTORY = "clipboard_history"
        private const val KEY_CLIPBOARD_DATA = "clipboard_data"
        private const val KEY_SLIDEBOARD_DATA = "slideboard_data"
        private const val KEY_LEARNING_ENABLED = "learning_enabled"
        private const val KEY_LEARNING_DATA = "learning_data"
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_CONFIG_DATA = "config_data"
        private const val KEY_LOCALE = "locale"
        private const val KEY_ENABLED_LOCALES = "enabled_locales"
    }
}
