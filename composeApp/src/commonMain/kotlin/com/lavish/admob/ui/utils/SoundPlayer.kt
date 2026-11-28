package com.lavish.admob.ui.utils

import androidx.compose.runtime.Composable

/**
 * Returns a suspending callback that plays a WAV audio payload and suspends until playback finishes.
 */
@Composable
expect fun rememberSoundPlayer(): suspend (ByteArray) -> Unit
