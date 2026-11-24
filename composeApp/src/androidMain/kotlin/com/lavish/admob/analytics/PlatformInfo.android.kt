package com.lavish.admob.analytics

import android.os.Build
import java.util.UUID

actual fun getPlatformName(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

actual fun generateRandomId(): String = UUID.randomUUID().toString()

actual fun isMixpanelSupported(): Boolean = true
