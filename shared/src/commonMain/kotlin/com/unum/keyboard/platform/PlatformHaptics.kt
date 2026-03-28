package com.unum.keyboard.platform

expect class PlatformHaptics {
    fun keyPress()
    fun longPress()
    fun error()
}
