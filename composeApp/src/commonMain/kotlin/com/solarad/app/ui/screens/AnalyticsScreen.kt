package com.solarad.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlin.math.roundToLong
import com.solarad.app.model.AnalyticsRange
import com.solarad.app.model.AnalyticsSummary
import com.solarad.app.model.TerritoryYield
import com.solarad.app.theme.BorderSubtle
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.BotanicalGreenLight
import com.solarad.app.theme.CoralCrimson
import com.solarad.app.theme.CoralCrimsonLight
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.LinenSand
import com.solarad.app.theme.MutedSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.OceanAzureLight
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarTheme
import com.solarad.app.ui.components.MetricTwinCard
import com.solarad.app.ui.components.RevenueCurveChart
import com.solarad.app.ui.components.SolarHeader
import com.solarad.app.ui.components.SolarPillTabs
import com.solarad.app.ui.utils.formatAmount
import com.solarad.app.ui.utils.formatCompactNumber

@Composable
fun AnalyticsScreen(
    analytics: AnalyticsSummary,
    selectedRange: AnalyticsRange,
    publisherId: String,
    accountName: String = "My AdMob",
    avatarUrl: String? = null,
    isOffline: Boolean = false,
    onRangeSelect: (AnalyticsRange) -> Unit,
    onProfileClick: () -> Unit,
    onExportReport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarTheme.colors.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Header
        item {
            SolarHeader(
                title = "Analytics",
                subtitle = if (isOffline) "Revenue Velocity & Yield (Cached)" else "Revenue Velocity & Yield",
                accountName = accountName,
                avatarUrl = avatarUrl,
                isOffline = isOffline,
                onAccountClick = onProfileClick
            )
        }

        // 2. Range Selector Pills
        item {
            Spacer(modifier = Modifier.height(2.dp))
            SolarPillTabs(
                items = AnalyticsRange.entries,
                selectedItem = selectedRange,
                labelProvider = { it.label },
                onSelect = onRangeSelect
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Main Trend Card with Bezier Revenue Curve Chart
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = SolarAmber.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    )
                    .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(20.dp)),
                color = SolarTheme.colors.cardBackground,
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
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "TOTAL EARNINGS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$${formatAmount(analytics.totalRevenue)}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SolarTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Growth badge
                        val isPositive = analytics.deltaPercent >= 0.0
                        Surface(
                            color = if (isPositive) SolarTheme.colors.successContainer else SolarTheme.colors.errorContainer,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isPositive) BotanicalGreen else CoralCrimson,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${if (isPositive) "+" else ""}${analytics.deltaPercent}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) BotanicalGreen else CoralCrimson
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Gross metrics for ${analytics.periodLabel.lowercase()}",
                        fontSize = 11.sp,
                        color = SolarTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Smooth Bezier Curve Canvas
                    RevenueCurveChart(
                        points = analytics.trendPoints,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chart mini stat bar - 3 columns for compact screens
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SolarTheme.colors.surfaceVariant)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Peak Period", fontSize = 10.sp, color = SolarTheme.colors.textSecondary, maxLines = 1)
                            val peak = analytics.trendPoints.maxByOrNull { it.amount }
                            Text(
                                text = if (peak != null && peak.amount > 0) "${peak.dayLabel}: $${formatAmount(peak.amount)}" else "N/A",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Daily Average", fontSize = 10.sp, color = SolarTheme.colors.textSecondary, maxLines = 1)
                            val avg = if (analytics.trendPoints.isNotEmpty()) {
                                (analytics.totalRevenue / analytics.trendPoints.size * 100.0).roundToLong() / 100.0
                            } else 0.0
                            Text(
                                text = "$${formatAmount(avg)}/d",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textPrimary,
                                maxLines = 1
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("Status", fontSize = 10.sp, color = SolarTheme.colors.textSecondary, maxLines = 1)
                            Text(
                                text = if (analytics.totalRevenue > 0) "Verified ⚡" else "Idle",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (analytics.totalRevenue > 0) BotanicalGreen else SolarTheme.colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 4. Twin Metric Tiles: Avg eCPM & Total Impressions
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTwinCard(
                    label = "Avg eCPM",
                    value = "$${formatAmount(analytics.avgEcpm)}",
                    subBadge = if (analytics.avgEcpm > 0) "Actual yield" else "No impressions",
                    isPositive = analytics.avgEcpm > 0,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                MetricTwinCard(
                    label = "Total Impressions",
                    value = formatCompactNumber(analytics.totalImpressions),
                    subBadge = if (analytics.totalImpressions > 0) "Live ad views" else "0 views",
                    isPositive = analytics.totalImpressions > 0,
                    icon = Icons.Default.Public,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Top Territories Breakdown
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = SolarAmber.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    )
                    .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(20.dp)),
                color = SolarTheme.colors.cardBackground,
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
                        Column {
                            Text(
                                text = "Top Territories",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textPrimary
                            )
                            Text(
                                text = "Country revenue distribution",
                                fontSize = 11.sp,
                                color = SolarTheme.colors.textSecondary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = SolarTheme.colors.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = OceanAzure,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (analytics.territories.isEmpty()) {
                        Text(
                            text = "No country distribution data recorded in this timeframe",
                            fontSize = 12.sp,
                            color = SolarTheme.colors.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        analytics.territories.forEachIndexed { index, territory ->
                            TerritoryRow(
                                territory = territory,
                                rank = index + 1
                            )
                            if (index < analytics.territories.lastIndex) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }

        // 6. Action Export Card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(18.dp)),
                color = SolarTheme.colors.cardBackground,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AdMob Analytics Export",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Export reporting performance summary",
                            fontSize = 11.sp,
                            color = SolarTheme.colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    ElevatedButton(
                        onClick = onExportReport,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SolarAmber,
                            contentColor = if (SolarTheme.colors.isDark) EspressoSlate else PureWhite
                        ),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerritoryRow(
    territory: TerritoryYield,
    rank: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
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
                Text(
                    text = "#$rank",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarTheme.colors.textSecondary,
                    modifier = Modifier.width(18.dp)
                )

                Text(
                    text = territory.flag,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Text(
                    text = territory.countryName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SolarTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$${formatAmount(territory.revenue)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarTheme.colors.textPrimary
                )

                Surface(
                    color = SolarTheme.colors.surfaceVariant,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${territory.sharePercent}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val progress = (territory.sharePercent / 100f).toFloat().coerceIn(0.02f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = if (rank == 1) SolarAmber else OceanAzure,
            trackColor = SolarTheme.colors.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}
