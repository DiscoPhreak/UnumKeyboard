package com.unum.keyboard.android.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.unum.keyboard.core.KeyAction
import com.unum.keyboard.core.KeyboardState
import com.unum.keyboard.hittarget.DynamicHitTargetResolver
import com.unum.keyboard.hittarget.TouchCalibrator
import com.unum.keyboard.layout.KeyGeometry
import com.unum.keyboard.layout.KeyType
import com.unum.keyboard.layout.LayoutEngine

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface KeyboardActionListener {
        fun onText(text: String)
        fun onDelete()
        fun onEnter()
    }

    var listener: KeyboardActionListener? = null

    private val keyboardState = KeyboardState()
    private val layoutEngine = LayoutEngine()
    private var computedLayout: LayoutEngine.ComputedLayout? = null

    // Dynamic hit target resolution (M6)
    private val hitTargetResolver = DynamicHitTargetResolver()

    /** Current word prefix — set by the IME to inform hit target predictions */
    var currentPrefix: String = ""

    /**
     * Resolve which key was tapped using Bayesian hit target resolution.
     * Falls back to simple point-in-rect if no layout is computed.
     */
    private fun resolveKey(x: Float, y: Float): KeyGeometry? {
        val layout = computedLayout ?: return null
        return hitTargetResolver.resolve(x, y, layout.keys, currentPrefix)
            ?: layout.findKeyAt(x, y) // fallback for edge cases
    }

    // Paint objects
    private val keyBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = KEY_BG_COLOR
        style = Paint.Style.FILL
    }
    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = KEY_PRESSED_COLOR
        style = Paint.Style.FILL
    }
    private val specialKeyBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SPECIAL_KEY_BG_COLOR
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_COLOR
        textAlign = Paint.Align.CENTER
    }
    private val specialTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SPECIAL_TEXT_COLOR
        textAlign = Paint.Align.CENTER
    }
    private val backgroundPaint = Paint().apply {
        color = BG_COLOR
    }

    private var pressedKeyId: String? = null
    private val keyCornerRadius = 8f

    // Backspace repeat
    private val handler = Handler(Looper.getMainLooper())
    private var isBackspaceHeld = false
    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (isBackspaceHeld) {
                listener?.onDelete()
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                handler.postDelayed(this, BACKSPACE_REPEAT_INTERVAL)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeLayout()
    }

    private fun recomputeLayout() {
        if (width > 0 && height > 0) {
            computedLayout = layoutEngine.computeLayout(
                layout = keyboardState.currentLayout,
                screenWidth = width.toFloat(),
                keyboardHeight = height.toFloat(),
                horizontalPadding = HORIZONTAL_PADDING * resources.displayMetrics.density,
                verticalPadding = VERTICAL_PADDING * resources.displayMetrics.density,
                keySpacing = KEY_SPACING * resources.displayMetrics.density
            )
            textPaint.textSize = TEXT_SIZE * resources.displayMetrics.density
            specialTextPaint.textSize = SPECIAL_TEXT_SIZE * resources.displayMetrics.density
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val layout = computedLayout ?: return
        val density = resources.displayMetrics.density
        val cornerRadius = keyCornerRadius * density

        for (geo in layout.keys) {
            val isPressed = geo.key.id == pressedKeyId
            val isSpecial = geo.key.type != KeyType.CHARACTER
            val bgPaint = when {
                isPressed -> keyPressedPaint
                isSpecial -> specialKeyBgPaint
                else -> keyBgPaint
            }

            val rect = RectF(geo.bounds.left, geo.bounds.top, geo.bounds.right, geo.bounds.bottom)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

            val paint = if (isSpecial) specialTextPaint else textPaint
            val label = getKeyLabel(geo)
            val textY = geo.center.y - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(label, geo.center.x, textY, paint)
        }
    }

    private fun getKeyLabel(geo: KeyGeometry): String {
        return when (geo.key.type) {
            KeyType.SHIFT -> when (keyboardState.shiftState) {
                com.unum.keyboard.core.ShiftState.OFF -> "⇧"
                com.unum.keyboard.core.ShiftState.ON -> "⬆"
                com.unum.keyboard.core.ShiftState.CAPS_LOCK -> "⇪"
            }
            else -> geo.key.primary
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val layout = computedLayout ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val geo = resolveKey(event.x, event.y)
                if (geo != null) {
                    pressedKeyId = geo.key.id
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                    if (geo.key.type == KeyType.BACKSPACE) {
                        isBackspaceHeld = true
                        listener?.onDelete()
                        handler.postDelayed(backspaceRepeatRunnable, BACKSPACE_REPEAT_DELAY)
                    }

                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBackspaceHeld = false
                handler.removeCallbacks(backspaceRepeatRunnable)

                if (event.action == MotionEvent.ACTION_UP) {
                    val geo = resolveKey(event.x, event.y)
                    if (geo != null) {
                        // Record touch for calibration learning
                        hitTargetResolver.calibrator.recordTouch(event.x, event.y, geo)
                        handleKeyPress(geo)
                    }
                }

                pressedKeyId = null
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val geo = resolveKey(event.x, event.y)
                val newId = geo?.key?.id
                if (newId != pressedKeyId) {
                    if (pressedKeyId == "backspace") {
                        isBackspaceHeld = false
                        handler.removeCallbacks(backspaceRepeatRunnable)
                    }
                    pressedKeyId = newId
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleKeyPress(geo: KeyGeometry) {
        when (geo.key.type) {
            KeyType.CHARACTER -> {
                listener?.onText(geo.key.primary)
                keyboardState.autoUnshift()
                recomputeLayout()
                invalidate()
            }
            KeyType.SPACE -> {
                listener?.onText(" ")
            }
            KeyType.ENTER -> {
                listener?.onEnter()
            }
            KeyType.BACKSPACE -> {
                // Already handled in ACTION_DOWN
            }
            KeyType.SHIFT -> {
                keyboardState.toggleShift()
                recomputeLayout()
                invalidate()
            }
            KeyType.SYMBOL_TOGGLE -> {
                keyboardState.handleSymbolToggle(geo.key.id)
                recomputeLayout()
                invalidate()
            }
            KeyType.CTRL -> {
                // Future M10
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val density = resources.displayMetrics.density
        val height = (KEYBOARD_HEIGHT_DP * density).toInt()
        setMeasuredDimension(width, height)
    }

    companion object {
        // AMOLED dark theme colors
        private const val BG_COLOR = 0xFF000000.toInt()        // Pure black
        private const val KEY_BG_COLOR = 0xFF1A1A1A.toInt()    // Dark gray keys
        private const val KEY_PRESSED_COLOR = 0xFF333333.toInt() // Pressed highlight
        private const val SPECIAL_KEY_BG_COLOR = 0xFF2A2A2A.toInt() // Special keys
        private const val TEXT_COLOR = Color.WHITE
        private const val SPECIAL_TEXT_COLOR = 0xFFBBBBBB.toInt()

        private const val KEYBOARD_HEIGHT_DP = 260f
        private const val HORIZONTAL_PADDING = 3f  // dp
        private const val VERTICAL_PADDING = 6f    // dp
        private const val KEY_SPACING = 4f          // dp
        private const val TEXT_SIZE = 22f            // sp
        private const val SPECIAL_TEXT_SIZE = 14f    // sp

        private const val BACKSPACE_REPEAT_DELAY = 400L   // ms before repeat starts
        private const val BACKSPACE_REPEAT_INTERVAL = 50L  // ms between repeats
    }
}
