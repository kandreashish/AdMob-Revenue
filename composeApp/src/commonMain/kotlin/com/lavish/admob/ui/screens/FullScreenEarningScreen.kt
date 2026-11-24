package com.lavish.admob.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavish.admob.model.AccountSummary
import com.lavish.admob.model.APP_VERSION
import com.lavish.admob.theme.BotanicalGreen
import com.lavish.admob.theme.BotanicalGreenDark
import com.lavish.admob.theme.BotanicalGreenLight
import com.lavish.admob.theme.SolarAmber
import com.lavish.admob.theme.SolarTheme
import com.lavish.admob.ui.components.CelebrationConfetti
import com.lavish.admob.ui.navigation.BackHandler
import com.lavish.admob.ui.utils.PlatformLandscapeEffect
import com.lavish.admob.ui.utils.formatAmount
import com.lavish.admob.ui.utils.formatCompactNumber
import kotlinx.coroutines.delay
import kotlinx.datetime.toLocalDateTime
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun FullScreenEarningScreen(
    summary: AccountSummary,
    onExit: () -> Unit,
    lastSyncMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    // 1. Force landscape orientation & hide system bars for distraction-free mode
    PlatformLandscapeEffect(enabled = true, keepScreenOn = true)

    // Handle back button to exit full screen
    BackHandler(enabled = true) {
        onExit()
    }

    // 2. Track increases to today's earnings to trigger celebration confetti
    var celebrationTrigger by remember { mutableStateOf(0) }
    var previousRevenue by remember { mutableStateOf<Double?>(null) }
    // Bounce plays on EVERY increase; value is read only in graphicsLayer (draw phase), no recomposition
    val bounce = remember { Animatable(1f) }

    LaunchedEffect(summary.todayRevenue) {
        val prev = previousRevenue
        previousRevenue = summary.todayRevenue
        if (prev != null && summary.todayRevenue > prev) {
            celebrationTrigger++
            bounce.animateTo(1.04f, tween(140, easing = FastOutSlowInEasing))
            bounce.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
    }

    // Deep navy HUD palette (matches the Play Store full-screen preview)
    val navyCenter = Color(0xFF16233A)
    val navyEdge = Color(0xFF0B0F17)
    // Burn-in: avoid pure white for the largest static glyphs
    val textPrimaryColor = Color(0xFFE0E0E0)
    val textSecondaryColor = Color(0xFF9AA0A6)
    val statValueColor = Color(0xFFE8EAED)
    val liveGreen = Color(0xFF81C995)
    val downRed = Color(0xFFF28B82)
    val amountGlow = Color(0xFF8AB4F8)

    // Controls visibility: auto-fades for absolute zero-distraction
    var showControls by remember { mutableStateOf(true) }
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000L)
            showControls = false
        }
    }

    // ---- Burn-in protection ----
    // (a) Pixel shift: drift main content & HUD corners to a random offset every 90s (draw phase only)
    val density = LocalDensity.current
    val shiftX = remember { Animatable(0f) }
    val shiftY = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val maxPx = with(density) { BURN_IN_MAX_SHIFT.toPx() }
        while (true) {
            delay(BURN_IN_SHIFT_INTERVAL_MS)
            val tx = (Random.nextFloat() * 2f - 1f) * maxPx
            val ty = (Random.nextFloat() * 2f - 1f) * maxPx
            launch { shiftX.animateTo(tx, tween(4_000, easing = LinearEasing)) }
            shiftY.animateTo(ty, tween(4_000, easing = LinearEasing))
        }
    }

    // (b) Idle / night dimming: dim after inactivity, dimmer at night; tap or revenue bump wakes it
    var lastActivityMs by remember { mutableStateOf(nowMs()) }
    LaunchedEffect(summary.todayRevenue) { lastActivityMs = nowMs() }
    val clockMs by produceState(nowMs()) {
        while (true) { delay(15_000L); value = nowMs() }
    }
    val isIdle = clockMs - lastActivityMs >= IDLE_DIM_AFTER_MS
    val targetBrightness = when {
        !isIdle -> 1f
        isNightHour(clockMs) -> NIGHT_DIM_BRIGHTNESS
        else -> IDLE_DIM_BRIGHTNESS
    }
    val brightness by animateFloatAsState(targetBrightness, tween(2_000), label = "burnInDim")

    // (c) Hourly refresh: briefly fade to black to "exercise" pixels evenly
    val refresh = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(REFRESH_INTERVAL_MS)
            refresh.animateTo(1f, tween(1_500))
            delay(3_000L)
            refresh.animateTo(0f, tween(1_500))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(navyCenter, navyEdge),
                    radius = 1400f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // While the "Tap screen to exit" hint is visible, a tap exits.
                // Once it has faded, the first tap just brings the controls back.
                // If dimmed, the first tap only wakes the screen.
                val wasIdle = isIdle
                lastActivityMs = nowMs()
                if (wasIdle) return@clickable
                if (showControls) onExit() else showControls = true
            }
    ) {
        // Center: Distraction-free earnings display
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .graphicsLayer { translationX = shiftX.value; translationY = shiftY.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ● LIVE · TODAY
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(color = Color(0xFFEA4335)) // red "recording" style live dot
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "LIVE · TODAY",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFFF28B82).copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Big amount with a soft blue glow; bounces on every increase (draw-phase only)
            Text(
                text = "${summary.currencySymbol}${formatAmount(summary.todayRevenue)}",
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimaryColor,
                letterSpacing = (-2).sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = amountGlow.copy(alpha = 0.35f),
                        blurRadius = 48f
                    )
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                }
            )

            // ▲ $132.40 vs yesterday
            val delta = summary.todayRevenue - summary.yesterdayRevenue
            val isUp = delta >= 0
            Text(
                text = "${if (isUp) "▲" else "▼"} ${summary.currencySymbol}${formatAmount(kotlin.math.abs(delta))} vs yesterday",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isUp) liveGreen else downRed
            )

            Spacer(modifier = Modifier.height(22.dp))

            // eCPM · Impressions · Goal
            val goalPct = if (summary.dailyGoal > 0.0) (summary.todayRevenue / summary.dailyGoal * 100).toInt() else 0
            Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                HudStat("eCPM", "${summary.currencySymbol}${formatAmount(summary.ecpm)}", textSecondaryColor, statValueColor)
                HudStat("Impressions", formatCompactNumber(summary.impressions), textSecondaryColor, statValueColor)
                HudStat("Goal", "$goalPct%", textSecondaryColor, statValueColor)
            }
        }

        // Top-Right Exit Button (Fade in/out for zero distraction)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(400)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onExit() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Full Screen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Top-Left: App version (very subtle, with burn-in drift)
        AnimatedVisibility(
            visible = !showControls,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .graphicsLayer { translationX = shiftX.value * 0.4f; translationY = shiftY.value * 0.4f }
        ) {
            Text(
                text = APP_VERSION,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                color = textSecondaryColor.copy(alpha = 0.45f)
            )
        }

        // Top-Left Mini Pill indicating Full Screen
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(400)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable { onExit() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = null,
                        tint = textSecondaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tap screen to exit",
                        fontSize = 11.sp,
                        color = textSecondaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom-Left: current time (always visible, subtle)
        HudClock(
            color = textSecondaryColor,
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp, vertical = 18.dp)
                .graphicsLayer { translationX = shiftX.value * 0.4f; translationY = shiftY.value * 0.4f }
        )

        // Bottom-Right: last sync
        HudLastSync(
            lastSyncMs = lastSyncMs,
            color = textSecondaryColor,
            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 24.dp, vertical = 18.dp)
                .graphicsLayer { translationX = shiftX.value * 0.4f; translationY = shiftY.value * 0.4f }
        )

        // Celebration Confetti Layer (Bursts on revenue changes)
        CelebrationConfetti(
            triggerKey = celebrationTrigger.takeIf { it > 0 },
            particleCount = 140
        )

        // Burn-in dim / refresh overlay (no click handler, so taps pass through to the root)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = maxOf(1f - brightness, refresh.value).coerceIn(0f, 1f)
                }
                .background(Color.Black)
        )
    }
}


private fun nowMs() = kotlin.time.Clock.System.now().toEpochMilliseconds()

// Burn-in protection tuning
private val BURN_IN_MAX_SHIFT = 16.dp
private const val BURN_IN_SHIFT_INTERVAL_MS = 90_000L
private const val IDLE_DIM_AFTER_MS = 10 * 60_000L
private const val IDLE_DIM_BRIGHTNESS = 0.55f
private const val NIGHT_DIM_BRIGHTNESS = 0.3f
private const val NIGHT_START_HOUR = 23
private const val NIGHT_END_HOUR = 7
private const val REFRESH_INTERVAL_MS = 60 * 60_000L

private fun isNightHour(ms: Long): Boolean {
    val hour = kotlin.time.Instant.fromEpochMilliseconds(ms)
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).hour
    return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR
}

private fun formatClock(ms: Long): String {
    val t = kotlin.time.Instant.fromEpochMilliseconds(ms)
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val h12 = if (t.hour % 12 == 0) 12 else t.hour % 12
    val ampm = if (t.hour < 12) "AM" else "PM"
    return "$h12:${t.minute.toString().padStart(2, '0')} $ampm"
}

/** Ticks every second, isolated so only this text recomposes. */
@Composable
private fun HudClock(color: Color, modifier: Modifier = Modifier) {
    val now by produceState(nowMs()) {
        while (true) { delay(1000L - (nowMs() % 1000L)); value = nowMs() }
    }
    Text(
        text = formatClock(now),
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun HudLastSync(lastSyncMs: Long, color: Color, modifier: Modifier = Modifier) {
    val now by produceState(nowMs()) {
        while (true) { delay(15_000L); value = nowMs() }
    }
    val text = if (lastSyncMs <= 0L) "Not synced yet" else {
        val mins = ((now - lastSyncMs).coerceAtLeast(0L) / 60_000L).toInt()
        val ago = when {
            mins < 1 -> "just now"
            mins < 60 -> "$mins min ago"
            else -> "${mins / 60}h ${mins % 60}m ago"
        }
        "Synced $ago · ${formatClock(lastSyncMs)}"
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun HudStat(label: String, value: String, labelColor: Color, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Normal, color = labelColor)
        Text(text = value, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

/** Isolated so its infinite animation only invalidates this layer's draw, not the whole HUD. */
@Composable
private fun LiveDot(color: Color) {
    val t = rememberInfiniteTransition(label = "liveDot")
    val alpha = t.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )
    Box(
        modifier = Modifier
            .size(12.dp)
            .graphicsLayer { this.alpha = alpha.value }
            .background(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun PulsingArrow(tint: Color) {
    val t = rememberInfiniteTransition(label = "arrow")
    val offset = t.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowOffset"
    )
    Icon(
        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
        contentDescription = "Earnings Going Up",
        tint = tint,
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                val sc = 1f + (offset.value / 60f)
                scaleX = sc
                scaleY = sc
            }
    )
}
