package com.lavish.admob.data

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private var appContext: Context? = null

internal fun androidAppContext(): Context? = appContext

fun initAndroidSettings(context: Context) {
    appContext = context.applicationContext
}

fun createAndroidSettings(context: Context): Settings {
    val prefs = context.applicationContext.getSharedPreferences("solarad_prefs", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(prefs)
}

actual fun createDefaultSettings(): Settings? {
    return appContext?.let { createAndroidSettings(it) }
}
