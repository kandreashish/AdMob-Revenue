package com.solarad.app.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// AdMob / Google Material palette.
// Legacy token names (SolarAmber, OceanAzure, ...) are kept so existing screens
// compile unchanged, but every value now maps to the AdMob console colors.
// ---------------------------------------------------------------------------

// Primary — AdMob / Google Blue
val SolarAmber = Color(0xFF1A73E8)          // Google Blue 600 (AdMob primary)
val SolarAmberLight = Color(0xFFE8F0FE)     // Blue 50 (selected / container)
val SolarAmberGlow = Color(0xFFF1F6FE)

// Secondary — AdMob chart comparison series (Cyan)
val OceanAzure = Color(0xFF12B5CB)
val OceanAzureDark = Color(0xFF098591)
val OceanAzureLight = Color(0xFFE4F7FB)

// Success — Google Green
val BotanicalGreen = Color(0xFF1E8E3E)
val BotanicalGreenDark = Color(0xFF137333)
val BotanicalGreenLight = Color(0xFFE6F4EA)

// Surfaces — AdMob console greys
val LinenSand = Color(0xFFF8F9FA)           // Grey 50 page background
val LinenSandDark = Color(0xFFF1F3F4)       // Grey 100
val PureWhite = Color(0xFFFFFFFF)

// Text / borders
val EspressoSlate = Color(0xFF202124)       // Grey 900 primary text
val MutedSlate = Color(0xFF5F6368)          // Grey 700 secondary text
val LightSlate = Color(0xFF80868B)          // Grey 600 muted
val BorderSubtle = Color(0xFFE8EAED)        // Grey 200
val BorderMedium = Color(0xFFDADCE0)        // Grey 300
val CoralCrimson = Color(0xFFD93025)        // Google Red 600
val CoralCrimsonLight = Color(0xFFFCE8E6)

// Accent — AdMob logo yellow (used sparingly: warnings, goal highlights)
val AdMobYellow = Color(0xFFFBBC04)
val AdMobYellowLight = Color(0xFFFEF7E0)

// AdMob home "Total estimated earnings" card
val AdMobHeroGrey = Color(0xFF5F6368)
val AdMobSelectedText = Color(0xFF1967D2)   // Blue 700 — selected nav / chip text

// Dark theme — Google dark surfaces
val DarkCharcoalBg = Color(0xFF202124)
val DarkCharcoalSurface = Color(0xFF292A2D)
val DarkCharcoalSurfaceElevated = Color(0xFF303134)
val DarkCharcoalSurfaceVariant = Color(0xFF35363A)

val DarkTextPrimary = Color(0xFFE8EAED)
val DarkTextSecondary = Color(0xFF9AA0A6)
val DarkTextMuted = Color(0xFF80868B)

val DarkBorderSubtle = Color(0xFF3C4043)
val DarkBorderMedium = Color(0xFF5F6368)

val DarkAmberContainer = Color(0xFF1C3A5F)   // blue container (dark)
val DarkOnAmberContainer = Color(0xFF8AB4F8) // Blue 300

val DarkOceanAzure = Color(0xFF78D9EC)
val DarkAzureContainer = Color(0xFF0B3A42)

val DarkBotanicalGreen = Color(0xFF81C995)
val DarkGreenContainer = Color(0xFF0D3B1E)

val DarkCoralCrimson = Color(0xFFF28B82)
val DarkCrimsonContainer = Color(0xFF4A1C19)

// Ad format colors — AdMob report chart series
val FormatRewarded = Color(0xFF1A73E8)     // Blue
val FormatInterstitial = Color(0xFF12B5CB) // Cyan
val FormatAppOpen = Color(0xFF9334E6)      // Purple
val FormatNative = Color(0xFF1E8E3E)       // Green
val FormatBanner = Color(0xFFF9AB00)       // Yellow 600
