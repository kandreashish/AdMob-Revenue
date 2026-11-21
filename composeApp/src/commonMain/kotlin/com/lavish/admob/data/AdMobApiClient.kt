package com.lavish.admob.data

import com.lavish.admob.model.AccountSummary
import com.lavish.admob.model.AdFormatMetric
import com.lavish.admob.model.AnalyticsRange
import com.lavish.admob.model.AnalyticsSummary
import com.lavish.admob.model.AppMonetization
import com.lavish.admob.model.RevenuePoint
import com.lavish.admob.model.TerritoryYield
import com.lavish.admob.model.TimeRange
import com.lavish.admob.model.UserSession
import com.lavish.admob.model.WalletSummary
import com.lavish.admob.model.maskPublisherId
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.withLock
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class GoogleUserInfo(
    val sub: String = "",
    val name: String = "AdMob Publisher",
    val email: String = "publisher@gmail.com",
    val picture: String? = null
)

@Serializable
data class PublisherAccount(
    val name: String,
    val publisherId: String,
    val reportingTimeZone: String = "America/Los_Angeles",
    val currencyCode: String = "USD"
)

@Serializable
data class AccountsListResponse(
    val account: List<PublisherAccount> = emptyList()
)

@Serializable
data class AdMobReportRow(
    val date: String,
    val appId: String,
    val appName: String,
    val format: String,
    val country: String,
    val earnings: Double,
    val impressions: Int,
    val clicks: Int,
    val adRequests: Long = 0L,
    val matchedRequests: Long = 0L
)

data class RealAdMobPayload(
    val session: UserSession,
    val primaryPublisherId: String,
    val reportRows: List<AdMobReportRow>,
    val registeredApps: List<AppMonetization>,
    val accountSummary: AccountSummary,
    val apps: List<AppMonetization>,
    val analytics: AnalyticsSummary,
    val wallet: WalletSummary,
    val reportingTimeZone: String = "",
    val statusMessage: String = ""
)

// In production release builds, sensitive API payloads and tokens are disabled by default
var isDebugLoggingEnabled: Boolean = false

private fun logAdMob(endpoint: String, message: String) {
    if (!isDebugLoggingEnabled) return
    // Never dump multi-MB report bodies (slow, and leaks data into logcat). Log a capped preview + size.
    val preview = if (message.length <= 500) message else message.take(500) + "… (${message.length} chars)"
    println("[AdMobAPI] $endpoint: $preview")
}

class AdMobApiClient(
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(jsonParser)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 45_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }
) {
    // ---- In-memory caches (cleared on logout via clearCaches()) ----
    private class Timed<T>(val value: T, val atMs: Long)
    private val cacheLock = kotlinx.coroutines.sync.Mutex()
    private var userInfoCache: Timed<GoogleUserInfo>? = null
    private var accountsCache: Timed<List<PublisherAccount>>? = null
    private var appsCache: Timed<List<AppMonetization>>? = null
    private var lifetimeCache: Timed<Double>? = null
    private var reportCache: ReportCache? = null

    /** Last full report, used to merge short incremental windows on auto-refresh. */
    private class ReportCache(
        val accountName: String,
        val source: String, // "mediation" | "network"
        val rows: List<AdMobReportRow>,
        val fullFetchedAtMs: Long,
        val fullFetchedForDay: String
    )

    private fun nowMs() = Clock.System.now().toEpochMilliseconds()
    private fun <T> Timed<T>?.fresh(ttlMs: Long): T? = this?.takeIf { nowMs() - it.atMs < ttlMs }?.value

    suspend fun clearCaches() = cacheLock.withLock {
        userInfoCache = null; accountsCache = null; appsCache = null; lifetimeCache = null; reportCache = null
    }

    /**
     * Fetches user identity (name, email) from Google OAuth2 userinfo endpoint
     */
    suspend fun fetchGoogleUserInfo(accessToken: String): Result<GoogleUserInfo> = runCatching {
        val response = httpClient.get("https://www.googleapis.com/oauth2/v3/userinfo") {
            header("Authorization", "Bearer $accessToken")
        }
        val text = response.bodyAsText()
        logAdMob("GoogleUserInfo", "HTTP ${response.status.value} -> $text")
        if (response.status.isSuccess()) {
            jsonParser.decodeFromString<GoogleUserInfo>(text)
        } else {
            throw Exception("Google userinfo failed: HTTP ${response.status.value} ($text)")
        }
    }

    /**
     * Lists publisher accounts linked to this Google profile
     */
    suspend fun fetchPublisherAccounts(accessToken: String): Result<List<PublisherAccount>> = runCatching {
        val response = httpClient.get("https://admob.googleapis.com/v1/accounts") {
            header("Authorization", "Bearer $accessToken")
        }
        val bodyText = response.bodyAsText()
        logAdMob("PublisherAccounts", "HTTP ${response.status.value} -> $bodyText")
        if (response.status.isSuccess()) {
            val list = jsonParser.decodeFromString<AccountsListResponse>(bodyText)
            list.account
        } else {
            throw Exception("AdMob accounts API HTTP ${response.status.value}: $bodyText")
        }
    }

    /**
     * Fetches list of registered apps from AdMob API
     */
    suspend fun fetchPublisherApps(accessToken: String, accountName: String): Result<List<AppMonetization>> = runCatching {
        val normalizedName = if (accountName.startsWith("accounts/")) accountName else "accounts/$accountName"
        val response = httpClient.get("https://admob.googleapis.com/v1/$normalizedName/apps") {
            header("Authorization", "Bearer $accessToken")
        }
        val bodyText = response.bodyAsText()
        logAdMob("PublisherApps", "HTTP ${response.status.value} -> $bodyText")
        if (response.status.isSuccess()) {
            val root = jsonParser.parseToJsonElement(bodyText).jsonObject
            val appsArray = root["apps"]?.jsonArray ?: root["app"]?.jsonArray ?: emptyList()
            appsArray.mapIndexed { index, appEl ->
                val appObj = appEl.jsonObject
                val appId = appObj["appId"]?.jsonPrimitive?.content ?: "app-$index"
                val platformStr = appObj["platform"]?.jsonPrimitive?.content ?: "ANDROID"
                val platform = if (platformStr.equals("IOS", ignoreCase = true)) "iOS" else "Android"
                val manualTitle = appObj["manualAppInfo"]?.jsonObject?.get("displayName")?.jsonPrimitive?.content
                val linkedAppInfo = appObj["linkedAppInfo"]?.jsonObject
                val linkedTitle = linkedAppInfo?.get("displayName")?.jsonPrimitive?.content
                val appStoreId = linkedAppInfo?.get("appStoreId")?.jsonPrimitive?.content
                val title = linkedTitle ?: manualTitle ?: "AdMob App #${index + 1}"

                AppMonetization(
                    id = appId,
                    name = title,
                    category = "Mobile App",
                    platform = platform,
                    todayRevenue = 0.0,
                    deltaVsYesterday = 0.0,
                    totalImpressions = 0,
                    fillRate = 0.0,
                    avgEcpm = 0.0,
                    formats = emptyList(),
                    appStoreId = appStoreId,
                    iconUrl = null
                )
            }
        } else {
            emptyList()
        }
    }

    /**
     * Calls AdMob Reporting API to fetch actual network performance
     */
    suspend fun fetchNetworkReportRaw(
        accessToken: String,
        accountName: String,
        startYear: Int, startMonth: Int, startDay: Int,
        endYear: Int, endMonth: Int, endDay: Int
    ): Result<String> = runCatching {
        val normalizedName = if (accountName.startsWith("accounts/")) accountName else "accounts/$accountName"
        val requestBody = """
        {
          "reportSpec": {
            "dateRange": {
              "startDate": { "year": $startYear, "month": $startMonth, "day": $startDay },
              "endDate": { "year": $endYear, "month": $endMonth, "day": $endDay }
            },
            "dimensions": ["DATE", "APP", "FORMAT", "COUNTRY"],
            "metrics": ["ESTIMATED_EARNINGS", "IMPRESSIONS", "CLICKS", "AD_REQUESTS", "MATCHED_REQUESTS"]
          }
        }
        """.trimIndent()

        logAdMob("NetworkReport:Request", "URL=https://admob.googleapis.com/v1/$normalizedName/networkReport:generate | Body=$requestBody")

        val response = httpClient.post("https://admob.googleapis.com/v1/$normalizedName/networkReport:generate") {
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        val bodyText = response.bodyAsText()
        logAdMob("NetworkReport:Response", "HTTP ${response.status.value} -> $bodyText")
        if (response.status.isSuccess()) {
            bodyText
        } else {
            throw Exception("AdMob Network Report HTTP ${response.status.value}: $bodyText")
        }
    }

    /**
     * Calls AdMob Mediation Reporting API to fetch full earnings across all ad sources
     * (Mediation, Bidding partners, and AdMob Network). This matches the AdMob Web Dashboard.
     */
    suspend fun fetchMediationReportRaw(
        accessToken: String,
        accountName: String,
        startYear: Int, startMonth: Int, startDay: Int,
        endYear: Int, endMonth: Int, endDay: Int
    ): Result<String> = runCatching {
        val normalizedName = if (accountName.startsWith("accounts/")) accountName else "accounts/$accountName"
        val requestBody = """
        {
          "reportSpec": {
            "dateRange": {
              "startDate": { "year": $startYear, "month": $startMonth, "day": $startDay },
              "endDate": { "year": $endYear, "month": $endMonth, "day": $endDay }
            },
            "dimensions": ["DATE", "APP", "FORMAT", "COUNTRY"],
            "metrics": ["ESTIMATED_EARNINGS", "IMPRESSIONS", "CLICKS", "AD_REQUESTS", "MATCHED_REQUESTS"]
          }
        }
        """.trimIndent()

        logAdMob("MediationReport:Request", "URL=https://admob.googleapis.com/v1/$normalizedName/mediationReport:generate | Body=$requestBody")

        val response = httpClient.post("https://admob.googleapis.com/v1/$normalizedName/mediationReport:generate") {
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        val bodyText = response.bodyAsText()
        logAdMob("MediationReport:Response", "HTTP ${response.status.value} -> $bodyText")
        if (response.status.isSuccess()) {
            bodyText
        } else {
            throw Exception("AdMob Mediation Report HTTP ${response.status.value}: $bodyText")
        }
    }

    /**
     * Calls AdMob Reporting API with maximum allowed date range (3 years / 1095 days)
     * without dimensions to fetch exact all-time lifetime earnings for accurate Wallet unpaid balance.
     */
    suspend fun fetchLifetimeEarnings(
        accessToken: String,
        accountName: String,
        today: LocalDate
    ): Result<Double> = runCatching {
        val normalizedName = if (accountName.startsWith("accounts/")) accountName else "accounts/$accountName"
        val threeYearsAgo = today.minus(1095, DateTimeUnit.DAY)
        val requestBody = """
        {
          "reportSpec": {
            "dateRange": {
              "startDate": { "year": ${threeYearsAgo.year}, "month": ${threeYearsAgo.month.number}, "day": ${threeYearsAgo.day} },
              "endDate": { "year": ${today.year}, "month": ${today.month.number}, "day": ${today.day} }
            },
            "metrics": ["ESTIMATED_EARNINGS"]
          }
        }
        """.trimIndent()

        // 1. Try Mediation report first for full lifetime earnings across all ad sources
        val mediationResp = runCatching {
            logAdMob("LifetimeEarnings:Request(Mediation)", "URL=https://admob.googleapis.com/v1/$normalizedName/mediationReport:generate | Body=$requestBody")
            val resp = httpClient.post("https://admob.googleapis.com/v1/$normalizedName/mediationReport:generate") {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            val text = resp.bodyAsText()
            logAdMob("LifetimeEarnings:Response(Mediation)", "HTTP ${resp.status.value} -> $text")
            if (resp.status.isSuccess()) parseLifetimeEarnings(text) else null
        }.getOrNull()

        if (mediationResp != null && mediationResp > 0.0) {
            return@runCatching mediationResp
        }

        // 2. Fallback to networkReport
        logAdMob("LifetimeEarnings:Request(Network)", "URL=https://admob.googleapis.com/v1/$normalizedName/networkReport:generate | Body=$requestBody")
        val response = httpClient.post("https://admob.googleapis.com/v1/$normalizedName/networkReport:generate") {
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        val bodyText = response.bodyAsText()
        logAdMob("LifetimeEarnings:Response(Network)", "HTTP ${response.status.value} -> $bodyText")
        if (response.status.isSuccess()) {
            parseLifetimeEarnings(bodyText)
        } else {
            throw Exception("AdMob Lifetime Earnings HTTP ${response.status.value}: $bodyText")
        }
    }

    /**
     * High level orchestrator: connects real Google user session and parses real AdMob metrics
     */
    suspend fun fetchRealAdMobData(
        accessToken: String,
        fallbackName: String = "",
        fallbackEmail: String = "",
        fallbackAvatarUrl: String? = null,
        customDailyGoal: Double? = null
    ): Result<RealAdMobPayload> = runCatching {
        // 1+2. User info and accounts are independent -> fetch in parallel, cached for 6h.
        val (userInfo, accounts) = coroutineScope {
            val userInfoDeferred = async {
                cacheLock.withLock { userInfoCache.fresh(META_TTL_MS) }
                    ?: fetchGoogleUserInfo(accessToken).getOrNull()?.also { ui ->
                        cacheLock.withLock { userInfoCache = Timed(ui, nowMs()) }
                    }
                    ?: GoogleUserInfo(
                        name = fallbackName.ifBlank { "AdMob Publisher" },
                        email = fallbackEmail.ifBlank { "publisher@gmail.com" },
                        picture = fallbackAvatarUrl
                    )
            }
            val accountsDeferred = async {
                cacheLock.withLock { accountsCache.fresh(META_TTL_MS) }?.let { Result.success(it) }
                    ?: fetchPublisherAccounts(accessToken).onSuccess { list ->
                        if (list.isNotEmpty()) cacheLock.withLock { accountsCache = Timed(list, nowMs()) }
                    }
            }
            val accountsResult = accountsDeferred.await()
            if (accountsResult.isFailure) {
                userInfoDeferred.cancel()
                val err = accountsResult.exceptionOrNull()?.message ?: "Unknown error"
                throw Exception("AdMob Publisher Accounts lookup failed: $err")
            }
            userInfoDeferred.await() to accountsResult.getOrThrow()
        }
        if (accounts.isEmpty()) {
            throw Exception("No AdMob publisher account was found for ${userInfo.email}. Please verify that your account is registered at admob.google.com and the AdMob API is enabled in your Google Cloud project.")
        }
        val primaryAccount: PublisherAccount = accounts.first()

        val currencyCode = primaryAccount.currencyCode.ifBlank { "USD" }
        val currencySymbol = resolveCurrencySymbol(currencyCode)

        // 4. Timezone-aware date calculations
        // Dates in AdMob reports are generated in the account's reportingTimeZone.
        // AdMob strictly rejects requests where endDate is in the future relative to this timezone.
        val accountTz = try {
            TimeZone.of(primaryAccount.reportingTimeZone)
        } catch (_: Exception) {
            TimeZone.currentSystemDefault()
        }
        val now = Clock.System.now()
        val today = now.toLocalDateTime(accountTz).date

        // 3 + 5 + lifetime: apps, report rows and lifetime earnings only depend on the account -> parallel.
        val (registeredApps, reportRows, lifetimeEarnings) = coroutineScope {
            val appsDeferred = async {
                cacheLock.withLock { appsCache.fresh(META_TTL_MS) }
                    ?: fetchPublisherApps(accessToken, primaryAccount.name).getOrDefault(emptyList()).also { list ->
                        if (list.isNotEmpty()) cacheLock.withLock { appsCache = Timed(list, nowMs()) }
                    }
            }
            val lifetimeDeferred = async {
                cacheLock.withLock { lifetimeCache.fresh(LIFETIME_TTL_MS) }
                    ?: fetchLifetimeEarnings(accessToken, primaryAccount.name, today).getOrNull()?.also { v ->
                        cacheLock.withLock { lifetimeCache = Timed(v, nowMs()) }
                    }
            }
            val rowsDeferred = async { fetchReportRowsSmart(accessToken, primaryAccount.name, today) }
            Triple(appsDeferred.await(), rowsDeferred.await(), lifetimeDeferred.await())
        }

        logAdMob("FetchRealData:Summary", "Parsed ${reportRows.size} daily report rows for publisher ${primaryAccount.publisherId}")

        val resolvedAvatar = userInfo.picture ?: fallbackAvatarUrl
        val session = UserSession(
            isLoggedIn = true,
            userName = userInfo.name,
            email = userInfo.email,
            publisherId = primaryAccount.publisherId,
            avatarUrl = resolvedAvatar,
            currencyCode = currencyCode,
            currencySymbol = currencySymbol
        )

        val accountSummary = computeAccountSummary(
            rows = reportRows,
            range = TimeRange.TODAY,
            publisherId = primaryAccount.publisherId,
            today = today,
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            customDailyGoal = customDailyGoal
        )
        val apps = computeAppsMonetization(reportRows, registeredApps, today)
        val analytics = computeAnalytics(reportRows, AnalyticsRange.DAYS_7, today)
        val wallet = computeWallet(reportRows, lifetimeEarnings)

        RealAdMobPayload(
            session = session,
            primaryPublisherId = primaryAccount.publisherId,
            reportRows = reportRows,
            registeredApps = registeredApps,
            accountSummary = accountSummary,
            apps = apps,
            analytics = analytics,
            wallet = wallet,
            reportingTimeZone = accountTz.id,
            statusMessage = run {
                val maskedId = maskPublisherId(primaryAccount.publisherId)
                if (maskedId.isNotBlank()) "Connected to AdMob Publisher $maskedId" else "Connected to AdMob"
            }
        )
    }

    /**
     * Full 90-day report at most once per [FULL_REPORT_TTL_MS] (or when the day rolls over / cache is empty).
     * In between, only the last [INCREMENTAL_DAYS] days are re-fetched and merged in: rows are keyed by
     * (date, app, format, country), so replacing every row whose date falls in the window is exact.
     */
    private suspend fun fetchReportRowsSmart(accessToken: String, accountName: String, today: LocalDate): List<AdMobReportRow> {
        val todayKey = formatDateKey(today)
        val oldestKey = formatDateKey(today.minus(90, DateTimeUnit.DAY))
        val cache = cacheLock.withLock { reportCache }
        val canIncrement = cache != null &&
            cache.accountName == accountName &&
            cache.rows.isNotEmpty() &&
            cache.fullFetchedForDay == todayKey &&
            nowMs() - cache.fullFetchedAtMs < FULL_REPORT_TTL_MS

        if (canIncrement) {
            val windowStart = today.minus(INCREMENTAL_DAYS - 1, DateTimeUnit.DAY)
            val windowStartKey = formatDateKey(windowStart)
            val raw = if (cache!!.source == "mediation") {
                fetchMediationReportRaw(accessToken, accountName,
                    windowStart.year, windowStart.month.number, windowStart.day,
                    today.year, today.month.number, today.day)
            } else {
                fetchNetworkReportRaw(accessToken, accountName,
                    windowStart.year, windowStart.month.number, windowStart.day,
                    today.year, today.month.number, today.day)
            }
            val fresh = raw.getOrNull()?.let { text -> mutableListOf<AdMobReportRow>().also { parseReportRows(text, it) } }
            if (fresh != null) {
                val merged = ArrayList<AdMobReportRow>(cache.rows.size + fresh.size)
                cache.rows.forEach { if (it.date < windowStartKey && it.date >= oldestKey) merged.add(it) }
                merged.addAll(fresh)
                cacheLock.withLock {
                    reportCache = ReportCache(accountName, cache.source, merged, cache.fullFetchedAtMs, cache.fullFetchedForDay)
                }
                logAdMob("ReportSelection", "Incremental ${cache.source}: ${fresh.size} fresh rows, ${merged.size} total")
                return merged
            }
            // Incremental call failed -> fall through to a full fetch
        }

        val ninetyDaysAgo = today.minus(90, DateTimeUnit.DAY)
        val reportRows = mutableListOf<AdMobReportRow>()
        var source = "mediation"
        val mediationResult = fetchMediationReportRaw(
            accessToken = accessToken,
            accountName = accountName,
            startYear = ninetyDaysAgo.year, startMonth = ninetyDaysAgo.month.number, startDay = ninetyDaysAgo.day,
            endYear = today.year, endMonth = today.month.number, endDay = today.day
        )
        if (mediationResult.isSuccess) {
            parseReportRows(mediationResult.getOrThrow(), reportRows)
            logAdMob("ReportSelection", "Using Mediation Report: parsed ${reportRows.size} rows")
        }

        // Fallback to Network Report if mediation returned no rows or failed
        if (reportRows.isEmpty()) {
            source = "network"
            val rawReportResult = fetchNetworkReportRaw(
                accessToken = accessToken,
                accountName = accountName,
                startYear = ninetyDaysAgo.year, startMonth = ninetyDaysAgo.month.number, startDay = ninetyDaysAgo.day,
                endYear = today.year, endMonth = today.month.number, endDay = today.day
            )
            if (rawReportResult.isFailure) {
                val reportErr = rawReportResult.exceptionOrNull()?.message ?: "AdMob network report failed"
                logAdMob("NetworkReport:Error", reportErr)
                throw Exception("AdMob Report Error: $reportErr")
            }
            parseReportRows(rawReportResult.getOrThrow(), reportRows)
            logAdMob("ReportSelection", "Using Network Report: parsed ${reportRows.size} rows")
        }

        if (reportRows.isNotEmpty()) {
            cacheLock.withLock {
                reportCache = ReportCache(accountName, source, reportRows.toList(), nowMs(), todayKey)
            }
        }
        return reportRows
    }

    companion object {
        private const val META_TTL_MS = 6 * 60 * 60 * 1000L
        private const val LIFETIME_TTL_MS = 60 * 60 * 1000L
        private const val FULL_REPORT_TTL_MS = 24 * 60 * 60 * 1000L
        private const val INCREMENTAL_DAYS = 3

        /** Contiguous YYYYMMDD key range covering [daysBack] days back through [today] (inclusive). */
        private fun keyRange(today: LocalDate, fromDaysBack: Int, toDaysBack: Int): ClosedRange<String> =
            formatDateKey(today.minus(fromDaysBack, DateTimeUnit.DAY))..formatDateKey(today.minus(toDaysBack, DateTimeUnit.DAY))

        val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun resolveCurrencySymbol(currencyCode: String): String {
            return when (currencyCode.uppercase()) {
                "EUR" -> "€"
                "GBP" -> "£"
                "INR" -> "₹"
                "JPY" -> "¥"
                "CAD" -> "CA$"
                "AUD" -> "A$"
                "BRL" -> "R$"
                else -> "$"
            }
        }

        fun parseReportRows(jsonText: String, outRows: MutableList<AdMobReportRow>) {
            val trimmed = jsonText.trim()
            if (trimmed.startsWith("[")) {
                // Standard JSON Array
                try {
                    val array = jsonParser.parseToJsonElement(trimmed).jsonArray
                    for (item in array) {
                        parseSingleChunk(item.jsonObject, outRows)
                    }
                } catch (e: Exception) {
                    println("AdMobApiClient JSON array parse error: ${e.message}")
                }
            } else {
                // Line-delimited JSON (NDJSON) or stream chunks
                trimmed.lineSequence().forEach { line ->
                    val lTrimmed = line.trim()
                    if (lTrimmed.startsWith("{") && lTrimmed.endsWith("}")) {
                        try {
                            val obj = jsonParser.parseToJsonElement(lTrimmed).jsonObject
                            parseSingleChunk(obj, outRows)
                        } catch (e: Exception) {
                            println("AdMobApiClient line parse error: ${e.message}")
                        }
                    }
                }
            }
        }

        private fun parseSingleChunk(obj: kotlinx.serialization.json.JsonObject, outRows: MutableList<AdMobReportRow>) {
            val row = obj["row"]?.jsonObject ?: return
            val dims = row["dimensionValues"]?.jsonObject ?: return
            val metrics = row["metricValues"]?.jsonObject ?: return

            val dateStr = dims["DATE"]?.jsonObject?.get("value")?.jsonPrimitive?.content ?: ""
            val appId = dims["APP"]?.jsonObject?.get("value")?.jsonPrimitive?.content ?: ""
            val appName = dims["APP"]?.jsonObject?.get("displayLabel")?.jsonPrimitive?.content
                ?: dims["APP"]?.jsonObject?.get("value")?.jsonPrimitive?.content
                ?: "AdMob App"
            val country = dims["COUNTRY"]?.jsonObject?.get("displayLabel")?.jsonPrimitive?.content
                ?: dims["COUNTRY"]?.jsonObject?.get("value")?.jsonPrimitive?.content
                ?: ""
            val format = dims["FORMAT"]?.jsonObject?.get("displayLabel")?.jsonPrimitive?.content
                ?: dims["FORMAT"]?.jsonObject?.get("value")?.jsonPrimitive?.content
                ?: dims["AD_FORMAT"]?.jsonObject?.get("displayLabel")?.jsonPrimitive?.content
                ?: dims["AD_FORMAT"]?.jsonObject?.get("value")?.jsonPrimitive?.content
                ?: "Standard"

            val earningsMicros = metrics["ESTIMATED_EARNINGS"]?.jsonObject?.get("microsValue")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            val earnings = earningsMicros / 1_000_000.0
            val imps = metrics["IMPRESSIONS"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val clicks = metrics["CLICKS"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val adRequests = metrics["AD_REQUESTS"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            val matchedRequests = metrics["MATCHED_REQUESTS"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L

            if (dateStr.isNotBlank()) {
                outRows.add(
                    AdMobReportRow(
                        date = dateStr,
                        appId = appId,
                        appName = appName,
                        format = format,
                        country = country,
                        earnings = earnings,
                        impressions = imps,
                        clicks = clicks,
                        adRequests = adRequests,
                        matchedRequests = matchedRequests
                    )
                )
            }
        }

        fun parseLifetimeEarnings(jsonText: String): Double {
            val trimmed = jsonText.trim()
            var total = 0.0
            fun extractFromChunk(obj: kotlinx.serialization.json.JsonObject) {
                val row = obj["row"]?.jsonObject ?: return
                val metrics = row["metricValues"]?.jsonObject ?: return
                val micros = metrics["ESTIMATED_EARNINGS"]?.jsonObject?.get("microsValue")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                total += micros / 1_000_000.0
            }

            if (trimmed.startsWith("[")) {
                try {
                    val array = jsonParser.parseToJsonElement(trimmed).jsonArray
                    for (item in array) {
                        extractFromChunk(item.jsonObject)
                    }
                } catch (_: Exception) {}
            } else {
                trimmed.lineSequence().forEach { line ->
                    val lTrimmed = line.trim()
                    if (lTrimmed.startsWith("{") && lTrimmed.endsWith("}")) {
                        try {
                            val obj = jsonParser.parseToJsonElement(lTrimmed).jsonObject
                            extractFromChunk(obj)
                        } catch (_: Exception) {}
                    }
                }
            }
            return roundTwoDecimals(total)
        }

        fun formatDateKey(date: LocalDate): String {
            return "${date.year}${date.month.number.toString().padStart(2, '0')}${date.day.toString().padStart(2, '0')}"
        }

        fun computeAccountSummary(
            rows: List<AdMobReportRow>,
            range: TimeRange,
            publisherId: String,
            today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
            currencyCode: String = "USD",
            currencySymbol: String = "$",
            customStartDate: LocalDate? = null,
            customEndDate: LocalDate? = null,
            customDailyGoal: Double? = null
        ): AccountSummary {
            val todayStr = formatDateKey(today)
            val yesterday = today.minus(1, DateTimeUnit.DAY)
            val yesterdayStr = formatDateKey(yesterday)

            // Dynamically scale daily goal based on recent completed 7-day average if available, min 10.0.
            // Using completed days (yesterday through 7 days ago) avoids partial-day distortion in the morning.
            val recentCompleted7Days = keyRange(today, 7, 1)
            val recentCompletedRows = rows.filter { it.date in recentCompleted7Days }
            val recent7Rev = recentCompletedRows.sumOf { it.earnings }
            val distinctDaysWithData = recentCompletedRows.map { it.date }.distinct().size

            val (effectiveRev, daysDivisor) = if (recent7Rev > 0.0) {
                recent7Rev to maxOf(1, distinctDaysWithData)
            } else {
                val recent7Days = keyRange(today, 6, 0)
                val rRows = rows.filter { it.date in recent7Days }
                val rRev = rRows.sumOf { it.earnings }
                val dCount = rRows.map { it.date }.distinct().size
                rRev to maxOf(1, dCount)
            }
            val avgDaily = if (effectiveRev > 0.0) (effectiveRev / daysDivisor.toDouble()) else 0.0
            val computedDailyGoal = when {
                avgDaily >= 100.0 -> ((avgDaily * 1.25) / 50.0).roundToLong() * 50.0
                avgDaily >= 10.0 -> ((avgDaily * 1.25) / 10.0).roundToLong() * 10.0
                avgDaily > 0.0 -> 10.0
                else -> 100.0
            }

            val isCustom = customDailyGoal != null && customDailyGoal > 0.0
            val effectiveDailyGoal = if (isCustom) customDailyGoal else computedDailyGoal

            return when (range) {
                TimeRange.TODAY -> {
                    val todayRows = rows.filter { it.date == todayStr }
                    val yesterdayRows = rows.filter { it.date == yesterdayStr }

                    val todayRev = roundTwoDecimals(todayRows.sumOf { it.earnings })
                    val yesterdayRev = roundTwoDecimals(yesterdayRows.sumOf { it.earnings })
                    val deltaAmt = roundTwoDecimals(todayRev - yesterdayRev)
                    val deltaPct = if (yesterdayRev > 0) roundOneDecimal(((todayRev - yesterdayRev) / yesterdayRev) * 100.0) else 0.0
                    val totalImp = todayRows.sumOf { it.impressions }
                    val totalClicks = todayRows.sumOf { it.clicks }
                    val avgEcpm = if (totalImp > 0) roundTwoDecimals((todayRev / totalImp) * 1000.0) else 0.0
                    val computedCtr = if (totalImp > 0) roundTwoDecimals((totalClicks.toDouble() / totalImp.toDouble()) * 100.0) else 0.0

                    AccountSummary(
                        todayRevenue = todayRev,
                        yesterdayRevenue = yesterdayRev,
                        deltaAmount = deltaAmt,
                        deltaPercentage = deltaPct,
                        dailyGoal = effectiveDailyGoal,
                        periodGoal = effectiveDailyGoal,
                        periodDays = 1,
                        dailyGoalProgress = if (todayRev > 0) (todayRev / effectiveDailyGoal).toFloat().coerceAtLeast(0f) else 0f,
                        ecpm = avgEcpm,
                        impressions = totalImp,
                        fillRate = matchRate(todayRows),
                        lastSyncTime = run {
                            val maskedPub = maskPublisherId(publisherId)
                            if (maskedPub.isNotBlank()) "Live • AdMob API ($maskedPub)" else "Live • AdMob API"
                        },
                        currencyCode = currencyCode,
                        currencySymbol = currencySymbol,
                        ctr = computedCtr,
                        clicks = totalClicks,
                        isCustomGoal = isCustom
                    )
                }

                TimeRange.YESTERDAY -> {
                    val yesterdayRows = rows.filter { it.date == yesterdayStr }
                    val dayBeforeYesterdayStr = formatDateKey(today.minus(2, DateTimeUnit.DAY))
                    val dayBeforeRows = rows.filter { it.date == dayBeforeYesterdayStr }

                    val yestRev = roundTwoDecimals(yesterdayRows.sumOf { it.earnings })
                    val dayBeforeRev = roundTwoDecimals(dayBeforeRows.sumOf { it.earnings })
                    val deltaAmt = roundTwoDecimals(yestRev - dayBeforeRev)
                    val deltaPct = if (dayBeforeRev > 0) roundOneDecimal(((yestRev - dayBeforeRev) / dayBeforeRev) * 100.0) else 0.0
                    val totalImp = yesterdayRows.sumOf { it.impressions }
                    val totalClicks = yesterdayRows.sumOf { it.clicks }
                    val avgEcpm = if (totalImp > 0) roundTwoDecimals((yestRev / totalImp) * 1000.0) else 0.0
                    val computedCtr = if (totalImp > 0) roundTwoDecimals((totalClicks.toDouble() / totalImp.toDouble()) * 100.0) else 0.0

                    AccountSummary(
                        todayRevenue = yestRev,
                        yesterdayRevenue = dayBeforeRev,
                        deltaAmount = deltaAmt,
                        deltaPercentage = deltaPct,
                        dailyGoal = effectiveDailyGoal,
                        periodGoal = effectiveDailyGoal,
                        periodDays = 1,
                        dailyGoalProgress = if (yestRev > 0) (yestRev / effectiveDailyGoal).toFloat().coerceAtLeast(0f) else 0f,
                        ecpm = avgEcpm,
                        impressions = totalImp,
                        fillRate = matchRate(yesterdayRows),
                        lastSyncTime = "Finalized Yesterday",
                        currencyCode = currencyCode,
                        currencySymbol = currencySymbol,
                        ctr = computedCtr,
                        clicks = totalClicks,
                        isCustomGoal = isCustom
                    )
                }

                TimeRange.LAST_7_DAYS -> {
                    val days7 = keyRange(today, 6, 0)
                    val prev7 = keyRange(today, 13, 7)

                    val curRows = rows.filter { it.date in days7 }
                    val prevRows = rows.filter { it.date in prev7 }

                    val curRev = roundTwoDecimals(curRows.sumOf { it.earnings })
                    val prevRev = roundTwoDecimals(prevRows.sumOf { it.earnings })
                    val deltaAmt = roundTwoDecimals(curRev - prevRev)
                    val deltaPct = if (prevRev > 0) roundOneDecimal(((curRev - prevRev) / prevRev) * 100.0) else 0.0
                    val totalImp = curRows.sumOf { it.impressions }
                    val totalClicks = curRows.sumOf { it.clicks }
                    val avgEcpm = if (totalImp > 0) roundTwoDecimals((curRev / totalImp) * 1000.0) else 0.0
                    val computedCtr = if (totalImp > 0) roundTwoDecimals((totalClicks.toDouble() / totalImp.toDouble()) * 100.0) else 0.0
                    val periodGoal = effectiveDailyGoal * 7.0

                    AccountSummary(
                        todayRevenue = curRev,
                        yesterdayRevenue = prevRev,
                        deltaAmount = deltaAmt,
                        deltaPercentage = deltaPct,
                        dailyGoal = effectiveDailyGoal,
                        periodGoal = periodGoal,
                        periodDays = 7,
                        dailyGoalProgress = if (curRev > 0) (curRev / periodGoal).toFloat().coerceAtLeast(0f) else 0f,
                        ecpm = avgEcpm,
                        impressions = totalImp,
                        fillRate = matchRate(curRows),
                        lastSyncTime = "Past 7 Days Total",
                        currencyCode = currencyCode,
                        currencySymbol = currencySymbol,
                        ctr = computedCtr,
                        clicks = totalClicks,
                        isCustomGoal = isCustom
                    )
                }

                TimeRange.THIS_MONTH -> {
                    // Month-to-date, compared with the same number of days at the start of last month (AdMob style)
                    val monthStart = LocalDate(today.year, today.month, 1)
                    val elapsedDays = today.day
                    val prevMonthStart = monthStart.minus(1, DateTimeUnit.MONTH)
                    val prevMonthEnd = monthStart.minus(1, DateTimeUnit.DAY)
                    val prevCompareEnd = minOf(prevMonthStart.plus(elapsedDays - 1, DateTimeUnit.DAY), prevMonthEnd)
                    val curRange = formatDateKey(monthStart)..formatDateKey(today)
                    val prevRange = formatDateKey(prevMonthStart)..formatDateKey(prevCompareEnd)

                    val curRows = rows.filter { it.date in curRange }
                    val prevRows = rows.filter { it.date in prevRange }

                    val curRev = roundTwoDecimals(curRows.sumOf { it.earnings })
                    val prevRev = roundTwoDecimals(prevRows.sumOf { it.earnings })
                    val deltaAmt = roundTwoDecimals(curRev - prevRev)
                    val deltaPct = if (prevRev > 0) roundOneDecimal(((curRev - prevRev) / prevRev) * 100.0) else 0.0
                    val totalImp = curRows.sumOf { it.impressions }
                    val totalClicks = curRows.sumOf { it.clicks }
                    val avgEcpm = if (totalImp > 0) roundTwoDecimals((curRev / totalImp) * 1000.0) else 0.0
                    val computedCtr = if (totalImp > 0) roundTwoDecimals((totalClicks.toDouble() / totalImp.toDouble()) * 100.0) else 0.0
                    val daysInMonth = daysInMonth(today)
                    val monthGoal = effectiveDailyGoal * daysInMonth

                    AccountSummary(
                        todayRevenue = curRev,
                        yesterdayRevenue = prevRev,
                        deltaAmount = deltaAmt,
                        deltaPercentage = deltaPct,
                        dailyGoal = effectiveDailyGoal,
                        periodGoal = monthGoal,
                        periodDays = daysInMonth,
                        dailyGoalProgress = if (curRev > 0) (curRev / monthGoal).toFloat().coerceAtLeast(0f) else 0f,
                        ecpm = avgEcpm,
                        impressions = totalImp,
                        fillRate = matchRate(curRows),
                        lastSyncTime = "Month to date (${elapsedDays}d)",
                        currencyCode = currencyCode,
                        currencySymbol = currencySymbol,
                        ctr = computedCtr,
                        clicks = totalClicks,
                        isCustomGoal = isCustom
                    )
                }

                TimeRange.DAYS_30 -> {
                    val days30 = keyRange(today, 29, 0)
                    val prev30 = keyRange(today, 59, 30)

                    val curRows = rows.filter { it.date in days30 }
                    val prevRows = rows.filter { it.date in prev30 }

                    val curRev = roundTwoDecimals(curRows.sumOf { it.earnings })
                    val prevRev = roundTwoDecimals(prevRows.sumOf { it.earnings })
                    val deltaAmt = roundTwoDecimals(curRev - prevRev)
                    val deltaPct = if (prevRev > 0) roundOneDecimal(((curRev - prevRev) / prevRev) * 100.0) else 0.0
                    val totalImp = curRows.sumOf { it.impressions }
                    val totalClicks = curRows.sumOf { it.clicks }
                    val avgEcpm = if (totalImp > 0) roundTwoDecimals((curRev / totalImp) * 1000.0) else 0.0
                    val computedCtr = if (totalImp > 0) roundTwoDecimals((totalClicks.toDouble() / totalImp.toDouble()) * 100.0) else 0.0
                    val monthlyGoal = effectiveDailyGoal * 30.0

                    AccountSummary(
                        todayRevenue = curRev,
                        yesterdayRevenue = prevRev,
                        deltaAmount = deltaAmt,
                        deltaPercentage = deltaPct,
                        dailyGoal = effectiveDailyGoal,
                        periodGoal = monthlyGoal,
                        periodDays = 30,
                        dailyGoalProgress = if (curRev > 0) (curRev / monthlyGoal).toFloat().coerceAtLeast(0f) else 0f,
                        ecpm = avgEcpm,
                        impressions = totalImp,
                        fillRate = matchRate(curRows),
                        lastSyncTime = "Past 30 Days Total",
                        currencyCode = currencyCode,
                        currencySymbol = currencySymbol,
                        ctr = computedCtr,
                        clicks = totalClicks,
                        isCustomGoal = isCustom
                    )
                }

                TimeRange.CUSTOM -> {
                    val start = customStartDate ?: today.minus(29, DateTimeUnit.DAY)
                    val end = customEndDate ?: today
                    val startStr = formatDateKey(start)
                    val endStr = formatDateKey(end)

                    val filteredRows = rows.filter { it.date in startStr..endStr }
                    val totalRev = roundTwoDecimals(filteredRows.sumOf { it.earnings })
                    val totalImp = filteredRows.sumOf { it.impressions }
                    val totalClicks = filteredRows.sumOf { it.clicks }
                    val avgEcpm = if (totalImp > 0) roundTwoDecimals((totalRev / totalImp) * 1000.0) else 0.0
                    val computedCtr = if (totalImp > 0) roundTwoDecimals((totalClicks.toDouble() / totalImp.toDouble()) * 100.0) else 0.0
                    val daysCount = maxOf(1L, (end.toEpochDays() - start.toEpochDays()) + 1L).toInt()
                    val periodGoal = effectiveDailyGoal * daysCount.toDouble()

                    val rangeLabel = if (customStartDate != null && customEndDate != null) {
                        "${customStartDate.month.number}/${customStartDate.day} - ${customEndDate.month.number}/${customEndDate.day}"
                    } else {
                        "Custom Date Range"
                    }

                    AccountSummary(
                        todayRevenue = totalRev,
                        yesterdayRevenue = 0.0,
                        deltaAmount = totalRev,
                        deltaPercentage = 0.0,
                        dailyGoal = effectiveDailyGoal,
                        periodGoal = periodGoal,
                        periodDays = daysCount,
                        dailyGoalProgress = if (totalRev > 0) (totalRev / periodGoal).toFloat().coerceAtLeast(0f) else 0f,
                        ecpm = avgEcpm,
                        impressions = totalImp,
                        fillRate = matchRate(filteredRows),
                        lastSyncTime = rangeLabel,
                        currencyCode = currencyCode,
                        currencySymbol = currencySymbol,
                        ctr = computedCtr,
                        clicks = totalClicks,
                        isCustomGoal = isCustom
                    )
                }
            }
        }

        fun computeAppsMonetization(
            rows: List<AdMobReportRow>,
            registeredApps: List<AppMonetization>,
            today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
            range: TimeRange = TimeRange.TODAY,
            customStartDate: LocalDate? = null,
            customEndDate: LocalDate? = null
        ): List<AppMonetization> {
            val todayStr = formatDateKey(today)
            val yesterdayStr = formatDateKey(today.minus(1, DateTimeUnit.DAY))
            val days7 = keyRange(today, 6, 0)
            val days30 = keyRange(today, 29, 0)
            val customStartStr = if (customStartDate != null) formatDateKey(customStartDate) else formatDateKey(today.minus(30, DateTimeUnit.DAY))
            val customEndStr = if (customEndDate != null) formatDateKey(customEndDate) else todayStr
            val monthStartStr = formatDateKey(LocalDate(today.year, today.month, 1))
            val (curStart, curEnd) = rangeWindow(range, today, customStartDate, customEndDate)
            val spanDays = (curEnd.toEpochDays() - curStart.toEpochDays()).toInt() + 1
            val prevRange = formatDateKey(curStart.minus(spanDays, DateTimeUnit.DAY))..formatDateKey(curStart.minus(1, DateTimeUnit.DAY))

            // Build map of registered apps by ID and lowercase name for quick lookup
            val regAppById = registeredApps.associateBy { it.id }
            val regAppByName = registeredApps.associateBy { it.name.lowercase() }

            // Group rows by appId if present, otherwise by appName
            val rowsByApp = rows.groupBy { row ->
                if (row.appId.isNotBlank()) row.appId else row.appName.ifBlank { "AdMob App" }
            }

            val appsList = mutableListOf<AppMonetization>()

            // 1. Build apps from rows with monetization activity
            rowsByApp.forEach { (appKey, appRows) ->
                val sampleRow = appRows.first()
                val matchedRegApp = regAppById[sampleRow.appId]
                    ?: regAppByName[sampleRow.appName.lowercase()]

                val resolvedAppId = if (sampleRow.appId.isNotBlank()) sampleRow.appId else (matchedRegApp?.id ?: "app-${appKey.hashCode()}")
                val resolvedAppName = matchedRegApp?.name ?: sampleRow.appName.ifBlank { "AdMob App" }
                val resolvedPlatform = matchedRegApp?.platform ?: if (resolvedAppName.contains("iOS", ignoreCase = true)) "iOS" else "Android"

                val activeRows = when (range) {
                    TimeRange.TODAY -> appRows.filter { it.date == todayStr }
                    TimeRange.YESTERDAY -> appRows.filter { it.date == yesterdayStr }
                    TimeRange.LAST_7_DAYS -> appRows.filter { it.date in days7 }
                    TimeRange.THIS_MONTH -> appRows.filter { it.date in monthStartStr..todayStr }
                    TimeRange.DAYS_30 -> appRows.filter { it.date in days30 }
                    TimeRange.CUSTOM -> appRows.filter { it.date in customStartStr..customEndStr }
                }

                // Compare against the previous period of equal length (not always "yesterday")
                val yesterdayRows = appRows.filter { it.date in prevRange }

                val periodRev = roundTwoDecimals(activeRows.sumOf { it.earnings })
                val yestRev = roundTwoDecimals(yesterdayRows.sumOf { it.earnings })
                val delta = roundTwoDecimals(periodRev - yestRev)
                val totalImp = activeRows.sumOf { it.impressions }
                val avgEcpm = if (totalImp > 0) roundTwoDecimals((periodRev / totalImp) * 1000.0) else 0.0

                // Build ad format breakdown for this app
                // If the selected range has activity, use activeRows; otherwise fallback to app's recent rows so formats aren't blank
                val formatSourceRows = if (activeRows.isNotEmpty()) activeRows else appRows
                val formatGroups = formatSourceRows.groupBy { it.format }
                val sourceRev = formatSourceRows.sumOf { it.earnings }.coerceAtLeast(0.01)

                val formats = formatGroups.map { (formatName, fRows) ->
                    val fRev = roundTwoDecimals(fRows.sumOf { it.earnings })
                    val fImp = fRows.sumOf { it.impressions }
                    val fEcpm = if (fImp > 0) roundTwoDecimals((fRev / fImp) * 1000.0) else 0.0
                    val share = roundOneDecimal((fRev / sourceRev) * 100.0)
                    AdFormatMetric(
                        formatName = formatName,
                        revenue = fRev,
                        ecpm = fEcpm,
                        impressions = fImp,
                        fillRate = matchRate(fRows),
                        sharePercent = share
                    )
                }

                appsList.add(
                    AppMonetization(
                        id = resolvedAppId,
                        name = resolvedAppName,
                        category = "Live Monetization",
                        platform = resolvedPlatform,
                        todayRevenue = periodRev,
                        deltaVsYesterday = delta,
                        totalImpressions = totalImp,
                        fillRate = matchRate(activeRows),
                        avgEcpm = avgEcpm,
                        formats = formats,
                        appStoreId = matchedRegApp?.appStoreId,
                        iconUrl = matchedRegApp?.iconUrl
                    )
                )
            }

            // 2. Add registered apps from AdMob console that had 0 rows in the report
            val seenIds = appsList.mapTo(HashSet()) { it.id }
            val seenNames = appsList.mapTo(HashSet()) { it.name.lowercase() }
            registeredApps.forEach { regApp ->
                if (regApp.id !in seenIds && regApp.name.lowercase() !in seenNames) {
                    appsList.add(regApp)
                    seenIds.add(regApp.id)
                    seenNames.add(regApp.name.lowercase())
                }
            }

            return appsList.sortedByDescending { it.todayRevenue }
        }

        fun computeAnalytics(
            rows: List<AdMobReportRow>,
            range: AnalyticsRange,
            today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        ): AnalyticsSummary {
            return when (range) {
                AnalyticsRange.DAYS_7 -> {
                    val days = (6 downTo 0).map { today.minus(it, DateTimeUnit.DAY) }
                    val dateRange = keyRange(today, 6, 0)
                    val filteredRows = rows.filter { it.date in dateRange }

                    val totalRev = roundTwoDecimals(filteredRows.sumOf { it.earnings })
                    val totalImp = filteredRows.sumOf { it.impressions }
                    val avgEcpm = if (totalImp > 0) roundTwoDecimals((totalRev / totalImp) * 1000.0) else 0.0

                    val revByDate = HashMap<String, Double>()
                    filteredRows.forEach { revByDate[it.date] = (revByDate[it.date] ?: 0.0) + it.earnings }
                    val trendPoints = days.mapIndexed { idx, day ->
                        val dStr = formatDateKey(day)
                        val dayRev = roundTwoDecimals(revByDate[dStr] ?: 0.0)
                        val label = when (day.dayOfWeek.name) {
                            "MONDAY" -> "Mon"
                            "TUESDAY" -> "Tue"
                            "WEDNESDAY" -> "Wed"
                            "THURSDAY" -> "Thu"
                            "FRIDAY" -> "Fri"
                            "SATURDAY" -> "Sat"
                            "SUNDAY" -> "Sun"
                            else -> "${day.month.number}/${day.day}"
                        }
                        RevenuePoint(
                            dayLabel = label,
                            amount = dayRev,
                            isCurrentOrPeak = (idx == days.lastIndex)
                        )
                    }

                    val territories = computeTerritories(filteredRows)

                    AnalyticsSummary(
                        periodLabel = "Past 7 Days",
                        totalRevenue = totalRev,
                        deltaPercent = 0.0,
                        avgEcpm = avgEcpm,
                        totalImpressions = totalImp,
                        trendPoints = trendPoints,
                        territories = territories
                    )
                }

                AnalyticsRange.DAYS_30 -> {
                    val days = (29 downTo 0).map { today.minus(it, DateTimeUnit.DAY) }
                    val dateRange = keyRange(today, 29, 0)
                    val filteredRows = rows.filter { it.date in dateRange }

                    val totalRev = roundTwoDecimals(filteredRows.sumOf { it.earnings })
                    val totalImp = filteredRows.sumOf { it.impressions }
                    val avgEcpm = if (totalImp > 0) roundTwoDecimals((totalRev / totalImp) * 1000.0) else 0.0
                    val revByDate = HashMap<String, Double>()
                    filteredRows.forEach { revByDate[it.date] = (revByDate[it.date] ?: 0.0) + it.earnings }

                    // 5 even 6-day buckets (previously chunked(7) produced a misleading 2-day "W5")
                    val chunkedDays = days.chunked(6)
                    val trendPoints = chunkedDays.mapIndexed { idx, weekDays ->
                        val weekRev = roundTwoDecimals(weekDays.sumOf { revByDate[formatDateKey(it)] ?: 0.0 })
                        RevenuePoint(
                            dayLabel = "${weekDays.first().month.number}/${weekDays.first().day}",
                            amount = weekRev,
                            isCurrentOrPeak = (idx == chunkedDays.lastIndex)
                        )
                    }

                    val territories = computeTerritories(filteredRows)

                    AnalyticsSummary(
                        periodLabel = "Past 30 Days",
                        totalRevenue = totalRev,
                        deltaPercent = 0.0,
                        avgEcpm = avgEcpm,
                        totalImpressions = totalImp,
                        trendPoints = trendPoints,
                        territories = territories
                    )
                }

                AnalyticsRange.DAYS_90 -> {
                    val days = (89 downTo 0).map { today.minus(it, DateTimeUnit.DAY) }
                    val dateRange = keyRange(today, 89, 0)
                    val filteredRows = rows.filter { it.date in dateRange }

                    val totalRev = roundTwoDecimals(filteredRows.sumOf { it.earnings })
                    val totalImp = filteredRows.sumOf { it.impressions }
                    val avgEcpm = if (totalImp > 0) roundTwoDecimals((totalRev / totalImp) * 1000.0) else 0.0

                    // Group by month
                    val rowsByMonth = filteredRows.groupBy { it.date.take(6) }
                    val sortedMonths = rowsByMonth.keys.sorted()
                    val trendPoints = if (sortedMonths.isNotEmpty()) {
                        sortedMonths.mapIndexed { idx, mStr ->
                            val mRev = roundTwoDecimals(rowsByMonth[mStr]?.sumOf { it.earnings } ?: 0.0)
                            val monthNum = mStr.takeLast(2).toIntOrNull() ?: 1
                            val monthName = when (monthNum) {
                                1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
                                7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                                else -> mStr
                            }
                            RevenuePoint(
                                dayLabel = monthName,
                                amount = mRev,
                                isCurrentOrPeak = (idx == sortedMonths.lastIndex)
                            )
                        }
                    } else {
                        listOf(
                            RevenuePoint("M1", 0.0),
                            RevenuePoint("M2", 0.0),
                            RevenuePoint("Current", 0.0, isCurrentOrPeak = true)
                        )
                    }

                    val territories = computeTerritories(filteredRows)

                    AnalyticsSummary(
                        periodLabel = "Past 90 Days",
                        totalRevenue = totalRev,
                        deltaPercent = 0.0,
                        avgEcpm = avgEcpm,
                        totalImpressions = totalImp,
                        trendPoints = trendPoints,
                        territories = territories
                    )
                }
            }
        }

        private fun computeTerritories(rows: List<AdMobReportRow>): List<TerritoryYield> {
            val countryRows = rows.filter { it.country.isNotBlank() }
            if (countryRows.isEmpty()) {
                return emptyList()
            }

            val countryEarnings = countryRows.groupBy { it.country }
                .mapValues { (_, rList) -> roundTwoDecimals(rList.sumOf { it.earnings }) }
                .filterValues { it >= 0.0 }

            val totalCountryRev = countryEarnings.values.sum().coerceAtLeast(0.01)

            return countryEarnings.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { (country, rev) ->
                    val flag = when (country.lowercase()) {
                        "united states", "usa", "us" -> "🇺🇸"
                        "japan", "jp" -> "🇯🇵"
                        "germany", "de" -> "🇩🇪"
                        "united kingdom", "uk", "gb" -> "🇬🇧"
                        "india", "in" -> "🇮🇳"
                        "canada", "ca" -> "🇨🇦"
                        "france", "fr" -> "🇫🇷"
                        "brazil", "br" -> "🇧🇷"
                        "australia", "au" -> "🇦🇺"
                        else -> "🌐"
                    }
                    val share = roundOneDecimal((rev / totalCountryRev) * 100.0)
                    TerritoryYield(flag, country, rev, share)
                }
        }

        fun computeWallet(rows: List<AdMobReportRow>, lifetimeEarnings: Double? = null): WalletSummary {
            val totalEarnings = if (lifetimeEarnings != null && lifetimeEarnings > 0.0) {
                lifetimeEarnings
            } else {
                roundTwoDecimals(rows.sumOf { it.earnings })
            }
            val threshold = 100.0
            val percent = (totalEarnings / threshold).toFloat().coerceIn(0f, 1f)

            return WalletSummary(
                unpaidBalance = totalEarnings,
                thresholdAmount = threshold,
                thresholdPercent = percent,
                nextDisbursementDate = "21st of next month",
                daysRemaining = 0,
                bankAccountTitle = "Google AdSense Payout",
                bankAccountMasked = if (totalEarnings > 0) "Linked" else "Awaiting Setup",
                isBankVerified = totalEarnings > 0,
                adServingStatus = if (rows.isNotEmpty() || totalEarnings > 0) "Active & Serving" else "Ready to Monetize",
                policyIssuesCount = 0,
                pastPayouts = emptyList()
            )
        }

        /** Real AdMob match rate (matched requests / ad requests). 0 when the API didn't return request metrics. */
        fun matchRate(rows: List<AdMobReportRow>): Double {
            val req = rows.sumOf { it.adRequests }
            if (req <= 0L) return 0.0
            return roundOneDecimal(rows.sumOf { it.matchedRequests }.toDouble() / req.toDouble() * 100.0)
        }

        fun daysInMonth(date: LocalDate): Int {
            val first = LocalDate(date.year, date.month, 1)
            return first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
        }

        /** Inclusive [start, end] dates covered by a [TimeRange]. */
        fun rangeWindow(
            range: TimeRange,
            today: LocalDate,
            customStartDate: LocalDate? = null,
            customEndDate: LocalDate? = null
        ): Pair<LocalDate, LocalDate> = when (range) {
            TimeRange.TODAY -> today to today
            TimeRange.YESTERDAY -> today.minus(1, DateTimeUnit.DAY).let { it to it }
            TimeRange.LAST_7_DAYS -> today.minus(6, DateTimeUnit.DAY) to today
            TimeRange.THIS_MONTH -> LocalDate(today.year, today.month, 1) to today
            TimeRange.DAYS_30 -> today.minus(29, DateTimeUnit.DAY) to today
            TimeRange.CUSTOM -> (customStartDate ?: today.minus(29, DateTimeUnit.DAY)) to (customEndDate ?: today)
        }

        private fun roundTwoDecimals(v: Double): Double {
            return (v * 100.0).roundToLong() / 100.0
        }

        private fun roundOneDecimal(v: Double): Double {
            return (v * 10.0).roundToLong() / 10.0
        }
    }
}
