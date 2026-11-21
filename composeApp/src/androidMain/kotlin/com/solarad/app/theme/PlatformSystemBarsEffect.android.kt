package com.solarad.app.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun PlatformSystemBarsEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        // Only touch the window when the theme actually flips, not on every recomposition
        LaunchedEffect(view, darkTheme) {
            val window = view.context.findActivity()?.window ?: return@LaunchedEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
