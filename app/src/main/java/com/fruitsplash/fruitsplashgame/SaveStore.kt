package com.fruitsplash.fruitsplashgame

import android.content.Context

/** Offline progression persisted in SharedPreferences. */
class SaveStore(context: Context) {
    private val prefs = context.getSharedPreferences("fruit_splash_save", Context.MODE_PRIVATE)

    var coins: Int
        get() = prefs.getInt(KEY_COINS, 0)
        set(value) = prefs.edit().putInt(KEY_COINS, value.coerceAtLeast(0)).apply()

    var zonesCleared: Int
        get() = prefs.getInt(KEY_ZONES, 0)
        set(value) = prefs.edit().putInt(KEY_ZONES, value.coerceIn(0, 5)).apply()

    var gardenLevel: Int
        get() = prefs.getInt(KEY_GARDEN, 0)
        set(value) = prefs.edit().putInt(KEY_GARDEN, value.coerceIn(0, 4)).apply()

    var splashPower: Int
        get() = prefs.getInt(KEY_SPLASH, 1)
        set(value) = prefs.edit().putInt(KEY_SPLASH, value.coerceIn(1, 5)).apply()

    var energyReserve: Int
        get() = prefs.getInt(KEY_ENERGY, 1)
        set(value) = prefs.edit().putInt(KEY_ENERGY, value.coerceIn(1, 5)).apply()

    var collectSpeed: Int
        get() = prefs.getInt(KEY_SPEED, 1)
        set(value) = prefs.edit().putInt(KEY_SPEED, value.coerceIn(1, 5)).apply()

    var gardenProtection: Int
        get() = prefs.getInt(KEY_PROTECT, 1)
        set(value) = prefs.edit().putInt(KEY_PROTECT, value.coerceIn(1, 5)).apply()

    var fruitTotal: Int
        get() = prefs.getInt(KEY_FRUIT_TOTAL, 0)
        set(value) = prefs.edit().putInt(KEY_FRUIT_TOTAL, value.coerceAtLeast(0)).apply()

    var pestsTotal: Int
        get() = prefs.getInt(KEY_PESTS_TOTAL, 0)
        set(value) = prefs.edit().putInt(KEY_PESTS_TOTAL, value.coerceAtLeast(0)).apply()

    var sfxEnabled: Boolean
        get() = prefs.getBoolean(KEY_SFX, true)
        set(value) = prefs.edit().putBoolean(KEY_SFX, value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    fun isFruitUnlocked(id: String): Boolean = prefs.getBoolean(KEY_FRUIT_PREFIX + id, false)

    fun unlockFruit(id: String) {
        if (!isFruitUnlocked(id)) prefs.edit().putBoolean(KEY_FRUIT_PREFIX + id, true).apply()
    }

    fun unlockedFruitCount(ids: Collection<String>): Int = ids.count { isFruitUnlocked(it) }

    var bestScore: Int
        get() = prefs.getInt(KEY_BEST_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_BEST_SCORE, value.coerceAtLeast(0)).apply()

    fun zoneStars(zone: Int): Int = prefs.getInt(KEY_STARS_PREFIX + zone, 0)

    fun setZoneStars(zone: Int, stars: Int) {
        if (stars > zoneStars(zone)) {
            prefs.edit().putInt(KEY_STARS_PREFIX + zone, stars.coerceIn(0, 3)).apply()
        }
    }

    fun totalStars(): Int = (0..4).sumOf { zoneStars(it) }

    fun isMissionClaimed(id: String): Boolean = prefs.getBoolean(KEY_MISSION_PREFIX + id, false)

    fun claimMission(id: String) {
        prefs.edit().putBoolean(KEY_MISSION_PREFIX + id, true).apply()
    }

    fun upgradeCost(level: Int): Int = 80 + (level - 1) * 70

    companion object {
        private const val KEY_COINS = "coins"
        private const val KEY_ZONES = "zones"
        private const val KEY_GARDEN = "garden"
        private const val KEY_SPLASH = "splash"
        private const val KEY_ENERGY = "energy"
        private const val KEY_SPEED = "speed"
        private const val KEY_PROTECT = "protect"
        private const val KEY_FRUIT_TOTAL = "fruit_total"
        private const val KEY_PESTS_TOTAL = "pests_total"
        private const val KEY_SFX = "sfx"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_FRUIT_PREFIX = "fruit_"
        private const val KEY_STARS_PREFIX = "stars_"
        private const val KEY_MISSION_PREFIX = "mission_"
    }
}
