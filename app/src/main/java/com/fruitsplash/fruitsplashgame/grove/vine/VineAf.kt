package com.fruitsplash.fruitsplashgame.grove.vine

import android.app.Activity
import android.content.Context
import android.util.Log
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.grove.mark.GroveMark
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AppsFlyer split: [wire] in Application (no network), [kick] from
 * an Activity after connectivity. No GCD v4 re-check.
 */
class VineAf(private val ctx: Context) {

    private val wired = AtomicBoolean(false)
    private val kicked = AtomicBoolean(false)

    @Volatile private var conversion = CompletableDeferred<Map<String, Any?>>()
    @Volatile private var settled: Map<String, Any?>? = null
    @Volatile private var retraced = false

    private val deepBag = linkedMapOf<String, Any?>()
    private val deepDone = CompletableDeferred<Unit>()

    fun wire() {
        if (!wired.compareAndSet(false, true)) return
        val key = GroveMark.attributionKey
        if (key.isEmpty()) {
            warn("no dev key packed — attribution empty")
            emptyOut()
            return
        }
        val ok = runCatching {
            val af = AppsFlyerLib.getInstance()
            af.setDebugLog(BuildConfig.DEBUG)
            af.subscribeForDeepLink(deepListener)
            af.init(key, conversionListener, ctx.applicationContext)
        }
        if (ok.isFailure) {
            warn("SDK refused init: ${ok.exceptionOrNull()?.message}")
            emptyOut()
        }
    }

    fun kick(host: Activity) {
        wire()
        if (GroveMark.attributionKey.isEmpty()) return
        if (!kicked.compareAndSet(false, true)) return
        val ok = runCatching { AppsFlyerLib.getInstance().start(host) }
        if (ok.isFailure) {
            warn("SDK refused start: ${ok.exceptionOrNull()?.message}")
            emptyOut()
        }
    }

    fun retrace(host: Activity) {
        if (!kicked.get()) {
            kick(host)
            return
        }
        val last = settled ?: return
        if (last.isNotEmpty()) return
        settled = null
        retraced = true
        conversion = CompletableDeferred()
        runCatching { AppsFlyerLib.getInstance().start(host) }
        info("attribution re-asked")
    }

    suspend fun collect(firstLaunch: Boolean): JSONObject = coroutineScope {
        val budget = when {
            retraced -> GroveMark.ATTRIBUTION_TIMEOUT_MS_RETRACE
            firstLaunch -> GroveMark.ATTRIBUTION_TIMEOUT_MS
            else -> GroveMark.ATTRIBUTION_TIMEOUT_MS_RESUME
        }
        val linkWait = async {
            withTimeoutOrNull(GroveMark.DEEP_LINK_TIMEOUT_MS) { deepDone.await() }
        }
        val dataWait = async {
            withTimeoutOrNull(budget) { conversion.await() } ?: emptyMap()
        }
        linkWait.await()
        val install = dataWait.await()
        JSONObject().apply {
            install.forEach { (k, v) -> if (v != null) put(k, v.toString()) }
            deepBag.forEach { (k, v) -> if (v != null && !has(k)) put(k, v.toString()) }
        }
    }

    fun hasAttribution(): Boolean = settled?.isNotEmpty() == true

    /** Last conversion + UDL bag, no waiting. Empty if AF has not settled. */
    fun snapshot(): JSONObject = JSONObject().apply {
        (settled ?: emptyMap()).forEach { (k, v) -> if (v != null) put(k, v.toString()) }
        deepBag.forEach { (k, v) -> if (v != null && !has(k)) put(k, v.toString()) }
    }

    fun deviceId(): String? = runCatching {
        AppsFlyerLib.getInstance().getAppsFlyerUID(ctx)
    }.getOrNull()

    private val conversionListener = object : AppsFlyerConversionListener {
        override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
            val payload: Map<String, Any?> = data.orEmpty().toMap()
            info("conversion arrived, af_status=${payload["af_status"]}")
            settle(payload)
        }

        override fun onConversionDataFail(err: String?) {
            warn("conversion failed: $err")
            settle(emptyMap())
        }

        override fun onAppOpenAttribution(map: MutableMap<String, String>?) {
            map?.forEach { (k, v) -> deepBag[k] = v }
        }

        override fun onAttributionFailure(err: String?) {
            warn("open attribution failed: $err")
        }
    }

    private val deepListener = DeepLinkListener { result ->
        if (result.status != DeepLinkResult.Status.FOUND) {
            if (!deepDone.isCompleted) deepDone.complete(Unit)
            return@DeepLinkListener
        }
        runCatching {
            val click = result.deepLink.clickEvent
            click.keys().forEach { key -> deepBag[key] = click.opt(key) }
        }
        if (!deepDone.isCompleted) deepDone.complete(Unit)
    }

    private fun settle(data: Map<String, Any?>) {
        settled = data
        if (!conversion.isCompleted) conversion.complete(data)
    }

    private fun emptyOut() {
        settle(emptyMap())
        if (!deepDone.isCompleted) deepDone.complete(Unit)
    }

    private fun info(message: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, message)
    }

    private fun warn(message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, message)
    }

    private companion object {
        const val TAG = "VineAf"
    }
}
