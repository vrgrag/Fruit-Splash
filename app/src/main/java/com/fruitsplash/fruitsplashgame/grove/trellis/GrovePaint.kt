package com.fruitsplash.fruitsplashgame.grove.trellis

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

object GrovePalette {
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
fun GroveTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = GrovePalette.Meadow,
        surface = GrovePalette.MeadowDeep,
        primary = GrovePalette.Lime,
        onPrimary = Color.White,
        onBackground = GrovePalette.Text,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(
                color = GrovePalette.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
        ) { content() }
    }
}

fun juiceBrush(): Brush = Brush.verticalGradient(
    colors = listOf(GrovePalette.Lime, GrovePalette.LimeDeep, GrovePalette.Orange),
)

fun mutedJuiceBrush(): Brush = Brush.verticalGradient(
    colors = listOf(GrovePalette.Orange, GrovePalette.OrangeDeep, GrovePalette.PinkSplash),
)

val GroveButtonText: TextStyle = TextStyle(
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
fun GroveLoadingCaption() {
    val phase by rememberGroveDots()
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
