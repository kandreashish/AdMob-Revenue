package com.solarad.app.ui.navigation

import androidx.compose.runtime.Composable

/**
 * Multiplatform BackHandler.
 * Intercepts platform back button / gestures on Android and is a no-op on platforms without hardware back buttons.
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
