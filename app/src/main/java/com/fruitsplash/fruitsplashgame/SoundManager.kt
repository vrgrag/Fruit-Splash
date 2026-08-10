package com.fruitsplash.fruitsplashgame

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(private val context: Context) {
    enum class Fx {
        BUTTON, FRUIT, COMBO, WAVE, SHIELD, SPLASH, WIN, LOSE,
        PORTAL, CLOSE, OPEN, UNLOCK, PEST, REWARD, UPGRADE
    }

    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = mapOf(
        Fx.BUTTON to R.raw.button_click,
        Fx.FRUIT to R.raw.fruit_collect,
        Fx.COMBO to R.raw.fruit_combo_collect,
        Fx.WAVE to R.raw.fruit_juice_wave,
        Fx.SHIELD to R.raw.fruit_shield_activation,
        Fx.SPLASH to R.raw.fruit_splash_activation,
        Fx.WIN to R.raw.level_complete,
        Fx.LOSE to R.raw.level_fail,
        Fx.PORTAL to R.raw.magic_portal_activation,
        Fx.CLOSE to R.raw.menu_close,
        Fx.OPEN to R.raw.menu_open,
        Fx.UNLOCK to R.raw.new_area_unlock,
        Fx.PEST to R.raw.pest_defeat,
        Fx.REWARD to R.raw.reward_collect,
        Fx.UPGRADE to R.raw.tree_upgrade,
    ).mapValues { pool.load(context, it.value, 1) }

    fun play(fx: Fx, rate: Float = 1f) {
        if (!SaveStore(context).sfxEnabled) return
        pool.play(ids.getValue(fx), 1f, 1f, 1, 0, rate)
    }

    fun release() = pool.release()
}
