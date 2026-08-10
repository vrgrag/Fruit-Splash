package com.fruitsplash.fruitsplashgame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {
    @Test
    fun normalizationHasUnitLength() {
        val v = GameLogic.normalized(3f, 4f)
        assertEquals(0.6f, v.x, 0.001f)
        assertEquals(0.8f, v.y, 0.001f)
    }

    @Test
    fun zoneProgressionIsSequential() {
        assertTrue(GameLogic.unlockedZone(2, 2))
        assertFalse(GameLogic.unlockedZone(2, 3))
        assertTrue(GameLogic.unlockedZone(0, 0))
    }

    @Test
    fun splashFallsOffAndStops() {
        assertTrue(GameLogic.splashDamage(10f, 100f, 3) > GameLogic.splashDamage(90f, 100f, 3))
        assertEquals(0, GameLogic.splashDamage(101f, 100f, 3))
    }

    @Test
    fun upgradesImproveValues() {
        assertTrue(GameLogic.splashRadius(100f, 3) > GameLogic.splashRadius(100f, 1))
        assertTrue(GameLogic.energyGain(10f, 4) > GameLogic.energyGain(10f, 1))
        assertTrue(GameLogic.damageTaken(10f, 4) < GameLogic.damageTaken(10f, 1))
        assertTrue(GameLogic.rewardForZone(2, 2, 80f) > GameLogic.rewardForZone(0, 0, 0f))
    }
}
