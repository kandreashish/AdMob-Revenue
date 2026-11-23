package com.lavish.admob.data

import com.russhwolf.settings.Settings
import com.lavish.admob.model.AccountSummary
import com.lavish.admob.model.AnalyticsRange
import com.lavish.admob.model.AnalyticsSummary
import com.lavish.admob.model.AppMonetization
import com.lavish.admob.model.AutoRefreshInterval
import com.lavish.admob.model.ThemeMode
import com.lavish.admob.model.TimeRange
import com.lavish.admob.model.UserSession
import com.lavish.admob.model.WalletSummary
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.lavish.admob.analytics.MixpanelTracker
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AdMobRepository(
    private val settings: Settings? = createDefaultSettings(),
    /** Large blobs (report history) live in files, not Settings. null → fall back to Settings (tests). */
    private val cacheFiles: CacheFileStore? = createCacheFileStore(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    val tracker: MixpanelTracker = MixpanelTracker.instance
) {
    companion object {
        private const val KEY_IS_LOGGED_IN = "solarad_is_logged_in"
        private const val KEY_USER_NAME = "solarad_user_name"
        private const val KEY_USER_EMAIL = "solarad_user_email"
        private const val KEY_PUBLISHER_ID = "solarad_publisher_id"
        private const val KEY_ACCESS_TOKEN = "solarad_access_token"
        private const val KEY_AUTO_REFRESH_INTERVAL = "solarad_auto_refresh_interval"
        private const val KEY_THEME_MODE = "solarad_theme_mode"
        private const val KEY_USER_AVATAR_URL = "solarad_user_avatar_url"
        private const val KEY_CACHE_SUMMARY = "solarad_cache_summary"
        private const val KEY_CACHE_ALL_APPS = "solarad_cache_all_apps"
        private const val KEY_CACHE_ANALYTICS = "solarad_cache_analytics"
        private const val KEY_CACHE_WALLET = "solarad_cache_wallet"
        private const val KEY_CACHE_REPORT_ROWS = "solarad_cache_report_rows"
        private const val KEY_CACHE_REGISTERED_APPS = "solarad_cache_registered_apps"
        private const val KEY_CACHE_TIMESTAMP = "solarad_cache_timestamp"
        private const val KEY_CUSTOM_DAILY_GOAL = "solarad_custom_daily_goal"
        private const val KEY_REFRESH_TOKEN = "solarad_oauth_refresh_token"
        private const val KEY_REPORTING_TZ = "solarad_reporting_timezone"
        private const val KEY_CACHE_ROWS_HASH = "solarad_cache_report_rows_hash"
        private const val KEY_CACHE_REG_APPS_HASH = "solarad_cache_registered_apps_hash"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private inline fun <reified T> loadCachedList(key: String): List<T> {
        val raw = settings?.getStringOrNull(key) ?: return emptyList()
        return try {
            json.decodeFromString<List<T>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private inline fun <reified T> loadCachedObject(key: String, default: () -> T): T {
        val raw = settings?.getStringOrNull(key) ?: return default()
        return try {
            json.decodeFromString<T>(raw)
        } catch (_: Exception) {
            default()
        }
    }

    private val apiClient = AdMobApiClient()
    @kotlin.concurrent.Volatile private var currentAccessToken: String? = settings?.getStringOrNull(KEY_ACCESS_TOKEN)

    // Large caches (full report history) are decoded off the main thread so the first frame isn't blocked.
    // The small, pre-aggregated caches below (summary/apps/analytics/wallet) stay synchronous so
    // the Today screen still renders instantly with real numbers.
    @kotlin.concurrent.Volatile private var cachedReportRows: List<AdMobReportRow> = emptyList()
    private val _reportRows = MutableStateFlow<List<AdMobReportRow>>(emptyList())
    val reportRows: StateFlow<List<AdMobReportRow>> = _reportRows.asStateFlow()

    @kotlin.concurrent.Volatile private var cachedRegisteredApps: List<AppMonetization> = emptyList()

    /** Completes once the heavy disk caches are decoded. Anything that reads/replaces them awaits this first. */
    private val diskCacheLoaded = kotlinx.coroutines.CompletableDeferred<Unit>()

    /**
     * Reads a large cached list from its file. If only the legacy Settings copy exists (pre-migration
     * install), it is decoded, moved into a file and removed from Settings so prefs stay small.
     */
    private inline fun <reified T> loadLargeList(key: String): List<T> {
        val files = cacheFiles ?: return loadCachedList(key)
        files.read(key)?.let { raw ->
            return try { json.decodeFromString<List<T>>(raw) } catch (_: Exception) { emptyList() }
        }
        val legacy = settings?.getStringOrNull(key) ?: return emptyList()
        val decoded = try { json.decodeFromString<List<T>>(legacy) } catch (_: Exception) { emptyList() }
        if (files.write(key, legacy)) settings.remove(key)
        return decoded
    }

    /** Writes a large blob to its file (or Settings when no file store). Returns true on success. */
    private fun storeLarge(key: String, content: String): Boolean {
        val files = cacheFiles
        if (files == null) {
            settings?.putString(key, content); return settings != null
        }
        val ok = files.write(key, content)
        if (ok) settings?.remove(key)
        return ok
    }

    private fun hasLarge(key: String): Boolean =
        cacheFiles?.exists(key) == true || settings?.hasKey(key) == true

    init {
        scope.launch {
            try {
                val rows = loadLargeList<AdMobReportRow>(KEY_CACHE_REPORT_ROWS)
                val registered = loadLargeList<AppMonetization>(KEY_CACHE_REGISTERED_APPS)
                cachedReportRows = rows
                cachedRegisteredApps = registered
                _reportRows.value = rows
            } finally {
                diskCacheLoaded.complete(Unit)
            }
        }
    }
    @kotlin.concurrent.Volatile private var activePublisherId: String = settings?.getString(KEY_PUBLISHER_ID, "") ?: ""

    // AdMob reports are dated in the account's reporting time zone. All "today" math must use it,
    // otherwise Today/Yesterday shift by a day for users far from the account TZ (e.g. IST vs PST).
    @kotlin.concurrent.Volatile private var reportingTimeZoneId: String? = settings?.getStringOrNull(KEY_REPORTING_TZ)

    /** Bumped on logout so in-flight fetches from the previous session are discarded. */
    @kotlin.concurrent.Volatile private var sessionGeneration: Int = 0

    fun accountToday(): LocalDate {
        val tz = reportingTimeZoneId?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: TimeZone.currentSystemDefault()
        return Clock.System.now().toLocalDateTime(tz).date
    }

    private val _session = MutableStateFlow(
        if (settings?.getBoolean(KEY_IS_LOGGED_IN, false) == true) {
            UserSession(
                isLoggedIn = true,
                userName = settings.getString(KEY_USER_NAME, "AdMob Publisher"),
                email = settings.getString(KEY_USER_EMAIL, ""),
                publisherId = settings.getString(KEY_PUBLISHER_ID, ""),
                avatarUrl = settings.getStringOrNull(KEY_USER_AVATAR_URL)
            )
        } else {
            UserSession(
                isLoggedIn = false,
                userName = "",
                email = "",
                publisherId = "",
                avatarUrl = null
            )
        }
    )
    val session: StateFlow<UserSession> = _session.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow(TimeRange.TODAY)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    private val _customDailyGoal = MutableStateFlow<Double?>(
        settings?.getDoubleOrNull(KEY_CUSTOM_DAILY_GOAL)?.takeIf { it > 0.0 }
    )
    val customDailyGoal: StateFlow<Double?> = _customDailyGoal.asStateFlow()

    private val _accountSummary = MutableStateFlow(
        loadCachedObject<AccountSummary>(KEY_CACHE_SUMMARY) { createEmptySummary() }
    )
    val accountSummary: StateFlow<AccountSummary> = _accountSummary.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppMonetization>>(
        loadCachedList(KEY_CACHE_ALL_APPS)
    )
    val allApps: StateFlow<List<AppMonetization>> = _allApps.asStateFlow()

    private val _selectedAnalyticsRange = MutableStateFlow(AnalyticsRange.DAYS_7)
    val selectedAnalyticsRange: StateFlow<AnalyticsRange> = _selectedAnalyticsRange.asStateFlow()

    private val _analytics = MutableStateFlow(
        loadCachedObject<AnalyticsSummary>(KEY_CACHE_ANALYTICS) { createEmptyAnalytics() }
    )
    val analytics: StateFlow<AnalyticsSummary> = _analytics.asStateFlow()

    private val _wallet = MutableStateFlow(
        loadCachedObject<WalletSummary>(KEY_CACHE_WALLET) { createEmptyWallet() }
    )
    val wallet: StateFlow<WalletSummary> = _wallet.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    val hasOfflineCache: Boolean
        get() = cachedReportRows.isNotEmpty() || _allApps.value.isNotEmpty()

    private val _autoRefreshInterval = MutableStateFlow(
        AutoRefreshInterval.fromSeconds(
            settings?.getInt(KEY_AUTO_REFRESH_INTERVAL, AutoRefreshInterval.MINUTE_1.seconds)
                ?: AutoRefreshInterval.MINUTE_1.seconds
        )
    )
    val autoRefreshInterval: StateFlow<AutoRefreshInterval> = _autoRefreshInterval.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.fromString(settings?.getStringOrNull(KEY_THEME_MODE))
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _lastRefreshTimestampMs = MutableStateFlow(
        settings?.getLong(KEY_CACHE_TIMESTAMP, Clock.System.now().toEpochMilliseconds())
            ?: Clock.System.now().toEpochMilliseconds()
    )
    val lastRefreshTimestampMs: StateFlow<Long> = _lastRefreshTimestampMs.asStateFlow()

    // Declared before init: the init block launches a fetch that uses these concurrently.
    var tokenRefresher: (suspend () -> String?)? = null
    @kotlin.concurrent.Volatile private var recomputeJob: kotlinx.coroutines.Job? = null
    private val recomputeMutex = Mutex()
    private val fetchMutex = Mutex()
    private val tokenRefreshMutex = Mutex()
    @kotlin.concurrent.Volatile private var refreshJob: kotlinx.coroutines.Job? = null
    @kotlin.concurrent.Volatile private var lastSnapshotSignature: String? = null
    private var customStartDate: kotlinx.datetime.LocalDate? = null
    private var customEndDate: kotlinx.datetime.LocalDate? = null

    init {
        if (_session.value.isLoggedIn) {
            val email = _session.value.email
            val pubId = _session.value.publisherId
            val distinctId = if (email.isNotBlank()) email else pubId
            if (distinctId.isNotBlank()) {
                tracker.identify(
                    userId = distinctId,
                    properties = mapOf(
                        "\$name" to _session.value.userName,
                        "\$email" to email,
                        "\$avatar" to (_session.value.avatarUrl ?: ""),
                        "publisher_id" to pubId
                    )
                )
            }
            val initialSummary = _accountSummary.value
            if (initialSummary.todayRevenue > 0.0) {
                tracker.trackRevenueSnapshot(
                    todayRevenue = initialSummary.todayRevenue,
                    yesterdayRevenue = initialSummary.yesterdayRevenue,
                    dailyGoal = initialSummary.dailyGoal,
                    dailyGoalProgress = initialSummary.dailyGoalProgress,
                    deltaAmount = initialSummary.deltaAmount,
                    deltaPercentage = initialSummary.deltaPercentage,
                    ecpm = initialSummary.ecpm,
                    impressions = initialSummary.impressions,
                    clicks = initialSummary.clicks,
                    currency = initialSummary.currencyCode.ifBlank { "USD" },
                    appsCount = _allApps.value.size,
                    topEarningAppName = _allApps.value.maxByOrNull { it.todayRevenue }?.name
                )
            }
        }

        // If restored from persisted session with an access token, fetch real data in background
        if (_session.value.isLoggedIn && !currentAccessToken.isNullOrBlank()) {
            refreshJob = scope.launch {
                val token = currentAccessToken ?: return@launch
                fetchAndApplyRealData(token, _session.value.userName, _session.value.email)
            }
        }
    }


    /** Cancels all background work owned by this repository (call when its owner is destroyed). */
    fun close() {
        scope.cancel()
    }

    fun getCurrentToken(): String? = currentAccessToken

    fun updateAccessToken(newToken: String) {
        val cleanToken = newToken.trim()
        if (cleanToken.isNotBlank()) {
            currentAccessToken = cleanToken
            settings?.putString(KEY_ACCESS_TOKEN, cleanToken)
            _authError.value = null
        }
    }

    fun getStoredRefreshToken(): String? = settings?.getStringOrNull(KEY_REFRESH_TOKEN)

    fun saveRefreshToken(token: String) {
        val cleanToken = token.trim()
        if (cleanToken.isNotBlank()) {
            settings?.putString(KEY_REFRESH_TOKEN, cleanToken)
        }
    }

    fun clearRefreshToken() {
        settings?.remove(KEY_REFRESH_TOKEN)
    }

    fun getCustomClientId(): String? = settings?.getStringOrNull("solarad_custom_client_id")

    fun saveCustomClientId(id: String) {
        val clean = id.trim()
        if (clean.isNotBlank()) {
            settings?.putString("solarad_custom_client_id", clean)
        } else {
            settings?.remove("solarad_custom_client_id")
        }
    }

    fun getCustomClientSecret(): String? = settings?.getStringOrNull("addash_custom_client_secret")

    fun saveCustomClientSecret(secret: String) {
        val clean = secret.trim()
        if (clean.isNotBlank()) {
            settings?.putString("addash_custom_client_secret", clean)
        } else {
            settings?.remove("addash_custom_client_secret")
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun setAuthError(message: String) {
        _authError.value = message
    }

    fun setAutoRefreshInterval(interval: AutoRefreshInterval) {
        _autoRefreshInterval.value = interval
        settings?.putInt(KEY_AUTO_REFRESH_INTERVAL, interval.seconds)
        tracker.trackAutoRefreshIntervalChanged(interval.seconds)
    }

    fun setCustomDailyGoal(goal: Double?) {
        if (goal != null && goal > 0.0) {
            _customDailyGoal.value = goal
            settings?.putDouble(KEY_CUSTOM_DAILY_GOAL, goal)
            tracker.trackDailyGoalUpdated(goal, _session.value.currencySymbol)
        } else {
            _customDailyGoal.value = null
            settings?.remove(KEY_CUSTOM_DAILY_GOAL)
        }
        recomputeSummary(persist = true)
    }


    /**
     * Heavy aggregation runs on Dispatchers.Default. Each new request waits for the previous one
     * (cancelAndJoin) so an older computation can never publish after a newer one.
     */
    private fun recomputeSummary(persist: Boolean = false) {
        val previous = recomputeJob
        recomputeJob = scope.launch {
            previous?.cancelAndJoin()
            diskCacheLoaded.await()
            recomputeMutex.withLock { recomputeSummaryNow(persist) }
        }
    }

    private fun recomputeSummaryNow(persist: Boolean) {
        if (cachedReportRows.isNotEmpty()) {
            val range = _selectedTimeRange.value
            val today = accountToday()
            val summary = AdMobApiClient.computeAccountSummary(
                rows = cachedReportRows,
                range = range,
                today = today,
                publisherId = activePublisherId.ifBlank { "AdMob" },
                currencyCode = _session.value.currencyCode,
                currencySymbol = _session.value.currencySymbol,
                customStartDate = customStartDate,
                customEndDate = customEndDate,
                customDailyGoal = _customDailyGoal.value
            )
            _accountSummary.value = if (_isOffline.value) {
                summary.copy(lastSyncTime = "Offline • Cached Data")
            } else {
                summary
            }
            _allApps.value = AdMobApiClient.computeAppsMonetization(
                rows = cachedReportRows,
                registeredApps = cachedRegisteredApps,
                today = today,
                range = range,
                customStartDate = customStartDate,
                customEndDate = customEndDate
            )
            _analytics.value = AdMobApiClient.computeAnalytics(
                rows = cachedReportRows,
                range = _selectedAnalyticsRange.value,
                today = today
            )
        } else {
            val current = _accountSummary.value
            val isCustom = _customDailyGoal.value != null && _customDailyGoal.value!! > 0.0
            val baseDaily = if (isCustom) _customDailyGoal.value!! else 100.0
            val range = _selectedTimeRange.value
            val periodDays = when (range) {
                TimeRange.TODAY, TimeRange.YESTERDAY -> 1
                TimeRange.LAST_7_DAYS -> 7
                TimeRange.THIS_MONTH -> AdMobApiClient.daysInMonth(accountToday())
                TimeRange.DAYS_30 -> 30
                TimeRange.CUSTOM -> {
                    val today = accountToday()
                    val start = customStartDate ?: today.minus(29, DateTimeUnit.DAY)
                    val end = customEndDate ?: today
                    maxOf(1L, (end.toEpochDays() - start.toEpochDays()) + 1L).toInt()
                }
            }
            val periodGoal = baseDaily * periodDays
            val progress = if (periodGoal > 0.0 && current.todayRevenue > 0.0) {
                (current.todayRevenue / periodGoal).toFloat().coerceAtLeast(0f)
            } else 0f
            _accountSummary.value = current.copy(
                dailyGoal = baseDaily,
                periodGoal = periodGoal,
                periodDays = periodDays,
                dailyGoalProgress = progress,
                isCustomGoal = isCustom
            )
        }
        if (!persist) return
        try {
            settings?.putString(KEY_CACHE_SUMMARY, json.encodeToString(_accountSummary.value))
            settings?.putString(KEY_CACHE_ALL_APPS, json.encodeToString(_allApps.value))
            settings?.putString(KEY_CACHE_ANALYTICS, json.encodeToString(_analytics.value))
        } catch (_: Exception) {}
    }

    fun recordRefreshTimestamp() {
        _lastRefreshTimestampMs.value = Clock.System.now().toEpochMilliseconds()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        settings?.putString(KEY_THEME_MODE, mode.name)
        tracker.trackThemeChanged(mode.name)
    }

    fun updateAvatarUrl(avatarUrl: String) {
        val cleanUrl = avatarUrl.trim()
        if (cleanUrl.isNotBlank() && _session.value.avatarUrl != cleanUrl) {
            _session.value = _session.value.copy(avatarUrl = cleanUrl)
            settings?.putString(KEY_USER_AVATAR_URL, cleanUrl)
        }
    }

    fun loginWithOAuthToken(name: String, email: String, token: String, avatarUrl: String? = null) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            val err = "OAuth authorization failed: Empty token received."
            _authError.value = err
            tracker.trackLoginFailed("Google OAuth", err)
            return
        }

        tracker.trackLoginInitiated("Google OAuth")
        _isRefreshing.value = true
        _authError.value = null
        _lastRefreshTimestampMs.value = Clock.System.now().toEpochMilliseconds()
        currentAccessToken = cleanToken

        val resolvedName = name.ifBlank { "AdMob Publisher" }
        val resolvedEmail = email.ifBlank { "" }
        val resolvedAvatar = avatarUrl ?: _session.value.avatarUrl

        _session.value = UserSession(
            isLoggedIn = true,
            userName = resolvedName,
            email = resolvedEmail,
            publisherId = if (activePublisherId.isNotBlank()) activePublisherId else "Connecting AdMob...",
            avatarUrl = resolvedAvatar
        )

        resolvedAvatar?.let { settings?.putString(KEY_USER_AVATAR_URL, it) }

        scope.launch {
            try {
                fetchAndApplyRealData(cleanToken, resolvedName, resolvedEmail, resolvedAvatar, isLogin = true)
            } catch (e: Exception) {
                val err = "AdMob API Error: ${e.message}"
                _authError.value = err
                tracker.trackLoginFailed("Google OAuth", err)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loginWithGoogleProfile(name: String, email: String, idToken: String? = null, avatarUrl: String? = null) {
        if (name.isBlank() && email.isBlank()) {
            val err = "Authentication cancelled or invalid Google account profile."
            _authError.value = err
            tracker.trackLoginFailed("Google Profile", err)
            return
        }

        tracker.trackLoginInitiated("Google Profile")
        _isRefreshing.value = true
        _authError.value = null
        _lastRefreshTimestampMs.value = Clock.System.now().toEpochMilliseconds()
        currentAccessToken = idToken

        val resolvedName = name.ifBlank { "AdMob Publisher" }
        val resolvedEmail = email.ifBlank { "" }
        val resolvedAvatar = avatarUrl ?: _session.value.avatarUrl

        _session.value = UserSession(
            isLoggedIn = true,
            userName = resolvedName,
            email = resolvedEmail,
            publisherId = if (activePublisherId.isNotBlank()) activePublisherId else "Connecting...",
            avatarUrl = resolvedAvatar
        )

        settings?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_NAME, resolvedName)
            putString(KEY_USER_EMAIL, resolvedEmail)
            resolvedAvatar?.let { putString(KEY_USER_AVATAR_URL, it) }
            if (!idToken.isNullOrBlank()) {
                putString(KEY_ACCESS_TOKEN, idToken)
            }
        }

        if (!idToken.isNullOrBlank()) {
            scope.launch {
                try {
                    fetchAndApplyRealData(idToken, fallbackAvatarUrl = resolvedAvatar, isLogin = true)
                } catch (e: Exception) {
                    val err = "Connection error: ${e.message ?: "Authentication failed"}"
                    _authError.value = err
                    tracker.trackLoginFailed("Google Profile", err)
                } finally {
                    _isRefreshing.value = false
                }
            }
        } else {
            _isRefreshing.value = false
        }
    }

    fun loginWithApiToken(token: String) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            val err = "Please enter a valid Google OAuth or API token."
            _authError.value = err
            tracker.trackLoginFailed("API Token", err)
            return
        }

        tracker.trackLoginInitiated("API Token")
        _isRefreshing.value = true
        _authError.value = null
        _lastRefreshTimestampMs.value = Clock.System.now().toEpochMilliseconds()

        scope.launch {
            try {
                currentAccessToken = cleanToken
                fetchAndApplyRealData(cleanToken, "AdMob Developer", "", isLogin = true)
            } catch (e: Exception) {
                currentAccessToken = null
                val err = "Authentication failed: ${e.message ?: "Network error"}"
                _authError.value = err
                tracker.trackLoginFailed("API Token", err)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun saveToOfflineCache(payload: RealAdMobPayload, token: String) {
        settings?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_NAME, payload.session.userName)
            putString(KEY_USER_EMAIL, payload.session.email)
            putString(KEY_PUBLISHER_ID, payload.session.publisherId)
            putString(KEY_ACCESS_TOKEN, token)
            payload.session.avatarUrl?.let { putString(KEY_USER_AVATAR_URL, it) }

            try {
                putString(KEY_CACHE_SUMMARY, json.encodeToString(payload.accountSummary))
                putString(KEY_CACHE_ALL_APPS, json.encodeToString(payload.apps))
                putString(KEY_CACHE_ANALYTICS, json.encodeToString(payload.analytics))
                putString(KEY_CACHE_WALLET, json.encodeToString(payload.wallet))
                // The report-rows blob can be several MB: only re-encode + rewrite it when the content changed.
                val rowsHash = payload.reportRows.hashCode()
                if (rowsHash != getInt(KEY_CACHE_ROWS_HASH, 0) || !hasLarge(KEY_CACHE_REPORT_ROWS)) {
                    if (storeLarge(KEY_CACHE_REPORT_ROWS, json.encodeToString(payload.reportRows))) {
                        putInt(KEY_CACHE_ROWS_HASH, rowsHash)
                    }
                }
                val appsHash = payload.registeredApps.hashCode()
                if (appsHash != getInt(KEY_CACHE_REG_APPS_HASH, 0) || !hasLarge(KEY_CACHE_REGISTERED_APPS)) {
                    if (storeLarge(KEY_CACHE_REGISTERED_APPS, json.encodeToString(payload.registeredApps))) {
                        putInt(KEY_CACHE_REG_APPS_HASH, appsHash)
                    }
                }
                putLong(KEY_CACHE_TIMESTAMP, Clock.System.now().toEpochMilliseconds())
            } catch (_: Exception) {
            }
        }
    }

    private fun isNetworkFailure(e: Throwable?): Boolean {
        if (e == null) return false
        val msg = (e.message ?: "").lowercase()
        val name = e::class.simpleName?.lowercase() ?: ""
        return msg.contains("network") ||
               msg.contains("timeout") ||
               msg.contains("connect") ||
               msg.contains("unreachable") ||
               msg.contains("offline") ||
               msg.contains("failed to connect") ||
               msg.contains("resolve host") ||
               msg.contains("unknownhost") ||
               msg.contains("socket") ||
               name.contains("socket") ||
               name.contains("connect") ||
               name.contains("host") ||
               name.contains("ioexception")
    }

    private fun isAuthExpiredError(msg: String): Boolean {
        val lower = msg.lowercase()
        return lower.contains("401") ||
               lower.contains("unauthorized") ||
               lower.contains("invalid authentication credentials") ||
               lower.contains("auth_token_stale") ||
               lower.contains("token expired") ||
               lower.contains("invalid_token") ||
               lower.contains("expected oauth 2 access token")
    }

    /** Serialises every network fetch so responses are applied strictly in order (no stale overwrite). */

    private suspend fun fetchAndApplyRealData(
        token: String,
        fallbackName: String = "",
        fallbackEmail: String = "",
        fallbackAvatarUrl: String? = null,
        isLogin: Boolean = false
    ) {
        diskCacheLoaded.await() // never let stale disk rows overwrite fresh network data
        fetchMutex.withLock {
            fetchAndApplyLocked(token, fallbackName, fallbackEmail, fallbackAvatarUrl, isLogin, allowAuthRetry = true)
        }
    }

    /** Single-flight token refresh: concurrent 401s share one refresher call. */
    private suspend fun refreshTokenOnce(staleToken: String): String? = tokenRefreshMutex.withLock {
        val current = currentAccessToken
        if (!current.isNullOrBlank() && current != staleToken) return@withLock current // someone already refreshed
        val fresh = try { tokenRefresher?.invoke() } catch (_: Exception) { null }
        if (!fresh.isNullOrBlank() && fresh != staleToken) updateAccessToken(fresh)
        fresh
    }

    private suspend fun fetchAndApplyLocked(
        token: String,
        fallbackName: String,
        fallbackEmail: String,
        fallbackAvatarUrl: String?,
        isLogin: Boolean,
        allowAuthRetry: Boolean
    ) {
        val resolvedName = fallbackName.ifBlank { _session.value.userName }
        val resolvedEmail = fallbackEmail.ifBlank { _session.value.email }
        val resolvedAvatar = fallbackAvatarUrl ?: _session.value.avatarUrl
        val generation = sessionGeneration
        val realResult = apiClient.fetchRealAdMobData(
            accessToken = token,
            fallbackName = resolvedName,
            fallbackEmail = resolvedEmail,
            fallbackAvatarUrl = resolvedAvatar,
            customDailyGoal = _customDailyGoal.value
        )
        if (generation != sessionGeneration) return // user logged out while this request was in flight
        if (realResult.isSuccess) {
            val payload = realResult.getOrThrow()
            if (payload.reportingTimeZone.isNotBlank()) {
                reportingTimeZoneId = payload.reportingTimeZone
                settings?.putString(KEY_REPORTING_TZ, payload.reportingTimeZone)
            }
            cachedReportRows = payload.reportRows
            if (_reportRows.value != payload.reportRows) _reportRows.value = payload.reportRows
            cachedRegisteredApps = payload.registeredApps
            activePublisherId = payload.primaryPublisherId

            _session.value = payload.session
            _wallet.value = payload.wallet
            _isOffline.value = false
            _lastRefreshTimestampMs.value = Clock.System.now().toEpochMilliseconds()

            if (isLogin) tracker.trackLoginSucceeded(
                method = if (token.startsWith("ya29")) "Google OAuth" else "API Token",
                userId = payload.session.email.ifBlank { payload.primaryPublisherId },
                email = payload.session.email,
                publisherId = payload.primaryPublisherId,
                userName = payload.session.userName,
                avatarUrl = payload.session.avatarUrl
            )

            // Recompute active summary, apps breakdown, and analytics to preserve user's selected ranges!
            recomputeJob?.cancelAndJoin()
            recomputeMutex.withLock { recomputeSummaryNow(persist = true) }

            val latestSummary = _accountSummary.value
            val topApp = _allApps.value.maxByOrNull { it.todayRevenue }?.name
            // Only report a revenue snapshot when the numbers actually moved
            val signature = "${latestSummary.todayRevenue}|${latestSummary.impressions}|${latestSummary.clicks}|${_allApps.value.size}"
            if (signature != lastSnapshotSignature) {
            lastSnapshotSignature = signature
            tracker.trackRevenueSnapshot(
                todayRevenue = latestSummary.todayRevenue,
                yesterdayRevenue = latestSummary.yesterdayRevenue,
                dailyGoal = latestSummary.dailyGoal,
                dailyGoalProgress = latestSummary.dailyGoalProgress,
                deltaAmount = latestSummary.deltaAmount,
                deltaPercentage = latestSummary.deltaPercentage,
                ecpm = latestSummary.ecpm,
                impressions = latestSummary.impressions,
                clicks = latestSummary.clicks,
                currency = latestSummary.currencyCode.ifBlank { "USD" },
                appsCount = _allApps.value.size,
                topEarningAppName = topApp
            )
            }

            saveToOfflineCache(payload, token)
        } else {
            val ex = realResult.exceptionOrNull()
            val err = ex?.message ?: "AdMob API request failed"
            tracker.trackLoginFailed(
                method = if (token.startsWith("ya29")) "Google OAuth" else "API Token",
                errorMessage = err
            )

            // If token expired, try refreshing silently via tokenRefresher
            if (allowAuthRetry && isAuthExpiredError(err) && tokenRefresher != null) {
                val freshToken = refreshTokenOnce(token)
                if (!freshToken.isNullOrBlank() && freshToken != token) {
                    // Exactly one retry; a second 401 falls through to the error path below
                    fetchAndApplyLocked(freshToken, fallbackName, fallbackEmail, fallbackAvatarUrl, isLogin, allowAuthRetry = false)
                    return
                }
            }

            if (hasOfflineCache && isNetworkFailure(ex)) {
                _isOffline.value = true
                _authError.value = null
            } else {
                _authError.value = err
            }
        }
    }

    fun refreshRealData() {
        val token = currentAccessToken
        if (token.isNullOrBlank()) {
            _authError.value = "No active OAuth session. Please tap Sign In."
            return
        }
        if (refreshJob?.isActive == true) return // single-flight: a refresh is already running
        tracker.trackRefreshTriggered("Dashboard", "Manual / Live Sync", _isOffline.value)
        _isRefreshing.value = true
        _authError.value = null
        _lastRefreshTimestampMs.value = Clock.System.now().toEpochMilliseconds()
        refreshJob = scope.launch {
            try {
                fetchAndApplyRealData(token, _session.value.userName, _session.value.email, _session.value.avatarUrl)
            } catch (e: Exception) {
                if (hasOfflineCache && isNetworkFailure(e)) {
                    _isOffline.value = true
                    _authError.value = null
                    recomputeSummary()
                } else {
                    _authError.value = "Refresh error: ${e.message}"
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private val _hourly = MutableStateFlow<HourlyState>(HourlyState.Idle)
    val hourly: StateFlow<HourlyState> = _hourly.asStateFlow()
    private var hourlyJob: kotlinx.coroutines.Job? = null

    /** Loads the last ~3 PST days hourly (covers local Today/Yesterday for any time zone). */
    fun loadHourly(force: Boolean = false) {
        if (_hourly.value is HourlyState.Unavailable && !force) return
        if (hourlyJob?.isActive == true) return
        val token = currentAccessToken
        val account = activePublisherId
        if (token.isNullOrBlank() || account.isBlank()) {
            _hourly.value = HourlyState.Unavailable("Sign in to load hourly data.")
            return
        }
        if (_hourly.value !is HourlyState.Ready) _hourly.value = HourlyState.Loading
        hourlyJob = scope.launch {
            val pstToday = Clock.System.now().toLocalDateTime(TimeZone.of("America/Los_Angeles")).date
            val result = apiClient.fetchHourlyReportRaw(token, account, pstToday.minus(2, DateTimeUnit.DAY), pstToday)
            _hourly.value = result.fold(
                onSuccess = { HourlyState.Ready(HourlyParser.parse(it)) },
                onFailure = { e ->
                    val msg = e.message.orEmpty()
                    HourlyState.Unavailable(
                        when {
                            "403" in msg || "PERMISSION" in msg -> "Google hasn't enabled hourly reporting (alpha API) for this AdMob account."
                            "404" in msg -> "Google has retired the hourly reporting API."
                            else -> "Couldn't load hourly data. Pull to refresh to retry."
                        }
                    )
                }
            )
        }
    }

    fun logout() {
        val prevUser = _session.value.email.ifBlank { _session.value.publisherId }
        if (prevUser.isNotBlank()) {
            tracker.trackUserLoggedOut(prevUser)
        } else {
            tracker.reset()
        }

        sessionGeneration++
        refreshJob?.cancel()
        lastSnapshotSignature = null
        scope.launch { apiClient.clearCaches() }
        currentAccessToken = null
        reportingTimeZoneId = null
        cachedReportRows = emptyList()
        _reportRows.value = emptyList()
        cachedRegisteredApps = emptyList()
        activePublisherId = ""

        settings?.apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_PUBLISHER_ID)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_USER_AVATAR_URL)
            remove(KEY_CACHE_SUMMARY)
            remove(KEY_CACHE_ALL_APPS)
            remove(KEY_CACHE_ANALYTICS)
            remove(KEY_CACHE_WALLET)
            remove(KEY_CACHE_REPORT_ROWS)
            remove(KEY_CACHE_ROWS_HASH)
            remove(KEY_CACHE_REG_APPS_HASH)
            remove(KEY_CACHE_REGISTERED_APPS)
            remove(KEY_CACHE_TIMESTAMP)
            cacheFiles?.delete(KEY_CACHE_REPORT_ROWS)
            cacheFiles?.delete(KEY_CACHE_REGISTERED_APPS)
            remove(KEY_CUSTOM_DAILY_GOAL)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_REPORTING_TZ)
        }
        _customDailyGoal.value = null
        _session.value = UserSession(
            isLoggedIn = false,
            userName = "",
            email = "",
            publisherId = "",
            avatarUrl = null
        )
        _accountSummary.value = createEmptySummary()
        _allApps.value = emptyList()
        _analytics.value = createEmptyAnalytics()
        _wallet.value = createEmptyWallet()
        _authError.value = null
        _isOffline.value = false
    }


    fun setTimeRange(
        range: TimeRange,
        customStart: kotlinx.datetime.LocalDate? = null,
        customEnd: kotlinx.datetime.LocalDate? = null
    ) {
        val prevRange = _selectedTimeRange.value.name
        _selectedTimeRange.value = range
        if (range == TimeRange.CUSTOM) {
            if (customStart != null) customStartDate = customStart
            if (customEnd != null) customEndDate = customEnd
            if (customStart != null && customEnd != null) {
                tracker.trackCustomDateRangeApplied(customStart.toString(), customEnd.toString())
            }
        }
        tracker.trackTimeRangeChanged("Today Dashboard", range.name, prevRange)
        recomputeSummary()
    }

    fun setAnalyticsRange(range: AnalyticsRange) {
        val prevRange = _selectedAnalyticsRange.value.name
        _selectedAnalyticsRange.value = range
        tracker.trackTimeRangeChanged("Analytics Screen", range.name, prevRange)
        recomputeSummary()
    }

    fun addApp(name: String, platform: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return

        tracker.trackAppRegistered(cleanName, platform)

        val newApp = AppMonetization(
            id = "app-local-${_allApps.value.size + 1}",
            name = cleanName,
            category = "Mobile App",
            platform = platform,
            todayRevenue = 0.0,
            deltaVsYesterday = 0.0,
            totalImpressions = 0,
            fillRate = 0.0,
            avgEcpm = 0.0,
            formats = emptyList()
        )
        val updated = _allApps.value + newApp
        _allApps.value = updated
        try {
            settings?.putString(KEY_CACHE_ALL_APPS, json.encodeToString(updated))
        } catch (_: Exception) {}
    }

    private fun createEmptySummary(): AccountSummary {
        val isCustom = _customDailyGoal.value != null && _customDailyGoal.value!! > 0.0
        val baseDaily = if (isCustom) _customDailyGoal.value!! else 100.0
        val range = _selectedTimeRange.value
        val periodDays = when (range) {
            TimeRange.TODAY, TimeRange.YESTERDAY -> 1
            TimeRange.LAST_7_DAYS -> 7
            TimeRange.THIS_MONTH -> 30
            TimeRange.DAYS_30 -> 30
            TimeRange.CUSTOM -> 30
        }
        val periodGoal = baseDaily * periodDays
        return AccountSummary(
            todayRevenue = 0.0,
            yesterdayRevenue = 0.0,
            deltaAmount = 0.0,
            deltaPercentage = 0.0,
            dailyGoal = baseDaily,
            periodGoal = periodGoal,
            periodDays = periodDays,
            dailyGoalProgress = 0f,
            ecpm = 0.0,
            impressions = 0,
            fillRate = 0.0,
            lastSyncTime = "Syncing...",
            isCustomGoal = isCustom
        )
    }

    private fun createEmptyAnalytics(): AnalyticsSummary {
        return AnalyticsSummary(
            periodLabel = "Past 7 Days",
            totalRevenue = 0.0,
            deltaPercent = 0.0,
            avgEcpm = 0.0,
            totalImpressions = 0,
            trendPoints = emptyList(),
            territories = emptyList()
        )
    }

    private fun createEmptyWallet(): WalletSummary {
        return WalletSummary(
            unpaidBalance = 0.0,
            thresholdAmount = 100.00,
            thresholdPercent = 0f,
            nextDisbursementDate = "21st of next month",
            daysRemaining = 0,
            bankAccountTitle = "Google AdSense Payout",
            bankAccountMasked = "Awaiting Setup",
            isBankVerified = false,
            adServingStatus = "Ready to Monetize",
            policyIssuesCount = 0,
            pastPayouts = emptyList()
        )
    }
}
