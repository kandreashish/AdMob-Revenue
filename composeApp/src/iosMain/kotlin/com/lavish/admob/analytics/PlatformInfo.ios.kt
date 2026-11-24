package com.lavish.admob.analytics

import platform.Foundation.NSUUID
import platform.UIKit.UIDevice

actual fun getPlatformName(): String {
    val device = UIDevice.currentDevice
    return "iOS ${device.systemName} ${device.systemVersion}"
}

actual fun generateRandomId(): String = NSUUID().UUIDString()

actual fun isMixpanelSupported(): Boolean = true
