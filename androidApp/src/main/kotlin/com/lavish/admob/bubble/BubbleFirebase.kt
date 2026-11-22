package com.lavish.admob.bubble

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

private const val TAG = "EarningsBubble"

/**
 * Firebase wiring for the Earnings Bubble feature (Android only).
 * Uses free Spark-plan services: Auth, Realtime Database, Remote Config, Analytics.
 */
object BubbleFirebase {

    /**
     * TESTING ONLY: overrides the trial length (minutes) so In-App Purchase can be tested quickly.
     * ⚠️ Set to null before releasing to production (then Remote Config `bubble_trial_hours` applies).
     */
    val TEST_TRIAL_MINUTES: Long? = null

    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    /** Links the existing Google Sign-In account to Firebase Auth so we get a stable uid. */
    suspend fun linkGoogleAccount(account: GoogleSignInAccount): Boolean {
        val idToken = account.idToken ?: run {
            Log.w(TAG, "No idToken on Google account; cannot sign in to Firebase")
            return false
        }
        val current = auth.currentUser
        if (current != null && current.email == account.email) return true
        return try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sign-in failed: ${e.message}")
            false
        }
    }

    fun signOut() = auth.signOut()

    // ---------------- Remote Config ----------------

    object Keys {
        const val FEATURE_ENABLED = "bubble_feature_enabled"
        const val TRIAL_HOURS = "bubble_trial_hours"
        const val PRICE_LABEL = "bubble_price_label"
        const val PROMO_ENABLED = "promo_enabled"
        const val PROMO_MIN_APP_OPENS = "promo_min_app_opens"
        const val PROMO_MAX_SHOWS = "promo_max_shows"
        const val PROMO_COOLDOWN_DAYS = "promo_cooldown_days"
        const val PROMO_TITLE = "promo_title"
        const val PROMO_CTA = "promo_cta"
    }

    private val defaults: Map<String, Any> = mapOf(
        Keys.FEATURE_ENABLED to true,
        Keys.TRIAL_HOURS to 24L,
        Keys.PRICE_LABEL to "$1",
        Keys.PROMO_ENABLED to true,
        Keys.PROMO_MIN_APP_OPENS to 2L,
        Keys.PROMO_MAX_SHOWS to 3L,
        Keys.PROMO_COOLDOWN_DAYS to 3L,
        Keys.PROMO_TITLE to "Your earnings, always on screen",
        Keys.PROMO_CTA to "Start free 24h trial"
    )

    val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build()
            )
            setDefaultsAsync(defaults)
        }
    }

    suspend fun fetchRemoteConfig() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            Log.w(TAG, "Remote Config fetch failed, using defaults: ${e.message}")
        }
    }

    fun config(): BubbleConfig = with(remoteConfig) {
        BubbleConfig(
            featureEnabled = getBoolean(Keys.FEATURE_ENABLED),
            trialHours = getLong(Keys.TRIAL_HOURS).coerceAtLeast(1),
            trialMinutesOverride = TEST_TRIAL_MINUTES,
            priceLabel = getString(Keys.PRICE_LABEL),
            promoEnabled = getBoolean(Keys.PROMO_ENABLED),
            promoMinAppOpens = getLong(Keys.PROMO_MIN_APP_OPENS).toInt(),
            promoMaxShows = getLong(Keys.PROMO_MAX_SHOWS).toInt(),
            promoCooldownDays = getLong(Keys.PROMO_COOLDOWN_DAYS).toInt(),
            promoTitle = getString(Keys.PROMO_TITLE),
            promoCta = getString(Keys.PROMO_CTA)
        ).let { c ->
            if (TEST_TRIAL_MINUTES != null) c.copy(promoCta = "Start free ${c.trialLabel} trial") else c
        }
    }

    // ---------------- Analytics ----------------

    fun log(context: Context, event: String, params: Map<String, Any?> = emptyMap()) {
        val bundle = Bundle().apply {
            params.forEach { (k, v) ->
                when (v) {
                    is Long -> putLong(k, v)
                    is Int -> putLong(k, v.toLong())
                    is Double -> putDouble(k, v)
                    is Boolean -> putString(k, v.toString())
                    null -> Unit
                    else -> putString(k, v.toString())
                }
            }
        }
        FirebaseAnalytics.getInstance(context).logEvent(event, bundle)
    }
}

data class BubbleConfig(
    val featureEnabled: Boolean = true,
    val trialHours: Long = 24,
    val trialMinutesOverride: Long? = null,
    val priceLabel: String = "$1",
    val promoEnabled: Boolean = true,
    val promoMinAppOpens: Int = 2,
    val promoMaxShows: Int = 3,
    val promoCooldownDays: Int = 3,
    val promoTitle: String = "Your earnings, always on screen",
    val promoCta: String = "Start free 24h trial"
) {
    val trialDurationMs: Long
        get() = trialMinutesOverride?.let { it * 60_000L } ?: (trialHours * 3_600_000L)

    /** Human label, e.g. "24 hours", "15 minutes". */
    val trialLabel: String
        get() = trialMinutesOverride?.let { "$it minutes" }
            ?: if (trialHours == 1L) "1 hour" else "$trialHours hours"
}

/** Analytics event names (also mirror these in Mixpanel later if desired). */
object BubbleEvents {
    const val PROMO_SHOWN = "bubble_promo_shown"
    const val PROMO_CTA_TRIAL = "bubble_promo_cta_trial"
    const val PROMO_CTA_BUY = "bubble_promo_cta_buy"
    const val PROMO_LATER = "bubble_promo_later"
    const val PROMO_NEVER = "bubble_promo_never"
    const val TRIAL_STARTED = "bubble_trial_started"
    const val OVERLAY_GRANTED = "bubble_overlay_permission_granted"
    const val OVERLAY_DENIED = "bubble_overlay_permission_denied"
    const val PURCHASE_SUCCESS = "bubble_purchase_success"
    const val PURCHASE_FAILED = "bubble_purchase_failed"
    const val PURCHASE_CANCELLED = "bubble_purchase_cancelled"
}
