package com.fruitsplash.fruitsplashgame.zest.peel

/**
 * Per-app string hider. Seed and stream length must stay unique to
 * this binary; the Python packer in tools/pack_secrets.py mirrors both.
 */
internal object ZestMix {

    private const val SEED_PHRASE = "Hq8!bN4w_pT6z"
    private const val STREAM_LEN = 23

    private val stream: IntArray by lazy(::buildStream)

    private fun buildStream(): IntArray {
        var h = 5381
        for (c in SEED_PHRASE) {
            h = (h shl 5) + h + c.code
        }
        var state = if (h == 0) 0xA5A5A5A5.toInt() else h
        val out = IntArray(STREAM_LEN)
        for (i in 0 until STREAM_LEN) {
            state = state xor (state shl 7)
            state = state xor (state ushr 9)
            state = state xor (state shl 8)
            out[i] = (state ushr 8) and 0xFF
        }
        return out
    }

    fun unveil(packed: IntArray): String {
        if (packed.isEmpty()) return ""
        val chars = CharArray(packed.size)
        for (i in packed.indices) {
            val byte = (packed[i] xor stream[i % STREAM_LEN] xor ((i * 13) and 0xFF)) and 0xFF
            chars[i] = byte.toChar()
        }
        return String(chars)
    }
}
