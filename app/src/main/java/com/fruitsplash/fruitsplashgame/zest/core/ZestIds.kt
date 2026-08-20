package com.fruitsplash.fruitsplashgame.zest.core

import com.fruitsplash.fruitsplashgame.zest.peel.ZestHidden

internal object ZestIds {

    const val PACKAGE_ID: String = "com.fruitsplash.fruitsplashgame"
    const val DISPLAY_NAME: String = "Fruit Splash"

    val configEndpoint: String get() = ZestHidden.configEndpoint()
    val attributionKey: String get() = ZestHidden.attributionKey()
    val messagingProject: String get() = ZestHidden.messagingProject()

    const val INVITE_COOLDOWN_SECONDS: Long = 3L * 24L * 60L * 60L

    const val REACH_PROBE_TIMEOUT_MS: Int = 2_200
    const val OFFLINE_DEBOUNCE_MS: Long = 820L
    const val CONNECT_GRACE_MS: Long = 3_000L

    const val ATTRIBUTION_TIMEOUT_MS: Long = 30_000L
    const val ATTRIBUTION_TIMEOUT_MS_RESUME: Long = 10_000L
    const val ATTRIBUTION_TIMEOUT_MS_RETRACE: Long = 8_000L
    const val DEEP_LINK_TIMEOUT_MS: Long = 5_000L
    const val GATE_TIMEOUT_MS: Long = 15_000L
    const val TOKEN_WAIT_MS: Long = 8_500L
    const val ORGANIC_GCD_PAUSE_MS: Long = 5_000L
    const val GCD_TIMEOUT_MS: Long = 8_000L

    const val HEARTBEAT_MS: Long = 6_000L
    const val CHAIN_SETTLE_MS: Long = 720L
    const val COVER_MAX_MS: Long = 18_000L
    const val REDIRECT_RETRY_MAX: Int = 6
    const val RENDERER_RECOVERY_MAX: Int = 3
    const val SAFE_AREA_DELAY_MS: Long = 920L
}
