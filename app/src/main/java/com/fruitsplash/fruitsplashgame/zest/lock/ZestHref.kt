package com.fruitsplash.fruitsplashgame.zest.lock

import android.net.Uri

/**
 * Accepts only http/https with a host. Does not rewrite the string —
 * the backend URL is loaded as returned.
 */
internal object ZestHref {

    private val WEB = setOf("http", "https")

    fun accepts(raw: String?): Boolean {
        val candidate = raw?.trim().orEmpty()
        if (candidate.isEmpty()) return false
        val parsed = runCatching { Uri.parse(candidate) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase() ?: return false
        if (scheme !in WEB) return false
        return !parsed.host.isNullOrBlank()
    }

    fun pick(raw: String?): String? {
        val candidate = raw?.trim()
        return if (accepts(candidate)) candidate else null
    }
}
