package com.solarad.app.ui.components

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeByteArrayToBitmap(bytes: ByteArray): ImageBitmap?
