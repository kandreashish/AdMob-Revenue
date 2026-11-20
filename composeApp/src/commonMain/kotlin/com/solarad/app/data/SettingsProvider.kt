package com.solarad.app.data

import com.russhwolf.settings.Settings

/**
 * Platform-specific Settings provider for persisting session and configuration.
 */
expect fun createDefaultSettings(): Settings?
