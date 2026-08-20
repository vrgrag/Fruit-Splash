package com.fruitsplash.fruitsplashgame.zest.peel

/**
 * Packed credentials. Plaintext must never appear in this file.
 * Rebuild with: python tools/pack_secrets.py
 */
internal object ZestHidden {

    val CONFIG_ENDPOINT_BYTES = intArrayOf(
        23, 178, 16, 215, 107, 159, 239, 140, 46, 245, 216, 238,
        35, 203, 64, 255, 100, 157, 14, 19, 187, 237, 109, 57,
        220, 88, 189, 29, 238, 158, 25, 157, 34, 234, 194,
    )

    val ATTRIBUTION_KEY_BYTES = intArrayOf(
        46, 156, 87, 242, 76, 239, 142, 226, 11, 205, 222, 238,
        39, 142, 119, 201, 112, 220, 51, 34, 216, 214,
    )

    val MESSAGING_PROJECT_BYTES = intArrayOf(
        75, 242, 85, 158, 47, 147, 244, 149, 112, 190, 153, 179,
    )

    val CHROME_VERSION_BYTES = intArrayOf(
        78, 242, 93, 137, 40, 139, 247, 155, 121, 181, 131, 179,
        99,
    )

    val WEBKIT_VERSION_BYTES = intArrayOf(
        74, 245, 83, 137, 43, 147,
    )

    fun configEndpoint(): String = ZestMix.unveil(CONFIG_ENDPOINT_BYTES)
    fun attributionKey(): String = ZestMix.unveil(ATTRIBUTION_KEY_BYTES)
    fun messagingProject(): String = ZestMix.unveil(MESSAGING_PROJECT_BYTES)
    fun chromeVersion(): String = ZestMix.unveil(CHROME_VERSION_BYTES)
    fun webkitVersion(): String = ZestMix.unveil(WEBKIT_VERSION_BYTES)
}
