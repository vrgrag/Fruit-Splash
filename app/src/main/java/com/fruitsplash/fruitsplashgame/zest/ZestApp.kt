package com.fruitsplash.fruitsplashgame.zest

import android.app.Application
import android.util.Log
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.zest.vine.ZestAf
import com.google.firebase.FirebaseApp

class ZestApp : Application() {

    lateinit var tracker: ZestAf
        private set

    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseApp.initializeApp(this) }
            .onFailure { if (BuildConfig.DEBUG) Log.w(TAG, "Firebase unavailable: ${it.message}") }
        tracker = ZestAf(this)
        tracker.prime()
    }

    private companion object {
        const val TAG = "ZestApp"
    }
}
