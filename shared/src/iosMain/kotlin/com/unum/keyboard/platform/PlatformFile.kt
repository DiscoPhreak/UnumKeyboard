package com.unum.keyboard.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class PlatformFile(private val path: String) {
    actual fun readText(): String {
        return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: ""
    }

    actual fun writeText(text: String) {
        val nsString = NSString.create(string = text)
        nsString.writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual fun exists(): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }
}
