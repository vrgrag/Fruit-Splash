package com.fruitsplash.fruitsplashgame.zest.vine

import android.content.Context
import android.util.Log
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.zest.core.ZestIds
import com.fruitsplash.fruitsplashgame.zest.core.ZestVerdict
import com.fruitsplash.fruitsplashgame.zest.lock.ZestHref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

internal class ZestPost {

    suspend fun query(body: JSONObject): ZestVerdict = withContext(Dispatchers.IO) {
        val endpoint = ZestIds.configEndpoint
        if (endpoint.isEmpty()) return@withContext ZestVerdict.mute("no endpoint")
        val serialized = body.toString()
        debug { "request (${body.length()} fields): $serialized" }

        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(serialized.toRequestBody(JSON))
            .build()

        val outcome = withTimeoutOrNull(ZestIds.GATE_TIMEOUT_MS) {
            runCatching {
                ZestChrome.http.newCall(request).execute().use { resp ->
                    val payload = resp.body?.string().orEmpty()
                    debug { "response ${resp.code}: $payload" }
                    interpret(resp.code, payload)
                }
            }.getOrElse { ZestVerdict.mute("io: ${it.message}") }
        }
        outcome ?: ZestVerdict.mute("timeout")
    }

    suspend fun decorate(
        body: JSONObject,
        ctx: Context,
        tracker: ZestAf,
        forcedToken: String? = null,
    ) {
        body.put("af_id", tracker.deviceId().orEmpty())
        body.put("bundle_id", ZestIds.PACKAGE_ID)
        body.put("os", "Android")
        body.put("store_id", ZestIds.PACKAGE_ID)
        body.put("locale", localeTag())

        val vault = ZestKeep(ctx)
        val token = forcedToken?.takeIf { it.isNotEmpty() }
            ?: vault.readPushToken()
            ?: withTimeoutOrNull(ZestIds.TOKEN_WAIT_MS) { ZestDrop.fetchToken() }
                ?.also(vault::writePushToken)
        val project = ZestIds.messagingProject
        if (!token.isNullOrEmpty() && project.isNotEmpty()) {
            body.put("push_token", token)
            body.put("firebase_project_id", project)
        }
    }

    /**
     * 404 / ok:false = real "no". Timeout / DNS / 403 / 5xx = silence.
     */
    private fun interpret(code: Int, payload: String): ZestVerdict {
        when (code) {
            404 -> return ZestVerdict.locked("http 404")
            in 200..299 -> Unit
            403, 408, 429 -> return ZestVerdict.mute("http $code")
            in 500..599 -> return ZestVerdict.mute("http $code")
            else -> return ZestVerdict.mute("http $code")
        }
        if (payload.isBlank()) return ZestVerdict.locked("empty body")
        val parsed = ZestVerdict.parse(payload)
        if (!parsed.allowed || !parsed.hasLink) {
            return ZestVerdict.locked(parsed.note ?: "ok=false")
        }
        if (!ZestHref.accepts(parsed.link)) {
            debug { "url rejected by href guard" }
            return ZestVerdict.mute("destination rejected")
        }
        return parsed
    }

    private fun localeTag(): String {
        val tag = Locale.getDefault().toLanguageTag()
        if (tag.isEmpty() || tag == "und") return "en"
        return tag.replace('-', '_')
    }

    private inline fun debug(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message())
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        const val TAG = "ZestPost"
    }
}
