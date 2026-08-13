package com.fruitsplash.fruitsplashgame.nectar.net

import android.content.Context
import android.util.Log
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.nectar.NectarApp
import com.fruitsplash.fruitsplashgame.nectar.box.NectarBox
import com.fruitsplash.fruitsplashgame.nectar.box.NectarPush
import com.fruitsplash.fruitsplashgame.nectar.gate.NectarUrl
import com.fruitsplash.fruitsplashgame.nectar.id.NectarBrand
import com.fruitsplash.fruitsplashgame.nectar.kind.NectarMode
import com.fruitsplash.fruitsplashgame.nectar.kind.NectarReply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLDecoder
import java.util.Locale

/** Single POST that asks whether this install sees the WebView. */
class NectarPost {

    suspend fun ask(body: JSONObject): NectarReply = withContext(Dispatchers.IO) {
        val endpoint = NectarBrand.configEndpoint
        if (endpoint.isEmpty()) return@withContext NectarReply.mute("no endpoint packed")

        val serialized = body.toString()
        trace { "request (${body.length()} fields): $serialized" }

        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(serialized.toRequestBody(JSON))
            .build()

        val outcome = withTimeoutOrNull(NectarBrand.GATE_TIMEOUT_MS) {
            runCatching {
                NectarChrome.http.newCall(request).execute().use { resp ->
                    val payload = resp.body?.string().orEmpty()
                    trace { "response ${resp.code}: $payload" }
                    read(resp.code, payload).also {
                        trace { "verdict: allowed=${it.allowed} note=${it.note}" }
                    }
                }
            }.getOrElse { NectarReply.mute("io: ${it.message}") }
        }
        outcome ?: NectarReply.mute("timeout")
    }

    /**
     * Device fields last. [push_token] + [firebase_project_id] are both
     * written or both omitted — never empty strings.
     */
    suspend fun stamp(
        body: JSONObject,
        tracker: NectarAf,
        vault: NectarBox,
        tokenOverride: String? = null,
    ) {
        body.put("af_id", tracker.deviceId().orEmpty())
        body.put("bundle_id", NectarBrand.PACKAGE_ID)
        body.put("os", "Android")
        body.put("store_id", NectarBrand.PACKAGE_ID)
        body.put("locale", localeTag())

        val token = tokenOverride?.takeIf { it.isNotEmpty() }
            ?: vault.readPushToken()
            ?: withTimeoutOrNull(NectarBrand.TOKEN_WAIT_MS) { NectarPush.fetchToken() }
                ?.also(vault::writePushToken)
        val project = NectarBrand.messagingProject
        if (!token.isNullOrEmpty() && project.isNotEmpty()) {
            body.put("push_token", token)
            body.put("firebase_project_id", project)
        }
        stampQa(body)
    }

    /**
     * Token refresh / first-arrival recovery. Re-POSTs the full body so
     * the backend can bind FCM. Does not flip native.
     */
    suspend fun publishToken(ctx: Context, token: String) {
        if (token.isEmpty()) return
        val app = ctx.applicationContext as? NectarApp ?: return
        val vault = NectarBox(ctx)
        if (vault.readMode() == NectarMode.Native) return
        vault.writePushToken(token)
        val body = app.tracker.snapshot()
        stamp(body, app.tracker, vault, token)
        val reply = ask(body)
        if (reply.allowed && reply.hasLink) {
            reply.link?.let(vault::writeCachedLink)
            reply.ttl?.let(vault::writeLinkTtl)
        }
    }

    private fun read(code: Int, payload: String): NectarReply {
        if (code !in 200..299) return NectarReply.closed("http $code")
        if (payload.isBlank()) return NectarReply.closed("empty body")
        val parsed = NectarReply.parse(payload)
        if (!parsed.allowed || !parsed.hasLink) {
            return NectarReply.closed(parsed.note ?: "ok=false")
        }
        if (!NectarUrl.accepts(parsed.link)) {
            trace { "non-web destination dropped" }
            return NectarReply.closed("destination rejected")
        }
        return parsed
    }

    private fun stampQa(body: JSONObject) {
        if (!BuildConfig.DEBUG) return
        val status = BuildConfig.FORCE_AF_STATUS
        if (status.isNotEmpty()) body.put("af_status", status)
        var injected = 0
        for (pair in BuildConfig.FORCE_PARAMS.split('&')) {
            val cut = pair.indexOf('=')
            if (cut <= 0) continue
            val key = pair.substring(0, cut).trim()
            if (key.isEmpty()) continue
            val value = runCatching {
                URLDecoder.decode(pair.substring(cut + 1), "UTF-8")
            }.getOrNull() ?: continue
            body.put(key, value)
            injected++
        }
        if (status.isNotEmpty() || injected > 0) {
            trace { "QA overrides: af_status=$status, $injected campaign fields" }
        }
    }

    private fun localeTag(): String {
        val tag = Locale.getDefault().toLanguageTag()
        if (tag.isEmpty() || tag == "und") return "en"
        return tag.replace('-', '_')
    }

    private inline fun trace(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message())
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        const val TAG = "NectarPost"
    }
}
