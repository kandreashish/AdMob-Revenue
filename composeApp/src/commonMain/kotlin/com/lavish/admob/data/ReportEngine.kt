package com.lavish.admob.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.math.roundToLong

/**
 * AdMob-style report builder ("Reports" tab).
 *
 * Works entirely on the cached [AdMobReportRow]s (last 90 days, DATE × APP × FORMAT × COUNTRY),
 * so switching dimensions / ranges / sorting is instant and works offline.
 */

enum class ReportDimension(val label: String) {
    DATE("Date"),
    APP("App"),
    FORMAT("Ad format"),
    COUNTRY("Country")
}

enum class ReportMetric(val label: String, val shortLabel: String) {
    EARNINGS("Est. earnings", "Earnings"),
    REQUESTS("Requests", "Requests"),
    MATCH_RATE("Match rate", "Match rate"),
    IMPRESSIONS("Impressions", "Impr."),
    ECPM("eCPM", "eCPM"),
    CLICKS("Clicks", "Clicks"),
    CTR("Impression CTR", "CTR")
}

enum class ReportDateRange(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 days"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    LAST_30_DAYS("Last 30 days"),
    LAST_90_DAYS("Last 90 days");

    /** Inclusive [start, end]. */
    fun window(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
        TODAY -> today to today
        YESTERDAY -> today.minus(1, DateTimeUnit.DAY).let { it to it }
        LAST_7_DAYS -> today.minus(6, DateTimeUnit.DAY) to today
        THIS_MONTH -> LocalDate(today.year, today.month, 1) to today
        LAST_MONTH -> {
            val end = LocalDate(today.year, today.month, 1).minus(1, DateTimeUnit.DAY)
            LocalDate(end.year, end.month, 1) to end
        }
        LAST_30_DAYS -> today.minus(29, DateTimeUnit.DAY) to today
        LAST_90_DAYS -> today.minus(89, DateTimeUnit.DAY) to today
    }
}

data class ReportTotals(
    val earnings: Double = 0.0,
    val requests: Long = 0L,
    val matchedRequests: Long = 0L,
    val impressions: Long = 0L,
    val clicks: Long = 0L
) {
    val matchRate: Double get() = if (requests > 0) matchedRequests.toDouble() / requests * 100.0 else 0.0
    val ecpm: Double get() = if (impressions > 0) earnings / impressions * 1000.0 else 0.0
    val ctr: Double get() = if (impressions > 0) clicks.toDouble() / impressions * 100.0 else 0.0
    val hasRequestData: Boolean get() = requests > 0

    fun value(metric: ReportMetric): Double = when (metric) {
        ReportMetric.EARNINGS -> earnings
        ReportMetric.REQUESTS -> requests.toDouble()
        ReportMetric.MATCH_RATE -> matchRate
        ReportMetric.IMPRESSIONS -> impressions.toDouble()
        ReportMetric.ECPM -> ecpm
        ReportMetric.CLICKS -> clicks.toDouble()
        ReportMetric.CTR -> ctr
    }

    operator fun plus(r: AdMobReportRow) = copy(
        earnings = earnings + r.earnings,
        requests = requests + r.adRequests,
        matchedRequests = matchedRequests + r.matchedRequests,
        impressions = impressions + r.impressions,
        clicks = clicks + r.clicks
    )
}

data class ReportLine(
    val key: String,
    val label: String,
    val totals: ReportTotals
)

data class ReportQuery(
    val dateRange: ReportDateRange = ReportDateRange.LAST_7_DAYS,
    val dimension: ReportDimension = ReportDimension.DATE,
    val sortMetric: ReportMetric? = null,   // null = sort by dimension (date newest first / label A-Z)
    val sortDescending: Boolean = true,
    val appFilter: String? = null,     // appId (or name when id missing)
    val formatFilter: String? = null
)

data class ReportResult(
    val query: ReportQuery,
    val start: LocalDate,
    val end: LocalDate,
    val lines: List<ReportLine>,
    val totals: ReportTotals,
    val previousTotals: ReportTotals
) {
    /** % change vs the previous period of equal length; null if no previous data. */
    fun change(metric: ReportMetric): Double? {
        val prev = previousTotals.value(metric)
        if (prev == 0.0) return null
        return (totals.value(metric) - prev) / prev * 100.0
    }
}

object ReportEngine {

    fun appKey(r: AdMobReportRow): String = r.appId.ifBlank { r.appName }

    fun run(rows: List<AdMobReportRow>, query: ReportQuery, today: LocalDate): ReportResult {
        val (start, end) = query.dateRange.window(today)
        val span = (end.toEpochDays() - start.toEpochDays()).toInt() + 1
        val prevStart = start.minus(span, DateTimeUnit.DAY)
        val prevEnd = start.minus(1, DateTimeUnit.DAY)

        val filtered = rows.filter { r ->
            (query.appFilter == null || appKey(r) == query.appFilter) &&
                (query.formatFilter == null || r.format == query.formatFilter)
        }
        // Build the key ranges once (previously re-formatted two date strings per row)
        val curRange = key(start)..key(end)
        val prevRange = key(prevStart)..key(prevEnd)
        val cur = filtered.filter { it.date in curRange }
        val prev = filtered.filter { it.date in prevRange }

        val grouped: Map<String, List<AdMobReportRow>> = when (query.dimension) {
            ReportDimension.DATE -> cur.groupBy { it.date }
            ReportDimension.APP -> cur.groupBy { appKey(it) }
            ReportDimension.FORMAT -> cur.groupBy { it.format }
            ReportDimension.COUNTRY -> cur.groupBy { it.country.ifBlank { "Unknown" } }
        }

        var lines = grouped.map { (k, list) ->
            ReportLine(
                key = k,
                label = when (query.dimension) {
                    ReportDimension.DATE -> prettyDate(k)
                    ReportDimension.APP -> list.first().appName.ifBlank { k }
                    ReportDimension.FORMAT -> prettyFormat(k)
                    ReportDimension.COUNTRY -> k
                },
                totals = list.fold(ReportTotals()) { acc, r -> acc + r }
            )
        }

        // Date reports show every day in range (AdMob shows zero days too)
        if (query.dimension == ReportDimension.DATE) {
            val existing = lines.associateBy { it.key }
            lines = (0 until span).map { i ->
                val d = key(start.plus(i))
                existing[d] ?: ReportLine(d, prettyDate(d), ReportTotals())
            }
        }

        val metric = query.sortMetric
        val comparator: Comparator<ReportLine> = if (metric == null) {
            if (query.dimension == ReportDimension.DATE) compareBy { it.key } else compareBy { it.label.lowercase() }
        } else {
            compareBy<ReportLine> { it.totals.value(metric) }.thenBy { it.key }
        }
        val sorted = if (query.sortDescending) lines.sortedWith(comparator.reversed()) else lines.sortedWith(comparator)

        return ReportResult(
            query = query,
            start = start,
            end = end,
            lines = sorted,
            totals = cur.fold(ReportTotals()) { acc, r -> acc + r },
            previousTotals = prev.fold(ReportTotals()) { acc, r -> acc + r }
        )
    }

    /** Date-ordered series for the chart (always chronological, independent of table sort). */
    fun chartSeries(result: ReportResult, metric: ReportMetric): List<Pair<String, Double>> {
        val lines = if (result.query.dimension == ReportDimension.DATE) {
            result.lines.sortedBy { it.key }
        } else {
            result.lines.sortedByDescending { it.totals.value(metric) }.take(8)
        }
        return lines.map { it.label to it.totals.value(metric) }
    }

    fun toCsv(result: ReportResult, currencyCode: String): String {
        val header = listOf(result.query.dimension.label) + ReportMetric.entries.map {
            if (it == ReportMetric.EARNINGS || it == ReportMetric.ECPM) "${it.label} ($currencyCode)" else it.label
        }
        fun row(label: String, t: ReportTotals) = (listOf(label) + ReportMetric.entries.map { formatPlain(it, t.value(it)) })
            .joinToString(",") { csvEscape(it) }
        return buildString {
            appendLine(header.joinToString(",") { csvEscape(it) })
            result.lines.forEach { appendLine(row(it.label, it.totals)) }
            appendLine(row("Total", result.totals))
        }
    }

    fun formatPlain(metric: ReportMetric, v: Double): String = when (metric) {
        ReportMetric.EARNINGS, ReportMetric.ECPM -> round2(v).toString()
        ReportMetric.MATCH_RATE, ReportMetric.CTR -> round2(v).toString()
        else -> v.roundToLong().toString()
    }

    private fun csvEscape(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    private fun round2(v: Double) = (v * 100.0).roundToLong() / 100.0

    private fun key(d: LocalDate) = AdMobApiClient.formatDateKey(d)

    private fun LocalDate.plus(days: Int): LocalDate = this.minus(-days, DateTimeUnit.DAY)

    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun prettyDate(raw: String): String {
        if (raw.length != 8) return raw
        val m = raw.substring(4, 6).toIntOrNull() ?: return raw
        val d = raw.substring(6, 8).toIntOrNull() ?: return raw
        return "${months.getOrElse(m - 1) { "?" }} $d, ${raw.substring(0, 4)}"
    }

    fun prettyFormat(raw: String): String =
        raw.lowercase().split('_', ' ').filter { it.isNotBlank() }
            .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
            .ifBlank { raw }
}
