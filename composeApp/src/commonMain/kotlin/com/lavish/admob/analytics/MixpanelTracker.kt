package com.lavish.admob.analytics

import com.russhwolf.settings.Settings
import com.lavish.admob.data.createDefaultSettings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Clock
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Mixpanel analytics client for Kotlin Multiplatform (Android, Desktop JVM, iOS).
 * Securely uses the generated MixpanelConfig.TOKEN (never hardcoded in version control).
 */
class MixpanelTracker(
    private val token: String = MixpanelConfig.TOKEN,
    private val settings: Settings? = createDefaultSettings(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
        }
    }
) {
    // ---- Batched event queue: bounded, flushed every FLUSH_INTERVAL_MS or when BATCH_SIZE is reached ----
    private val queueLock = Mutex()
    private val queue = ArrayDeque<JsonObject>()
    @kotlin.concurrent.Volatile private var flushLoop: Job? = null
    private val flushLock = Mutex()

    companion object {
        private const val KEY_DISTINCT_ID = "solarad_mixpanel_distinct_id"
        private const val KEY_IS_IDENTIFIED = "solarad_mixpanel_is_identified"
        private const val TRACK_ENDPOINT = "https://api.mixpanel.com/track"
        private const val ENGAGE_ENDPOINT = "https://api.mixpanel.com/engage"
        private const val MAX_QUEUE = 500
        private const val BATCH_SIZE = 50
        private const val FLUSH_INTERVAL_MS = 45_000L

        val instance: MixpanelTracker by lazy { MixpanelTracker() }
    }

    @kotlin.concurrent.Volatile private var currentDistinctId: String
    @kotlin.concurrent.Volatile private var isIdentified: Boolean

    init {
        val savedId = settings?.getStringOrNull(KEY_DISTINCT_ID)
        if (savedId.isNullOrBlank()) {
            val newAnonId = "anon_${generateRandomId()}"
            settings?.putString(KEY_DISTINCT_ID, newAnonId)
            settings?.putBoolean(KEY_IS_IDENTIFIED, false)
            currentDistinctId = newAnonId
            isIdentified = false
        } else {
            currentDistinctId = savedId
            isIdentified = settings.getBoolean(KEY_IS_IDENTIFIED, false)
        }
    }

    fun getDistinctId(): String = currentDistinctId

    /**
     * Identify a user with a unique identifier (e.g. Email or Publisher ID).
     * Automatically links the previous anonymous ID via Mixpanel $identify.
     */
    fun identify(userId: String, properties: Map<String, Any?> = emptyMap()) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) return

        val previousAnonId = currentDistinctId
        val wasAnonymous = !isIdentified

        currentDistinctId = cleanUserId
        isIdentified = true
        settings?.putString(KEY_DISTINCT_ID, cleanUserId)
        settings?.putBoolean(KEY_IS_IDENTIFIED, true)

        if (token.isBlank()) return

        // If previously anonymous, track $identify to alias and preserve history
        if (wasAnonymous && previousAnonId.isNotBlank() && previousAnonId != cleanUserId) {
            track(
                eventName = "\$identify",
                properties = mapOf(
                    "distinct_id" to cleanUserId,
                    "\$anon_distinct_id" to previousAnonId
                )
            )
        }

        if (properties.isNotEmpty()) {
            setUserProfile(properties)
        }
    }

    /**
     * Set or update Mixpanel People / User Profile properties.
     */
    fun setUserProfile(properties: Map<String, Any?>) {
        if (token.isBlank()) {
            println("[Mixpanel] Token not configured. Skipping user profile update.")
            return
        }

        val enrichedProps = properties.toMutableMap().apply {
            put("\$last_seen", Clock.System.now().toEpochMilliseconds())
            put("platform", getPlatformName())
            put("app_version", com.lavish.admob.model.APP_VERSION_NAME)
        }

        val payload = buildJsonObject {
            put("\$token", JsonPrimitive(token))
            put("\$distinct_id", JsonPrimitive(currentDistinctId))
            put("\$set", enrichedProps.toJsonObject())
        }

        val jsonArray = JsonArray(listOf(payload)).toString()

        scope.launch {
            try {
                val response = httpClient.post(ENGAGE_ENDPOINT) {
                    contentType(ContentType.Application.Json)
                    header("Accept", "text/plain")
                    setBody(jsonArray)
                }
                val body = response.bodyAsText()
                if (!response.status.isSuccess() || body.trim() != "1") {
                    println("[Mixpanel] Engage profile update response: HTTP ${response.status.value} ($body)")
                }
            } catch (e: Exception) {
                println("[Mixpanel] Engage error: ${e.message}")
            }
        }
    }

    /**
     * Track a specific event with properties.
     */
    fun track(eventName: String, properties: Map<String, Any?> = emptyMap()) {
        if (token.isBlank()) {
            println("[Mixpanel] Token not configured. Event '$eventName' skipped.")
            return
        }

        val nowEpochSeconds = Clock.System.now().toEpochMilliseconds() / 1000

        val eventProperties = properties.toMutableMap().apply {
            put("token", token)
            put("distinct_id", currentDistinctId)
            put("time", nowEpochSeconds)
            put("\$insert_id", generateRandomId())
            put("platform", getPlatformName())
            put("app_version", com.lavish.admob.model.APP_VERSION_NAME)
            put("is_identified_user", isIdentified)
        }

        val eventObject = buildJsonObject {
            put("event", JsonPrimitive(eventName))
            put("properties", eventProperties.toJsonObject())
        }

        enqueue(eventObject)
    }

    private fun enqueue(event: JsonObject) {
        scope.launch {
            val size = queueLock.withLock {
                queue.addLast(event)
                while (queue.size > MAX_QUEUE) queue.removeFirst() // drop oldest when offline for long
                queue.size
            }
            ensureFlushLoop()
            if (size >= BATCH_SIZE) flushNow()
        }
    }

    private fun ensureFlushLoop() {
        if (flushLoop?.isActive == true) return
        flushLoop = scope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                flushNow()
            }
        }
    }

    /** Sends all queued events now (call when the app goes to background). Safe to call from any thread. */
    fun flush() {
        scope.launch { flushNow() }
    }

    private suspend fun flushNow(): Unit = flushLock.withLock {
        while (true) {
            val batch = queueLock.withLock {
                if (queue.isEmpty()) return@withLock emptyList()
                val n = minOf(BATCH_SIZE, queue.size)
                List(n) { queue.removeFirst() }
            }
            if (batch.isEmpty()) return@withLock
            val ok = try {
                val response = httpClient.post(TRACK_ENDPOINT) {
                    contentType(ContentType.Application.Json)
                    header("Accept", "text/plain")
                    setBody(JsonArray(batch).toString())
                }
                val body = response.bodyAsText()
                val success = response.status.isSuccess() && body.trim() == "1"
                if (!success) println("[Mixpanel] Batch(${batch.size}) response: HTTP ${response.status.value} ($body)")
                // 4xx = malformed payload; don't retry forever
                success || response.status.value in 400..499
            } catch (e: Exception) {
                println("[Mixpanel] Batch error: ${e.message}")
                false
            }
            if (!ok) {
                // Network failure: put the batch back at the front and try again on the next tick
                queueLock.withLock {
                    batch.asReversed().forEach { queue.addFirst(it) }
                    while (queue.size > MAX_QUEUE) queue.removeLast()
                }
                return@withLock
            }
        }
    }

    /**
     * Reset user session upon logout. Generates a new anonymous distinct ID.
     */
    fun reset() {
        val newAnonId = "anon_${generateRandomId()}"
        currentDistinctId = newAnonId
        isIdentified = false
        settings?.putString(KEY_DISTINCT_ID, newAnonId)
        settings?.putBoolean(KEY_IS_IDENTIFIED, false)
    }

    // =========================================================================
    // HIGH-LEVEL MEANINGFUL USER & NAVIGATION ANALYTICS HELPERS
    // =========================================================================

    /**
     * Track user screen navigation with meaningful naming.
     */
    fun trackScreenViewed(
        screenName: String,
        previousScreen: String? = null,
        tabName: String? = null,
        additionalData: Map<String, Any?> = emptyMap()
    ) {
        val props = mutableMapOf<String, Any?>(
            "screen_name" to screenName,
            "category" to "Navigation"
        )
        previousScreen?.let { props["previous_screen"] = it }
        tabName?.let { props["tab_name"] = it }
        props.putAll(additionalData)

        track("Navigation: Screen Viewed", props)
    }

    /**
     * Track switching between bottom navigation tabs.
     */
    fun trackTabSwitched(
        fromTab: String,
        toTab: String,
        source: String = "bottom_dock"
    ) {
        track(
            "Navigation: Tab Switched",
            mapOf(
                "from_tab" to fromTab,
                "to_tab" to toTab,
                "navigation_source" to source
            )
        )
    }

    /**
     * Track opening an overlay or modal dialog.
     */
    fun trackModalOpened(
        modalName: String,
        triggerSource: String = "",
        details: Map<String, Any?> = emptyMap()
    ) {
        val props = mutableMapOf<String, Any?>(
            "modal_name" to modalName,
            "trigger_source" to triggerSource
        )
        props.putAll(details)
        track("Navigation: Modal Opened", props)
    }

    /**
     * Track dismissing an overlay or modal dialog.
     */
    fun trackModalDismissed(
        modalName: String,
        dismissMethod: String
    ) {
        track(
            "Navigation: Modal Dismissed",
            mapOf(
                "modal_name" to modalName,
                "dismiss_method" to dismissMethod
            )
        )
    }

    /**
     * Track system or custom back-button navigation.
     */
    fun trackBackPressed(
        currentScreen: String,
        actionTaken: String
    ) {
        track(
            "Navigation: Back Pressed",
            mapOf(
                "current_screen" to currentScreen,
                "action_taken" to actionTaken
            )
        )
    }

    /**
     * Track login initiation.
     */
    fun trackLoginInitiated(method: String) {
        track(
            "User: Login Initiated",
            mapOf("auth_method" to method)
        )
    }

    /**
     * Track successful user login, updates profile and identifies user.
     */
    fun trackLoginSucceeded(
        method: String,
        userId: String,
        email: String,
        publisherId: String,
        userName: String,
        avatarUrl: String? = null
    ) {
        val distinctId = if (email.isNotBlank()) email else (if (userId.isNotBlank()) userId else publisherId)
        
        identify(
            userId = distinctId,
            properties = mapOf(
                "\$name" to userName,
                "\$email" to email,
                "\$avatar" to (avatarUrl ?: ""),
                "publisher_id" to publisherId,
                "auth_method" to method
            )
        )

        track(
            "User: Login Succeeded",
            mapOf(
                "auth_method" to method,
                "user_id" to userId,
                "email" to email,
                "publisher_id" to publisherId,
                "user_name" to userName
            )
        )
    }

    /**
     * Track login failure.
     */
    fun trackLoginFailed(method: String, errorMessage: String) {
        track(
            "User: Login Failed",
            mapOf(
                "auth_method" to method,
                "error_message" to errorMessage
            )
        )
    }

    /**
     * Track user sign out.
     */
    fun trackUserLoggedOut(userId: String) {
        track(
            "User: Logged Out",
            mapOf("user_id" to userId)
        )
        reset()
    }

    /**
     * Track today's earnings & account performance metrics.
     */
    fun trackRevenueSnapshot(
        todayRevenue: Double,
        yesterdayRevenue: Double,
        dailyGoal: Double,
        dailyGoalProgress: Float,
        deltaAmount: Double,
        deltaPercentage: Double,
        ecpm: Double,
        impressions: Int,
        clicks: Int,
        currency: String,
        appsCount: Int = 0,
        topEarningAppName: String? = null
    ) {
        val props = mutableMapOf<String, Any?>(
            "today_revenue" to todayRevenue,
            "yesterday_revenue" to yesterdayRevenue,
            "daily_goal" to dailyGoal,
            "daily_goal_progress" to dailyGoalProgress,
            "delta_amount" to deltaAmount,
            "delta_percentage" to deltaPercentage,
            "ecpm" to ecpm,
            "impressions" to impressions,
            "clicks" to clicks,
            "currency" to currency,
            "apps_count" to appsCount
        )
        topEarningAppName?.let { props["top_earning_app"] = it }

        track("Revenue: Today Earnings Synced", props)

        // Also update the User Profile in Mixpanel with the latest revenue and performance stats
        setUserProfile(
            mapOf(
                "latest_today_revenue" to todayRevenue,
                "latest_yesterday_revenue" to yesterdayRevenue,
                "daily_goal" to dailyGoal,
                "currency" to currency,
                "apps_count" to appsCount
            )
        )
    }

    /**
     * Track dashboard time horizon selection.
     */
    fun trackTimeRangeChanged(screen: String, newRange: String, previousRange: String? = null) {
        val props = mutableMapOf<String, Any?>(
            "screen" to screen,
            "new_range" to newRange
        )
        previousRange?.let { props["previous_range"] = it }
        track("Dashboard: Time Range Changed", props)
    }

    /**
     * Track custom date range application.
     */
    fun trackCustomDateRangeApplied(startDate: String, endDate: String) {
        track(
            "Dashboard: Custom Date Range Applied",
            mapOf(
                "start_date" to startDate,
                "end_date" to endDate
            )
        )
    }

    /**
     * Track goal update.
     */
    fun trackDailyGoalUpdated(amount: Double, currency: String) {
        track(
            "Dashboard: Daily Goal Updated",
            mapOf(
                "goal_amount" to amount,
                "currency" to currency
            )
        )
    }

    /**
     * Track user or auto refresh data.
     */
    fun trackRefreshTriggered(tab: String, triggerMethod: String, isOffline: Boolean) {
        track(
            "Dashboard: Data Refresh Triggered",
            mapOf(
                "current_tab" to tab,
                "trigger_method" to triggerMethod,
                "is_offline" to isOffline
            )
        )
    }

    /**
     * Track clicking into app details.
     */
    fun trackAppSelected(appName: String, platform: String, earnings: Double, currency: String) {
        track(
            "App Directory: App Selected",
            mapOf(
                "app_name" to appName,
                "app_platform" to platform,
                "estimated_earnings" to earnings,
                "currency" to currency
            )
        )
    }

    /**
     * Track registering a new app.
     */
    fun trackAppRegistered(appName: String, platform: String) {
        track(
            "App Directory: App Registered",
            mapOf(
                "app_name" to appName,
                "app_platform" to platform
            )
        )
    }

    /**
     * Track theme toggle.
     */
    fun trackThemeChanged(newTheme: String) {
        track(
            "Settings: Theme Changed",
            mapOf("new_theme" to newTheme)
        )
    }

    /**
     * Track auto-refresh interval change.
     */
    fun trackAutoRefreshIntervalChanged(seconds: Int) {
        track(
            "Settings: Auto Refresh Interval Changed",
            mapOf("interval_seconds" to seconds)
        )
    }
}

private fun Map<String, Any?>.toJsonObject(): JsonObject = buildJsonObject {
    forEach { (key, value) ->
        when (value) {
            null -> put(key, JsonNull)
            is Boolean -> put(key, JsonPrimitive(value))
            is Number -> put(key, JsonPrimitive(value))
            is String -> put(key, JsonPrimitive(value))
            is JsonElement -> put(key, value)
            else -> put(key, JsonPrimitive(value.toString()))
        }
    }
}
