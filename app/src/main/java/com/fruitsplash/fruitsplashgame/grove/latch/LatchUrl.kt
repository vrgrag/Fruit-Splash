package com.fruitsplash.fruitsplashgame.grove.latch

import android.net.Uri

/**
 * Shape check for every inbound URL (config, push, deep link).
 * Not a host allowlist — partner domains rotate. Only http/https
 * with a host pass; everything else is rejected before loadUrl.
 * Cleartext is promoted to TLS.
 */
object LatchUrl {

    private val WEB = setOf("http", "https")

    fun accepts(raw: String?): Boolean {
        val candidate = raw?.trim().orEmpty()
        if (candidate.isEmpty()) return false
        val parsed = runCatching { Uri.parse(candidate) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase() ?: return false
        if (scheme !in WEB) return false
        return !parsed.host.isNullOrBlank()
    }

    fun clean(raw: String?): String? {
        val candidate = raw?.trim()
        if (!accepts(candidate)) return null
        val parsed = Uri.parse(candidate)
        if (!parsed.scheme.equals("http", ignoreCase = true)) return candidate

        val upgraded = parsed.buildUpon().scheme("https")
        if (parsed.port == 80) {
            val host = parsed.host.orEmpty()
            val userInfo = parsed.encodedUserInfo
            upgraded.encodedAuthority(if (userInfo.isNullOrEmpty()) host else "$userInfo@$host")
        }
        return upgraded.build().toString()
    }
}
