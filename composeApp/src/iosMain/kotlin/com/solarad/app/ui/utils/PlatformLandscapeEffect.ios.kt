package com.solarad.app.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication
import platform.UIKit.setStatusBarHidden

@Composable
actual fun PlatformLandscapeEffect(enabled: Boolean, keepScreenOn: Boolean) {
    DisposableEffect(enabled, keepScreenOn) {
        if (enabled) {
            UIApplication.sharedApplication.setStatusBarHidden(true, animated = true)
            if (keepScreenOn) {
                UIApplication.sharedApplication.idleTimerDisabled = true
            }
            onDispose {
                UIApplication.sharedApplication.setStatusBarHidden(false, animated = true)
                if (keepScreenOn) {
                    UIApplication.sharedApplication.idleTimerDisabled = false
                }
            }
        } else {
            onDispose {}
        }
    }
}
