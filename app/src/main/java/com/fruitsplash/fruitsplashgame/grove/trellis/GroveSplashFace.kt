package com.fruitsplash.fruitsplashgame.grove.trellis

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fruitsplash.fruitsplashgame.R

@Composable
fun GroveSplashFace(progress: Float) {
    val cfg = LocalConfiguration.current
    val landscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bg = if (landscape) R.drawable.grove_splash_land else R.drawable.grove_splash_port

    BoxWithConstraints(Modifier.fillMaxSize().background(GrovePalette.Meadow)) {
        val screenW = maxWidth
        val screenH = maxHeight
        Image(
            painter = painterResource(bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        val captionBottomPct = if (landscape) 0.18f else 0.21f
        val barBottomPct = if (landscape) 0.09f else 0.13f
        val barWidth = if (landscape) screenW * 0.58f else screenW - 72.dp

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = screenH * captionBottomPct),
        ) {
            GroveLoadingCaption()
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = screenH * barBottomPct)
                .width(barWidth)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x66000000)),
        ) {
            val animated by animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 260),
                label = "groveBar",
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction = animated)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(juiceBrush()),
            )
        }
    }
}
