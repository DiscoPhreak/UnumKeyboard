package com.unum.keyboard.platform

import android.content.SharedPreferences
import androidx.core.content.edit

actual class PlatformSettings(context: PlatformContext) {
    private val prefs: SharedPreferences =
        context.context.getSharedPreferences("unum_keyboard_prefs", android.content.Context.MODE_PRIVATE)

    actual fun getString(key: String, default: String): String =
        prefs.getString(key, default) ?: default

    actual fun putString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    actual fun putBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    actual fun getInt(key: String, default: Int): Int =
        prefs.getInt(key, default)

    actual fun putInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }
}
