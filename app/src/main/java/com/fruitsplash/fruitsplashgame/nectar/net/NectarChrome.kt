package com.fruitsplash.fruitsplashgame.nectar.net

import android.os.Build
import com.fruitsplash.fruitsplashgame.nectar.mix.NectarBytes
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Forged Chrome-on-Android UA shared by OkHttp and the WebView.
 *
 * GAME THEME CATEGORY: crash / casual fruit garden — no appid/appname suffix.
 */
object NectarChrome {

    val value: String by lazy(::compose)

    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(16, TimeUnit.SECONDS)
            .callTimeout(22, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req: Request = chain.request()
                chain.proceed(
                    if (req.header("User-Agent") == null) {
                        req.newBuilder().header("User-Agent", value).build()
                    } else req,
                )
            }
            .build()
    }

    private fun compose(): String {
        val chrome = NectarBytes.chromeVersion().ifEmpty { "149.0.0.0" }
        val webkit = NectarBytes.webkitVersion().ifEmpty { "537.36" }
        val release = Build.VERSION.RELEASE ?: "14"
        val maker = (Build.MANUFACTURER ?: "Google").replaceFirstChar { it.uppercaseChar() }
        val model = Build.MODEL ?: "Pixel 8"
        val tag = (Build.ID ?: "AP4A").take(20)
        return "Mozilla/5.0 (Linux; Android $release; $maker $model Build/$tag) " +
            "AppleWebKit/$webkit (KHTML, like Gecko) " +
            "Chrome/$chrome Mobile Safari/$webkit"
    }
}
