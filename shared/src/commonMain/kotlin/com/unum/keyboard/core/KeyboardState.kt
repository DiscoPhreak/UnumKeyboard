package com.unum.keyboard.core

import com.unum.keyboard.layout.KeyType
import com.unum.keyboard.layout.KeyboardLayout
import com.unum.keyboard.layout.QwertyLayouts

enum class ShiftState {
    OFF,
    ON,
    CAPS_LOCK
}

enum class LayoutMode {
    LETTERS,
    SYMBOLS,
    SYMBOLS_2
}

class KeyboardState {
    var shiftState: ShiftState = ShiftState.OFF
        private set
    var layoutMode: LayoutMode = LayoutMode.LETTERS
        private set

    val currentLayout: KeyboardLayout
        get() = when (layoutMode) {
            LayoutMode.LETTERS -> when (shiftState) {
                ShiftState.OFF -> QwertyLayouts.enUsLowercase
                ShiftState.ON, ShiftState.CAPS_LOCK -> QwertyLayouts.enUsUppercase
            }
            LayoutMode.SYMBOLS -> QwertyLayouts.enUsSymbols
            LayoutMode.SYMBOLS_2 -> QwertyLayouts.enUsSymbols2
        }

    fun toggleShift() {
        shiftState = when (shiftState) {
            ShiftState.OFF -> ShiftState.ON
            ShiftState.ON -> ShiftState.CAPS_LOCK
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
    }

    fun autoUnshift() {
        if (shiftState == ShiftState.ON) {
            shiftState = ShiftState.OFF
        }
    }

    fun toggleSymbols() {
        layoutMode = when (layoutMode) {
            LayoutMode.LETTERS -> LayoutMode.SYMBOLS
            LayoutMode.SYMBOLS -> LayoutMode.LETTERS
            LayoutMode.SYMBOLS_2 -> LayoutMode.SYMBOLS
        }
    }

    fun toggleSymbols2() {
        layoutMode = when (layoutMode) {
            LayoutMode.SYMBOLS -> LayoutMode.SYMBOLS_2
            LayoutMode.SYMBOLS_2 -> LayoutMode.SYMBOLS
            else -> layoutMode
        }
    }

    fun handleKeyType(keyType: KeyType): KeyAction {
        return when (keyType) {
            KeyType.SHIFT -> {
                toggleShift()
                KeyAction.LayoutChanged
            }
            KeyType.SYMBOL_TOGGLE -> {
                when (layoutMode) {
                    LayoutMode.LETTERS -> toggleSymbols()
                    LayoutMode.SYMBOLS -> {
                        // Check if it's the #+=  or ABC toggle
                        toggleSymbols()
                    }
                    LayoutMode.SYMBOLS_2 -> toggleSymbols2()
                }
                KeyAction.LayoutChanged
            }
            KeyType.BACKSPACE -> KeyAction.Delete
            KeyType.SPACE -> KeyAction.Insert(" ")
            KeyType.ENTER -> KeyAction.Enter
            KeyType.CHARACTER -> KeyAction.None
            KeyType.CTRL -> KeyAction.None
        }
    }

    fun handleSymbolToggle(keyId: String): KeyAction {
        when (keyId) {
            "symbol_toggle" -> {
                layoutMode = LayoutMode.SYMBOLS
            }
            "abc_toggle" -> {
                layoutMode = LayoutMode.LETTERS
            }
            "symbol_toggle_2" -> {
                layoutMode = LayoutMode.SYMBOLS_2
            }
            "symbol_toggle_1" -> {
                layoutMode = LayoutMode.SYMBOLS
            }
        }
        return KeyAction.LayoutChanged
    }
}

sealed interface KeyAction {
    data class Insert(val text: String) : KeyAction
    data object Delete : KeyAction
    data object Enter : KeyAction
    data object LayoutChanged : KeyAction
    data object None : KeyAction
}
