package com.lavish.admob.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import kotlin.coroutines.resume

@Composable
actual fun rememberSoundPlayer(): suspend (ByteArray) -> Unit {
    return remember {
        { bytes ->
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { cont ->
                    var clip: Clip? = null
                    runCatching {
                        val ais = AudioSystem.getAudioInputStream(
                            BufferedInputStream(ByteArrayInputStream(bytes))
                        )
                        clip = AudioSystem.getClip().apply {
                            addLineListener { event ->
                                if (event.type == LineEvent.Type.STOP) {
                                    close()
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            }
                            open(ais)
                            start()
                        }
                    }.onFailure {
                        runCatching { clip?.close() }
                        if (cont.isActive) cont.resume(Unit)
                    }
                    cont.invokeOnCancellation {
                        runCatching {
                            clip?.stop()
                            clip?.close()
                        }
                    }
                }
            }
        }
    }
}
