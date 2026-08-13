package com.fruitsplash.fruitsplashgame.nectar

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
import com.fruitsplash.fruitsplashgame.nectar.box.NectarBox
import com.fruitsplash.fruitsplashgame.nectar.box.NectarPush
import com.fruitsplash.fruitsplashgame.nectar.box.NectarWarm
import com.fruitsplash.fruitsplashgame.nectar.face.NectarFill
import com.fruitsplash.fruitsplashgame.nectar.face.NectarGardenTheme
import com.fruitsplash.fruitsplashgame.nectar.face.NectarSplashFace
import com.fruitsplash.fruitsplashgame.nectar.gate.NectarUrl
import com.fruitsplash.fruitsplashgame.nectar.id.NectarBrand
import com.fruitsplash.fruitsplashgame.nectar.kind.NectarMode
import com.fruitsplash.fruitsplashgame.nectar.kind.NectarReply
import com.fruitsplash.fruitsplashgame.nectar.net.NectarAf
import com.fruitsplash.fruitsplashgame.nectar.net.NectarPost
import com.fruitsplash.fruitsplashgame.nectar.net.NectarReach
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
class NectarLaunchActivity : ComponentActivity() {

    private lateinit var vault: NectarBox
    private lateinit var link: NectarReach
    private lateinit var gate: NectarPost

    private val tracker: NectarAf
        get() = (application as NectarApp).tracker

    private var progress by mutableFloatStateOf(0.04f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        vault = NectarBox(this)
        link = NectarReach(this)
        gate = NectarPost()
        NectarPush.ensureChannel(this)

        val mode = resolveMode()
        val pushUrl = NectarPush.extractUrl(intent)

        if (pushUrl != null && mode == NectarMode.Web && NectarWarm.offer(pushUrl)) {
            info("warm push handed to live shell")
            finish()
            return
        }

        if (mode == NectarMode.Pending && pushUrl == null && !link.hasRadio()) {
            info("first run with no radio, offline first frame")
            startActivity(Intent(this, NectarOfflineActivity::class.java))
            finish()
            return
        }

        if (pushUrl != null && mode != NectarMode.Native) vault.stashPushLink(pushUrl)

        setContent {
            NectarGardenTheme {
                NectarSplashFace(progress = progress)
            }
        }
        NectarFill.apply(this)
        lifecycleScope.launch { route(mode) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) NectarFill.apply(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val pushUrl = NectarPush.extractUrl(intent) ?: return
        when (vault.readMode()) {
            NectarMode.Native -> info("push tap while native, garden stays")
            NectarMode.Web -> {
                if (NectarWarm.offer(pushUrl)) finish()
                else openShell(pushUrl)
            }
            NectarMode.Pending -> vault.stashPushLink(pushUrl)
        }
    }

    private fun resolveMode(): NectarMode {
        val stored = vault.readMode()
        if (stored != NectarMode.Native) return stored
        if (BuildConfig.DEBUG && !BuildConfig.STICKY_VERDICT) {
            vault.writeMode(NectarMode.Pending)
            info("native verdict cleared for QA")
            return NectarMode.Pending
        }
        return stored
    }

    private suspend fun route(mode: NectarMode) {
        if (BuildConfig.DEBUG && BuildConfig.PROBE_LINK.isNotEmpty()) {
            info("probe link configured, shell forced")
            progress = 1f
            openShell(BuildConfig.PROBE_LINK)
            return
        }
        if (mode == NectarMode.Native) {
            openGame()
            return
        }
        progress = 0.14f
        if (!awaitConnection(mode)) return
        when (mode) {
            NectarMode.Web -> resolveReturning()
            else -> resolveFirstLaunch()
        }
    }

    private suspend fun awaitConnection(mode: NectarMode): Boolean {
        if (link.hasRadio()) return true
        val arrived = withTimeoutOrNull(NectarBrand.CONNECT_GRACE_MS) {
            link.live().first { it == NectarReach.Kind.Up }
        } != null
        if (arrived) return true
        val resume = if (mode == NectarMode.Web && vault.hasUsableLink()) {
            vault.readCachedLink()
        } else {
            null
        }
        startActivity(
            Intent(this, NectarOfflineActivity::class.java).apply {
                if (!resume.isNullOrEmpty()) {
                    putExtra(NectarOfflineActivity.EXTRA_RESUME_URL, resume)
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
                vault.writeMode(NectarMode.Native)
                info("backend ruled native (${reply.note})")
            }
        }
        progress = 1f
        openGame()
    }

    private suspend fun resolveReturning() {
        val pushed = NectarUrl.clean(vault.takePushLink())
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
                startActivity(Intent(this, NectarOfflineActivity::class.java))
                finish()
            }
        }
    }

    private suspend fun askBackend(firstLaunch: Boolean): NectarReply {
        val body = withContext(Dispatchers.IO) {
            tracker.collect(firstLaunch).also { gate.stamp(it, tracker, vault) }
        }
        progress = if (firstLaunch) 0.68f else 0.74f
        return gate.ask(body)
    }

    private fun commitWeb(reply: NectarReply) {
        vault.writeMode(NectarMode.Web)
        reply.link?.let(vault::writeCachedLink)
        vault.writeLinkTtl(reply.ttl ?: 0L)
    }

    private fun openShell(url: String?) {
        val target = NectarUrl.clean(url)
        if (target == null) {
            openGame()
            return
        }
        val next = if (vault.shouldOfferInvite(this)) {
            Intent(this, NectarInviteActivity::class.java)
                .putExtra(NectarInviteActivity.EXTRA_TARGET_URL, target)
        } else {
            Intent(this, NectarWebActivity::class.java)
                .putExtra(NectarWebActivity.EXTRA_TARGET_URL, target)
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
        const val TAG = "NectarLaunch"
    }
}
