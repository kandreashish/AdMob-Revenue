package com.solarad.app.data

import com.russhwolf.settings.Settings
import com.solarad.app.model.AccountSummary
import com.solarad.app.model.AnalyticsRange
import com.solarad.app.model.AnalyticsSummary
import com.solarad.app.model.AppMonetization
import com.solarad.app.model.AutoRefreshInterval
import com.solarad.app.model.ThemeMode
import com.solarad.app.model.TimeRange
import com.solarad.app.model.UserSession
import com.solarad.app.model.WalletSummary
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AdMobRepository(
    private val settings: Settings? = createDefaultSettings(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
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
    private var currentAccessToken: String? = settings?.getStringOrNull(KEY_ACCESS_TOKEN)

    private var cachedReportRows: List<AdMobReportRow> = loadCachedList(KEY_CACHE_REPORT_ROWS)
    private val _reportRows = MutableStateFlow<List<AdMobReportRow>>(cachedReportRows)
    val reportRows: StateFlow<List<AdMobReportRow>> = _reportRows.asStateFlow()

    private var cachedRegisteredApps: List<AppMonetization> = loadCachedList(KEY_CACHE_REGISTERED_APPS)
    private var activePublisherId: String = settings?.getString(KEY_PUBLISHER_ID, "") ?: ""

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

    init {
        // If restored from persisted session with an access token, fetch real data in background
        if (_session.value.isLoggedIn && !currentAccessToken.isNullOrBlank()) {
            scope.launch {
                val token = currentAccessToken ?: return@launch
                fetchAndApplyRealData(token, _session.value.userName, _session.value.email)
            }
        }
    }

    var tokenRefresher: (suspend () -> String?)? = null

    fun getCurrentToken(): String? = currentAccessToken

    fun updateAccessToken(newToken: String) {
        val cleanToken = newToken.trim()
        if (cleanToken.isNotBlank()) {
            currentAccessToken = cleanToken
            settings?.putString(KEY_ACCESS_TOKEN, cleanToken)
            _authError.value = null
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
    }

    fun setCustomDailyGoal(goal: Double?) {
        if (goal != null && goal > 0.0) {
            _customDailyGoal.value = goal
            settings?.putDouble(KEY_CUSTOM_DAILY_GOAL, goal)
        } else {
            _customDailyGoal.value = null
            settings?.remove(KEY_CUSTOM_DAILY_GOAL)
        }
        recomputeSummary()
    }

    private fun recomputeSummary() {
        if (cachedReportRows.isNotEmpty()) {
            val range = _selectedTimeRange.value
            val summary = AdMobApiClient.computeAccountSummary(
                rows = cachedReportRows,
                range = range,
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
                range = range,
                customStartDate = customStartDate,
                customEndDate = customEndDate
            )
            _analytics.value = AdMobApiClient.computeAnalytics(
                rows = cachedReportRows,
                range = _selectedAnalyticsRange.value
            )
        } else {
            val current = _accountSummary.value
            val isCustom = _customDailyGoal.value != null && _customDailyGoal.value!! > 0.0
            val goalVal = if (isCustom) _customDailyGoal.value!! else 100.0
            val progress = if (goalVal > 0.0 && current.todayRevenue > 0.0) {
                (current.todayRevenue / goalVal).toFloat().coerceIn(0f, 1f)
            } else 0f
            _accountSummary.value = current.copy(
                dailyGoal = goalVal,
                dailyGoalProgress = progress,
                isCustomGoal = isCustom
            )
        }
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
            _authError.value = "OAuth authorization failed: Empty token received."
            return
        }

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
                fetchAndApplyRealData(cleanToken, resolvedName, resolvedEmail, resolvedAvatar)
            } catch (e: Exception) {
                _authError.value = "AdMob API Error: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loginWithGoogleProfile(name: String, email: String, idToken: String? = null, avatarUrl: String? = null) {
        if (name.isBlank() && email.isBlank()) {
            _authError.value = "Authentication cancelled or invalid Google account profile."
            return
        }

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
                    fetchAndApplyRealData(idToken, fallbackAvatarUrl = resolvedAvatar)
                } catch (e: Exception) {
                    _authError.value = "Connection error: ${e.message ?: "Authentication failed"}"
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
            _authError.value = "Please enter a valid Google OAuth or API token."
            return
        }

        _isRefreshing.value = true
        _authError.value = null
        _lastRefreshTimestampMs.value = Clock.System.now().toEpochMilliseconds()

        scope.launch {
            try {
                currentAccessToken = cleanToken
                fetchAndApplyRealData(cleanToken, "AdMob Developer", "")
            } catch (e: Exception) {
                currentAccessToken = null
                _authError.value = "Authentication failed: ${e.message ?: "Network error"}"
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
                putString(KEY_CACHE_REPORT_ROWS, json.encodeToString(payload.reportRows))
                putString(KEY_CACHE_REGISTERED_APPS, json.encodeToString(payload.registeredApps))
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

    private suspend fun fetchAndApplyRealData(
        token: String,
        fallbackName: String = "",
        fallbackEmail: String = "",
        fallbackAvatarUrl: String? = null
    ) {
        val resolvedName = fallbackName.ifBlank { _session.value.userName }
        val resolvedEmail = fallbackEmail.ifBlank { _session.value.email }
        val resolvedAvatar = fallbackAvatarUrl ?: _session.value.avatarUrl
        val realResult = apiClient.fetchRealAdMobData(
            accessToken = token,
            fallbackName = resolvedName,
            fallbackEmail = resolvedEmail,
            fallbackAvatarUrl = resolvedAvatar,
            customDailyGoal = _customDailyGoal.value
        )
        if (realResult.isSuccess) {
            val payload = realResult.getOrThrow()
            cachedReportRows = payload.reportRows
            _reportRows.value = payload.reportRows
            cachedRegisteredApps = payload.registeredApps
            activePublisherId = payload.primaryPublisherId

            _session.value = payload.session
            _wallet.value = payload.wallet
            _isOffline.value = false
            _lastRefreshTimestampMs.value = Clock.System.now().toEpochMilliseconds()

            // Recompute active summary, apps breakdown, and analytics to preserve user's selected ranges!
            recomputeSummary()

            saveToOfflineCache(payload, token)
        } else {
            val ex = realResult.exceptionOrNull()
            val err = ex?.message ?: "AdMob API request failed"

            // If token expired, try refreshing silently via tokenRefresher
            if (isAuthExpiredError(err) && tokenRefresher != null) {
                try {
                    val freshToken = tokenRefresher?.invoke()
                    if (!freshToken.isNullOrBlank() && freshToken != token) {
                        updateAccessToken(freshToken)
                        fetchAndApplyRealData(freshToken, fallbackName, fallbackEmail, fallbackAvatarUrl)
                        return
                    }
                } catch (_: Exception) {}
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
        _isRefreshing.value = true
        _authError.value = null
        _lastRefreshTimestampMs.value = Clock.System.now().toEpochMilliseconds()
        scope.launch {
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

    fun logout() {
        currentAccessToken = null
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
            remove(KEY_CACHE_REGISTERED_APPS)
            remove(KEY_CACHE_TIMESTAMP)
            remove(KEY_CUSTOM_DAILY_GOAL)
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

    private var customStartDate: kotlinx.datetime.LocalDate? = null
    private var customEndDate: kotlinx.datetime.LocalDate? = null

    fun setTimeRange(
        range: TimeRange,
        customStart: kotlinx.datetime.LocalDate? = null,
        customEnd: kotlinx.datetime.LocalDate? = null
    ) {
        _selectedTimeRange.value = range
        if (range == TimeRange.CUSTOM) {
            if (customStart != null) customStartDate = customStart
            if (customEnd != null) customEndDate = customEnd
        }
        recomputeSummary()
    }

    fun setAnalyticsRange(range: AnalyticsRange) {
        _selectedAnalyticsRange.value = range
        recomputeSummary()
    }

    fun addApp(name: String, platform: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return

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
        val goalVal = if (isCustom) _customDailyGoal.value!! else 100.0
        return AccountSummary(
            todayRevenue = 0.0,
            yesterdayRevenue = 0.0,
            deltaAmount = 0.0,
            deltaPercentage = 0.0,
            dailyGoal = goalVal,
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
