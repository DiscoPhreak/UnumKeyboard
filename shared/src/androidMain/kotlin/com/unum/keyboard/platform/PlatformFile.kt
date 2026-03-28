package com.unum.keyboard.platform

import java.io.File

actual class PlatformFile(private val file: File) {
    constructor(path: String) : this(File(path))

    actual fun readText(): String = file.readText()
    actual fun writeText(text: String) { file.writeText(text) }
    actual fun exists(): Boolean = file.exists()
}
