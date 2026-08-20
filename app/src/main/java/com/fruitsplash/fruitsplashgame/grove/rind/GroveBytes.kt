package com.fruitsplash.fruitsplashgame.grove.rind

/**
 * Encoded blobs for the gray flow. Plaintext must never appear here.
 *
 * Fill via `python tools/pack_secrets.py` after editing the packer
 * table and mirroring the seed in [GroveXor].
 *
 * AppsFlyer key and Firebase project number are packed; plaintext
 * never appears in this file.
 */
internal object GroveBytes {

    private val CONFIG_ENDPOINT_BYTES = intArrayOf(
        49, 50, 129, 157, 109, 37, 137, 63, 138, 251, 192, 170, 165, 244, 222, 13,
        104, 89, 125, 3, 73, 210, 191, 31, 92, 201, 247, 27, 84, 248, 170, 57, 44,
        242, 134,
    )
    private val ATTRIBUTION_KEY_BYTES = intArrayOf(
        8, 28, 198, 184, 74, 85, 232, 81, 175, 195, 198, 170, 161, 177, 233, 59,
        124, 24, 64, 50, 42, 233,
    )
    private val MESSAGING_PROJECT_BYTES = intArrayOf(
        109, 114, 196, 212, 41, 41, 146, 38, 212, 176, 129, 247,
    )
    private val CHROME_VERSION_BYTES = intArrayOf(
        104, 114, 204, 195, 46, 49, 145, 39, 216, 187, 155, 242, 224, 191,
    )
    private val WEBKIT_VERSION_BYTES = intArrayOf(108, 117, 194, 195, 45, 41)

    fun configEndpoint(): String = GroveXor.reveal(CONFIG_ENDPOINT_BYTES)
    fun attributionKey(): String = GroveXor.reveal(ATTRIBUTION_KEY_BYTES)
    fun messagingProject(): String = GroveXor.reveal(MESSAGING_PROJECT_BYTES)
    fun chromeVersion(): String = GroveXor.reveal(CHROME_VERSION_BYTES)
    fun webkitVersion(): String = GroveXor.reveal(WEBKIT_VERSION_BYTES)
}
