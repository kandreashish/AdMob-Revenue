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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Today
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
fun SolarHeader(
    title: String,
    subtitle: String,
    accountName: String? = null,
    avatarUrl: String? = null,
    isOffline: Boolean = false,
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
                if (icon != null) {
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

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SolarTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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
    TODAY("Today", Icons.Filled.Today, Icons.Outlined.Today),
    APPS("Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
    REPORTS("Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment),
    ANALYTICS("Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics),
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

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(999.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(999.dp)),
        color = SolarTheme.colors.bottomDockBackground,
        shape = RoundedCornerShape(999.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp)
                .height(52.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                // Glide: press anywhere on the dock and slide — the pill follows your finger
                // and tabs switch live as you cross them (iOS 26 tab bar behaviour).
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
            // Sliding pill
            if (trackWidthPx > 0f) {
                val slotWDp = with(density) { (trackWidthPx / tabCount).toDp() }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = indicatorPos.value * (trackWidthPx / tabCount)
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .width(slotWDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            SolarTheme.colors.primaryContainer.copy(alpha = if (isDragging) 0.75f else 1f)
                        )
                )
            }

            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    // 0..1 how "under the pill" this slot is — drives smooth colour blending while gliding.
                    val closeness = (1f - kotlin.math.abs(indicatorPos.value - index)).coerceIn(0f, 1f)
                    val activeTint = if (SolarTheme.colors.isDark) SolarTheme.colors.onPrimaryContainer else com.lavish.admob.theme.AdMobSelectedText
                    val tint = androidx.compose.ui.graphics.lerp(SolarTheme.colors.textSecondary, activeTint, closeness)
                    val isSelected = tab == currentTab

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (tab != currentTab) haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            }
                            .graphicsLayer {
                                val s = 1f + 0.08f * closeness * (if (isDragging) 1f else 0f)
                                scaleX = s; scaleY = s
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
