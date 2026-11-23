package com.lavish.admob.bubble

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "EarningsBubble"

/**
 * Firebase wiring for the Earnings Bubble feature (Android only).
 * Uses free Spark-plan services: Auth, Realtime Database, Remote Config, Analytics.
 */
object BubbleFirebase {

    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseDatabase.getInstance()

    private val _configState = MutableStateFlow(BubbleConfig())

    /** True once /config has actually been read from the server (or restored from the last good copy). */
    private val _configLoaded = MutableStateFlow(false)
    val configLoaded: StateFlow<Boolean> = _configLoaded.asStateFlow()

    private const val CFG_PREFS = "earnings_bubble_config"
    private var cfgPrefs: android.content.SharedPreferences? = null
    private var configListener: ValueEventListener? = null
    val configState: StateFlow<BubbleConfig> = _configState.asStateFlow()

    fun config(): BubbleConfig = _configState.value

    private var listeningConfig = false

    /**
     * Listens to the `/config` node in Firebase Realtime Database.
     * Admin can modify `trialHours`, `trialMinutes`, etc. in Firebase Console,
     * protected by Realtime Database rules (.write: false for regular clients).
     */
    fun startListeningConfig(context: Context? = null) {
        if (context != null && cfgPrefs == null) {
            cfgPrefs = context.applicationContext.getSharedPreferences(CFG_PREFS, Context.MODE_PRIVATE)
            restoreCachedConfig()
        }
        if (listeningConfig) return
        listeningConfig = true

        // /config needs auth (rules: auth != null). Attach only when signed in and re-attach on
        // every sign-in: a denied listener is cancelled by RTDB for good and never retries.
        auth.addAuthStateListener { a ->
            if (a.currentUser != null) attachConfigListener() else detachConfigListener()
        }
    }

    private fun detachConfigListener() {
        configListener?.let { db.getReference("config").removeEventListener(it) }
        configListener = null
    }

    private fun attachConfigListener() {
        detachConfigListener()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Accept numbers stored as either numbers or strings in the console
                fun num(key: String): Long? = snapshot.child(key).value?.toString()?.trim()?.toDoubleOrNull()?.toLong()
                val hours = num("trialHours")
                val minutes = num("trialMinutes")
                val price = snapshot.child("priceLabel").getValue(String::class.java)
                val promoEnabled = snapshot.child("promoEnabled").getValue(Boolean::class.java)

                val current = _configState.value
                val updatedHours = hours ?: current.trialHours
                val updatedMinutes = minutes
                val updated = current.copy(
                    trialHours = updatedHours,
                    trialMinutes = updatedMinutes,
                    priceLabel = price ?: current.priceLabel,
                    promoEnabled = promoEnabled ?: current.promoEnabled
                )
                _configState.value = updated.copy(
                    promoCta = "Start free ${updated.trialLabel} trial"
                )
                _configLoaded.value = true
                cfgPrefs?.edit()
                    ?.putLong("trialHours", updatedHours)
                    ?.putLong("trialMinutes", updatedMinutes ?: -1L)
                    ?.apply()
                Log.d(TAG, "Loaded /config: trialHours=$updatedHours trialMinutes=$updatedMinutes")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "RTDB /config read failed: ${error.message}")
                configListener = null // allow the next auth change to re-attach
            }
        }
        configListener = listener
        db.getReference("config").addValueEventListener(listener)
    }

    /** Use the last config we got from the server until a fresh one arrives (offline / cold start). */
    private fun restoreCachedConfig() {
        val p = cfgPrefs ?: return
        if (!p.contains("trialHours")) return
        val minutes = p.getLong("trialMinutes", -1L).takeIf { it > 0 }
        val updated = _configState.value.copy(trialHours = p.getLong("trialHours", 24L), trialMinutes = minutes)
        _configState.value = updated.copy(promoCta = "Start free ${updated.trialLabel} trial")
        _configLoaded.value = true
    }

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
        Keys.PROMO_CTA to "Start free trial"
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
            val current = _configState.value
            _configState.value = current.copy(
                featureEnabled = remoteConfig.getBoolean(Keys.FEATURE_ENABLED),
                promoMinAppOpens = remoteConfig.getLong(Keys.PROMO_MIN_APP_OPENS).toInt(),
                promoMaxShows = remoteConfig.getLong(Keys.PROMO_MAX_SHOWS).toInt(),
                promoCooldownDays = remoteConfig.getLong(Keys.PROMO_COOLDOWN_DAYS).toInt(),
                promoTitle = remoteConfig.getString(Keys.PROMO_TITLE)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Remote Config fetch failed, using defaults: ${e.message}")
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
    val trialMinutes: Long? = null,
    val priceLabel: String = "$1",
    val promoEnabled: Boolean = true,
    val promoMinAppOpens: Int = 2,
    val promoMaxShows: Int = 3,
    val promoCooldownDays: Int = 3,
    val promoTitle: String = "Your earnings, always on screen",
    val promoCta: String = "Start free trial"
) {
    val trialDurationMs: Long
        get() = if (trialMinutes != null && trialMinutes > 0) {
            trialMinutes * 60_000L
        } else {
            trialHours * 3_600_000L
        }

    /** Human label, e.g. "3 days", "24 hours", "15 minutes". */
    val trialLabel: String
        get() = when {
            trialMinutes != null && trialMinutes > 0 -> "$trialMinutes minute" + (if (trialMinutes == 1L) "" else "s")
            trialHours == 1L -> "1 hour"
            trialHours % 24L == 0L -> "${trialHours / 24L} day" + (if (trialHours / 24L == 1L) "" else "s")
            else -> "$trialHours hours"
        }
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
