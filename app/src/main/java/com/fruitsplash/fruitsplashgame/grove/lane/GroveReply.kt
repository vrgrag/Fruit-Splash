package com.fruitsplash.fruitsplashgame.grove.lane

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class GroveRawReply(
    val ok: Boolean = false,
    val url: String? = null,
    val message: String? = null,
    val expires: JsonElement? = null,
)

/**
 * [answered] is independent of [allowed]. A timeout is not a native
 * verdict — only an HTTP response that actually arrived may lock the
 * install into the game.
 */
data class GroveReply(
    val allowed: Boolean,
    val link: String? = null,
    val note: String? = null,
    val ttl: Long? = null,
    val answered: Boolean = true,
) {
    val hasLink: Boolean get() = !link.isNullOrEmpty()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(body: String): GroveReply = try {
            val raw = json.decodeFromString(GroveRawReply.serializer(), body)
            val ttl = raw.expires?.let { e ->
                when (val p = runCatching { e.jsonPrimitive }.getOrNull()) {
                    null -> null
                    else -> p.longOrNull ?: p.content.toLongOrNull()
                }
            }
            GroveReply(
                allowed = raw.ok,
                link = raw.url,
                note = raw.message,
                ttl = ttl,
                answered = true,
            )
        } catch (t: Throwable) {
            closed("parse: ${t.message}")
        }

        fun closed(note: String): GroveReply =
            GroveReply(allowed = false, note = note, answered = true)

        fun mute(note: String): GroveReply =
            GroveReply(allowed = false, note = note, answered = false)
    }
}
