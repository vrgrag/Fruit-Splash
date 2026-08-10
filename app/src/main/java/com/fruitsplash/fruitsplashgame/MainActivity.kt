package com.fruitsplash.fruitsplashgame

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var gameView: FruitGameView

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        goFullscreen()
        gameView = FruitGameView(this)
        setContentView(gameView)
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

    override fun onPause() {
        super.onPause()
        gameView.onHostPause()
    }

    override fun onResume() {
        super.onResume()
        goFullscreen()
        gameView.resumeLoop()
    }

    override fun onDestroy() {
        gameView.release()
        super.onDestroy()
    }
}

private enum class Screen {
    MENU, ZONES, GAME, RESTORE, UPGRADES, COLLECTION, MISSIONS, SETTINGS, RESULT
}

private enum class Kind {
    FRUIT, RARE, CRYSTAL, PEST, OBSTACLE, MAGIC_PLANT, PORTAL, CHEST, HAZARD
}

private enum class PestType { BEETLE, CATERPILLAR, SNAIL, WASP }

private class Thing(
    var x: Float,
    var y: Float,
    val kind: Kind,
    var hp: Int = 1,
    var maxHp: Int = 1,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val sprite: String = "",
    val fruitId: String = "",
    val pest: PestType = PestType.BEETLE,
    var timer: Float = 0f,
    var life: Float = 0f,
    var flash: Float = 0f,
    var phase: Float = 0f,
)

private class HitRect(val id: String, val rect: RectF)

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Int,
    val size: Float,
)

private class FloatText(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Int,
    var life: Float,
    val size: Float,
)

class FruitGameView(private val host: MainActivity) : View(host) {
    private val save = SaveStore(host)
    private val sounds = SoundManager(host)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ui = UiKit(paint)
    private val bitmaps = mutableMapOf<String, Bitmap>()
    private val resourceBitmaps = mutableMapOf<Int, Bitmap>()
    private val things = mutableListOf<Thing>()
    private val particles = mutableListOf<Particle>()
    private val floatTexts = mutableListOf<FloatText>()
    private val hitTargets = mutableListOf<HitRect>()
    private val rnd = Random.Default

    private var screen = Screen.MENU
    private var selectedZone = 0
    private var anim = 0f
    private var pressedId: String? = null

    private var px = 0f
    private var py = 0f
    private var faceDir = 1f
    private var moving = false
    private var dragX = 0f
    private var dragY = 0f
    private var dragOriginX = 0f
    private var dragOriginY = 0f
    private var dragging = false

    private var health = 100f
    private var energy = 0f
    private var restoration = 0f
    private var shield = 0f
    private var shieldCooldown = 0f
    private var splashWave = 0f
    private var collected = 0
    private var rare = 0
    private var defeated = 0
    private var score = 0
    private var combo = 0
    private var comboTimer = 0f
    private var wave = 1
    private var waveTimer = 0f
    private var gameTime = 0f
    private var pickupTimer = 0f
    private var hurtCooldown = 0f
    private var hurtFlash = 0f
    private var shake = 0f
    private var slowTime = 0f
    private var magicPlantUsed = false
    private var portalSpawned = false
    private var chestSpawned = false
    private var portalActive = false
    private var portalUsed = false
    private var portalTimer = 0f

    private var resultWin = false
    private var resultStars = 0
    private var lastReward = 0
    private var resultAnim = 0f
    private var toast = ""
    private var toastTimer = 0f
    private var banner = ""
    private var bannerTimer = 0f
    private var lastFrame = System.nanoTime()
    private var loopRunning = false
    private var gamePaused = false

    private val fruits = listOf(
        "apple" to "fruits/apple.png",
        "orange" to "fruits/orange.png",
        "strawberry" to "fruits/strawberry.png",
        "lemon" to "fruits/lemon.png",
        "grapes" to "fruits/grapes.png",
        "watermelon" to "fruits/watermelon.png",
        "cherries" to "fruits/cherries.png",
        "mango" to "fruits/mango.png",
    )
    private val rares = listOf(
        "starfruit" to "fruits/starfruit.png",
        "dragonfruit" to "fruits/dragonfruit.png",
        "moonberry" to "fruits/moonberry.png",
        "golden_apple" to "fruits/golden_apple.png",
    )
    private val seeds = listOf(
        "sun_seed" to "fruits/sun_seed.png",
        "rain_seed" to "fruits/rain_seed.png",
        "moon_seed" to "fruits/moon_seed.png",
        "rainbow_seed" to "fruits/rainbow_seed.png",
    )
    private val magicPlants = listOf(
        "world/flower.png",
        "world/sprout.png",
        "world/bush.png",
        "world/herbs.png",
    )
    private val backgrounds = intArrayOf(
        R.drawable.fruit_garden_background,
        R.drawable.tropical_garden_background,
        R.drawable.sunny_valley_background,
        R.drawable.fruit_greenhouse_background,
        R.drawable.berry_valley_background,
    )
    private val zoneNames = arrayOf(
        "Fruit Garden", "Tropical Garden", "Sunny Valley", "Fruit Greenhouse", "Berry Valley"
    )
    private val zoneObjectives = arrayOf(
        "Collect 16 fruit",
        "Defeat 12 pests",
        "Find 3 rare fruit",
        "Restore the greenhouse",
        "Survive 75 seconds",
    )

    private val vw: Float get() = width.toFloat()
    private val vh: Float get() = height.toFloat()
    private val hudBottom: Float get() = vh * 0.145f
    private val fieldTop: Float get() = hudBottom + vw * 0.14f
    private val fieldBottom: Float get() = vh - vw * 0.37f

    init {
        isFocusable = true
        isClickable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resumeLoop()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(loop)
        loopRunning = false
        super.onDetachedFromWindow()
    }

    fun onHostPause() {
        if (screen == Screen.GAME) gamePaused = true
    }

    fun resumeLoop() {
        if (loopRunning) return
        loopRunning = true
        lastFrame = System.nanoTime()
        removeCallbacks(loop)
        post(loop)
    }

    private val loop = object : Runnable {
        override fun run() {
            val now = System.nanoTime()
            val dt = ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.04f)
            lastFrame = now
            anim += dt
            if (toastTimer > 0f) toastTimer -= dt
            if (screen == Screen.RESULT) resultAnim += dt
            if (screen == Screen.GAME && !gamePaused) update(dt)
            invalidate()
            postDelayed(this, 16)
        }
    }

    // region drawing entry points

    override fun onDraw(c: Canvas) {
        hitTargets.clear()
        when (screen) {
            Screen.MENU -> drawMenu(c)
            Screen.ZONES -> drawZones(c)
            Screen.GAME -> drawGame(c)
            Screen.RESTORE -> drawRestore(c)
            Screen.UPGRADES -> drawUpgrades(c)
            Screen.COLLECTION -> drawCollection(c)
            Screen.MISSIONS -> drawMissions(c)
            Screen.SETTINGS -> drawSettings(c)
            Screen.RESULT -> drawResult(c)
        }
        if (toastTimer > 0f && toast.isNotEmpty()) {
            val r = RectF(vw * 0.12f, vh * 0.44f, vw * 0.88f, vh * 0.51f)
            ui.chip(c, r, toast, "check", Color.argb(232, 10, 48, 24))
        }
    }

    private fun drawBackdrop(c: Canvas, resource: Int = R.drawable.main_garden_level_background, dim: Int = 90) {
        val bmp = resourceBitmaps.getOrPut(resource) { BitmapFactory.decodeResource(resources, resource) }
        val scale = max(vw / bmp.width, vh / bmp.height)
        val w = bmp.width * scale
        val h = bmp.height * scale
        paint.alpha = 255
        c.drawBitmap(bmp, null, RectF((vw - w) / 2, (vh - h) / 2, (vw + w) / 2, (vh + h) / 2), paint)
        ui.scrim(c, vw, vh, dim)
        ui.vignette(c, vw, vh)
    }

    private fun btn(
        c: Canvas,
        id: String,
        label: String,
        r: RectF,
        style: BtnStyle = BtnStyle.PRIMARY,
        enabled: Boolean = true,
        icon: String? = null,
    ) {
        ui.button(c, r, label, style, enabled, pressedId == id, icon)
        if (enabled) hitTargets += HitRect(id, r)
    }

    private fun circleBtn(
        c: Canvas,
        id: String,
        iconName: String,
        cx: Float,
        cy: Float,
        radius: Float,
        style: BtnStyle = BtnStyle.LEAF,
        enabled: Boolean = true,
        progress: Float = -1f,
    ) {
        ui.circleButton(c, cx, cy, radius, iconName, style, enabled, pressedId == id, progress)
        if (enabled) {
            val pad = radius * 1.2f
            hitTargets += HitRect(id, RectF(cx - pad, cy - pad, cx + pad, cy + pad))
        }
    }

    // endregion

    // region menu

    private fun drawMenu(c: Canvas) {
        drawBackdrop(c, dim = 80)

        val glow = 0.5f + 0.5f * sin(anim * 1.6f)
        paint.shader = RadialGradient(
            vw * 0.5f, vh * 0.44f, vw * (0.42f + glow * 0.05f),
            Color.argb(90, 255, 220, 120), Color.TRANSPARENT, Shader.TileMode.CLAMP,
        )
        c.drawCircle(vw * 0.5f, vh * 0.44f, vw * 0.5f, paint)
        ui.reset()
        drawAsset(c, "world/magic_fruit_fountain.png", vw * 0.5f, vh * 0.46f + sin(anim * 1.2f) * vh * 0.006f, vw * 0.52f)

        val logo = resourceBitmaps.getOrPut(R.drawable.game_name) {
            BitmapFactory.decodeResource(resources, R.drawable.game_name)
        }
        paint.alpha = 255
        // Fit the logo inside the reserved slot keeping its natural aspect ratio
        val logoSlotW = vw * 0.78f
        val logoSlotH = vh * 0.20f
        val logoRatio = logo.width.toFloat() / logo.height.toFloat()
        val logoDrawW: Float
        val logoDrawH: Float
        if (logoSlotW / logoSlotH > logoRatio) {
            logoDrawH = logoSlotH
            logoDrawW = logoDrawH * logoRatio
        } else {
            logoDrawW = logoSlotW
            logoDrawH = logoDrawW / logoRatio
        }
        val logoTop = vh * 0.078f
        val logoCx = vw * 0.5f
        val logoCy = logoTop + logoDrawH / 2f
        c.drawBitmap(
            logo,
            null,
            RectF(logoCx - logoDrawW / 2f, logoCy - logoDrawH / 2f, logoCx + logoDrawW / 2f, logoCy + logoDrawH / 2f),
            paint,
        )

        ui.chip(c, RectF(vw * 0.04f, vh * 0.02f, vw * 0.40f, vh * 0.062f), "${save.coins}", "coin")
        ui.chip(c, RectF(vw * 0.43f, vh * 0.02f, vw * 0.72f, vh * 0.062f), "${save.totalStars()}/15", "star")
        circleBtn(c, "settings", "gear", vw * 0.89f, vh * 0.041f, vw * 0.052f, BtnStyle.SKY)

        val pulse = 1f + 0.02f * sin(anim * 3.4f)
        val playW = vw * 0.68f * pulse
        val playR = RectF(vw * 0.5f - playW / 2, vh * 0.60f, vw * 0.5f + playW / 2, vh * 0.60f + vh * 0.082f)
        paint.shader = RadialGradient(
            playR.centerX(), playR.centerY(), playW * 0.75f,
            Color.argb((70 + 50 * glow).toInt(), 255, 190, 80), Color.TRANSPARENT, Shader.TileMode.CLAMP,
        )
        c.drawCircle(playR.centerX(), playR.centerY(), playW * 0.75f, paint)
        ui.reset()
        btn(c, "play", "PLAY", playR, BtnStyle.PRIMARY, icon = "play")
        btn(
            c,
            "zones",
            "ZONE MAP",
            RectF(vw * 0.24f, vh * 0.710f, vw * 0.76f, vh * 0.710f + vh * 0.062f),
            BtnStyle.LEAF,
            icon = "map",
        )

        val r = vw * 0.082f
        val cy = vh * 0.852f
        menuIcon(c, "restore", "sprout", "GARDEN", vw * 0.155f, cy, r, BtnStyle.LEAF)
        menuIcon(c, "upgrades", "up", "UPGRADE", vw * 0.385f, cy, r, BtnStyle.SKY)
        menuIcon(c, "collection", "book", "FRUITS", vw * 0.615f, cy, r, BtnStyle.BERRY)
        menuIcon(c, "missions", "flag", "TASKS", vw * 0.845f, cy, r, BtnStyle.PRIMARY)

        ui.label(c, "Zones cleared ${save.zonesCleared}/5", vw * 0.5f, vh * 0.968f, vw * 0.032f, Palette.muted, Paint.Align.CENTER)
    }

    private fun menuIcon(
        c: Canvas,
        id: String,
        iconName: String,
        label: String,
        cx: Float,
        cy: Float,
        radius: Float,
        style: BtnStyle,
    ) {
        circleBtn(c, id, iconName, cx, cy, radius, style)
        ui.label(c, label, cx, cy + radius * 1.75f, vw * 0.028f, Palette.cream, Paint.Align.CENTER, bold = true)
    }

    // endregion

    // region zone map

    private fun drawZones(c: Canvas) {
        drawBackdrop(c, dim = 130)
        circleBtn(c, "back", "back", vw * 0.11f, vh * 0.06f, vw * 0.052f, BtnStyle.LEAF)
        ui.heading(c, "ZONE MAP", vw * 0.54f, vh * 0.075f, vw * 0.062f)
        ui.chip(c, RectF(vw * 0.32f, vh * 0.105f, vw * 0.68f, vh * 0.145f), "${save.coins} coins", "coin")

        val cardH = vh * 0.125f
        for (i in 0..4) {
            val top = vh * 0.17f + i * (cardH + vh * 0.018f)
            val r = RectF(vw * 0.05f, top, vw * 0.95f, top + cardH)
            val unlocked = GameLogic.unlockedZone(save.zonesCleared, i)
            ui.panel(
                c,
                r,
                cardH * 0.24f,
                if (unlocked) Palette.cardTop else Color.argb(228, 46, 52, 46),
                if (unlocked) Palette.cardBottom else Color.argb(238, 20, 24, 20),
                if (unlocked) Palette.panelEdge else Color.argb(110, 170, 178, 170),
            )

            val thumb = RectF(r.left + cardH * 0.12f, r.top + cardH * 0.12f, r.left + cardH * 0.96f, r.bottom - cardH * 0.12f)
            val bmp = resourceBitmaps.getOrPut(backgrounds[i]) { BitmapFactory.decodeResource(resources, backgrounds[i]) }
            c.save()
            val clip = android.graphics.Path()
            clip.addRoundRect(thumb, cardH * 0.16f, cardH * 0.16f, android.graphics.Path.Direction.CW)
            c.clipPath(clip)
            val sc = max(thumb.width() / bmp.width, thumb.height() / bmp.height)
            val dw = bmp.width * sc
            val dh = bmp.height * sc
            paint.alpha = if (unlocked) 255 else 90
            c.drawBitmap(
                bmp,
                null,
                RectF(thumb.centerX() - dw / 2, thumb.centerY() - dh / 2, thumb.centerX() + dw / 2, thumb.centerY() + dh / 2),
                paint,
            )
            paint.alpha = 255
            c.restore()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.argb(180, 255, 246, 210)
            c.drawRoundRect(thumb, cardH * 0.16f, cardH * 0.16f, paint)
            ui.reset()

            val textX = thumb.right + cardH * 0.16f
            ui.label(
                c,
                zoneNames[i],
                textX,
                r.top + cardH * 0.36f,
                vw * 0.043f,
                if (unlocked) Palette.cream else Color.argb(190, 210, 214, 210),
                bold = true,
            )
            ui.label(
                c,
                if (unlocked) zoneObjectives[i] else "Clear ${zoneNames[(i - 1).coerceAtLeast(0)]} first",
                textX,
                r.top + cardH * 0.6f,
                vw * 0.03f,
                Palette.muted,
            )
            if (unlocked) {
                ui.stars(c, textX + vw * 0.075f, r.bottom - cardH * 0.22f, vw * 0.042f, save.zoneStars(i))
                hitTargets += HitRect("zone_$i", r)
            } else {
                ui.icon(c, "lock", r.right - cardH * 0.36f, r.centerY(), cardH * 0.42f, Color.argb(210, 226, 232, 226))
            }
            if (i < save.zonesCleared) {
                ui.icon(c, "check", r.right - cardH * 0.32f, r.top + cardH * 0.3f, cardH * 0.3f, Palette.restore)
            }
        }
    }

    // endregion

    // region gameplay simulation

    private fun startGame(zone: Int) {
        selectedZone = zone
        screen = Screen.GAME
        gamePaused = false
        health = 100f
        energy = 0f
        restoration = 0f
        shield = 0f
        shieldCooldown = 0f
        splashWave = 0f
        collected = 0
        rare = 0
        defeated = 0
        score = 0
        combo = 0
        comboTimer = 0f
        wave = 1
        waveTimer = 0f
        gameTime = 0f
        pickupTimer = 0f
        hurtCooldown = 0f
        hurtFlash = 0f
        shake = 0f
        slowTime = 0f
        magicPlantUsed = false
        portalSpawned = false
        chestSpawned = false
        portalActive = false
        portalUsed = false
        lastReward = 0
        resultAnim = 0f
        things.clear()
        particles.clear()
        floatTexts.clear()
        px = vw * 0.5f
        py = (fieldTop + fieldBottom) / 2f
        repeat(9) { spawnPickup() }
        repeat(4 + zone / 2) { spawnObstacle() }
        repeat(2 + zone / 2) { spawnPest() }
        spawnMagicPlant()
        showBanner(zoneObjectives[zone])
        sounds.play(SoundManager.Fx.OPEN)
    }

    private fun update(dt: Float) {
        gameTime += dt
        if (shield > 0f) shield -= dt
        if (shieldCooldown > 0f) shieldCooldown -= dt
        if (splashWave > 0f) splashWave -= dt * 1.4f
        if (slowTime > 0f) slowTime -= dt
        if (hurtCooldown > 0f) hurtCooldown -= dt
        if (hurtFlash > 0f) hurtFlash -= dt
        if (bannerTimer > 0f) bannerTimer -= dt
        if (shake > 0f) shake = (shake - dt * 2.4f).coerceAtLeast(0f)
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) combo = 0
        }

        val timeScale = if (slowTime > 0f) 0.5f else 1f

        val move = GameLogic.normalized(dragX, dragY)
        moving = move.x != 0f || move.y != 0f
        if (move.x != 0f) faceDir = if (move.x < 0f) -1f else 1f
        val speed = GameLogic.moveSpeed(vw * 0.62f, save.collectSpeed)
        px = (px + move.x * speed * dt).coerceIn(vw * 0.09f, vw * 0.91f)
        py = (py + move.y * speed * dt).coerceIn(fieldTop, fieldBottom)

        waveTimer += dt
        if (waveTimer >= GameLogic.waveInterval(selectedZone)) {
            waveTimer = 0f
            wave++
            repeat(GameLogic.pestsPerWave(selectedZone, wave)) { spawnPest() }
            showBanner("WAVE $wave")
            sounds.play(SoundManager.Fx.UNLOCK, 1.2f)
        }

        pickupTimer += dt
        if (pickupTimer > 1.1f) {
            pickupTimer = 0f
            val pickups = things.count { it.kind == Kind.FRUIT || it.kind == Kind.RARE || it.kind == Kind.CRYSTAL }
            if (pickups < 11) spawnPickup()
        }

        if (!portalSpawned && gameTime > 22f) {
            portalSpawned = true
            spawnPortal()
        }
        if (!chestSpawned && gameTime > 12f && rnd.nextFloat() < 0.012f) {
            chestSpawned = true
            spawnChest()
        }
        if (portalActive) {
            portalTimer -= dt
            if (portalTimer <= 0f) {
                portalActive = false
                things.removeAll { it.kind == Kind.PORTAL }
            }
        }

        val magnet = GameLogic.magnetRadius(vw * 0.15f, save.collectSpeed)
        val remove = mutableListOf<Thing>()

        for (t in things) {
            if (t.flash > 0f) t.flash -= dt
            t.phase += dt
            when (t.kind) {
                Kind.PEST -> updatePest(t, dt * timeScale)
                Kind.FRUIT, Kind.RARE, Kind.CRYSTAL -> {
                    val d = GameLogic.distance(px, py, t.x, t.y)
                    if (d < magnet && d > 1f) {
                        val n = GameLogic.normalized(px - t.x, py - t.y)
                        val pull = (1f - d / magnet) * vw * 0.85f
                        t.x += n.x * pull * dt
                        t.y += n.y * pull * dt
                    }
                }
                Kind.HAZARD -> {
                    t.life -= dt
                    if (t.life <= 0f) remove += t
                }
                else -> Unit
            }
            handleContact(t, remove)
        }

        separatePests()
        things.removeAll(remove.toSet())
        updateParticles(dt)

        if (objectiveComplete()) finishGame(true)
        else if (health <= 0f) finishGame(false)
        else if (selectedZone != 4 && gameTime > 150f) finishGame(false)
    }

    private fun updatePest(t: Thing, dt: Float) {
        val zoneBoost = 1f + selectedZone * 0.07f
        when (t.pest) {
            PestType.BEETLE -> {
                val n = GameLogic.normalized(px - t.x, py - t.y)
                t.x += n.x * vw * 0.17f * zoneBoost * dt
                t.y += n.y * vw * 0.17f * zoneBoost * dt
            }
            PestType.SNAIL -> {
                val n = GameLogic.normalized(px - t.x, py - t.y)
                t.x += n.x * vw * 0.10f * zoneBoost * dt
                t.y += n.y * vw * 0.10f * zoneBoost * dt
            }
            PestType.CATERPILLAR -> {
                t.x += t.vx * dt
                t.y += t.vy * dt
                if (t.x < vw * 0.06f || t.x > vw * 0.94f) t.vx = -t.vx
                if (t.y < fieldTop - vw * 0.05f || t.y > fieldBottom + vw * 0.05f) t.vy = -t.vy
            }
            PestType.WASP -> {
                t.timer -= dt
                val n = GameLogic.normalized(px - t.x, py - t.y)
                val dash = if (t.timer < 0.7f) 2.1f else 1f
                t.x += (n.x * vw * 0.26f * dash + sin(t.phase * 4f) * vw * 0.05f) * zoneBoost * dt
                t.y += n.y * vw * 0.26f * dash * zoneBoost * dt
                if (t.timer <= 0f) {
                    t.timer = 3.4f
                    spawnHazard(t.x, t.y)
                }
            }
        }
        t.x = t.x.coerceIn(vw * 0.04f, vw * 0.96f)
        t.y = t.y.coerceIn(fieldTop - vw * 0.08f, fieldBottom + vw * 0.08f)
    }

    private fun separatePests() {
        val pests = things.filter { it.kind == Kind.PEST }
        val minDist = vw * 0.085f
        for (i in pests.indices) {
            for (j in i + 1 until pests.size) {
                val a = pests[i]
                val b = pests[j]
                val d = GameLogic.distance(a.x, a.y, b.x, b.y)
                if (d in 0.01f..minDist) {
                    val n = GameLogic.normalized(a.x - b.x, a.y - b.y)
                    val push = (minDist - d) * 0.5f
                    a.x += n.x * push
                    a.y += n.y * push
                    b.x -= n.x * push
                    b.y -= n.y * push
                }
            }
        }
    }

    private fun handleContact(t: Thing, remove: MutableList<Thing>) {
        val d = GameLogic.distance(px, py, t.x, t.y)
        val hitR = when (t.kind) {
            Kind.OBSTACLE -> vw * 0.085f
            Kind.PEST -> vw * 0.075f
            Kind.HAZARD -> vw * 0.095f
            Kind.MAGIC_PLANT, Kind.PORTAL, Kind.CHEST -> vw * 0.105f
            else -> vw * 0.08f
        }
        if (d >= hitR) return
        when (t.kind) {
            Kind.FRUIT -> {
                remove += t
                collectFruit(t, 8f, 4f, false)
            }
            Kind.RARE -> {
                remove += t
                rare++
                collectFruit(t, 20f, 9f, true)
            }
            Kind.CRYSTAL -> {
                remove += t
                energy = (energy + GameLogic.energyGain(32f, save.energyReserve)).coerceAtMost(100f)
                health = (health + 6f).coerceAtMost(100f)
                addScore(25, t.x, t.y, Palette.sky)
                spawnBurst(t.x, t.y, 14, Color.rgb(120, 230, 255))
                sounds.play(SoundManager.Fx.FRUIT, 1.15f)
            }
            Kind.OBSTACLE -> {
                val n = GameLogic.normalized(px - t.x, py - t.y)
                px = (px + n.x * (hitR - d)).coerceIn(vw * 0.09f, vw * 0.91f)
                py = (py + n.y * (hitR - d)).coerceIn(fieldTop, fieldBottom)
            }
            Kind.MAGIC_PLANT -> {
                if (!magicPlantUsed) {
                    magicPlantUsed = true
                    slowTime = 7f
                    health = (health + 18f).coerceAtMost(100f)
                    restoration = (restoration + 12f).coerceAtMost(100f)
                    remove += t
                    showBanner("Magic plant slows the pests!")
                    spawnBurst(t.x, t.y, 26, Color.rgb(200, 140, 255))
                    sounds.play(SoundManager.Fx.PORTAL)
                }
            }
            Kind.PORTAL -> {
                if (!portalUsed) {
                    portalUsed = true
                    portalActive = false
                    remove += t
                    health = 100f
                    energy = (energy + 40f).coerceAtMost(100f)
                    restoration = (restoration + 22f).coerceAtMost(100f)
                    addScore(80, t.x, t.y, Palette.restore)
                    showBanner("Garden energy restored!")
                    spawnBurst(t.x, t.y, 30, Color.rgb(120, 255, 170))
                    sounds.play(SoundManager.Fx.PORTAL)
                }
            }
            Kind.CHEST -> {
                remove += t
                val bonus = 30 + selectedZone * 12
                lastReward += bonus
                addScore(100, t.x, t.y, Palette.gold)
                showBanner("Reward chest: +$bonus coins")
                spawnBurst(t.x, t.y, 26, Color.rgb(255, 210, 90))
                sounds.play(SoundManager.Fx.REWARD)
                haptic()
            }
            Kind.PEST -> {
                val damage = when (t.pest) {
                    PestType.BEETLE -> 10f
                    PestType.CATERPILLAR -> 8f
                    PestType.SNAIL -> 14f
                    PestType.WASP -> 7f
                }
                if (hurt(damage * zoneDamageScale(), t.x, t.y) && t.pest == PestType.WASP) {
                    energy = (energy - 14f).coerceAtLeast(0f)
                    addFloatText("-ENERGY", px, py - vw * 0.14f, Palette.energy, vw * 0.03f)
                }
            }
            Kind.HAZARD -> hurt(7f * zoneDamageScale(), t.x, t.y)
        }
    }

    /** Early zones hit softer so the first runs stay welcoming. */
    private fun zoneDamageScale(): Float = 0.7f + selectedZone * 0.1f

    /** Returns true when the guardian actually took the hit. */
    private fun hurt(amount: Float, fromX: Float, fromY: Float): Boolean {
        if (shield > 0f || hurtCooldown > 0f) return false
        health -= GameLogic.damageTaken(amount, save.gardenProtection)
        hurtCooldown = 1.1f
        hurtFlash = 0.35f
        shake = 0.6f
        combo = 0
        comboTimer = 0f
        val n = GameLogic.normalized(px - fromX, py - fromY)
        px = (px + n.x * vw * 0.05f).coerceIn(vw * 0.09f, vw * 0.91f)
        py = (py + n.y * vw * 0.05f).coerceIn(fieldTop, fieldBottom)
        spawnBurst(px, py, 8, Color.rgb(255, 90, 80))
        sounds.play(SoundManager.Fx.PEST, 0.8f)
        haptic()
        return true
    }

    private fun collectFruit(t: Thing, energyBase: Float, restoreBase: Float, isRare: Boolean) {
        collected++
        combo = (combo + 1).coerceAtMost(20)
        comboTimer = 2.8f
        if (t.fruitId.isNotEmpty()) save.unlockFruit(t.fruitId)
        health = (health + if (isRare) 5f else 1.5f).coerceAtMost(100f)
        energy = (energy + GameLogic.energyGain(energyBase, save.energyReserve)).coerceAtMost(100f)
        restoration = (restoration + restoreBase).coerceAtMost(100f)
        val mult = 1 + combo / 4
        addScore((if (isRare) 60 else 10) * mult, t.x, t.y, if (isRare) Palette.berry else Palette.gold)
        spawnBurst(t.x, t.y, if (isRare) 20 else 9, if (isRare) Color.rgb(255, 170, 255) else Color.rgb(255, 210, 80))
        sounds.play(
            when {
                isRare -> SoundManager.Fx.REWARD
                combo >= 5 -> SoundManager.Fx.COMBO
                else -> SoundManager.Fx.FRUIT
            }
        )
        haptic()
    }

    private fun addScore(amount: Int, x: Float, y: Float, color: Int) {
        score += amount
        addFloatText("+$amount", x, y, color, vw * 0.038f)
    }

    private fun addFloatText(text: String, x: Float, y: Float, color: Int, size: Float) {
        floatTexts += FloatText(x, y, text, color, 0.85f, size)
        if (floatTexts.size > 24) floatTexts.removeAt(0)
    }

    private fun showBanner(text: String) {
        banner = text
        bannerTimer = 2.2f
    }

    private fun objectiveComplete(): Boolean = when (selectedZone) {
        0 -> collected >= 16
        1 -> defeated >= 12
        2 -> rare >= 3
        3 -> restoration >= 100f
        else -> gameTime >= 75f
    }

    private fun objectiveStatus(): String = when (selectedZone) {
        0 -> "Fruit $collected/16"
        1 -> "Pests $defeated/12"
        2 -> "Rare $rare/3"
        3 -> "Restore ${restoration.toInt()}/100"
        else -> "Survive ${(75f - gameTime).coerceAtLeast(0f).toInt()}s"
    }

    private fun objectiveProgress(): Float = when (selectedZone) {
        0 -> collected / 16f
        1 -> defeated / 12f
        2 -> rare / 3f
        3 -> restoration / 100f
        else -> gameTime / 75f
    }.coerceIn(0f, 1f)

    private fun spawnPickup() {
        val rareChance = when (selectedZone) {
            0 -> 10
            1, 2 -> 8
            else -> 6
        }
        val roll = rnd.nextInt(rareChance)
        val kind = when {
            roll == 0 -> Kind.RARE
            roll == 1 -> Kind.CRYSTAL
            else -> Kind.FRUIT
        }
        val pair = when (kind) {
            Kind.RARE -> rares.random()
            Kind.CRYSTAL -> "crystal" to "effects/fruit_energy_crystal.png"
            else -> fruits.random()
        }
        things += Thing(
            x = rnd.nextFloat() * vw * 0.78f + vw * 0.11f,
            y = fieldTop + rnd.nextFloat() * (fieldBottom - fieldTop),
            kind = kind,
            sprite = pair.second,
            fruitId = if (kind == Kind.CRYSTAL) "" else pair.first,
            phase = rnd.nextFloat() * 6f,
        )
    }

    private fun spawnPest() {
        if (things.count { it.kind == Kind.PEST } >= 5 + selectedZone * 2) return
        val pool = mutableListOf(PestType.BEETLE, PestType.CATERPILLAR)
        if (selectedZone >= 1) pool += PestType.SNAIL
        if (selectedZone >= 2) pool += PestType.WASP
        val type = pool.random()
        val hp = when (type) {
            PestType.BEETLE -> 3 + selectedZone
            PestType.CATERPILLAR -> 2 + selectedZone
            PestType.SNAIL -> 5 + selectedZone * 2
            PestType.WASP -> 2 + selectedZone / 2
        }
        val sprite = when (type) {
            PestType.BEETLE -> "pests/beetle.png"
            PestType.CATERPILLAR -> "pests/caterpillar.png"
            PestType.SNAIL -> "pests/snail.png"
            PestType.WASP -> "pests/wasp.png"
        }
        val fromSide = rnd.nextBoolean()
        val x = if (fromSide) (if (rnd.nextBoolean()) vw * 0.05f else vw * 0.95f) else rnd.nextFloat() * vw
        val y = if (fromSide) fieldTop + rnd.nextFloat() * (fieldBottom - fieldTop) else {
            if (rnd.nextBoolean()) fieldTop else fieldBottom
        }
        things += Thing(
            x = x,
            y = y,
            kind = Kind.PEST,
            hp = hp,
            maxHp = hp,
            vx = (rnd.nextFloat() - 0.5f) * vw * 0.3f,
            vy = (rnd.nextFloat() - 0.5f) * vw * 0.3f,
            sprite = sprite,
            pest = type,
            timer = 3.4f,
            phase = rnd.nextFloat() * 6f,
        )
    }

    private fun spawnObstacle() {
        val names = listOf("world/rock.png", "world/stump.png", "world/thorn_bush.png", "world/mud.png")
        things += Thing(
            x = rnd.nextFloat() * vw * 0.74f + vw * 0.13f,
            y = fieldTop + vw * 0.08f + rnd.nextFloat() * (fieldBottom - fieldTop - vw * 0.16f),
            kind = Kind.OBSTACLE,
            sprite = names.random(),
        )
    }

    /** Picks a spot inside the play field that is not right under the guardian. */
    private fun fieldSpot(minDistance: Float): Pair<Float, Float> {
        var x = vw * 0.5f
        var y = fieldTop
        var attempts = 0
        do {
            x = vw * (0.16f + rnd.nextFloat() * 0.68f)
            y = fieldTop + rnd.nextFloat() * (fieldBottom - fieldTop)
            attempts++
        } while (GameLogic.distance(px, py, x, y) < minDistance && attempts < 14)
        return x to y
    }

    private fun spawnMagicPlant() {
        val (x, y) = fieldSpot(vw * 0.4f)
        things += Thing(x = x, y = y, kind = Kind.MAGIC_PLANT, sprite = magicPlants.random())
    }

    private fun spawnPortal() {
        portalActive = true
        portalUsed = false
        portalTimer = 12f
        val (x, y) = fieldSpot(vw * 0.45f)
        things += Thing(x = x, y = y, kind = Kind.PORTAL, sprite = "world/restoration_portal.png")
        showBanner("Restoration portal opened!")
        sounds.play(SoundManager.Fx.PORTAL)
    }

    private fun spawnChest() {
        val (x, y) = fieldSpot(vw * 0.4f)
        things += Thing(x = x, y = y, kind = Kind.CHEST, sprite = "rewards/fruit_reward_chest.png")
        showBanner("A reward chest appeared!")
        sounds.play(SoundManager.Fx.REWARD, 0.9f)
    }

    private fun spawnHazard(x: Float, y: Float) {
        things += Thing(x = x, y = y, kind = Kind.HAZARD, life = 3.2f, sprite = "world/mud.png")
    }

    private fun spawnBurst(x: Float, y: Float, count: Int, color: Int) {
        repeat(count) {
            val angle = rnd.nextFloat() * 6.283f
            val speed = vw * (0.1f + rnd.nextFloat() * 0.35f)
            val life = 0.4f + rnd.nextFloat() * 0.5f
            particles += Particle(
                x = x,
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - vw * 0.08f,
                life = life,
                maxLife = life,
                color = color,
                size = vw * (0.006f + rnd.nextFloat() * 0.012f),
            )
        }
        if (particles.size > 220) repeat(particles.size - 220) { particles.removeAt(0) }
    }

    private fun updateParticles(dt: Float) {
        val dead = mutableListOf<Particle>()
        for (p in particles) {
            p.life -= dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += vw * 0.5f * dt
            if (p.life <= 0f) dead += p
        }
        particles.removeAll(dead.toSet())
        val goneTexts = mutableListOf<FloatText>()
        for (f in floatTexts) {
            f.life -= dt
            f.y -= vh * 0.05f * dt
            if (f.life <= 0f) goneTexts += f
        }
        floatTexts.removeAll(goneTexts.toSet())
    }

    private fun activateSplash() {
        if (energy < 100f) return
        energy = 0f
        splashWave = 1f
        shake = 1f
        sounds.play(SoundManager.Fx.SPLASH)
        sounds.play(SoundManager.Fx.WAVE)
        val radius = GameLogic.splashRadius(vw * 0.46f, save.splashPower)
        val doomed = mutableListOf<Thing>()
        for (t in things) {
            val d = GameLogic.distance(px, py, t.x, t.y)
            if (d > radius) continue
            when {
                t.kind == Kind.PEST -> {
                    t.hp -= GameLogic.splashDamage(d, radius, save.splashPower)
                    t.flash = 0.25f
                    if (t.hp <= 0) {
                        doomed += t
                        defeated++
                        restoration = (restoration + 4f).coerceAtMost(100f)
                        addScore(20 + selectedZone * 5, t.x, t.y, Palette.restoreLight)
                        spawnBurst(t.x, t.y, 12, Color.rgb(140, 255, 160))
                    }
                }
                t.kind == Kind.FRUIT || t.kind == Kind.RARE || t.kind == Kind.CRYSTAL -> {
                    val n = GameLogic.normalized(px - t.x, py - t.y)
                    t.x += n.x * d * 0.9f
                    t.y += n.y * d * 0.9f
                }
                t.kind == Kind.HAZARD -> doomed += t
                t.kind == Kind.OBSTACLE && t.sprite.endsWith("mud.png") -> doomed += t
            }
        }
        if (doomed.isNotEmpty()) sounds.play(SoundManager.Fx.PEST)
        things.removeAll(doomed.toSet())
        spawnBurst(px, py, 30, Palette.energy)
        haptic()
    }

    private fun finishGame(win: Boolean) {
        if (screen == Screen.RESULT) return
        resultWin = win
        screen = Screen.RESULT
        gamePaused = false
        resultAnim = 0f
        dragging = false
        dragX = 0f
        dragY = 0f
        if (win) {
            resultStars = GameLogic.starsForScore(score, GameLogic.scoreTarget(selectedZone))
            save.setZoneStars(selectedZone, resultStars)
            lastReward += GameLogic.rewardForZone(selectedZone, rare, restoration) +
                GameLogic.starBonus(resultStars)
            save.coins += lastReward
            save.fruitTotal += collected
            save.pestsTotal += defeated
            if (score > save.bestScore) save.bestScore = score
            if (selectedZone == save.zonesCleared && save.zonesCleared < 5) {
                save.zonesCleared = save.zonesCleared + 1
                sounds.play(SoundManager.Fx.UNLOCK)
                showToast("New zone unlocked!")
            }
            if (selectedZone >= 1) {
                save.unlockFruit(seeds[(selectedZone - 1).coerceIn(0, seeds.lastIndex)].first)
            }
            sounds.play(SoundManager.Fx.WIN)
        } else {
            resultStars = 0
            lastReward = 0
            sounds.play(SoundManager.Fx.LOSE)
        }
    }

    // endregion

    // region gameplay drawing

    private fun drawGame(c: Canvas) {
        c.save()
        if (shake > 0f) {
            c.translate(
                (rnd.nextFloat() - 0.5f) * shake * vw * 0.02f,
                (rnd.nextFloat() - 0.5f) * shake * vw * 0.02f,
            )
        }
        drawBackdrop(c, backgrounds[selectedZone], dim = 60)
        drawAsset(c, "world/garden_path.png", vw * 0.5f, (fieldTop + fieldBottom) / 2f, vw * 1.05f, 140)

        val sorted = things.sortedBy { it.y }
        for (t in sorted) {
            if (t.kind == Kind.HAZARD) {
                drawHazard(c, t)
                continue
            }
            val size = when (t.kind) {
                Kind.OBSTACLE -> vw * 0.155f
                Kind.PEST -> if (t.pest == PestType.SNAIL) vw * 0.145f else vw * 0.125f
                Kind.MAGIC_PLANT, Kind.PORTAL -> vw * 0.17f
                Kind.CHEST -> vw * 0.15f
                else -> vw * 0.105f
            }
            val bob = when (t.kind) {
                Kind.FRUIT, Kind.RARE, Kind.CRYSTAL -> sin(t.phase * 3f) * vw * 0.008f
                else -> 0f
            }
            groundShadow(c, t.x, t.y + size * 0.34f, size * 0.62f)
            if (t.kind == Kind.PORTAL) {
                val pulse = 0.85f + 0.15f * sin(anim * 4f)
                paint.shader = RadialGradient(
                    t.x, t.y, vw * 0.14f * pulse,
                    Color.argb(140, 120, 255, 180), Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
                c.drawCircle(t.x, t.y, vw * 0.14f * pulse, paint)
                ui.reset()
            }
            if (t.kind == Kind.RARE || t.kind == Kind.CRYSTAL) {
                paint.shader = RadialGradient(
                    t.x, t.y + bob, size * 0.8f,
                    Color.argb(120, 255, 240, 170), Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
                c.drawCircle(t.x, t.y + bob, size * 0.8f, paint)
                ui.reset()
            }
            drawAsset(c, t.sprite, t.x, t.y + bob, size, if (t.flash > 0f) 170 else 255)
            if (t.kind == Kind.PEST && t.hp < t.maxHp) drawPestHealth(c, t, size)
        }

        for (p in particles) {
            paint.color = p.color
            paint.alpha = (255 * (p.life / p.maxLife)).toInt().coerceIn(0, 235)
            c.drawCircle(p.x, p.y, p.size, paint)
        }
        ui.reset()

        drawPlayer(c)

        if (splashWave > 0f) {
            val t = 1f - splashWave
            val radius = GameLogic.splashRadius(vw * 0.46f, save.splashPower) * (0.35f + t * 0.75f)
            drawAsset(c, "effects/fruit_juice_wave.png", px, py, radius * 2.2f, (200 * splashWave).toInt())
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = vw * 0.03f * splashWave
            paint.color = Color.argb((220 * splashWave).toInt().coerceIn(0, 255), 255, 190, 60)
            c.drawCircle(px, py, radius, paint)
            paint.strokeWidth = vw * 0.012f * splashWave
            paint.color = Color.argb((190 * splashWave).toInt().coerceIn(0, 255), 255, 246, 190)
            c.drawCircle(px, py, radius * 0.78f, paint)
            ui.reset()
        }

        for (f in floatTexts) {
            val alpha = (255 * (f.life / 0.85f)).toInt().coerceIn(0, 255)
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.textSize = f.size
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = f.size * 0.24f
            paint.color = Color.argb((alpha * 0.8f).toInt(), 0, 20, 8)
            c.drawText(f.text, f.x, f.y, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(alpha, Color.red(f.color), Color.green(f.color), Color.blue(f.color))
            c.drawText(f.text, f.x, f.y, paint)
        }
        ui.reset()
        c.restore()

        if (hurtFlash > 0f) {
            paint.shader = RadialGradient(
                vw / 2f, vh / 2f, maxOf(vw, vh) * 0.7f,
                Color.TRANSPARENT, Color.argb((hurtFlash * 380).toInt().coerceIn(0, 190), 220, 30, 24),
                Shader.TileMode.CLAMP,
            )
            c.drawRect(0f, 0f, vw, vh, paint)
            ui.reset()
        }

        drawHud(c)
        drawControls(c)
        if (gamePaused) drawPause(c)
    }

    private fun drawPlayer(c: Canvas) {
        val size = vw * 0.21f
        val bob = if (moving) sin(anim * 12f) * vw * 0.008f else sin(anim * 2.4f) * vw * 0.004f
        groundShadow(c, px, py + size * 0.36f, size * 0.6f)
        if (shield > 0f) {
            val pulse = 0.9f + 0.1f * sin(anim * 8f)
            paint.shader = RadialGradient(
                px, py, vw * 0.15f * pulse,
                Color.argb(30, 120, 240, 255), Color.argb(150, 90, 220, 255), Shader.TileMode.CLAMP,
            )
            c.drawCircle(px, py, vw * 0.15f * pulse, paint)
            ui.reset()
        }
        val flashing = hurtCooldown > 0f && (anim * 14f).toInt() % 2 == 0
        c.save()
        c.scale(faceDir, 1f, px, py)
        drawAsset(c, "player/fruit_gardener.png", px, py + bob, size, if (flashing) 130 else 255)
        c.restore()
        if (shield > 0f) {
            drawAsset(c, "effects/fruit_shell_shield.png", px, py, vw * 0.27f, 130)
        }
        if (combo >= 3 && comboTimer > 0f) {
            ui.label(
                c,
                "x${1 + combo / 4}",
                px,
                py - size * 0.6f,
                vw * 0.05f,
                Palette.gold,
                Paint.Align.CENTER,
                bold = true,
            )
        }
    }

    private fun drawHazard(c: Canvas, t: Thing) {
        val pulse = 0.85f + 0.15f * sin(anim * 7f)
        val r = vw * 0.1f * pulse
        paint.shader = RadialGradient(
            t.x, t.y, r,
            Color.argb(150, 180, 60, 200), Color.argb(20, 120, 30, 160), Shader.TileMode.CLAMP,
        )
        c.drawCircle(t.x, t.y, r, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = vw * 0.008f
        paint.color = Color.argb(200, 220, 120, 255)
        c.drawCircle(t.x, t.y, r, paint)
        ui.reset()
    }

    private fun groundShadow(c: Canvas, x: Float, y: Float, w: Float) {
        paint.color = Color.argb(70, 0, 0, 0)
        c.drawOval(RectF(x - w / 2, y - w * 0.16f, x + w / 2, y + w * 0.16f), paint)
        ui.reset()
    }

    private fun drawPestHealth(c: Canvas, t: Thing, size: Float) {
        val w = size * 0.62f
        val top = t.y - size * 0.55f
        val r = RectF(t.x - w / 2, top, t.x + w / 2, top + vh * 0.007f)
        paint.color = Color.argb(180, 0, 0, 0)
        c.drawRoundRect(r, r.height(), r.height(), paint)
        paint.color = Palette.health
        c.drawRoundRect(
            RectF(r.left, r.top, r.left + r.width() * (t.hp.toFloat() / t.maxHp).coerceIn(0f, 1f), r.bottom),
            r.height(),
            r.height(),
            paint,
        )
        ui.reset()
    }

    private fun drawHud(c: Canvas) {
        val panel = RectF(vw * 0.025f, vh * 0.012f, vw * 0.975f, hudBottom)
        ui.panel(c, panel, vh * 0.022f, Color.argb(222, 16, 62, 34), Color.argb(232, 6, 34, 18))

        val barLeft = vw * 0.125f
        val barRight = vw * 0.74f
        val barH = vh * 0.026f
        val ys = floatArrayOf(vh * 0.038f, vh * 0.078f, vh * 0.118f)
        ui.bar(c, RectF(barLeft, ys[0] - barH / 2, barRight, ys[0] + barH / 2), health, Palette.health, Palette.healthLight, "heart")
        ui.bar(c, RectF(barLeft, ys[1] - barH / 2, barRight, ys[1] + barH / 2), energy, Palette.energy, Palette.energyLight, "bolt")
        ui.bar(c, RectF(barLeft, ys[2] - barH / 2, barRight, ys[2] + barH / 2), restoration, Palette.restore, Palette.restoreLight, "leaf")
        circleBtn(c, "pause", "pause", vw * 0.875f, vh * 0.078f, vw * 0.055f, BtnStyle.SKY)

        val chipH = vh * 0.042f
        val chipY = hudBottom + vh * 0.012f
        ui.chip(c, RectF(vw * 0.025f, chipY, vw * 0.60f, chipY + chipH), objectiveStatus(), "target")
        ui.chip(c, RectF(vw * 0.62f, chipY, vw * 0.975f, chipY + chipH), "WAVE $wave", "bug")

        val track = RectF(vw * 0.06f, chipY + chipH + vh * 0.004f, vw * 0.57f, chipY + chipH + vh * 0.012f)
        paint.color = Color.argb(150, 0, 0, 0)
        c.drawRoundRect(track, track.height(), track.height(), paint)
        paint.color = Palette.gold
        c.drawRoundRect(
            RectF(track.left, track.top, track.left + track.width() * objectiveProgress(), track.bottom),
            track.height(),
            track.height(),
            paint,
        )
        ui.reset()

        ui.label(c, "SCORE $score", vw * 0.035f, chipY + chipH + vh * 0.032f, vw * 0.032f, Palette.cream, bold = true)
        if (slowTime > 0f) {
            ui.label(
                c,
                "SLOW ${slowTime.toInt() + 1}s",
                vw * 0.965f,
                chipY + chipH + vh * 0.032f,
                vw * 0.032f,
                Color.rgb(206, 168, 255),
                Paint.Align.RIGHT,
                bold = true,
            )
        }

        if (combo >= 2 && comboTimer > 0f) {
            val scale = 1f + 0.08f * sin(anim * 10f)
            ui.label(
                c,
                "COMBO x$combo",
                vw * 0.5f,
                vh * 0.30f,
                vw * 0.055f * scale,
                Palette.gold,
                Paint.Align.CENTER,
                bold = true,
            )
        }
        if (bannerTimer > 0f && banner.isNotEmpty()) {
            val alpha = (bannerTimer.coerceAtMost(1f) * 235).toInt()
            val r = RectF(vw * 0.1f, vh * 0.36f, vw * 0.9f, vh * 0.42f)
            ui.chip(c, r, banner, "star", Color.argb(alpha, 8, 40, 20))
        }
    }

    private fun drawControls(c: Canvas) {
        val splashReady = energy >= 100f
        val splashCx = vw * 0.81f
        val splashCy = vh - vw * 0.19f
        if (splashReady) {
            val pulse = 0.9f + 0.12f * sin(anim * 6f)
            paint.shader = RadialGradient(
                splashCx, splashCy, vw * 0.22f * pulse,
                Color.argb(120, 255, 200, 60), Color.TRANSPARENT, Shader.TileMode.CLAMP,
            )
            c.drawCircle(splashCx, splashCy, vw * 0.22f * pulse, paint)
            ui.reset()
        }
        circleBtn(
            c,
            "splash",
            "splash",
            splashCx,
            splashCy,
            vw * 0.115f,
            BtnStyle.PRIMARY,
            splashReady,
            energy / 100f,
        )
        if (!splashReady) {
            ui.label(
                c,
                "${energy.toInt()}%",
                splashCx,
                splashCy + vw * 0.185f,
                vw * 0.03f,
                Palette.muted,
                Paint.Align.CENTER,
                bold = true,
            )
        }

        val shieldReady = shield <= 0f && shieldCooldown <= 0f
        val shieldCx = vw * 0.55f
        val shieldCy = vh - vw * 0.13f
        circleBtn(
            c,
            "shield",
            "shield",
            shieldCx,
            shieldCy,
            vw * 0.078f,
            BtnStyle.SKY,
            shieldReady,
            if (shieldReady) -1f else 1f - (shieldCooldown / 9f).coerceIn(0f, 1f),
        )

        if (dragging) {
            paint.shader = RadialGradient(
                dragOriginX, dragOriginY, vw * 0.115f,
                Color.argb(120, 255, 255, 255), Color.argb(18, 255, 255, 255), Shader.TileMode.CLAMP,
            )
            c.drawCircle(dragOriginX, dragOriginY, vw * 0.115f, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = vw * 0.008f
            paint.color = Color.argb(150, 255, 255, 255)
            c.drawCircle(dragOriginX, dragOriginY, vw * 0.115f, paint)
            paint.style = Paint.Style.FILL
            val knobX = dragOriginX + dragX * 0.5f
            val knobY = dragOriginY + dragY * 0.5f
            paint.color = Color.argb(80, 0, 0, 0)
            c.drawCircle(knobX + 2f, knobY + 4f, vw * 0.045f, paint)
            paint.color = Color.argb(235, 255, 252, 235)
            c.drawCircle(knobX, knobY, vw * 0.045f, paint)
            ui.reset()
        } else {
            ui.label(
                c,
                "Drag anywhere to move",
                vw * 0.24f,
                vh - vw * 0.05f,
                vw * 0.028f,
                Color.argb(150, 220, 240, 224),
                Paint.Align.CENTER,
            )
        }
    }

    private fun drawPause(c: Canvas) {
        ui.scrim(c, vw, vh, 225)
        val panel = RectF(vw * 0.1f, vh * 0.22f, vw * 0.9f, vh * 0.72f)
        ui.panel(c, panel, vw * 0.07f)
        ui.heading(c, "PAUSED", vw * 0.5f, vh * 0.3f, vw * 0.075f)
        drawAsset(c, "effects/fruit_splash_energy.png", vw * 0.5f, vh * 0.385f, vw * 0.22f, 220)
        ui.label(c, objectiveStatus(), vw * 0.5f, vh * 0.45f, vw * 0.04f, Palette.cream, Paint.Align.CENTER, bold = true)
        ui.label(c, "Score $score  •  Wave $wave", vw * 0.5f, vh * 0.49f, vw * 0.032f, Palette.muted, Paint.Align.CENTER)
        btn(c, "resume", "RESUME", RectF(vw * 0.18f, vh * 0.53f, vw * 0.82f, vh * 0.60f), BtnStyle.PRIMARY, icon = "play")
        btn(c, "exit_game", "QUIT TO MENU", RectF(vw * 0.18f, vh * 0.62f, vw * 0.82f, vh * 0.685f), BtnStyle.GHOST, icon = "home")
    }

    // endregion

    // region result & progression screens

    private fun drawResult(c: Canvas) {
        drawBackdrop(c, backgrounds[selectedZone], dim = 160)
        val panel = RectF(vw * 0.07f, vh * 0.14f, vw * 0.93f, vh * 0.78f)
        ui.panel(c, panel, vw * 0.07f)
        ui.heading(c, if (resultWin) "ZONE CLEARED!" else "GARDEN OVERRUN", vw * 0.5f, vh * 0.225f, vw * 0.062f)

        val shown = if (resultWin) ((resultAnim / 0.4f).toInt()).coerceIn(0, resultStars) else 0
        ui.stars(c, vw * 0.5f, vh * 0.28f, vw * 0.1f, shown)

        drawAsset(
            c,
            if (resultWin) "rewards/fruit_reward_chest.png" else "pests/beetle.png",
            vw * 0.5f,
            vh * 0.42f,
            vw * 0.30f,
        )

        val rowY = vh * 0.53f
        val gap = vh * 0.045f
        statRow(c, "SCORE", "$score", rowY, "star")
        statRow(c, "FRUIT COLLECTED", "$collected", rowY + gap, "leaf")
        statRow(c, "PESTS DEFEATED", "$defeated", rowY + gap * 2, "bug")
        statRow(c, if (resultWin) "REWARD" else "REWARD LOST", "$lastReward", rowY + gap * 3, "coin")

        btn(
            c,
            if (resultWin) "claim" else "retry",
            if (resultWin) "COLLECT REWARD" else "TRY AGAIN",
            RectF(vw * 0.14f, vh * 0.81f, vw * 0.86f, vh * 0.88f),
            BtnStyle.PRIMARY,
            icon = if (resultWin) "coin" else "retry",
        )
        btn(c, "menu", "MAIN MENU", RectF(vw * 0.26f, vh * 0.90f, vw * 0.74f, vh * 0.96f), BtnStyle.GHOST, icon = "home")
    }

    private fun statRow(c: Canvas, label: String, value: String, y: Float, iconName: String) {
        val r = RectF(vw * 0.12f, y - vh * 0.018f, vw * 0.88f, y + vh * 0.018f)
        paint.color = Color.argb(90, 0, 0, 0)
        c.drawRoundRect(r, r.height() * 0.4f, r.height() * 0.4f, paint)
        ui.reset()
        ui.icon(c, iconName, r.left + vw * 0.045f, r.centerY(), vw * 0.045f, Palette.gold)
        ui.label(c, label, r.left + vw * 0.085f, r.centerY() + vw * 0.012f, vw * 0.031f, Palette.muted, bold = true)
        ui.label(c, value, r.right - vw * 0.04f, r.centerY() + vw * 0.014f, vw * 0.038f, Palette.cream, Paint.Align.RIGHT, bold = true)
    }

    private fun screenHeader(c: Canvas, title: String) {
        circleBtn(c, "back", "back", vw * 0.11f, vh * 0.06f, vw * 0.052f, BtnStyle.LEAF)
        ui.heading(c, title, vw * 0.55f, vh * 0.075f, vw * 0.058f)
        ui.chip(c, RectF(vw * 0.32f, vh * 0.105f, vw * 0.68f, vh * 0.145f), "${save.coins} coins", "coin")
    }

    private fun drawRestore(c: Canvas) {
        drawBackdrop(c, dim = 120)
        screenHeader(c, "GARDEN")
        val level = save.gardenLevel
        val panel = RectF(vw * 0.06f, vh * 0.19f, vw * 0.94f, vh * 0.70f)
        ui.panel(c, panel, vw * 0.06f)

        val trees = listOf(
            "world/apple_tree.png",
            "world/orange_tree.png",
            "world/berry_tree.png",
            "world/lemon_tree.png",
            "world/upgraded_fruit_tree.png",
        )
        val plants = listOf("world/flower.png", "world/sprout.png", "world/bush.png", "world/herbs.png")
        val decor = listOf("world/bench.png", "world/lantern.png", "world/fence.png", "world/sign.png")
        drawAsset(c, "world/garden_path.png", vw * 0.5f, vh * 0.505f, vw * 0.8f, 150)
        drawAsset(c, trees[level.coerceAtMost(trees.lastIndex)], vw * 0.5f, vh * 0.325f, vw * 0.46f)
        drawAsset(c, plants[level.coerceAtMost(plants.lastIndex)], vw * 0.23f, vh * 0.475f, vw * 0.19f)
        drawAsset(c, decor[level.coerceAtMost(decor.lastIndex)], vw * 0.77f, vh * 0.475f, vw * 0.19f)

        val barR = RectF(vw * 0.22f, vh * 0.59f, vw * 0.86f, vh * 0.62f)
        ui.bar(c, barR, (level + 1) * 20f, Palette.restore, Palette.restoreLight, "sprout", showValue = false)
        ui.label(c, "Garden level ${level + 1} / 5", vw * 0.5f, vh * 0.66f, vw * 0.036f, Palette.cream, Paint.Align.CENTER, bold = true)

        val cost = 120 + level * 90
        btn(
            c,
            "upgrade_garden",
            if (level >= 4) "FULLY RESTORED" else "RESTORE — $cost",
            RectF(vw * 0.14f, vh * 0.74f, vw * 0.86f, vh * 0.81f),
            BtnStyle.PRIMARY,
            enabled = level < 4 && save.coins >= cost,
            icon = "sprout",
        )
        ui.label(
            c,
            "Every restored level makes the whole garden bloom.",
            vw * 0.5f,
            vh * 0.86f,
            vw * 0.03f,
            Palette.muted,
            Paint.Align.CENTER,
        )
    }

    private fun drawUpgrades(c: Canvas) {
        drawBackdrop(c, dim = 130)
        screenHeader(c, "UPGRADES")
        upgradeRow(c, "splash", "bolt", "Splash Power", "Bigger juice wave", save.splashPower, vh * 0.19f)
        upgradeRow(c, "energy", "drop", "Energy Reserve", "Fill the wave faster", save.energyReserve, vh * 0.375f)
        upgradeRow(c, "speed", "up", "Collection Speed", "Move faster, bigger magnet", save.collectSpeed, vh * 0.56f)
        upgradeRow(c, "protect", "shield", "Garden Protection", "Take less pest damage", save.gardenProtection, vh * 0.745f)
    }

    private fun upgradeRow(c: Canvas, id: String, iconName: String, name: String, detail: String, level: Int, top: Float) {
        val r = RectF(vw * 0.05f, top, vw * 0.95f, top + vh * 0.155f)
        ui.panel(c, r, vw * 0.05f, Palette.cardTop, Palette.cardBottom)
        val badgeCx = r.left + vw * 0.11f
        paint.color = Color.argb(120, 0, 0, 0)
        c.drawCircle(badgeCx, r.top + vh * 0.05f, vw * 0.065f, paint)
        ui.reset()
        ui.icon(c, iconName, badgeCx, r.top + vh * 0.05f, vw * 0.075f, Palette.gold)
        ui.label(c, name, r.left + vw * 0.2f, r.top + vh * 0.042f, vw * 0.042f, Palette.cream, bold = true)
        ui.label(c, detail, r.left + vw * 0.2f, r.top + vh * 0.072f, vw * 0.029f, Palette.muted)

        ui.label(c, "Lv $level/5", r.left + vw * 0.045f, r.bottom - vh * 0.028f, vw * 0.028f, Palette.muted, bold = true)
        for (i in 0 until 5) {
            val cx = r.left + vw * 0.215f + i * vw * 0.062f
            val cy = r.bottom - vh * 0.035f
            paint.color = if (i < level) Palette.gold else Color.argb(90, 255, 255, 255)
            c.drawRoundRect(RectF(cx - vw * 0.022f, cy - vh * 0.008f, cx + vw * 0.022f, cy + vh * 0.008f), 8f, 8f, paint)
        }
        ui.reset()

        val cost = save.upgradeCost(level)
        btn(
            c,
            "buy_$id",
            if (level >= 5) "MAX" else "$cost",
            RectF(r.right - vw * 0.28f, r.top + vh * 0.045f, r.right - vw * 0.04f, r.top + vh * 0.11f),
            BtnStyle.PRIMARY,
            enabled = level < 5 && save.coins >= cost,
            icon = if (level >= 5) "check" else "coin",
        )
    }

    private fun drawCollection(c: Canvas) {
        drawBackdrop(c, dim = 130)
        screenHeader(c, "FRUITS")
        val all = fruits + rares + seeds
        val panel = RectF(vw * 0.04f, vh * 0.17f, vw * 0.96f, vh * 0.80f)
        ui.panel(c, panel, vw * 0.05f)
        all.forEachIndexed { i, (id, path) ->
            val col = i % 4
            val row = i / 4
            val cellW = panel.width() / 4f
            val cx = panel.left + cellW * (col + 0.5f)
            val cy = panel.top + vh * 0.075f + row * vh * 0.145f
            val unlocked = save.isFruitUnlocked(id)
            val cell = RectF(cx - cellW * 0.4f, cy - vh * 0.055f, cx + cellW * 0.4f, cy + vh * 0.06f)
            paint.color = Color.argb(if (unlocked) 110 else 60, 0, 0, 0)
            c.drawRoundRect(cell, vw * 0.035f, vw * 0.035f, paint)
            ui.reset()
            if (unlocked) {
                drawAsset(c, path, cx, cy - vh * 0.012f, cellW * 0.6f)
            } else {
                drawAsset(c, path, cx, cy - vh * 0.012f, cellW * 0.6f, 45)
                ui.icon(c, "lock", cx, cy - vh * 0.012f, cellW * 0.34f, Color.argb(200, 220, 230, 220))
            }
            ui.label(
                c,
                if (unlocked) id.replace('_', ' ').uppercase() else "???",
                cx,
                cy + vh * 0.048f,
                vw * 0.022f,
                if (unlocked) Palette.cream else Palette.muted,
                Paint.Align.CENTER,
                bold = true,
            )
        }
        val found = save.unlockedFruitCount(all.map { it.first })
        ui.chip(c, RectF(vw * 0.25f, vh * 0.83f, vw * 0.75f, vh * 0.878f), "Discovered $found/${all.size}", "book")
    }

    private fun drawMissions(c: Canvas) {
        drawBackdrop(c, dim = 130)
        screenHeader(c, "TASKS")
        mission(c, "harvest", "leaf", "Fresh Harvest", "Collect 50 fruit", save.fruitTotal, 50, 80, vh * 0.19f)
        mission(c, "guardian", "shield", "Garden Guardian", "Clear 3 zones", save.zonesCleared, 3, 120, vh * 0.375f)
        mission(c, "master", "sprout", "Master Gardener", "Reach garden level 4", save.gardenLevel, 4, 150, vh * 0.56f)
        mission(c, "hunter", "bug", "Pest Hunter", "Defeat 40 pests", save.pestsTotal, 40, 100, vh * 0.745f)
    }

    private fun mission(
        c: Canvas,
        id: String,
        iconName: String,
        name: String,
        detail: String,
        value: Int,
        target: Int,
        reward: Int,
        top: Float,
    ) {
        val r = RectF(vw * 0.05f, top, vw * 0.95f, top + vh * 0.155f)
        ui.panel(c, r, vw * 0.05f, Palette.cardTop, Palette.cardBottom)
        val badgeCx = r.left + vw * 0.11f
        paint.color = Color.argb(120, 0, 0, 0)
        c.drawCircle(badgeCx, r.top + vh * 0.05f, vw * 0.065f, paint)
        ui.reset()
        ui.icon(c, iconName, badgeCx, r.top + vh * 0.05f, vw * 0.075f, Palette.gold)
        ui.label(c, name, r.left + vw * 0.2f, r.top + vh * 0.042f, vw * 0.042f, Palette.cream, bold = true)
        ui.label(c, detail, r.left + vw * 0.2f, r.top + vh * 0.072f, vw * 0.029f, Palette.muted)

        val claimed = save.isMissionClaimed(id)
        val progress = (value.toFloat() / target * 100f).coerceIn(0f, 100f)
        ui.bar(
            c,
            RectF(r.left + vw * 0.11f, r.bottom - vh * 0.045f, r.right - vw * 0.44f, r.bottom - vh * 0.023f),
            progress,
            Palette.restore,
            Palette.restoreLight,
            "target",
            showValue = false,
        )
        ui.label(
            c,
            "${value.coerceAtMost(target)}/$target",
            r.right - vw * 0.41f,
            r.bottom - vh * 0.026f,
            vw * 0.028f,
            Palette.muted,
            bold = true,
        )
        val ready = !claimed && value >= target
        btn(
            c,
            "claim_$id",
            if (claimed) "DONE" else "+$reward",
            RectF(r.right - vw * 0.28f, r.top + vh * 0.045f, r.right - vw * 0.04f, r.top + vh * 0.11f),
            if (ready) BtnStyle.PRIMARY else BtnStyle.GHOST,
            enabled = ready,
            icon = if (claimed) "check" else "coin",
        )
    }

    private fun drawSettings(c: Canvas) {
        drawBackdrop(c, dim = 140)
        screenHeader(c, "SETTINGS")
        val panel = RectF(vw * 0.06f, vh * 0.18f, vw * 0.94f, vh * 0.47f)
        ui.panel(c, panel, vw * 0.06f)
        toggle(c, "sfx", "sound", "Sound effects", save.sfxEnabled, vh * 0.245f)
        toggle(c, "haptics", "vibrate", "Haptic feedback", save.hapticsEnabled, vh * 0.345f)
        ui.label(
            c,
            "Gameplay is fully offline.",
            vw * 0.5f,
            vh * 0.435f,
            vw * 0.03f,
            Palette.muted,
            Paint.Align.CENTER,
        )
        btn(c, "privacy", "PRIVACY POLICY", RectF(vw * 0.14f, vh * 0.52f, vw * 0.86f, vh * 0.585f), BtnStyle.SKY, icon = "doc")
        btn(c, "support", "SUPPORT", RectF(vw * 0.14f, vh * 0.615f, vw * 0.86f, vh * 0.68f), BtnStyle.LEAF, icon = "help")
        ui.label(c, "Best score ${save.bestScore}", vw * 0.5f, vh * 0.75f, vw * 0.034f, Palette.cream, Paint.Align.CENTER, bold = true)
        ui.label(c, "Stars ${save.totalStars()}/15", vw * 0.5f, vh * 0.79f, vw * 0.032f, Palette.muted, Paint.Align.CENTER)
    }

    private fun toggle(c: Canvas, id: String, iconName: String, label: String, on: Boolean, cy: Float) {
        ui.icon(c, iconName, vw * 0.15f, cy, vw * 0.055f, Palette.gold)
        ui.label(c, label, vw * 0.22f, cy + vw * 0.016f, vw * 0.04f, Palette.cream, bold = true)
        val r = RectF(vw * 0.7f, cy - vw * 0.035f, vw * 0.88f, cy + vw * 0.035f)
        paint.color = if (on) Palette.leafDeep else Color.argb(200, 40, 46, 40)
        c.drawRoundRect(r, r.height() / 2, r.height() / 2, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.argb(150, 220, 250, 224)
        c.drawRoundRect(r, r.height() / 2, r.height() / 2, paint)
        paint.style = Paint.Style.FILL
        val knobX = if (on) r.right - r.height() / 2 else r.left + r.height() / 2
        paint.color = Color.argb(90, 0, 0, 0)
        c.drawCircle(knobX + 1f, r.centerY() + 3f, r.height() * 0.42f, paint)
        paint.color = if (on) Palette.restoreLight else Color.rgb(200, 206, 200)
        c.drawCircle(knobX, r.centerY(), r.height() * 0.42f, paint)
        ui.reset()
        hitTargets += HitRect(id, RectF(r.left - vw * 0.02f, r.top - vw * 0.02f, r.right + vw * 0.02f, r.bottom + vw * 0.02f))
    }

    // endregion

    private fun drawAsset(c: Canvas, path: String, x: Float, y: Float, size: Float, alpha: Int = 255) {
        val b = bitmaps.getOrPut(path) { openAsset(path) }
        val ratio = b.height / b.width.toFloat()
        paint.shader = null
        paint.colorFilter = null
        paint.style = Paint.Style.FILL
        paint.alpha = alpha.coerceIn(0, 255)
        c.drawBitmap(b, null, RectF(x - size / 2, y - size * ratio / 2, x + size / 2, y + size * ratio / 2), paint)
        paint.alpha = 255
    }

    private fun openAsset(path: String): Bitmap = try {
        host.assets.open("sprites/$path").use { BitmapFactory.decodeStream(it) }
            ?: Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    } catch (_: Exception) {
        Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    }

    private fun haptic() {
        if (!save.hapticsEnabled) return
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            val manager = host.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            host.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(18)
        }
    }

    private fun showToast(text: String) {
        toast = text
        toastTimer = 2.2f
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = hitTargets.lastOrNull { it.rect.contains(x, y) }
                if (hit != null) {
                    pressedId = hit.id
                    if (hit.id == "splash" || hit.id == "shield") handleHit(hit.id)
                    invalidate()
                    return true
                }
                if (screen == Screen.GAME && !gamePaused && y > hudBottom) {
                    dragging = true
                    dragOriginX = x
                    dragOriginY = y
                    dragX = 0f
                    dragY = 0f
                }
            }
            MotionEvent.ACTION_MOVE -> if (screen == Screen.GAME && dragging) {
                val maxDrag = vw * 0.2f
                dragX = (x - dragOriginX).coerceIn(-maxDrag, maxDrag)
                dragY = (y - dragOriginY).coerceIn(-maxDrag, maxDrag)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val id = pressedId
                pressedId = null
                if (id != null && e.actionMasked == MotionEvent.ACTION_UP &&
                    id != "splash" && id != "shield"
                ) {
                    val target = hitTargets.firstOrNull { it.id == id }
                    if (target != null && target.rect.contains(x, y)) handleHit(id)
                }
                dragging = false
                dragX = 0f
                dragY = 0f
            }
        }
        invalidate()
        return true
    }

    private fun handleHit(id: String) {
        sounds.play(SoundManager.Fx.BUTTON)
        when (id) {
            "play", "zones" -> {
                screen = Screen.ZONES
                sounds.play(SoundManager.Fx.OPEN)
            }
            "restore" -> screen = Screen.RESTORE
            "upgrades" -> screen = Screen.UPGRADES
            "collection" -> screen = Screen.COLLECTION
            "missions" -> screen = Screen.MISSIONS
            "settings" -> screen = Screen.SETTINGS
            "back", "menu" -> {
                screen = Screen.MENU
                sounds.play(SoundManager.Fx.CLOSE)
            }
            "pause" -> gamePaused = true
            "resume" -> gamePaused = false
            "exit_game" -> {
                gamePaused = false
                screen = Screen.MENU
            }
            "shield" -> if (shield <= 0f && shieldCooldown <= 0f) {
                shield = 5.5f
                shieldCooldown = 9f
                sounds.play(SoundManager.Fx.SHIELD)
                haptic()
            }
            "splash" -> activateSplash()
            "claim" -> {
                sounds.play(SoundManager.Fx.REWARD)
                screen = Screen.ZONES
            }
            "retry" -> startGame(selectedZone)
            "privacy" -> openWeb("privacy")
            "support" -> openWeb("support")
            "sfx" -> save.sfxEnabled = !save.sfxEnabled
            "haptics" -> save.hapticsEnabled = !save.hapticsEnabled
            "upgrade_garden" -> {
                val cost = 120 + save.gardenLevel * 90
                if (save.gardenLevel < 4 && save.coins >= cost) {
                    save.coins -= cost
                    save.gardenLevel += 1
                    sounds.play(SoundManager.Fx.UPGRADE)
                    showToast("Garden restored!")
                }
            }
            "buy_splash" -> tryBuy({ save.splashPower }, { save.splashPower = it })
            "buy_energy" -> tryBuy({ save.energyReserve }, { save.energyReserve = it })
            "buy_speed" -> tryBuy({ save.collectSpeed }, { save.collectSpeed = it })
            "buy_protect" -> tryBuy({ save.gardenProtection }, { save.gardenProtection = it })
            "claim_harvest" -> claimMission("harvest", 50, save.fruitTotal, 80)
            "claim_guardian" -> claimMission("guardian", 3, save.zonesCleared, 120)
            "claim_master" -> claimMission("master", 4, save.gardenLevel, 150)
            "claim_hunter" -> claimMission("hunter", 40, save.pestsTotal, 100)
            else -> if (id.startsWith("zone_")) {
                val zone = id.removePrefix("zone_").toIntOrNull() ?: return
                if (GameLogic.unlockedZone(save.zonesCleared, zone)) startGame(zone)
            }
        }
    }

    private fun tryBuy(get: () -> Int, set: (Int) -> Unit) {
        val level = get()
        if (level >= 5) return
        val cost = save.upgradeCost(level)
        if (save.coins < cost) {
            showToast("Not enough fruit coins")
            return
        }
        save.coins -= cost
        set(level + 1)
        sounds.play(SoundManager.Fx.UPGRADE)
        showToast("Upgrade purchased!")
    }

    private fun claimMission(id: String, target: Int, value: Int, reward: Int) {
        if (save.isMissionClaimed(id) || value < target) return
        save.claimMission(id)
        save.coins += reward
        sounds.play(SoundManager.Fx.REWARD)
        showToast("Mission reward: +$reward")
    }

    private fun openWeb(kind: String) {
        host.startActivity(Intent(host, WebPageActivity::class.java).putExtra("kind", kind))
    }

    fun release() {
        removeCallbacks(loop)
        loopRunning = false
        sounds.release()
        bitmaps.values.forEach { it.recycle() }
        bitmaps.clear()
        resourceBitmaps.values.forEach { it.recycle() }
        resourceBitmaps.clear()
    }
}
