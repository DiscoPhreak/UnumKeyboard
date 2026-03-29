package com.unum.keyboard.layout

object QwertyLayouts {

    val enUsLowercase = KeyboardLayout(
        id = "en-US-qwerty-lower",
        rows = listOf(
            KeyRow(listOf(
                Key("q", "q", flickUp = "1"),
                Key("w", "w", flickUp = "2"),
                Key("e", "e", flickUp = "3", flickRight = "é"),
                Key("r", "r", flickUp = "4"),
                Key("t", "t", flickUp = "5"),
                Key("y", "y", flickUp = "6"),
                Key("u", "u", flickUp = "7", flickRight = "ü"),
                Key("i", "i", flickUp = "8", flickRight = "í"),
                Key("o", "o", flickUp = "9", flickRight = "ó"),
                Key("p", "p", flickUp = "0")
            )),
            KeyRow(listOf(
                Key("a", "a", flickUp = "@", flickRight = "á"),
                Key("s", "s", flickUp = "#", flickRight = "$"),
                Key("d", "d", flickUp = "&"),
                Key("f", "f", flickUp = "*"),
                Key("g", "g", flickUp = "-"),
                Key("h", "h", flickUp = "+"),
                Key("j", "j", flickUp = "("),
                Key("k", "k", flickUp = ")"),
                Key("l", "l", flickUp = "'")
            )),
            KeyRow(listOf(
                Key("shift", "⇧", width = 1.5f, type = KeyType.SHIFT),
                Key("z", "z", flickUp = "!"),
                Key("x", "x", flickUp = "\""),
                Key("c", "c", flickUp = ":", flickRight = "ç"),
                Key("v", "v", flickUp = ";"),
                Key("b", "b", flickUp = "/"),
                Key("n", "n", flickUp = "?", flickRight = "ñ"),
                Key("m", "m", flickUp = ","),
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
                Key("Q", "Q", flickUp = "1"),
                Key("W", "W", flickUp = "2"),
                Key("E", "E", flickUp = "3", flickRight = "É"),
                Key("R", "R", flickUp = "4"),
                Key("T", "T", flickUp = "5"),
                Key("Y", "Y", flickUp = "6"),
                Key("U", "U", flickUp = "7", flickRight = "Ü"),
                Key("I", "I", flickUp = "8", flickRight = "Í"),
                Key("O", "O", flickUp = "9", flickRight = "Ó"),
                Key("P", "P", flickUp = "0")
            )),
            KeyRow(listOf(
                Key("A", "A", flickUp = "@", flickRight = "Á"),
                Key("S", "S", flickUp = "#", flickRight = "$"),
                Key("D", "D", flickUp = "&"),
                Key("F", "F", flickUp = "*"),
                Key("G", "G", flickUp = "-"),
                Key("H", "H", flickUp = "+"),
                Key("J", "J", flickUp = "("),
                Key("K", "K", flickUp = ")"),
                Key("L", "L", flickUp = "'")
            )),
            KeyRow(listOf(
                Key("shift", "⇧", width = 1.5f, type = KeyType.SHIFT),
                Key("Z", "Z", flickUp = "!"),
                Key("X", "X", flickUp = "\""),
                Key("C", "C", flickUp = ":", flickRight = "Ç"),
                Key("V", "V", flickUp = ";"),
                Key("B", "B", flickUp = "/"),
                Key("N", "N", flickUp = "?", flickRight = "Ñ"),
                Key("M", "M", flickUp = ","),
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
