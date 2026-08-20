package com.fruitsplash.fruitsplashgame.grove.lane

enum class GroveLane(val wire: String) {
    Web("web"),
    Native("native"),
    Pending("pending");

    companion object {
        fun fromWire(raw: String?): GroveLane = when (raw) {
            "web" -> Web
            "native" -> Native
            else -> Pending
        }
    }
}
