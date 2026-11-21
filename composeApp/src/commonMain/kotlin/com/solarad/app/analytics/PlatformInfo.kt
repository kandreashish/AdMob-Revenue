package com.solarad.app.analytics

/**
 * Multiplatform information provider for analytics metadata.
 */
expect fun getPlatformName(): String

expect fun generateRandomId(): String
