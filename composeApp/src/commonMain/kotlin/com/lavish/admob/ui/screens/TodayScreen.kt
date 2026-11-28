package com.lavish.admob.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import androidx.compose.material.icons.outlined.Info
import com.lavish.admob.ui.components.IncreaseChip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import com.lavish.admob.theme.AdMobHeroGrey
import com.lavish.admob.theme.AdMobHeroGreyBottom
import com.lavish.admob.theme.AdMobProgressBlue
import com.lavish.admob.theme.AdMobSelectedText
import com.lavish.admob.theme.BotanicalGreenDark
import com.lavish.admob.ui.components.AdMobBrandHeader
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

    // --- Increase tracking: compare each refresh against the previous values for this range ---
    var baseline by remember(selectedRange) { mutableStateOf<AccountSummary?>(null) }
    var previous by remember(selectedRange) { mutableStateOf<AccountSummary?>(null) }
    var infoMetric by remember { mutableStateOf<HomeMetric?>(null) }
    val latestSummary by rememberUpdatedState(summary)
    var lastSyncedAt by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(summary, isRefreshing) {
        if (!isRefreshing) lastSyncedAt = currentTimeLabel()
    }
    // After a range switch, wait for the new range's data to settle before taking a baseline
    LaunchedEffect(selectedRange, isRefreshing) {
        if (!isRefreshing && baseline == null) {
            delay(1000)
            baseline = latestSummary
        }
    }
    LaunchedEffect(summary, isRefreshing) {
        val b = baseline ?: return@LaunchedEffect
        if (!isRefreshing && summary != b) {
            previous = b
            baseline = summary
        }
    }
    val prev = previous

    infoMetric?.let { metric ->
        MetricInfoDialog(
            metric = metric,
            current = summary,
            previous = prev,
            selectedRange = selectedRange,
            lastSyncedAt = lastSyncedAt,
            onDismiss = { infoMetric = null }
        )
    }

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
        // Top Header (AdMob Brand Header)
        item {
            AdMobBrandHeader(
                title = when (selectedRange) {
                    TimeRange.TODAY -> dynamicTodayTitle
                    TimeRange.YESTERDAY -> "Yesterday"
                    TimeRange.LAST_7_DAYS -> "Past 7 Days"
                    TimeRange.THIS_MONTH -> "This Month"
                    TimeRange.DAYS_30 -> "Past 30 Days"
                    TimeRange.CUSTOM -> "Custom Range"
                },
                accountName = accountName,
                avatarUrl = avatarUrl,
                isOffline = isOffline,
                isRefreshing = isRefreshing,
                onAccountClick = onAccountClick
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Time Range Horizon Ribbon
        item {
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

        // Dark Grey Hero Earnings Card (#5F6368 -> #4A4E52)
        item {
            AnimatedContent(
                targetState = summary,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { targetSummary ->
                val heroBrush = remember {
                    Brush.verticalGradient(
                        colors = listOf(AdMobHeroGrey, AdMobHeroGreyBottom)
                    )
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.04f)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(heroBrush)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
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
                                        tint = Color(0xFFFDD663),
                                        modifier = Modifier.size(16.dp)
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
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.94f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isPositive = targetSummary.deltaAmount >= 0.0
                                    val deltaBadgeBg = if (isPositive) BotanicalGreenLight else CoralCrimsonLight
                                    val deltaBadgeText = if (isPositive) BotanicalGreenDark else Color(0xFFC5221F)

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
                                                tint = deltaBadgeText,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${if (isPositive) "+" else "-"}${targetSummary.currencySymbol}${formatAmount(abs(targetSummary.deltaAmount))} vs prev",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = deltaBadgeText,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (onOpenFullScreen != null) {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.14f),
                                            shape = CircleShape,
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
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${targetSummary.currencySymbol}${formatAmount(targetSummary.todayRevenue)}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = (-0.02).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                HomeMetric.REVENUE.increaseText(prev, targetSummary)?.let {
                                    IncreaseChip(text = it, color = Color(0xFF81C995), modifier = Modifier.padding(start = 8.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Earnings details",
                                    tint = Color(0xFFE8EAED),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { infoMetric = HomeMetric.REVENUE }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.16f))
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
                                            "${(targetSummary.dailyGoalProgress * 100).toInt()}% of daily goal reached!"
                                        } else {
                                            "${(targetSummary.dailyGoalProgress * 100).toInt()}% of daily goal"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isGoalAchieved) Color(0xFF81C995) else Color.White.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    val formattedGoal = if (targetSummary.periodGoal % 1.0 == 0.0) {
                                        targetSummary.periodGoal.toInt().toString()
                                    } else {
                                        formatAmount(targetSummary.periodGoal)
                                    }
                                    Surface(
                                        color = Color.White.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (targetSummary.isCustomGoal) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.22f)
                                        ),
                                        modifier = Modifier.clickable(enabled = onSetDailyGoal != null) {
                                            showSetGoalDialog = true
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Goal: ${targetSummary.currencySymbol}$formattedGoal",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Goal",
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val p = targetSummary.dailyGoalProgress
                                val progressColor = if (p >= 1f) Color(0xFF81C995) else AdMobProgressBlue
                                AnimatedGradientProgressBar(
                                    progress = p.coerceIn(0f, 1f),
                                    baseColor = progressColor,
                                    trackColor = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Elevated 2x2 KPI Metric Cards (matching the blue-card Stitch design)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedKpiCard(
                        label = "Avg eCPM",
                        value = "${summary.currencySymbol}${formatAmount(summary.ecpm)}",
                        badgeText = if (summary.ecpm > 0) "↗ Live yield" else "No impressions",
                        badgeBg = if (summary.ecpm > 0) SolarTheme.colors.successContainer else SolarTheme.colors.surfaceVariant,
                        badgeColor = if (summary.ecpm > 0) SolarTheme.colors.onSuccessContainer else SolarTheme.colors.textSecondary,
                        increaseText = HomeMetric.ECPM.increaseText(prev, summary),
                        icon = Icons.Outlined.Info,
                        onClick = { infoMetric = HomeMetric.ECPM },
                        modifier = Modifier.weight(1f)
                    )
                    ElevatedKpiCard(
                        label = "Impressions",
                        value = formatCompactNumber(summary.impressions),
                        badgeText = if (summary.impressions > 0) "${summary.fillRate}% fill rate" else "0% fill rate",
                        badgeBg = if (summary.impressions > 0) SolarTheme.colors.primaryContainer else SolarTheme.colors.surfaceVariant,
                        badgeColor = if (summary.impressions > 0) {
                            if (SolarTheme.colors.isDark) SolarTheme.colors.onPrimaryContainer else AdMobSelectedText
                        } else {
                            SolarTheme.colors.textSecondary
                        },
                        increaseText = HomeMetric.IMPRESSIONS.increaseText(prev, summary),
                        icon = Icons.Default.RemoveRedEye,
                        onClick = { infoMetric = HomeMetric.IMPRESSIONS },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedKpiCard(
                        label = "CTR",
                        value = "${summary.ctr}%",
                        badgeText = "${formatCompactNumber(summary.clicks)} clicks",
                        badgeBg = SolarTheme.colors.surfaceVariant,
                        badgeColor = SolarTheme.colors.textSecondary,
                        increaseText = HomeMetric.CTR.increaseText(prev, summary),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        onClick = { infoMetric = HomeMetric.CTR },
                        modifier = Modifier.weight(1f)
                    )
                    ElevatedKpiCard(
                        label = "Total Clicks",
                        value = formatCompactNumber(summary.clicks),
                        badgeText = if (summary.clicks > 0) "User actions" else "No clicks",
                        badgeBg = SolarTheme.colors.surfaceVariant,
                        badgeColor = SolarTheme.colors.textSecondary,
                        increaseText = HomeMetric.CLICKS.increaseText(prev, summary),
                        icon = Icons.Outlined.Info,
                        onClick = { infoMetric = HomeMetric.CLICKS },
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
                        text = "View all →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SolarAmber,
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
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SolarTheme.colors.borderMedium)
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
            val totalAppRevenue = apps.sumOf { it.todayRevenue }.coerceAtLeast(0.01)
            itemsIndexed(topApps, key = { i, _ -> topKeys[i] }) { _, app ->
                val shareRatio = (app.todayRevenue / totalAppRevenue).toFloat().coerceIn(0f, 1f)
                val sharePct = ((shareRatio * 1000).toInt() / 10.0)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.04f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(20.dp))
                        .clickable { onSelectApp(app) },
                    color = SolarTheme.colors.cardBackground,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                AppIcon(app = app, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
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
                                        text = "${formatCompactNumber(app.totalImpressions)} imp • ${summary.currencySymbol}${formatAmount(app.avgEcpm)} eCPM",
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
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$sharePct% share",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BotanicalGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(SolarTheme.colors.borderSubtle)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(shareRatio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(SolarAmber)
                            )
                        }
                    }
                }
            }
        }

        // Network Health Status Row
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.16f)
                    )
                    .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(16.dp)),
                color = SolarTheme.colors.cardBackground,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isOffline) com.lavish.admob.theme.AdMobYellow else BotanicalGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOffline) "AdMob Network: Offline (Cached)" else "AdMob Network: 100% operational",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SolarTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOffline) com.lavish.admob.theme.AdMobYellow else BotanicalGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = lastSyncedAt ?: "Synced",
                            fontSize = 11.sp,
                            color = SolarTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }

    if (showCustomDateDialog) {
        val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
        val earliest = remember(now) { now.minus(90, DateTimeUnit.DAY) }
        var startDate by remember { mutableStateOf(now.minus(29, DateTimeUnit.DAY)) }
        var endDate by remember { mutableStateOf(now) }
        var inputError by remember { mutableStateOf<String?>(null) }
        var showRangePicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (startDate > endDate) {
                            inputError = "Start date must be before end date"
                        } else if (endDate > now) {
                            inputError = "End date can't be in the future"
                        } else if (startDate < earliest) {
                            inputError = "Only the last 90 days are synced (from $earliest)"
                        } else {
                            showCustomDateDialog = false
                            onSelectCustomRange?.invoke(startDate, endDate)
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
                                    startDate = span.first
                                    endDate = span.second
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

                    Spacer(modifier = Modifier.height(4.dp))

                    DatePickerField(
                        label = "Start Date",
                        date = startDate,
                        onClick = { showRangePicker = true }
                    )

                    DatePickerField(
                        label = "End Date",
                        date = endDate,
                        onClick = { showRangePicker = true }
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

        if (showRangePicker) {
            CustomDateRangePickerModal(
                initialStartDate = startDate,
                initialEndDate = endDate,
                minDate = earliest,
                maxDate = now,
                onRangeSelected = { pickedStart, pickedEnd ->
                    startDate = pickedStart
                    endDate = pickedEnd
                    inputError = null
                    showRangePicker = false
                },
                onDismiss = { showRangePicker = false }
            )
        }
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

@Composable
private fun AnimatedGradientProgressBar(
    progress: Float,
    baseColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "goalProgressFill"
    )
    // Skip infinite frame invalidation when there is no progress fill to shimmer.
    val shimmerPhase = if (animatedProgress > 0f) {
        val transition = rememberInfiniteTransition(label = "goalProgressShimmer")
        transition.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "goalProgressPhase"
        )
    } else {
        null
    }
    val fillPath = remember { Path() }

    Spacer(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .drawWithCache {
                val corner = CornerRadius(size.height / 2f, size.height / 2f)
                val band = (size.width * 0.25f).coerceAtLeast(size.height * 4f)
                // Cached once per size/color change — zero Brush/List/Shader allocations per frame.
                val shimmerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    start = Offset.Zero,
                    end = Offset(band * 2f, 0f)
                )
                onDrawBehind {
                    drawRoundRect(
                        color = trackColor,
                        size = size,
                        cornerRadius = corner
                    )
                    val p = animatedProgress
                    val fillWidth = (size.width * p).coerceAtLeast(if (p > 0f) size.height else 0f)
                    if (fillWidth > 0f) {
                        fillPath.reset()
                        fillPath.addRoundRect(RoundRect(0f, 0f, fillWidth, size.height, corner))
                        clipPath(fillPath) {
                            drawRect(
                                color = baseColor,
                                size = Size(fillWidth, size.height)
                            )
                            val phase = shimmerPhase?.value ?: 0f
                            val startX = phase * fillWidth - band
                            translate(left = startX, top = 0f) {
                                drawRect(
                                    brush = shimmerBrush,
                                    size = Size(band * 2f, size.height)
                                )
                            }
                        }
                    }
                }
            }
    )
}


/** Metrics shown as cards on the home screen, with how to read/compare/format them. */
internal enum class HomeMetric(val title: String, val description: String) {
    REVENUE("Estimated Earnings", "Total estimated AdMob earnings for the selected period across all your apps."),
    ECPM("Average eCPM", "Effective cost per mille — what you earn per 1,000 impressions (earnings ÷ impressions × 1000)."),
    IMPRESSIONS("Impressions", "Number of ads shown to users during the selected period."),
    CTR("CTR (Click Rate)", "Click-through rate — the percentage of impressions that resulted in a click (clicks ÷ impressions × 100)."),
    CLICKS("Total Clicks", "Number of times users clicked on your ads during the selected period.");

    fun raw(s: AccountSummary): Double = when (this) {
        REVENUE -> s.todayRevenue
        ECPM -> s.ecpm
        IMPRESSIONS -> s.impressions.toDouble()
        CTR -> s.ctr
        CLICKS -> s.clicks.toDouble()
    }

    fun format(s: AccountSummary, v: Double): String = when (this) {
        REVENUE, ECPM -> "${s.currencySymbol}${formatAmount(v)}"
        IMPRESSIONS, CLICKS -> formatCompactNumber(v.toInt())
        CTR -> "${formatAmount(v)}%"
    }

    /** "+X" if the metric went up since [prev], otherwise null. */
    fun increaseText(prev: AccountSummary?, current: AccountSummary): String? {
        if (prev == null) return null
        val diff = raw(current) - raw(prev)
        val threshold = if (this == IMPRESSIONS || this == CLICKS) 0.5 else 0.005
        if (diff < threshold) return null
        return "+${format(current, diff)}"
    }
}

@Composable
private fun MetricInfoDialog(
    metric: HomeMetric,
    current: AccountSummary,
    previous: AccountSummary?,
    selectedRange: TimeRange,
    lastSyncedAt: String?,
    onDismiss: () -> Unit
) {
    val now = metric.raw(current)
    val periodLabel = when (selectedRange) {
        TimeRange.TODAY -> "Today"
        TimeRange.YESTERDAY -> "Yesterday"
        TimeRange.LAST_7_DAYS -> "Last 7 days"
        TimeRange.THIS_MONTH -> "This month"
        TimeRange.DAYS_30 -> "Last 30 days"
        TimeRange.CUSTOM -> "Custom range"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SolarTheme.colors.cardBackground,
        title = {
            Text(metric.title, fontWeight = FontWeight.Bold, color = SolarTheme.colors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(metric.description, fontSize = 13.sp, color = SolarTheme.colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Period", periodLabel)
                InfoRow("Current value", metric.format(current, now))
                if (previous != null) {
                    val before = metric.raw(previous)
                    val diff = now - before
                    InfoRow("Before last refresh", metric.format(previous, before))
                    val sign = if (diff >= 0) "+" else "-"
                    val pct = if (before != 0.0) " (${sign}${formatAmount(abs(diff) / before * 100)}%)" else ""
                    InfoRow(
                        "Change",
                        "$sign${metric.format(current, abs(diff))}$pct",
                        valueColor = when {
                            diff > 0 -> com.lavish.admob.ui.components.IncreaseGreen
                            diff < 0 -> CoralCrimson
                            else -> SolarTheme.colors.textPrimary
                        }
                    )
                } else {
                    Text(
                        "No change recorded yet — the increase will appear after the next refresh.",
                        fontSize = 12.sp,
                        color = SolarTheme.colors.textSecondary
                    )
                }
                if (metric == HomeMetric.REVENUE) {
                    InfoRow("Previous period", "${current.currencySymbol}${formatAmount(current.yesterdayRevenue)}")
                    InfoRow("Goal progress", "${(current.dailyGoalProgress * 100).toInt()}%")
                }
                if (metric == HomeMetric.IMPRESSIONS) InfoRow("Fill rate", "${current.fillRate}%")
                InfoRow("Last synced", lastSyncedAt ?: "—")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = SolarTheme.colors.textPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = SolarTheme.colors.textSecondary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

/** Local wall-clock time like "3:07 PM". */
private fun currentTimeLabel(): String {
    val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val h12 = if (t.hour % 12 == 0) 12 else t.hour % 12
    val ampm = if (t.hour < 12) "AM" else "PM"
    return "$h12:${t.minute.toString().padStart(2, '0')} $ampm"
}

@Composable
private fun ElevatedKpiCard(
    label: String,
    value: String,
    badgeText: String,
    badgeBg: Color,
    badgeColor: Color,
    increaseText: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = SolarTheme.colors.cardBackground,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SolarTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = "$label details",
                    tint = SolarTheme.colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                increaseText?.let {
                    IncreaseChip(
                        text = it,
                        color = BotanicalGreen,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SolarTheme.colors.surfaceVariant,
        border = BorderStroke(1.dp, SolarTheme.colors.textMuted.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SolarTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${date.day}, ${date.year}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SolarTheme.colors.textPrimary
                )
            }
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Pick $label",
                tint = SolarAmber,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangePickerModal(
    initialStartDate: LocalDate,
    initialEndDate: LocalDate,
    minDate: LocalDate,
    maxDate: LocalDate,
    onRangeSelected: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val minMillis = remember(minDate) { minDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }
    val maxMillis = remember(maxDate) { maxDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }
    val rangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        initialSelectedEndDateMillis = initialEndDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis in minMillis..maxMillis

            override fun isSelectableYear(year: Int): Boolean =
                year in minDate.year..maxDate.year
        }
    )

    val canConfirm = rangePickerState.selectedStartDateMillis != null &&
        rangePickerState.selectedEndDateMillis != null

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val startMillis = rangePickerState.selectedStartDateMillis
                    val endMillis = rangePickerState.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
                        val sDate = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(TimeZone.UTC).date
                        val eDate = Instant.fromEpochMilliseconds(endMillis).toLocalDateTime(TimeZone.UTC).date
                        onRangeSelected(sDate, eDate)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(
                    "OK",
                    fontWeight = FontWeight.Bold,
                    color = if (canConfirm) SolarAmber else SolarTheme.colors.textMuted
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SolarTheme.colors.textSecondary)
            }
        }
    ) {
        DateRangePicker(
            state = rangePickerState,
            showModeToggle = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(top = 8.dp)
        )
    }
}
