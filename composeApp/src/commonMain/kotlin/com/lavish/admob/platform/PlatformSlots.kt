package com.lavish.admob.platform

import androidx.compose.runtime.Composable

/**
 * Optional UI slots a platform can fill (e.g. Android's Earnings Bubble).
 * They stay null on iOS/desktop, so nothing is rendered there.
 */
object PlatformSlots {
    /** Card shown on the Today screen below the time-range selector. */
    var todayBanner: (@Composable () -> Unit)? = null

    /** Row shown in Profile & Settings, above Appearance. */
    var settingsRow: (@Composable () -> Unit)? = null
}
