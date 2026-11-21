package com.solarad.app.ui.components

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes [bytes] and downsamples so the longest edge is at most [maxPx].
 * Icons/avatars are shown at <=48dp, so full-size (512px+) bitmaps only waste memory.
 */
expect fun decodeByteArrayToBitmap(bytes: ByteArray, maxPx: Int = 192): ImageBitmap?
