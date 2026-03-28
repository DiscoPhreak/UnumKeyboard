package com.unum.keyboard.platform

expect class PlatformFile {
    fun readText(): String
    fun writeText(text: String)
    fun exists(): Boolean
}
