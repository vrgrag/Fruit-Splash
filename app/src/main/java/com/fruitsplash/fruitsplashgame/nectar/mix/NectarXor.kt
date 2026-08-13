package com.fruitsplash.fruitsplashgame.nectar.mix

/**
 * DJB2 + multiply-with-carry keystream used to keep the config
 * endpoint, AppsFlyer key, Firebase project id and Chrome fragments
 * out of the APK string table.
 *
 *  1. [SEED_PHRASE] is hashed with DJB2.
 *  2. That hash seeds an MWC generator of [STREAM_LEN] bytes.
 *  3. Each output byte is `input XOR stream[i mod len] XOR ((i * 13) AND 0xFF)`.
 *
 * Symmetric with `tools/pack_secrets.py`.
 *
 * [FINGERPRINT] Change BOTH constants on every fork, then re-pack.
 */
object NectarXor {

    // [FINGERPRINT]
    private const val SEED_PHRASE = "kP9#wL2mQx7!"

    // [FINGERPRINT] 16..48, unique per project.
    private const val STREAM_LEN = 23

    private val stream: IntArray by lazy(::buildStream)

    private fun buildStream(): IntArray {
        var h = 5381L
        for (c in SEED_PHRASE.toCharArray()) {
            h = ((h shl 5) + h + c.code) and 0xFFFFFFFFL
        }
        var state = if (h == 0L) 0xA5A5A5A5L else h
        val out = IntArray(STREAM_LEN)
        for (i in 0 until STREAM_LEN) {
            state = (18000L * (state and 0xFFFF) + (state ushr 16)) and 0xFFFFFFFFL
            out[i] = ((state ushr 8) and 0xFF).toInt()
        }
        return out
    }

    fun reveal(packed: IntArray): String {
        if (packed.isEmpty()) return ""
        val chars = CharArray(packed.size)
        for (i in packed.indices) {
            val byte = (packed[i] xor stream[i % STREAM_LEN] xor ((i * 13) and 0xFF)) and 0xFF
            chars[i] = byte.toChar()
        }
        return String(chars)
    }
}
