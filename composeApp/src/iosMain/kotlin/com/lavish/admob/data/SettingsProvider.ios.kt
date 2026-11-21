package com.lavish.admob.data

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createDefaultSettings(): Settings? {
    return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
}
