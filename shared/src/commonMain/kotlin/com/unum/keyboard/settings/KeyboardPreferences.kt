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

    companion object {
        private const val KEY_ENHANCED_PREDICTIONS = "enhanced_predictions"
        private const val KEY_SHOW_PREDICTIONS = "show_predictions"
        private const val KEY_AUTOCORRECT = "autocorrect"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
        private const val KEY_SOUND_FEEDBACK = "sound_feedback"
        private const val KEY_GESTURE_TYPING = "gesture_typing"
    }
}
