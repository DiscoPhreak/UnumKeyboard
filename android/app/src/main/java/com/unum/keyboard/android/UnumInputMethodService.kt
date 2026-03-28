package com.unum.keyboard.android

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.FrameLayout

class UnumInputMethodService : InputMethodService() {

    override fun onCreateInputView(): View {
        return FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            minimumHeight = resources.displayMetrics.density.toInt() * 260
        }
    }
}
