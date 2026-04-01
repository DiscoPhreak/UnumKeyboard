package com.unum.keyboard.android

import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.view.KeyEvent
import com.unum.keyboard.android.ui.ClipboardPanel
import com.unum.keyboard.android.ui.KeyboardView
import com.unum.keyboard.android.ui.SuggestionBar
import com.unum.keyboard.core.TextAction
import com.unum.keyboard.gesture.GestureCandidate
import com.unum.keyboard.prediction.PredictionService
import com.unum.keyboard.prediction.StubNeuralReranker
import com.unum.keyboard.prediction.TwoStagePipeline
import com.unum.keyboard.settings.KeyboardConfig
import com.unum.keyboard.settings.KeyboardTheme
import com.unum.keyboard.text.EditingAction

class UnumInputMethodService : InputMethodService(),
    KeyboardView.KeyboardActionListener,
    SuggestionBar.SuggestionListener,
    ClipboardPanel.ClipboardPanelListener {

    private var keyboardView: KeyboardView? = null
    private var suggestionBar: SuggestionBar? = null
    private var clipboardPanel: ClipboardPanel? = null
    private val predictionService = PredictionService()
    private var pipeline: TwoStagePipeline? = null
    private val currentWord = StringBuilder()
    private val contextWords = mutableListOf<String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var learningEnabled = true
    private var currentLocale = "en-US"

    // Autocorrect undo state
    private data class AutoCorrectionUndo(
        val original: String,
        val corrected: String
    )
    private var lastAutoCorrection: AutoCorrectionUndo? = null

    // System clipboard listener
    private var systemClipboard: AndroidClipboardManager? = null
    private val clipboardListener = AndroidClipboardManager.OnPrimaryClipChangedListener {
        onSystemClipboardChanged()
    }

    override fun onCreate() {
        super.onCreate()
        loadDictionary()
        loadLearningData()
        setupSystemClipboardListener()
    }

    private fun setupSystemClipboardListener() {
        systemClipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? AndroidClipboardManager
        systemClipboard?.addPrimaryClipChangedListener(clipboardListener)
    }

    private fun onSystemClipboardChanged() {
        val clip = systemClipboard?.primaryClip ?: return
        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: return
            if (text.isNotBlank()) {
                clipboardPanel?.getClipboardManager()?.addClip(
                    text, System.currentTimeMillis()
                )
                saveClipboardData()
            }
        }
    }

    private fun loadDictionary() {
        try {
            val unigrams = assets.open("en-US/unigrams.txt").bufferedReader().readText()
            val bigrams = assets.open("en-US/bigrams.txt").bufferedReader().readText()
            val trigrams = assets.open("en-US/trigrams.txt").bufferedReader().readText()
            predictionService.initialize(unigrams, bigrams, trigrams)

            val enhancedPredictions = getSharedPreferences("unum_keyboard_prefs", MODE_PRIVATE)
                .getBoolean("enhanced_predictions", true)

            pipeline = TwoStagePipeline(
                predictionService = predictionService,
                reranker = StubNeuralReranker(),
                enabled = enhancedPredictions
            ).also { p ->
                p.onPredictionsUpdated = { predictions ->
                    mainHandler.post {
                        suggestionBar?.updateSuggestions(predictions.map { it.word })
                    }
                }
                p.loadModel()
            }
        } catch (e: Exception) {
            android.util.Log.e("UnumIME", "Failed to load dictionary", e)
        }
    }

    override fun onDestroy() {
        systemClipboard?.removePrimaryClipChangedListener(clipboardListener)
        saveClipboardData()
        saveLearningData()
        pipeline?.destroy()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        // Use FrameLayout as root so clipboard panel can overlay the keyboard
        val rootFrame = FrameLayout(this)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        suggestionBar = SuggestionBar(this).also {
            it.listener = this
            container.addView(it)
        }

        keyboardView = KeyboardView(this).also {
            it.listener = this
            val prefs = getSharedPreferences("unum_keyboard_prefs", MODE_PRIVATE)
            val gestureEnabled = prefs.getBoolean("gesture_typing", false)
            it.gestureTypingEnabled = gestureEnabled
            predictionService.dictionary?.let { dict -> it.setDictionary(dict) }

            // Apply locale (M13)
            currentLocale = prefs.getString("locale", "en-US") ?: "en-US"
            it.setLocale(currentLocale)

            // Apply theme and config (M12)
            val themeId = prefs.getString("theme_id", "amoled_dark") ?: "amoled_dark"
            it.applyTheme(KeyboardTheme.fromId(themeId))
            val configData = prefs.getString("config_data", "") ?: ""
            if (configData.isNotEmpty()) {
                it.applyConfig(KeyboardConfig.deserialize(configData))
            }

            container.addView(it)
        }

        rootFrame.addView(container)

        // Clipboard panel overlays on top
        clipboardPanel = ClipboardPanel(this).also {
            it.listener = this
            it.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            rootFrame.addView(it)
        }

        // Restore saved clipboard data
        loadClipboardData()

        return rootFrame
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentWord.clear()
        contextWords.clear()
        pipeline?.onSentenceBoundary()
    }

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return

        if (text == " ") {
            if (currentWord.isNotEmpty()) {
                val word = currentWord.toString()
                val correction = predictionService.getAutoCorrection(word)
                val committedWord: String

                if (correction != null && correction.shouldAutoApply) {
                    // Auto-correct: replace typed word with correction + space
                    ic.deleteSurroundingText(word.length, 0)
                    ic.commitText(correction.corrected + " ", 1)
                    committedWord = correction.corrected
                    lastAutoCorrection = AutoCorrectionUndo(word, correction.corrected)
                } else {
                    ic.commitText(" ", 1)
                    committedWord = word
                    lastAutoCorrection = null
                }

                contextWords.add(committedWord)
                if (contextWords.size > 5) contextWords.removeAt(0)
                pipeline?.onWordCommitted(committedWord)
                if (learningEnabled) {
                    predictionService.learnWord(committedWord, System.currentTimeMillis())
                }
                currentWord.clear()
                keyboardView?.currentPrefix = ""
            } else {
                ic.commitText(" ", 1)
                lastAutoCorrection = null
            }
        } else if (text == "." || text == "!" || text == "?" || text == "\n") {
            if (currentWord.isNotEmpty()) {
                val word = currentWord.toString()
                val correction = predictionService.getAutoCorrection(word)
                val committedWord: String

                if (correction != null && correction.shouldAutoApply) {
                    ic.deleteSurroundingText(word.length, 0)
                    ic.commitText(correction.corrected + text, 1)
                    committedWord = correction.corrected
                } else {
                    ic.commitText(text, 1)
                    committedWord = word
                }

                pipeline?.onWordCommitted(committedWord)
                if (learningEnabled) {
                    predictionService.learnWord(committedWord, System.currentTimeMillis())
                }
                currentWord.clear()
                keyboardView?.currentPrefix = ""
            } else {
                ic.commitText(text, 1)
            }
            lastAutoCorrection = null
            contextWords.clear()
            pipeline?.onSentenceBoundary()
        } else {
            ic.commitText(text, 1)
            lastAutoCorrection = null
            currentWord.append(text)
            pipeline?.onKeystroke(currentWord.toString(), contextWords)
            keyboardView?.currentPrefix = currentWord.toString()
        }
    }

    override fun onDelete() {
        val ic = currentInputConnection ?: return

        val undo = lastAutoCorrection
        if (undo != null) {
            // Undo auto-correction: remove "corrected " and insert "original "
            ic.deleteSurroundingText(undo.corrected.length + 1, 0)
            ic.commitText(undo.original + " ", 1)
            lastAutoCorrection = null

            // Block this word from future auto-corrections
            predictionService.addToBlockList(undo.original)

            currentWord.clear()
            pipeline?.onKeystroke("", contextWords)
            keyboardView?.currentPrefix = ""
            return
        }

        // Normal backspace
        ic.deleteSurroundingText(1, 0)

        if (currentWord.isNotEmpty()) {
            currentWord.deleteCharAt(currentWord.length - 1)
        }

        pipeline?.onKeystroke(currentWord.toString(), contextWords)
        keyboardView?.currentPrefix = currentWord.toString()
    }

    override fun onEnter() {
        val ic = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo

        if (currentWord.isNotEmpty()) {
            pipeline?.onWordCommitted(currentWord.toString())
            currentWord.clear()
        }
        contextWords.clear()
        pipeline?.onSentenceBoundary()

        if (editorInfo != null) {
            val action = editorInfo.imeOptions and
                (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)

            when (action) {
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_SEND,
                EditorInfo.IME_ACTION_GO,
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_NEXT -> {
                    ic.performEditorAction(action)
                    return
                }
            }
        }
        ic.commitText("\n", 1)
    }

    // ---- Text Editing (M10) ----

    override fun onTextAction(action: TextAction) {
        val ic = currentInputConnection ?: return

        when (action) {
            is TextAction.MoveCursor -> {
                // Move cursor by offset using ExtractedText
                val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                if (extracted != null) {
                    val newPos = (extracted.selectionStart + action.offset)
                        .coerceIn(0, extracted.text?.length ?: 0)
                    ic.setSelection(newPos, newPos)
                }
            }
            is TextAction.ExtendSelection -> {
                val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                if (extracted != null) {
                    val textLen = extracted.text?.length ?: 0
                    val newEnd = (extracted.selectionEnd + action.offset).coerceIn(0, textLen)
                    ic.setSelection(extracted.selectionStart, newEnd)
                }
            }
            is TextAction.SelectAll -> {
                ic.performContextMenuAction(android.R.id.selectAll)
            }
            is TextAction.Cut -> {
                ic.performContextMenuAction(android.R.id.cut)
            }
            is TextAction.Copy -> {
                ic.performContextMenuAction(android.R.id.copy)
            }
            is TextAction.Paste -> {
                ic.performContextMenuAction(android.R.id.paste)
            }
            is TextAction.Undo -> {
                // Send Ctrl+Z key event
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z).apply {
                    // Note: meta state for Ctrl isn't easily set this way; apps may not support it
                })
            }
            is TextAction.Redo -> {
                // Send Ctrl+Y key event
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Y))
            }
            is TextAction.MoveCursorLeft -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
            }
            is TextAction.MoveCursorRight -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
            }
            else -> { /* Other TextActions handled elsewhere */ }
        }
    }

    override fun onEditingAction(action: EditingAction) {
        when (action) {
            EditingAction.CURSOR_LEFT -> onTextAction(TextAction.MoveCursor(-1))
            EditingAction.CURSOR_RIGHT -> onTextAction(TextAction.MoveCursor(1))
            EditingAction.CURSOR_WORD_LEFT -> {
                // Move left by word — use key event with Ctrl modifier
                val ic = currentInputConnection ?: return
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_CTRL_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_CTRL_ON))
            }
            EditingAction.CURSOR_WORD_RIGHT -> {
                val ic = currentInputConnection ?: return
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_CTRL_ON))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_CTRL_ON))
            }
            EditingAction.CURSOR_LINE_START -> {
                val ic = currentInputConnection ?: return
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME))
            }
            EditingAction.CURSOR_LINE_END -> {
                val ic = currentInputConnection ?: return
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END))
            }
            EditingAction.SELECT_ALL -> onTextAction(TextAction.SelectAll)
            EditingAction.SELECT_LEFT -> onTextAction(TextAction.ExtendSelection(-1))
            EditingAction.SELECT_RIGHT -> onTextAction(TextAction.ExtendSelection(1))
            EditingAction.CUT -> onTextAction(TextAction.Cut)
            EditingAction.COPY -> onTextAction(TextAction.Copy)
            EditingAction.PASTE -> onTextAction(TextAction.Paste)
            EditingAction.UNDO -> onTextAction(TextAction.Undo)
            EditingAction.REDO -> onTextAction(TextAction.Redo)
        }
    }

    override fun onGestureWord(candidates: List<GestureCandidate>) {
        if (candidates.isNotEmpty()) {
            suggestionBar?.updateSuggestions(candidates.map { it.word })
            val topWord = candidates[0].word
            contextWords.add(topWord)
            if (contextWords.size > 5) contextWords.removeAt(0)
            pipeline?.onWordCommitted(topWord)
            if (learningEnabled) {
                predictionService.learnWord(topWord, System.currentTimeMillis())
            }
            currentWord.clear()
            keyboardView?.currentPrefix = ""
        }
    }

    override fun onSuggestionSelected(word: String) {
        val ic = currentInputConnection ?: return

        if (currentWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentWord.length, 0)
        }

        ic.commitText("$word ", 1)
        contextWords.add(word)
        if (contextWords.size > 5) contextWords.removeAt(0)
        pipeline?.onWordCommitted(word)
        if (learningEnabled) {
            predictionService.learnWord(word, System.currentTimeMillis())
        }
        currentWord.clear()
        keyboardView?.currentPrefix = ""
    }

    // ---- ClipboardPanel callbacks ----

    override fun onClipSelected(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        clipboardPanel?.hide()
    }

    override fun onSnippetSelected(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        clipboardPanel?.hide()
    }

    override fun onClipboardPanelClosed() {
        saveClipboardData()
    }

    /**
     * Toggle the clipboard panel visibility.
     * Called from a future clipboard button on the keyboard toolbar.
     */
    fun toggleClipboardPanel() {
        val panel = clipboardPanel ?: return
        if (panel.isShowing) {
            panel.hide()
        } else {
            panel.show()
        }
    }

    // ---- Learning persistence (M11) ----

    private fun loadLearningData() {
        val prefs = getSharedPreferences("unum_keyboard_prefs", MODE_PRIVATE)
        learningEnabled = prefs.getBoolean("learning_enabled", true)
        val data = prefs.getString("learning_data", "") ?: ""
        if (data.isNotEmpty()) {
            predictionService.loadLearningData(data)
        }
        val blockListData = prefs.getString("autocorrect_blocklist", "") ?: ""
        if (blockListData.isNotEmpty()) {
            predictionService.loadBlockList(blockListData)
        }
    }

    private fun saveLearningData() {
        if (!learningEnabled) return
        val prefs = getSharedPreferences("unum_keyboard_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("learning_data", predictionService.saveLearningData())
            .putString("autocorrect_blocklist", predictionService.serializeBlockList())
            .apply()
    }

    // ---- Clipboard persistence ----

    private fun loadClipboardData() {
        val prefs = getSharedPreferences("unum_keyboard_prefs", MODE_PRIVATE)
        val clipData = prefs.getString("clipboard_data", "") ?: ""
        val snippetData = prefs.getString("slideboard_data", "") ?: ""

        clipboardPanel?.getClipboardManager()?.deserialize(clipData)
        clipboardPanel?.getSlideboardManager()?.deserialize(snippetData)
    }

    private fun saveClipboardData() {
        val prefs = getSharedPreferences("unum_keyboard_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("clipboard_data", clipboardPanel?.getClipboardManager()?.serialize() ?: "")
            .putString("slideboard_data", clipboardPanel?.getSlideboardManager()?.serialize() ?: "")
            .apply()
    }
}
