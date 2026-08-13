package com.fruitsplash.fruitsplashgame.nectar.face

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fruitsplash.fruitsplashgame.R

/**
 * Push invite. NO systemBars / safeDrawing padding — landscape
 * cutout would shift the pills off the plaque centre.
 */
@Composable
fun NectarInviteFace(
    onAccept: () -> Unit,
    onSkip: () -> Unit,
) {
    val cfg = LocalConfiguration.current
    val landscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bg = if (landscape) R.drawable.nectar_invite_land else R.drawable.nectar_invite_port
    val card = if (landscape) NectarArtMetrics.InviteLandscape else NectarArtMetrics.InvitePortrait

    BoxWithConstraints(Modifier.fillMaxSize().background(NectarPalette.Meadow)) {
        val screenW = maxWidth
        val band = card.projectInto(maxWidth, maxHeight)
        Image(
            painter = painterResource(bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (landscape) {
            val btnHeight = 50.dp
            val btnWidth = screenW * 0.24f
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = band.offsetForInvite(btnHeight)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.width(btnWidth).height(btnHeight)) {
                    NectarPill(label = "Accept", brush = juiceBrush(), onTap = onAccept)
                }
                Box(Modifier.width(btnWidth).height(btnHeight)) {
                    NectarPill(label = "Skip", brush = mutedJuiceBrush(), onTap = onSkip)
                }
            }
        } else {
            val acceptHeight = 54.dp
            val skipHeight = 48.dp
            val spacing = 10.dp
            val btnWidth = screenW * 0.68f
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = band.offsetForInvite(acceptHeight + spacing + skipHeight))
                    .width(btnWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                Box(Modifier.width(btnWidth).height(acceptHeight)) {
                    NectarPill(label = "Accept", brush = juiceBrush(), onTap = onAccept)
                }
                Box(Modifier.width(btnWidth).height(skipHeight)) {
                    NectarPill(label = "Skip", brush = mutedJuiceBrush(), onTap = onSkip)
                }
            }
        }
    }
}

@Composable
fun NectarOfflineFace(
    busy: Boolean = false,
    onRetry: () -> Unit,
) {
    val cfg = LocalConfiguration.current
    val landscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bg = if (landscape) R.drawable.nectar_quiet_land else R.drawable.nectar_quiet_port
    val card = if (landscape) NectarArtMetrics.OfflineLandscape else NectarArtMetrics.OfflinePortrait

    BoxWithConstraints(Modifier.fillMaxSize().background(NectarPalette.Meadow)) {
        val screenW = maxWidth
        val band = card.projectInto(maxWidth, maxHeight)
        Image(
            painter = painterResource(bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        val btnWidth = if (landscape) screenW * 0.28f else screenW * 0.52f
        val btnHeight = 52.dp
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = band.offsetForOffline(btnHeight))
                .width(btnWidth)
                .height(btnHeight),
        ) {
            NectarPill(label = "Retry", brush = juiceBrush(), onTap = onRetry, busy = busy)
        }
    }
}

@Composable
internal fun NectarPill(
    label: String,
    brush: Brush,
    onTap: () -> Unit,
    busy: Boolean = false,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(80),
        label = "nectarPill",
    )
    Box(
        Modifier
            .fillMaxSize()
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(brush)
            .pointerInput(busy) {
                if (busy) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        val released = tryAwaitRelease()
                        pressed = false
                        if (released) onTap()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.4.dp,
            )
        } else {
            Text(label, style = NectarButtonText)
        }
    }
}
