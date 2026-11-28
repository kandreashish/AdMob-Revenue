package com.lavish.admob.ui.utils

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

@Composable
actual fun rememberSoundPlayer(): suspend (ByteArray) -> Unit {
    val appContext = LocalContext.current.applicationContext

    return remember(appContext) {
        { bytes ->
            withContext(Dispatchers.IO) {
                val cacheFile = File(appContext.cacheDir, "fireworks_sfx.wav")
                if (!cacheFile.exists() || cacheFile.length() != bytes.size.toLong()) {
                    cacheFile.writeBytes(bytes)
                }
                suspendCancellableCoroutine { cont ->
                    var player: MediaPlayer? = null
                    runCatching {
                        player = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_GAME)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            setDataSource(cacheFile.absolutePath)
                            setOnCompletionListener { mp ->
                                mp.release()
                                if (cont.isActive) cont.resume(Unit)
                            }
                            setOnErrorListener { mp, _, _ ->
                                mp.release()
                                if (cont.isActive) cont.resume(Unit)
                                true
                            }
                            prepare()
                            start()
                        }
                    }.onFailure {
                        runCatching { player?.release() }
                        if (cont.isActive) cont.resume(Unit)
                    }
                    cont.invokeOnCancellation {
                        runCatching {
                            player?.stop()
                            player?.release()
                        }
                    }
                }
            }
        }
    }
}
