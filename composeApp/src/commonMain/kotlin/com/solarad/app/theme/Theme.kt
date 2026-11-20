package com.solarad.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.model.ThemeMode

data class SolarColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val borderMedium: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val bottomDockBackground: Color,
    val pillSelectedBg: Color,
    val pillSelectedText: Color,
    val pillUnselectedBg: Color,
    val pillUnselectedText: Color
)

val LightSolarColors = SolarColors(
    isDark = false,
    background = LinenSand,
    surface = PureWhite,
    surfaceElevated = PureWhite,
    surfaceVariant = LinenSandDark,
    cardBackground = PureWhite,
    textPrimary = EspressoSlate,
    textSecondary = MutedSlate,
    textMuted = LightSlate,
    borderSubtle = BorderSubtle,
    borderMedium = BorderMedium,
    primary = SolarAmber,
    primaryContainer = SolarAmberLight,
    onPrimaryContainer = EspressoSlate,
    secondary = OceanAzure,
    secondaryContainer = OceanAzureLight,
    onSecondaryContainer = OceanAzureDark,
    success = BotanicalGreen,
    successContainer = BotanicalGreenLight,
    onSuccessContainer = BotanicalGreenDark,
    error = CoralCrimson,
    errorContainer = CoralCrimsonLight,
    onErrorContainer = CoralCrimson,
    bottomDockBackground = PureWhite,
    pillSelectedBg = EspressoSlate,
    pillSelectedText = PureWhite,
    pillUnselectedBg = PureWhite,
    pillUnselectedText = EspressoSlate
)

val DarkSolarColors = SolarColors(
    isDark = true,
    background = DarkCharcoalBg,
    surface = DarkCharcoalSurface,
    surfaceElevated = DarkCharcoalSurfaceElevated,
    surfaceVariant = DarkCharcoalSurfaceVariant,
    cardBackground = DarkCharcoalSurface,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    borderSubtle = DarkBorderSubtle,
    borderMedium = DarkBorderMedium,
    primary = SolarAmber,
    primaryContainer = DarkAmberContainer,
    onPrimaryContainer = DarkOnAmberContainer,
    secondary = DarkOceanAzure,
    secondaryContainer = DarkAzureContainer,
    onSecondaryContainer = DarkOceanAzure,
    success = DarkBotanicalGreen,
    successContainer = DarkGreenContainer,
    onSuccessContainer = DarkBotanicalGreen,
    error = DarkCoralCrimson,
    errorContainer = DarkCrimsonContainer,
    onErrorContainer = DarkCoralCrimson,
    bottomDockBackground = DarkCharcoalSurfaceElevated,
    pillSelectedBg = SolarAmber,
    pillSelectedText = EspressoSlate,
    pillUnselectedBg = DarkCharcoalSurfaceVariant,
    pillUnselectedText = DarkTextPrimary
)

val LocalSolarColors = staticCompositionLocalOf { LightSolarColors }

object SolarTheme {
    val colors: SolarColors
        @Composable
        get() = LocalSolarColors.current
}

private val SolarAdColorScheme = lightColorScheme(
    primary = SolarAmber,
    onPrimary = PureWhite,
    primaryContainer = SolarAmberLight,
    onPrimaryContainer = EspressoSlate,
    secondary = OceanAzure,
    onSecondary = PureWhite,
    secondaryContainer = OceanAzureLight,
    onSecondaryContainer = OceanAzureDark,
    background = LinenSand,
    onBackground = EspressoSlate,
    surface = PureWhite,
    onSurface = EspressoSlate,
    surfaceVariant = LinenSandDark,
    onSurfaceVariant = MutedSlate,
    outline = BorderMedium,
    outlineVariant = BorderSubtle
)

private val SolarAdDarkColorScheme = darkColorScheme(
    primary = SolarAmber,
    onPrimary = EspressoSlate,
    primaryContainer = DarkAmberContainer,
    onPrimaryContainer = DarkOnAmberContainer,
    secondary = DarkOceanAzure,
    onSecondary = EspressoSlate,
    secondaryContainer = DarkAzureContainer,
    onSecondaryContainer = DarkOceanAzure,
    background = DarkCharcoalBg,
    onBackground = DarkTextPrimary,
    surface = DarkCharcoalSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCharcoalSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderMedium,
    outlineVariant = DarkBorderSubtle
)

val SolarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val SolarTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.03).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.02).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.015).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
expect fun PlatformSystemBarsEffect(darkTheme: Boolean)

@Composable
fun SolarAdTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SolarAdDarkColorScheme else SolarAdColorScheme
    val solarColors = if (darkTheme) DarkSolarColors else LightSolarColors

    PlatformSystemBarsEffect(darkTheme = darkTheme)

    CompositionLocalProvider(LocalSolarColors provides solarColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SolarTypography,
            shapes = SolarShapes,
            content = content
        )
    }
}
