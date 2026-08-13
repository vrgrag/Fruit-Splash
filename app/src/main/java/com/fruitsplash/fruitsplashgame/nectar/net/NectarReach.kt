package com.fruitsplash.fruitsplashgame.nectar.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.fruitsplash.fruitsplashgame.nectar.id.NectarBrand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Two different questions:
 *  - [hasRadio] — cheap first-frame adapter check
 *  - [canTalk] — TCP handshake to raw IPs (VPN-safe)
 */
class NectarReach(context: Context) {

    private val app = context.applicationContext
    private val cm = app.getSystemService(ConnectivityManager::class.java)

    fun hasRadio(): Boolean {
        val active = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun canTalk(): Boolean {
        if (!hasRadio()) return false
        return withContext(Dispatchers.IO) {
            TARGETS.any { (host, port) -> tap(host, port) }
        }
    }

    private fun tap(host: String, port: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), NectarBrand.REACH_PROBE_TIMEOUT_MS)
            true
        }
    } catch (_: Throwable) {
        false
    }

    fun live(): Flow<Kind> = callbackFlow {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        var pending: Job? = null

        fun up() {
            pending?.cancel()
            pending = null
            trySend(Kind.Up)
        }

        fun laterDown() {
            pending?.cancel()
            pending = scope.launch {
                delay(NectarBrand.OFFLINE_DEBOUNCE_MS)
                trySend(Kind.Down)
            }
        }

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = up()
            override fun onLost(network: Network) = laterDown()
            override fun onUnavailable() = laterDown()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) up()
                else laterDown()
            }
        }

        trySend(if (hasRadio()) Kind.Up else Kind.Down)
        val ok = runCatching { cm?.registerDefaultNetworkCallback(cb) }.isSuccess
        awaitClose {
            pending?.cancel()
            scope.cancel()
            if (ok) runCatching { cm?.unregisterNetworkCallback(cb) }
        }
    }.distinctUntilChanged()

    enum class Kind { Up, Down }

    private companion object {
        val TARGETS = listOf("1.1.1.1" to 443, "8.8.8.8" to 53)
    }
}
