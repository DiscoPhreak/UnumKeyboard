package com.unum.keyboard.android.setup

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SetupActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val pad = (24 * density).toInt()

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "Unum Keyboard"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, (32 * density).toInt(), 0, (16 * density).toInt())
        }

        val subtitle = TextView(this).apply {
            text = "Set up your keyboard in two steps:"
            textSize = 16f
            setTextColor(0xFFBBBBBB.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (32 * density).toInt())
        }

        val step1Label = TextView(this).apply {
            text = "Step 1: Enable Unum Keyboard"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, (8 * density).toInt())
        }

        val step1Desc = TextView(this).apply {
            text = "Open Language & Input settings and enable Unum Keyboard in the list of keyboards."
            textSize = 14f
            setTextColor(0xFF999999.toInt())
            setPadding(0, 0, 0, (12 * density).toInt())
        }

        val enableButton = Button(this).apply {
            text = "Open Keyboard Settings"
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setPadding((24 * density).toInt(), (12 * density).toInt(), (24 * density).toInt(), (12 * density).toInt())
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }

        val step2Label = TextView(this).apply {
            text = "\nStep 2: Switch to Unum Keyboard"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, (24 * density).toInt(), 0, (8 * density).toInt())
        }

        val step2Desc = TextView(this).apply {
            text = "Select Unum Keyboard as your active input method."
            textSize = 14f
            setTextColor(0xFF999999.toInt())
            setPadding(0, 0, 0, (12 * density).toInt())
        }

        val switchButton = Button(this).apply {
            text = "Switch Input Method"
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setPadding((24 * density).toInt(), (12 * density).toInt(), (24 * density).toInt(), (12 * density).toInt())
            setOnClickListener {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(step1Label)
        layout.addView(step1Desc)
        layout.addView(enableButton)
        layout.addView(step2Label)
        layout.addView(step2Desc)
        layout.addView(switchButton)

        scrollView.addView(layout)
        setContentView(scrollView)
    }
}
