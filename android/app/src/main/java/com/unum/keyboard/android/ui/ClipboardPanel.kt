package com.unum.keyboard.android.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.unum.keyboard.text.ClipEntry
import com.unum.keyboard.text.ClipboardManager
import com.unum.keyboard.text.SlideboardManager
import com.unum.keyboard.text.TextSnippet

/**
 * A panel that shows clipboard history and text snippets.
 * Slides in over the keyboard when activated.
 */
class ClipboardPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface ClipboardPanelListener {
        fun onClipSelected(text: String)
        fun onSnippetSelected(text: String)
        fun onClipboardPanelClosed()
    }

    var listener: ClipboardPanelListener? = null

    private val clipboardManager = ClipboardManager()
    private val slideboardManager = SlideboardManager()

    private val density = resources.displayMetrics.density
    private val contentContainer: LinearLayout
    private val tabBar: LinearLayout
    private val scrollView: ScrollView
    private val itemContainer: LinearLayout

    private var activeTab: Tab = Tab.CLIPBOARD

    enum class Tab { CLIPBOARD, SNIPPETS }

    init {
        setBackgroundColor(BG_COLOR)
        visibility = View.GONE

        // Main vertical layout
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        // Header with tabs and close button
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(HEADER_BG_COLOR)
            gravity = Gravity.CENTER_VERTICAL
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Tab bar
        tabBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val clipTab = makeTabButton("Clipboard") { switchTab(Tab.CLIPBOARD) }
        val snippetTab = makeTabButton("Snippets") { switchTab(Tab.SNIPPETS) }
        tabBar.addView(clipTab)
        tabBar.addView(snippetTab)
        headerLayout.addView(tabBar)

        // Close button
        val closeBtn = TextView(context).apply {
            text = "✕"
            textSize = 18f
            setTextColor(TEXT_COLOR)
            gravity = Gravity.CENTER
            val pad = (12 * density).toInt()
            setPadding(pad, pad, pad, pad)
            setOnClickListener { hide() }
        }
        headerLayout.addView(closeBtn)

        mainLayout.addView(headerLayout)

        // Scrollable content
        scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        itemContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (8 * density).toInt()
            setPadding(pad, 0, pad, pad)
        }

        scrollView.addView(itemContainer)
        mainLayout.addView(scrollView)
        contentContainer = mainLayout
        addView(contentContainer)
    }

    private fun makeTabButton(label: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = label
            textSize = 14f
            setTextColor(TEXT_COLOR)
            gravity = Gravity.CENTER
            val pad = (12 * density).toInt()
            setPadding(pad, (6 * density).toInt(), pad, (6 * density).toInt())
            setOnClickListener { onClick() }
        }
    }

    private fun switchTab(tab: Tab) {
        activeTab = tab
        refreshContent()
    }

    /**
     * Show the clipboard panel.
     */
    fun show() {
        visibility = View.VISIBLE
        refreshContent()
    }

    /**
     * Hide the clipboard panel.
     */
    fun hide() {
        visibility = View.GONE
        listener?.onClipboardPanelClosed()
    }

    /** Whether the panel is currently visible */
    val isShowing: Boolean get() = visibility == View.VISIBLE

    /**
     * Get the clipboard manager for external access.
     */
    fun getClipboardManager(): ClipboardManager = clipboardManager

    /**
     * Get the slideboard manager for external access.
     */
    fun getSlideboardManager(): SlideboardManager = slideboardManager

    /**
     * Refresh the panel content based on the active tab.
     */
    fun refreshContent() {
        itemContainer.removeAllViews()

        when (activeTab) {
            Tab.CLIPBOARD -> showClipboardItems()
            Tab.SNIPPETS -> showSnippetItems()
        }
    }

    private fun showClipboardItems() {
        val clips = clipboardManager.history

        if (clips.isEmpty()) {
            addEmptyMessage("No clipboard history.\nCopied text will appear here.")
            return
        }

        for ((index, clip) in clips.withIndex()) {
            addClipItem(clip, index)
        }
    }

    private fun showSnippetItems() {
        val snippets = slideboardManager.snippets

        if (snippets.isEmpty()) {
            addEmptyMessage("No snippets saved.\nAdd frequently used text here.")
            return
        }

        for ((index, snippet) in snippets.withIndex()) {
            addSnippetItem(snippet, index)
        }
    }

    private fun addClipItem(clip: ClipEntry, index: Int) {
        val item = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(ITEM_BG_COLOR)
            gravity = Gravity.CENTER_VERTICAL
            val pad = (12 * density).toInt()
            setPadding(pad, pad, pad, pad)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = (4 * density).toInt()
            layoutParams = params

            // Pin indicator
            if (clip.isPinned) {
                addView(TextView(context).apply {
                    text = "📌 "
                    textSize = 14f
                })
            }

            // Clip text
            addView(TextView(context).apply {
                text = clip.preview
                textSize = 14f
                setTextColor(TEXT_COLOR)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                maxLines = 2
            })

            setOnClickListener {
                listener?.onClipSelected(clip.text)
            }

            setOnLongClickListener {
                if (clip.isPinned) {
                    clipboardManager.unpinClip(index)
                } else {
                    clipboardManager.pinClip(index)
                }
                refreshContent()
                true
            }
        }
        itemContainer.addView(item)
    }

    private fun addSnippetItem(snippet: TextSnippet, index: Int) {
        val item = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ITEM_BG_COLOR)
            val pad = (12 * density).toInt()
            setPadding(pad, pad, pad, pad)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = (4 * density).toInt()
            layoutParams = params

            // Label
            addView(TextView(context).apply {
                text = snippet.label
                textSize = 13f
                setTextColor(LABEL_COLOR)
            })

            // Text preview
            addView(TextView(context).apply {
                val preview = if (snippet.text.length > 60) snippet.text.take(60) + "…" else snippet.text
                text = preview
                textSize = 14f
                setTextColor(TEXT_COLOR)
                maxLines = 2
            })

            setOnClickListener {
                slideboardManager.recordUsage(snippet.label)
                listener?.onSnippetSelected(snippet.text)
            }
        }
        itemContainer.addView(item)
    }

    private fun addEmptyMessage(message: String) {
        itemContainer.addView(TextView(context).apply {
            text = message
            textSize = 14f
            setTextColor(EMPTY_TEXT_COLOR)
            gravity = Gravity.CENTER
            val pad = (32 * density).toInt()
            setPadding(pad, pad, pad, pad)
        })
    }

    companion object {
        private const val BG_COLOR = 0xFF000000.toInt()
        private const val HEADER_BG_COLOR = 0xFF111111.toInt()
        private const val ITEM_BG_COLOR = 0xFF1A1A1A.toInt()
        private const val TEXT_COLOR = 0xFFDDDDDD.toInt()
        private const val LABEL_COLOR = 0xFF888888.toInt()
        private const val EMPTY_TEXT_COLOR = 0xFF666666.toInt()
    }
}
