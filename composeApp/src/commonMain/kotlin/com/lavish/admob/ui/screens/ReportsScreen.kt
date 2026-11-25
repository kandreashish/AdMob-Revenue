package com.lavish.admob.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavish.admob.data.AdMobReportRow
import com.lavish.admob.data.ReportDateRange
import com.lavish.admob.data.ReportDimension
import com.lavish.admob.data.ReportEngine
import com.lavish.admob.data.ReportMetric
import com.lavish.admob.data.ReportQuery
import com.lavish.admob.data.ReportResult
import com.lavish.admob.data.ReportTotals
import com.lavish.admob.theme.BotanicalGreen
import com.lavish.admob.theme.CoralCrimson
import com.lavish.admob.theme.SolarAmber
import com.lavish.admob.theme.SolarTheme
import com.lavish.admob.model.AppMonetization
import com.lavish.admob.data.ReportLine
import com.lavish.admob.ui.components.AppIcon
import com.lavish.admob.ui.components.ShimmerTable
import com.lavish.admob.ui.components.SolarHeader
import com.lavish.admob.ui.utils.formatAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * AdMob-console-style Reports: pick a date range + dimension, filter by app / ad format,
 * see scorecards with period-over-period change, a chart, and a sortable table you can export.
 */
@Composable
fun ReportsScreen(
    rows: List<AdMobReportRow>,
    today: LocalDate,
    apps: List<AppMonetization> = emptyList(),
    currencySymbol: String,
    currencyCode: String,
    accountName: String = "My AdMob",
    avatarUrl: String? = null,
    isOffline: Boolean = false,
    onProfileClick: () -> Unit,
    onReportChanged: (String, String) -> Unit = { _, _ -> },
    onExported: (Int) -> Unit = {}
) {
    var rangeName by rememberSaveable { mutableStateOf(ReportDateRange.LAST_7_DAYS.name) }
    // Selected dimensions in the order they were picked (comma-joined so it survives config changes)
    var dimensionNames by rememberSaveable { mutableStateOf(ReportDimension.DATE.name) }
    var sortMetricName by rememberSaveable { mutableStateOf<String?>(null) }
    var sortDescending by rememberSaveable { mutableStateOf(true) }
    var chartMetricName by rememberSaveable { mutableStateOf(ReportMetric.EARNINGS.name) }
    var appFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var formatFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var showCtrInfo by remember { mutableStateOf(false) }

    if (showCtrInfo) CtrWarningDialog(onDismiss = { showCtrInfo = false })

    val range = ReportDateRange.valueOf(rangeName)
    val dimensions = dimensionNames.split(',').filter { it.isNotBlank() }.map { ReportDimension.valueOf(it) }
        .ifEmpty { listOf(ReportDimension.DATE) }
    val dimension = dimensions.first()
    val dateOnly = dimensions == listOf(ReportDimension.DATE)
    val chartMetric = ReportMetric.valueOf(chartMetricName)
    val query = ReportQuery(
        dateRange = range,
        dimension = dimension,
        sortMetric = sortMetricName?.let { ReportMetric.valueOf(it) },
        sortDescending = sortDescending,
        appFilter = appFilter,
        formatFilter = formatFilter,
        extraDimensions = dimensions.drop(1)
    )
    // First result is computed once synchronously so the screen never flashes empty;
    // every later change (filters, sort, new sync) is computed on Dispatchers.Default while the
    // previous result stays on screen.
    var result by remember { mutableStateOf(ReportEngine.run(emptyList(), query, today)) } // cheap placeholder
    var ready by remember { mutableStateOf(false) } // false until the first real report is built
    LaunchedEffect(rows, query, today) {
        val fresh = withContext(Dispatchers.Default) { ReportEngine.run(rows, query, today) }
        result = fresh
        ready = true
    }

    val appOptions = remember(rows) {
        rows.groupBy { ReportEngine.appKey(it) }
            .map { (k, list) -> k to list.first().appName.ifBlank { k } }
            .sortedBy { it.second.lowercase() }
    }
    val formatOptions = remember(rows) { rows.map { it.format }.distinct().sorted() }

    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(copied) {
        if (copied) { delay(2000); copied = false }
    }

    // Derived values are cached per result so scrolling/recomposition doesn't redo them
    val chartSeries = remember(result, chartMetric) { ReportEngine.chartSeries(result, chartMetric) }
    val chartLines = remember(result, chartMetric) { ReportEngine.chartLines(result, chartMetric) }
    val chartAppIndex = dimensions.indexOf(ReportDimension.APP)
    val chartAppLookup = remember(apps) { apps.associateBy { it.id } + apps.associateBy { it.name } }
    val highCtrRows = remember(result) { result.lines.count { isHighCtr(it.totals) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SolarTheme.colors.background),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            SolarHeader(
                title = "Reports",
                subtitle = "${shortDate(result.start)} – ${shortDate(result.end)}" + if (isOffline) " • Cached" else "",
                accountName = accountName,
                avatarUrl = avatarUrl,
                isOffline = isOffline,
                onAccountClick = onProfileClick
            )
        }

        // Date range chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportDateRange.entries.forEach { r ->
                    ReportChip(label = r.label, selected = r == range) {
                        rangeName = r.name
                        onReportChanged("date_range", r.name)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // Dimensions (multi-select, like AdMob: e.g. App + Country + Ad unit)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Break down by", fontSize = 12.sp, color = SolarTheme.colors.textSecondary)
                ReportDimension.entries.forEach { d ->
                    val idx = dimensions.indexOf(d)
                    ReportChip(
                        label = if (idx >= 0 && dimensions.size > 1) "${idx + 1}. ${d.label}" else d.label,
                        selected = idx >= 0
                    ) {
                        val next = if (idx >= 0) dimensions - d else dimensions + d
                        if (next.isNotEmpty()) {
                            dimensionNames = next.joinToString(",") { it.name }
                            // AdMob defaults: date-only newest-first, everything else by earnings
                            sortMetricName = if (next == listOf(ReportDimension.DATE)) null else ReportMetric.EARNINGS.name
                            sortDescending = true
                            onReportChanged("dimension", dimensionNames)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // Filters + export
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterDropdown(
                    title = "App",
                    current = appOptions.firstOrNull { it.first == appFilter }?.second ?: "All apps",
                    options = listOf<Pair<String?, String>>(null to "All apps") + appOptions,
                    onSelect = { appFilter = it },
                    modifier = Modifier.weight(1f)
                )
                FilterDropdown(
                    title = "Format",
                    current = formatFilter?.let { ReportEngine.prettyFormat(it) } ?: "All formats",
                    options = listOf<Pair<String?, String>>(null to "All formats") + formatOptions.map { it to ReportEngine.prettyFormat(it) },
                    onSelect = { formatFilter = it },
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SolarTheme.colors.surface,
                    modifier = Modifier
                        .border(1.dp, SolarTheme.colors.borderMedium, RoundedCornerShape(8.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(ReportEngine.toCsv(result, currencyCode)))
                            copied = true
                            onExported(result.lines.size)
                        }
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy CSV", tint = SolarAmber, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (copied) "Copied" else "CSV", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SolarAmber)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Scorecards (tap to chart a metric) — mirrors AdMob's report scorecard row
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportMetric.entries.forEach { m ->
                    Scorecard(
                        metric = m,
                        value = formatMetric(m, result.totals.value(m), currencySymbol, requestData = result.totals.hasRequestData),
                        change = result.change(m),
                        warn = m == ReportMetric.CTR && isHighCtr(result.totals),
                        selected = m == chartMetric,
                        onClick = { chartMetricName = m.name }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Chart (skipped when there's only one data point — the scorecard already shows it)
        if (ready && chartSeries.size > 1) item {
            Card {
                Text(
                    "${chartMetric.label} by " + dimensions.joinToString(" · ") { it.label.lowercase() },
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SolarTheme.colors.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                val series = chartSeries
                val seriesEmpty = remember(series) { series.all { it.second == 0.0 } }
                val barValues = remember(series) { series.map { it.second } }
                if (seriesEmpty) {
                    Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Text("No data for this period", fontSize = 13.sp, color = SolarTheme.colors.textSecondary)
                    }
                } else if (dateOnly) {
                    val barLabels = remember(series, currencySymbol) {
                        series.map { formatCompactMetric(chartMetric, it.second, currencySymbol) }
                    }
                    BarChart(barValues, barLabels, Modifier.fillMaxWidth().height(160.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(series.first().first.substringBeforeLast(","), fontSize = 10.sp, color = SolarTheme.colors.textSecondary)
                        Text(series.last().first.substringBeforeLast(","), fontSize = 10.sp, color = SolarTheme.colors.textSecondary)
                    }
                } else {
                    val max = remember(series) { series.maxOf { it.second }.takeIf { it > 0 } ?: 1.0 }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        series.forEachIndexed { idx, (label, v) ->
                            val line = chartLines.getOrNull(idx)
                            // App rows: small icon instead of the (long) app name
                            val app = if (chartAppIndex >= 0 && line != null) {
                                val key = line.key.split('\u0001').getOrNull(chartAppIndex).orEmpty()
                                val name = line.labels.getOrElse(chartAppIndex) { key }
                                chartAppLookup[key] ?: chartAppLookup[name] ?: AppMonetization(
                                    id = key, name = name, category = "", platform = "", todayRevenue = 0.0,
                                    deltaVsYesterday = 0.0, totalImpressions = 0, fillRate = 0.0, avgEcpm = 0.0
                                )
                            } else null
                            val text = if (app != null && line != null)
                                line.labels.filterIndexed { i, _ -> i != chartAppIndex }.joinToString(" · ")
                            else label
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    if (app != null) {
                                        AppIcon(app = app, size = 22.dp, showPlatformBadge = true)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(text, fontSize = 12.sp, color = SolarTheme.colors.textPrimary, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text(formatMetric(chartMetric, v, currencySymbol, true), fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium, color = SolarTheme.colors.textPrimary)
                                }
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.fillMaxWidth().height(6.dp).background(SolarTheme.colors.surfaceVariant, RoundedCornerShape(3.dp))) {
                                    Box(Modifier.fillMaxWidth((v / max).toFloat().coerceIn(0f, 1f)).height(6.dp)
                                        .background(SolarAmber, RoundedCornerShape(3.dp)))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Subtle high-CTR notice (tap for details)
        val totalHighCtr = isHighCtr(result.totals)
        if (highCtrRows > 0 || totalHighCtr) item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .background(CoralCrimson.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .clickable { showCtrInfo = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = CoralCrimson, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (highCtrRows > 0) "$highCtrRows ${if (highCtrRows == 1) "row has" else "rows have"} CTR above ${CTR_WARN_PCT.toInt()}%. Google may treat this as invalid traffic."
                    else "CTR is above ${CTR_WARN_PCT.toInt()}%. Google may treat this as invalid traffic.",
                    fontSize = 12.sp, color = SolarTheme.colors.textPrimary, modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.Info, contentDescription = "Learn more", tint = SolarTheme.colors.textSecondary, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        // Table
        item {
            Card(padding = 0.dp) {
                if (!ready) {
                    ShimmerTable()
                } else if (result.lines.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No rows. Try a different date range or filter.", fontSize = 13.sp, color = SolarTheme.colors.textSecondary)
                    }
                } else {
                    ReportTable(
                        result = result,
                        apps = apps,
                        currencySymbol = currencySymbol,
                        onSortDimension = {
                            if (query.sortMetric == null) sortDescending = !sortDescending
                            else { sortMetricName = null; sortDescending = dimension == ReportDimension.DATE }
                        },
                        onSortMetric = { m ->
                            if (query.sortMetric == m) sortDescending = !sortDescending
                            else { sortMetricName = m.name; sortDescending = true }
                        }
                    )
                }
            }
        }

        item {
            Text(
                "Based on synced data for the last 90 days (account reporting time zone). " +
                    "Requests and match rate appear after the next sync. Ad unit data fills in after the next full sync. " +
                    "Hourly breakdowns aren't available through the AdMob API.",
                fontSize = 11.sp,
                color = SolarTheme.colors.textMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

private val DIM_COL = 132.dp
/** App column shows only the icon (names took too much room). */
private val APP_COL = 52.dp

private fun dimWidth(d: ReportDimension) = if (d == ReportDimension.APP) APP_COL else DIM_COL
private const val PAGE_SIZE = 50
private val METRIC_COL = 92.dp

@Composable
private fun ReportTable(
    result: ReportResult,
    apps: List<AppMonetization>,
    currencySymbol: String,
    onSortDimension: () -> Unit,
    onSortMetric: (ReportMetric) -> Unit
) {
    // Paginated: never compose thousands of rows at once
    var visibleCount by remember(result.query) { mutableStateOf(PAGE_SIZE) }
    val visible = remember(result.lines, visibleCount) { result.lines.take(visibleCount) }
    val hasReq = result.totals.hasRequestData
    val dims = result.query.dimensions
    val appIndex = dims.indexOf(ReportDimension.APP)
    // Lookup by app id first, then by name (rows cached without an id)
    val appLookup = remember(apps) { apps.associateBy { it.id } + apps.associateBy { it.name } }
    fun appCell(line: ReportLine): AppMonetization? {
        if (appIndex < 0) return null
        val key = line.key.split('\u0001').getOrNull(appIndex) ?: return null
        val name = line.labels.getOrElse(appIndex) { key }
        return appLookup[key] ?: appLookup[name] ?: AppMonetization(
            id = key, name = name, category = "", platform = "", todayRevenue = 0.0,
            deltaVsYesterday = 0.0, totalImpressions = 0, fillRate = 0.0, avgEcpm = 0.0
        )
    }

    Column(Modifier.horizontalScroll(rememberScrollState())) {
        // Header
        Row(
            Modifier.background(SolarTheme.colors.surfaceVariant).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            result.query.dimensions.forEachIndexed { i, d ->
                HeaderCell(d.label, dimWidth(d), TextAlign.Start,
                    sortArrow = if (i == 0 && result.query.sortMetric == null) result.query.sortDescending else null, onClick = onSortDimension)
            }
            ReportMetric.entries.forEach { m ->
                HeaderCell(m.shortLabel, METRIC_COL, TextAlign.End,
                    sortArrow = if (result.query.sortMetric == m) result.query.sortDescending else null) { onSortMetric(m) }
            }
        }
        // Total row (AdMob pins totals at the top)
        val dimCount = result.query.dimensions.size
        TableRow(listOf("Total") + List(dimCount - 1) { "" }, result.totals, currencySymbol, hasReq, bold = true,
            dims = dims, totalRow = true)
        HorizontalDivider(color = SolarTheme.colors.borderMedium)
        visible.forEachIndexed { i, line ->
            TableRow(line.labels, line.totals, currencySymbol, hasReq, dims = dims, app = appCell(line))
            if (i != visible.lastIndex) HorizontalDivider(color = SolarTheme.colors.borderSubtle)
        }
    }
    if (result.lines.size > visible.size) {
        val remaining = result.lines.size - visible.size
        Text(
            "Show ${minOf(PAGE_SIZE, remaining)} more (${visible.size} of ${result.lines.size})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = SolarAmber,
            modifier = Modifier.fillMaxWidth().clickable { visibleCount += PAGE_SIZE }.padding(14.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp, align: TextAlign, sortArrow: Boolean?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.width(width).clickable(onClick = onClick).padding(horizontal = 12.dp),
        horizontalArrangement = if (align == TextAlign.End) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sortArrow != null && align == TextAlign.End) SortIcon(sortArrow)
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = if (sortArrow != null) SolarAmber else SolarTheme.colors.textSecondary, maxLines = 1)
        if (sortArrow != null && align != TextAlign.End) SortIcon(sortArrow)
    }
}

@Composable
private fun SortIcon(desc: Boolean) {
    Icon(
        if (desc) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
        contentDescription = if (desc) "Sorted descending" else "Sorted ascending",
        tint = SolarAmber,
        modifier = Modifier.size(12.dp).padding(horizontal = 1.dp)
    )
}

@Composable
private fun TableRow(
    labels: List<String>, t: ReportTotals, symbol: String, hasReq: Boolean, bold: Boolean = false,
    dims: List<ReportDimension> = emptyList(), app: AppMonetization? = null, totalRow: Boolean = false
) {
    val rowBg = if (isHighCtr(t)) CoralCrimson.copy(alpha = 0.06f) else androidx.compose.ui.graphics.Color.Transparent
    Row(Modifier.background(rowBg).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { i, label ->
            val d = dims.getOrNull(i)
            if (d == ReportDimension.APP && !totalRow) {
                Box(Modifier.width(APP_COL).padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (app != null) AppIcon(app = app, size = 26.dp, showPlatformBadge = true)
                }
                return@forEachIndexed
            }
            // "Total" label may be wider than the icon column; let it overflow into the next blank cell
            val w = if (d != null) dimWidth(d) else DIM_COL
            Text(
            label, modifier = Modifier.width(w).padding(horizontal = 12.dp),
            fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = SolarTheme.colors.textPrimary, maxLines = 1,
            overflow = if (totalRow) TextOverflow.Visible else TextOverflow.Ellipsis, softWrap = false
        ) }
        val highCtr = isHighCtr(t)
        ReportMetric.entries.forEach { m ->
            val flag = highCtr && m == ReportMetric.CTR
            Row(
                Modifier.width(METRIC_COL).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (flag) {
                    Icon(Icons.Default.Warning, contentDescription = "High CTR", tint = CoralCrimson, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                }
                Text(
                    formatMetric(m, t.value(m), symbol, hasReq),
                    fontSize = 13.sp, fontWeight = if (bold || flag) FontWeight.Bold else FontWeight.Normal,
                    color = if (flag) CoralCrimson else SolarTheme.colors.textPrimary, textAlign = TextAlign.End, maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun Scorecard(metric: ReportMetric, value: String, change: Double?, selected: Boolean, warn: Boolean = false, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) SolarTheme.colors.primaryContainer else SolarTheme.colors.cardBackground,
        modifier = Modifier
            .width(128.dp)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) SolarAmber else SolarTheme.colors.borderMedium,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(metric.label, fontSize = 12.sp, color = SolarTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                color = if (warn) CoralCrimson else SolarTheme.colors.textPrimary, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            if (change != null) {
                val up = change >= 0
                Text(
                    "${if (up) "▲" else "▼"} ${formatOne(abs(change))}%",
                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    color = if (up) BotanicalGreen else CoralCrimson
                )
            } else {
                Text("—", fontSize = 11.sp, color = SolarTheme.colors.textMuted)
            }
        }
    }
}

@Composable
private fun ReportChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) SolarTheme.colors.primaryContainer else SolarTheme.colors.surface,
        modifier = Modifier
            .border(1.dp, if (selected) SolarAmber else SolarTheme.colors.borderMedium, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                !selected -> SolarTheme.colors.textPrimary
                SolarTheme.colors.isDark -> SolarTheme.colors.onPrimaryContainer
                else -> SolarAmber
            }
        )
    }
}

@Composable
private fun FilterDropdown(
    title: String,
    current: String,
    options: List<Pair<String?, String>>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SolarTheme.colors.surface,
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, SolarTheme.colors.borderMedium, RoundedCornerShape(8.dp))
                .clickable { open = true }
        ) {
            Row(Modifier.padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 10.sp, color = SolarTheme.colors.textSecondary)
                    Text(current, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SolarTheme.colors.textPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SolarTheme.colors.textSecondary)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(text = { Text(label, fontSize = 13.sp) }, onClick = { onSelect(value); open = false })
            }
        }
    }
}

@Composable
private fun Card(padding: Dp = 16.dp, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .border(1.dp, SolarTheme.colors.borderMedium, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = SolarTheme.colors.cardBackground
    ) {
        Column(Modifier.padding(padding)) { content() }
    }
}

@Composable
private fun BarChart(values: List<Double>, labels: List<String>, modifier: Modifier) {
    val barColor = SolarAmber
    val gridColor = SolarTheme.colors.borderSubtle
    val labelColor = SolarTheme.colors.textSecondary
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
    val sampleW = remember(labels, labelColor) { measurer.measure(labels.maxByOrNull { it.length } ?: "", labelStyle).size.width }
    val measured = remember(labels, labelColor) { labels.map { measurer.measure(it, labelStyle) } }
    Canvas(modifier) {
        val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val n0 = values.size.coerceAtLeast(1)
        val slot0 = size.width / n0
        // Label every bar when they fit; otherwise label every k-th bar plus the peak
        val step = if (sampleW <= 0) 1 else kotlin.math.ceil((sampleW + 4f) / slot0).toInt().coerceAtLeast(1)
        val peak = values.indices.maxByOrNull { values[it] } ?: -1
        val topPad = 14.dp.toPx()
        val chartH = size.height - topPad
        for (i in 0..3) {
            val y = topPad + chartH * i / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val n = values.size.coerceAtLeast(1)
        val slot = size.width / n
        val barW = (slot * 0.7f).coerceAtLeast(1f)
        values.forEachIndexed { i, v ->
            val h = (v / max * chartH).toFloat()
            drawRoundRect(
                color = barColor,
                topLeft = Offset(i * slot + (slot - barW) / 2f, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(minOf(barW / 2f, 6f))
            )
            val showLabel = v > 0 && (i == peak || (i % step == 0 && (peak < 0 || kotlin.math.abs(i - peak) >= step)))
            if (showLabel) {
                val layout = measured.getOrNull(i) ?: return@forEachIndexed
                val x = (i * slot + slot / 2f - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                drawText(layout, topLeft = Offset(x, size.height - h - layout.size.height - 2f))
            }
        }
    }
}

// ---- high CTR warning ----

/** AdMob treats unusually high CTR as a common sign of invalid clicks. */
private const val CTR_WARN_PCT = 20.0
/** Ignore tiny samples (e.g. 1 impression + 1 click = 100%) to avoid noisy warnings. */
private const val CTR_WARN_MIN_IMPRESSIONS = 10L

private fun isHighCtr(t: ReportTotals) = t.impressions >= CTR_WARN_MIN_IMPRESSIONS && t.ctr > CTR_WARN_PCT

@Composable
private fun CtrWarningDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = CoralCrimson) },
        title = { Text("High click-through rate") },
        text = {
            Text(
                "Some rows have a CTR above ${CTR_WARN_PCT.toInt()}%. Most apps see a CTR of a few percent, " +
                    "so Google may treat a rate this high as invalid traffic. That can lead to clicks being " +
                    "deducted, ad serving being limited, or the account being reviewed.\n\n" +
                    "Common causes: ads placed close to buttons, accidental taps, encouraging users to click, " +
                    "or clicking your own ads. Rows highlighted in red are worth checking.",
                fontSize = 14.sp
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

// ---- formatting helpers ----

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun shortDate(d: LocalDate) = "${MONTHS[d.month.ordinal]} ${d.day}"

private fun formatOne(v: Double): String {
    val r = (v * 10).roundToLong()
    return "${r / 10}.${abs(r % 10)}"
}

private fun groupThousands(n: Long): String {
    val s = abs(n).toString().reversed().chunked(3).joinToString(",").reversed()
    return if (n < 0) "-$s" else s
}

/** Short labels for chart bars, e.g. ₹1.2K, 3.4M, 85% */
private fun formatCompactMetric(m: ReportMetric, v: Double, symbol: String): String {
    fun compact(x: Double): String = when {
        abs(x) >= 1_000_000 -> "${formatOne(x / 1_000_000)}M"
        abs(x) >= 1_000 -> "${formatOne(x / 1_000)}K"
        else -> null
    } ?: ""
    return when (m) {
        ReportMetric.EARNINGS, ReportMetric.ECPM -> symbol + (compact(v).ifEmpty { formatAmount(v) })
        ReportMetric.MATCH_RATE, ReportMetric.SHOW_RATE, ReportMetric.CTR -> "${formatOne(v)}%"
        else -> compact(v).ifEmpty { v.roundToLong().toString() }
    }
}

internal fun formatMetric(m: ReportMetric, v: Double, symbol: String, requestData: Boolean): String = when (m) {
    ReportMetric.EARNINGS, ReportMetric.ECPM -> "$symbol${formatAmount(v)}"
    ReportMetric.MATCH_RATE, ReportMetric.SHOW_RATE -> if (requestData) "${formatAmount(v)}%" else "—"
    ReportMetric.REQUESTS, ReportMetric.MATCHED_REQUESTS -> if (requestData) groupThousands(v.roundToLong()) else "—"
    ReportMetric.CTR -> "${formatAmount(v)}%"
    ReportMetric.IMPRESSIONS, ReportMetric.CLICKS -> groupThousands(v.roundToLong())
}
