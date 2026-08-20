package com.fruitsplash.fruitsplashgame.grove.trellis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

@Composable
fun rememberGroveDots(): State<Int> {
    val phase = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            phase.value = (phase.value + 1) % 4
        }
    }
    return phase
}
