package com.lavish.admob.ui.utils

import kotlin.math.roundToLong

/**
 * Common formatting utilities for consistent currency and numerical display across all screens.
 */
fun formatAmount(amount: Double): String {
    val rounded = (amount * 100.0).roundToLong() / 100.0
    val parts = rounded.toString().split(".")
    val integerPart = parts[0].reversed().chunked(3).joinToString(",").reversed()
    val decimalPart = if (parts.size > 1) parts[1].padEnd(2, '0').take(2) else "00"
    return "$integerPart.$decimalPart"
}

fun formatCompactNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> "${((number / 100_000.0).roundToLong()) / 10.0}M"
        number >= 1_000 -> "${((number / 100.0).roundToLong()) / 10.0}K"
        else -> number.toString()
    }
}
