package com.fruitsplash.fruitsplashgame.grove.vine

import android.content.Context
import android.util.Log
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.grove.GroveApp
import com.fruitsplash.fruitsplashgame.grove.crate.PulpVault
import com.fruitsplash.fruitsplashgame.grove.crate.GroveNote
import com.fruitsplash.fruitsplashgame.grove.latch.LatchUrl
import com.fruitsplash.fruitsplashgame.grove.mark.GroveMark
import com.fruitsplash.fruitsplashgame.grove.lane.GroveLane
import com.fruitsplash.fruitsplashgame.grove.lane.GroveReply
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
class VinePost {

    suspend fun ask(body: JSONObject): GroveReply = withContext(Dispatchers.IO) {
        val endpoint = GroveMark.configEndpoint
        if (endpoint.isEmpty()) return@withContext GroveReply.mute("no endpoint packed")

        val serialized = body.toString()
        trace { "request (${body.length()} fields): $serialized" }

        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(serialized.toRequestBody(JSON))
            .build()

        val outcome = withTimeoutOrNull(GroveMark.GATE_TIMEOUT_MS) {
            runCatching {
                VineChrome.http.newCall(request).execute().use { resp ->
                    val payload = resp.body?.string().orEmpty()
                    trace { "response ${resp.code}: $payload" }
                    read(resp.code, payload).also {
                        trace { "verdict: allowed=${it.allowed} note=${it.note}" }
                    }
                }
            }.getOrElse { GroveReply.mute("io: ${it.message}") }
        }
        outcome ?: GroveReply.mute("timeout")
    }

    /**
     * Device fields last. [push_token] + [firebase_project_id] are both
     * written or both omitted — never empty strings.
     */
    suspend fun stamp(
        body: JSONObject,
        tracker: VineAf,
        vault: PulpVault,
        tokenOverride: String? = null,
    ) {
        body.put("af_id", tracker.deviceId().orEmpty())
        body.put("bundle_id", GroveMark.PACKAGE_ID)
        body.put("os", "Android")
        body.put("store_id", GroveMark.PACKAGE_ID)
        body.put("locale", localeTag())

        val token = tokenOverride?.takeIf { it.isNotEmpty() }
            ?: vault.readPushToken()
            ?: withTimeoutOrNull(GroveMark.TOKEN_WAIT_MS) { GroveNote.fetchToken() }
                ?.also(vault::writePushToken)
        val project = GroveMark.messagingProject
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
        val app = ctx.applicationContext as? GroveApp ?: return
        val vault = PulpVault(ctx)
        if (vault.readMode() == GroveLane.Native) return
        vault.writePushToken(token)
        val body = app.tracker.snapshot()
        stamp(body, app.tracker, vault, token)
        val reply = ask(body)
        if (reply.allowed && reply.hasLink) {
            reply.link?.let(vault::writeCachedLink)
            reply.ttl?.let(vault::writeLinkTtl)
        }
    }

    private fun read(code: Int, payload: String): GroveReply {
        if (code !in 200..299) return GroveReply.closed("http $code")
        if (payload.isBlank()) return GroveReply.closed("empty body")
        val parsed = GroveReply.parse(payload)
        if (!parsed.allowed || !parsed.hasLink) {
            return GroveReply.closed(parsed.note ?: "ok=false")
        }
        if (!LatchUrl.accepts(parsed.link)) {
            trace { "non-web destination dropped" }
            return GroveReply.closed("destination rejected")
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
        const val TAG = "VinePost"
    }
}
