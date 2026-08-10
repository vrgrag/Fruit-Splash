package com.fruitsplash.fruitsplashgame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

object Palette {
    val panelTop = Color.argb(236, 30, 92, 50)
    val panelBottom = Color.argb(244, 9, 44, 23)
    val panelEdge = Color.argb(150, 172, 244, 184)
    val cardTop = Color.argb(226, 24, 76, 42)
    val cardBottom = Color.argb(238, 10, 42, 22)
    val cream = Color.rgb(255, 250, 234)
    val mango = Color.rgb(255, 160, 30)
    val mangoDeep = Color.rgb(226, 104, 10)
    val leaf = Color.rgb(62, 182, 88)
    val leafDeep = Color.rgb(22, 116, 52)
    val berry = Color.rgb(230, 66, 108)
    val berryDeep = Color.rgb(172, 26, 70)
    val sky = Color.rgb(74, 192, 240)
    val skyDeep = Color.rgb(26, 126, 190)
    val gold = Color.rgb(255, 208, 68)
    val muted = Color.argb(215, 200, 232, 206)
    val locked = Color.rgb(96, 108, 96)
    val lockedDeep = Color.rgb(52, 62, 52)
    val health = Color.rgb(242, 80, 72)
    val healthLight = Color.rgb(255, 164, 152)
    val energy = Color.rgb(255, 198, 36)
    val energyLight = Color.rgb(255, 242, 162)
    val restore = Color.rgb(64, 216, 118)
    val restoreLight = Color.rgb(180, 255, 202)
}

enum class BtnStyle { PRIMARY, LEAF, BERRY, SKY, GHOST }

/** Shared drawing primitives so every screen keeps the same premium-casual look. */
class UiKit(private val p: Paint) {

    fun reset() {
        p.shader = null
        p.colorFilter = null
        p.style = Paint.Style.FILL
        p.alpha = 255
        p.clearShadowLayer()
        p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.DEFAULT
    }

    fun shade(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255),
    )

    fun dropShadow(c: Canvas, r: RectF, rad: Float, dy: Float, alpha: Int) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = Color.argb(alpha, 0, 0, 0)
        c.drawRoundRect(RectF(r.left + 2f, r.top + dy, r.right + 2f, r.bottom + dy), rad, rad, p)
    }

    fun panel(
        c: Canvas,
        r: RectF,
        rad: Float,
        top: Int = Palette.panelTop,
        bottom: Int = Palette.panelBottom,
        edge: Int = Palette.panelEdge,
        shadow: Boolean = true,
    ) {
        if (shadow) dropShadow(c, r, rad, rad * 0.4f + 4f, 78)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(r.left, r.top, r.left, r.bottom, top, bottom, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, rad, rad, p)
        p.shader = null
        val glossH = minOf(r.height() * 0.4f, rad * 2.6f)
        p.color = Color.argb(38, 255, 255, 255)
        c.drawRoundRect(
            RectF(r.left + rad * 0.3f, r.top + rad * 0.22f, r.right - rad * 0.3f, r.top + glossH),
            rad * 0.7f,
            rad * 0.7f,
            p,
        )
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        p.color = edge
        c.drawRoundRect(r, rad, rad, p)
        reset()
    }

    fun button(
        c: Canvas,
        r: RectF,
        label: String,
        style: BtnStyle = BtnStyle.PRIMARY,
        enabled: Boolean = true,
        pressed: Boolean = false,
        icon: String? = null,
        textSize: Float = r.height() * 0.38f,
    ) {
        val sink = if (pressed) r.height() * 0.06f else 0f
        val face = RectF(r.left, r.top + sink, r.right, r.bottom + sink * 0.4f)
        val rad = face.height() * 0.36f
        val top: Int
        val bottom: Int
        when {
            !enabled -> { top = Palette.locked; bottom = Palette.lockedDeep }
            style == BtnStyle.PRIMARY -> { top = Palette.mango; bottom = Palette.mangoDeep }
            style == BtnStyle.LEAF -> { top = Palette.leaf; bottom = Palette.leafDeep }
            style == BtnStyle.BERRY -> { top = Palette.berry; bottom = Palette.berryDeep }
            style == BtnStyle.SKY -> { top = Palette.sky; bottom = Palette.skyDeep }
            else -> { top = Color.argb(210, 34, 84, 48); bottom = Color.argb(226, 12, 46, 26) }
        }
        dropShadow(c, face, rad, face.height() * 0.16f + 3f, 84)
        p.style = Paint.Style.FILL
        p.color = shade(bottom, 0.7f)
        c.drawRoundRect(
            RectF(face.left, face.top + face.height() * 0.14f, face.right, face.bottom + face.height() * 0.1f),
            rad,
            rad,
            p,
        )
        p.shader = LinearGradient(face.left, face.top, face.left, face.bottom, top, bottom, Shader.TileMode.CLAMP)
        c.drawRoundRect(face, rad, rad, p)
        p.shader = null
        p.color = Color.argb(78, 255, 255, 255)
        c.drawRoundRect(
            RectF(face.left + rad * 0.4f, face.top + rad * 0.24f, face.right - rad * 0.4f, face.top + face.height() * 0.44f),
            rad * 0.6f,
            rad * 0.6f,
            p,
        )
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        p.color = if (enabled) Color.argb(220, 255, 250, 220) else Color.argb(150, 210, 220, 210)
        c.drawRoundRect(face, rad, rad, p)
        p.style = Paint.Style.FILL

        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.CENTER
        val iconSize = face.height() * 0.5f
        val gap = if (icon == null) 0f else iconSize * 1.25f
        val maxContent = face.width() - face.height() * 0.5f
        var size = textSize
        p.textSize = size
        var labelWidth = p.measureText(label)
        while (labelWidth + gap > maxContent && size > face.height() * 0.16f) {
            size *= 0.94f
            p.textSize = size
            labelWidth = p.measureText(label)
        }
        val contentLeft = face.centerX() - (labelWidth + gap) / 2f
        if (icon != null) {
            icon(c, icon, contentLeft + iconSize / 2f, face.centerY(), iconSize, Color.WHITE)
            p.typeface = Typeface.DEFAULT_BOLD
            p.textAlign = Paint.Align.CENTER
            p.textSize = size
        }
        val textCx = contentLeft + gap + labelWidth / 2f
        p.color = Color.argb(120, 0, 0, 0)
        c.drawText(label, textCx, face.centerY() + size * 0.38f, p)
        p.color = if (enabled) Color.WHITE else Color.argb(200, 226, 232, 226)
        c.drawText(label, textCx, face.centerY() + size * 0.35f, p)
        reset()
    }

    fun circleButton(
        c: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        iconName: String,
        style: BtnStyle = BtnStyle.LEAF,
        enabled: Boolean = true,
        pressed: Boolean = false,
        progress: Float = -1f,
    ) {
        val sink = if (pressed) radius * 0.06f else 0f
        val top: Int
        val bottom: Int
        when {
            !enabled -> { top = Palette.locked; bottom = Palette.lockedDeep }
            style == BtnStyle.PRIMARY -> { top = Palette.mango; bottom = Palette.mangoDeep }
            style == BtnStyle.BERRY -> { top = Palette.berry; bottom = Palette.berryDeep }
            style == BtnStyle.SKY -> { top = Palette.sky; bottom = Palette.skyDeep }
            else -> { top = Palette.leaf; bottom = Palette.leafDeep }
        }
        p.style = Paint.Style.FILL
        p.color = Color.argb(80, 0, 0, 0)
        c.drawCircle(cx + 2f, cy + sink + radius * 0.16f, radius, p)
        p.color = shade(bottom, 0.7f)
        c.drawCircle(cx, cy + sink + radius * 0.1f, radius, p)
        p.shader = LinearGradient(cx, cy - radius + sink, cx, cy + radius + sink, top, bottom, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy + sink, radius, p)
        p.shader = null
        p.color = Color.argb(70, 255, 255, 255)
        c.drawCircle(cx, cy - radius * 0.3f + sink, radius * 0.62f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = radius * 0.09f
        p.color = if (enabled) Color.argb(225, 255, 250, 220) else Color.argb(140, 200, 210, 200)
        c.drawCircle(cx, cy + sink, radius, p)
        if (progress in 0f..1f) {
            p.strokeWidth = radius * 0.14f
            p.color = Color.argb(70, 0, 0, 0)
            c.drawCircle(cx, cy + sink, radius * 1.16f, p)
            p.color = Palette.gold
            c.drawArc(
                RectF(cx - radius * 1.16f, cy + sink - radius * 1.16f, cx + radius * 1.16f, cy + sink + radius * 1.16f),
                -90f,
                360f * progress,
                false,
                p,
            )
        }
        p.style = Paint.Style.FILL
        icon(c, iconName, cx, cy + sink, radius * 1.05f, if (enabled) Color.WHITE else Color.argb(190, 220, 226, 220))
        reset()
    }

    fun bar(
        c: Canvas,
        r: RectF,
        value: Float,
        from: Int,
        to: Int,
        iconName: String,
        showValue: Boolean = true,
    ) {
        val rad = r.height() * 0.5f
        p.style = Paint.Style.FILL
        p.color = Color.argb(170, 0, 0, 0)
        c.drawRoundRect(r, rad, rad, p)
        val pct = (value / 100f).coerceIn(0f, 1f)
        val inner = RectF(r.left + 3f, r.top + 3f, r.right - 3f, r.bottom - 3f)
        if (pct > 0.01f) {
            val fillRight = inner.left + inner.width() * pct
            val fill = RectF(inner.left, inner.top, maxOf(fillRight, inner.left + inner.height()), inner.bottom)
            p.shader = LinearGradient(fill.left, fill.top, fill.left, fill.bottom, to, from, Shader.TileMode.CLAMP)
            c.drawRoundRect(fill, rad, rad, p)
            p.shader = null
            p.color = Color.argb(90, 255, 255, 255)
            c.drawRoundRect(
                RectF(fill.left + 3f, fill.top + 2f, fill.right - 3f, fill.top + fill.height() * 0.42f),
                rad * 0.6f,
                rad * 0.6f,
                p,
            )
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        p.color = Color.argb(120, 255, 255, 255)
        c.drawRoundRect(r, rad, rad, p)
        p.style = Paint.Style.FILL
        val badgeR = r.height() * 0.86f
        p.color = shade(from, 0.55f)
        c.drawCircle(r.left, r.centerY(), badgeR, p)
        p.color = from
        c.drawCircle(r.left, r.centerY(), badgeR * 0.84f, p)
        icon(c, iconName, r.left, r.centerY(), badgeR * 1.05f, Color.WHITE)
        if (showValue) {
            p.typeface = Typeface.DEFAULT_BOLD
            p.textAlign = Paint.Align.RIGHT
            p.textSize = r.height() * 0.72f
            p.color = Color.argb(150, 0, 0, 0)
            c.drawText("${value.toInt()}", r.right - 8f, r.centerY() + r.height() * 0.28f, p)
            p.color = Color.WHITE
            c.drawText("${value.toInt()}", r.right - 9f, r.centerY() + r.height() * 0.26f, p)
        }
        reset()
    }

    fun chip(c: Canvas, r: RectF, label: String, iconName: String?, tint: Int = Color.argb(200, 8, 40, 20)) {
        val rad = r.height() * 0.5f
        p.style = Paint.Style.FILL
        p.color = Color.argb(70, 0, 0, 0)
        c.drawRoundRect(RectF(r.left + 1f, r.top + 4f, r.right + 1f, r.bottom + 4f), rad, rad, p)
        p.color = tint
        c.drawRoundRect(r, rad, rad, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        p.color = Color.argb(120, 210, 255, 220)
        c.drawRoundRect(r, rad, rad, p)
        p.style = Paint.Style.FILL
        val iconSize = r.height() * 0.62f
        val gap = if (iconName == null) 0f else iconSize * 1.3f
        val maxContent = r.width() - r.height() * 0.6f
        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.LEFT
        var size = r.height() * 0.46f
        p.textSize = size
        var textW = p.measureText(label)
        while (textW + gap > maxContent && size > r.height() * 0.2f) {
            size *= 0.94f
            p.textSize = size
            textW = p.measureText(label)
        }
        val left = r.centerX() - (textW + gap) / 2f
        if (iconName != null) icon(c, iconName, left + iconSize / 2f, r.centerY(), iconSize, Palette.gold)
        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.LEFT
        p.textSize = size
        p.color = Color.WHITE
        c.drawText(label, left + gap, r.centerY() + size * 0.35f, p)
        reset()
    }

    fun heading(c: Canvas, text: String, cx: Float, cy: Float, size: Float) {
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = size
        p.style = Paint.Style.STROKE
        p.strokeWidth = size * 0.16f
        p.color = Color.argb(215, 8, 40, 20)
        c.drawText(text, cx, cy, p)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(cx, cy - size, cx, cy + size * 0.3f, Palette.cream, Palette.gold, Shader.TileMode.CLAMP)
        c.drawText(text, cx, cy, p)
        reset()
    }

    fun label(
        c: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int = Color.WHITE,
        align: Paint.Align = Paint.Align.LEFT,
        bold: Boolean = false,
        shadow: Boolean = true,
    ) {
        p.textAlign = align
        p.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        p.textSize = size
        p.style = Paint.Style.FILL
        if (shadow) {
            p.color = Color.argb(140, 0, 0, 0)
            c.drawText(text, x + 1.5f, y + 2f, p)
        }
        p.color = color
        c.drawText(text, x, y, p)
        reset()
    }

    fun stars(c: Canvas, cx: Float, cy: Float, size: Float, earned: Int, total: Int = 3) {
        val step = size * 1.15f
        val start = cx - step * (total - 1) / 2f
        for (i in 0 until total) {
            val filled = i < earned
            p.style = Paint.Style.FILL
            starPath(start + i * step, cy, size / 2f).let {
                p.color = Color.argb(160, 0, 0, 0)
                c.drawPath(starPath(start + i * step, cy + 2f, size / 2f), p)
                p.color = if (filled) Palette.gold else Color.argb(120, 240, 240, 240)
                c.drawPath(it, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = size * 0.08f
                p.color = if (filled) Color.rgb(255, 246, 200) else Color.argb(150, 255, 255, 255)
                c.drawPath(it, p)
            }
        }
        reset()
    }

    private fun starPath(cx: Float, cy: Float, r: Float): Path {
        val path = Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.46f
            val a = (-90f + i * 36f) * Math.PI.toFloat() / 180f
            val x = cx + cos(a) * rad
            val y = cy + sin(a) * rad
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    fun vignette(c: Canvas, w: Float, h: Float, strength: Int = 150) {
        p.shader = RadialGradient(
            w / 2f, h * 0.48f, maxOf(w, h) * 0.72f,
            Color.TRANSPARENT, Color.argb(strength, 0, 12, 4), Shader.TileMode.CLAMP,
        )
        c.drawRect(0f, 0f, w, h, p)
        reset()
    }

    fun scrim(c: Canvas, w: Float, h: Float, alpha: Int) {
        p.color = Color.argb(alpha, 4, 22, 10)
        c.drawRect(0f, 0f, w, h, p)
        reset()
    }

    /** Draws a flat vector icon centred on (cx, cy) inside a box of [s]. */
    fun icon(c: Canvas, name: String, cx: Float, cy: Float, s: Float, color: Int) {
        val h = s / 2f
        p.color = color
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        val path = Path()
        when (name) {
            "play" -> {
                path.moveTo(cx - h * 0.34f, cy - h * 0.6f)
                path.lineTo(cx + h * 0.66f, cy)
                path.lineTo(cx - h * 0.34f, cy + h * 0.6f)
                path.close()
                c.drawPath(path, p)
            }
            "pause" -> {
                val bw = h * 0.26f
                c.drawRoundRect(RectF(cx - h * 0.44f, cy - h * 0.56f, cx - h * 0.44f + bw, cy + h * 0.56f), bw * 0.4f, bw * 0.4f, p)
                c.drawRoundRect(RectF(cx + h * 0.18f, cy - h * 0.56f, cx + h * 0.18f + bw, cy + h * 0.56f), bw * 0.4f, bw * 0.4f, p)
            }
            "gear" -> {
                path.fillType = Path.FillType.EVEN_ODD
                val teeth = 8
                for (i in 0 until teeth) {
                    val a = i * (360f / teeth) * Math.PI.toFloat() / 180f
                    val tx = cx + cos(a) * h * 0.66f
                    val ty = cy + sin(a) * h * 0.66f
                    val tp = Path()
                    tp.addRoundRect(RectF(tx - h * 0.16f, ty - h * 0.16f, tx + h * 0.16f, ty + h * 0.16f), h * 0.05f, h * 0.05f, Path.Direction.CW)
                    path.addPath(tp)
                }
                path.addCircle(cx, cy, h * 0.6f, Path.Direction.CW)
                path.addCircle(cx, cy, h * 0.26f, Path.Direction.CCW)
                c.drawPath(path, p)
            }
            "back" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.24f
                path.moveTo(cx + h * 0.3f, cy - h * 0.55f)
                path.lineTo(cx - h * 0.3f, cy)
                path.lineTo(cx + h * 0.3f, cy + h * 0.55f)
                c.drawPath(path, p)
            }
            "chest" -> {
                c.drawRoundRect(RectF(cx - h * 0.68f, cy - h * 0.1f, cx + h * 0.68f, cy + h * 0.6f), h * 0.12f, h * 0.12f, p)
                path.moveTo(cx - h * 0.68f, cy - h * 0.1f)
                path.quadTo(cx, cy - h * 0.95f, cx + h * 0.68f, cy - h * 0.1f)
                path.close()
                c.drawPath(path, p)
                p.color = Color.argb(140, 0, 0, 0)
                c.drawRect(cx - h * 0.14f, cy - h * 0.5f, cx + h * 0.14f, cy + h * 0.6f, p)
            }
            "book" -> {
                c.drawRoundRect(RectF(cx - h * 0.6f, cy - h * 0.66f, cx + h * 0.6f, cy + h * 0.66f), h * 0.12f, h * 0.12f, p)
                p.color = Color.argb(150, 0, 0, 0)
                c.drawRect(cx - h * 0.08f, cy - h * 0.66f, cx + h * 0.08f, cy + h * 0.66f, p)
                for (i in 0..1) {
                    c.drawRect(cx - h * 0.46f, cy - h * 0.3f + i * h * 0.34f, cx - h * 0.18f, cy - h * 0.22f + i * h * 0.34f, p)
                    c.drawRect(cx + h * 0.18f, cy - h * 0.3f + i * h * 0.34f, cx + h * 0.46f, cy - h * 0.22f + i * h * 0.34f, p)
                }
            }
            "flag" -> {
                c.drawRoundRect(RectF(cx - h * 0.52f, cy - h * 0.72f, cx - h * 0.36f, cy + h * 0.72f), h * 0.08f, h * 0.08f, p)
                path.moveTo(cx - h * 0.36f, cy - h * 0.66f)
                path.lineTo(cx + h * 0.62f, cy - h * 0.34f)
                path.lineTo(cx - h * 0.36f, cy + h * 0.02f)
                path.close()
                c.drawPath(path, p)
            }
            "shield" -> {
                path.moveTo(cx, cy - h * 0.74f)
                path.lineTo(cx + h * 0.62f, cy - h * 0.42f)
                path.lineTo(cx + h * 0.56f, cy + h * 0.14f)
                path.quadTo(cx + h * 0.42f, cy + h * 0.62f, cx, cy + h * 0.78f)
                path.quadTo(cx - h * 0.42f, cy + h * 0.62f, cx - h * 0.56f, cy + h * 0.14f)
                path.lineTo(cx - h * 0.62f, cy - h * 0.42f)
                path.close()
                c.drawPath(path, p)
            }
            "star" -> c.drawPath(starPath(cx, cy, h * 0.78f), p)
            "heart" -> {
                path.moveTo(cx, cy + h * 0.66f)
                path.cubicTo(cx - h * 1.12f, cy - h * 0.12f, cx - h * 0.5f, cy - h * 0.86f, cx, cy - h * 0.3f)
                path.cubicTo(cx + h * 0.5f, cy - h * 0.86f, cx + h * 1.12f, cy - h * 0.12f, cx, cy + h * 0.66f)
                path.close()
                c.drawPath(path, p)
            }
            "bolt" -> {
                path.moveTo(cx + h * 0.28f, cy - h * 0.78f)
                path.lineTo(cx - h * 0.5f, cy + h * 0.12f)
                path.lineTo(cx - h * 0.04f, cy + h * 0.12f)
                path.lineTo(cx - h * 0.24f, cy + h * 0.8f)
                path.lineTo(cx + h * 0.52f, cy - h * 0.16f)
                path.lineTo(cx + h * 0.06f, cy - h * 0.16f)
                path.close()
                c.drawPath(path, p)
            }
            "leaf" -> {
                path.moveTo(cx - h * 0.6f, cy + h * 0.6f)
                path.quadTo(cx - h * 0.7f, cy - h * 0.5f, cx + h * 0.62f, cy - h * 0.66f)
                path.quadTo(cx + h * 0.66f, cy + h * 0.5f, cx - h * 0.6f, cy + h * 0.6f)
                path.close()
                c.drawPath(path, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.1f
                p.color = Color.argb(120, 0, 0, 0)
                c.drawLine(cx - h * 0.5f, cy + h * 0.5f, cx + h * 0.42f, cy - h * 0.44f, p)
            }
            "coin" -> {
                c.drawCircle(cx, cy, h * 0.74f, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.12f
                p.color = Color.argb(150, 0, 0, 0)
                c.drawCircle(cx, cy, h * 0.5f, p)
            }
            "drop" -> {
                path.moveTo(cx, cy - h * 0.8f)
                path.quadTo(cx + h * 0.78f, cy + h * 0.12f, cx, cy + h * 0.78f)
                path.quadTo(cx - h * 0.78f, cy + h * 0.12f, cx, cy - h * 0.8f)
                path.close()
                c.drawPath(path, p)
            }
            "lock" -> {
                c.drawRoundRect(RectF(cx - h * 0.52f, cy - h * 0.1f, cx + h * 0.52f, cy + h * 0.66f), h * 0.14f, h * 0.14f, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.18f
                c.drawArc(RectF(cx - h * 0.34f, cy - h * 0.72f, cx + h * 0.34f, cy - h * 0.04f), 180f, 180f, false, p)
            }
            "retry" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.2f
                c.drawArc(RectF(cx - h * 0.6f, cy - h * 0.6f, cx + h * 0.6f, cy + h * 0.6f), 40f, 280f, false, p)
                p.style = Paint.Style.FILL
                path.moveTo(cx + h * 0.66f, cy - h * 0.66f)
                path.lineTo(cx + h * 0.74f, cy + h * 0.06f)
                path.lineTo(cx + h * 0.06f, cy - h * 0.2f)
                path.close()
                c.drawPath(path, p)
            }
            "home" -> {
                path.moveTo(cx, cy - h * 0.74f)
                path.lineTo(cx + h * 0.76f, cy - h * 0.02f)
                path.lineTo(cx - h * 0.76f, cy - h * 0.02f)
                path.close()
                c.drawPath(path, p)
                c.drawRoundRect(RectF(cx - h * 0.5f, cy - h * 0.06f, cx + h * 0.5f, cy + h * 0.68f), h * 0.1f, h * 0.1f, p)
            }
            "check" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.24f
                path.moveTo(cx - h * 0.56f, cy)
                path.lineTo(cx - h * 0.14f, cy + h * 0.44f)
                path.lineTo(cx + h * 0.6f, cy - h * 0.44f)
                c.drawPath(path, p)
            }
            "splash" -> {
                c.drawCircle(cx, cy, h * 0.4f, p)
                for (i in 0 until 8) {
                    val a = i * 45f * Math.PI.toFloat() / 180f
                    val rr = if (i % 2 == 0) h * 0.78f else h * 0.62f
                    c.drawCircle(cx + cos(a) * rr, cy + sin(a) * rr, h * 0.15f, p)
                }
            }
            "wave" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.16f
                for (i in 1..3) c.drawCircle(cx, cy, h * 0.24f * i, p)
            }
            "target" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.16f
                c.drawCircle(cx, cy, h * 0.66f, p)
                c.drawCircle(cx, cy, h * 0.3f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, h * 0.1f, p)
            }
            "timer" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.16f
                c.drawCircle(cx, cy + h * 0.1f, h * 0.62f, p)
                c.drawLine(cx, cy + h * 0.1f, cx, cy - h * 0.3f, p)
                c.drawLine(cx, cy + h * 0.1f, cx + h * 0.3f, cy + h * 0.1f, p)
                p.style = Paint.Style.FILL
                c.drawRect(cx - h * 0.24f, cy - h * 0.82f, cx + h * 0.24f, cy - h * 0.62f, p)
            }
            "bug" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.13f
                c.drawLine(cx - h * 0.5f, cy - h * 0.12f, cx - h * 0.82f, cy - h * 0.3f, p)
                c.drawLine(cx + h * 0.5f, cy - h * 0.12f, cx + h * 0.82f, cy - h * 0.3f, p)
                c.drawLine(cx - h * 0.5f, cy + h * 0.24f, cx - h * 0.82f, cy + h * 0.4f, p)
                c.drawLine(cx + h * 0.5f, cy + h * 0.24f, cx + h * 0.82f, cy + h * 0.4f, p)
                c.drawLine(cx - h * 0.18f, cy - h * 0.6f, cx - h * 0.42f, cy - h * 0.88f, p)
                c.drawLine(cx + h * 0.18f, cy - h * 0.6f, cx + h * 0.42f, cy - h * 0.88f, p)
                p.style = Paint.Style.FILL
                c.drawOval(RectF(cx - h * 0.52f, cy - h * 0.34f, cx + h * 0.52f, cy + h * 0.78f), p)
                c.drawOval(RectF(cx - h * 0.3f, cy - h * 0.72f, cx + h * 0.3f, cy - h * 0.22f), p)
                p.color = Color.argb(150, 0, 0, 0)
                c.drawRect(cx - h * 0.05f, cy - h * 0.26f, cx + h * 0.05f, cy + h * 0.7f, p)
            }
            "sprout" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.16f
                c.drawLine(cx, cy + h * 0.72f, cx, cy - h * 0.2f, p)
                p.style = Paint.Style.FILL
                path.moveTo(cx, cy - h * 0.06f)
                path.quadTo(cx - h * 0.8f, cy - h * 0.2f, cx - h * 0.62f, cy - h * 0.74f)
                path.quadTo(cx - h * 0.1f, cy - h * 0.7f, cx, cy - h * 0.06f)
                path.close()
                c.drawPath(path, p)
                val right = Path()
                right.moveTo(cx, cy + h * 0.16f)
                right.quadTo(cx + h * 0.82f, cy + h * 0.02f, cx + h * 0.64f, cy - h * 0.52f)
                right.quadTo(cx + h * 0.12f, cy - h * 0.48f, cx, cy + h * 0.16f)
                right.close()
                c.drawPath(right, p)
            }
            "sound" -> {
                path.moveTo(cx - h * 0.62f, cy - h * 0.22f)
                path.lineTo(cx - h * 0.3f, cy - h * 0.22f)
                path.lineTo(cx + h * 0.04f, cy - h * 0.62f)
                path.lineTo(cx + h * 0.04f, cy + h * 0.62f)
                path.lineTo(cx - h * 0.3f, cy + h * 0.22f)
                path.lineTo(cx - h * 0.62f, cy + h * 0.22f)
                path.close()
                c.drawPath(path, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.14f
                c.drawArc(RectF(cx - h * 0.1f, cy - h * 0.44f, cx + h * 0.5f, cy + h * 0.44f), -60f, 120f, false, p)
                c.drawArc(RectF(cx - h * 0.06f, cy - h * 0.72f, cx + h * 0.82f, cy + h * 0.72f), -55f, 110f, false, p)
            }
            "vibrate" -> {
                c.drawRoundRect(RectF(cx - h * 0.26f, cy - h * 0.68f, cx + h * 0.26f, cy + h * 0.68f), h * 0.14f, h * 0.14f, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.14f
                c.drawLine(cx - h * 0.62f, cy - h * 0.3f, cx - h * 0.62f, cy + h * 0.3f, p)
                c.drawLine(cx + h * 0.62f, cy - h * 0.3f, cx + h * 0.62f, cy + h * 0.3f, p)
            }
            "doc" -> {
                c.drawRoundRect(RectF(cx - h * 0.5f, cy - h * 0.72f, cx + h * 0.5f, cy + h * 0.72f), h * 0.12f, h * 0.12f, p)
                p.color = Color.argb(150, 0, 0, 0)
                for (i in 0..2) {
                    c.drawRect(cx - h * 0.3f, cy - h * 0.36f + i * h * 0.32f, cx + h * 0.3f, cy - h * 0.26f + i * h * 0.32f, p)
                }
            }
            "help" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = h * 0.16f
                c.drawCircle(cx, cy, h * 0.72f, p)
                p.style = Paint.Style.FILL
                p.textAlign = Paint.Align.CENTER
                p.typeface = Typeface.DEFAULT_BOLD
                p.textSize = h * 1.1f
                c.drawText("?", cx, cy + h * 0.4f, p)
            }
            "map" -> {
                path.moveTo(cx - h * 0.74f, cy - h * 0.5f)
                path.lineTo(cx - h * 0.25f, cy - h * 0.72f)
                path.lineTo(cx + h * 0.25f, cy - h * 0.5f)
                path.lineTo(cx + h * 0.74f, cy - h * 0.72f)
                path.lineTo(cx + h * 0.74f, cy + h * 0.52f)
                path.lineTo(cx + h * 0.25f, cy + h * 0.74f)
                path.lineTo(cx - h * 0.25f, cy + h * 0.52f)
                path.lineTo(cx - h * 0.74f, cy + h * 0.74f)
                path.close()
                c.drawPath(path, p)
            }
            "up" -> {
                path.moveTo(cx, cy - h * 0.72f)
                path.lineTo(cx + h * 0.72f, cy + h * 0.06f)
                path.lineTo(cx + h * 0.3f, cy + h * 0.06f)
                path.lineTo(cx + h * 0.3f, cy + h * 0.7f)
                path.lineTo(cx - h * 0.3f, cy + h * 0.7f)
                path.lineTo(cx - h * 0.3f, cy + h * 0.06f)
                path.lineTo(cx - h * 0.72f, cy + h * 0.06f)
                path.close()
                c.drawPath(path, p)
            }
        }
        reset()
    }
}
