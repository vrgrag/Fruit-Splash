package com.fruitsplash.fruitsplashgame.nectar.box

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.R
import com.fruitsplash.fruitsplashgame.nectar.NectarLaunchActivity
import com.fruitsplash.fruitsplashgame.nectar.gate.NectarUrl
import com.fruitsplash.fruitsplashgame.nectar.kind.NectarMode
import com.fruitsplash.fruitsplashgame.nectar.net.NectarChrome
import com.fruitsplash.fruitsplashgame.nectar.net.NectarPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object NectarPush {

    // [FINGERPRINT] Must match AndroidManifest default_notification_channel_id.
    const val CHANNEL_ID = "nectar_garden_notes"
    private const val CHANNEL_NAME = "Garden notes"
    private const val CHANNEL_DESC = "Garden news, bonuses and reminders"
    private const val TAG = "NectarPush"

    const val EXTRA_URL = "nectar_dest"
    const val EXTRA_FROM_PUSH = "nectar_note_tap"

    private val RAW_URL_KEYS = listOf("url", "link", "target_url")

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            },
        )
    }

    suspend fun fetchToken(): String? = suspendCancellableCoroutine { cont ->
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }.onFailure {
            if (cont.isActive) cont.resume(null)
        }
    }

    fun extractUrl(intent: Intent?): String? {
        if (intent == null) return null
        val own = intent.getStringExtra(EXTRA_URL)
        val raw = RAW_URL_KEYS.firstNotNullOfOrNull { intent.getStringExtra(it) }
        return NectarUrl.clean(own ?: raw)
    }

    fun isPushTap(intent: Intent?): Boolean {
        if (intent == null) return false
        if (intent.getBooleanExtra(EXTRA_FROM_PUSH, false)) return true
        return RAW_URL_KEYS.any { intent.hasExtra(it) }
    }

    internal fun buildTapIntent(ctx: Context, url: String?): Intent =
        Intent(ctx, NectarLaunchActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_FROM_PUSH, true)
            if (!url.isNullOrEmpty()) putExtra(EXTRA_URL, url)
        }

    internal fun render(ctx: Context, title: String, body: String, url: String?, imageUrl: String?) {
        ensureChannel(ctx)
        val pending = PendingIntent.getActivity(
            ctx,
            System.currentTimeMillis().toInt(),
            buildTapIntent(ctx, url),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nectar_flame)
            .setColor(ContextCompat.getColor(ctx, R.color.nectar_flame_tint))
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val bitmap = imageUrl?.takeIf { it.isNotEmpty() }?.let(::loadBitmap)
        if (bitmap != null) {
            builder
                .setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as Bitmap?),
                )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }
        val id = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
        val mgr = NotificationManagerCompat.from(ctx)
        if (!mgr.areNotificationsEnabled()) {
            if (BuildConfig.DEBUG) Log.w(TAG, "OS notifications disabled, skip notify")
            return
        }
        runCatching { mgr.notify(id, builder.build()) }
    }

    private fun loadBitmap(url: String): Bitmap? = runCatching {
        NectarChrome.http.newCall(okhttp3.Request.Builder().url(url).get().build())
            .execute().use { resp ->
                val body = resp.body
                if (!resp.isSuccessful || body == null) null
                else body.byteStream().use { BitmapFactory.decodeStream(it) }
            }
    }.getOrElse {
        if (BuildConfig.DEBUG) Log.w(TAG, "big-picture load failed: ${it.message}")
        null
    }
}

class NectarPushService : FirebaseMessagingService() {

    private val bg = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onDestroy() {
        bg.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val vault = NectarBox(applicationContext)
        vault.writePushToken(token)
        if (vault.readMode() == NectarMode.Native) return
        bg.launch {
            runCatching { NectarPost().publishToken(applicationContext, token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val notification = message.notification
        val title = data["title"] ?: notification?.title.orEmpty()
        val body = data["body"] ?: notification?.body.orEmpty()
        if (title.isEmpty() && body.isEmpty()) return

        val url = NectarUrl.clean(data["url"] ?: data["link"] ?: data["target_url"])
        val image = data["image"] ?: notification?.imageUrl?.toString()
        val vault = NectarBox(applicationContext)
        val mode = vault.readMode()

        if (url != null && mode != NectarMode.Native) {
            if (NectarWarm.offer(url)) {
                bg.launch { NectarPush.render(applicationContext, title, body, null, image) }
                return
            }
            vault.stashPushLink(url)
        }
        val tapUrl = if (mode == NectarMode.Native) null else url
        bg.launch { NectarPush.render(applicationContext, title, body, tapUrl, image) }
    }
}
