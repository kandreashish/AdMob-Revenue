package com.lavish.admob.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberSoundPlayer(): suspend (ByteArray) -> Unit {
    return remember {
        { bytes ->
            var player: AVAudioPlayer? = null
            try {
                val session = AVAudioSession.sharedInstance()
                session.setCategory(AVAudioSessionCategoryAmbient, error = null)
                session.setActive(true, error = null)

                val data = bytes.usePinned { pinned ->
                    NSData.create(
                        bytes = pinned.addressOf(0),
                        length = bytes.size.toULong()
                    )
                }
                player = AVAudioPlayer(data = data, error = null).apply {
                    prepareToPlay()
                    play()
                }
                while (player.playing) {
                    delay(100L)
                }
            } catch (e: CancellationException) {
                player?.stop()
                throw e
            } catch (_: Throwable) {
                player?.stop()
            }
        }
    }
}
