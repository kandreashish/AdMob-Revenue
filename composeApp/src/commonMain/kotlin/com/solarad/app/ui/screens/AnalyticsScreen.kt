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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.model.AnalyticsRange
import com.solarad.app.model.AnalyticsSummary
import com.solarad.app.model.TerritoryYield
import com.solarad.app.ui.components.MetricTwinCard
import com.solarad.app.ui.components.RevenueCurveChart
import com.solarad.app.ui.components.SolarHeader
import com.solarad.app.ui.components.SolarPillTabs
import com.solarad.app.theme.BorderMedium
import com.solarad.app.theme.BorderSubtle
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.BotanicalGreenLight
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.LinenSand
import com.solarad.app.theme.MutedSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.OceanAzureLight
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarAmberLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    analytics: AnalyticsSummary,
    selectedRange: AnalyticsRange,
    publisherId: String,
    onRangeSelect: (AnalyticsRange) -> Unit,
    onProfileClick: () -> Unit,
    onExportReport: () -> Unit
) {
    var selectedTerritory by remember { mutableStateOf<TerritoryYield?>(null) }
    var showExportToast by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LinenSand),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Header
        item {
            SolarHeader(
                title = "Analytics",
                subtitle = "Trends & Country Yield",
                accountName = publisherId.takeLast(8),
                onAccountClick = onProfileClick
            )
        }

        // 2. Range Selector Pills
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SolarPillTabs(
                items = AnalyticsRange.entries,
                selectedItem = selectedRange,
                labelProvider = { it.label },
                onSelect = onRangeSelect
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Main Trend Card with Bezier Revenue Curve Chart
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = SolarAmber.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    )
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
                color = PureWhite,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL REVENUE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedSlate,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$${formatAmount(analytics.totalRevenue)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EspressoSlate
                            )
                        }

                        // Growth badge
                        Surface(
                            color = BotanicalGreenLight,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = BotanicalGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${analytics.deltaPercent}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BotanicalGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gross earnings for ${analytics.periodLabel.lowercase()}",
                        fontSize = 12.sp,
                        color = MutedSlate
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Smooth Bezier Curve Canvas
                    RevenueCurveChart(
                        points = analytics.trendPoints,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chart mini stat bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LinenSand)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Peak Day", fontSize = 11.sp, color = MutedSlate)
                            val peak = analytics.trendPoints.maxByOrNull { it.amount }
                            Text(
                                text = "${peak?.dayLabel}: $${peak?.amount?.toInt() ?: 0}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EspressoSlate
                            )
                        }
                        Column {
                            Text("Daily Average", fontSize = 11.sp, color = MutedSlate)
                            val avg = if (analytics.trendPoints.isNotEmpty()) {
                                (analytics.totalRevenue / analytics.trendPoints.size).toInt()
                            } else 0
                            Text(
                                text = "$$avg/day",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EspressoSlate
                            )
                        }
                        Column {
                            Text("Status", fontSize = 11.sp, color = MutedSlate)
                            Text(
                                text = "Verified ⚡",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BotanicalGreen
                            )
                        }
                    }
                }
            }
        }

        // 4. Twin Metric Tiles: Avg eCPM & Total Impressions
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTwinCard(
                    label = "Avg eCPM",
                    value = "$${analytics.avgEcpm}",
                    subBadge = "+$2.10 vs avg",
                    isPositive = true,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                MetricTwinCard(
                    label = "Total Impressions",
                    value = formatCompactNumber(analytics.totalImpressions),
                    subBadge = "+12.8% volume",
                    isPositive = true,
                    icon = Icons.Default.Analytics,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Top Territories Breakdown
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = SolarAmber.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    )
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
                color = PureWhite,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Top Territories",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EspressoSlate
                            )
                            Text(
                                text = "Revenue distribution by country",
                                fontSize = 12.sp,
                                color = MutedSlate
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = OceanAzureLight
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = OceanAzure,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    analytics.territories.forEachIndexed { index, territory ->
                        TerritoryRow(
                            territory = territory,
                            rank = index + 1,
                            onClick = { selectedTerritory = territory }
                        )
                        if (index < analytics.territories.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        // 6. Action Export Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                color = PureWhite,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audience Yield Report",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EspressoSlate
                        )
                        Text(
                            text = "Download CSV breakdown with eCPM per geo & ad unit",
                            fontSize = 11.sp,
                            color = MutedSlate
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    ElevatedButton(
                        onClick = {
                            onExportReport()
                            showExportToast = true
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SolarAmber,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export",
                            fontSize = 12.sp,
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
    rank: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank number
                Text(
                    text = "#$rank",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedSlate,
                    modifier = Modifier.width(22.dp)
                )

                // Flag
                Text(
                    text = territory.flag,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Name
                Text(
                    text = territory.countryName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EspressoSlate
                )
            }

            // Yield & Share
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$${formatAmount(territory.revenue)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EspressoSlate
                )

                Surface(
                    color = LinenSand,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${territory.sharePercent}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedSlate,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar reflecting share percentage
        val progress = (territory.sharePercent / 100f).toFloat().coerceIn(0.02f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = if (rank == 1) SolarAmber else OceanAzure,
            trackColor = Color(0xFFEBE7DF),
            strokeCap = StrokeCap.Round
        )
    }
}

private fun formatAmount(amount: Double): String {
    val rounded = (amount * 100).toLong() / 100.0
    val parts = rounded.toString().split(".")
    val integerPart = parts[0].reversed().chunked(3).joinToString(",").reversed()
    val decimalPart = if (parts.size > 1) parts[1].padEnd(2, '0').take(2) else "00"
    return "$integerPart.$decimalPart"
}

private fun formatCompactNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> "${(number / 100_000) / 10.0}M"
        number >= 1_000 -> "${(number / 100) / 10.0}K"
        else -> number.toString()
    }
}
