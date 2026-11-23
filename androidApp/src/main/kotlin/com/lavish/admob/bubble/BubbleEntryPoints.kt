package com.lavish.admob.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private fun remainingLabel(ms: Long): String {
    val h = ms / 3_600_000
    val m = (ms % 3_600_000) / 60_000
    return if (h > 0) "${h}h ${m}m left" else "${m}m left"
}

/** Today-screen card. Hidden once the user owns the bubble and has it running. */
@Composable
fun BubbleTodayBanner() {
    val context = LocalContext.current
    val access by BubbleEntitlementManager.access.collectAsState()
    val price by BubbleBillingManager.product.collectAsState()
    val config by BubbleFirebase.configState.collectAsState()
    val priceLabel = price?.oneTimePurchaseOfferDetails?.formattedPrice ?: config.priceLabel
    // Reactive: hides the "bubble is off" card the instant the bubble starts
    val enabled = EarningsBubbleService.enabledFlow(context).collectAsState().value == true

    val (title, body) = when (val a = access) {
        BubbleAccess.NotStarted -> "Earnings bubble" to "See today's earnings on top of any app. Try it free for ${config.trialLabel}."
        BubbleAccess.TrialExpired -> "Your bubble trial has ended" to "Unlock it forever for $priceLabel, one time."
        is BubbleAccess.TrialActive -> if (enabled) return else
            "Earnings bubble is off" to "Trial: ${remainingLabel(a.remainingMs)}. Tap to turn it on."
        BubbleAccess.Unlocked -> if (enabled) return else "Earnings bubble is off" to "Tap to show it again."
        else -> return
    }

    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { BubblePromo.open() }
    ) {
        Row(
            Modifier
                .background(Brush.horizontalGradient(listOf(primary.copy(alpha = 0.14f), Color(0xFF12B5CB).copy(alpha = 0.10f))))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(Brush.linearGradient(listOf(primary, Color(0xFF12B5CB))), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Layers, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (access == BubbleAccess.NotStarted) {
                        Spacer(Modifier.width(6.dp))
                        NewBadge()
                    }
                }
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = primary)
        }
    }
}

/** Row in Profile & Settings: on/off switch + status. */
@Composable
fun BubbleSettingsRow() {
    val context = LocalContext.current
    val access by BubbleEntitlementManager.access.collectAsState()
    val config by BubbleFirebase.configState.collectAsState()
    val enabled = EarningsBubbleService.enabledFlow(context).collectAsState().value == true

    val status = when (val a = access) {
        BubbleAccess.Unlocked -> "Unlocked forever"
        is BubbleAccess.TrialActive -> "Free trial: ${remainingLabel(a.remainingMs)}"
        BubbleAccess.TrialExpired -> "Trial ended. Tap to unlock"
        BubbleAccess.NotStarted -> "Try free for ${config.trialLabel}"
        else -> "Sign in to use"
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { if (!access.canUseBubble) BubblePromo.open() }
    ) {
      Column(Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Earnings bubble", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (access == BubbleAccess.NotStarted) {
                        Spacer(Modifier.width(6.dp))
                        NewBadge()
                    }
                }
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = enabled && access.canUseBubble,
                onCheckedChange = { on ->
                    if (on) {
                        BubblePromo.open() // starts it, or shows promo if locked
                    } else {
                        EarningsBubbleService.stop(context)
                    }
                }
            )
        }
        if (access.canUseBubble) {
            Spacer(Modifier.height(10.dp))
            BubbleSizeSelector()
        }
      }
    }
}

/** Small red "NEW" pill shown until the user has tried the feature. */
@Composable
private fun NewBadge() {
    Box(
        Modifier
            .background(Color(0xFFD93025), RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            "NEW",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/** Size slider with a live preview of the bubble (preview shows while dragging). */
@Composable
private fun BubbleSizeSelector() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { BubbleSizePref.init(context) }
    val saved by BubbleSizePref.diameterDp.collectAsState()
    var sliding by remember { mutableStateOf(false) }
    var value by remember(saved) { mutableFloatStateOf(saved.toFloat()) }
    // Never leave the real bubble stuck visible if this screen closes mid-drag
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { BubbleSizePref.previewDp.value = null }
    }

    val repo = remember { com.lavish.admob.RepositoryHolder.get(context) }
    val summary by repo.accountSummary.collectAsState()
    val session by repo.session.collectAsState()
    val amount = session.currencySymbol + com.lavish.admob.ui.utils.formatAmount(summary.todayRevenue)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Size",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(BubbleSizePref.scaleFor(value.toInt()) * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = {
                value = it
                sliding = true
                BubbleSizePref.previewDp.value = it.toInt() // real bubble resizes live
            },
            onValueChangeFinished = {
                sliding = false
                BubbleSizePref.set(context, value.toInt())
                BubbleSizePref.previewDp.value = null       // bubble hides again (app is open)
            },
            valueRange = BubbleSizePref.MIN_DP.toFloat()..BubbleSizePref.MAX_DP.toFloat()
        )
        // If the real bubble is running, it shows itself while sliding. Otherwise show an in-app copy.
        val realBubbleShowing = EarningsBubbleService.isEnabled(context) &&
            android.provider.Settings.canDrawOverlays(context)
        AnimatedVisibility(
            visible = sliding && !realBubbleShowing,
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(3000)) +
                androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(3000))
        ) {
            Box(
                Modifier.fillMaxWidth().height((BubbleSizePref.MAX_DP + 16).dp),
                contentAlignment = Alignment.Center
            ) {
                BubblePreviewCircle(
                    diameterDp = value.toInt(),
                    amount = amount,
                    colors = BubbleColors.forProgress(BubbleColors.progress(summary.todayRevenue, summary.dailyGoal))
                        .map { Color(it) }
                )
            }
        }
    }
}

/** Mirrors the real overlay bubble's look (gradient circle, TODAY + amount, translucent). */
@Composable
private fun BubblePreviewCircle(diameterDp: Int, amount: String, colors: List<Color>) {
    val scale = BubbleSizePref.scaleFor(diameterDp)
    Box(
        Modifier
            .size(diameterDp.dp)
            .graphicsLayer { alpha = 0.85f }
            .background(
                Brush.linearGradient(colors),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val noPad = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            )
            Text("TODAY", color = Color.White.copy(alpha = 0.86f), fontSize = (8f * scale).sp, fontWeight = FontWeight.Bold, style = noPad)
            Spacer(Modifier.height((1f * scale).dp))
            Text(
                amount,
                style = noPad,
                color = Color.White,
                fontSize = ((if (amount.length > 7) 11f else 13f) * scale).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
