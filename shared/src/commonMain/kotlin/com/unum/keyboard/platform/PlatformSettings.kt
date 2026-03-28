package com.unum.keyboard.platform

expect class PlatformSettings {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
}
