package com.unum.keyboard.core

import com.unum.keyboard.layout.KeyType
import com.unum.keyboard.layout.KeyboardLayout
import com.unum.keyboard.layout.LayoutRegistry

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

    /** Current locale — determines which layout set is used */
    var locale: String = "en-US"
        private set

    private var localeLayouts: LayoutRegistry.LocaleLayouts = LayoutRegistry.getLayouts("en-US")

    val currentLayout: KeyboardLayout
        get() = when (layoutMode) {
            LayoutMode.LETTERS -> when (shiftState) {
                ShiftState.OFF -> localeLayouts.lowercase
                ShiftState.ON, ShiftState.CAPS_LOCK -> localeLayouts.uppercase
            }
            LayoutMode.SYMBOLS -> localeLayouts.symbols
            LayoutMode.SYMBOLS_2 -> localeLayouts.symbols2
        }

    /**
     * Switch to a different locale. Resets to letters mode.
     */
    fun setLocale(locale: String) {
        this.locale = locale
        localeLayouts = LayoutRegistry.getLayouts(locale)
        layoutMode = LayoutMode.LETTERS
        shiftState = ShiftState.OFF
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
