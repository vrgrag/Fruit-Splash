package com.fruitsplash.fruitsplashgame.nectar

import android.app.Application
import android.util.Log
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.nectar.box.NectarBox
import com.fruitsplash.fruitsplashgame.nectar.box.NectarPush
import com.fruitsplash.fruitsplashgame.nectar.net.NectarAf
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process entry. Firebase + AppsFlyer [NectarAf.wire] happen here
 * before any Activity exists. [NectarAf.kick] waits for connectivity.
 */
class NectarApp : Application() {

    lateinit var tracker: NectarAf
        private set

    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseApp.initializeApp(this) }
            .onFailure { warn("Firebase unavailable: ${it.message}") }
        tracker = NectarAf(this)
        tracker.wire()
        io.launch {
            NectarPush.fetchToken()?.let { NectarBox(this@NectarApp).writePushToken(it) }
        }
    }

    private fun warn(message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, message)
    }

    private companion object {
        const val TAG = "NectarApp"
    }
}
