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
import android.graphics.Path
import com.unum.keyboard.core.KeyAction
import com.unum.keyboard.core.KeyboardState
import com.unum.keyboard.core.TextAction
import com.unum.keyboard.gesture.FlickGestureDetector
import com.unum.keyboard.gesture.GestureCandidate
import com.unum.keyboard.gesture.GestureDecoder
import com.unum.keyboard.gesture.GesturePathTracker
import com.unum.keyboard.hittarget.DynamicHitTargetResolver
import com.unum.keyboard.hittarget.TouchCalibrator
import com.unum.keyboard.layout.FlickDirection
import com.unum.keyboard.layout.KeyGeometry
import com.unum.keyboard.layout.KeyType
import com.unum.keyboard.layout.LayoutEngine
import com.unum.keyboard.prediction.TrieDictionary
import com.unum.keyboard.text.CursorController
import com.unum.keyboard.text.EditingAction

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface KeyboardActionListener {
        fun onText(text: String)
        fun onDelete()
        fun onEnter()
        /** Called when gesture typing produces word candidates */
        fun onGestureWord(candidates: List<GestureCandidate>) {}
        /** Called when a text editing action is triggered (M10) */
        fun onTextAction(action: TextAction) {}
        /** Called when an editing toolbar action is triggered (M10) */
        fun onEditingAction(action: EditingAction) {}
    }

    var listener: KeyboardActionListener? = null

    /** Enable/disable gesture typing mode */
    var gestureTypingEnabled: Boolean = false

    private val keyboardState = KeyboardState()
    private val layoutEngine = LayoutEngine()
    private var computedLayout: LayoutEngine.ComputedLayout? = null

    // Dynamic hit target resolution (M6)
    private val hitTargetResolver = DynamicHitTargetResolver()

    // Flick gesture detection (M7)
    private val flickDetector = FlickGestureDetector()
    private var flickOriginKey: KeyGeometry? = null

    // Gesture typing (M8)
    private val gestureTracker = GesturePathTracker()

    // Spacebar trackpad cursor control (M10)
    private val cursorController = CursorController()
    private var isSpacebarTrackpadActive = false
    private var gestureDecoder: GestureDecoder? = null
    private var isGesturing: Boolean = false
    private val gesturePath = Path()  // Android graphics path for drawing the swipe trail

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
    private val flickHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = FLICK_HINT_COLOR
        textAlign = Paint.Align.CENTER
    }
    private val gestureTrailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GESTURE_TRAIL_COLOR
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val trackpadActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TRACKPAD_ACTIVE_COLOR
        style = Paint.Style.FILL
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
            flickHintPaint.textSize = FLICK_HINT_SIZE * resources.displayMetrics.density
            gestureTrailPaint.strokeWidth = GESTURE_TRAIL_WIDTH * resources.displayMetrics.density
        }
    }

    /**
     * Set the dictionary for gesture typing word lookup.
     * Must be called before gesture typing will produce results.
     */
    fun setDictionary(dictionary: TrieDictionary) {
        gestureDecoder = GestureDecoder(dictionary)
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

            // Draw flick-up hint in top-right corner of key
            val flickUp = geo.key.flickUp
            if (flickUp != null && !isSpecial) {
                val hintX = geo.bounds.right - cornerRadius
                val hintY = geo.bounds.top + flickHintPaint.textSize + 2f * density
                canvas.drawText(flickUp, hintX, hintY, flickHintPaint)
            }
        }

        // Draw gesture trail
        if (isGesturing && !gesturePath.isEmpty) {
            canvas.drawPath(gesturePath, gestureTrailPaint)
        }

        // Draw trackpad active indicator on spacebar
        if (isSpacebarTrackpadActive) {
            val spaceGeo = layout.keys.find { it.key.type == KeyType.SPACE }
            if (spaceGeo != null) {
                val rect = RectF(spaceGeo.bounds.left, spaceGeo.bounds.top,
                    spaceGeo.bounds.right, spaceGeo.bounds.bottom)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, trackpadActivePaint)
                val label = if (cursorController.isSelecting) "SELECT" else "TRACKPAD"
                val labelPaint = specialTextPaint
                val ty = spaceGeo.center.y - (labelPaint.descent() + labelPaint.ascent()) / 2f
                canvas.drawText(label, spaceGeo.center.x, ty, labelPaint)
            }
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
        val timestamp = event.eventTime

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val geo = resolveKey(event.x, event.y)
                if (geo != null) {
                    pressedKeyId = geo.key.id
                    flickOriginKey = geo
                    flickDetector.onTouchStart(event.x, event.y, timestamp)

                    // Start gesture tracking if enabled (on character keys only)
                    if (gestureTypingEnabled && geo.key.type == KeyType.CHARACTER) {
                        gestureTracker.start(event.x, event.y, timestamp)
                        gestureTracker.updateCurrentKey(geo)
                        gesturePath.reset()
                        gesturePath.moveTo(event.x, event.y)
                        isGesturing = false // not yet — becomes true after crossing 2+ keys
                    }

                    // Start cursor controller tracking on spacebar (M10)
                    if (geo.key.type == KeyType.SPACE) {
                        cursorController.onTouchStart(event.x, event.y, timestamp)
                    }

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
            MotionEvent.ACTION_MOVE -> {
                // Spacebar trackpad cursor control (M10)
                if (pressedKeyId == "space") {
                    val consumed = cursorController.onTouchMove(event.x, event.y, timestamp)
                    if (consumed) {
                        if (!isSpacebarTrackpadActive) {
                            isSpacebarTrackpadActive = true
                            // Cancel gesture/flick tracking
                            flickDetector.cancel()
                            gestureTracker.reset()
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                        // Dispatch pending cursor actions
                        for (action in cursorController.drainActions()) {
                            listener?.onTextAction(action)
                        }
                        invalidate()
                        return true
                    }
                }

                // Gesture typing mode: track the swipe path
                if (gestureTracker.isActive) {
                    gestureTracker.addPoint(event.x, event.y, timestamp)
                    gesturePath.lineTo(event.x, event.y)

                    val geo = layout.findKeyAt(event.x, event.y)
                    gestureTracker.updateCurrentKey(geo)

                    // Transition to gesture mode once we've crossed enough keys
                    if (!isGesturing && gestureTracker.isValidGesture()) {
                        isGesturing = true
                        // Cancel flick detection — this is a swipe, not a flick
                        flickDetector.cancel()
                        flickOriginKey = null
                        isBackspaceHeld = false
                        handler.removeCallbacks(backspaceRepeatRunnable)
                    }

                    if (isGesturing) {
                        // Highlight current key under finger
                        pressedKeyId = geo?.key?.id
                        invalidate()
                        return true
                    }
                }

                // Check for flick during move (for immediate feedback)
                if (!isGesturing) {
                    val flickDir = flickDetector.onTouchMove(event.x, event.y, timestamp)
                    if (flickDir != FlickDirection.NONE) {
                        val originKey = flickOriginKey
                        if (originKey != null) {
                            val flickChar = originKey.key.flickChar(flickDir)
                            if (flickChar != null) {
                                isBackspaceHeld = false
                                handler.removeCallbacks(backspaceRepeatRunnable)
                                gestureTracker.reset()

                                listener?.onText(flickChar)
                                keyboardState.autoUnshift()
                                recomputeLayout()
                                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                pressedKeyId = null
                                flickOriginKey = null
                                invalidate()
                                return true
                            }
                        }
                    }

                    // Normal move tracking for key highlight
                    val geo = resolveKey(event.x, event.y)
                    val newId = geo?.key?.id
                    if (newId != pressedKeyId && !flickDetector.tracking) {
                        if (pressedKeyId == "backspace") {
                            isBackspaceHeld = false
                            handler.removeCallbacks(backspaceRepeatRunnable)
                        }
                        pressedKeyId = newId
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBackspaceHeld = false
                handler.removeCallbacks(backspaceRepeatRunnable)

                // Handle spacebar trackpad end (M10)
                if (isSpacebarTrackpadActive) {
                    cursorController.onTouchEnd(event.x, event.y, timestamp)
                    isSpacebarTrackpadActive = false
                    pressedKeyId = null
                    flickOriginKey = null
                    invalidate()
                    return true
                }
                cursorController.cancel()

                if (event.action == MotionEvent.ACTION_UP) {
                    // Check if we were gesture typing
                    if (isGesturing) {
                        gestureTracker.addPoint(event.x, event.y, timestamp)
                        val geo = layout.findKeyAt(event.x, event.y)
                        gestureTracker.updateCurrentKey(geo)

                        val gesturePath = gestureTracker.finish()
                        val decoder = gestureDecoder
                        if (decoder != null && gesturePath.characters.isNotEmpty()) {
                            val candidates = decoder.decode(gesturePath, layout.keys)
                            if (candidates.isNotEmpty()) {
                                // Insert the top candidate and notify listener of all candidates
                                listener?.onText(candidates[0].word)
                                listener?.onText(" ") // auto-space after gesture word
                                listener?.onGestureWord(candidates)
                            }
                        }

                        isGesturing = false
                        this.gesturePath.reset()
                        gestureTracker.reset()
                        pressedKeyId = null
                        flickOriginKey = null
                        invalidate()
                        return true
                    }

                    gestureTracker.reset()

                    val flickResult = flickDetector.onTouchEnd(event.x, event.y, timestamp)
                    val originKey = flickOriginKey

                    if (!flickResult.isTap && flickResult.direction != FlickDirection.NONE && originKey != null) {
                        val flickChar = originKey.key.flickChar(flickResult.direction)
                        if (flickChar != null) {
                            listener?.onText(flickChar)
                            keyboardState.autoUnshift()
                            recomputeLayout()
                        } else {
                            handleKeyPress(originKey)
                        }
                    } else if (flickResult.isTap) {
                        val geo = originKey ?: resolveKey(event.x, event.y)
                        if (geo != null) {
                            hitTargetResolver.calibrator.recordTouch(event.x, event.y, geo)
                            handleKeyPress(geo)
                        }
                    }
                } else {
                    flickDetector.cancel()
                    gestureTracker.reset()
                    isGesturing = false
                    gesturePath.reset()
                }

                pressedKeyId = null
                flickOriginKey = null
                invalidate()
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

        private const val FLICK_HINT_COLOR = 0xFF666666.toInt()  // Subtle gray hints
        private const val GESTURE_TRAIL_COLOR = 0x80FFFFFF.toInt() // Semi-transparent white trail
        private const val TRACKPAD_ACTIVE_COLOR = 0xFF0D47A1.toInt() // Blue tint when trackpad active
        private const val KEYBOARD_HEIGHT_DP = 260f
        private const val HORIZONTAL_PADDING = 3f  // dp
        private const val VERTICAL_PADDING = 6f    // dp
        private const val KEY_SPACING = 4f          // dp
        private const val TEXT_SIZE = 22f            // sp
        private const val SPECIAL_TEXT_SIZE = 14f    // sp
        private const val FLICK_HINT_SIZE = 10f      // sp
        private const val GESTURE_TRAIL_WIDTH = 3f    // dp

        private const val BACKSPACE_REPEAT_DELAY = 400L   // ms before repeat starts
        private const val BACKSPACE_REPEAT_INTERVAL = 50L  // ms between repeats
    }
}
