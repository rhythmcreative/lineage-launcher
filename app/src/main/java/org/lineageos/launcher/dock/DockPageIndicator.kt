package org.lineageos.launcher.dock

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Material 3 Adaptive Monochrome Page Dots Indicator for the Scrollable Dock.
 * Draws high-contrast page dots with an outer dark/light halo to remain legible
 * over dynamic wallpapers.
 */
class DockPageIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pageCount: Int = 1
    private var activePage: Int = 0
    private var positionOffset: Float = 0f

    private val density = context.resources.displayMetrics.density
    private val dotRadius = 3f * density
    private val dotSpacing = 14f * density

    fun setPageCount(count: Int) {
        pageCount = count.coerceAtLeast(1)
        visibility = if (pageCount > 1) VISIBLE else GONE
        requestLayout()
        invalidate()
    }

    fun onPageScrolled(position: Int, offset: Float) {
        activePage = position
        positionOffset = offset
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = if (pageCount <= 1) {
            0
        } else {
            ((pageCount - 1) * dotSpacing + dotRadius * 4).toInt() + paddingLeft + paddingRight
        }
        val height = (dotRadius * 4).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pageCount <= 1) return

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val activeDotColor = if (isDark) Color.WHITE else Color.parseColor("#1C1C1E")
        val inactiveDotColor = if (isDark) Color.argb(100, 255, 255, 255) else Color.argb(90, 28, 28, 30)
        val haloColor = if (isDark) Color.argb(180, 0, 0, 0) else Color.argb(180, 255, 255, 255)

        val totalWidth = (pageCount - 1) * dotSpacing
        val startX = (width - totalWidth) / 2f
        val cy = height / 2f

        for (i in 0 until pageCount) {
            val cx = startX + i * dotSpacing

            // Compute interpolated proximity to active indicator position
            val currentPos = activePage + positionOffset
            val distance = Math.abs(currentPos - i).coerceAtMost(1f)
            val factor = 1f - distance

            val radius = dotRadius + (1.2f * density * factor)

            // 1. Protective Contrast Halo
            paint.style = Paint.Style.FILL
            paint.color = haloColor
            canvas.drawCircle(cx, cy, radius + 1.2f * density, paint)

            // 2. Monochrome Dot
            paint.color = if (factor > 0.5f) activeDotColor else inactiveDotColor
            paint.alpha = (90 + (255 - 90) * factor).toInt()
            canvas.drawCircle(cx, cy, radius, paint)
        }
    }
}
