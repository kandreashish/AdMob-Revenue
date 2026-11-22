package com.lavish.admob.bubble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lavish.admob.data.AdMobRepository
import com.lavish.admob.theme.SolarAdTheme
import com.lavish.admob.ui.utils.formatAmount
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Lets other entry points (Today banner, Settings row, locked bubble) open the promo on demand. */
object BubblePromo {
    private val _manualRequest = MutableStateFlow(false)
    val manualRequest: StateFlow<Boolean> = _manualRequest.asStateFlow()
    fun open() { _manualRequest.value = true }
    internal fun consume() { _manualRequest.value = false }
}

/** Local app-open counter used for the "show after N opens" rule. */
object BubbleLocalPrefs {
    private const val PREFS = "earnings_bubble"
    private const val KEY_APP_OPENS = "app_opens"
    fun incrementAppOpens(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putInt(KEY_APP_OPENS, p.getInt(KEY_APP_OPENS, 0) + 1).apply()
    }
    fun appOpens(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_APP_OPENS, 0)
}

/**
 * Overlay placed on top of App() in MainActivity. Decides when to auto-show the promo,
 * and runs the trial -> overlay-permission flow.
 */
@Composable
fun BubblePromoHost(
    repository: AdMobRepository,
    billingAvailable: Boolean = false, // flips to true once Play Billing lands (step 5)
    onBuyRequest: () -> Unit = {},
    onBubbleReady: () -> Unit = {}      // starts EarningsBubbleService (step 1)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val session by repository.session.collectAsState()
    val summary by repository.accountSummary.collectAsState()
    val themeMode by repository.themeMode.collectAsState()
    val access by BubbleEntitlementManager.access.collectAsState()
    val promo by BubbleEntitlementManager.promo.collectAsState()
    val manualRequest by BubblePromo.manualRequest.collectAsState()

    val liveConfig by BubbleFirebase.configState.collectAsState()
    val product by BubbleBillingManager.product.collectAsState()
    val purchaseInProgress by BubbleBillingManager.purchaseInProgress.collectAsState()
    // Prefer Play's localised price over the config label
    val config = product?.oneTimePurchaseOfferDetails?.formattedPrice
        ?.let { liveConfig.copy(priceLabel = it) } ?: liveConfig
    var showPromo by remember { mutableStateOf(false) }
    var autoShownThisSession by rememberSaveable { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BubbleFirebase.fetchRemoteConfig()
    }

    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(context)) {
            BubbleFirebase.log(context, BubbleEvents.OVERLAY_GRANTED)
            onBubbleReady()
        } else {
            BubbleFirebase.log(context, BubbleEvents.OVERLAY_DENIED)
            Toast.makeText(context, "Allow \"Display over other apps\" to show the bubble.", Toast.LENGTH_LONG).show()
        }
    }

    fun launchOverlayFlow() {
        if (Settings.canDrawOverlays(context)) {
            onBubbleReady()
        } else {
            overlayLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
        }
    }

    // Android 13+: ask for notifications first (bubble's "Hide" action + trial reminder), then overlay.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        launchOverlayFlow()
    }

    fun ensureOverlayThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            launchOverlayFlow()
        }
    }

    // ---- Trial-ending reminder ----
    LaunchedEffect(access) {
        when (val a = access) {
            is BubbleAccess.TrialActive ->
                BubbleReminder.schedule(context, a.endsAtMs, BubbleEntitlementManager.serverNow())
            BubbleAccess.Unlocked -> BubbleReminder.cancel(context)
            else -> Unit
        }
    }

    // ---- Keep the bubble running if the user had it on (e.g. after the app process was killed) ----
    LaunchedEffect(access) {
        if (access.canUseBubble && EarningsBubbleService.isEnabled(context) && Settings.canDrawOverlays(context)) {
            onBubbleReady()
        }
    }

    // ---- Auto-show rules ----
    // Auto-promo is ONLY for users who have never engaged. Anyone who started a trial, bought,
    // turned the bubble on, dismissed it, or hit the show cap never gets it automatically again.
    fun autoEligible(): Boolean {
        val a = BubbleEntitlementManager.access.value          // read live values, not stale captures
        val p = BubbleEntitlementManager.promo.value
        val cooldownMs = config.promoCooldownDays * 86_400_000L
        return repository.session.value.isLoggedIn &&
            config.featureEnabled && config.promoEnabled &&
            a is BubbleAccess.NotStarted &&                    // server-confirmed, never "Loading"
            !p.completed && !p.dismissed &&
            p.shownCount < config.promoMaxShows &&
            BubbleEntitlementManager.serverNow() - p.lastShownAt >= cooldownMs &&
            !EarningsBubbleService.isEnabled(context) &&
            !BubbleBillingManager.purchaseInProgress.value &&
            BubbleLocalPrefs.appOpens(context) >= config.promoMinAppOpens
    }

    LaunchedEffect(session.isLoggedIn, access, promo, config) {
        if (autoShownThisSession || showPromo) return@LaunchedEffect
        if (!autoEligible()) return@LaunchedEffect
        delay(1500) // let the Today screen settle first
        if (!autoEligible() || showPromo) return@LaunchedEffect // state may have changed meanwhile
        autoShownThisSession = true
        showPromo = true
        BubbleEntitlementManager.recordPromoShown()
        BubbleFirebase.log(context, BubbleEvents.PROMO_SHOWN, mapOf("source" to "auto"))
    }

    // ---- Manual open (banner / settings / locked bubble) ----
    LaunchedEffect(manualRequest) {
        if (!manualRequest) return@LaunchedEffect
        BubblePromo.consume()
        // Wait (briefly) for the real state; never show the intro just because we're still loading.
        val known = withTimeoutOrNull(5_000) {
            BubbleEntitlementManager.access.first { it.isKnown }
        }
        when (known) {
            null, BubbleAccess.Loading, BubbleAccess.SignedOut ->
                Toast.makeText(context, "Still syncing your account. Try again in a moment.", Toast.LENGTH_SHORT).show()
            BubbleAccess.Unlocked, is BubbleAccess.TrialActive -> ensureOverlayThenStart()
            else -> {
                showPromo = true
                BubbleFirebase.log(context, BubbleEvents.PROMO_SHOWN, mapOf("source" to "manual"))
            }
        }
    }

    // Close the promo if the user became entitled while it was open (restore, other device, etc.)
    LaunchedEffect(access, showPromo) {
        if (showPromo && access.canUseBubble && !isWorking) showPromo = false
    }

    if (!showPromo) return

    val mode = if (access is BubbleAccess.TrialExpired) PromoMode.TrialEnded else PromoMode.Intro
    val earningsText = session.currencySymbol + formatAmount(summary.todayRevenue)

    SolarAdTheme(themeMode = themeMode) {
        BubblePromoDialog(
            mode = mode,
            earningsText = earningsText,
            config = config,
            billingAvailable = billingAvailable,
            isWorking = isWorking || purchaseInProgress,
            onStartTrial = {
                BubbleFirebase.log(context, BubbleEvents.PROMO_CTA_TRIAL)
                isWorking = true
                scope.launch {
                    val ok = BubbleEntitlementManager.startTrial()
                    isWorking = false
                    if (ok) {
                        BubbleFirebase.log(context, BubbleEvents.TRIAL_STARTED)
                        showPromo = false
                        Toast.makeText(context, "Your ${config.trialLabel} free trial has started!", Toast.LENGTH_SHORT).show()
                        ensureOverlayThenStart()
                    } else {
                        Toast.makeText(context, "Couldn't start the trial. Check your connection and try again.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onBuy = {
                BubbleFirebase.log(context, BubbleEvents.PROMO_CTA_BUY)
                BubbleEntitlementManager.markPromoCompleted()
                showPromo = false
                onBuyRequest()
            },
            onLater = {
                BubbleFirebase.log(context, BubbleEvents.PROMO_LATER)
                showPromo = false
            },
            onNeverShow = {
                BubbleFirebase.log(context, BubbleEvents.PROMO_NEVER)
                BubbleEntitlementManager.recordPromoDismissedForever()
                showPromo = false
            }
        )
    }
}
