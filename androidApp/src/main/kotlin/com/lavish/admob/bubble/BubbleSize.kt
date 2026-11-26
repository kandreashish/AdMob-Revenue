package com.lavish.admob.bubble

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Bubble size as a diameter in dp. Text scales proportionally (64dp = 100%). */
object BubbleSizePref {
    const val MIN_DP = 36
    const val MAX_DP = 96
    const val DEFAULT_DP = 64

    private const val PREFS = "earnings_bubble"
    private const val KEY = "bubble_size_dp"

    private val _diameterDp = MutableStateFlow(DEFAULT_DP)
    /** Observed live by the bubble service. */
    val diameterDp: StateFlow<Int> = _diameterDp.asStateFlow()
    private var loaded = false

    /**
     * Non-null while the user is dragging the size slider. The running bubble shows itself
     * (even over AdDash) and resizes live to this value, so the user sees the real thing.
     */
    val previewDp = MutableStateFlow<Int?>(null)

    fun scaleFor(dp: Int): Float = dp / DEFAULT_DP.toFloat()

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        _diameterDp.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY, DEFAULT_DP).coerceIn(MIN_DP, MAX_DP)
    }

    fun set(context: Context, dp: Int) {
        val v = dp.coerceIn(MIN_DP, MAX_DP)
        _diameterDp.value = v
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, v).apply()
    }
}

/** Bubble idle opacity as a percentage (20%..100%, default 60%). */
object BubbleAlphaPref {
    const val MIN_PERCENT = 20
    const val MAX_PERCENT = 100
    const val DEFAULT_PERCENT = 60

    private const val PREFS = "earnings_bubble"
    private const val KEY = "bubble_alpha_pct"

    private val _alphaPercent = MutableStateFlow(DEFAULT_PERCENT)
    val alphaPercent: StateFlow<Int> = _alphaPercent.asStateFlow()
    private var loaded = false

    val previewPercent = MutableStateFlow<Int?>(null)

    fun toFloat(percent: Int): Float = (percent.coerceIn(MIN_PERCENT, MAX_PERCENT)) / 100f

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        _alphaPercent.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY, DEFAULT_PERCENT).coerceIn(MIN_PERCENT, MAX_PERCENT)
    }

    fun set(context: Context, percent: Int) {
        val v = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)
        _alphaPercent.value = v
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, v).apply()
    }
}
