package com.solarad.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.model.AdFormatMetric
import com.solarad.app.model.RevenuePoint
import com.solarad.app.theme.BorderMedium
import com.solarad.app.theme.BorderSubtle
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.BotanicalGreenLight
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.FormatAppOpen
import com.solarad.app.theme.FormatBanner
import com.solarad.app.theme.FormatInterstitial
import com.solarad.app.theme.FormatNative
import com.solarad.app.theme.FormatRewarded
import com.solarad.app.theme.LinenSand
import com.solarad.app.theme.MutedSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.OceanAzureLight
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarAmberLight

@Composable
fun SolarHeader(
    title: String,
    subtitle: String,
    accountName: String? = null,
    onAccountClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EspressoSlate
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BotanicalGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MutedSlate,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (accountName != null) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, BorderMedium, RoundedCornerShape(999.dp))
                    .clickable(enabled = onAccountClick != null) { onAccountClick?.invoke() },
                color = PureWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SolarAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = accountName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EspressoSlate
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF1EFEA))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            val bgColor by animateColorAsState(if (isSelected) SolarAmber else Color.Transparent)
            val textColor by animateColorAsState(if (isSelected) PureWhite else MutedSlate)

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
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
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
                shape = RoundedCornerShape(20.dp),
                ambientColor = SolarAmber.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
        color = PureWhite,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedSlate
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OceanAzure,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EspressoSlate
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = if (isPositive) OceanAzureLight else LinenSand,
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPositive) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = OceanAzure,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = subBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPositive) OceanAzure else MutedSlate
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
    val totalRevenue = formats.sumOf { it.revenue }.coerceAtLeast(0.01)

    Column(modifier = modifier.fillMaxWidth()) {
        // Multi-colored segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFEDE8E1))
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
                        .height(10.dp)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Format metric pills
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = format.formatName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = EspressoSlate
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$${format.revenue}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EspressoSlate
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "eCPM $${format.ecpm}",
                            fontSize = 11.sp,
                            color = MutedSlate
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
    if (points.isEmpty()) return

    val maxAmount = points.maxOfOrNull { it.amount } ?: 1.0
    val minAmount = (points.minOfOrNull { it.amount } ?: 0.0) * 0.85

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val bottomPadding = 24.dp.toPx()
                val usableHeight = height - bottomPadding

                if (points.size < 2) return@Canvas

                val stepX = width / (points.size - 1)
                val path = Path()
                val fillPath = Path()

                val coords = points.mapIndexed { index, point ->
                    val x = index * stepX
                    val normalized = ((point.amount - minAmount) / (maxAmount - minAmount)).toFloat()
                    val y = usableHeight - (normalized * (usableHeight * 0.75f))
                    Offset(x, y)
                }

                // Smooth cubic bezier path
                path.moveTo(coords.first().x, coords.first().y)
                fillPath.moveTo(coords.first().x, coords.first().y)

                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val controlX1 = (p0.x + p1.x) / 2
                    val controlY1 = p0.y
                    val controlX2 = (p0.x + p1.x) / 2
                    val controlY2 = p1.y

                    path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                }

                // Fill gradient area below the curve
                fillPath.lineTo(coords.last().x, usableHeight)
                fillPath.lineTo(coords.first().x, usableHeight)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SolarAmber.copy(alpha = 0.25f),
                            SolarAmber.copy(alpha = 0.03f)
                        ),
                        startY = 0f,
                        endY = usableHeight
                    )
                )

                // Stroke line
                drawPath(
                    path = path,
                    color = SolarAmber,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight peak/current point
                coords.forEachIndexed { idx, pt ->
                    if (points[idx].isCurrentOrPeak) {
                        drawCircle(
                            color = PureWhite,
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = SolarAmber,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = SolarAmber.copy(alpha = 0.3f),
                            radius = 10.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        // X-axis Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.dayLabel,
                    fontSize = 11.sp,
                    fontWeight = if (pt.isCurrentOrPeak) FontWeight.Bold else FontWeight.Medium,
                    color = if (pt.isCurrentOrPeak) SolarAmber else MutedSlate
                )
            }
        }
    }
}

enum class NavigationTab(val label: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    TODAY("Today", Icons.Filled.Today, Icons.Outlined.Today),
    APPS("Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
    ANALYTICS("Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    WALLET("Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
}

@Composable
fun SolarBottomDock(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(999.dp),
                ambientColor = SolarAmber.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp)),
        color = PureWhite,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                val tabBgColor by animateColorAsState(if (isSelected) SolarAmber else Color.Transparent)
                val iconTint by animateColorAsState(if (isSelected) PureWhite else MutedSlate)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(tabBgColor)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                        contentDescription = tab.label,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }
        }
    }
}
