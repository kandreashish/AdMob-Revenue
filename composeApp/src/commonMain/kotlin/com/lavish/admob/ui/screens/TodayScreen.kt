package com.lavish.admob.ui.screens

import androidx.compose.animation.AnimatedContent
import com.lavish.admob.platform.PlatformSlots
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.lavish.admob.ui.components.uniqueKeys
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import com.lavish.admob.model.AccountSummary
import com.lavish.admob.model.APP_VERSION
import com.lavish.admob.model.AppMonetization
import com.lavish.admob.model.TimeRange
import com.lavish.admob.theme.BorderSubtle
import com.lavish.admob.theme.BotanicalGreen
import com.lavish.admob.theme.BotanicalGreenLight
import com.lavish.admob.theme.CoralCrimson
import com.lavish.admob.theme.CoralCrimsonLight
import com.lavish.admob.theme.EspressoSlate
import com.lavish.admob.theme.LinenSand
import com.lavish.admob.theme.MutedSlate
import com.lavish.admob.theme.OceanAzure
import com.lavish.admob.theme.OceanAzureLight
import com.lavish.admob.theme.PureWhite
import com.lavish.admob.theme.SolarAmber
import com.lavish.admob.theme.SolarAmberGlow
import com.lavish.admob.theme.SolarAmberLight
import com.lavish.admob.theme.SolarTheme
import com.lavish.admob.ui.components.AppIcon
import com.lavish.admob.ui.components.MetricTwinCard
import com.lavish.admob.ui.components.SetGoalDialog
import com.lavish.admob.ui.components.SolarHeader
import com.lavish.admob.ui.components.SolarPillTabs
import com.lavish.admob.ui.components.SolarTimeHorizonSelector
import com.lavish.admob.ui.utils.formatAmount
import com.lavish.admob.ui.utils.formatCompactNumber

@Composable
fun TodayScreen(
    summary: AccountSummary,
    apps: List<AppMonetization>,
    selectedRange: TimeRange,
    accountName: String = "My AdMob",
    avatarUrl: String? = null,
    isRefreshing: Boolean = false,
    isOffline: Boolean = false,
    onSelectRange: (TimeRange) -> Unit,
    onViewAllApps: () -> Unit,
    onSelectApp: (AppMonetization) -> Unit,
    onAccountClick: () -> Unit,
    onSelectCustomRange: ((LocalDate, LocalDate) -> Unit)? = null,
    onSetDailyGoal: ((Double?) -> Unit)? = null,
    onOpenFullScreen: (() -> Unit)? = null
) {
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var showSetGoalDialog by remember { mutableStateOf(false) }

    val currentDate by rememberCurrentDate()
    val dynamicTodayTitle = remember(currentDate) {
        val today = currentDate
        val monthStr = when (today.month) {
            Month.JANUARY -> "Jan"
            Month.FEBRUARY -> "Feb"
            Month.MARCH -> "Mar"
            Month.APRIL -> "Apr"
            Month.MAY -> "May"
            Month.JUNE -> "Jun"
            Month.JULY -> "Jul"
            Month.AUGUST -> "Aug"
            Month.SEPTEMBER -> "Sep"
            Month.OCTOBER -> "Oct"
            Month.NOVEMBER -> "Nov"
            Month.DECEMBER -> "Dec"
        }
        "Today, $monthStr ${today.day}"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarTheme.colors.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Top Header
        item {
            SolarHeader(
                title = when (selectedRange) {
                    TimeRange.TODAY -> dynamicTodayTitle
                    TimeRange.YESTERDAY -> "Yesterday's Performance"
                    TimeRange.LAST_7_DAYS -> "Past 7 Days"
                    TimeRange.THIS_MONTH -> "This Month"
                    TimeRange.DAYS_30 -> "Past 30 Days"
                    TimeRange.CUSTOM -> "Custom Range"
                },
                subtitle = when {
                    isRefreshing -> "Syncing..."
                    isOffline -> "Offline • Cached Data"
                    summary.lastSyncTime.isBlank() || summary.lastSyncTime.equals("Awaiting Sync", ignoreCase = true) -> "Syncing..."
                    else -> summary.lastSyncTime
                },
                accountName = accountName,
                avatarUrl = avatarUrl,
                isOffline = isOffline,
                version = APP_VERSION,
                onAccountClick = onAccountClick
            )
        }

        // Time Range Horizon Ribbon
        item {
            Spacer(modifier = Modifier.height(2.dp))
            SolarTimeHorizonSelector(
                selectedRange = selectedRange,
                onSelectRange = onSelectRange,
                onCustomClick = { showCustomDateDialog = true },
                customDateLabel = if (selectedRange == TimeRange.CUSTOM) summary.lastSyncTime else null
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Platform-specific banner (Android: Earnings Bubble)
        PlatformSlots.todayBanner?.let { banner -> item { banner() } }

        // Hero Sunlit Revenue Card
        item {
            AnimatedContent(
                targetState = summary,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { targetSummary ->
                val heroGlow = if (SolarTheme.colors.isDark) {
                    SolarAmber.copy(alpha = 0.12f)
                } else {
                    SolarAmberGlow.copy(alpha = 0.45f)
                }
                // AdMob "Total estimated earnings" card: dark grey with white text
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = com.lavish.admob.theme.AdMobHeroGrey,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                                                        .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = Color(0xFFE8EAED),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (selectedRange) {
                                        TimeRange.TODAY -> "Today's Earnings"
                                        TimeRange.YESTERDAY -> "Yesterday's Final"
                                        TimeRange.LAST_7_DAYS -> "Last 7 Days Revenue"
                                        TimeRange.THIS_MONTH -> "This Month Revenue"
                                        TimeRange.DAYS_30 -> "Last 30 Days Revenue"
                                        TimeRange.CUSTOM -> "Custom Range Revenue"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE8EAED),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Delta Badge
                                val isPositive = targetSummary.deltaAmount >= 0.0
                                val deltaBadgeColor = if (isPositive) Color(0xFF81C995) else Color(0xFFF28B82)
                                val deltaBadgeBg = Color.White.copy(alpha = 0.12f)

                                Surface(
                                    color = deltaBadgeBg,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = deltaBadgeColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${if (isPositive) "+" else "-"}${targetSummary.currencySymbol}${formatAmount(abs(targetSummary.deltaAmount))} vs prev",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = deltaBadgeColor,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Full Screen Distraction-Free Button
                                if (onOpenFullScreen != null) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.12f),
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { onOpenFullScreen() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.OpenInFull,
                                                contentDescription = "Full Screen Landscape Mode",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large Confident Currency Figure
                        Text(
                            text = "${targetSummary.currencySymbol}${formatAmount(targetSummary.todayRevenue)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            letterSpacing = (-0.02).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Daily Goal Progress Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isGoalAchieved = targetSummary.dailyGoalProgress >= 1f
                                Text(
                                    text = if (isGoalAchieved) {
                                        "🎯 ${(targetSummary.dailyGoalProgress * 100).toInt()}% goal achieved!"
                                    } else {
                                        "${(targetSummary.dailyGoalProgress * 100).toInt()}% of goal reached"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isGoalAchieved) Color(0xFF81C995) else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (targetSummary.isCustomGoal) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.clickable(enabled = onSetDailyGoal != null) {
                                        showSetGoalDialog = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Goal",
                                            tint = if (targetSummary.isCustomGoal) Color.White else Color(0xFFE8EAED),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        val formattedGoal = if (targetSummary.periodGoal % 1.0 == 0.0) {
                                            targetSummary.periodGoal.toInt().toString()
                                        } else {
                                            formatAmount(targetSummary.periodGoal)
                                        }
                                        Text(
                                            text = "Goal: ${targetSummary.currencySymbol}$formattedGoal${if (targetSummary.isCustomGoal) "" else " (Auto)"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (targetSummary.isCustomGoal) Color.White else Color(0xFFE8EAED),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            // Same tiers as the earnings bubble (light tints for contrast on the blue card)
                            val p = targetSummary.dailyGoalProgress
                            val progressColor = when {
                                p >= 1f -> Color(0xFF81C995)     // goal hit: green
                                p >= 0.66f -> Color(0xFF8AB4F8)  // close: blue
                                p >= 0.33f -> Color(0xFFFDD663)  // halfway: amber
                                else -> Color(0xFFF28B82)        // behind: red
                            }
                            LinearProgressIndicator(
                                progress = { targetSummary.dailyGoalProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = progressColor,
                                trackColor = Color.White.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Focus Metric Twin Cards: Row 1 (eCPM, Impressions) & Row 2 (CTR, Clicks)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTwinCard(
                        label = "Average eCPM",
                        value = "${summary.currencySymbol}${formatAmount(summary.ecpm)}",
                        subBadge = if (summary.ecpm > 0) "Live yield" else "No impressions",
                        isPositive = summary.ecpm > 0,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )

                    MetricTwinCard(
                        label = "Impressions",
                        value = formatCompactNumber(summary.impressions),
                        subBadge = if (summary.impressions > 0) "${summary.fillRate}% fill" else "0 fill",
                        isPositive = summary.impressions > 0,
                        icon = Icons.Default.RemoveRedEye,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTwinCard(
                        label = "CTR (Click Rate)",
                        value = "${summary.ctr}%",
                        subBadge = if (summary.ctr > 0) "${formatCompactNumber(summary.clicks)} clicks" else "0 clicks",
                        isPositive = summary.ctr > 0,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )

                    MetricTwinCard(
                        label = "Total Clicks",
                        value = formatCompactNumber(summary.clicks),
                        subBadge = if (summary.clicks > 0) "User actions" else "No clicks",
                        isPositive = summary.clicks > 0,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Top Performing Apps Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Performing Apps",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarTheme.colors.textPrimary
                )
                if (apps.isNotEmpty()) {
                    Text(
                        text = "View All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanAzure,
                        modifier = Modifier.clickable { onViewAllApps() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (apps.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = SolarTheme.colors.cardBackground,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SolarTheme.colors.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No apps connected yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Applications registered in your Google AdMob account will appear here.",
                            fontSize = 11.sp,
                            color = SolarTheme.colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val topApps = apps.take(4)
            val topKeys = uniqueKeys(topApps) { "today_" + it.id }
            itemsIndexed(topApps, key = { i, _ -> topKeys[i] }) { _, app ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(18.dp))
                        .clickable { onSelectApp(app) },
                    color = SolarTheme.colors.cardBackground
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            // Real Store Icon with graceful Monogram fallback
                            AppIcon(app = app, size = 38.dp)

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = app.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${app.platform} • ${app.formats.firstOrNull()?.formatName ?: "AdMob Network"}",
                                    fontSize = 11.sp,
                                    color = SolarTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${summary.currencySymbol}${formatAmount(app.todayRevenue)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val isAppPositive = app.deltaVsYesterday >= 0.0
                            Surface(
                                color = if (isAppPositive) SolarTheme.colors.successContainer else SolarTheme.colors.errorContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "${if (isAppPositive) "+" else "-"}${summary.currencySymbol}${formatAmount(abs(app.deltaVsYesterday))}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAppPositive) BotanicalGreen else CoralCrimson,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDateDialog) {
        val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
        var startInput by remember { mutableStateOf(now.minus(29, DateTimeUnit.DAY).toString()) }
        var endInput by remember { mutableStateOf(now.toString()) }
        var inputError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val sDate = LocalDate.parse(startInput.trim())
                            val eDate = LocalDate.parse(endInput.trim())
                            val earliest = now.minus(90, DateTimeUnit.DAY)
                            if (sDate > eDate) {
                                inputError = "Start date must be before end date"
                            } else if (eDate > now) {
                                inputError = "End date can't be in the future"
                            } else if (sDate < earliest) {
                                inputError = "Only the last 90 days are synced (from $earliest)"
                            } else {
                                showCustomDateDialog = false
                                onSelectCustomRange?.invoke(sDate, eDate)
                            }
                        } catch (_: Exception) {
                            inputError = "Invalid date format. Use YYYY-MM-DD"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber)
                ) {
                    Text("Apply Filter", fontWeight = FontWeight.Bold, color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text("Cancel", color = SolarTheme.colors.textSecondary)
                }
            },
            title = {
                Text("Select Date Range", fontWeight = FontWeight.Bold, color = SolarTheme.colors.textPrimary)
            },
            containerColor = SolarTheme.colors.cardBackground,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SolarTheme.colors.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val monthStart = LocalDate(now.year, now.month, 1)
                        val lastMonthEnd = monthStart.minus(1, DateTimeUnit.DAY)
                        val lastMonthStart = LocalDate(lastMonthEnd.year, lastMonthEnd.month, 1)
                        listOf(
                            "This Month" to (monthStart to now),
                            "Last Month" to (lastMonthStart to lastMonthEnd),
                            "Last 14 Days" to (now.minus(13, DateTimeUnit.DAY) to now),
                            "Last 60 Days" to (now.minus(59, DateTimeUnit.DAY) to now),
                            "Last 90 Days" to (now.minus(89, DateTimeUnit.DAY) to now)
                        ).forEach { (label, span) ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = SolarTheme.colors.surfaceVariant,
                                modifier = Modifier.clickable {
                                    startInput = span.first.toString()
                                    endInput = span.second.toString()
                                    inputError = null
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SolarTheme.colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = startInput,
                        onValueChange = { startInput = it; inputError = null },
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = endInput,
                        onValueChange = { endInput = it; inputError = null },
                        label = { Text("End Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (inputError != null) {
                        Text(
                            text = inputError ?: "",
                            fontSize = 11.sp,
                            color = CoralCrimson,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        )
    }

    if (showSetGoalDialog) {
        SetGoalDialog(
            currentGoal = summary.dailyGoal,
            currencySymbol = summary.currencySymbol,
            isCustomGoal = summary.isCustomGoal,
            selectedRange = selectedRange,
            periodDays = summary.periodDays,
            onDismiss = { showSetGoalDialog = false },
            onSaveGoal = { newGoal ->
                onSetDailyGoal?.invoke(newGoal)
            },
            onResetToAuto = {
                onSetDailyGoal?.invoke(null)
            }
        )
    }
}
