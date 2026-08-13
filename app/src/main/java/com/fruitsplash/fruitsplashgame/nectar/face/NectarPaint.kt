package com.fruitsplash.fruitsplashgame.nectar.face

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object NectarPalette {
    val Meadow = Color(0xFF16351D)
    val MeadowDeep = Color(0xFF0C2414)
    val Lime = Color(0xFFB8E986)
    val LimeDeep = Color(0xFF5CB85C)
    val Orange = Color(0xFFFFB347)
    val OrangeDeep = Color(0xFFE67A22)
    val PinkSplash = Color(0xFFFF6B9D)
    val Text = Color(0xFFFFF8E7)
}

@Composable
fun NectarGardenTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = NectarPalette.Meadow,
        surface = NectarPalette.MeadowDeep,
        primary = NectarPalette.Lime,
        onPrimary = Color.White,
        onBackground = NectarPalette.Text,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(
                color = NectarPalette.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
        ) { content() }
    }
}

fun juiceBrush(): Brush = Brush.verticalGradient(
    colors = listOf(NectarPalette.Lime, NectarPalette.LimeDeep, NectarPalette.Orange),
)

fun mutedJuiceBrush(): Brush = Brush.verticalGradient(
    colors = listOf(NectarPalette.Orange, NectarPalette.OrangeDeep, NectarPalette.PinkSplash),
)

val NectarButtonText: TextStyle = TextStyle(
    color = Color.White,
    fontSize = 18.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 18.sp,
    letterSpacing = 0.5.sp,
    shadow = Shadow(
        color = Color(0x99003311),
        offset = Offset(0f, 2f),
        blurRadius = 4f,
    ),
)

@Composable
fun NectarLoadingCaption() {
    val phase by rememberNectarDots()
    val dots = when (phase) {
        0 -> ""
        1 -> "."
        2 -> ". ."
        else -> ". . ."
    }
    Text(
        text = "Loading $dots",
        style = TextStyle(
            color = Color.White.copy(alpha = 0.94f),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            shadow = Shadow(
                color = Color(0xB3000000),
                offset = Offset(0f, 2f),
                blurRadius = 6f,
            ),
        ),
    )
}
