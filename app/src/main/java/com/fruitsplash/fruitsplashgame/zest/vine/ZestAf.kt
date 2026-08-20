package com.fruitsplash.fruitsplashgame.zest.vine

import android.app.Activity
import android.content.Context
import android.util.Log
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.zest.core.ZestIds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AppsFlyer split: [prime] in Application (no traffic), [ignite] from
 * an Activity after a live network is confirmed.
 */
class ZestAf(private val ctx: Context) {

    private val primed = AtomicBoolean(false)
    private val started = AtomicBoolean(false)

    @Volatile private var conversion = CompletableDeferred<Map<String, Any?>>()
    @Volatile private var settled: Map<String, Any?>? = null
    @Volatile private var retraced = false

    private val deepLinkBag = linkedMapOf<String, Any?>()
    private val deepLink = CompletableDeferred<Unit>()

    fun prime() {
        if (!primed.compareAndSet(false, true)) return
        val key = ZestIds.attributionKey
        if (key.isEmpty()) {
            warn("no attribution key")
            resolveEmpty()
            return
        }
        val wired = runCatching {
            val af = AppsFlyerLib.getInstance()
            af.setDebugLog(BuildConfig.DEBUG)
            af.subscribeForDeepLink(deepLinkListener)
            af.init(key, conversionListener, ctx.applicationContext)
        }
        if (wired.isFailure) {
            warn("init refused: ${wired.exceptionOrNull()?.message}")
            resolveEmpty()
        }
    }

    fun ignite(host: Activity) {
        prime()
        if (ZestIds.attributionKey.isEmpty()) return
        if (!started.compareAndSet(false, true)) return
        val lit = runCatching { AppsFlyerLib.getInstance().start(host) }
        if (lit.isFailure) {
            warn("start refused: ${lit.exceptionOrNull()?.message}")
            resolveEmpty()
        }
    }

    fun retrace(host: Activity) {
        if (!started.get()) {
            ignite(host)
            return
        }
        val last = settled ?: return
        if (last.isNotEmpty()) return
        settled = null
        retraced = true
        conversion = CompletableDeferred()
        runCatching { AppsFlyerLib.getInstance().start(host) }
        info("attribution asked again")
    }

    suspend fun collectBody(firstLaunch: Boolean): JSONObject = coroutineScope {
        val budget = when {
            retraced -> ZestIds.ATTRIBUTION_TIMEOUT_MS_RETRACE
            firstLaunch -> ZestIds.ATTRIBUTION_TIMEOUT_MS
            else -> ZestIds.ATTRIBUTION_TIMEOUT_MS_RESUME
        }
        val linkWait = async {
            withTimeoutOrNull(ZestIds.DEEP_LINK_TIMEOUT_MS) { deepLink.await() }
        }
        val dataWait = async {
            withTimeoutOrNull(budget) { conversion.await() } ?: emptyMap()
        }
        linkWait.await()
        var install = dataWait.await()
        if (firstLaunch) {
            install = secondLookIfOrganic(install)
        }
        JSONObject().apply {
            install.forEach { (k, v) -> if (v != null) put(k, v.toString()) }
            deepLinkBag.forEach { (k, v) -> if (v != null && !has(k)) put(k, v.toString()) }
        }
    }

    fun hasAttributionData(): Boolean = settled?.isNotEmpty() == true

    fun deviceId(): String? = runCatching {
        AppsFlyerLib.getInstance().getAppsFlyerUID(ctx)
    }.getOrNull()

    /**
     * Early Organic from the SDK is often a lie on a OneLink install.
     * Wait, then prefer a non-empty GCD payload when it disagrees.
     */
    private suspend fun secondLookIfOrganic(data: Map<String, Any?>): Map<String, Any?> {
        val status = data["af_status"]?.toString().orEmpty()
        val looksOrganic = data.isEmpty() || status.equals("Organic", ignoreCase = true)
        if (!looksOrganic) return data
        delay(ZestIds.ORGANIC_GCD_PAUSE_MS)
        val gcd = fetchGcd() ?: return data
        if (gcd.isEmpty()) return data
        val gcdStatus = gcd["af_status"]?.toString().orEmpty()
        if (gcdStatus.equals("Non-organic", ignoreCase = true) || data.isEmpty()) {
            info("GCD replaced SDK map, af_status=$gcdStatus")
            settle(gcd)
            return gcd
        }
        return data
    }

    private suspend fun fetchGcd(): Map<String, Any?>? {
        val uid = deviceId() ?: return null
        val key = ZestIds.attributionKey
        if (key.isEmpty()) return null
        val url =
            "https://gcdsdk.appsflyer.com/install_data/v4.0/${ZestIds.PACKAGE_ID}?device_id=$uid"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $key")
            .header("Accept", "application/json")
            .get()
            .build()
        val payload = withTimeoutOrNull(ZestIds.GCD_TIMEOUT_MS) {
            runCatching {
                ZestChrome.http.newCall(request).execute().use { resp ->
                    info("GCD HTTP ${resp.code}")
                    if (!resp.isSuccessful) return@use null
                    resp.body?.string()
                }
            }.getOrNull()
        } ?: return null
        return runCatching {
            val json = JSONObject(payload)
            val out = linkedMapOf<String, Any?>()
            json.keys().forEach { k -> out[k] = json.opt(k) }
            out
        }.getOrNull()
    }

    private val conversionListener = object : AppsFlyerConversionListener {
        override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
            val payload: Map<String, Any?> = data.orEmpty().toMap()
            info("conversion af_status=${payload["af_status"]}")
            settle(payload)
        }

        override fun onConversionDataFail(err: String?) {
            warn("conversion failed: $err")
            settle(emptyMap())
        }

        override fun onAppOpenAttribution(map: MutableMap<String, String>?) {
            map?.forEach { (k, v) -> deepLinkBag[k] = v }
        }

        override fun onAttributionFailure(err: String?) {
            warn("open attribution failed: $err")
        }
    }

    private val deepLinkListener = DeepLinkListener { result ->
        if (result.status != DeepLinkResult.Status.FOUND) {
            if (!deepLink.isCompleted) deepLink.complete(Unit)
            return@DeepLinkListener
        }
        runCatching {
            val click = result.deepLink.clickEvent
            click.keys().forEach { key -> deepLinkBag[key] = click.opt(key) }
        }
        if (!deepLink.isCompleted) deepLink.complete(Unit)
    }

    private fun settle(data: Map<String, Any?>) {
        settled = data
        if (!conversion.isCompleted) conversion.complete(data)
    }

    private fun resolveEmpty() {
        settle(emptyMap())
        if (!deepLink.isCompleted) deepLink.complete(Unit)
    }

    private fun info(message: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, message)
    }

    private fun warn(message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, message)
    }

    private companion object {
        const val TAG = "ZestAf"
    }
}
