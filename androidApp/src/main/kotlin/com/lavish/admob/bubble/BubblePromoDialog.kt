package com.lavish.admob.bubble

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand red (#D93025) for the purchase call-to-action. */
private val UnlockRed = Color(0xFFD93025)

enum class PromoMode {
    /** First-time pitch: trial available. */
    Intro,
    /** Trial used up: only purchase is possible. */
    TrialEnded
}

/**
 * Bottom-sheet promo that explains the Earnings Bubble end to end:
 * what it does, how it behaves, pricing, and the overlay permission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubblePromoDialog(
    mode: PromoMode,
    earningsText: String,
    config: BubbleConfig,
    billingAvailable: Boolean,
    isWorking: Boolean,
    onStartTrial: () -> Unit,
    onBuy: () -> Unit,
    onLater: () -> Unit,
    onNeverShow: () -> Unit,
    promoCodeError: String? = null,
    onRedeemCode: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onLater, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BubblePreview(earningsText)
            Spacer(Modifier.height(20.dp))

            Text(
                text = if (mode == PromoMode.Intro) config.promoTitle else "Your free trial has ended",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (mode == PromoMode.Intro)
                    "See today's AdMob earnings in a floating bubble, on your lockscreen and in home-screen widgets, without opening AdDash."
                else
                    "Keep the bubble, lockscreen earnings and home-screen widgets with a one-time ${config.priceLabel} unlock.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("What you get")
            FeatureRow(Icons.Outlined.Layers, "Floating bubble", "Today's earnings over any app. Drag it anywhere, tap to open AdDash, drag to ✕ to hide.")
            FeatureRow(Icons.Outlined.ScreenLockPortrait, "Lockscreen earnings", "See today's earnings without unlocking your phone.")
            FeatureRow(Icons.Outlined.Widgets, "Home-screen widgets", "Small: today's earnings. Large: vs yesterday, goal progress, impressions, eCPM, clicks.")
            FeatureRow(Icons.Outlined.Refresh, "Updates automatically", "Everything refreshes in the background. Light on battery.")

            Spacer(Modifier.height(16.dp))
            SectionLabel("Pricing")
            PricingCard(mode, config)

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp).offset(y = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    "For the bubble, Android will ask for the \"Display over other apps\" permission. AdDash uses it only to draw the bubble. You can turn any feature off any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
            if (mode == PromoMode.Intro) {
                Button(
                    onClick = onStartTrial,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isWorking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(config.promoCta, fontWeight = FontWeight.SemiBold)
                }
                if (billingAvailable) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onBuy,
                        enabled = !isWorking,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = UnlockRed),
                        border = BorderStroke(1.dp, UnlockRed),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Unlock forever for ${config.priceLabel}")
                    }
                }
            } else {
                Button(
                    onClick = onBuy,
                    colors = ButtonDefaults.buttonColors(containerColor = UnlockRed, contentColor = Color.White),
                    enabled = billingAvailable && !isWorking,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Unlock forever for ${config.priceLabel}", fontWeight = FontWeight.SemiBold) }
            }

            Spacer(Modifier.height(4.dp))
            PromoCodeSection(isWorking = isWorking, error = promoCodeError, onRedeem = onRedeemCode)

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onNeverShow) { Text("Don't show again") }
                TextButton(onClick = onLater) { Text("Maybe later") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** "Have a promo code?" link that opens the premium OTP-style redeem dialog. */
@Composable
private fun PromoCodeSection(isWorking: Boolean, error: String?, onRedeem: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = true }) {
        Icon(Icons.Outlined.CardGiftcard, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Have a promo code?", fontWeight = FontWeight.Medium)
    }
    if (open) {
        PromoCodeDialog(
            isWorking = isWorking,
            error = error,
            onRedeem = onRedeem,
            onDismiss = { open = false }
        )
    }
}

@Composable
private fun PricingCard(mode: PromoMode, config: BubbleConfig) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            if (mode == PromoMode.Intro) {
                PriceLine("Free for ${config.trialLabel}", "No payment needed to start")
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }
            PriceLine("Then ${config.priceLabel}, one time", "Bubble, lockscreen and widgets forever. Not a subscription.")
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            PriceLine("Nothing is charged automatically", "When the trial ends, these features lock until you choose to buy.")
        }
    }
}

@Composable
private fun PriceLine(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Mock phone screen with the bubble floating over fake app icons, showing the user's real number. */
@Composable
private fun BubblePreview(earningsText: String) {
    val transition = rememberInfiniteTransition(label = "bubble")
    val floatY by transition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .padding(top = 4.dp)
            .size(width = 220.dp, height = 150.dp)
            .background(
                Brush.verticalGradient(listOf(primary.copy(alpha = 0.18f), primary.copy(alpha = 0.05f))),
                RoundedCornerShape(24.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
    ) {
        // fake app grid
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) {
                        Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                    }
                }
            }
        }
        // the bubble
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-12).dp, y = floatY.dp)
                .shadow(10.dp, CircleShape)
                .size(72.dp)
                .background(Brush.linearGradient(listOf(primary, Color(0xFF12B5CB))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TODAY", color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                Text(earningsText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
