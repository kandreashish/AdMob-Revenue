package com.lavish.admob.bubble

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.PhotoSizeSelectSmall
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import androidx.core.content.ContextCompat
import com.lavish.admob.lockscreen.LockscreenEarningsNotification

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
    var trialEndedDismissed by remember { mutableStateOf(BubbleLocalPrefs.isTrialEndedBannerDismissed(context)) }
    var offDismissed by remember { mutableStateOf(BubbleLocalPrefs.isOffBannerDismissed(context)) }
    if (access == BubbleAccess.TrialExpired && trialEndedDismissed) return
    val isOffCard = !enabled && (access is BubbleAccess.TrialActive || access == BubbleAccess.Unlocked)
    if (isOffCard && offDismissed) return

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
            if (access == BubbleAccess.TrialExpired || isOffCard) {
                // Let users hide the "trial ended" / "bubble is off" card for good (still available in Settings)
                IconButton(
                    onClick = {
                        if (isOffCard) {
                            BubbleLocalPrefs.dismissOffBanner(context)
                            offDismissed = true
                        } else {
                            BubbleLocalPrefs.dismissTrialEndedBanner(context)
                            trialEndedDismissed = true
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            } else {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = primary)
            }
        }
    }
}

/** Unified Settings card for Floating Bubble + Lockscreen Earnings under one shared paywall. */
@Composable
fun BubbleSettingsRow() {
    val context = LocalContext.current
    val access by BubbleEntitlementManager.access.collectAsState()
    val config by BubbleFirebase.configState.collectAsState()
    val bubbleEnabled = EarningsBubbleService.enabledFlow(context).collectAsState().value == true
    val lockscreenEnabled = LockscreenEarningsNotification.enabledFlow(context).collectAsState().value == true

    val repo = remember { com.lavish.admob.RepositoryHolder.get(context) }
    val todaySummary by repo.todaySummary.collectAsState()
    val session by repo.session.collectAsState()
    val currency = session.currencySymbol.ifBlank { todaySummary.currencySymbol }
    val liveAmount = currency + com.lavish.admob.ui.utils.formatAmount(todaySummary.todayRevenue)

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            LockscreenEarningsNotification.enable(context)
        } else {
            Toast.makeText(
                context,
                "Notification permission is needed to show earnings on the lockscreen.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun toggleLockscreen(on: Boolean) {
        if (!access.canUseBubble) {
            BubblePromo.open(PromoTarget.LOCKSCREEN)
            return
        }
        if (!on) {
            LockscreenEarningsNotification.disable(context)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            LockscreenEarningsNotification.enable(context)
        }
    }

    val status = when (val a = access) {
        BubbleAccess.Unlocked -> "Unlocked forever"
        is BubbleAccess.TrialActive -> "Free trial: ${remainingLabel(a.remainingMs)}"
        BubbleAccess.TrialExpired -> "Trial ended. Tap to unlock"
        BubbleAccess.NotStarted -> "Try free for ${config.trialLabel}"
        else -> "Sign in to use"
    }

    val primaryBlue = Color(0xFF3367D6)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { if (!access.canUseBubble) BubblePromo.open() }
    ) {
        Column(Modifier.padding(14.dp)) {
            // Header row: Title + NEW badge + Unlock forever CTA pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Live earnings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (access == BubbleAccess.NotStarted) {
                            Spacer(Modifier.width(6.dp))
                            NewBadge()
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusIcon = if (access == BubbleAccess.Unlocked) {
                            Icons.Outlined.CheckCircle
                        } else {
                            Icons.Outlined.Timer
                        }
                        val statusTint = if (access == BubbleAccess.Unlocked) {
                            Color(0xFF1E8E3E)
                        } else {
                            primaryBlue
                        }
                        Icon(
                            statusIcon,
                            contentDescription = null,
                            tint = statusTint,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (access != BubbleAccess.Unlocked && access.isKnown) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = primaryBlue,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable { BubblePromo.open() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Unlock forever",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Sub-card 1: Floating bubble
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Floating bubble",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Overlay today's earnings over other apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Switch(
                            checked = bubbleEnabled && access.canUseBubble,
                            onCheckedChange = { on ->
                                if (on) {
                                    BubblePromo.open(PromoTarget.BUBBLE)
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

            Spacer(Modifier.height(10.dp))

            // Sub-card 2: Lockscreen earnings
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Lockscreen earnings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        val subtitle = buildAnnotatedString {
                            append("Show Today's earnings : ")
                            withStyle(
                                SpanStyle(
                                    background = Color(0xFF9CF5A6),
                                    color = Color(0xFF0B4F1C),
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(" $liveAmount ")
                            }
                            append(" on lockscreen")
                        }
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = lockscreenEnabled && access.canUseBubble,
                        onCheckedChange = { on -> toggleLockscreen(on) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Sub-card 3: Home-screen widgets (same paywall)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Home-screen widgets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Small: today's earnings. Large: today vs yesterday, goal, impressions, eCPM, clicks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        listOf(false to "Add small", true to "Add large").forEach { (large, label) ->
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    if (!access.canUseBubble) {
                                        BubblePromo.open()
                                    } else if (!com.lavish.admob.widget.EarningsWidgets.requestPin(context, large)) {
                                        Toast.makeText(context, "Long-press your home screen → Widgets → AdDash", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                if (!access.canUseBubble) {
                                    Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(label)
                            }
                        }
                    }
                }
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

/** Size & Opacity sliders with a live preview of the bubble (preview shows while dragging). */
@Composable
private fun BubbleSizeSelector() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        BubbleSizePref.init(context)
        BubbleAlphaPref.init(context)
    }
    val savedSize by BubbleSizePref.diameterDp.collectAsState()
    val savedAlpha by BubbleAlphaPref.alphaPercent.collectAsState()
    var sliding by remember { mutableStateOf(false) }
    var sizeValue by remember(savedSize) { mutableFloatStateOf(savedSize.toFloat()) }
    var alphaValue by remember(savedAlpha) { mutableFloatStateOf(savedAlpha.toFloat()) }
    // Never leave the real bubble stuck visible if this screen closes mid-drag
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            BubbleSizePref.previewDp.value = null
            BubbleAlphaPref.previewPercent.value = null
        }
    }

    val repo = remember { com.lavish.admob.RepositoryHolder.get(context) }
    val summary by repo.accountSummary.collectAsState()
    val session by repo.session.collectAsState()
    val amount = session.currencySymbol + com.lavish.admob.ui.utils.formatAmount(summary.todayRevenue)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.PhotoSizeSelectSmall,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Size",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(BubbleSizePref.scaleFor(sizeValue.toInt()) * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF3367D6),
                fontWeight = FontWeight.SemiBold
            )
        }
        SlimBubbleSlider(
            value = sizeValue,
            valueRange = BubbleSizePref.MIN_DP.toFloat()..BubbleSizePref.MAX_DP.toFloat(),
            onValueChange = {
                sizeValue = it
                sliding = true
                BubbleSizePref.previewDp.value = it.toInt()
            },
            onValueChangeFinished = {
                sliding = false
                BubbleSizePref.set(context, sizeValue.toInt())
                BubbleSizePref.previewDp.value = null
            }
        )

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Opacity,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Opacity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${alphaValue.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF3367D6),
                fontWeight = FontWeight.SemiBold
            )
        }
        SlimBubbleSlider(
            value = alphaValue,
            valueRange = BubbleAlphaPref.MIN_PERCENT.toFloat()..BubbleAlphaPref.MAX_PERCENT.toFloat(),
            onValueChange = {
                alphaValue = it
                sliding = true
                BubbleAlphaPref.previewPercent.value = it.toInt()
            },
            onValueChangeFinished = {
                sliding = false
                BubbleAlphaPref.set(context, alphaValue.toInt())
                BubbleAlphaPref.previewPercent.value = null
            }
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
                    diameterDp = sizeValue.toInt(),
                    alpha = alphaValue / 100f,
                    amount = amount,
                    colors = BubbleColors.forProgress(BubbleColors.progress(summary.todayRevenue, summary.dailyGoal))
                        .map { Color(it) }
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SlimBubbleSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        thumb = {
            Box(
                Modifier
                    .size(18.dp)
                    .background(Color(0xFF3367D6), CircleShape)
                    .padding(4.dp)
                    .background(Color.White, CircleShape)
            )
        },
        track = { sliderState ->
            val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            val fraction = if (range > 0f) {
                ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
            } else 0f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .background(Color(0xFF3367D6), CircleShape)
                )
            }
        }
    )
}

/** Mirrors the real overlay bubble's look (gradient circle, TODAY + amount, translucent). */
@Composable
private fun BubblePreviewCircle(diameterDp: Int, alpha: Float, amount: String, colors: List<Color>) {
    val scale = BubbleSizePref.scaleFor(diameterDp)
    Box(
        Modifier
            .size(diameterDp.dp)
            .graphicsLayer { this.alpha = alpha }
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
