package com.fruitsplash.fruitsplashgame.nectar.kind

enum class NectarMode(val wire: String) {
    Web("web"),
    Native("native"),
    Pending("pending");

    companion object {
        fun fromWire(raw: String?): NectarMode = when (raw) {
            "web" -> Web
            "native" -> Native
            else -> Pending
        }
    }
}
