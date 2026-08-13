package com.fruitsplash.fruitsplashgame.nectar

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.fruitsplash.fruitsplashgame.nectar.box.NectarBox
import com.fruitsplash.fruitsplashgame.nectar.box.NectarPush
import com.fruitsplash.fruitsplashgame.nectar.face.NectarFill
import com.fruitsplash.fruitsplashgame.nectar.face.NectarGardenTheme
import com.fruitsplash.fruitsplashgame.nectar.face.NectarInviteFace
import com.fruitsplash.fruitsplashgame.nectar.net.NectarPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NectarInviteActivity : ComponentActivity() {

    private lateinit var vault: NectarBox
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
        vault = NectarBox(this)
        targetUrl = intent.getStringExtra(EXTRA_TARGET_URL)
        setContent {
            NectarGardenTheme {
                NectarInviteFace(
                    onAccept = ::onAccept,
                    onSkip = {
                        vault.armInviteCooldown()
                        proceed()
                    },
                )
            }
        }
        NectarFill.apply(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) NectarFill.apply(this)
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
                ?: NectarPush.fetchToken()?.also(vault::writePushToken)
            if (!token.isNullOrEmpty()) {
                runCatching { NectarPost().publishToken(applicationContext, token) }
            }
        }
    }

    private fun proceed() {
        startActivity(
            Intent(this, NectarWebActivity::class.java)
                .putExtra(NectarWebActivity.EXTRA_TARGET_URL, targetUrl)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }

    companion object {
        const val EXTRA_TARGET_URL = "nectar_invite_dest"
    }
}
