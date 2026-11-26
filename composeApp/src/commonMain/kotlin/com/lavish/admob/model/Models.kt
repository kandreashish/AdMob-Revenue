package com.lavish.admob.model

import kotlinx.serialization.Serializable

enum class TimeRange(
    val label: String,
    val title: String,
    val description: String
) {
    TODAY("td", "Today", "Live real-time earnings"),
    YESTERDAY("yd", "Yesterday", "Finalized daily earnings"),
    LAST_7_DAYS("7d", "Last 7 Days", "Past 7 days performance"),
    THIS_MONTH("mtd", "This Month", "Month-to-date earnings"),
    DAYS_30("30d", "Last 30 Days", "Past 30 days performance"),
    CUSTOM("custom", "Custom Range", "Custom selected date range");

    val shortLabel: String get() = label
}

enum class AnalyticsRange(val label: String) {
    DAYS_7("Last 7 Days"),
    DAYS_30("30 Days"),
    DAYS_90("90 Days")
}

enum class PlatformFilter(val label: String) {
    ALL("All Platforms"),
    IOS("iOS"),
    ANDROID("Android")
}

enum class AutoRefreshInterval(val seconds: Int, val label: String) {
    OFF(0, "Off"),
    SECONDS_30(30, "30s"),
    MINUTE_1(60, "1 min"),
    MINUTES_2(120, "2 min"),
    MINUTES_5(300, "5 min");

    companion object {
        fun fromSeconds(seconds: Int): AutoRefreshInterval {
            return entries.find { it.seconds == seconds } ?: MINUTE_1
        }
    }
}

@Serializable
data class AdFormatMetric(
    val formatName: String,
    val revenue: Double,
    val ecpm: Double,
    val impressions: Int,
    val fillRate: Double,
    val sharePercent: Double
)

@Serializable
data class AppMonetization(
    val id: String,
    val name: String,
    val category: String,
    val platform: String, // "iOS" or "Android"
    val todayRevenue: Double,
    val deltaVsYesterday: Double,
    val totalImpressions: Int,
    val fillRate: Double,
    val avgEcpm: Double,
    val formats: List<AdFormatMetric> = emptyList(),
    val appStoreId: String? = null,
    val iconUrl: String? = null
)

@Serializable
data class AccountSummary(
    val todayRevenue: Double,
    val yesterdayRevenue: Double,
    val deltaAmount: Double,
    val deltaPercentage: Double,
    val dailyGoal: Double,
    val periodGoal: Double = dailyGoal,
    val periodDays: Int = 1,
    val dailyGoalProgress: Float,
    val ecpm: Double,
    val impressions: Int,
    val fillRate: Double,
    val lastSyncTime: String,
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val ctr: Double = 0.0,
    val clicks: Int = 0,
    val isCustomGoal: Boolean = false
)

@Serializable
data class RevenuePoint(
    val dayLabel: String,
    val amount: Double,
    val isCurrentOrPeak: Boolean = false
)

@Serializable
data class TerritoryYield(
    val flag: String,
    val countryName: String,
    val revenue: Double,
    val sharePercent: Double
)

@Serializable
data class AnalyticsSummary(
    val periodLabel: String,
    val totalRevenue: Double,
    val deltaPercent: Double,
    val avgEcpm: Double,
    val totalImpressions: Int,
    val trendPoints: List<RevenuePoint>,
    val territories: List<TerritoryYield>
)

@Serializable
data class PastPayout(
    val id: String,
    val monthYear: String,
    val amount: Double,
    val depositDate: String,
    val status: String = "Paid"
)

@Serializable
data class WalletSummary(
    val unpaidBalance: Double,
    val thresholdAmount: Double = 100.0,
    val thresholdPercent: Float,
    val nextDisbursementDate: String,
    val daysRemaining: Int,
    val bankAccountTitle: String,
    val bankAccountMasked: String,
    val isBankVerified: Boolean,
    val adServingStatus: String,
    val policyIssuesCount: Int,
    val pastPayouts: List<PastPayout>
)

@Serializable
data class UserSession(
    val isLoggedIn: Boolean,
    val userName: String,
    val email: String,
    val publisherId: String,
    val avatarUrl: String? = null,
    val currencyCode: String = "USD",
    val currencySymbol: String = "$"
)

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromString(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LIGHT
    }
}

fun maskPublisherId(publisherId: String): String {
    val clean = publisherId.trim()
    if (clean.isBlank()) return ""
    if (clean.startsWith("Connecting", ignoreCase = true)) return clean

    val prefix = when {
        clean.startsWith("ca-app-pub-", ignoreCase = true) -> clean.take(11)
        clean.startsWith("accounts/pub-", ignoreCase = true) -> clean.take(13)
        clean.startsWith("pub-", ignoreCase = true) -> clean.take(4)
        else -> ""
    }
    val body = clean.substring(prefix.length)
    if (body.isEmpty()) return clean

    // Dash the second half of the ID
    val keepCount = (body.length + 1) / 2
    val dashCount = body.length - keepCount
    return prefix + body.take(keepCount) + "-".repeat(dashCount)
}

const val APP_VERSION_NAME = "0.1.1"
const val APP_VERSION = "v$APP_VERSION_NAME"



