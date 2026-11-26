package com.lavish.admob.ui.components

import com.lavish.admob.resources.Res
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
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

/**
 * Full-screen fireworks Lottie overlay that plays once whenever [triggerKey] changes to a non-null value.
 */
@Composable
fun FireworksLottie(
    triggerKey: Any?,
    modifier: Modifier = Modifier
) {
    var activeKey by remember { mutableStateOf<Any?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(triggerKey) {
        if (triggerKey != null) {
            activeKey = triggerKey
            isPlaying = true
        }
    }

    if (activeKey == null) return

    val composition by rememberLottieComposition(activeKey) {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/fireworks.json").decodeToString()
        )
    }

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = 1,
        restartOnPlay = true
    )

    LaunchedEffect(progress, composition) {
        if (composition != null && progress >= 1f) {
            isPlaying = false
            activeKey = null
        }
    }

    if (composition != null) {
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
}
