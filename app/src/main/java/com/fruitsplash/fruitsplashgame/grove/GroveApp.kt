package com.fruitsplash.fruitsplashgame.grove

import android.app.Application
import android.util.Log
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.grove.crate.PulpVault
import com.fruitsplash.fruitsplashgame.grove.crate.GroveNote
import com.fruitsplash.fruitsplashgame.grove.vine.VineAf
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process entry. Firebase + AppsFlyer [VineAf.wire] happen here
 * before any Activity exists. [VineAf.kick] waits for connectivity.
 */
class GroveApp : Application() {

    lateinit var tracker: VineAf
        private set

    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseApp.initializeApp(this) }
            .onFailure { warn("Firebase unavailable: ${it.message}") }
        tracker = VineAf(this)
        tracker.wire()
        io.launch {
            GroveNote.fetchToken()?.let { PulpVault(this@GroveApp).writePushToken(it) }
        }
    }

    private fun warn(message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, message)
    }

    private companion object {
        const val TAG = "GroveApp"
    }
}
