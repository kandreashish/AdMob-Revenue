package com.lavish.admob.ui.components

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxHeight
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavish.admob.model.AdFormatMetric
import com.lavish.admob.model.RevenuePoint
import com.lavish.admob.theme.BorderMedium
import com.lavish.admob.theme.BorderSubtle
import com.lavish.admob.theme.BotanicalGreen
import com.lavish.admob.theme.CoralCrimson
import com.lavish.admob.theme.EspressoSlate
import com.lavish.admob.theme.FormatAppOpen
import com.lavish.admob.theme.FormatBanner
import com.lavish.admob.theme.FormatInterstitial
import com.lavish.admob.theme.FormatNative
import com.lavish.admob.theme.FormatRewarded
import com.lavish.admob.theme.LinenSand
import com.lavish.admob.theme.MutedSlate
import com.lavish.admob.theme.OceanAzure
import com.lavish.admob.theme.OceanAzureLight
import com.lavish.admob.theme.PureWhite
import com.lavish.admob.theme.SolarAmber
import com.lavish.admob.theme.SolarTheme

@Composable
fun AdMobBrandHeader(
    title: String,
    accountName: String? = null,
    avatarUrl: String? = null,
    isOffline: Boolean = false,
    isRefreshing: Boolean = false,
    onAccountClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SolarTheme.colors.surface
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // AdMob 3-dot geometric brand mark
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(com.lavish.admob.theme.AdMobCoralRed)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(com.lavish.admob.theme.AdMobYellow)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SolarAmber)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AdMob",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SolarTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(SolarTheme.colors.borderMedium)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusBg = if (isOffline) com.lavish.admob.theme.AdMobYellowLight else SolarTheme.colors.successContainer
                    val statusDot = if (isOffline) com.lavish.admob.theme.AdMobYellow else BotanicalGreen
                    val statusText = if (isOffline) com.lavish.admob.theme.EspressoSlate else SolarTheme.colors.onSuccessContainer
                    val statusLabel = when {
                        isOffline -> "Offline"
                        isRefreshing -> "Syncing"
                        else -> "Live"
                    }

                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusDot)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = statusLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusText
                            )
                        }
                    }

                    if (onAccountClick != null) {
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onAccountClick() },
                            color = com.lavish.admob.theme.AdMobCoralRed,
                            shape = CircleShape
                        ) {
                            UserProfileAvatar(
                                avatarUrl = avatarUrl,
                                userName = accountName ?: "",
                                modifier = Modifier.fillMaxSize(),
                                iconSize = 18.dp
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SolarTheme.colors.borderMedium)
            )
        }
    }
}

@Composable
fun SolarHeader(
    title: String,
    subtitle: String,
    accountName: String? = null,
    avatarUrl: String? = null,
    isOffline: Boolean = false,
    version: String? = null,
    onAccountClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SolarTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Brand red accent underline under the screen title
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .width(28.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(com.lavish.admob.theme.CoralCrimson)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isOffline) com.lavish.admob.theme.AdMobYellow else BotanicalGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = SolarTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onAccountClick != null || version != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onAccountClick != null) {
                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, SolarTheme.colors.borderMedium, CircleShape)
                            .clickable { onAccountClick() },
                        color = SolarTheme.colors.surface,
                        shadowElevation = 1.dp
                    ) {
                        UserProfileAvatar(
                            avatarUrl = avatarUrl,
                            userName = accountName ?: "",
                            modifier = Modifier.fillMaxSize(),
                            iconSize = 20.dp
                        )
                    }
                }
                if (version != null) {
                    if (onAccountClick != null) Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = version,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.4.sp,
                        color = SolarTheme.colors.textSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun <T> SolarPillTabs(
    items: List<T>,
    selectedItem: T,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(999.dp),
        color = SolarTheme.colors.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val bgColor by animateColorAsState(if (isSelected) SolarTheme.colors.primaryContainer else Color.Transparent)
                val textColor by animateColorAsState(
                    if (isSelected) {
                        if (SolarTheme.colors.isDark) SolarTheme.colors.onPrimaryContainer else com.lavish.admob.theme.AdMobSelectedText
                    } else {
                        SolarTheme.colors.textSecondary
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(item) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MetricTwinCard(
    label: String,
    value: String,
    subBadge: String,
    isPositive: Boolean = true,
    icon: ImageVector? = null,
    increaseText: String? = null,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(18.dp)),
        color = SolarTheme.colors.cardBackground,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SolarTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (onInfoClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "$label details",
                        tint = SolarTheme.colors.textSecondary,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { onInfoClick() }
                            .padding(3.dp)
                    )
                } else if (icon != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OceanAzure,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (increaseText != null) {
                    IncreaseChip(text = increaseText, color = IncreaseGreen, modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = if (isPositive) SolarTheme.colors.secondaryContainer else SolarTheme.colors.surfaceVariant,
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (isPositive) OceanAzure else SolarTheme.colors.textSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = subBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPositive) OceanAzure else SolarTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

val IncreaseGreen = Color(0xFF1E8E3E)

/** Small "▲ +X" chip shown when a metric went up; fades out after [visibleMillis]. */
@Composable
fun IncreaseChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    visibleMillis: Long = 12_000L
) {
    // Re-keyed on text so a new increase shows the chip again.
    var visible by androidx.compose.runtime.remember(text) { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(text) {
        kotlinx.coroutines.delay(visibleMillis)
        visible = false
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(durationMillis = 800)),
        modifier = modifier
    ) {
        Surface(
            color = color.copy(alpha = 0.14f),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = "▲ $text",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
fun FormatSegmentedBar(
    formats: List<AdFormatMetric>,
    modifier: Modifier = Modifier
) {
    if (formats.isEmpty()) {
        Text(
            text = "No ad unit breakdown recorded",
            fontSize = 11.sp,
            color = SolarTheme.colors.textSecondary
        )
        return
    }

    val totalRevenue = formats.sumOf { it.revenue }.coerceAtLeast(0.01)

    Column(modifier = modifier.fillMaxWidth()) {
        // Multi-colored segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(SolarTheme.colors.surfaceVariant)
        ) {
            formats.forEach { format ->
                val weight = (format.revenue / totalRevenue).toFloat().coerceAtLeast(0.02f)
                val color = when {
                    format.formatName.contains("Rewarded", ignoreCase = true) -> FormatRewarded
                    format.formatName.contains("Interstitial", ignoreCase = true) -> FormatInterstitial
                    format.formatName.contains("App Open", ignoreCase = true) -> FormatAppOpen
                    format.formatName.contains("Native", ignoreCase = true) -> FormatNative
                    else -> FormatBanner
                }
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(8.dp)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Format metric pills
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            formats.forEach { format ->
                val color = when {
                    format.formatName.contains("Rewarded", ignoreCase = true) -> FormatRewarded
                    format.formatName.contains("Interstitial", ignoreCase = true) -> FormatInterstitial
                    format.formatName.contains("App Open", ignoreCase = true) -> FormatAppOpen
                    format.formatName.contains("Native", ignoreCase = true) -> FormatNative
                    else -> FormatBanner
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = format.formatName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SolarTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$${format.revenue}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "eCPM $${format.ecpm}",
                            fontSize = 10.sp,
                            color = SolarTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RevenueCurveChart(
    points: List<RevenuePoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No revenue trends in this period", fontSize = 12.sp, color = MutedSlate)
        }
        return
    }

    val maxAmount = remember(points) { points.maxOfOrNull { it.amount } ?: 1.0 }
    val minAmount = remember(points) { (points.minOfOrNull { it.amount } ?: 0.0) * 0.85 }
    val range = (maxAmount - minAmount).coerceAtLeast(1.0)
    val dotCenterColor = SolarTheme.colors.cardBackground

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            // drawWithCache: paths/brush/coords are rebuilt only when size or points change,
            // not on every draw pass.
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithCache {
                        val width = size.width
                        val height = size.height
                        val bottomPadding = 20.dp.toPx()
                        val usableHeight = (height - bottomPadding).coerceAtLeast(1f)
                        if (points.size < 2) return@drawWithCache onDrawBehind {}

                        val stepX = width / (points.size - 1)
                        val coords = points.mapIndexed { index, point ->
                            val normalized = if (maxAmount > minAmount) {
                                ((point.amount - minAmount) / range).toFloat().coerceIn(0f, 1f)
                            } else 0.5f
                            Offset(index * stepX, usableHeight - (normalized * (usableHeight * 0.75f)))
                        }
                        val path = Path()
                        val fillPath = Path()
                        path.moveTo(coords.first().x, coords.first().y)
                        fillPath.moveTo(coords.first().x, coords.first().y)
                        for (i in 0 until coords.size - 1) {
                            val p0 = coords[i]
                            val p1 = coords[i + 1]
                            val cx = (p0.x + p1.x) / 2
                            path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                        fillPath.lineTo(coords.last().x, usableHeight)
                        fillPath.lineTo(coords.first().x, usableHeight)
                        fillPath.close()
                        val fillBrush = Brush.verticalGradient(
                            colors = listOf(SolarAmber.copy(alpha = 0.25f), SolarAmber.copy(alpha = 0.03f)),
                            startY = 0f,
                            endY = usableHeight
                        )
                        val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        val outerR = 5.dp.toPx()
                        val innerR = 3.5.dp.toPx()
                        val highlights = coords.filterIndexed { idx, _ -> points[idx].isCurrentOrPeak }

                        onDrawBehind {
                            drawPath(path = fillPath, brush = fillBrush)
                            drawPath(path = path, color = SolarAmber, style = stroke)
                            highlights.forEach { pt ->
                                drawCircle(color = dotCenterColor, radius = outerR, center = pt)
                                drawCircle(color = SolarAmber, radius = innerR, center = pt)
                            }
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // X-axis Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.dayLabel,
                    fontSize = 10.sp,
                    fontWeight = if (pt.isCurrentOrPeak) FontWeight.Bold else FontWeight.Medium,
                    color = if (pt.isCurrentOrPeak) SolarAmber else SolarTheme.colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

enum class NavigationTab(val label: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    TODAY("Today", Icons.Filled.AutoGraph, Icons.Outlined.AutoGraph),
    APPS("Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
    REPORTS("Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    ANALYTICS("Analytics", Icons.Filled.QueryStats, Icons.Outlined.QueryStats),
    WALLET("Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
}

@Composable
fun SolarBottomDock(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = NavigationTab.entries
    val tabCount = tabs.size
    val density = androidx.compose.ui.platform.LocalDensity.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val latestOnTabSelected by androidx.compose.runtime.rememberUpdatedState(onTabSelected)
    val latestTab by androidx.compose.runtime.rememberUpdatedState(currentTab)

    var trackWidthPx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    // Indicator position in "slot units" (0f..tabCount-1); fractional while gliding.
    val indicatorPos = androidx.compose.runtime.remember {
        androidx.compose.animation.core.Animatable(currentTab.ordinal.toFloat())
    }
    var isDragging by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        if (isDragging) 1.12f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "dockLift"
    )
    val ioSpring = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.72f, stiffness = 420f)

    // Follow external tab changes (taps, back button, links) with an iOS-like spring.
    androidx.compose.runtime.LaunchedEffect(currentTab) {
        if (!isDragging) indicatorPos.animateTo(currentTab.ordinal.toFloat(), ioSpring)
    }

    fun slotFor(x: Float): Float {
        if (trackWidthPx <= 0f) return 0f
        val slotW = trackWidthPx / tabCount
        return ((x - slotW / 2f) / slotW).coerceIn(0f, (tabCount - 1).toFloat())
    }

    fun selectIfChanged(index: Int) {
        val tab = tabs[index]
        if (tab != latestTab) {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            latestOnTabSelected(tab)
        }
    }

    val dockShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val activeTint = if (SolarTheme.colors.isDark) {
        SolarTheme.colors.onPrimaryContainer
    } else {
        com.lavish.admob.theme.AdMobSelectedText
    }
    val pillBg = SolarTheme.colors.primaryContainer
    val pillWidthDp = 56.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = dockShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .border(1.dp, SolarTheme.colors.borderSubtle.copy(alpha = 0.85f), dockShape),
        color = SolarTheme.colors.bottomDockBackground,
        shape = dockShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp)
                .height(68.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                // Glide: press anywhere on the dock and slide — the pill follows your finger
                // and tabs switch live as you cross them.
                .pointerInput(tabCount) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            scope.launch { indicatorPos.animateTo(slotFor(offset.x), ioSpring) }
                            selectIfChanged(kotlin.math.round(slotFor(offset.x)).toInt())
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val pos = slotFor(change.position.x)
                            scope.launch { indicatorPos.snapTo(pos) }
                            selectIfChanged(kotlin.math.round(pos).toInt())
                        },
                        onDragEnd = {
                            isDragging = false
                            val target = kotlin.math.round(indicatorPos.value).toInt()
                            selectIfChanged(target)
                            scope.launch { indicatorPos.animateTo(target.toFloat(), ioSpring) }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch { indicatorPos.animateTo(latestTab.ordinal.toFloat(), ioSpring) }
                        }
                    )
                }
        ) {
            // Sliding pill indicator behind the active icon
            if (trackWidthPx > 0f) {
                val slotWPx = trackWidthPx / tabCount
                val pillWPx = with(density) { pillWidthDp.toPx() }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 8.dp)
                        .graphicsLayer {
                            translationX = indicatorPos.value * slotWPx + (slotWPx - pillWPx) / 2f
                            scaleX = pressScale
                        }
                        .width(pillWidthDp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(pillBg)
                )
            }

            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    // 0..1 how close the indicator is to this slot — drives smooth colour blending while gliding.
                    val closeness = (1f - kotlin.math.abs(indicatorPos.value - index)).coerceIn(0f, 1f)
                    val tint = androidx.compose.ui.graphics.lerp(SolarTheme.colors.textSecondary, activeTint, closeness)
                    val isSelected = tab == currentTab

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (tab != currentTab) haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            }
                            .padding(top = 8.dp, bottom = 8.dp)
                            .graphicsLayer {
                                val s = 1f + 0.06f * closeness * (if (isDragging) 1f else 0f)
                                scaleX = s; scaleY = s
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.height(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.label,
                                tint = tint,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
