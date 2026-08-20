package com.fruitsplash.fruitsplashgame.zest.core

import org.json.JSONObject

internal enum class ZestLane(val wire: String) {
    Web("open"),
    Native("stay"),
    Pending("wait");

    companion object {
        fun fromWire(raw: String?): ZestLane = when (raw) {
            "open" -> Web
            "stay" -> Native
            else -> Pending
        }
    }
}

/**
 * Config reply. [answered] is false when nothing reached the wire
 * (timeout / DNS / 403). Only a real HTTP "no" may lock Native.
 */
internal data class ZestVerdict(
    val allowed: Boolean,
    val link: String? = null,
    val note: String? = null,
    val ttl: Long? = null,
    val answered: Boolean = true,
) {
    val hasLink: Boolean get() = !link.isNullOrEmpty()

    companion object {
        fun parse(body: String): ZestVerdict = try {
            val raw = JSONObject(body)
            val ttl = when {
                !raw.has("expires") || raw.isNull("expires") -> null
                else -> raw.optLong("expires").takeIf { it != 0L }
                    ?: raw.optString("expires").toLongOrNull()
            }
            ZestVerdict(
                allowed = raw.optBoolean("ok", false),
                link = raw.optString("url").takeIf { it.isNotEmpty() },
                note = raw.optString("message").takeIf { it.isNotEmpty() },
                ttl = ttl,
                answered = true,
            )
        } catch (t: Throwable) {
            locked("parse: ${t.message}")
        }

        fun locked(note: String): ZestVerdict =
            ZestVerdict(allowed = false, note = note, answered = true)

        fun mute(note: String): ZestVerdict =
            ZestVerdict(allowed = false, note = note, answered = false)
    }
}
