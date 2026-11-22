package com.lavish.admob.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavish.admob.data.AppIconResolver
import com.lavish.admob.model.AppMonetization
import com.lavish.admob.theme.BotanicalGreen
import com.lavish.admob.theme.OceanAzure
import com.lavish.admob.theme.SolarTheme
import kotlin.math.abs

private val AppGradientPalettes = listOf(
    listOf(Color(0xFF3367D6), Color(0xFFD93025)), // Amber to Coral
    listOf(Color(0xFF12B5CB), Color(0xFF185ABC)), // Azure to Deep Blue
    listOf(Color(0xFF1E8E3E), Color(0xFF137333)), // Emerald to Forest
    listOf(Color(0xFF9334E6), Color(0xFF4285F4)), // Violet to Indigo
    listOf(Color(0xFFE52592), Color(0xFFB80672)), // Pink to Crimson
    listOf(Color(0xFFF97316), Color(0xFF4285F4)), // Orange to Gold
    listOf(Color(0xFF06B6D4), Color(0xFF0F766E)), // Cyan to Teal
    listOf(Color(0xFF4285F4), Color(0xFF1967D2))  // Indigo to Midnight
)

private fun getInitials(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return "AP"
    val parts = clean.split(Regex("[\\s._-]+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        clean.length >= 2 -> clean.take(2).uppercase()
        else -> clean.take(1).uppercase()
    }
}

@Composable
fun AppIcon(
    app: AppMonetization,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    showPlatformBadge: Boolean = true
) {
    // Observe only THIS app's icon url so one resolved icon doesn't recompose every icon in the list
    val updatedUrlFlow = remember(app.id) {
        AppIconResolver.iconUpdates
            .map { it[app.id] }
            .distinctUntilChanged()
    }
    val updatedUrl by updatedUrlFlow.collectAsState(initial = AppIconResolver.iconUpdates.value[app.id])
    val resolvedUrl = updatedUrl ?: AppIconResolver.getCachedIconUrl(app)

    // Key on app.id so the state isn't wiped when resolvedUrl goes from null → URL
    var bitmap by remember(app.id) { mutableStateOf(AvatarImageLoader.peek(resolvedUrl)) }

    LaunchedEffect(app.id, app.appStoreId, app.name) {
        AppIconResolver.requestIconResolve(app)
    }

    // Load (or reload) the bitmap whenever a new URL becomes available
    LaunchedEffect(resolvedUrl) {
        if (!resolvedUrl.isNullOrBlank() && bitmap == null) {
            val loaded = AvatarImageLoader.load(resolvedUrl)
            if (loaded != null) bitmap = loaded
        }
    }

    val cornerRadius = size * 0.24f
    val isIos = app.platform.equals("iOS", ignoreCase = true)

    Box(modifier = modifier.size(size)) {
        if (bitmap != null) {
            // Real Store App Icon
            Image(
                bitmap = bitmap!!,
                contentDescription = app.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
                    .border(0.5.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(cornerRadius))
            )
        } else {
            // Elegant Monogram Tile
            val paletteIndex = abs(app.name.hashCode()) % AppGradientPalettes.size
            val gradientColors = AppGradientPalettes[paletteIndex]
            val initials = remember(app.name) { getInitials(app.name) }

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Optional tiny platform badge in bottom-right corner
        if (showPlatformBadge && size >= 32.dp) {
            Surface(
                shape = CircleShape,
                color = SolarTheme.colors.cardBackground,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .size(size * 0.38f)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isIos) Icons.Default.PhoneIphone else Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = if (isIos) OceanAzure else BotanicalGreen,
                        modifier = Modifier.size(size * 0.26f)
                    )
                }
            }
        }
    }
}
