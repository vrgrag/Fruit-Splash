package com.fruitsplash.fruitsplashgame.nectar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.fruitsplash.fruitsplashgame.nectar.box.NectarBox
import com.fruitsplash.fruitsplashgame.nectar.box.NectarPush
import com.fruitsplash.fruitsplashgame.nectar.face.NectarFill
import com.fruitsplash.fruitsplashgame.nectar.face.NectarGardenTheme
import com.fruitsplash.fruitsplashgame.nectar.face.NectarOfflineFace
import com.fruitsplash.fruitsplashgame.nectar.id.NectarBrand
import com.fruitsplash.fruitsplashgame.nectar.net.NectarReach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class NectarOfflineActivity : ComponentActivity() {

    private lateinit var link: NectarReach
    private var resumeUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        link = NectarReach(this)
        resumeUrl = intent.getStringExtra(EXTRA_RESUME_URL)
        setContent {
            NectarGardenTheme {
                var busy by remember { mutableStateOf(false) }
                NectarOfflineFace(
                    busy = busy,
                    onRetry = {
                        if (!busy) {
                            busy = true
                            lifecycleScope.launch {
                                if (!attemptResume()) busy = false
                            }
                        }
                    },
                )
            }
        }
        NectarFill.apply(this)
        lifecycleScope.launch {
            link.live().collect { status ->
                if (status == NectarReach.Kind.Up) attemptResume()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) NectarFill.apply(this)
    }

    private suspend fun attemptResume(): Boolean {
        if (!link.canTalk()) return false
        withTimeoutOrNull(NectarBrand.TOKEN_PREFETCH_MS) {
            NectarPush.fetchToken()
        }?.let { NectarBox(this).writePushToken(it) }
        val saved = resumeUrl
        val next = if (!saved.isNullOrEmpty()) {
            Intent(this, NectarWebActivity::class.java)
                .putExtra(NectarWebActivity.EXTRA_TARGET_URL, saved)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } else {
            Intent(this, NectarLaunchActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(next)
        finish()
        return true
    }

    companion object {
        const val EXTRA_RESUME_URL = "nectar_resume_href"
    }
}
