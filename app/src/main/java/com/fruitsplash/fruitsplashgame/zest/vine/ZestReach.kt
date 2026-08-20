package com.fruitsplash.fruitsplashgame.zest.vine

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.fruitsplash.fruitsplashgame.zest.core.ZestIds
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

internal class ZestReach(context: Context) {

    private val app = context.applicationContext
    private val cm = app.getSystemService(ConnectivityManager::class.java)

    fun hasAnyAdapter(): Boolean {
        val active = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun isReachable(): Boolean {
        if (!hasAnyAdapter()) return false
        return withContext(Dispatchers.IO) {
            PROBES.any { (host, port) -> knock(host, port) }
        }
    }

    private fun knock(host: String, port: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), ZestIds.REACH_PROBE_TIMEOUT_MS)
            true
        }
    } catch (_: Throwable) {
        false
    }

    fun statusStream(): Flow<Status> = callbackFlow {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        var pendingDrop: Job? = null

        fun goOnline() {
            pendingDrop?.cancel()
            pendingDrop = null
            trySend(Status.Online)
        }

        fun scheduleDrop() {
            pendingDrop?.cancel()
            pendingDrop = scope.launch {
                delay(ZestIds.OFFLINE_DEBOUNCE_MS)
                trySend(Status.Offline)
            }
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = goOnline()
            override fun onLost(network: Network) = scheduleDrop()
            override fun onUnavailable() = scheduleDrop()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    goOnline()
                } else {
                    scheduleDrop()
                }
            }
        }

        trySend(if (hasAnyAdapter()) Status.Online else Status.Offline)
        val registered = runCatching { cm?.registerDefaultNetworkCallback(callback) }.isSuccess

        awaitClose {
            pendingDrop?.cancel()
            scope.cancel()
            if (registered) runCatching { cm?.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()

    enum class Status { Online, Offline }

    private companion object {
        val PROBES = listOf("1.0.0.1" to 443, "8.8.4.4" to 53)
    }
}
