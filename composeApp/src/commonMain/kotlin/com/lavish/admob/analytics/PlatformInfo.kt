package com.lavish.admob.analytics

/**
 * Multiplatform information provider for analytics metadata.
 */
expect fun getPlatformName(): String

expect fun generateRandomId(): String

/**
 * Whether Mixpanel analytics tracking is supported and enabled on this platform.
 * Enabled on Android and iOS; disabled on Desktop JVM.
 */
expect fun isMixpanelSupported(): Boolean
