package com.fruitsplash.fruitsplashgame.grove

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.fruitsplash.fruitsplashgame.grove.crate.PulpVault
import com.fruitsplash.fruitsplashgame.grove.crate.GroveNote
import com.fruitsplash.fruitsplashgame.grove.trellis.GroveFill
import com.fruitsplash.fruitsplashgame.grove.trellis.GroveTheme
import com.fruitsplash.fruitsplashgame.grove.trellis.GroveAskFace
import com.fruitsplash.fruitsplashgame.grove.vine.VinePost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrellisAsk : ComponentActivity() {

    private lateinit var vault: PulpVault
    private var targetUrl: String? = null

    private val askOs = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            vault.markPushAllowed(true)
            refreshTokenQuietly()
        } else {
            vault.markPushBlockedByOs()
        }
        proceed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = PulpVault(this)
        targetUrl = intent.getStringExtra(EXTRA_TARGET_URL)
        setContent {
            GroveTheme {
                GroveAskFace(
                    onAccept = ::onAccept,
                    onSkip = {
                        vault.armInviteCooldown()
                        proceed()
                    },
                )
            }
        }
        GroveFill.apply(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) GroveFill.apply(this)
    }

    private fun onAccept() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            vault.markPushAllowed(true)
            refreshTokenQuietly()
            proceed()
            return
        }
        if (vault.osGranted()) {
            vault.markPushAllowed(true)
            refreshTokenQuietly()
            proceed()
            return
        }
        vault.markOsAsked()
        askOs.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun refreshTokenQuietly() {
        lifecycleScope.launch(Dispatchers.IO) {
            val token = vault.readPushToken()
                ?: GroveNote.fetchToken()?.also(vault::writePushToken)
            if (!token.isNullOrEmpty()) {
                runCatching { VinePost().publishToken(applicationContext, token) }
            }
        }
    }

    private fun proceed() {
        startActivity(
            Intent(this, CanopyShell::class.java)
                .putExtra(CanopyShell.EXTRA_TARGET_URL, targetUrl)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }

    companion object {
        const val EXTRA_TARGET_URL = "jx4_ask"
    }
}
