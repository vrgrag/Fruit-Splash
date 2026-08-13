package com.fruitsplash.fruitsplashgame.nectar.box

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fruitsplash.fruitsplashgame.nectar.id.NectarBrand
import com.fruitsplash.fruitsplashgame.nectar.kind.NectarMode

class NectarBox(context: Context) {

    private val app = context.applicationContext

    private val plain: SharedPreferences =
        app.getSharedPreferences(PLAIN_FILE, Context.MODE_PRIVATE)

    private val sealed: SharedPreferences? = runCatching {
        val key = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            app,
            SECRET_FILE,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrNull()

    private val ram = HashMap<String, String?>()

    private fun readSealed(key: String): String? =
        sealed?.getString(key, null) ?: ram[key]

    private fun writeSealed(key: String, value: String?) {
        val store = sealed
        if (store == null) {
            ram[key] = value
            return
        }
        val editor = store.edit()
        if (value.isNullOrEmpty()) editor.remove(key) else editor.putString(key, value)
        editor.apply()
    }

    fun readMode(): NectarMode = NectarMode.fromWire(plain.getString(K_MODE, null))

    fun writeMode(mode: NectarMode) {
        plain.edit().putString(K_MODE, mode.wire).apply()
    }

    fun readCachedLink(): String? = readSealed(K_CACHED_LINK)

    fun writeCachedLink(link: String) = writeSealed(K_CACHED_LINK, link)

    fun readLinkTtl(): Long? =
        if (plain.contains(K_LINK_TTL)) plain.getLong(K_LINK_TTL, 0L) else null

    fun writeLinkTtl(unixSeconds: Long) {
        plain.edit().putLong(K_LINK_TTL, unixSeconds).apply()
    }

    fun hasUsableLink(): Boolean {
        if (readCachedLink().isNullOrEmpty()) return false
        val ttl = readLinkTtl() ?: return true
        return ttl == 0L || nowSeconds() < ttl
    }

    fun stashPushLink(link: String?) = writeSealed(K_PUSH_LINK, link)

    fun takePushLink(): String? {
        val link = readSealed(K_PUSH_LINK) ?: return null
        writeSealed(K_PUSH_LINK, null)
        return link.ifEmpty { null }
    }

    fun readPushToken(): String? = readSealed(K_PUSH_TOKEN)

    fun writePushToken(token: String?) = writeSealed(K_PUSH_TOKEN, token)

    fun isPushAllowed(): Boolean = plain.getBoolean(K_PUSH_ALLOWED, false)

    fun markPushAllowed(value: Boolean) {
        plain.edit().putBoolean(K_PUSH_ALLOWED, value).commit()
    }

    fun isPushBlockedByOs(): Boolean = plain.getBoolean(K_PUSH_BLOCKED_OS, false)

    fun markPushBlockedByOs() {
        plain.edit().putBoolean(K_PUSH_BLOCKED_OS, true).commit()
    }

    fun wasOsAsked(): Boolean = plain.getBoolean(K_PUSH_OS_ASKED, false)

    fun markOsAsked() {
        plain.edit().putBoolean(K_PUSH_OS_ASKED, true).commit()
    }

    /**
     * Skip hides the invite until local midnight three calendar days later.
     * A wall-clock +72h cooldown misses the usual QA move: set the date
     * +3 days at an earlier time of day.
     */
    fun armInviteCooldown() {
        val zone = java.time.ZoneId.systemDefault()
        val until = java.time.LocalDate.now(zone)
            .plusDays(NectarBrand.INVITE_COOLDOWN_SECONDS / 86_400L)
            .atStartOfDay(zone)
            .toEpochSecond()
        plain.edit().putLong(K_INVITE_UNTIL, until).commit()
    }

    fun shouldOfferInvite(host: Activity): Boolean {
        if (isPushAllowed() || isPushBlockedByOs()) return false
        if (osGranted()) {
            markPushAllowed(true)
            return false
        }
        if (osRefusedForGood(host)) {
            markPushBlockedByOs()
            return false
        }
        val until = if (plain.contains(K_INVITE_UNTIL)) plain.getLong(K_INVITE_UNTIL, 0L) else 0L
        return nowSeconds() >= until
    }

    /**
     * True only when API 33+ has granted POST_NOTIFICATIONS.
     * Below 33 there is no runtime permission — the promo screen is
     * shown once as opt-in/opt-out, not skipped.
     */
    fun osGranted(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun osRefusedForGood(host: Activity): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wasOsAsked() &&
                !host.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            !NotificationManagerCompat.from(host).areNotificationsEnabled()
        }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000L

    private companion object {
        const val PLAIN_FILE = "pulp_plain_jar"
        const val SECRET_FILE = "pulp_sealed_jar"
        const val K_MODE = "lane_flag_q"
        const val K_CACHED_LINK = "syrup_blob"
        const val K_LINK_TTL = "syrup_until"
        const val K_PUSH_LINK = "pip_blob"
        const val K_PUSH_TOKEN = "pip_id"
        const val K_INVITE_UNTIL = "ask_after"
        const val K_PUSH_ALLOWED = "pip_ok"
        const val K_PUSH_BLOCKED_OS = "pip_shut"
        const val K_PUSH_OS_ASKED = "pip_asked"
    }
}
