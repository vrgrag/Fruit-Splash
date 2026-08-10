package com.fruitsplash.fruitsplashgame

import kotlin.math.sqrt

object GameLogic {
    data class Vec(val x: Float, val y: Float)

    fun distance(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    fun normalized(x: Float, y: Float): Vec {
        val length = sqrt(x * x + y * y)
        return if (length < 0.001f) Vec(0f, 0f) else Vec(x / length, y / length)
    }

    fun unlockedZone(zonesCleared: Int, zone: Int): Boolean =
        zone in 0..4 && zone <= zonesCleared.coerceIn(0, 5)

    fun splashRadius(base: Float, splashPower: Int): Float =
        base * (1f + (splashPower - 1) * 0.12f)

    fun splashDamage(distance: Float, radius: Float, splashPower: Int): Int {
        if (distance > radius) return 0
        val falloff = 1f - distance / radius * 0.4f
        return ((2 + splashPower) * falloff).toInt().coerceAtLeast(1)
    }

    fun energyGain(base: Float, energyReserve: Int): Float =
        base * (1f + (energyReserve - 1) * 0.18f)

    fun moveSpeed(base: Float, collectSpeed: Int): Float =
        base * (1f + (collectSpeed - 1) * 0.1f)

    fun damageTaken(base: Float, gardenProtection: Int): Float =
        base * (1f - (gardenProtection - 1) * 0.12f).coerceAtLeast(0.45f)

    fun rewardForZone(zone: Int, rare: Int, restoration: Float): Int =
        50 + zone * 30 + rare * 12 + (restoration / 20f).toInt() * 5

    /** Pickups inside this radius drift towards the guardian. */
    fun magnetRadius(base: Float, collectSpeed: Int): Float =
        base * (1f + (collectSpeed - 1) * 0.24f)

    /** Score needed for a single star in a zone; two and three stars scale from it. */
    fun scoreTarget(zone: Int): Int = 420 + zone * 180

    fun starsForScore(score: Int, target: Int): Int = when {
        score >= target * 2 -> 3
        score >= (target * 1.4f).toInt() -> 2
        else -> 1
    }

    /** Reward bonus paid out per star so replaying a zone stays worthwhile. */
    fun starBonus(stars: Int): Int = stars.coerceIn(0, 3) * 25

    fun waveInterval(zone: Int): Float = (15f - zone * 1.2f).coerceAtLeast(9f)

    fun pestsPerWave(zone: Int, wave: Int): Int = (1 + zone / 2 + wave / 2).coerceAtMost(5)
}
