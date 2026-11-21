package com.solarad.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.data.AdMobApiClient
import com.solarad.app.data.AdMobReportRow
import com.solarad.app.model.AppMonetization
import com.solarad.app.model.TimeRange
import com.solarad.app.ui.components.AppIcon
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarAmberGlow
import com.solarad.app.theme.SolarAmberLight
import com.solarad.app.theme.SolarTheme
import com.solarad.app.ui.components.MetricTwinCard
import com.solarad.app.ui.utils.formatAmount
import com.solarad.app.ui.utils.formatCompactNumber
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

private enum class DetailTimeHorizon(val label: String, val badge: String) {
    TODAY("Today", "Live"),
    YESTERDAY("Y'day", "Final"),
    DAYS_7("7D", "1W"),
    DAYS_30("30D", "1M"),
    DAYS_90("90D", "All")
}

private data class CountryBreakdown(
    val countryCode: String,
    val countryName: String,
    val flagEmoji: String,
    val earnings: Double,
    val impressions: Int,
    val clicks: Int,
    val ecpm: Double,
    val sharePercent: Float
)

private data class AdFormatBreakdown(
    val formatKey: String,
    val displayName: String,
    val icon: ImageVector,
    val earnings: Double,
    val impressions: Int,
    val clicks: Int,
    val ecpm: Double,
    val sharePercent: Float
)

private data class DailyBreakdown(
    val rawDate: String,
    val formattedDate: String,
    val earnings: Double,
    val impressions: Int,
    val clicks: Int,
    val ecpm: Double
)

fun countryCodeToFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    val ch1 = countryCode[0].uppercaseChar()
    val ch2 = countryCode[1].uppercaseChar()
    if (ch1 !in 'A'..'Z' || ch2 !in 'A'..'Z') return "🌐"
    val high = 0xD83C.toChar()
    val low1 = (0xDDE6 + (ch1 - 'A')).toChar()
    val low2 = (0xDDE6 + (ch2 - 'A')).toChar()
    return "$high$low1$high$low2"
}

fun resolveCountryName(code: String): String {
    return when (code.uppercase()) {
        "US" -> "United States"
        "IN" -> "India"
        "BR" -> "Brazil"
        "FR" -> "France"
        "DE" -> "Germany"
        "GB" -> "United Kingdom"
        "NG" -> "Nigeria"
        "ID" -> "Indonesia"
        "EG" -> "Egypt"
        "SA" -> "Saudi Arabia"
        "AE" -> "United Arab Emirates"
        "CA" -> "Canada"
        "AU" -> "Australia"
        "IT" -> "Italy"
        "ES" -> "Spain"
        "MX" -> "Mexico"
        "JP" -> "Japan"
        "PK" -> "Pakistan"
        "VN" -> "Vietnam"
        "TR" -> "Turkey"
        "RU" -> "Russia"
        "KR" -> "South Korea"
        "NL" -> "Netherlands"
        "PH" -> "Philippines"
        "BD" -> "Bangladesh"
        "CO" -> "Colombia"
        "AR" -> "Argentina"
        "ZA" -> "South Africa"
        "TH" -> "Thailand"
        "MY" -> "Malaysia"
        "CL" -> "Chile"
        "PE" -> "Peru"
        "PL" -> "Poland"
        "RO" -> "Romania"
        "UA" -> "Ukraine"
        "CR" -> "Costa Rica"
        "HR" -> "Croatia"
        "PY" -> "Paraguay"
        "TW" -> "Taiwan"
        "DZ" -> "Algeria"
        "MA" -> "Morocco"
        "IQ" -> "Iraq"
        "KW" -> "Kuwait"
        "OM" -> "Oman"
        "QA" -> "Qatar"
        "IL" -> "Israel"
        "IE" -> "Ireland"
        "SE" -> "Sweden"
        "CH" -> "Switzerland"
        "AT" -> "Austria"
        "BE" -> "Belgium"
        "PT" -> "Portugal"
        "GR" -> "Greece"
        "CZ" -> "Czechia"
        "HU" -> "Hungary"
        "RS" -> "Serbia"
        "KE" -> "Kenya"
        "GH" -> "Ghana"
        "LK" -> "Sri Lanka"
        "NP" -> "Nepal"
        "MM" -> "Myanmar"
        "KZ" -> "Kazakhstan"
        "UZ" -> "Uzbekistan"
        "EC" -> "Ecuador"
        "GT" -> "Guatemala"
        "DO" -> "Dominican Rep."
        "BO" -> "Bolivia"
        "UY" -> "Uruguay"
        "SN" -> "Senegal"
        "ET" -> "Ethiopia"
        "TZ" -> "Tanzania"
        "UG" -> "Uganda"
        "ZW" -> "Zimbabwe"
        "ZM" -> "Zambia"
        else -> code
    }
}

private fun formatDisplayDate(raw: String, todayStr: String, yesterdayStr: String): String {
    if (raw == todayStr) return "Today"
    if (raw == yesterdayStr) return "Yesterday"
    if (raw.length == 8) {
        val y = raw.substring(0, 4)
        val m = raw.substring(4, 6)
        val d = raw.substring(6, 8)
        val monthName = when (m) {
            "01" -> "Jan"
            "02" -> "Feb"
            "03" -> "Mar"
            "04" -> "Apr"
            "05" -> "May"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Aug"
            "09" -> "Sep"
            "10" -> "Oct"
            "11" -> "Nov"
            "12" -> "Dec"
            else -> m
        }
        return "$monthName ${d.trimStart('0')}"
    }
    return raw
}

private fun mapTimeRangeToHorizon(timeRange: TimeRange?): DetailTimeHorizon {
    return when (timeRange) {
        TimeRange.TODAY -> DetailTimeHorizon.TODAY
        TimeRange.YESTERDAY -> DetailTimeHorizon.YESTERDAY
        TimeRange.LAST_7_DAYS -> DetailTimeHorizon.DAYS_7
        TimeRange.DAYS_30, TimeRange.THIS_MONTH -> DetailTimeHorizon.DAYS_30
        else -> DetailTimeHorizon.TODAY
    }
}

private const val MAX_EXPANDED_COUNTRIES = 50

@Composable
fun AppDetailScreen(
    app: AppMonetization,
    reportRows: List<AdMobReportRow>,
    initialTimeRange: TimeRange? = TimeRange.TODAY,
    currencySymbol: String = "$",
    onBack: () -> Unit
) {
    var selectedHorizon by remember(app.id, initialTimeRange) {
        mutableStateOf(mapTimeRangeToHorizon(initialTimeRange))
    }
    var showAllCountries by remember { mutableStateOf(false) }

    val isIos = app.platform.equals("iOS", ignoreCase = true)
    val now by rememberCurrentDate()
    val todayStr = remember(now) { AdMobApiClient.formatDateKey(now) }
    val yesterdayStr = remember(now) { AdMobApiClient.formatDateKey(now.minus(1, DateTimeUnit.DAY)) }
    val days7List = remember(now) { (6 downTo 0).mapTo(HashSet()) { AdMobApiClient.formatDateKey(now.minus(it, DateTimeUnit.DAY)) } }
    val days30List = remember(now) { (29 downTo 0).mapTo(HashSet()) { AdMobApiClient.formatDateKey(now.minus(it, DateTimeUnit.DAY)) } }

    // Filter report rows specifically for this app
    val appAllRows = remember(reportRows, app.id, app.name) {
        reportRows.filter { row ->
            (row.appId.isNotBlank() && row.appId == app.id) ||
            (row.appName.isNotBlank() && row.appName.equals(app.name, ignoreCase = true))
        }
    }

    // Filter rows based on active horizon
    val activeRows = remember(appAllRows, selectedHorizon, todayStr, yesterdayStr, days7List, days30List) {
        when (selectedHorizon) {
            DetailTimeHorizon.TODAY -> appAllRows.filter { it.date == todayStr }
            DetailTimeHorizon.YESTERDAY -> appAllRows.filter { it.date == yesterdayStr }
            DetailTimeHorizon.DAYS_7 -> appAllRows.filter { it.date in days7List }
            DetailTimeHorizon.DAYS_30 -> appAllRows.filter { it.date in days30List }
            DetailTimeHorizon.DAYS_90 -> appAllRows
        }
    }

    // Aggregated Metrics for selected horizon
    val totalRevenue = remember(activeRows) { activeRows.sumOf { it.earnings } }
    val totalImpressions = remember(activeRows) { activeRows.sumOf { it.impressions } }
    val totalClicks = remember(activeRows) { activeRows.sumOf { it.clicks } }
    val avgEcpm = remember(totalRevenue, totalImpressions) {
        if (totalImpressions > 0) (totalRevenue / totalImpressions) * 1000.0 else 0.0
    }
    val ctr = remember(totalClicks, totalImpressions) {
        if (totalImpressions > 0) (totalClicks.toDouble() / totalImpressions.toDouble()) * 100.0 else 0.0
    }

    // Country Breakdown
    val countryList = remember(activeRows, totalRevenue) {
        activeRows
            .filter { it.country.isNotBlank() }
            .groupBy { it.country.uppercase() }
            .map { (code, cRows) ->
                val cRev = cRows.sumOf { it.earnings }
                val cImp = cRows.sumOf { it.impressions }
                val cClicks = cRows.sumOf { it.clicks }
                val cEcpm = if (cImp > 0) (cRev / cImp) * 1000.0 else 0.0
                val share = if (totalRevenue > 0.0) (cRev / totalRevenue).toFloat().coerceIn(0f, 1f) else 0f
                CountryBreakdown(
                    countryCode = code,
                    countryName = resolveCountryName(code),
                    flagEmoji = countryCodeToFlagEmoji(code),
                    earnings = cRev,
                    impressions = cImp,
                    clicks = cClicks,
                    ecpm = cEcpm,
                    sharePercent = share
                )
            }
            .sortedByDescending { it.earnings }
    }

    // Ad Format Breakdown
    val formatList = remember(activeRows, totalRevenue) {
        activeRows
            .filter { it.format.isNotBlank() }
            .groupBy { it.format.lowercase() }
            .map { (rawFmt, fRows) ->
                val fRev = fRows.sumOf { it.earnings }
                val fImp = fRows.sumOf { it.impressions }
                val fClicks = fRows.sumOf { it.clicks }
                val fEcpm = if (fImp > 0) (fRev / fImp) * 1000.0 else 0.0
                val share = if (totalRevenue > 0.0) (fRev / totalRevenue).toFloat().coerceIn(0f, 1f) else 0f
                val (dispName, icon) = when {
                    rawFmt.contains("reward") -> "Rewarded Video" to Icons.Default.WbSunny
                    rawFmt.contains("interstitial") -> "Interstitial Ad" to Icons.Default.ViewCarousel
                    rawFmt.contains("native") -> "Native Advanced" to Icons.Default.Widgets
                    rawFmt.contains("banner") -> "Banner Ad" to Icons.Default.ViewCarousel
                    rawFmt.contains("open") -> "App Open Ad" to Icons.Default.PhoneAndroid
                    else -> rawFmt.replaceFirstChar { it.uppercase() } to Icons.Default.Widgets
                }
                AdFormatBreakdown(
                    formatKey = rawFmt,
                    displayName = dispName,
                    icon = icon,
                    earnings = fRev,
                    impressions = fImp,
                    clicks = fClicks,
                    ecpm = fEcpm,
                    sharePercent = share
                )
            }
            .sortedByDescending { it.earnings }
    }

    // Daily Timeline Breakdown
    val dailyList = remember(activeRows, todayStr, yesterdayStr) {
        activeRows
            .groupBy { it.date }
            .map { (dateKey, dRows) ->
                val dRev = dRows.sumOf { it.earnings }
                val dImp = dRows.sumOf { it.impressions }
                val dClicks = dRows.sumOf { it.clicks }
                val dEcpm = if (dImp > 0) (dRev / dImp) * 1000.0 else 0.0
                DailyBreakdown(
                    rawDate = dateKey,
                    formattedDate = formatDisplayDate(dateKey, todayStr, yesterdayStr),
                    earnings = dRev,
                    impressions = dImp,
                    clicks = dClicks,
                    ecpm = dEcpm
                )
            }
            .sortedByDescending { it.rawDate }
    }

    val maxDailyEarnings = remember(dailyList) {
        dailyList.maxOfOrNull { it.earnings }?.takeIf { it > 0.0 } ?: 1.0
    }

    Scaffold(
        containerColor = SolarTheme.colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SolarTheme.colors.background)
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
        // Top App Bar
        item {
            Surface(
                color = SolarTheme.colors.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SolarTheme.colors.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SolarTheme.colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(app = app, size = 26.dp, showPlatformBadge = false)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${app.platform} • ${app.category} • ${if (app.id.isNotBlank()) app.id.takeLast(10) else "Active"}",
                            fontSize = 11.sp,
                            color = SolarTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Time Horizon Filter Dock (Matches Home Screen Styling & Colors)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                color = SolarTheme.colors.surfaceVariant,
                border = BorderStroke(1.dp, SolarTheme.colors.borderSubtle),
                shadowElevation = if (SolarTheme.colors.isDark) 0.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val pc = SolarTheme.colors.primaryContainer
                    val activeBgGradient = remember(pc) { Brush.horizontalGradient(colors = listOf(pc, pc)) }
                    DetailTimeHorizon.entries.forEach { horizon ->
                        val isSelected = horizon == selectedHorizon
                        val elevation by animateDpAsState(
                            targetValue = if (isSelected) 2.dp else 0.dp,
                            animationSpec = spring(stiffness = 500f)
                        )
                        val activeContentColor = if (SolarTheme.colors.isDark) SolarTheme.colors.onPrimaryContainer else com.solarad.app.theme.AdMobSelectedText

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(
                                    elevation = elevation,
                                    shape = RoundedCornerShape(11.dp),
                                    ambientColor = Color.Transparent,
                                    spotColor = Color.Transparent
                                )
                                .clip(RoundedCornerShape(11.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedHorizon = horizon
                                },
                            shape = RoundedCornerShape(11.dp),
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (isSelected) Modifier.background(activeBgGradient)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    if (horizon == DetailTimeHorizon.TODAY) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) activeContentColor else BotanicalGreen)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    Text(
                                        text = horizon.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) activeContentColor else SolarTheme.colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Scope Peek Sub-Banner (Identical to Home Screen)
            AnimatedContent(
                targetState = selectedHorizon,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) }
            ) { horizon ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (horizon == DetailTimeHorizon.TODAY) BotanicalGreen else SolarAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (horizon) {
                            DetailTimeHorizon.TODAY -> "Live updates • Real-time AdMob sync"
                            DetailTimeHorizon.YESTERDAY -> "Finalized 24h ledger • Yesterday"
                            DetailTimeHorizon.DAYS_7 -> "Rolling 7-day trend"
                            DetailTimeHorizon.DAYS_30 -> "Rolling 30-day view"
                            DetailTimeHorizon.DAYS_90 -> "All Time (90-day archive)"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SolarTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Hero KPI Revenue Card for this app
        item {
            val heroGlow = if (SolarTheme.colors.isDark) {
                SolarAmber.copy(alpha = 0.12f)
            } else {
                SolarAmberGlow.copy(alpha = 0.45f)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    )
                    .border(1.dp, SolarAmber.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
                color = SolarTheme.colors.cardBackground,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(heroGlow)
                        .padding(18.dp)
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
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (selectedHorizon) {
                                    DetailTimeHorizon.TODAY -> "Today's Revenue"
                                    DetailTimeHorizon.YESTERDAY -> "Yesterday's Revenue"
                                    DetailTimeHorizon.DAYS_7 -> "Last 7 Days Revenue"
                                    DetailTimeHorizon.DAYS_30 -> "Last 30 Days Revenue"
                                    DetailTimeHorizon.DAYS_90 -> "All Time (90D) Revenue"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SolarTheme.colors.textSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = SolarTheme.colors.surfaceVariant
                        ) {
                            Text(
                                text = selectedHorizon.badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "$currencySymbol${formatAmount(totalRevenue)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = SolarTheme.colors.textPrimary,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("eCPM", fontSize = 11.sp, color = SolarTheme.colors.textSecondary)
                            Text(
                                text = "$currencySymbol${formatAmount(avgEcpm)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanAzure
                            )
                        }
                        Column {
                            Text("Impressions", fontSize = 11.sp, color = SolarTheme.colors.textSecondary)
                            Text(
                                text = formatCompactNumber(totalImpressions),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textPrimary
                            )
                        }
                        Column {
                            Text("Clicks", fontSize = 11.sp, color = SolarTheme.colors.textSecondary)
                            Text(
                                text = formatCompactNumber(totalClicks),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textPrimary
                            )
                        }
                        Column {
                            Text("CTR", fontSize = 11.sp, color = SolarTheme.colors.textSecondary)
                            Text(
                                text = "${formatAmount(ctr)}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BotanicalGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // SECTION: What Ads (Ad Format Breakdown)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewCarousel,
                        contentDescription = null,
                        tint = SolarAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AD FORMATS BREAKDOWN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarTheme.colors.textSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (formatList.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SolarTheme.colors.cardBackground,
                        border = BorderStroke(1.dp, SolarTheme.colors.borderSubtle)
                    ) {
                        Text(
                            text = "No ad format activity recorded in this time range",
                            fontSize = 12.sp,
                            color = SolarTheme.colors.textSecondary,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = SolarTheme.colors.cardBackground,
                        border = BorderStroke(1.dp, SolarTheme.colors.borderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            formatList.forEach { formatItem ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(SolarAmber.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = formatItem.icon,
                                                    contentDescription = null,
                                                    tint = SolarAmber,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = formatItem.displayName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SolarTheme.colors.textPrimary
                                                )
                                                Text(
                                                    text = "${formatCompactNumber(formatItem.impressions)} imps • ${formatItem.clicks} clicks • $currencySymbol${formatAmount(formatItem.ecpm)} eCPM",
                                                    fontSize = 11.sp,
                                                    color = SolarTheme.colors.textSecondary
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$currencySymbol${formatAmount(formatItem.earnings)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SolarTheme.colors.textPrimary
                                            )
                                            Text(
                                                text = "${(formatItem.sharePercent * 100).toInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SolarAmber
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LinearProgressIndicator(
                                        progress = { formatItem.sharePercent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(999.dp)),
                                        color = SolarAmber,
                                        trackColor = SolarTheme.colors.surfaceVariant,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // SECTION: Which Country (Geographic Breakdown)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = OceanAzure,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GEOGRAPHIC BREAKDOWN (${countryList.size} COUNTRIES)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (countryList.size > 5) {
                        Text(
                            text = if (showAllCountries) "Show Less" else "View Top ${minOf(countryList.size, MAX_EXPANDED_COUNTRIES)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OceanAzure,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showAllCountries = !showAllCountries }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (countryList.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SolarTheme.colors.cardBackground,
                        border = BorderStroke(1.dp, SolarTheme.colors.borderSubtle)
                    ) {
                        Text(
                            text = "No country geographic records found for this period",
                            fontSize = 12.sp,
                            color = SolarTheme.colors.textSecondary,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val displayedCountries = remember(countryList, showAllCountries) {
                        // cap expanded list: Column composes every row eagerly
                        if (showAllCountries) countryList.take(MAX_EXPANDED_COUNTRIES) else countryList.take(5)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = SolarTheme.colors.cardBackground,
                        border = BorderStroke(1.dp, SolarTheme.colors.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            displayedCountries.forEachIndexed { index, country ->
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
                                            text = country.flagEmoji,
                                            fontSize = 20.sp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = country.countryName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SolarTheme.colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "(${country.countryCode})",
                                                    fontSize = 11.sp,
                                                    color = SolarTheme.colors.textSecondary
                                                )
                                            }
                                            Text(
                                                text = "${formatCompactNumber(country.impressions)} imps • ${country.clicks} clicks • $currencySymbol${formatAmount(country.ecpm)} eCPM",
                                                fontSize = 11.sp,
                                                color = SolarTheme.colors.textSecondary
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$currencySymbol${formatAmount(country.earnings)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SolarTheme.colors.textPrimary
                                        )
                                        Text(
                                            text = "${(country.sharePercent * 100).toInt()}% share",
                                            fontSize = 11.sp,
                                            color = BotanicalGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (index < displayedCountries.lastIndex) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(SolarTheme.colors.borderSubtle.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // SECTION: What Time (Daily Performance Timeline)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = BotanicalGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DAILY PERFORMANCE TIMELINE (${dailyList.size} DAYS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarTheme.colors.textSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (dailyList.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SolarTheme.colors.cardBackground,
                        border = BorderStroke(1.dp, SolarTheme.colors.borderSubtle)
                    ) {
                        Text(
                            text = "No daily log recorded for this date range",
                            fontSize = 12.sp,
                            color = SolarTheme.colors.textSecondary,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = SolarTheme.colors.cardBackground,
                        border = BorderStroke(1.dp, SolarTheme.colors.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            dailyList.forEachIndexed { index, day ->
                                val barFraction = (day.earnings / maxDailyEarnings).toFloat().coerceIn(0.04f, 1f)

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = day.formattedDate,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SolarTheme.colors.textPrimary
                                            )
                                            Text(
                                                text = "${formatCompactNumber(day.impressions)} imps • ${day.clicks} clicks • $currencySymbol${formatAmount(day.ecpm)} eCPM",
                                                fontSize = 11.sp,
                                                color = SolarTheme.colors.textSecondary
                                            )
                                        }

                                        Text(
                                            text = "$currencySymbol${formatAmount(day.earnings)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SolarTheme.colors.textPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LinearProgressIndicator(
                                        progress = { barFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(999.dp)),
                                        color = BotanicalGreen,
                                        trackColor = SolarTheme.colors.surfaceVariant,
                                        strokeCap = StrokeCap.Round
                                    )
                                }

                                if (index < dailyList.lastIndex) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(SolarTheme.colors.borderSubtle.copy(alpha = 0.4f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
