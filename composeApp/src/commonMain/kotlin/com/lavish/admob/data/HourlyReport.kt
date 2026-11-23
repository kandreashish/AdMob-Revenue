package com.lavish.admob.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One hour of data, already converted to the device's local time. */
data class HourlyPoint(val date: LocalDate, val hour: Int, val earnings: Double, val impressions: Long)

sealed interface HourlyState {
    data object Idle : HourlyState
    data object Loading : HourlyState
    /** AdMob refused the alpha Ads Activity report for this account (or it was retired). */
    data class Unavailable(val reason: String) : HourlyState
    data class Ready(val points: List<HourlyPoint>) : HourlyState
}

internal object HourlyParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    /** AdMob reports HOUR in Pacific time regardless of the account's reporting time zone. */
    private val PACIFIC = TimeZone.of("America/Los_Angeles")

    fun parse(body: String, local: TimeZone = TimeZone.currentSystemDefault()): List<HourlyPoint> {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return emptyList()
        val rows = mutableListOf<JsonObject>()
        collectRows(root, rows)
        return rows.mapNotNull { row ->
            val dims = row["dimensionValues"]?.jsonObject ?: return@mapNotNull null
            val mets = row["metricValues"]?.jsonObject ?: JsonObject(emptyMap())
            val dateStr = scalar(dims["DATE"]) ?: return@mapNotNull null
            val hour = scalar(dims["HOUR"])?.toIntOrNull() ?: return@mapNotNull null
            if (dateStr.length != 8) return@mapNotNull null
            val pstDate = LocalDate(dateStr.substring(0, 4).toInt(), dateStr.substring(4, 6).toInt(), dateStr.substring(6, 8).toInt())
            val instant = LocalDateTime(pstDate, LocalTime(hour, 0)).toInstant(PACIFIC)
            val localDt = instant.toLocalDateTime(local)
            HourlyPoint(
                date = localDt.date,
                hour = localDt.hour,
                earnings = money(mets["ESTIMATED_EARNINGS"]),
                impressions = number(mets["IMPRESSIONS"]).toLong()
            )
        }
            // Same local hour can appear twice around DST changes — merge.
            .groupBy { it.date to it.hour }
            .map { (k, v) -> HourlyPoint(k.first, k.second, v.sumOf { it.earnings }, v.sumOf { it.impressions }) }
    }

    private fun collectRows(e: JsonElement, out: MutableList<JsonObject>) {
        when (e) {
            is JsonArray -> e.forEach { collectRows(it, out) }
            is JsonObject -> if (e.containsKey("dimensionValues")) out += e else e.values.forEach { collectRows(it, out) }
            else -> {}
        }
    }

    /** Dimension may be {"value": "05"} or a bare scalar "05". */
    private fun scalar(e: JsonElement?): String? = when (e) {
        is JsonPrimitive -> e.contentOrNull
        is JsonObject -> (e["value"] as? JsonPrimitive)?.contentOrNull
        else -> null
    }

    private fun number(e: JsonElement?): Double {
        val o = e as? JsonObject ?: return (e as? JsonPrimitive)?.doubleOrNull ?: 0.0
        o["doubleValue"]?.jsonPrimitive?.doubleOrNull?.let { return it }
        o["integerValue"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.let { return it }
        return 0.0
    }

    private fun money(e: JsonElement?): Double {
        val o = e as? JsonObject
        o?.get("microsValue")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.let { return it / 1_000_000.0 }
        return number(e)
    }
}
