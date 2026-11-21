package com.solarad.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Today's date that updates itself when the day rolls over, so screens left open past
 * midnight don't keep showing yesterday's "Today". Wakes at most once per 15 min
 * (cheap, and also catches manual clock / time-zone changes).
 */
@Composable
internal fun rememberCurrentDate(): State<LocalDate> {
    fun today() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return produceState(initialValue = today()) {
        while (true) {
            delay(15 * 60 * 1000L)
            val d = today()
            if (d != value) value = d
        }
    }
}
