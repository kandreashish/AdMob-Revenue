package com.solarad.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.model.AccountSummary
import com.solarad.app.model.AppMonetization
import com.solarad.app.model.TimeRange
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
import com.solarad.app.theme.SolarAmberGlow
import com.solarad.app.theme.SolarAmberLight
import com.solarad.app.ui.components.MetricTwinCard
import com.solarad.app.ui.components.SolarHeader
import com.solarad.app.ui.components.SolarPillTabs

@Composable
fun TodayScreen(
    summary: AccountSummary,
    apps: List<AppMonetization>,
    selectedRange: TimeRange,
    onSelectRange: (TimeRange) -> Unit,
    onViewAllApps: () -> Unit,
    onSelectApp: (AppMonetization) -> Unit,
    onAccountClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LinenSand),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Header
        item {
            SolarHeader(
                title = "Today, Nov 19",
                subtitle = summary.lastSyncTime,
                accountName = "Alex's AdMob",
                onAccountClick = onAccountClick
            )
        }

        // Time Range Pills (Today, Yesterday, This Month)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SolarPillTabs(
                items = TimeRange.values().toList(),
                selectedItem = selectedRange,
                labelProvider = { it.label },
                onSelect = onSelectRange
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Hero Sunlit Revenue Card
        item {
            AnimatedContent(
                targetState = summary,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { targetSummary ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = SolarAmber.copy(alpha = 0.18f),
                            spotColor = Color.Black.copy(alpha = 0.05f)
                        )
                        .border(1.dp, SolarAmber.copy(alpha = 0.35f), RoundedCornerShape(28.dp)),
                    color = PureWhite,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(SolarAmberGlow.copy(alpha = 0.45f))
                            .padding(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = SolarAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (selectedRange) {
                                        TimeRange.TODAY -> "Today's Total"
                                        TimeRange.YESTERDAY -> "Yesterday's Final"
                                        TimeRange.THIS_MONTH -> "This Month to Date"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MutedSlate
                                )
                            }

                            // Delta Badge
                            Surface(
                                color = OceanAzureLight,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = OceanAzure,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+$${targetSummary.deltaAmount.toInt()} vs prev",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OceanAzure
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Confident Currency Figure
                        Text(
                            text = "$${targetSummary.todayRevenue}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EspressoSlate,
                            letterSpacing = (-0.03).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Daily Goal Progress Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(targetSummary.dailyGoalProgress * 100).toInt()}% of diurnal goal reached",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EspressoSlate
                                )
                                Text(
                                    text = "Goal: $${targetSummary.dailyGoal.toInt()}",
                                    fontSize = 12.sp,
                                    color = MutedSlate
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { targetSummary.dailyGoalProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = SolarAmber,
                                trackColor = Color(0xFFF1EBE1),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2 Crisp Focus Metric Twin Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTwinCard(
                    label = "Average eCPM",
                    value = "$${summary.ecpm}",
                    subBadge = "+8.4% pacing",
                    isPositive = true,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                MetricTwinCard(
                    label = "Impressions",
                    value = "${summary.impressions / 1000}k",
                    subBadge = "${summary.fillRate}% fill",
                    isPositive = true,
                    icon = Icons.Default.RemoveRedEye,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Top Performing Apps Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Performing Apps",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EspressoSlate
                )
                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OceanAzure,
                    modifier = Modifier.clickable { onViewAllApps() }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // App Mini List Items
        items(apps.take(3)) { app ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .clickable { onSelectApp(app) },
                color = PureWhite
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // App Icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (app.name) {
                                        "CyberDrift" -> Color(0xFF0284C7)
                                        "FocusAI" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF8B5CF6)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (app.name) {
                                    "CyberDrift" -> Icons.Default.SportsEsports
                                    "FocusAI" -> Icons.Default.TrendingUp
                                    else -> Icons.Default.Widgets
                                },
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = app.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = EspressoSlate
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${app.platform} • ${app.formats.firstOrNull()?.formatName ?: "AdMob"}",
                                fontSize = 12.sp,
                                color = MutedSlate
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${app.todayRevenue}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = EspressoSlate
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = BotanicalGreenLight,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "+$${app.deltaVsYesterday.toInt()} vs y'day",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BotanicalGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
