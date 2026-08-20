package com.fruitsplash.fruitsplashgame.grove.trellis

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

internal data class GroveCard(
    val artWidth: Int,
    val artHeight: Int,
    val bottom: Float,
) {
    fun projectInto(frameWidth: Dp, frameHeight: Dp): GroveBand {
        val w = frameWidth.value
        val h = frameHeight.value
        val factor = max(w / artWidth, h / artHeight)
        val painted = artHeight * factor
        val top = (h - painted) / 2f
        return GroveBand(
            cardBottom = (top + bottom * painted).coerceIn(0f, h).dp,
            artBottom = min(h, top + painted).dp,
            frameHeight = frameHeight,
        )
    }
}

internal data class GroveBand(
    val cardBottom: Dp,
    val artBottom: Dp,
    val frameHeight: Dp,
) {
    fun offsetForInvite(blockHeight: Dp, gap: Dp = 20.dp, safety: Dp = 14.dp): Dp {
        val bandTop = cardBottom.value + gap.value
        val bandBottom = min(artBottom.value, frameHeight.value) - safety.value
        val slack = bandBottom - bandTop - blockHeight.value
        val offset = bandTop + max(0f, slack) * 0.30f
        val ceiling = frameHeight.value - blockHeight.value - safety.value
        return max(0f, min(offset, ceiling)).dp
    }

    fun offsetForOffline(blockHeight: Dp, gap: Dp = 22.dp, safety: Dp = 14.dp): Dp {
        val bandTop = cardBottom.value + gap.value
        val bandBottom = min(artBottom.value, frameHeight.value) - safety.value
        val slack = bandBottom - bandTop - blockHeight.value
        val offset = bandTop + max(0f, slack) * 0.38f
        val ceiling = frameHeight.value - blockHeight.value - safety.value
        return max(0f, min(offset, ceiling)).dp
    }
}

internal object GroveArtMetrics {
    val InvitePortrait = GroveCard(1080, 2400, 0.64f)
    val InviteLandscape = GroveCard(2400, 1080, 0.68f)
    val OfflinePortrait = GroveCard(1080, 2400, 0.66f)
    val OfflineLandscape = GroveCard(2400, 1080, 0.70f)
}
