package com.unum.keyboard.layout

object QwertyLayouts {

    val enUsLowercase = KeyboardLayout(
        id = "en-US-qwerty-lower",
        rows = listOf(
            KeyRow(listOf(
                Key("q", "q"), Key("w", "w"), Key("e", "e"), Key("r", "r"), Key("t", "t"),
                Key("y", "y"), Key("u", "u"), Key("i", "i"), Key("o", "o"), Key("p", "p")
            )),
            KeyRow(listOf(
                Key("a", "a"), Key("s", "s"), Key("d", "d"), Key("f", "f"), Key("g", "g"),
                Key("h", "h"), Key("j", "j"), Key("k", "k"), Key("l", "l")
            )),
            KeyRow(listOf(
                Key("shift", "⇧", width = 1.5f, type = KeyType.SHIFT),
                Key("z", "z"), Key("x", "x"), Key("c", "c"), Key("v", "v"),
                Key("b", "b"), Key("n", "n"), Key("m", "m"),
                Key("backspace", "⌫", width = 1.5f, type = KeyType.BACKSPACE)
            )),
            KeyRow(listOf(
                Key("symbol_toggle", "?123", width = 1.5f, type = KeyType.SYMBOL_TOGGLE),
                Key("comma", ","),
                Key("space", " ", width = 5.0f, type = KeyType.SPACE),
                Key("period", "."),
                Key("enter", "↵", width = 1.5f, type = KeyType.ENTER)
            ))
        )
    )

    val enUsUppercase = KeyboardLayout(
        id = "en-US-qwerty-upper",
        rows = listOf(
            KeyRow(listOf(
                Key("Q", "Q"), Key("W", "W"), Key("E", "E"), Key("R", "R"), Key("T", "T"),
                Key("Y", "Y"), Key("U", "U"), Key("I", "I"), Key("O", "O"), Key("P", "P")
            )),
            KeyRow(listOf(
                Key("A", "A"), Key("S", "S"), Key("D", "D"), Key("F", "F"), Key("G", "G"),
                Key("H", "H"), Key("J", "J"), Key("K", "K"), Key("L", "L")
            )),
            KeyRow(listOf(
                Key("shift", "⇧", width = 1.5f, type = KeyType.SHIFT),
                Key("Z", "Z"), Key("X", "X"), Key("C", "C"), Key("V", "V"),
                Key("B", "B"), Key("N", "N"), Key("M", "M"),
                Key("backspace", "⌫", width = 1.5f, type = KeyType.BACKSPACE)
            )),
            KeyRow(listOf(
                Key("symbol_toggle", "?123", width = 1.5f, type = KeyType.SYMBOL_TOGGLE),
                Key("comma", ","),
                Key("space", " ", width = 5.0f, type = KeyType.SPACE),
                Key("period", "."),
                Key("enter", "↵", width = 1.5f, type = KeyType.ENTER)
            ))
        )
    )

    val enUsSymbols = KeyboardLayout(
        id = "en-US-qwerty-symbols",
        rows = listOf(
            KeyRow(listOf(
                Key("1", "1"), Key("2", "2"), Key("3", "3"), Key("4", "4"), Key("5", "5"),
                Key("6", "6"), Key("7", "7"), Key("8", "8"), Key("9", "9"), Key("0", "0")
            )),
            KeyRow(listOf(
                Key("at", "@"), Key("hash", "#"), Key("dollar", "$"), Key("underscore", "_"),
                Key("ampersand", "&"), Key("hyphen", "-"), Key("plus", "+"),
                Key("lparen", "("), Key("rparen", ")")
            )),
            KeyRow(listOf(
                Key("symbol_toggle_2", "#+=", width = 1.5f, type = KeyType.SYMBOL_TOGGLE),
                Key("asterisk", "*"), Key("dquote", "\""), Key("squote", "'"),
                Key("colon", ":"), Key("semicolon", ";"), Key("excl", "!"), Key("question", "?"),
                Key("backspace", "⌫", width = 1.5f, type = KeyType.BACKSPACE)
            )),
            KeyRow(listOf(
                Key("abc_toggle", "ABC", width = 1.5f, type = KeyType.SYMBOL_TOGGLE),
                Key("comma", ","),
                Key("space", " ", width = 5.0f, type = KeyType.SPACE),
                Key("period", "."),
                Key("enter", "↵", width = 1.5f, type = KeyType.ENTER)
            ))
        )
    )

    val enUsSymbols2 = KeyboardLayout(
        id = "en-US-qwerty-symbols2",
        rows = listOf(
            KeyRow(listOf(
                Key("tilde", "~"), Key("backtick", "`"), Key("pipe", "|"), Key("bullet", "•"),
                Key("sqrt", "√"), Key("pi", "π"), Key("divide", "÷"), Key("multiply", "×"),
                Key("paragraph", "¶"), Key("delta", "∆")
            )),
            KeyRow(listOf(
                Key("pound", "£"), Key("cent", "¢"), Key("euro", "€"), Key("yen", "¥"),
                Key("caret", "^"), Key("lbracket", "["), Key("rbracket", "]"),
                Key("lbrace", "{"), Key("rbrace", "}")
            )),
            KeyRow(listOf(
                Key("symbol_toggle_1", "123", width = 1.5f, type = KeyType.SYMBOL_TOGGLE),
                Key("percent", "%"), Key("copyright", "©"), Key("registered", "®"),
                Key("trademark", "™"), Key("checkmark", "✓"), Key("backslash", "\\"),
                Key("lessthan", "<"), Key("greaterthan", ">"),
                Key("backspace", "⌫", width = 1.5f, type = KeyType.BACKSPACE)
            )),
            KeyRow(listOf(
                Key("abc_toggle", "ABC", width = 1.5f, type = KeyType.SYMBOL_TOGGLE),
                Key("comma", ","),
                Key("space", " ", width = 5.0f, type = KeyType.SPACE),
                Key("period", "."),
                Key("enter", "↵", width = 1.5f, type = KeyType.ENTER)
            ))
        )
    )
}
