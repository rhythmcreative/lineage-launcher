package org.lineageos.launcher.preview

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator

/**
 * Official Material 3 / Motion Assist styled vector animation preview for the Scrollable Dock.
 * Pure Monochrome (Blanco y Negro) design with themed icons, high-contrast halos,
 * and fluid swipe gesture motion assist.
 */
class DockPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private val phoneBounds = RectF()
    private val screenBounds = RectF()
    private val dockBounds = RectF()
    private val tempRect = RectF()
    private val tempPath = Path()

    private val m3Interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)

    private var animProgress = 0f
    private var animator: ValueAnimator? = null

    private var isDragging = false
    private var touchDownX = 0f
    private var manualPageOffset = 0f

    init {
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startCycleAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    private fun startCycleAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                if (!isDragging) {
                    animProgress = animation.animatedValue as Float
                    invalidate()
                }
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        var width = MeasureSpec.getSize(widthMeasureSpec)
        if (width <= 0) {
            width = resources.displayMetrics.widthPixels
        }
        val height = (240 * density).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = x
                isDragging = true
                animator?.pause()
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = x - touchDownX
                    val delta = -dx / (width * 0.4f)
                    manualPageOffset = (manualPageOffset + delta).coerceIn(0f, 1f)
                    touchDownX = x
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    animator?.resume()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val density = resources.displayMetrics.density
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 1. Calculate animation phases
        val pageOffset: Float
        var touchVisible = false
        var touchAlpha = 0f
        var touchSub = 0f
        var swipeLeft = true

        if (isDragging) {
            pageOffset = manualPageOffset
        } else {
            when {
                animProgress < 0.15f -> pageOffset = 0f
                animProgress < 0.35f -> {
                    val subT = (animProgress - 0.15f) / 0.20f
                    pageOffset = m3Interpolator.getInterpolation(subT)
                    touchVisible = true
                    touchAlpha = Math.sin(subT * Math.PI).toFloat()
                    touchSub = subT
                    swipeLeft = true
                }
                animProgress < 0.65f -> pageOffset = 1.0f
                animProgress < 0.85f -> {
                    val subT = (animProgress - 0.65f) / 0.20f
                    pageOffset = 1.0f - m3Interpolator.getInterpolation(subT)
                    touchVisible = true
                    touchAlpha = Math.sin(subT * Math.PI).toFloat()
                    touchSub = subT
                    swipeLeft = false
                }
                else -> pageOffset = 0f
            }
        }

        // 2. Card Background (Material 3 Surface Container, 28dp radius)
        bounds.set(0f, 0f, w, h)
        val cardRadius = 28f * density
        paint.style = Paint.Style.FILL
        paint.color = if (isDark) Color.parseColor("#141414") else Color.parseColor("#F4F4F4")
        canvas.drawRoundRect(bounds, cardRadius, cardRadius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f * density
        paint.color = if (isDark) Color.parseColor("#262626") else Color.parseColor("#DEDEDE")
        canvas.drawRoundRect(bounds, cardRadius, cardRadius, paint)

        // 3. Ambient Motion Assist Inertial Flow Streams (Pure Monochrome)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        // Wave 1
        paint.strokeWidth = 3f * density
        paint.color = if (isDark) Color.argb(30, 255, 255, 255) else Color.argb(20, 0, 0, 0)
        tempPath.reset()
        tempPath.moveTo(30f * density, 195f * density)
        tempPath.cubicTo(100f * density, 170f * density, 180f * density, 185f * density, 280f * density, 215f * density)
        tempPath.lineTo(w - 30f * density, 170f * density)
        canvas.drawPath(tempPath, paint)

        // Wave 2
        paint.strokeWidth = 2f * density
        paint.color = if (isDark) Color.argb(20, 255, 255, 255) else Color.argb(15, 0, 0, 0)
        tempPath.reset()
        tempPath.moveTo(45f * density, 65f * density)
        tempPath.cubicTo(130f * density, 45f * density, 200f * density, 60f * density, 310f * density, 45f * density)
        tempPath.lineTo(w - 40f * density, 75f * density)
        canvas.drawPath(tempPath, paint)

        // Ambient decorative sparkles
        paint.style = Paint.Style.FILL
        paint.color = if (isDark) Color.argb(40, 255, 255, 255) else Color.argb(25, 0, 0, 0)
        canvas.drawCircle(79f * density, 112f * density, 3.5f * density, paint)
        canvas.drawCircle(w - 76f * density, 130f * density, 3.5f * density, paint)

        // 4. Stylized Pixel Phone in Center
        val phoneW = 146f * density
        val phoneH = 206f * density
        val phoneX = (w - phoneW) / 2f
        val phoneY = (h - phoneH) / 2f
        val phoneRadius = 24f * density
        phoneBounds.set(phoneX, phoneY, phoneX + phoneW, phoneY + phoneH)

        // Phone Hardware Bezel
        paint.style = Paint.Style.FILL
        paint.color = if (isDark) Color.parseColor("#1C1C1E") else Color.parseColor("#E6E6E8")
        canvas.drawRoundRect(phoneBounds, phoneRadius, phoneRadius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * density
        paint.color = if (isDark) Color.parseColor("#323236") else Color.parseColor("#C8C8CC")
        canvas.drawRoundRect(phoneBounds, phoneRadius, phoneRadius, paint)

        // Phone Screen Glass Area
        val screenPad = 5f * density
        val screenX = phoneX + screenPad
        val screenY = phoneY + screenPad
        val screenW = phoneW - screenPad * 2f
        val screenH = phoneH - screenPad * 2f
        val screenRadius = 20f * density
        screenBounds.set(screenX, screenY, screenX + screenW, screenY + screenH)

        canvas.save()
        tempPath.reset()
        tempPath.addRoundRect(screenBounds, screenRadius, screenRadius, Path.Direction.CW)
        canvas.clipPath(tempPath)

        // Screen Wallpaper Gradient (Monochrome Neutral)
        val gradTop = if (isDark) Color.parseColor("#121212") else Color.parseColor("#EDEDED")
        val gradBot = if (isDark) Color.parseColor("#080808") else Color.parseColor("#DFDFDF")
        val grad = LinearGradient(screenX, screenY, screenX, screenY + screenH, gradTop, gradBot, Shader.TileMode.CLAMP)
        paint.style = Paint.Style.FILL
        paint.shader = grad
        canvas.drawRect(screenBounds, paint)
        paint.shader = null

        // Subtle curved wallpaper accent
        paint.color = if (isDark) Color.argb(18, 255, 255, 255) else Color.argb(18, 0, 0, 0)
        tempRect.set(screenX - 20f * density, screenY + 30f * density, screenX + screenW + 30f * density, screenY + screenH + 40f * density)
        canvas.drawOval(tempRect, paint)

        // Camera Punch Hole
        paint.color = Color.parseColor("#000000")
        canvas.drawCircle(screenX + screenW / 2f, screenY + 7f * density, 2.8f * density, paint)

        // Top At-a-Glance widget silhouette
        paint.color = if (isDark) Color.argb(20, 255, 255, 255) else Color.argb(60, 0, 0, 0)
        tempRect.set(screenX + 12f * density, screenY + 18f * density, screenX + screenW - 12f * density, screenY + 36f * density)
        canvas.drawRoundRect(tempRect, 9f * density, 9f * density, paint)

        // Desktop workspace apps silhouettes
        val appR = 6.5f * density
        paint.color = if (isDark) Color.argb(18, 255, 255, 255) else Color.argb(50, 0, 0, 0)
        for (row in 0 until 2) {
            val rowY = screenY + 52f * density + row * 24f * density
            for (col in 0 until 4) {
                val cx = screenX + 16f * density + col * ((screenW - 32f * density) / 3f)
                canvas.drawCircle(cx, rowY, appR, paint)
            }
        }

        // Search Bar (QSB) - Pure Monochrome
        val qsbH = 16f * density
        val qsbY = screenY + screenH - 78f * density
        tempRect.set(screenX + 10f * density, qsbY, screenX + screenW - 10f * density, qsbY + qsbH)
        paint.color = if (isDark) Color.argb(25, 255, 255, 255) else Color.argb(60, 0, 0, 0)
        canvas.drawRoundRect(tempRect, qsbH / 2f, qsbH / 2f, paint)
        // Neutral Search icon indicator (White / Gray instead of blue)
        paint.color = if (isDark) Color.argb(180, 255, 255, 255) else Color.argb(160, 0, 0, 0)
        canvas.drawCircle(screenX + 18f * density, qsbY + qsbH / 2f, 2.8f * density, paint)

        // 5. Scrollable Dock Pill inside screen
        val dockH = 36f * density
        val dockY = screenY + screenH - 56f * density
        val dockMargin = 6f * density
        val dockX = screenX + dockMargin
        val dockW = screenW - dockMargin * 2f
        val dockRadius = dockH / 2f
        dockBounds.set(dockX, dockY, dockX + dockW, dockY + dockH)

        // Dock background
        paint.style = Paint.Style.FILL
        paint.color = if (isDark) Color.argb(38, 255, 255, 255) else Color.argb(70, 0, 0, 0)
        canvas.drawRoundRect(dockBounds, dockRadius, dockRadius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * density
        paint.color = if (isDark) Color.argb(55, 255, 255, 255) else Color.argb(90, 0, 0, 0)
        canvas.drawRoundRect(dockBounds, dockRadius, dockRadius, paint)

        // Icons sliding container
        canvas.save()
        tempPath.reset()
        tempPath.addRoundRect(dockBounds, dockRadius, dockRadius, Path.Direction.CW)
        canvas.clipPath(tempPath)

        val iconCy = dockY + dockH / 2f
        val iconR = 10.5f * density
        val slideOffset = -pageOffset * dockW

        drawMiniPage1(canvas, dockX + slideOffset, iconCy, dockW, iconR, density, isDark)
        drawMiniPage2(canvas, dockX + slideOffset + dockW, iconCy, dockW, iconR, density, isDark)

        canvas.restore() // Clip dock

        // 6. Motion Assist Adaptive Dots (Black & White with high-contrast halo)
        val dotsY = dockY + dockH + 6.5f * density
        val dotBaseR = 2.4f * density
        val dotSpacing = 11f * density
        val cDotsX = screenX + screenW / 2f
        val d1X = cDotsX - (dotSpacing / 2f)
        val d2X = cDotsX + (dotSpacing / 2f)

        val clampedOff = pageOffset.coerceIn(0f, 1f)
        val baseDotColor = if (isDark) Color.WHITE else Color.BLACK
        val haloColor = if (isDark) Color.argb(180, 0, 0, 0) else Color.argb(200, 255, 255, 255)

        paint.style = Paint.Style.FILL

        // Dot 1 (Page 1)
        val alpha1 = (255 - (255 - 90) * clampedOff).toInt()
        val halo1R = dotBaseR + 1.2f * density * (1f - clampedOff)
        paint.color = haloColor
        canvas.drawCircle(d1X, dotsY, halo1R, paint)
        paint.color = baseDotColor
        paint.alpha = alpha1
        canvas.drawCircle(d1X, dotsY, dotBaseR, paint)

        // Dot 2 (Page 2)
        val alpha2 = (90 + (255 - 90) * clampedOff).toInt()
        val halo2R = dotBaseR + 1.2f * density * clampedOff
        paint.color = haloColor
        canvas.drawCircle(d2X, dotsY, halo2R, paint)
        paint.color = baseDotColor
        paint.alpha = alpha2
        canvas.drawCircle(d2X, dotsY, dotBaseR, paint)

        // 7. Navigation Bar Gesture Pill
        val navW = 26f * density
        val navH = 2.2f * density
        val navY = screenY + screenH - 7f * density
        tempRect.set(screenX + screenW / 2f - navW / 2f, navY, screenX + screenW / 2f + navW / 2f, navY + navH)
        paint.color = if (isDark) Color.argb(120, 255, 255, 255) else Color.argb(120, 0, 0, 0)
        canvas.drawRoundRect(tempRect, navH / 2f, navH / 2f, paint)

        // 8. Interactive Touch Ripple (Finger gesture showing swipe)
        if (touchVisible && touchAlpha > 0.05f) {
            val tx = if (swipeLeft) {
                (dockX + dockW * 0.8f) - touchSub * (dockW * 0.6f)
            } else {
                (dockX + dockW * 0.2f) + touchSub * (dockW * 0.6f)
            }
            val ty = iconCy
            val touchR = 13f * density

            paint.style = Paint.Style.FILL
            paint.color = Color.argb((touchAlpha * 50).toInt(), 255, 255, 255)
            canvas.drawCircle(tx, ty, touchR * 1.5f, paint)

            paint.color = Color.argb((touchAlpha * 120).toInt(), 255, 255, 255)
            canvas.drawCircle(tx, ty, touchR, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f * density
            paint.color = Color.argb((touchAlpha * 200).toInt(), 255, 255, 255)
            canvas.drawCircle(tx, ty, touchR, paint)
        }

        canvas.restore() // Clip screen
    }

    private fun drawMiniPage1(canvas: Canvas, startX: Float, cy: Float, pageWidth: Float, r: Float, density: Float, isDark: Boolean) {
        val step = pageWidth / 5f
        val iconBg = if (isDark) Color.parseColor("#262A2E") else Color.parseColor("#E0E3E7")
        val iconFg = if (isDark) Color.parseColor("#F0F2F5") else Color.parseColor("#1B1F23")

        // 1. Phone (Monochrome Material You)
        val x0 = startX + step * 0.5f
        paint.style = Paint.Style.FILL
        paint.color = iconBg
        canvas.drawCircle(x0, cy, r, paint)
        paint.color = iconFg
        tempRect.set(x0 - 2.8f * density, cy - 4.2f * density, x0 + 2.8f * density, cy + 4.2f * density)
        canvas.drawRoundRect(tempRect, 2.2f * density, 2.2f * density, paint)

        // 2. Messages (Monochrome Material You)
        val x1 = startX + step * 1.5f
        paint.color = iconBg
        canvas.drawCircle(x1, cy, r, paint)
        paint.color = iconFg
        tempRect.set(x1 - 4f * density, cy - 3.2f * density, x1 + 4f * density, cy + 3.2f * density)
        canvas.drawRoundRect(tempRect, 2.5f * density, 2.5f * density, paint)

        // 3. Chrome / Browser (Monochrome Material You)
        val x2 = startX + step * 2.5f
        paint.color = iconBg
        canvas.drawCircle(x2, cy, r, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.3f * density
        paint.color = iconFg
        canvas.drawCircle(x2, cy, r * 0.45f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x2, cy, r * 0.2f, paint)

        // 4. Camera (Monochrome Material You)
        val x3 = startX + step * 3.5f
        paint.color = iconBg
        canvas.drawCircle(x3, cy, r, paint)
        paint.color = iconFg
        tempRect.set(x3 - 4.2f * density, cy - 3f * density, x3 + 4.2f * density, cy + 3.5f * density)
        canvas.drawRoundRect(tempRect, 1.5f * density, 1.5f * density, paint)
        paint.color = iconBg
        canvas.drawCircle(x3, cy + 0.3f * density, 1.8f * density, paint)

        // 5. Photos (Monochrome Material You)
        val x4 = startX + step * 4.5f
        paint.color = iconBg
        canvas.drawCircle(x4, cy, r, paint)
        paint.color = iconFg
        val pr = r * 0.35f
        canvas.drawCircle(x4, cy - pr * 0.6f, pr * 0.6f, paint)
        canvas.drawCircle(x4 + pr * 0.6f, cy, pr * 0.6f, paint)
        canvas.drawCircle(x4, cy + pr * 0.6f, pr * 0.6f, paint)
        canvas.drawCircle(x4 - pr * 0.6f, cy, pr * 0.6f, paint)
    }

    private fun drawMiniPage2(canvas: Canvas, startX: Float, cy: Float, pageWidth: Float, r: Float, density: Float, isDark: Boolean) {
        val step = pageWidth / 5f
        val iconBg = if (isDark) Color.parseColor("#262A2E") else Color.parseColor("#E0E3E7")
        val iconFg = if (isDark) Color.parseColor("#F0F2F5") else Color.parseColor("#1B1F23")

        // 1. Maps (Monochrome Material You)
        val x0 = startX + step * 0.5f
        paint.style = Paint.Style.FILL
        paint.color = iconBg
        canvas.drawCircle(x0, cy, r, paint)
        paint.color = iconFg
        canvas.drawCircle(x0, cy - 1.2f * density, r * 0.35f, paint)
        tempPath.reset()
        tempPath.moveTo(x0 - 2f * density, cy - 0.5f * density)
        tempPath.lineTo(x0, cy + 3.5f * density)
        tempPath.lineTo(x0 + 2f * density, cy - 0.5f * density)
        tempPath.close()
        canvas.drawPath(tempPath, paint)

        // 2. YT Music (Monochrome Material You)
        val x1 = startX + step * 1.5f
        paint.color = iconBg
        canvas.drawCircle(x1, cy, r, paint)
        paint.color = iconFg
        tempPath.reset()
        tempPath.moveTo(x1 - 2.2f * density, cy - 3f * density)
        tempPath.lineTo(x1 + 3.2f * density, cy)
        tempPath.lineTo(x1 - 2.2f * density, cy + 3f * density)
        tempPath.close()
        canvas.drawPath(tempPath, paint)

        // 3. Gmail (Monochrome Material You)
        val x2 = startX + step * 2.5f
        paint.color = iconBg
        canvas.drawCircle(x2, cy, r, paint)
        paint.color = iconFg
        tempRect.set(x2 - 3.8f * density, cy - 2.6f * density, x2 + 3.8f * density, cy + 2.6f * density)
        canvas.drawRoundRect(tempRect, 1.2f * density, 1.2f * density, paint)
        paint.color = iconBg
        canvas.drawCircle(x2, cy, 1.3f * density, paint)

        // 4. Keep (Monochrome Material You)
        val x3 = startX + step * 3.5f
        paint.color = iconBg
        canvas.drawCircle(x3, cy, r, paint)
        paint.color = iconFg
        tempRect.set(x3 - 2.8f * density, cy - 3.2f * density, x3 + 2.8f * density, cy + 3.2f * density)
        canvas.drawRoundRect(tempRect, 1.2f * density, 1.2f * density, paint)

        // 5. Settings (Monochrome Material You)
        val x4 = startX + step * 4.5f
        paint.color = iconBg
        canvas.drawCircle(x4, cy, r, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.4f * density
        paint.color = iconFg
        canvas.drawCircle(x4, cy, r * 0.42f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x4, cy, r * 0.18f, paint)
    }
}
