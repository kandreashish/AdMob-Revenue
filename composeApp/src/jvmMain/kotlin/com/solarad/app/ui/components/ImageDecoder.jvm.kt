package com.solarad.app.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode

actual fun decodeByteArrayToBitmap(bytes: ByteArray, maxPx: Int): ImageBitmap? {
    return try {
        val image = Image.makeFromEncoded(bytes)
        val longest = maxOf(image.width, image.height)
        if (maxPx <= 0 || longest <= maxPx) return image.toComposeImageBitmap()
        val s = maxPx.toFloat() / longest
        val w = (image.width * s).toInt().coerceAtLeast(1)
        val h = (image.height * s).toInt().coerceAtLeast(1)
        val bitmap = Bitmap()
        bitmap.allocN32Pixels(w, h)
        val pixmap = bitmap.peekPixels() ?: return image.toComposeImageBitmap()
        if (!image.scalePixels(pixmap, SamplingMode.LINEAR, false)) return image.toComposeImageBitmap()
        image.close()
        bitmap.setImmutable()
        bitmap.asComposeImageBitmap()
    } catch (_: Throwable) {
        null
    }
}
