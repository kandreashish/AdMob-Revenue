package com.lavish.admob.ui.components

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
import com.lavish.admob.theme.EspressoSlate
import com.lavish.admob.theme.SolarAmber
import com.lavish.admob.theme.SolarAmberLight
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.withLock
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess

object AvatarImageLoader {
    private const val MAX_ENTRIES = 48
    // Access-ordered LRU so icons/avatars can't grow memory unbounded
    private val cache = LinkedHashMap<String, ImageBitmap>()
    private val inFlight = mutableMapOf<String, kotlinx.coroutines.Deferred<ImageBitmap?>>()
    private val lock = kotlinx.coroutines.sync.Mutex()
    private val client = HttpClient()
    // One long-lived scope for all image fetches (no per-request unstructured scopes)
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    // Read-only snapshot for lock-free, thread-safe peeks from the UI thread
    @kotlin.concurrent.Volatile private var snapshot: Map<String, ImageBitmap> = emptyMap()

    /** Synchronous cache lookup so composables can render the image on the very first frame. */
    fun peek(url: String?): ImageBitmap? {
        val key = url?.trim().orEmpty()
        if (key.isBlank()) return null
        return snapshot[key]
    }

    private fun put(key: String, bmp: ImageBitmap) {
        cache.remove(key)
        cache[key] = bmp
        while (cache.size > MAX_ENTRIES) cache.remove(cache.keys.first())
        snapshot = HashMap(cache)
    }

    suspend fun load(url: String): ImageBitmap? {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return null
        snapshot[cleanUrl]?.let { return it }

        // De-duplicate concurrent requests for the same URL (e.g. same icon in several lists)
        val deferred = lock.withLock {
            cache[cleanUrl]?.let { return it }
            inFlight.getOrPut(cleanUrl) {
                scope.async {
                    try {
                        val response = client.get(cleanUrl)
                        if (response.status.isSuccess()) {
                            // Decode off the main thread
                            decodeByteArrayToBitmap(response.readRawBytes())
                        } else null
                    } catch (_: Throwable) {
                        null
                    }
                }
            }
        }
        val bitmap = try { deferred.await() } catch (_: Throwable) { null }
        lock.withLock {
            inFlight.remove(cleanUrl)
            if (bitmap != null) put(cleanUrl, bitmap)
        }
        return bitmap
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
    var imageBitmap by remember(avatarUrl) { mutableStateOf(AvatarImageLoader.peek(avatarUrl)) }

    LaunchedEffect(avatarUrl) {
        if (!avatarUrl.isNullOrBlank()) {
            if (imageBitmap == null) imageBitmap = AvatarImageLoader.load(avatarUrl)
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
