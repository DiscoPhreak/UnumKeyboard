package com.unum.keyboard.layout

/**
 * Registry of keyboard layouts organized by locale.
 *
 * Each locale provides lowercase, uppercase, symbols, and symbols2 layouts.
 * Layouts can be registered dynamically for new languages.
 */
object LayoutRegistry {

    data class LocaleLayouts(
        val locale: String,
        val displayName: String,
        val lowercase: KeyboardLayout,
        val uppercase: KeyboardLayout,
        val symbols: KeyboardLayout,
        val symbols2: KeyboardLayout
    )

    private val registry = mutableMapOf<String, LocaleLayouts>()

    init {
        register(enUs())
        register(esEs())
        register(frFr())
        register(deDe())
        register(ptBr())
    }

    fun register(layouts: LocaleLayouts) {
        registry[layouts.locale] = layouts
    }

    fun getLayouts(locale: String): LocaleLayouts =
        registry[locale] ?: registry["en-US"]!!

    fun availableLocales(): List<String> =
        registry.keys.toList().sorted()

    fun localeDisplayNames(): List<Pair<String, String>> =
        registry.entries.map { it.key to it.value.displayName }.sortedBy { it.second }

    fun hasLocale(locale: String): Boolean =
        registry.containsKey(locale)

    // ---- Built-in layouts ----

    private fun enUs() = LocaleLayouts(
        locale = "en-US",
        displayName = "English (US)",
        lowercase = QwertyLayouts.enUsLowercase,
        uppercase = QwertyLayouts.enUsUppercase,
        symbols = QwertyLayouts.enUsSymbols,
        symbols2 = QwertyLayouts.enUsSymbols2
    )

    private fun esEs() = LocaleLayouts(
        locale = "es-ES",
        displayName = "Español",
        lowercase = KeyboardLayout(
            id = "es-ES-qwerty-lower",
            rows = listOf(
                KeyRow(listOf(
                    Key("q", "q", flickUp = "1"),
                    Key("w", "w", flickUp = "2"),
                    Key("e", "e", flickUp = "3", flickRight = "é"),
                    Key("r", "r", flickUp = "4"),
                    Key("t", "t", flickUp = "5"),
                    Key("y", "y", flickUp = "6"),
                    Key("u", "u", flickUp = "7", flickRight = "ú"),
                    Key("i", "i", flickUp = "8", flickRight = "í"),
                    Key("o", "o", flickUp = "9", flickRight = "ó"),
                    Key("p", "p", flickUp = "0")
                )),
                KeyRow(listOf(
                    Key("a", "a", flickUp = "@", flickRight = "á"),
                    Key("s", "s", flickUp = "#"),
                    Key("d", "d", flickUp = "&"),
                    Key("f", "f", flickUp = "*"),
                    Key("g", "g", flickUp = "-"),
                    Key("h", "h", flickUp = "+"),
                    Key("j", "j", flickUp = "("),
                    Key("k", "k", flickUp = ")"),
                    Key("l", "l", flickUp = "'"),
                    Key("ñ", "ñ", flickUp = "~")
                )),
                KeyRow(listOf(
                    Key("shift", "⇧", width = 1.5f, type = KeyType.SHIFT),
                    Key("z", "z", flickUp = "!"),
                    Key("x", "x", flickUp = "\""),
                    Key("c", "c", flickUp = ":", flickRight = "ç"),
                    Key("v", "v", flickUp = ";"),
                    Key("b", "b", flickUp = "/"),
                    Key("n", "n", flickUp = "?"),
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
        ),
        uppercase = KeyboardLayout(
            id = "es-ES-qwerty-upper",
            rows = listOf(
                KeyRow(listOf(
                    Key("Q", "Q", flickUp = "1"),
                    Key("W", "W", flickUp = "2"),
                    Key("E", "E", flickUp = "3", flickRight = "É"),
                    Key("R", "R", flickUp = "4"),
                    Key("T", "T", flickUp = "5"),
                    Key("Y", "Y", flickUp = "6"),
                    Key("U", "U", flickUp = "7", flickRight = "Ú"),
                    Key("I", "I", flickUp = "8", flickRight = "Í"),
                    Key("O", "O", flickUp = "9", flickRight = "Ó"),
                    Key("P", "P", flickUp = "0")
                )),
                KeyRow(listOf(
                    Key("A", "A", flickUp = "@", flickRight = "Á"),
                    Key("S", "S", flickUp = "#"),
                    Key("D", "D", flickUp = "&"),
                    Key("F", "F", flickUp = "*"),
                    Key("G", "G", flickUp = "-"),
                    Key("H", "H", flickUp = "+"),
                    Key("J", "J", flickUp = "("),
                    Key("K", "K", flickUp = ")"),
                    Key("L", "L", flickUp = "'"),
                    Key("Ñ", "Ñ", flickUp = "~")
                )),
                KeyRow(listOf(
                    Key("shift", "⇧", width = 1.5f, type = KeyType.SHIFT),
                    Key("Z", "Z", flickUp = "!"),
                    Key("X", "X", flickUp = "\""),
                    Key("C", "C", flickUp = ":", flickRight = "Ç"),
                    Key("V", "V", flickUp = ";"),
                    Key("B", "B", flickUp = "/"),
                    Key("N", "N", flickUp = "?"),
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
        ),
        symbols = QwertyLayouts.enUsSymbols,
        symbols2 = QwertyLayouts.enUsSymbols2
    )

    private fun frFr() = LocaleLayouts(
        locale = "fr-FR",
        displayName = "Français",
        lowercase = KeyboardLayout(
            id = "fr-FR-azerty-lower",
            rows = listOf(
                KeyRow(listOf(
                    Key("a", "a", flickUp = "1", flickRight = "à"),
                    Key("z", "z", flickUp = "2"),
                    Key("e", "e", flickUp = "3", flickRight = "é"),
                    Key("r", "r", flickUp = "4"),
                    Key("t", "t", flickUp = "5"),
                    Key("y", "y", flickUp = "6"),
                    Key("u", "u", flickUp = "7", flickRight = "ù"),
                    Key("i", "i", flickUp = "8", flickRight = "î"),
                    Key("o", "o", flickUp = "9", flickRight = "ô"),
                    Key("p", "p", flickUp = "0")
                )),
                KeyRow(listOf(
                    Key("q", "q", flickUp = "@"),
                    Key("s", "s", flickUp = "#"),
                    Key("d", "d", flickUp = "&"),
                    Key("f", "f", flickUp = "*"),
                    Key("g", "g", flickUp = "-"),
                    Key("h", "h", flickUp = "+"),
                    Key("j", "j", flickUp = "("),
                    Key("k", "k", flickUp = ")"),
                    Key("l", "l", flickUp = "'"),
                    Key("m", "m", flickUp = "\"")
                )),
                KeyRow(listOf(
                    Key("shift", "⇧", width = 1.5f, type = KeyType.SHIFT),
                    Key("w", "w", flickUp = "!"),
                    Key("x", "x", flickUp = ":"),
                    Key("c", "c", flickUp = ";", flickRight = "ç"),
                    Key("v", "v", flickUp = "/"),
                    Key("b", "b", flickUp = "?"),
                    Key("n", "n", flickUp = ","),
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
        ),
        uppercase = KeyboardLayout(
            id = "fr-FR-azerty-upper",
            rows = listOf(
                KeyRow(listOf(
                    Key("A", "A", flickUp = "1", flickRight = "À"),
                    Key("Z", "Z", flickUp = "2"),
                    Key("E", "E", flickUp = "3", flickRight = "É"),
                    Key("R", "R", flickUp = "4"),
                    Key("T", "T", flickUp = "5"),
                    Key("Y", "Y", flickUp = "6"),
                    Key("U", "U", flickUp = "7", flickRight = "Ù"),
                    Key("I", "I", flickUp = "8", flickRight = "Î"),
                    Key("O", "O", flickUp = "9", flickRight = "Ô"),
                    Key("P", "P", flickUp = "0")
                )),
                KeyRow(listOf(
                    Key("Q", "Q", flickUp = "@"),
                    Key("S", "S", flickUp = "#"),
                    Key("D", "D", flickUp = "&"),
                    Key("F", "F", flickUp = "*"),
                    Key("G", "G", flickUp = "-"),
                    Key("H", "H", flickUp = "+"),
                    Key("J", "J", flickUp = "("),
                    Key("K", "K", flickUp = ")"),
                    Key("L", "L", flickUp = "'"),
                    Key("M", "M", flickUp = "\"")
                )),
                KeyRow(listOf(
                    Key("shift", "⇧", width = 1.5f, type = KeyType.SHIFT),
                    Key("W", "W", flickUp = "!"),
                    Key("X", "X", flickUp = ":"),
                    Key("C", "C", flickUp = ";", flickRight = "Ç"),
                    Key("V", "V", flickUp = "/"),
                    Key("B", "B", flickUp = "?"),
                    Key("N", "N", flickUp = ","),
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
        ),
        symbols = QwertyLayouts.enUsSymbols,
        symbols2 = QwertyLayouts.enUsSymbols2
    )

    private fun deDe() = LocaleLayouts(
        locale = "de-DE",
        displayName = "Deutsch",
        lowercase = KeyboardLayout(
            id = "de-DE-qwertz-lower",
            rows = listOf(
                KeyRow(listOf(
                    Key("q", "q", flickUp = "1"),
                    Key("w", "w", flickUp = "2"),
                    Key("e", "e", flickUp = "3", flickRight = "é"),
                    Key("r", "r", flickUp = "4"),
                    Key("t", "t", flickUp = "5"),
                    Key("z", "z", flickUp = "6"),
                    Key("u", "u", flickUp = "7", flickRight = "ü"),
                    Key("i", "i", flickUp = "8"),
                    Key("o", "o", flickUp = "9", flickRight = "ö"),
                    Key("p", "p", flickUp = "0")
                )),
                KeyRow(listOf(
                    Key("a", "a", flickUp = "@", flickRight = "ä"),
                    Key("s", "s", flickUp = "#", flickRight = "ß"),
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
                    Key("y", "y", flickUp = "!"),
                    Key("x", "x", flickUp = "\""),
                    Key("c", "c", flickUp = ":"),
                    Key("v", "v", flickUp = ";"),
                    Key("b", "b", flickUp = "/"),
                    Key("n", "n", flickUp = "?"),
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
        ),
        uppercase = KeyboardLayout(
            id = "de-DE-qwertz-upper",
            rows = listOf(
                KeyRow(listOf(
                    Key("Q", "Q", flickUp = "1"),
                    Key("W", "W", flickUp = "2"),
                    Key("E", "E", flickUp = "3", flickRight = "É"),
                    Key("R", "R", flickUp = "4"),
                    Key("T", "T", flickUp = "5"),
                    Key("Z", "Z", flickUp = "6"),
                    Key("U", "U", flickUp = "7", flickRight = "Ü"),
                    Key("I", "I", flickUp = "8"),
                    Key("O", "O", flickUp = "9", flickRight = "Ö"),
                    Key("P", "P", flickUp = "0")
                )),
                KeyRow(listOf(
                    Key("A", "A", flickUp = "@", flickRight = "Ä"),
                    Key("S", "S", flickUp = "#", flickRight = "ẞ"),
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
                    Key("Y", "Y", flickUp = "!"),
                    Key("X", "X", flickUp = "\""),
                    Key("C", "C", flickUp = ":"),
                    Key("V", "V", flickUp = ";"),
                    Key("B", "B", flickUp = "/"),
                    Key("N", "N", flickUp = "?"),
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
        ),
        symbols = QwertyLayouts.enUsSymbols,
        symbols2 = QwertyLayouts.enUsSymbols2
    )

    private fun ptBr() = LocaleLayouts(
        locale = "pt-BR",
        displayName = "Português (BR)",
        lowercase = KeyboardLayout(
            id = "pt-BR-qwerty-lower",
            rows = listOf(
                KeyRow(listOf(
                    Key("q", "q", flickUp = "1"),
                    Key("w", "w", flickUp = "2"),
                    Key("e", "e", flickUp = "3", flickRight = "é"),
                    Key("r", "r", flickUp = "4"),
                    Key("t", "t", flickUp = "5"),
                    Key("y", "y", flickUp = "6"),
                    Key("u", "u", flickUp = "7", flickRight = "ú"),
                    Key("i", "i", flickUp = "8", flickRight = "í"),
                    Key("o", "o", flickUp = "9", flickRight = "ó"),
                    Key("p", "p", flickUp = "0")
                )),
                KeyRow(listOf(
                    Key("a", "a", flickUp = "@", flickRight = "ã"),
                    Key("s", "s", flickUp = "#"),
                    Key("d", "d", flickUp = "&"),
                    Key("f", "f", flickUp = "*"),
                    Key("g", "g", flickUp = "-"),
                    Key("h", "h", flickUp = "+"),
                    Key("j", "j", flickUp = "("),
                    Key("k", "k", flickUp = ")"),
                    Key("l", "l", flickUp = "'"),
                    Key("ç", "ç", flickUp = "~")
                )),
                KeyRow(listOf(
                    Key("shift", "⇧", width = 1.5f, type = KeyType.SHIFT),
                    Key("z", "z", flickUp = "!"),
                    Key("x", "x", flickUp = "\""),
                    Key("c", "c", flickUp = ":"),
                    Key("v", "v", flickUp = ";"),
                    Key("b", "b", flickUp = "/"),
                    Key("n", "n", flickUp = "?"),
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
        ),
        uppercase = KeyboardLayout(
            id = "pt-BR-qwerty-upper",
            rows = listOf(
                KeyRow(listOf(
                    Key("Q", "Q", flickUp = "1"),
                    Key("W", "W", flickUp = "2"),
                    Key("E", "E", flickUp = "3", flickRight = "É"),
                    Key("R", "R", flickUp = "4"),
                    Key("T", "T", flickUp = "5"),
                    Key("Y", "Y", flickUp = "6"),
                    Key("U", "U", flickUp = "7", flickRight = "Ú"),
                    Key("I", "I", flickUp = "8", flickRight = "Í"),
                    Key("O", "O", flickUp = "9", flickRight = "Ó"),
                    Key("P", "P", flickUp = "0")
                )),
                KeyRow(listOf(
                    Key("A", "A", flickUp = "@", flickRight = "Ã"),
                    Key("S", "S", flickUp = "#"),
                    Key("D", "D", flickUp = "&"),
                    Key("F", "F", flickUp = "*"),
                    Key("G", "G", flickUp = "-"),
                    Key("H", "H", flickUp = "+"),
                    Key("J", "J", flickUp = "("),
                    Key("K", "K", flickUp = ")"),
                    Key("L", "L", flickUp = "'"),
                    Key("Ç", "Ç", flickUp = "~")
                )),
                KeyRow(listOf(
                    Key("shift", "⇧", width = 1.5f, type = KeyType.SHIFT),
                    Key("Z", "Z", flickUp = "!"),
                    Key("X", "X", flickUp = "\""),
                    Key("C", "C", flickUp = ":"),
                    Key("V", "V", flickUp = ";"),
                    Key("B", "B", flickUp = "/"),
                    Key("N", "N", flickUp = "?"),
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
        ),
        symbols = QwertyLayouts.enUsSymbols,
        symbols2 = QwertyLayouts.enUsSymbols2
    )
}
