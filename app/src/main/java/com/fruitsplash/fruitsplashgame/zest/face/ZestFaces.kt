package com.fruitsplash.fruitsplashgame.zest.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.fruitsplash.fruitsplashgame.R

internal class ZestLoadView(context: Context) : View(context) {
    var progress: Float = 0.05f
        set(value) {
            field = value.coerceIn(0f, 1f)
            postInvalidateOnAnimation()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var port: Bitmap? = null
    private var land: Bitmap? = null
    private var started = System.currentTimeMillis()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (port == null) port = BitmapFactory.decodeResource(resources, R.drawable.vertical_loading_screen)
        if (land == null) land = BitmapFactory.decodeResource(resources, R.drawable.horizontal_loading_screen)
    }

    override fun onDraw(c: Canvas) {
        val bmp = if (width > height) land else port
        if (bmp != null) drawCropped(c, bmp)
        else c.drawColor(Color.rgb(22, 53, 29))

        val barTop = height * 0.82f
        val barBot = height * 0.875f
        paint.color = Color.argb(190, 9, 28, 18)
        c.drawRoundRect(width * 0.1f, barTop, width * 0.9f, barBot, 22f, 22f, paint)
        paint.color = Color.rgb(255, 193, 7)
        c.drawRoundRect(
            width * 0.11f,
            barTop + (barBot - barTop) * 0.18f,
            width * 0.11f + width * 0.78f * progress,
            barBot - (barBot - barTop) * 0.18f,
            18f,
            18f,
            paint,
        )
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = minOf(width, height) * 0.048f
        paint.typeface = Typeface.DEFAULT_BOLD
        val dots = ((System.currentTimeMillis() - started) / 450 % 3 + 1).toInt()
        c.drawText("Loading" + ".".repeat(dots), width / 2f, height * 0.79f, paint)
        if (progress < 1f) postInvalidateDelayed(150)
    }
}

internal class ZestStillView(
    context: Context,
    private val onRetry: () -> Unit,
) : View(context) {
    var busy: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var port: Bitmap? = null
    private var land: Bitmap? = null
    private var retryRect = RectF()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (port == null) port = BitmapFactory.decodeResource(resources, R.drawable.zest_still_port)
        if (land == null) land = BitmapFactory.decodeResource(resources, R.drawable.zest_still_land)
    }

    override fun onDraw(c: Canvas) {
        val bmp = if (width > height) land else port
        if (bmp != null) drawCropped(c, bmp)
        else c.drawColor(Color.rgb(22, 53, 29))
        val landscape = width > height
        val bw = if (landscape) width * 0.28f else width * 0.62f
        val bh = if (landscape) height * 0.155f else height * 0.072f
        val top = if (landscape) height * 0.78f else height * 0.82f
        retryRect.set((width - bw) / 2f, top, (width + bw) / 2f, top + bh)
        drawPill(c, retryRect, if (busy) "..." else "Retry", filled = true)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && retryRect.contains(event.x, event.y) && !busy) {
            onRetry()
            return true
        }
        return event.action == MotionEvent.ACTION_DOWN && retryRect.contains(event.x, event.y)
    }
}

internal class ZestPermitView(
    context: Context,
    private val onAccept: () -> Unit,
    private val onSkip: () -> Unit,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var port: Bitmap? = null
    private var land: Bitmap? = null
    private var acceptRect = RectF()
    private var skipRect = RectF()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (port == null) port = BitmapFactory.decodeResource(resources, R.drawable.zest_permit_port)
        if (land == null) land = BitmapFactory.decodeResource(resources, R.drawable.zest_permit_land)
    }

    override fun onDraw(c: Canvas) {
        val bmp = if (width > height) land else port
        if (bmp != null) drawCropped(c, bmp)
        else c.drawColor(Color.rgb(22, 53, 29))
        val landscape = width > height
        val bw = if (landscape) width * 0.26f else width * 0.64f
        val bh = if (landscape) height * 0.145f else height * 0.068f
        val gap = if (landscape) width * 0.035f else height * 0.018f
        if (landscape) {
            val top = height * 0.78f
            val pair = bw * 2f + gap
            val left = (width - pair) / 2f
            acceptRect.set(left, top, left + bw, top + bh)
            skipRect.set(left + bw + gap, top, left + bw + gap + bw, top + bh)
        } else {
            val acceptTop = height * 0.78f
            acceptRect.set((width - bw) / 2f, acceptTop, (width + bw) / 2f, acceptTop + bh)
            skipRect.set((width - bw) / 2f, acceptRect.bottom + gap, (width + bw) / 2f, acceptRect.bottom + gap + bh)
        }
        drawPill(c, acceptRect, "Accept", filled = true)
        drawPill(c, skipRect, "Skip", filled = false)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) {
            return acceptRect.contains(event.x, event.y) || skipRect.contains(event.x, event.y)
        }
        when {
            acceptRect.contains(event.x, event.y) -> onAccept()
            skipRect.contains(event.x, event.y) -> onSkip()
            else -> return false
        }
        return true
    }
}

private fun View.drawCropped(c: Canvas, bmp: Bitmap) {
    val scale = maxOf(width / bmp.width.toFloat(), height / bmp.height.toFloat())
    val dw = bmp.width * scale
    val dh = bmp.height * scale
    c.drawBitmap(
        bmp,
        null,
        RectF((width - dw) / 2f, (height - dh) / 2f, (width + dw) / 2f, (height + dh) / 2f),
        null,
    )
}

private fun View.drawPill(c: Canvas, rect: RectF, label: String, filled: Boolean) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = rect.height() / 2f
    if (filled) {
        paint.color = Color.rgb(255, 193, 7)
        c.drawRoundRect(rect, radius, radius, paint)
        paint.color = Color.rgb(22, 40, 18)
    } else {
        paint.color = Color.argb(160, 9, 28, 18)
        c.drawRoundRect(rect, radius, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.WHITE
        c.drawRoundRect(rect, radius, radius, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
    }
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = rect.height() * 0.42f
    paint.typeface = Typeface.DEFAULT_BOLD
    val y = rect.centerY() - (paint.descent() + paint.ascent()) / 2f
    c.drawText(label, rect.centerX(), y, paint)
}