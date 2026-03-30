package com.unum.keyboard.android.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class SuggestionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface SuggestionListener {
        fun onSuggestionSelected(word: String)
    }

    var listener: SuggestionListener? = null
    private val slots = mutableListOf<TextView>()

    init {
        orientation = HORIZONTAL
        setBackgroundColor(BG_COLOR)
        gravity = Gravity.CENTER_VERTICAL

        val density = resources.displayMetrics.density
        val verticalPad = (6 * density).toInt()
        setPadding(0, verticalPad, 0, verticalPad)

        for (i in 0 until MAX_SUGGESTIONS) {
            val slot = TextView(context).apply {
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                gravity = Gravity.CENTER
                textSize = 16f
                setTextColor(TEXT_COLOR)
                text = ""
                setBackgroundColor(Color.TRANSPARENT)
                setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())

                setOnClickListener {
                    val word = this.text?.toString()
                    if (!word.isNullOrBlank()) {
                        listener?.onSuggestionSelected(word)
                    }
                }
            }
            slots.add(slot)
            addView(slot)

            // Add divider between slots
            if (i < MAX_SUGGESTIONS - 1) {
                addView(android.view.View(context).apply {
                    layoutParams = LayoutParams((1 * density).toInt(), LayoutParams.MATCH_PARENT).apply {
                        topMargin = (8 * density).toInt()
                        bottomMargin = (8 * density).toInt()
                    }
                    setBackgroundColor(DIVIDER_COLOR)
                })
            }
        }
    }

    fun updateSuggestions(predictions: List<String>) {
        for (i in 0 until MAX_SUGGESTIONS) {
            slots[i].text = predictions.getOrNull(i) ?: ""
        }
    }

    fun clear() {
        for (slot in slots) {
            slot.text = ""
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val density = resources.displayMetrics.density
        val height = (BAR_HEIGHT_DP * density).toInt()
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
    }

    companion object {
        private const val MAX_SUGGESTIONS = 3
        private const val BAR_HEIGHT_DP = 44f
        private const val BG_COLOR = 0xFF0A0A0A.toInt()
        private const val TEXT_COLOR = 0xFFDDDDDD.toInt()
        private const val DIVIDER_COLOR = 0xFF333333.toInt()
    }
}
