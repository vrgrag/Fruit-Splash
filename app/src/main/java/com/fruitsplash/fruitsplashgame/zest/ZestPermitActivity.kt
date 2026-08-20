package com.fruitsplash.fruitsplashgame.zest

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.fruitsplash.fruitsplashgame.zest.face.ZestPermitView
import com.fruitsplash.fruitsplashgame.zest.vine.ZestKeep

class ZestPermitActivity : ComponentActivity() {

    private lateinit var vault: ZestKeep
    private var targetUrl: String? = null

    private val askOs = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) vault.markPushAllowed(true) else vault.markPushBlockedByOs()
        proceed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        vault = ZestKeep(this)
        targetUrl = intent.getStringExtra(EXTRA_TARGET_URL)
        setContentView(
            ZestPermitView(
                this,
                onAccept = ::onAccept,
                onSkip = {
                    vault.armInviteCooldown()
                    proceed()
                },
            ),
        )
    }

    private fun onAccept() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            vault.markPushAllowed(true)
            proceed()
            return
        }
        if (vault.osGranted()) {
            vault.markPushAllowed(true)
            proceed()
            return
        }
        vault.markOsAsked()
        askOs.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun proceed() {
        startActivity(
            Intent(this, ZestShellActivity::class.java)
                .putExtra(ZestShellActivity.EXTRA_TARGET_URL, targetUrl)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        window.setDecorFitsSystemWindows(false)
        window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
    }

    companion object {
        const val EXTRA_TARGET_URL = "zest_permit_target"
    }
}
