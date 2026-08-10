package com.fruitsplash.fruitsplashgame

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import java.util.concurrent.Executors

class SplashActivity : Activity() {
    private lateinit var view: SplashView
    private val backgrounds = mutableMapOf<Int, Bitmap>()
    private val handler = Handler(Looper.getMainLooper())
    @Volatile
    private var opened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goFullscreen()
        view = SplashView()
        setContentView(view)
        warmup()
        // Hard fallback: never stay on splash longer than 4s even if a step stalls.
        handler.postDelayed({ openMenu() }, 4000)
    }

    private fun goFullscreen() {
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    private fun warmup() {
        Executors.newSingleThreadExecutor().execute {
            val steps: List<() -> Unit> = listOf(
                { runCatching { SaveStore(this).coins } },
                { runCatching { BitmapFactory.decodeResource(resources, R.drawable.vertical_loading_screen)?.recycle() } },
                { runCatching { BitmapFactory.decodeResource(resources, R.drawable.main_garden_level_background)?.recycle() } },
                {
                    runCatching {
                        assets.open("sprites/player/fruit_gardener.png").use {
                            BitmapFactory.decodeStream(it)?.recycle()
                        }
                    }
                },
                { runCatching { assets.open("sprites/sprites.json").use { it.readBytes() } } },
                { runCatching { SoundManager(this).release() } },
            )
            steps.forEachIndexed { index, step ->
                step()
                val progress = ((index + 1).toFloat() / steps.size * 92f).toInt()
                runOnUiThread {
                    view.progress = progress
                    view.invalidate()
                }
            }
            runOnUiThread {
                view.progress = 100
                view.invalidate()
                handler.postDelayed({ openMenu() }, 220)
            }
        }
    }

    private fun openMenu() {
        if (opened) return
        opened = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        backgrounds.values.forEach { it.recycle() }
        backgrounds.clear()
        super.onDestroy()
    }

    inner class SplashView : View(this@SplashActivity) {
        var progress = 0
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var started = System.currentTimeMillis()

        override fun onDraw(c: Canvas) {
            val landscape = width > height
            val backgroundId =
                if (landscape) R.drawable.horizontal_loading_screen else R.drawable.vertical_loading_screen
            val bmp = backgrounds.getOrPut(backgroundId) {
                BitmapFactory.decodeResource(resources, backgroundId)
            }
            val scale = maxOf(width / bmp.width.toFloat(), height / bmp.height.toFloat())
            val dw = bmp.width * scale
            val dh = bmp.height * scale
            c.drawBitmap(
                bmp,
                null,
                RectF((width - dw) / 2, (height - dh) / 2, (width + dw) / 2, (height + dh) / 2),
                paint,
            )
            paint.color = Color.argb(190, 9, 28, 18)
            c.drawRoundRect(width * 0.1f, height * 0.82f, width * 0.9f, height * 0.875f, 22f, 22f, paint)
            paint.color = Color.rgb(255, 193, 7)
            c.drawRoundRect(
                width * 0.11f,
                height * 0.833f,
                width * 0.11f + width * 0.78f * (progress / 100f),
                height * 0.862f,
                18f,
                18f,
                paint,
            )
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = minOf(width, height) * 0.052f
            paint.typeface = Typeface.DEFAULT_BOLD
            val dots = ((System.currentTimeMillis() - started) / 450 % 3 + 1).toInt()
            c.drawText("Loading" + ".".repeat(dots), width / 2f, height * 0.79f, paint)
            if (progress < 100) postInvalidateDelayed(150)
        }
    }
}
