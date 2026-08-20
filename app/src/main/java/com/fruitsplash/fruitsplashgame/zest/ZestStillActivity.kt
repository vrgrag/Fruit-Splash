package com.fruitsplash.fruitsplashgame.zest

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.fruitsplash.fruitsplashgame.zest.face.ZestStillView
import com.fruitsplash.fruitsplashgame.zest.vine.ZestDrop
import com.fruitsplash.fruitsplashgame.zest.vine.ZestKeep
import com.fruitsplash.fruitsplashgame.zest.vine.ZestReach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ZestStillActivity : ComponentActivity() {

    private lateinit var link: ZestReach
    private lateinit var stillView: ZestStillView
    private var resumeUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        link = ZestReach(this)
        resumeUrl = intent.getStringExtra(EXTRA_RESUME_URL)
        stillView = ZestStillView(this) {
            if (!stillView.busy) {
                stillView.busy = true
                lifecycleScope.launch {
                    if (!attemptResume()) stillView.busy = false
                }
            }
        }
        setContentView(stillView)
        lifecycleScope.launch {
            link.statusStream().collect { status ->
                if (status == ZestReach.Status.Online) attemptResume()
            }
        }
    }

    private suspend fun attemptResume(): Boolean {
        if (!link.isReachable()) return false
        withTimeoutOrNull(3_000L) { ZestDrop.fetchToken() }
            ?.also { ZestKeep(this).writePushToken(it) }
        val saved = resumeUrl
        val next = if (!saved.isNullOrEmpty()) {
            Intent(this, ZestShellActivity::class.java)
                .putExtra(ZestShellActivity.EXTRA_TARGET_URL, saved)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } else {
            Intent(this, ZestGateActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(next)
        finish()
        return true
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
        const val EXTRA_RESUME_URL = "zest_resume_url"
    }
}
