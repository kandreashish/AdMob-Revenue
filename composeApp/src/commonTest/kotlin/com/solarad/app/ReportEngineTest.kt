package com.solarad.app

import com.solarad.app.data.AdMobApiClient
import com.solarad.app.data.AdMobReportRow
import com.solarad.app.data.ReportDateRange
import com.solarad.app.data.ReportDimension
import com.solarad.app.data.ReportEngine
import com.solarad.app.data.ReportMetric
import com.solarad.app.data.ReportQuery
import com.solarad.app.model.TimeRange
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReportEngineTest {
    private val today = LocalDate(2026, 11, 21)

    private fun row(date: String, app: String, format: String, country: String, earn: Double, imp: Int, clicks: Int = 0, req: Long = 0, matched: Long = 0) =
        AdMobReportRow(date, app, "$app name", format, country, earn, imp, clicks, req, matched)

    private val rows = listOf(
        row("20261121", "a1", "BANNER", "US", 5.0, 1000, 10, 2000, 1800),
        row("20261120", "a1", "INTERSTITIAL", "IN", 3.0, 500, 5, 600, 600),
        row("20261120", "a2", "BANNER", "US", 2.0, 400, 4, 500, 400),
        row("20261101", "a2", "REWARDED", "DE", 10.0, 100, 1),
        row("20261031", "a1", "BANNER", "US", 7.0, 700, 7), // last month
        row("20261021", "a1", "BANNER", "US", 4.0, 400, 4)  // previous MTD window
    )

    @Test
    fun thisMonthSummaryIsMonthToDateAndComparesSameDaysLastMonth() {
        val s = AdMobApiClient.computeAccountSummary(rows, TimeRange.THIS_MONTH, "pub-1", today = today)
        assertEquals(20.0, s.todayRevenue)       // 5 + 3 + 2 + 10
        assertEquals(4.0, s.yesterdayRevenue)    // Oct 1..21 → only 20261021
        assertEquals(30, s.periodDays)           // November
    }

    @Test
    fun reportByAppSortsByEarningsAndTotals() {
        val r = ReportEngine.run(rows, ReportQuery(ReportDateRange.THIS_MONTH, ReportDimension.APP, ReportMetric.EARNINGS), today)
        assertEquals(listOf("a2", "a1"), r.lines.map { it.key })
        assertEquals(20.0, r.totals.earnings)
        assertEquals(2000L, r.totals.impressions)
    }

    @Test
    fun dateReportFillsEmptyDaysAndSortsNewestFirst() {
        val r = ReportEngine.run(rows, ReportQuery(ReportDateRange.LAST_7_DAYS, ReportDimension.DATE), today)
        assertEquals(7, r.lines.size)
        assertEquals("20261121", r.lines.first().key)
        assertEquals("20261115", r.lines.last().key)
    }

    @Test
    fun matchRateAndFilters() {
        val r = ReportEngine.run(rows, ReportQuery(ReportDateRange.LAST_7_DAYS, ReportDimension.FORMAT, formatFilter = "BANNER"), today)
        assertEquals(2200L, r.totals.matchedRequests)
        assertTrue(kotlin.math.abs(r.totals.matchRate - 88.0) < 0.01)
    }

    @Test
    fun lastMonthWindow() {
        val (s, e) = ReportDateRange.LAST_MONTH.window(today)
        assertEquals(LocalDate(2026, 10, 1), s)
        assertEquals(LocalDate(2026, 10, 31), e)
    }

    @Test
    fun changeComparesWithPreviousPeriod() {
        // Today = 5.0, previous period (yesterday) = 3.0 + 2.0 = 5.0 → 0%
        val r = ReportEngine.run(rows, ReportQuery(ReportDateRange.TODAY, ReportDimension.DATE), today)
        assertEquals(0.0, r.change(ReportMetric.EARNINGS))
    }

    @Test
    fun csvHasHeaderRowsAndTotal() {
        val r = ReportEngine.run(rows, ReportQuery(ReportDateRange.THIS_MONTH, ReportDimension.COUNTRY, ReportMetric.EARNINGS), today)
        val lines = ReportEngine.toCsv(r, "USD").trim().lines()
        assertTrue(lines.first().startsWith("Country,Est. earnings (USD)"))
        assertTrue(lines.last().startsWith("Total,20.0"))
        assertEquals(r.lines.size + 2, lines.size)
        assertNull(ReportEngine.run(emptyList(), ReportQuery(), today).change(ReportMetric.EARNINGS))
    }
}
