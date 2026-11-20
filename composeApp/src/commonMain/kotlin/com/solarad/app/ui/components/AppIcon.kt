package com.solarad.app.ui.components

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
import com.solarad.app.data.AppIconResolver
import com.solarad.app.model.AppMonetization
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.SolarTheme
import kotlin.math.abs

private val AppGradientPalettes = listOf(
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)), // Amber to Coral
    listOf(Color(0xFF0284C7), Color(0xFF1E40AF)), // Azure to Deep Blue
    listOf(Color(0xFF10B981), Color(0xFF047857)), // Emerald to Forest
    listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)), // Violet to Indigo
    listOf(Color(0xFFEC4899), Color(0xFFBE185D)), // Pink to Crimson
    listOf(Color(0xFFF97316), Color(0xFFFBBF24)), // Orange to Gold
    listOf(Color(0xFF06B6D4), Color(0xFF0F766E)), // Cyan to Teal
    listOf(Color(0xFF6366F1), Color(0xFF3730A3))  // Indigo to Midnight
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
    val updates by AppIconResolver.iconUpdates.collectAsState()
    val resolvedUrl = updates[app.id] ?: AppIconResolver.getCachedIconUrl(app)

    var bitmap by remember(resolvedUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(app.id, app.appStoreId, app.name) {
        AppIconResolver.requestIconResolve(app)
    }

    LaunchedEffect(resolvedUrl) {
        if (!resolvedUrl.isNullOrBlank()) {
            bitmap = AvatarImageLoader.load(resolvedUrl)
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
            val initials = getInitials(app.name)

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
