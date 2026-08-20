package com.fruitsplash.fruitsplashgame.grove

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.fruitsplash.fruitsplashgame.grove.crate.PulpVault
import com.fruitsplash.fruitsplashgame.grove.crate.GroveNote
import com.fruitsplash.fruitsplashgame.grove.trellis.GroveFill
import com.fruitsplash.fruitsplashgame.grove.trellis.GroveTheme
import com.fruitsplash.fruitsplashgame.grove.trellis.GroveQuietFace
import com.fruitsplash.fruitsplashgame.grove.mark.GroveMark
import com.fruitsplash.fruitsplashgame.grove.vine.VineReach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class QuietCanopy : ComponentActivity() {

    private lateinit var link: VineReach
    private var resumeUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        link = VineReach(this)
        resumeUrl = intent.getStringExtra(EXTRA_RESUME_URL)
        setContent {
            GroveTheme {
                var busy by remember { mutableStateOf(false) }
                GroveQuietFace(
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
        GroveFill.apply(this)
        lifecycleScope.launch {
            link.live().collect { status ->
                if (status == VineReach.Kind.Up) attemptResume()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) GroveFill.apply(this)
    }

    private suspend fun attemptResume(): Boolean {
        if (!link.canTalk()) return false
        withTimeoutOrNull(GroveMark.TOKEN_PREFETCH_MS) {
            GroveNote.fetchToken()
        }?.let { PulpVault(this).writePushToken(it) }
        val saved = resumeUrl
        val next = if (!saved.isNullOrEmpty()) {
            Intent(this, CanopyShell::class.java)
                .putExtra(CanopyShell.EXTRA_TARGET_URL, saved)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } else {
            Intent(this, HarvestGate::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(next)
        finish()
        return true
    }

    companion object {
        const val EXTRA_RESUME_URL = "jx4_back"
    }
}
