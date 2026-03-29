package com.unum.keyboard.android

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import com.unum.keyboard.android.ui.KeyboardView
import com.unum.keyboard.android.ui.SuggestionBar
import com.unum.keyboard.prediction.PredictionService

class UnumInputMethodService : InputMethodService(),
    KeyboardView.KeyboardActionListener,
    SuggestionBar.SuggestionListener {

    private var keyboardView: KeyboardView? = null
    private var suggestionBar: SuggestionBar? = null
    private val predictionService = PredictionService()
    private val currentWord = StringBuilder()

    override fun onCreate() {
        super.onCreate()
        loadDictionary()
    }

    private fun loadDictionary() {
        try {
            val unigrams = assets.open("en-US/unigrams.txt").bufferedReader().readText()
            val bigrams = assets.open("en-US/bigrams.txt").bufferedReader().readText()
            val trigrams = assets.open("en-US/trigrams.txt").bufferedReader().readText()
            predictionService.initialize(unigrams, bigrams, trigrams)
        } catch (e: Exception) {
            // Dictionary loading failed — predictions will be empty
            android.util.Log.e("UnumIME", "Failed to load dictionary", e)
        }
    }

    override fun onCreateInputView(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        suggestionBar = SuggestionBar(this).also {
            it.listener = this
            container.addView(it)
        }

        keyboardView = KeyboardView(this).also {
            it.listener = this
            container.addView(it)
        }

        return container
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentWord.clear()
        predictionService.resetContext()
        updatePredictions()
    }

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)

        if (text == " ") {
            if (currentWord.isNotEmpty()) {
                predictionService.commitWord(currentWord.toString())
                currentWord.clear()
            }
        } else if (text == "." || text == "!" || text == "?" || text == "\n") {
            if (currentWord.isNotEmpty()) {
                predictionService.commitWord(currentWord.toString())
                currentWord.clear()
            }
            predictionService.resetContext()
        } else {
            currentWord.append(text)
        }

        predictionService.updatePrefix(currentWord.toString())
        updatePredictions()
    }

    override fun onDelete() {
        val ic = currentInputConnection ?: return
        ic.deleteSurroundingText(1, 0)

        if (currentWord.isNotEmpty()) {
            currentWord.deleteCharAt(currentWord.length - 1)
        }

        predictionService.updatePrefix(currentWord.toString())
        updatePredictions()
    }

    override fun onEnter() {
        val ic = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo

        if (currentWord.isNotEmpty()) {
            predictionService.commitWord(currentWord.toString())
            currentWord.clear()
        }
        predictionService.resetContext()

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
                    updatePredictions()
                    return
                }
            }
        }
        ic.commitText("\n", 1)
        updatePredictions()
    }

    override fun onSuggestionSelected(word: String) {
        val ic = currentInputConnection ?: return

        // Delete the current partial word and replace with the selected prediction
        if (currentWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentWord.length, 0)
        }

        ic.commitText("$word ", 1)
        predictionService.commitWord(word)
        currentWord.clear()
        predictionService.updatePrefix("")
        updatePredictions()
    }

    private fun updatePredictions() {
        val predictions = predictionService.predict(3)
        suggestionBar?.updateSuggestions(predictions.map { it.word })
    }
}
