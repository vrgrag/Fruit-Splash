package com.fruitsplash.fruitsplashgame.grove

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.SplashActivity
import com.fruitsplash.fruitsplashgame.grove.crate.PulpVault
import com.fruitsplash.fruitsplashgame.grove.crate.GroveNote
import com.fruitsplash.fruitsplashgame.grove.crate.WarmPip
import com.fruitsplash.fruitsplashgame.grove.trellis.GroveFill
import com.fruitsplash.fruitsplashgame.grove.trellis.GroveTheme
import com.fruitsplash.fruitsplashgame.grove.trellis.GroveSplashFace
import com.fruitsplash.fruitsplashgame.grove.latch.LatchUrl
import com.fruitsplash.fruitsplashgame.grove.mark.GroveMark
import com.fruitsplash.fruitsplashgame.grove.lane.GroveLane
import com.fruitsplash.fruitsplashgame.grove.lane.GroveReply
import com.fruitsplash.fruitsplashgame.grove.vine.VineAf
import com.fruitsplash.fruitsplashgame.grove.vine.VinePost
import com.fruitsplash.fruitsplashgame.grove.vine.VineReach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Launcher + router.
 *
 * Pending + no radio → offline on frame one, nothing persisted.
 * Native is written only when the endpoint answered AND attribution
 * was non-empty. Web returning users never fall into the game.
 * White path opens [SplashActivity] so the garden still warms up offline.
 */
class HarvestGate : ComponentActivity() {

    private lateinit var vault: PulpVault
    private lateinit var link: VineReach
    private lateinit var gate: VinePost

    private val tracker: VineAf
        get() = (application as GroveApp).tracker

    private var progress by mutableFloatStateOf(0.04f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        vault = PulpVault(this)
        link = VineReach(this)
        gate = VinePost()
        GroveNote.ensureChannel(this)

        val mode = resolveMode()
        val pushUrl = GroveNote.extractUrl(intent)

        if (pushUrl != null && mode == GroveLane.Web && WarmPip.offer(pushUrl)) {
            info("warm push handed to live shell")
            finish()
            return
        }

        if (mode == GroveLane.Pending && pushUrl == null && !link.hasRadio()) {
            info("first run with no radio, offline first frame")
            startActivity(Intent(this, QuietCanopy::class.java))
            finish()
            return
        }

        if (pushUrl != null && mode != GroveLane.Native) vault.stashPushLink(pushUrl)

        setContent {
            GroveTheme {
                GroveSplashFace(progress = progress)
            }
        }
        GroveFill.apply(this)
        lifecycleScope.launch { route(mode) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) GroveFill.apply(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val pushUrl = GroveNote.extractUrl(intent) ?: return
        when (vault.readMode()) {
            GroveLane.Native -> info("push tap while native, garden stays")
            GroveLane.Web -> {
                if (WarmPip.offer(pushUrl)) finish()
                else openShell(pushUrl)
            }
            GroveLane.Pending -> vault.stashPushLink(pushUrl)
        }
    }

    private fun resolveMode(): GroveLane {
        val stored = vault.readMode()
        if (stored != GroveLane.Native) return stored
        if (BuildConfig.DEBUG && !BuildConfig.STICKY_VERDICT) {
            vault.writeMode(GroveLane.Pending)
            info("native verdict cleared for QA")
            return GroveLane.Pending
        }
        return stored
    }

    private suspend fun route(mode: GroveLane) {
        if (BuildConfig.DEBUG && BuildConfig.PROBE_LINK.isNotEmpty()) {
            info("probe link configured, shell forced")
            progress = 1f
            openShell(BuildConfig.PROBE_LINK)
            return
        }
        if (mode == GroveLane.Native) {
            openGame()
            return
        }
        progress = 0.14f
        if (!awaitConnection(mode)) return
        when (mode) {
            GroveLane.Web -> resolveReturning()
            else -> resolveFirstLaunch()
        }
    }

    private suspend fun awaitConnection(mode: GroveLane): Boolean {
        if (link.hasRadio()) return true
        val arrived = withTimeoutOrNull(GroveMark.CONNECT_GRACE_MS) {
            link.live().first { it == VineReach.Kind.Up }
        } != null
        if (arrived) return true
        val resume = if (mode == GroveLane.Web && vault.hasUsableLink()) {
            vault.readCachedLink()
        } else {
            null
        }
        startActivity(
            Intent(this, QuietCanopy::class.java).apply {
                if (!resume.isNullOrEmpty()) {
                    putExtra(QuietCanopy.EXTRA_RESUME_URL, resume)
                }
            },
        )
        finish()
        return false
    }

    private suspend fun resolveFirstLaunch() {
        tracker.kick(this)
        tracker.retrace(this)
        progress = 0.38f
        val reply = askBackend(firstLaunch = true)
        progress = 0.94f
        if (reply.allowed && reply.hasLink) {
            commitWeb(reply)
            progress = 1f
            openShell(reply.link)
            return
        }
        when {
            !reply.answered ->
                info("endpoint unreachable, garden for now, decision left open")
            !tracker.hasAttribution() ->
                info("nothing behind the answer, garden for now, decision left open")
            !BuildConfig.STICKY_VERDICT ->
                info("backend ruled native; not persisted (QA)")
            else -> {
                vault.writeMode(GroveLane.Native)
                info("backend ruled native (${reply.note})")
            }
        }
        progress = 1f
        openGame()
    }

    private suspend fun resolveReturning() {
        val pushed = LatchUrl.clean(vault.takePushLink())
        if (pushed != null) {
            openShell(pushed)
            return
        }
        tracker.kick(this)
        tracker.retrace(this)
        progress = 0.48f
        val reply = askBackend(firstLaunch = false)
        progress = 0.94f
        val cached = vault.readCachedLink()
        when {
            reply.allowed && reply.hasLink -> {
                commitWeb(reply)
                progress = 1f
                openShell(reply.link)
            }
            !cached.isNullOrEmpty() -> {
                progress = 1f
                openShell(cached)
            }
            else -> {
                startActivity(Intent(this, QuietCanopy::class.java))
                finish()
            }
        }
    }

    private suspend fun askBackend(firstLaunch: Boolean): GroveReply {
        val body = withContext(Dispatchers.IO) {
            tracker.collect(firstLaunch).also { gate.stamp(it, tracker, vault) }
        }
        progress = if (firstLaunch) 0.68f else 0.74f
        return gate.ask(body)
    }

    private fun commitWeb(reply: GroveReply) {
        vault.writeMode(GroveLane.Web)
        reply.link?.let(vault::writeCachedLink)
        vault.writeLinkTtl(reply.ttl ?: 0L)
    }

    private fun openShell(url: String?) {
        val target = LatchUrl.clean(url)
        if (target == null) {
            openGame()
            return
        }
        val next = if (vault.shouldOfferInvite(this)) {
            Intent(this, TrellisAsk::class.java)
                .putExtra(TrellisAsk.EXTRA_TARGET_URL, target)
        } else {
            Intent(this, CanopyShell::class.java)
                .putExtra(CanopyShell.EXTRA_TARGET_URL, target)
        }
        startActivity(next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun openGame() {
        startActivity(
            Intent(this, SplashActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    private fun info(message: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, message)
    }

    private companion object {
        const val TAG = "HarvestGate"
    }
}
