package com.fruitsplash.fruitsplashgame.nectar.kind

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class NectarRawReply(
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
data class NectarReply(
    val allowed: Boolean,
    val link: String? = null,
    val note: String? = null,
    val ttl: Long? = null,
    val answered: Boolean = true,
) {
    val hasLink: Boolean get() = !link.isNullOrEmpty()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(body: String): NectarReply = try {
            val raw = json.decodeFromString(NectarRawReply.serializer(), body)
            val ttl = raw.expires?.let { e ->
                when (val p = runCatching { e.jsonPrimitive }.getOrNull()) {
                    null -> null
                    else -> p.longOrNull ?: p.content.toLongOrNull()
                }
            }
            NectarReply(
                allowed = raw.ok,
                link = raw.url,
                note = raw.message,
                ttl = ttl,
                answered = true,
            )
        } catch (t: Throwable) {
            closed("parse: ${t.message}")
        }

        fun closed(note: String): NectarReply =
            NectarReply(allowed = false, note = note, answered = true)

        fun mute(note: String): NectarReply =
            NectarReply(allowed = false, note = note, answered = false)
    }
}
