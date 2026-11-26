package com.lavish.admob.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lavish.admob.model.AppMonetization
import com.lavish.admob.theme.BotanicalGreen
import com.lavish.admob.theme.MutedSlate
import com.lavish.admob.theme.OceanAzure
import com.lavish.admob.theme.SolarAmber

/**
 * Supported app target platforms with Stitch-designed compact 24dp vector paths
 * (rendered at 14dp–15dp in analytics tables and icon badges).
 */
enum class AppPlatform(
    val label: String,
    val tint: Color,
    val vector: ImageVector
) {
    ANDROID(
        label = "Android",
        tint = BotanicalGreen,
        vector = buildVector(
            name = "PlatformAndroid",
            pathData = "M17.6 9.48l1.84-3.18c.16-.31.04-.69-.26-.85-.29-.15-.65-.06-.83.22l-1.88 3.24a11.43 11.43 0 0 0-8.94 0L5.65 5.67c-.19-.28-.54-.37-.83-.22-.3.16-.42.54-.26.85l1.84 3.18C4.81 10.97 3.63 13.06 3.5 15.5h17c-.13-2.44-1.31-4.53-2.9-6.02zM9 13.25c-.41 0-.75-.34-.75-.75s.34-.75.75-.75.75.34.75.75-.34.75-.75.75zm6 0c-.41 0-.75-.34-.75-.75s.34-.75.75-.75.75.34.75.75-.34.75-.75.75z"
        )
    ),
    IPHONE(
        label = "iPhone",
        tint = OceanAzure,
        vector = buildVector(
            name = "PlatformIPhone",
            pathData = "M16 1H8C6.34 1 5 2.34 5 4v16c0 1.66 1.34 3 3 3h8c1.66 0 3-1.34 3-3V4c0-1.66-1.34-3-3-3zm-2 20h-4v-1h4v1zm3.25-3H6.75V4h10.5v14z"
        )
    ),
    MACOS(
        label = "macOS",
        tint = MutedSlate,
        vector = buildVector(
            name = "PlatformMacOS",
            pathData = "M20 18c1.1 0 1.99-.9 1.99-2L22 6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2H0v2h24v-2h-4zM4 6h16v10H4V6z"
        )
    ),
    WEBSITE(
        label = "Website",
        tint = SolarAmber,
        vector = buildVector(
            name = "PlatformWebsite",
            pathData = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"
        )
    );

    companion object {
        fun resolve(
            platform: String,
            appName: String = "",
            appStoreId: String? = null
        ): AppPlatform {
            val p = platform.trim().lowercase()
            when {
                p.contains("mac") || p.contains("osx") || p.contains("desktop") -> return MACOS
                p.contains("web") || p.contains("site") || p.contains("browser") -> return WEBSITE
                p == "ios" || p.contains("iphone") || p.contains("ipad") -> return IPHONE
                p == "android" -> return ANDROID
            }

            // Fallback inference when platform string is blank or unrecognized
            val name = appName.trim().lowercase()
            val store = appStoreId?.trim()?.lowercase().orEmpty()
            return when {
                name.contains("macos") || name.contains("mac os") || name.contains("(mac)") -> MACOS
                name.contains("website") || name.contains("web app") ||
                    store.startsWith("http") ||
                    name.endsWith(".com") || name.endsWith(".io") || name.endsWith(".org") || name.endsWith(".net") -> WEBSITE
                name.contains("ios") || name.contains("iphone") || name.contains("ipad") -> IPHONE
                else -> ANDROID
            }
        }
    }
}

private fun buildVector(name: String, pathData: String): ImageVector {
    val nodes = PathParser().parsePathString(pathData).toNodes()
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = nodes,
        fill = SolidColor(Color.Black)
    ).build()
}

/**
 * Small, compact platform indicator icon (Android / iPhone / macOS / Website).
 */
@Composable
fun CompactPlatformIcon(
    platform: AppPlatform,
    modifier: Modifier = Modifier,
    size: Dp = 15.dp
) {
    Icon(
        imageVector = platform.vector,
        contentDescription = platform.label,
        tint = platform.tint,
        modifier = modifier.size(size)
    )
}

@Composable
fun CompactPlatformIcon(
    app: AppMonetization,
    modifier: Modifier = Modifier,
    size: Dp = 15.dp
) {
    CompactPlatformIcon(
        platform = AppPlatform.resolve(app.platform, app.name, app.appStoreId),
        modifier = modifier,
        size = size
    )
}
