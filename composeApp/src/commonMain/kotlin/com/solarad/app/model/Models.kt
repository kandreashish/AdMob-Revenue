package com.solarad.app.model

import kotlinx.serialization.Serializable

enum class TimeRange(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_MONTH("This Month")
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
    val formats: List<AdFormatMetric>
)

@Serializable
data class AccountSummary(
    val todayRevenue: Double,
    val yesterdayRevenue: Double,
    val deltaAmount: Double,
    val deltaPercentage: Double,
    val dailyGoal: Double,
    val dailyGoalProgress: Float,
    val ecpm: Double,
    val impressions: Int,
    val fillRate: Double,
    val lastSyncTime: String
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
    val publisherId: String
)
