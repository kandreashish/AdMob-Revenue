package com.lavish.admob.ui.components

import com.lavish.admob.resources.Res
import com.lavish.admob.ui.utils.rememberSoundPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

/**
 * Full-screen fireworks Lottie overlay that loops while the fireworks sound plays
 * whenever [triggerKey] changes to a non-null value.
 */
@Composable
fun FireworksLottie(
    triggerKey: Any?,
    modifier: Modifier = Modifier
) {
    var activeKey by remember { mutableStateOf<Any?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    val playSound = rememberSoundPlayer()
    var soundBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Preload both Lottie composition and sound bytes so playback starts immediately on trigger.
    val composition by rememberLottieComposition(Unit) {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/fireworks.json").decodeToString()
        )
    }

    LaunchedEffect(Unit) {
        if (soundBytes == null) {
            soundBytes = runCatching { Res.readBytes("files/fireworks.wav") }.getOrNull()
        }
    }

    LaunchedEffect(triggerKey) {
        if (triggerKey != null) {
            activeKey = triggerKey
            isPlaying = true
            try {
                val bytes = soundBytes ?: runCatching { Res.readBytes("files/fireworks.wav") }
                    .getOrNull()
                    ?.also { soundBytes = it }
                if (bytes != null) {
                    playSound(bytes)
                }
            } finally {
                if (activeKey == triggerKey) {
                    isPlaying = false
                    activeKey = null
                }
            }
        }
    }

    if (activeKey == null || composition == null) return

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = Compottie.IterateForever,
        restartOnPlay = true
    )

    Image(
        painter = rememberLottiePainter(
            composition = composition,
            progress = { progress }
        ),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize()
    )
}
