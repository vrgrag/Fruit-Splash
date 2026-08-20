package com.fruitsplash.fruitsplashgame.zest.vine

import android.os.Build
import com.fruitsplash.fruitsplashgame.zest.peel.ZestHidden
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * One Chrome-on-Android UA for config, GCD and WebView.
 * Chrome 149+, no `; wv`, no okhttp/Dart, no appid/appname.
 */
internal object ZestChrome {

    val value: String by lazy(::build)

    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request: Request = chain.request()
                chain.proceed(
                    if (request.header("User-Agent") == null) {
                        request.newBuilder().header("User-Agent", value).build()
                    } else request,
                )
            }
            .build()
    }

    private fun build(): String {
        val chrome = ZestHidden.chromeVersion().ifEmpty { "149.0.7812.44" }
        val webkit = ZestHidden.webkitVersion().ifEmpty { "537.36" }
        val release = Build.VERSION.RELEASE ?: "14"
        val manufacturer = (Build.MANUFACTURER ?: "Google").replaceFirstChar { it.uppercaseChar() }
        val model = Build.MODEL ?: "Pixel 8"
        val buildTag = (Build.ID ?: "AP2A").take(18)
        return "Mozilla/5.0 (Linux; Android $release; $manufacturer $model Build/$buildTag) " +
            "AppleWebKit/$webkit (KHTML, like Gecko) " +
            "Chrome/$chrome Mobile Safari/$webkit"
    }
}
