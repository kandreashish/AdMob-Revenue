package com.solarad.app.data

import com.solarad.app.model.AccountSummary
import com.solarad.app.model.AdFormatMetric
import com.solarad.app.model.AnalyticsRange
import com.solarad.app.model.AnalyticsSummary
import com.solarad.app.model.AppMonetization
import com.solarad.app.model.PastPayout
import com.solarad.app.model.PlatformFilter
import com.solarad.app.model.RevenuePoint
import com.solarad.app.model.TerritoryYield
import com.solarad.app.model.TimeRange
import com.solarad.app.model.UserSession
import com.solarad.app.model.WalletSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdMobRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val _session = MutableStateFlow(
        UserSession(
            isLoggedIn = true,
            userName = "Alex",
            email = "alex.dev@gmail.com",
            publisherId = "pub-9307144625788048"
        )
    )
    val session: StateFlow<UserSession> = _session.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow(TimeRange.TODAY)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    private val _accountSummary = MutableStateFlow(createInitialSummary(TimeRange.TODAY))
    val accountSummary: StateFlow<AccountSummary> = _accountSummary.asStateFlow()

    private val _allApps = MutableStateFlow(createInitialApps())
    val allApps: StateFlow<List<AppMonetization>> = _allApps.asStateFlow()

    private val _selectedAnalyticsRange = MutableStateFlow(AnalyticsRange.DAYS_7)
    val selectedAnalyticsRange: StateFlow<AnalyticsRange> = _selectedAnalyticsRange.asStateFlow()

    private val _analytics = MutableStateFlow(createAnalytics(AnalyticsRange.DAYS_7))
    val analytics: StateFlow<AnalyticsSummary> = _analytics.asStateFlow()

    private val _wallet = MutableStateFlow(createInitialWallet())
    val wallet: StateFlow<WalletSummary> = _wallet.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Subtle live ticker simulation: every 15 seconds, small realistic increment to mimic incoming ad bids
        scope.launch {
            while (true) {
                delay(15000)
                if (_session.value.isLoggedIn && _selectedTimeRange.value == TimeRange.TODAY) {
                    val current = _accountSummary.value
                    val increment = 0.35 + (0..10).random() * 0.05
                    val newRev = current.todayRevenue + increment
                    _accountSummary.value = current.copy(
                        todayRevenue = ((newRev * 100).toLong() / 100.0),
                        deltaAmount = current.deltaAmount + increment,
                        impressions = current.impressions + (5..15).random()
                    )
                }
            }
        }
    }

    fun loginWithGoogle() {
        _isRefreshing.value = true
        scope.launch {
            delay(800)
            _session.value = UserSession(
                isLoggedIn = true,
                userName = "Alex",
                email = "alex.dev@gmail.com",
                publisherId = "pub-9307144625788048"
            )
            _isRefreshing.value = false
        }
    }

    fun loginWithApiToken(token: String) {
        _isRefreshing.value = true
        scope.launch {
            delay(800)
            _session.value = UserSession(
                isLoggedIn = true,
                userName = "API Studio",
                email = "token-auth@admob.internal",
                publisherId = "pub-custom-api"
            )
            _isRefreshing.value = false
        }
    }

    fun logout() {
        _session.value = UserSession(
            isLoggedIn = false,
            userName = "",
            email = "",
            publisherId = ""
        )
    }

    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        _accountSummary.value = createInitialSummary(range)
    }

    fun setAnalyticsRange(range: AnalyticsRange) {
        _selectedAnalyticsRange.value = range
        _analytics.value = createAnalytics(range)
    }

    fun refresh() {
        scope.launch {
            _isRefreshing.value = true
            delay(600)
            _accountSummary.value = createInitialSummary(_selectedTimeRange.value)
            _allApps.value = createInitialApps()
            _analytics.value = createAnalytics(_selectedAnalyticsRange.value)
            _wallet.value = createInitialWallet()
            _isRefreshing.value = false
        }
    }

    fun addApp(name: String, platform: String) {
        val newApp = AppMonetization(
            id = "app-${_allApps.value.size + 1}",
            name = name,
            category = "Utilities",
            platform = platform,
            todayRevenue = 120.40,
            deltaVsYesterday = 15.2,
            totalImpressions = 12500,
            fillRate = 97.4,
            avgEcpm = 14.80,
            formats = listOf(
                AdFormatMetric("Rewarded Video", 72.24, 24.50, 2900, 98.2, 60.0),
                AdFormatMetric("Interstitial", 36.12, 16.20, 2200, 97.5, 30.0),
                AdFormatMetric("Banner", 12.04, 3.80, 7400, 96.5, 10.0)
            )
        )
        _allApps.value = _allApps.value + newApp
    }

    private fun createInitialSummary(range: TimeRange): AccountSummary {
        return when (range) {
            TimeRange.TODAY -> AccountSummary(
                todayRevenue = 1842.60,
                yesterdayRevenue = 1480.20,
                deltaAmount = 362.40,
                deltaPercentage = 24.5,
                dailyGoal = 2000.0,
                dailyGoalProgress = 0.92f,
                ecpm = 18.40,
                impressions = 142800,
                fillRate = 99.4,
                lastSyncTime = "Live • 2:45 PM"
            )
            TimeRange.YESTERDAY -> AccountSummary(
                todayRevenue = 1480.20,
                yesterdayRevenue = 1320.00,
                deltaAmount = 160.20,
                deltaPercentage = 12.1,
                dailyGoal = 2000.0,
                dailyGoalProgress = 0.74f,
                ecpm = 16.90,
                impressions = 128400,
                fillRate = 99.1,
                lastSyncTime = "Finalized"
            )
            TimeRange.THIS_MONTH -> AccountSummary(
                todayRevenue = 34820.50,
                yesterdayRevenue = 29400.00,
                deltaAmount = 5420.50,
                deltaPercentage = 18.4,
                dailyGoal = 45000.0,
                dailyGoalProgress = 0.77f,
                ecpm = 17.85,
                impressions = 2480000,
                fillRate = 99.5,
                lastSyncTime = "Month to Date"
            )
        }
    }

    private fun createInitialApps(): List<AppMonetization> {
        return listOf(
            AppMonetization(
                id = "app_1",
                name = "CyberDrift",
                category = "Game • Racing",
                platform = "iOS",
                todayRevenue = 890.40,
                deltaVsYesterday = 120.00,
                totalImpressions = 28218,
                fillRate = 99.8,
                avgEcpm = 31.57,
                formats = listOf(
                    AdFormatMetric("Rewarded Video", 520.00, 38.40, 13540, 99.8, 58.4),
                    AdFormatMetric("Interstitial", 280.00, 22.10, 12660, 99.2, 31.4),
                    AdFormatMetric("App Open Ad", 90.40, 44.80, 2018, 97.4, 10.2)
                )
            ),
            AppMonetization(
                id = "app_2",
                name = "FocusAI",
                category = "Productivity • AI",
                platform = "iOS",
                todayRevenue = 612.00,
                deltaVsYesterday = 85.00,
                totalImpressions = 38400,
                fillRate = 99.5,
                avgEcpm = 15.93,
                formats = listOf(
                    AdFormatMetric("Rewarded Video", 310.00, 28.50, 10880, 99.6, 50.7),
                    AdFormatMetric("Native", 210.00, 14.20, 14790, 99.5, 34.3),
                    AdFormatMetric("Banner", 92.00, 7.20, 12730, 99.4, 15.0)
                )
            ),
            AppMonetization(
                id = "app_3",
                name = "ZenBlocks",
                category = "Casual • Puzzle",
                platform = "Android",
                todayRevenue = 340.20,
                deltaVsYesterday = 25.00,
                totalImpressions = 76200,
                fillRate = 99.2,
                avgEcpm = 4.46,
                formats = listOf(
                    AdFormatMetric("Interstitial", 210.00, 8.40, 25000, 99.3, 61.7),
                    AdFormatMetric("Banner", 130.20, 2.54, 51200, 99.1, 38.3)
                )
            )
        )
    }

    private fun createAnalytics(range: AnalyticsRange): AnalyticsSummary {
        return when (range) {
            AnalyticsRange.DAYS_7 -> AnalyticsSummary(
                periodLabel = "7-Day Revenue",
                totalRevenue = 11480.20,
                deltaPercent = 18.4,
                avgEcpm = 18.40,
                totalImpressions = 642500,
                trendPoints = listOf(
                    RevenuePoint("Mon", 1420.00),
                    RevenuePoint("Tue", 1510.00),
                    RevenuePoint("Wed", 1490.00),
                    RevenuePoint("Thu", 1680.00),
                    RevenuePoint("Fri", 1720.00),
                    RevenuePoint("Sat", 1817.60),
                    RevenuePoint("Sun", 1842.60, isCurrentOrPeak = true)
                ),
                territories = listOf(
                    TerritoryYield("🇺🇸", "United States", 6210.00, 54.1),
                    TerritoryYield("🇯🇵", "Japan", 2520.00, 22.0),
                    TerritoryYield("🇩🇪", "Germany", 1490.00, 13.0),
                    TerritoryYield("🇬🇧", "United Kingdom", 810.00, 7.0),
                    TerritoryYield("🌐", "Rest of World", 450.20, 3.9)
                )
            )
            AnalyticsRange.DAYS_30 -> AnalyticsSummary(
                periodLabel = "30-Day Revenue",
                totalRevenue = 48210.00,
                deltaPercent = 22.1,
                avgEcpm = 17.90,
                totalImpressions = 2690000,
                trendPoints = listOf(
                    RevenuePoint("W1", 10200.00),
                    RevenuePoint("W2", 11800.00),
                    RevenuePoint("W3", 12400.00),
                    RevenuePoint("W4", 13810.00, isCurrentOrPeak = true)
                ),
                territories = listOf(
                    TerritoryYield("🇺🇸", "United States", 26500.00, 55.0),
                    TerritoryYield("🇯🇵", "Japan", 10600.00, 22.0),
                    TerritoryYield("🇩🇪", "Germany", 6200.00, 12.9),
                    TerritoryYield("🇬🇧", "United Kingdom", 3400.00, 7.1),
                    TerritoryYield("🌐", "Rest of World", 1510.00, 3.0)
                )
            )
            AnalyticsRange.DAYS_90 -> AnalyticsSummary(
                periodLabel = "90-Day Revenue",
                totalRevenue = 139400.00,
                deltaPercent = 31.8,
                avgEcpm = 17.50,
                totalImpressions = 7965000,
                trendPoints = listOf(
                    RevenuePoint("Sep", 41200.00),
                    RevenuePoint("Oct", 48900.00),
                    RevenuePoint("Nov", 49300.00, isCurrentOrPeak = true)
                ),
                territories = listOf(
                    TerritoryYield("🇺🇸", "United States", 76600.00, 54.9),
                    TerritoryYield("🇯🇵", "Japan", 30600.00, 21.9),
                    TerritoryYield("🇩🇪", "Germany", 18100.00, 13.0),
                    TerritoryYield("🇬🇧", "United Kingdom", 9800.00, 7.0),
                    TerritoryYield("🌐", "Rest of World", 4300.00, 3.2)
                )
            )
        }
    }

    private fun createInitialWallet(): WalletSummary {
        return WalletSummary(
            unpaidBalance = 4890.20,
            thresholdAmount = 100.00,
            thresholdPercent = 1.0f,
            nextDisbursementDate = "Nov 21",
            daysRemaining = 2,
            bankAccountTitle = "Primary Checking Account",
            bankAccountMasked = "•••• 8912",
            isBankVerified = true,
            adServingStatus = "100% Normal / Uncapped",
            policyIssuesCount = 0,
            pastPayouts = listOf(
                PastPayout("pay_1", "Oct 2026", 4120.80, "Deposited Oct 21", "Paid"),
                PastPayout("pay_2", "Sep 2026", 3890.40, "Deposited Sep 21", "Paid"),
                PastPayout("pay_3", "Aug 2026", 3612.00, "Deposited Aug 21", "Paid")
            )
        )
    }
}
