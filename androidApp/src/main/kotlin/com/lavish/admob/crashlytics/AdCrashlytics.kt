package com.lavish.admob.crashlytics

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Lightweight facade for Firebase Crashlytics crash reporting and diagnostic logging.
 */
object AdCrashlytics {
    private const val TAG = "AdCrashlytics"

    private val crashlytics: FirebaseCrashlytics?
        get() = try {
            FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Crashlytics not available: ${e.message}")
            null
        }

    /**
     * Initializes default keys and settings for Crashlytics.
     */
    fun init(versionName: String, versionCode: Int, isDebug: Boolean) {
        try {
            val inst = crashlytics ?: return
            // Enable crash collection in both debug and release, or configure as desired
            inst.isCrashlyticsCollectionEnabled = true
            inst.setCustomKey("app_version_name", versionName)
            inst.setCustomKey("app_version_code", versionCode)
            inst.setCustomKey("is_debug_build", isDebug)
            inst.log("AdDash initialized (v$versionName-$versionCode)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to init Crashlytics: ${e.message}")
        }
    }

    /**
     * Sets the user identifier (e.g. publisher email) so reports are attributed properly.
     */
    fun setUserId(userId: String?) {
        try {
            crashlytics?.setUserId(userId ?: "")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set Crashlytics user ID: ${e.message}")
        }
    }

    /**
     * Clears user identifier on sign out.
     */
    fun clearUserId() {
        setUserId("")
    }

    /**
     * Logs a diagnostic breadcrumb message attached to any subsequent crash report.
     */
    fun log(message: String) {
        try {
            crashlytics?.log(message)
        } catch (_: Exception) {}
    }

    /**
     * Sets a custom string key.
     */
    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (_: Exception) {}
    }

    /**
     * Sets a custom boolean key.
     */
    fun setCustomKey(key: String, value: Boolean) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (_: Exception) {}
    }

    /**
     * Sets a custom integer key.
     */
    fun setCustomKey(key: String, value: Int) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (_: Exception) {}
    }

    /**
     * Records a non-fatal exception without crashing the app.
     */
    fun recordException(throwable: Throwable) {
        try {
            crashlytics?.recordException(throwable)
        } catch (_: Exception) {}
    }
}
