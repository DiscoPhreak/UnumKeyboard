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
import com.unum.keyboard.android.ui.ClipboardPanel
import com.unum.keyboard.android.ui.KeyboardView
import com.unum.keyboard.android.ui.SuggestionBar
import com.unum.keyboard.gesture.GestureCandidate
import com.unum.keyboard.prediction.PredictionService
import com.unum.keyboard.prediction.StubNeuralReranker
import com.unum.keyboard.prediction.TwoStagePipeline

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

    // System clipboard listener
    private var systemClipboard: AndroidClipboardManager? = null
    private val clipboardListener = AndroidClipboardManager.OnPrimaryClipChangedListener {
        onSystemClipboardChanged()
    }

    override fun onCreate() {
        super.onCreate()
        loadDictionary()
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
            val gestureEnabled = getSharedPreferences("unum_keyboard_prefs", MODE_PRIVATE)
                .getBoolean("gesture_typing", false)
            it.gestureTypingEnabled = gestureEnabled
            predictionService.dictionary?.let { dict -> it.setDictionary(dict) }
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
        ic.commitText(text, 1)

        if (text == " ") {
            if (currentWord.isNotEmpty()) {
                val word = currentWord.toString()
                contextWords.add(word)
                if (contextWords.size > 5) contextWords.removeAt(0)
                pipeline?.onWordCommitted(word)
                currentWord.clear()
                keyboardView?.currentPrefix = ""
            }
        } else if (text == "." || text == "!" || text == "?" || text == "\n") {
            if (currentWord.isNotEmpty()) {
                val word = currentWord.toString()
                pipeline?.onWordCommitted(word)
                currentWord.clear()
                keyboardView?.currentPrefix = ""
            }
            contextWords.clear()
            pipeline?.onSentenceBoundary()
        } else {
            currentWord.append(text)
            pipeline?.onKeystroke(currentWord.toString(), contextWords)
            keyboardView?.currentPrefix = currentWord.toString()
        }
    }

    override fun onDelete() {
        val ic = currentInputConnection ?: return
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

    override fun onGestureWord(candidates: List<GestureCandidate>) {
        if (candidates.isNotEmpty()) {
            suggestionBar?.updateSuggestions(candidates.map { it.word })
            val topWord = candidates[0].word
            contextWords.add(topWord)
            if (contextWords.size > 5) contextWords.removeAt(0)
            pipeline?.onWordCommitted(topWord)
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

    // ---- Persistence ----

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
