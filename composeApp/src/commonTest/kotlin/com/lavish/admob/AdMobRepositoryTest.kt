package com.lavish.admob

import com.russhwolf.settings.MapSettings
import com.lavish.admob.data.AdMobApiClient
import com.lavish.admob.data.AdMobReportRow
import com.lavish.admob.data.AdMobRepository
import com.lavish.admob.model.AccountSummary
import com.lavish.admob.model.AppMonetization
import com.lavish.admob.model.AutoRefreshInterval
import com.lavish.admob.model.TimeRange
import com.lavish.admob.model.maskPublisherId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdMobRepositoryTest {

    @Test
    fun testDefaultStateIsLoggedOut() {
        val repo = AdMobRepository(settings = null)
        assertFalse(repo.session.value.isLoggedIn)
        assertEquals("", repo.session.value.userName)
        assertEquals("", repo.session.value.email)
    }

    @Test
    fun testEmptyTokenRejection() {
        val repo = AdMobRepository(settings = null)
        repo.loginWithApiToken("   ")
        assertFalse(repo.session.value.isLoggedIn, "Should not log in with empty token")
        assertNotNull(repo.authError.value, "Auth error should be reported")
    }

    @Test
    fun testGoogleProfileLoginAndLogout() {
        val repo = AdMobRepository(settings = null)
        repo.loginWithGoogleProfile(name = "Test Publisher", email = "publisher@test.com")
        assertTrue(repo.session.value.isLoggedIn, "Should log in with valid profile")
        assertEquals("Test Publisher", repo.session.value.userName)
        assertEquals("publisher@test.com", repo.session.value.email)

        repo.logout()
        assertFalse(repo.session.value.isLoggedIn, "Should log out")
        assertEquals("", repo.session.value.userName)
    }

    @Test
    fun testParseReportRowsWithFormatAndArray() {
        val jsonArray = """
        [
          {
            "header": { "dateRange": { "startDate": { "year": 2026, "month": 11, "day": 1 }, "endDate": { "year": 2026, "month": 11, "day": 19 } } }
          },
          {
            "row": {
              "dimensionValues": {
                "DATE": { "value": "20261119" },
                "APP": { "value": "ca-app-pub-123456789~1111111111", "displayLabel": "Solar Runner" },
                "FORMAT": { "value": "BANNER", "displayLabel": "Banner" },
                "COUNTRY": { "value": "US", "displayLabel": "United States" }
              },
              "metricValues": {
                "ESTIMATED_EARNINGS": { "microsValue": "4500000" },
                "IMPRESSIONS": { "integerValue": "1500" },
                "CLICKS": { "integerValue": "45" }
              }
            }
          },
          {
            "footer": { "matchingRowCount": 1 }
          }
        ]
        """.trimIndent()

        val rows = mutableListOf<AdMobReportRow>()
        AdMobApiClient.parseReportRows(jsonArray, rows)

        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals("20261119", row.date)
        assertEquals("ca-app-pub-123456789~1111111111", row.appId)
        assertEquals("Solar Runner", row.appName)
        assertEquals("Banner", row.format)
        assertEquals("United States", row.country)
        assertEquals(4.50, row.earnings)
        assertEquals(1500, row.impressions)
        assertEquals(45, row.clicks)
    }

    @Test
    fun testParseReportRowsWithNDJSONAndFallback() {
        val ndjson = """
        {"header":{}}
        {"row":{"dimensionValues":{"DATE":{"value":"20261118"},"APP":{"value":"ca-app-pub-999~222"},"FORMAT":{"value":"INTERSTITIAL"},"COUNTRY":{"value":"IN"}},"metricValues":{"ESTIMATED_EARNINGS":{"microsValue":"10000000"},"IMPRESSIONS":{"integerValue":"2000"},"CLICKS":{"integerValue":"120"}}}}
        {"footer":{}}
        """.trimIndent()

        val rows = mutableListOf<AdMobReportRow>()
        AdMobApiClient.parseReportRows(ndjson, rows)

        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals("20261118", row.date)
        assertEquals("ca-app-pub-999~222", row.appId)
        assertEquals("ca-app-pub-999~222", row.appName) // Falls back to appId value
        assertEquals("INTERSTITIAL", row.format) // Falls back to format value
        assertEquals("IN", row.country) // Falls back to country value
        assertEquals(10.0, row.earnings)
        assertEquals(2000, row.impressions)
        assertEquals(120, row.clicks)
    }

    @Test
    fun testComputeAccountSummaryWithTargetDate() {
        val testDate = LocalDate(2026, 11, 19)
        val rows = listOf(
            AdMobReportRow(
                date = "20261119",
                appId = "app1",
                appName = "App One",
                format = "Banner",
                country = "US",
                earnings = 15.50,
                impressions = 3000,
                clicks = 60
            ),
            AdMobReportRow(
                date = "20261118",
                appId = "app1",
                appName = "App One",
                format = "Banner",
                country = "US",
                earnings = 10.00,
                impressions = 2000,
                clicks = 40
            )
        )

        val summary = AdMobApiClient.computeAccountSummary(
            rows = rows,
            range = TimeRange.TODAY,
            publisherId = "pub-12345",
            today = testDate,
            currencyCode = "USD",
            currencySymbol = "$"
        )

        assertEquals(15.50, summary.todayRevenue)
        assertEquals(10.00, summary.yesterdayRevenue)
        assertEquals(5.50, summary.deltaAmount)
        assertEquals(55.0, summary.deltaPercentage)
        assertEquals(3000, summary.impressions)
        assertEquals(5.17, summary.ecpm)
        assertEquals("$", summary.currencySymbol)
    }

    @Test
    fun testComputeAppsMonetizationMergesRegisteredApps() {
        val testDate = LocalDate(2026, 11, 19)
        val registeredApps = listOf(
            AppMonetization(
                id = "ca-app-pub-123~001",
                name = "Solar Tracker Pro",
                category = "Utility",
                platform = "iOS",
                todayRevenue = 0.0,
                deltaVsYesterday = 0.0,
                totalImpressions = 0,
                fillRate = 0.0,
                avgEcpm = 0.0
            ),
            AppMonetization(
                id = "ca-app-pub-123~002",
                name = "Idle AdMob Tycoon",
                category = "Game",
                platform = "Android",
                todayRevenue = 0.0,
                deltaVsYesterday = 0.0,
                totalImpressions = 0,
                fillRate = 0.0,
                avgEcpm = 0.0
            )
        )

        val reportRows = listOf(
            AdMobReportRow(
                date = "20261119",
                appId = "ca-app-pub-123~001",
                appName = "Solar Tracker Pro",
                format = "Rewarded",
                country = "US",
                earnings = 25.0,
                impressions = 500,
                clicks = 50
            )
        )

        val computedApps = AdMobApiClient.computeAppsMonetization(reportRows, registeredApps, testDate)

        assertEquals(2, computedApps.size)
        val activeApp = computedApps.first { it.id == "ca-app-pub-123~001" }
        assertEquals("iOS", activeApp.platform, "Should keep real platform from registeredApps")
        assertEquals(25.0, activeApp.todayRevenue)
        assertEquals(1, activeApp.formats.size)
        assertEquals("Rewarded", activeApp.formats.first().formatName)

        val inactiveApp = computedApps.first { it.id == "ca-app-pub-123~002" }
        assertEquals("Android", inactiveApp.platform)
        assertEquals(0.0, inactiveApp.todayRevenue)
    }

    @Test
    fun testLast7DaysAndCustomRangeSummary() {
        val testDate = LocalDate(2026, 11, 19)
        val rows = listOf(
            AdMobReportRow("20261119", "app1", "App", "Banner", "US", 10.0, 1000, 20),
            AdMobReportRow("20261118", "app1", "App", "Banner", "US", 20.0, 2000, 40),
            AdMobReportRow("20261115", "app1", "App", "Banner", "US", 30.0, 3000, 60),
            AdMobReportRow("20261101", "app1", "App", "Banner", "US", 50.0, 5000, 100)
        )

        val summary7Days = AdMobApiClient.computeAccountSummary(
            rows = rows,
            range = TimeRange.LAST_7_DAYS,
            publisherId = "pub-12345",
            today = testDate
        )
        // 11/19, 11/18, 11/15 are within 7 days (60.0), 11/01 is older
        assertEquals(60.0, summary7Days.todayRevenue)
        assertEquals(6000, summary7Days.impressions)

        val customSummary = AdMobApiClient.computeAccountSummary(
            rows = rows,
            range = TimeRange.CUSTOM,
            publisherId = "pub-12345",
            today = testDate,
            customStartDate = LocalDate(2026, 11, 1),
            customEndDate = LocalDate(2026, 11, 18)
        )
        // 11/01 (50.0) + 11/15 (30.0) + 11/18 (20.0) = 100.0
        assertEquals(100.0, customSummary.todayRevenue)
        assertEquals(10000, customSummary.impressions)
    }

    @Test
    fun testShortLabelsAnd30DayRange() {
        assertEquals("td", TimeRange.TODAY.label)
        assertEquals("yd", TimeRange.YESTERDAY.label)
        assertEquals("7d", TimeRange.LAST_7_DAYS.label)
        assertEquals("30d", TimeRange.DAYS_30.label)
        assertEquals("custom", TimeRange.CUSTOM.label)

        val testDate = LocalDate(2026, 11, 19)
        val rows = listOf(
            AdMobReportRow("20261119", "app1", "App", "Banner", "US", 10.0, 1000, 20),
            AdMobReportRow("20261101", "app1", "App", "Banner", "US", 50.0, 5000, 100),
            AdMobReportRow("20261025", "app1", "App", "Banner", "US", 40.0, 4000, 80),
            AdMobReportRow("20260901", "app1", "App", "Banner", "US", 99.0, 9000, 99) // older than 30 days
        )

        val summary30 = AdMobApiClient.computeAccountSummary(
            rows = rows,
            range = TimeRange.DAYS_30,
            publisherId = "pub-12345",
            today = testDate
        )
        // 11/19 (10.0) + 11/01 (50.0) + 10/25 (40.0) = 100.0
        assertEquals(100.0, summary30.todayRevenue)
        assertEquals(10000, summary30.impressions)
    }

    @Test
    fun testAutoRefreshIntervalDefaultAndUpdates() {
        val repo = AdMobRepository(settings = null)
        assertEquals(AutoRefreshInterval.MINUTE_1, repo.autoRefreshInterval.value)

        repo.setAutoRefreshInterval(AutoRefreshInterval.SECONDS_30)
        assertEquals(AutoRefreshInterval.SECONDS_30, repo.autoRefreshInterval.value)

        repo.setAutoRefreshInterval(AutoRefreshInterval.OFF)
        assertEquals(AutoRefreshInterval.OFF, repo.autoRefreshInterval.value)
    }

    @Test
    fun testAutoRefreshIntervalPersistence() {
        val settings = MapSettings()
        val repo1 = AdMobRepository(settings = settings, cacheFiles = null)
        assertEquals(AutoRefreshInterval.MINUTE_1, repo1.autoRefreshInterval.value)

        repo1.setAutoRefreshInterval(AutoRefreshInterval.MINUTES_5)
        assertEquals(AutoRefreshInterval.MINUTES_5, repo1.autoRefreshInterval.value)

        // New repo instance loading from same settings
        val repo2 = AdMobRepository(settings = settings, cacheFiles = null)
        assertEquals(AutoRefreshInterval.MINUTES_5, repo2.autoRefreshInterval.value)
    }

    @Test
    fun testLastRefreshTimestampTracksRefreshes() {
        val repo = AdMobRepository(settings = null)
        val initialTimestamp = repo.lastRefreshTimestampMs.value
        assertTrue(initialTimestamp > 0L)

        // Simulating a refresh timestamp update
        repo.recordRefreshTimestamp()
        assertTrue(repo.lastRefreshTimestampMs.value >= initialTimestamp)
    }

    @Test
    fun testAutoRefreshIntervalFromSeconds() {
        assertEquals(AutoRefreshInterval.OFF, AutoRefreshInterval.fromSeconds(0))
        assertEquals(AutoRefreshInterval.SECONDS_30, AutoRefreshInterval.fromSeconds(30))
        assertEquals(AutoRefreshInterval.MINUTE_1, AutoRefreshInterval.fromSeconds(60))
        assertEquals(AutoRefreshInterval.MINUTES_2, AutoRefreshInterval.fromSeconds(120))
        assertEquals(AutoRefreshInterval.MINUTES_5, AutoRefreshInterval.fromSeconds(300))
        // Unknown defaults to MINUTE_1
        assertEquals(AutoRefreshInterval.MINUTE_1, AutoRefreshInterval.fromSeconds(999))
    }

    @Test
    fun testMaskPublisherIdDashesSecondHalf() {
        // Standard AdMob pub ID with 16 digits
        assertEquals("pub-12345678--------", maskPublisherId("pub-1234567890123456"))
        
        // With ca-app-pub- prefix
        assertEquals("ca-app-pub-12345678--------", maskPublisherId("ca-app-pub-1234567890123456"))

        // With accounts/pub- prefix
        assertEquals("accounts/pub-12345678--------", maskPublisherId("accounts/pub-1234567890123456"))

        // Connecting / blank states
        assertEquals("Connecting...", maskPublisherId("Connecting..."))
        assertEquals("", maskPublisherId(""))
        assertEquals("", maskPublisherId("   "))

        // Odd length digits (e.g. 5 digits: keep 3, dash 2)
        assertEquals("pub-123--", maskPublisherId("pub-12345"))
    }

    @Test
    fun testOfflineCacheRestorationAndDateSwitching() {
        val settings = MapSettings()
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        val cachedSummary = AccountSummary(
            todayRevenue = 150.0,
            yesterdayRevenue = 120.0,
            deltaAmount = 30.0,
            deltaPercentage = 25.0,
            dailyGoal = 200.0,
            dailyGoalProgress = 0.75f,
            ecpm = 4.5,
            impressions = 3000,
            fillRate = 98.0,
            lastSyncTime = "Live • AdMob API (pub-12345678--------)"
        )
        val cachedApp = AppMonetization(
            id = "app-cached-1",
            name = "Offline Solitaire",
            category = "Game",
            platform = "Android",
            todayRevenue = 150.0,
            deltaVsYesterday = 30.0,
            totalImpressions = 3000,
            fillRate = 98.0,
            avgEcpm = 4.5
        )
        val today = kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val yesterday = today.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        val todayStr = AdMobApiClient.formatDateKey(today)
        val yesterdayStr = AdMobApiClient.formatDateKey(yesterday)

        val cachedRows = listOf(
            AdMobReportRow(
                date = todayStr,
                appId = "app-cached-1",
                appName = "Offline Solitaire",
                format = "BANNER",
                country = "US",
                earnings = 150.0,
                impressions = 3000,
                clicks = 120
            ),
            AdMobReportRow(
                date = yesterdayStr,
                appId = "app-cached-1",
                appName = "Offline Solitaire",
                format = "BANNER",
                country = "US",
                earnings = 120.0,
                impressions = 2500,
                clicks = 90
            )
        )

        settings.putBoolean("solarad_is_logged_in", true)
        settings.putString("solarad_user_name", "Offline User")
        settings.putString("solarad_user_email", "offline@user.com")
        settings.putString("solarad_publisher_id", "pub-1234567890123456")
        settings.putString("solarad_cache_summary", json.encodeToString(cachedSummary))
        settings.putString("solarad_cache_all_apps", json.encodeToString(listOf(cachedApp)))
        settings.putString("solarad_cache_report_rows", json.encodeToString(cachedRows))
        settings.putString("solarad_cache_registered_apps", json.encodeToString(listOf(cachedApp)))

        // Instantiate repository without active network token, restoring entirely from offline cache
        val repo = AdMobRepository(settings = settings, cacheFiles = null)

        assertTrue(repo.session.value.isLoggedIn)
        assertEquals("Offline User", repo.session.value.userName)
        assertTrue(repo.hasOfflineCache)

        // Verifying cached summary restored
        assertEquals(150.0, repo.accountSummary.value.todayRevenue)
        assertEquals(1, repo.allApps.value.size)
        assertEquals("Offline Solitaire", repo.allApps.value[0].name)

        // Switching date range offline computes dynamically from cached report rows
        repo.setTimeRange(TimeRange.YESTERDAY)
        assertEquals(120.0, repo.accountSummary.value.todayRevenue)
        assertEquals(2500, repo.accountSummary.value.impressions)

        // Logout cleanly clears offline cache
        repo.logout()
        assertFalse(repo.session.value.isLoggedIn)
        assertTrue(repo.allApps.value.isEmpty())
        assertNull(settings.getStringOrNull("solarad_cache_summary"))
        assertNull(settings.getStringOrNull("solarad_cache_report_rows"))
    }

    @Test
    fun testCustomDailyGoalPersistenceAndRecomputation() {
        val settings = MapSettings()
        val repo1 = AdMobRepository(settings = settings, cacheFiles = null)

        // Initially no custom goal set
        assertNull(repo1.customDailyGoal.value)
        assertFalse(repo1.accountSummary.value.isCustomGoal)

        // Set custom goal
        repo1.setCustomDailyGoal(250.0)
        assertEquals(250.0, repo1.customDailyGoal.value)
        assertEquals(250.0, repo1.accountSummary.value.dailyGoal)
        assertTrue(repo1.accountSummary.value.isCustomGoal)

        // New repo instance with same settings restores the custom goal
        val repo2 = AdMobRepository(settings = settings, cacheFiles = null)
        assertEquals(250.0, repo2.customDailyGoal.value)
        assertEquals(250.0, repo2.accountSummary.value.dailyGoal)
        assertTrue(repo2.accountSummary.value.isCustomGoal)

        // Reset to auto goal
        repo2.setCustomDailyGoal(null)
        assertNull(repo2.customDailyGoal.value)
        assertFalse(repo2.accountSummary.value.isCustomGoal)
    }

    @Test
    fun testCustomDailyGoalScalesAcrossRanges() {
        val testDate = LocalDate(2026, 11, 19)
        val rows = listOf(
            AdMobReportRow("20261119", "app1", "App", "Banner", "US", 50.0, 5000, 100),
            AdMobReportRow("20261118", "app1", "App", "Banner", "US", 40.0, 4000, 80),
            AdMobReportRow("20261115", "app1", "App", "Banner", "US", 60.0, 6000, 120)
        )

        val customDaily = 100.0

        // Today -> 1x daily goal
        val todaySummary = AdMobApiClient.computeAccountSummary(
            rows = rows,
            range = TimeRange.TODAY,
            publisherId = "pub-test",
            today = testDate,
            customDailyGoal = customDaily
        )
        assertEquals(100.0, todaySummary.dailyGoal)
        assertEquals(100.0, todaySummary.periodGoal)
        assertEquals(1, todaySummary.periodDays)
        assertEquals(0.50f, todaySummary.dailyGoalProgress)
        assertTrue(todaySummary.isCustomGoal)

        // Last 7 Days -> base daily goal is 100, periodGoal is 700
        val summary7d = AdMobApiClient.computeAccountSummary(
            rows = rows,
            range = TimeRange.LAST_7_DAYS,
            publisherId = "pub-test",
            today = testDate,
            customDailyGoal = customDaily
        )
        assertEquals(100.0, summary7d.dailyGoal)
        assertEquals(700.0, summary7d.periodGoal)
        assertEquals(7, summary7d.periodDays)
        // 50 + 40 + 60 = 150.0 -> 150.0 / 700.0 ~ 0.214f
        assertTrue(summary7d.dailyGoalProgress > 0.21f && summary7d.dailyGoalProgress < 0.22f)
        assertTrue(summary7d.isCustomGoal)

        // Past 30 Days -> base daily goal is 100, periodGoal is 3000
        val summary30d = AdMobApiClient.computeAccountSummary(
            rows = rows,
            range = TimeRange.DAYS_30,
            publisherId = "pub-test",
            today = testDate,
            customDailyGoal = customDaily
        )
        assertEquals(100.0, summary30d.dailyGoal)
        assertEquals(3000.0, summary30d.periodGoal)
        assertEquals(30, summary30d.periodDays)
        assertTrue(summary30d.isCustomGoal)

        // Custom Date Range: 10 days -> 10x daily goal
        val start = LocalDate(2026, 11, 10)
        val end = LocalDate(2026, 11, 19) // 10 days inclusive
        val summaryCustom = AdMobApiClient.computeAccountSummary(
            rows = rows,
            range = TimeRange.CUSTOM,
            publisherId = "pub-test",
            today = testDate,
            customStartDate = start,
            customEndDate = end,
            customDailyGoal = customDaily
        )
        assertEquals(100.0, summaryCustom.dailyGoal)
        assertEquals(1000.0, summaryCustom.periodGoal)
        assertEquals(10, summaryCustom.periodDays)
        assertTrue(summaryCustom.isCustomGoal)
    }

    @Test
    fun testGoalProgressUncappedWhenExceedingTarget() {
        val testDate = LocalDate(2026, 11, 19)
        val rows = listOf(
            AdMobReportRow("20261119", "app1", "App", "Banner", "US", 150.0, 15000, 300)
        )
        val summary = AdMobApiClient.computeAccountSummary(
            rows = rows,
            range = TimeRange.TODAY,
            publisherId = "pub-test",
            today = testDate,
            customDailyGoal = 100.0
        )
        assertEquals(100.0, summary.dailyGoal)
        assertEquals(100.0, summary.periodGoal)
        // 150 / 100 = 1.5f (150% achievement, un-clamped)
        assertEquals(1.50f, summary.dailyGoalProgress)
    }

    @Test
    fun testRepositoryFallbackScalesAcrossTimeRangesWhenOfflineOrEmpty() {
        val settings = MapSettings()
        val repo = AdMobRepository(settings = settings, cacheFiles = null)
        repo.setCustomDailyGoal(100.0)

        // Today default
        assertEquals(100.0, repo.accountSummary.value.dailyGoal)
        assertEquals(100.0, repo.accountSummary.value.periodGoal)
        assertEquals(1, repo.accountSummary.value.periodDays)

        // Switch to Last 7 Days
        repo.setTimeRange(TimeRange.LAST_7_DAYS)
        assertEquals(100.0, repo.accountSummary.value.dailyGoal)
        assertEquals(700.0, repo.accountSummary.value.periodGoal)
        assertEquals(7, repo.accountSummary.value.periodDays)

        // Switch to 30 Days
        repo.setTimeRange(TimeRange.DAYS_30)
        assertEquals(100.0, repo.accountSummary.value.dailyGoal)
        assertEquals(3000.0, repo.accountSummary.value.periodGoal)
        assertEquals(30, repo.accountSummary.value.periodDays)

        // Switch to Custom Range 5 days
        val start = LocalDate(2026, 11, 1)
        val end = LocalDate(2026, 11, 5)
        repo.setTimeRange(TimeRange.CUSTOM, start, end)
        assertEquals(100.0, repo.accountSummary.value.dailyGoal)
        assertEquals(500.0, repo.accountSummary.value.periodGoal)
        assertEquals(5, repo.accountSummary.value.periodDays)
    }

    @Test
    fun testAutoGoalUsesCompletedHistoricalDays() {
        val testDate = LocalDate(2026, 11, 20)
        // 7 completed days: 20261113..20261119 each with 100.0 -> avg = 100.0, computed goal = 100 * 1.25 rounded = 150.0
        // today 20261120 only has partial 5.0 revenue (morning)
        val rows = listOf(
            AdMobReportRow("20261120", "app1", "App", "Banner", "US", 5.0, 500, 10),
            AdMobReportRow("20261119", "app1", "App", "Banner", "US", 100.0, 10000, 200),
            AdMobReportRow("20261118", "app1", "App", "Banner", "US", 100.0, 10000, 200),
            AdMobReportRow("20261117", "app1", "App", "Banner", "US", 100.0, 10000, 200),
            AdMobReportRow("20261116", "app1", "App", "Banner", "US", 100.0, 10000, 200),
            AdMobReportRow("20261115", "app1", "App", "Banner", "US", 100.0, 10000, 200),
            AdMobReportRow("20261114", "app1", "App", "Banner", "US", 100.0, 10000, 200),
            AdMobReportRow("20261113", "app1", "App", "Banner", "US", 100.0, 10000, 200)
        )
        val summary = AdMobApiClient.computeAccountSummary(
            rows = rows,
            range = TimeRange.TODAY,
            publisherId = "pub-test",
            today = testDate
        )
        // Auto goal = 7 completed days / 7 = 700 / 7 = 100.0
        assertEquals(100.0, summary.dailyGoal)
        assertEquals(100.0, summary.periodGoal)
        assertFalse(summary.isCustomGoal)
    }

    @Test
    fun testAutoGoalIsWeeklyEarningsOverSevenRoundedUp() {
        val rows = listOf(AdMobReportRow("20261118", "app1", "App", "Banner", "US", 10.0, 1000, 10))
        val summary = AdMobApiClient.computeAccountSummary(rows, TimeRange.TODAY, "pub-test", today = LocalDate(2026, 11, 20))
        assertEquals(1.5, summary.dailyGoal) // 10 / 7 = 1.43 -> 1.5
        assertEquals(1.4, AdMobApiClient.ceilToTenth(1.4))
    }

    @Test
    fun testParseLifetimeEarningsAndComputeWallet() {
        val jsonArray = """
        [
          { "header": {} },
          {
            "row": {
              "metricValues": {
                "ESTIMATED_EARNINGS": { "microsValue": "42296624" }
              }
            }
          },
          { "footer": { "matchingRowCount": "1" } }
        ]
        """.trimIndent()

        val lifetime = AdMobApiClient.parseLifetimeEarnings(jsonArray)
        assertEquals(42.30, lifetime)

        val wallet = AdMobApiClient.computeWallet(rows = emptyList(), lifetimeEarnings = lifetime)
        assertEquals(42.30, wallet.unpaidBalance)
        assertEquals(0.423f, wallet.thresholdPercent)
        assertTrue(wallet.isBankVerified)
    }

    @Test
    fun testRefreshPreservesSelectedTimeRange() {
        val settings = MapSettings()
        val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

        val todayDate = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayStr = AdMobApiClient.formatDateKey(todayDate)
        val yestStr = AdMobApiClient.formatDateKey(todayDate.minus(1, DateTimeUnit.DAY))
        val day2Str = AdMobApiClient.formatDateKey(todayDate.minus(2, DateTimeUnit.DAY))

        val rows = listOf(
            AdMobReportRow(todayStr, "app1", "Game", "Banner", "US", 50.0, 1000, 20),
            AdMobReportRow(yestStr, "app1", "Game", "Banner", "US", 70.0, 1400, 30),
            AdMobReportRow(day2Str, "app1", "Game", "Banner", "US", 80.0, 1600, 40)
        )
        settings.putBoolean("solarad_is_logged_in", true)
        settings.putString("solarad_user_name", "Test User")
        settings.putString("solarad_publisher_id", "pub-test")
        settings.putString("solarad_cache_report_rows", json.encodeToString(rows))

        val repo = AdMobRepository(settings = settings, cacheFiles = null)
        // Select Last 7 Days
        repo.setTimeRange(TimeRange.LAST_7_DAYS)
        // 50.0 + 70.0 + 80.0 = 200.0
        assertEquals(200.0, repo.accountSummary.value.todayRevenue)
        assertEquals(TimeRange.LAST_7_DAYS, repo.selectedTimeRange.value)

        // Update custom goal which triggers recomputeSummary
        repo.setCustomDailyGoal(500.0)

        // Crucial verification: selected range is still LAST_7_DAYS and revenue is still 200.0, NOT 50.0!
        assertEquals(TimeRange.LAST_7_DAYS, repo.selectedTimeRange.value)
        assertEquals(200.0, repo.accountSummary.value.todayRevenue)
    }

    @Test
    fun testReportPreferencesPersistenceAndLogoutReset() {
        val settings = MapSettings()
        val repo1 = AdMobRepository(settings = settings, cacheFiles = null)
        val saved = com.lavish.admob.data.ReportPreferences(
            rangeName = "LAST_30_DAYS",
            dimensionNames = "APP,COUNTRY",
            sortMetricName = "IMPRESSIONS",
            sortDescending = false,
            chartMetricName = "ECPM",
            appFilter = "app-123",
            formatFilter = "BANNER"
        )
        repo1.setReportPreferences(saved)
        assertEquals(saved, repo1.reportPreferences.value)

        // New repo instance with same settings restores the report preferences
        val repo2 = AdMobRepository(settings = settings, cacheFiles = null)
        assertEquals(saved, repo2.reportPreferences.value)

        // Logout clears report preferences back to defaults
        repo2.logout()
        assertEquals(com.lavish.admob.data.ReportPreferences(), repo2.reportPreferences.value)
    }
}
