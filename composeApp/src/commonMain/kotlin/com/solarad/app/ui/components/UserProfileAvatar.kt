package com.solarad.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarAmberLight
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess

object AvatarImageLoader {
    private val cache = mutableMapOf<String, ImageBitmap>()
    private val client = HttpClient()

    suspend fun load(url: String): ImageBitmap? {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return null
        cache[cleanUrl]?.let { return it }

        return try {
            val response = client.get(cleanUrl)
            if (response.status.isSuccess()) {
                val bytes = response.readRawBytes()
                val bitmap = decodeByteArrayToBitmap(bytes)
                if (bitmap != null) {
                    cache[cleanUrl] = bitmap
                }
                bitmap
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * Renders the user's actual Google Profile picture.
 * Fallback to initials or account icon if loading fails or URL is blank.
 */
@Composable
fun UserProfileAvatar(
    avatarUrl: String?,
    userName: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp
) {
    var imageBitmap by remember(avatarUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(avatarUrl) {
        if (!avatarUrl.isNullOrBlank()) {
            imageBitmap = AvatarImageLoader.load(avatarUrl)
        } else {
            imageBitmap = null
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = "Profile Photo",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initial = userName.trim().firstOrNull()?.uppercaseChar()?.toString()
        if (!initial.isNullOrBlank() && userName.isNotBlank() && userName != "Google User" && userName != "AdMob Publisher") {
            Box(
                modifier = modifier
                    .clip(CircleShape)
                    .background(SolarAmberLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = SolarAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = (iconSize.value * 0.7f).sp
                )
            }
        } else {
            Box(
                modifier = modifier.clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Photo",
                    tint = EspressoSlate,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
