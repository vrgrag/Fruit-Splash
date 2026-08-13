package com.fruitsplash.fruitsplashgame.nectar.box

import java.util.concurrent.atomic.AtomicReference

/** In-process warm push hand-off. Never persisted. */
object NectarWarm {

    private val sink = AtomicReference<((String) -> Unit)?>(null)

    @Volatile
    var shellAlive: Boolean = false

    private val parked = AtomicReference<String?>(null)

    fun attach(sinkFn: (String) -> Unit) {
        sink.set(sinkFn)
    }

    fun detach() {
        sink.set(null)
    }

    fun offer(url: String): Boolean {
        sink.get()?.let { deliver ->
            deliver(url)
            return true
        }
        if (!shellAlive) return false
        parked.set(url)
        return true
    }

    fun drain(): String? = parked.getAndSet(null)
}
