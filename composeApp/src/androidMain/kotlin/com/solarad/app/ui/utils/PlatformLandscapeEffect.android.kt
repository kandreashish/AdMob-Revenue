package com.solarad.app.ui.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun PlatformLandscapeEffect(enabled: Boolean, keepScreenOn: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    DisposableEffect(enabled, keepScreenOn) {
        val activity = view.context.findActivity() ?: return@DisposableEffect onDispose {}

        if (enabled) {
            val originalOrientation = activity.requestedOrientation
            val window = activity.window

            // Force landscape orientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            // Hide system status and navigation bars for distraction-free full screen
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())

            // Optional: keep screen awake
            if (keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            onDispose {
                // Restore whatever orientation the activity had before (avoids an extra forced recreate)
                activity.requestedOrientation = originalOrientation
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                if (keepScreenOn) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        } else {
            onDispose {}
        }
    }
}
