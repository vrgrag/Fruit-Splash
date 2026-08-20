package com.fruitsplash.fruitsplashgame.grove.mark

import com.fruitsplash.fruitsplashgame.grove.rind.GroveBytes

/**
 * Identity + timing knobs for the gray flow.
 *
 * [FINGERPRINT] PACKAGE_ID / DISPLAY_NAME must match the Gradle
 * applicationId and the launcher label.
 */
object GroveMark {

    const val PACKAGE_ID: String = "com.fruitsplash.fruitsplashgame"
    const val MARKET_ID: String = "com.fruitsplash.fruitsplashgame"
    const val DISPLAY_NAME: String = "Fruit Splash"

    val configEndpoint: String get() = GroveBytes.configEndpoint()
    val attributionKey: String get() = GroveBytes.attributionKey()
    val messagingProject: String get() = GroveBytes.messagingProject()

    /** Skip cooldown — three days, product decision, not a fingerprint. */
    const val INVITE_COOLDOWN_SECONDS: Long = 3L * 24 * 60 * 60

    const val REACH_PROBE_TIMEOUT_MS: Int = 2_100
    const val OFFLINE_DEBOUNCE_MS: Long = 850L
    const val CONNECT_GRACE_MS: Long = 2_800L

    const val ATTRIBUTION_TIMEOUT_MS: Long = 26_000L
    const val ATTRIBUTION_TIMEOUT_MS_RESUME: Long = 9_000L
    const val ATTRIBUTION_TIMEOUT_MS_RETRACE: Long = 7_500L
    const val DEEP_LINK_TIMEOUT_MS: Long = 4_500L
    const val GATE_TIMEOUT_MS: Long = 15_000L

    const val HEARTBEAT_MS: Long = 4_800L
    const val CHAIN_SETTLE_MS: Long = 720L
    const val COVER_MAX_MS: Long = 18_000L
    const val REDIRECT_RETRY_MAX: Int = 6
    const val CHAIN_HOP_SOFT_CAP: Int = 16
    const val RENDERER_RECOVERY_MAX: Int = 3
    const val SAFE_AREA_DELAY_MS: Long = 920L
    const val TOKEN_WAIT_MS: Long = 8_000L
    const val TOKEN_PREFETCH_MS: Long = 3_000L
}
