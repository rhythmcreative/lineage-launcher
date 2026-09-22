package org.lineageos.launcher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.launcher.model.AppInfo

/**
 * Modern Alphabetical FastScroller (A-Z) matching LineageOS Trebuchet and GrapheneOS AllApps.
 * Supports touch drag, section jumping, haptic ticks, and floating letter indicator.
 */
class FastScrollerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val alphabet = listOf(
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
        "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
        "U", "V", "W", "X", "Y", "Z", "#"
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private var selectedIndex = -1
    private var recyclerView: RecyclerView? = null
    private var appsList: List<AppInfo> = emptyList()
    private var popupIndicator: TextView? = null

    fun attachRecyclerView(rv: RecyclerView, popup: TextView? = null) {
        this.recyclerView = rv
        this.popupIndicator = popup
    }

    fun setApps(apps: List<AppInfo>) {
        this.appsList = apps
        visibility = if (apps.size >= 12) View.VISIBLE else View.GONE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (alphabet.isEmpty() || height <= 0) return

        val itemHeight = height.toFloat() / alphabet.size
        val x = width / 2f

        for (i in alphabet.indices) {
            val letter = alphabet[i]
            val y = (i * itemHeight) + (itemHeight / 2f) + (paint.textSize / 3f)

            if (i == selectedIndex) {
                canvas.drawText(letter, x, y, highlightPaint)
            } else {
                canvas.drawText(letter, x, y, paint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val index = ((event.y / height) * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)
                if (index != selectedIndex) {
                    selectedIndex = index
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    val letter = alphabet[selectedIndex]
                    scrollToLetter(letter)
                    showPopup(letter)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                selectedIndex = -1
                hidePopup()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun showPopup(letter: String) {
        popupIndicator?.apply {
            text = letter
            visibility = View.VISIBLE
        }
    }

    private fun hidePopup() {
        popupIndicator?.visibility = View.GONE
    }

    private fun scrollToLetter(letter: String) {
        if (appsList.isEmpty() || recyclerView == null) return

        val targetIndex = if (letter == "#") {
            appsList.indexOfFirst { !it.label.first().isLetter() }
        } else {
            appsList.indexOfFirst { it.label.startsWith(letter, ignoreCase = true) }
        }

        if (targetIndex >= 0) {
            val layoutManager = recyclerView?.layoutManager
            when (layoutManager) {
                is GridLayoutManager -> layoutManager.scrollToPositionWithOffset(targetIndex, 0)
                is LinearLayoutManager -> layoutManager.scrollToPositionWithOffset(targetIndex, 0)
                else -> recyclerView?.scrollToPosition(targetIndex)
            }
        }
    }
}
