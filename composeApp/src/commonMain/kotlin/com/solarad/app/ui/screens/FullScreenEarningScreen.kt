package com.solarad.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import com.solarad.app.model.AccountSummary
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.BotanicalGreenDark
import com.solarad.app.theme.BotanicalGreenLight
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarTheme
import com.solarad.app.ui.components.CelebrationConfetti
import com.solarad.app.ui.navigation.BackHandler
import com.solarad.app.ui.utils.PlatformLandscapeEffect
import com.solarad.app.ui.utils.formatAmount
import kotlinx.coroutines.delay

@Composable
fun FullScreenEarningScreen(
    summary: AccountSummary,
    onExit: () -> Unit,
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

    LaunchedEffect(summary.todayRevenue) {
        val prev = previousRevenue
        if (prev != null && summary.todayRevenue > prev) {
            celebrationTrigger++
        }
        previousRevenue = summary.todayRevenue
    }

    // Subtle scale bounce when number updates or confetti fires
    val scaleAnim by animateFloatAsState(
        targetValue = if (celebrationTrigger > 0) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        finishedListener = {
            // return to neutral
        }
    )

    // Gentle upward pulse for the green arrow
    val infiniteTransition = rememberInfiniteTransition()
    val arrowFloatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Distraction-free deep OLED background styling
    val isDark = SolarTheme.colors.isDark
    val bgColor = if (isDark) Color(0xFF0A0908) else Color(0xFF0F172A)
    val textPrimaryColor = Color(0xFFF8FAFC)
    val textSecondaryColor = Color(0xFF94A3B8)
    val neonGreen = Color(0xFF10B981)
    val neonGreenGlow = Color(0xFF34D399)

    // Controls visibility: auto-fades for absolute zero-distraction
    var showControls by remember { mutableStateOf(true) }
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000L)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tapping only toggles exit button / controls visibility
                showControls = !showControls
            }
    ) {
        // Ambient soft radial gradient behind the big digits
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(450.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            neonGreen.copy(alpha = 0.12f),
                            SolarAmber.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Center: Distraction-free earnings display
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Live Status Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Pulsing Live Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(neonGreen.copy(alpha = liveDotAlpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TODAY'S EARNINGS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = textSecondaryColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Massive Bold Numbers + Upward Green Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.scale(scaleAnim)
            ) {
                // Green Arrow Going Up
                Surface(
                    color = neonGreen.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(end = 18.dp)
                        .size(64.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Earnings Going Up",
                            tint = neonGreenGlow,
                            modifier = Modifier
                                .size(40.dp)
                                .scale(1f + (arrowFloatOffset / 60f))
                        )
                    }
                }

                // Big Letters Earnings Display
                val formattedAmount = formatAmount(summary.todayRevenue)
                Text(
                    text = "${summary.currencySymbol}$formattedAmount",
                    fontSize = 82.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimaryColor,
                    letterSpacing = (-0.03).sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            }

            // Subtle Hint for Live Auto-Sync
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (summary.lastSyncTime.isNotBlank() && !summary.lastSyncTime.equals("Awaiting Sync", ignoreCase = true)) {
                    "Live AdMob Sync • ${summary.lastSyncTime}"
                } else {
                    "Live AdMob Performance • Real-time Monitoring"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondaryColor.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
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
                shape = RoundedCornerShape(999.dp)
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

        // Celebration Confetti Layer (Bursts on revenue changes)
        CelebrationConfetti(
            triggerKey = celebrationTrigger.takeIf { it > 0 },
            particleCount = 140
        )
    }
}
