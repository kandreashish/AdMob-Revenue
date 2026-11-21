package com.solarad.app.analytics

import java.util.UUID

actual fun getPlatformName(): String {
    val osName = System.getProperty("os.name") ?: "JVM"
    val osVersion = System.getProperty("os.version") ?: ""
    return "Desktop ($osName $osVersion)".trim()
}

actual fun generateRandomId(): String = UUID.randomUUID().toString()
