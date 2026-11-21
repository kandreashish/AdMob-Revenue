package com.lavish.admob.data

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun createDefaultSettings(): Settings? {
    return try {
        val prefs = Preferences.userRoot().node("com.lavish.admob")
        PreferencesSettings(prefs)
    } catch (_: Throwable) {
        null
    }
}
