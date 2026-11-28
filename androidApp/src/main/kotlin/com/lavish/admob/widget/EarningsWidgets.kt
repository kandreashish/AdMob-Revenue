package com.lavish.admob.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lavish.admob.MainActivity
import com.lavish.admob.RepositoryHolder
import com.lavish.admob.android.R
import com.lavish.admob.bubble.BubbleEntitlementManager
import com.lavish.admob.model.AccountSummary
import com.lavish.admob.model.UserSession
import com.lavish.admob.ui.utils.formatAmount
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/** Small (2x1) home-screen widget: today's earnings only. */
class EarningsWidgetSmall : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        EarningsWidgets.updateAll(context)
        EarningsWidgets.ensurePeriodic(context) // re-arm if WorkManager lost it (app update, data clear)
        EarningsWidgets.refreshNow(context) // fetch fresh data even if the app isn't running
    }
    override fun onEnabled(context: Context) = EarningsWidgets.scheduleRefresh(context)
    override fun onDisabled(context: Context) = EarningsWidgets.cancelIfNoWidgets(context)
}

/** Large (4x2) home-screen widget: today vs yesterday, goal progress, impressions, eCPM, clicks. */
class EarningsWidgetLarge : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        EarningsWidgets.updateAll(context)
        EarningsWidgets.ensurePeriodic(context) // re-arm if WorkManager lost it (app update, data clear)
        EarningsWidgets.refreshNow(context) // fetch fresh data even if the app isn't running
    }
    override fun onEnabled(context: Context) = EarningsWidgets.scheduleRefresh(context)
    override fun onDisabled(context: Context) = EarningsWidgets.cancelIfNoWidgets(context)
}

object EarningsWidgets {
    private const val WORK_NAME = "earnings_widget_refresh"
    private const val PREFS = "earnings_widget_prefs"
    private const val KEY_SYNCED_AT = "synced_at"
    private const val KEY_ATTEMPT_AT = "attempt_at"
    private const val KEY_LAST_FAILED = "last_failed"
    private const val KEY_SYNCING_SINCE = "syncing_since"
    private const val MANUAL_WORK_NAME = "earnings_widget_manual_refresh"
    private const val MANUAL_DEBOUNCE_MS = 5_000L
    /** "Syncing…" older than this is treated as stale (process killed mid-fetch) and ignored. */
    private const val SYNCING_STALE_MS = 2 * 60_000L
    private const val NET_WORK_NAME = "earnings_widget_net_refresh"
    private const val MIN_NET_REFRESH_GAP_MS = 2 * 60_000L
    @Volatile private var networkWatching = false
    @Volatile private var wasOffline = false

    /**
     * Refresh widgets when the device regains internet (e.g. Wi-Fi <-> mobile, or back from offline).
     * Lives as long as the app process; the periodic worker covers the rest
     * (its CONNECTED constraint also makes a missed run fire on reconnect).
     */
    fun watchNetwork(context: Context) {
        if (networkWatching) return
        val app = context.applicationContext
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return
        networkWatching = true
        wasOffline = cm.activeNetwork == null
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) { wasOffline = true }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return
                    val changed = wasOffline
                    wasOffline = false
                    if (changed) refreshNow(app)
                }
            })
        }.onFailure { networkWatching = false }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Last successful sync, or 0 if never / invalid (stored time is in the future after a clock change). */
    private fun syncedAt(context: Context): Long {
        val at = prefs(context).getLong(KEY_SYNCED_AT, 0L)
        return if (at > System.currentTimeMillis()) 0L else at
    }

    /** One-off background refresh, throttled so flaky networks don't spam the AdMob API. */
    fun refreshNow(context: Context) {
        val app = context.applicationContext
        if (!hasAny(app)) return
        // Throttle on the last *attempt* (success or failure) so a failing network can't spam, and
        // a clock moved backwards (negative gap) can't block refreshes forever.
        val p = prefs(app)
        val last = maxOf(syncedAt(app), p.getLong(KEY_ATTEMPT_AT, 0L).takeIf { it <= System.currentTimeMillis() } ?: 0L)
        val gap = System.currentTimeMillis() - last
        if (last > 0L && gap in 0 until MIN_NET_REFRESH_GAP_MS) return
        val req = OneTimeWorkRequestBuilder<EarningsWidgetWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(app).enqueueUniqueWork(NET_WORK_NAME, ExistingWorkPolicy.KEEP, req)
    }

    /**
     * Refresh button. Unlike [refreshNow] this skips the 2-min throttle (the user asked for it),
     * but ignores double-taps within [MANUAL_DEBOUNCE_MS] and taps while a refresh is already running.
     * Shows "Syncing…" immediately so the tap has visible feedback.
     */
    fun manualRefresh(context: Context) {
        val app = context.applicationContext
        if (!hasAny(app)) return
        val now = System.currentTimeMillis()
        val since = prefs(app).getLong(KEY_SYNCING_SINCE, 0L)
        if (since in (now - MANUAL_DEBOUNCE_MS)..now) return
        if (!isOnline(app)) {
            // Don't sit on "Syncing…" waiting for a network that isn't there; say so right away.
            markSyncFailed(app)
            updateAll(app)
            return
        }
        prefs(app).edit().putLong(KEY_SYNCING_SINCE, now).apply()
        updateAll(app)
        val req = OneTimeWorkRequestBuilder<EarningsWidgetWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        // REPLACE: a tap should start a fresh attempt even if an older one is sitting in retry backoff.
        WorkManager.getInstance(app).enqueueUniqueWork(MANUAL_WORK_NAME, ExistingWorkPolicy.REPLACE, req)
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isSyncing(context: Context): Boolean {
        val since = prefs(context).getLong(KEY_SYNCING_SINCE, 0L)
        val now = System.currentTimeMillis()
        return since in (now - SYNCING_STALE_MS)..now
    }

    /** Clears the "Syncing…" state without recording a result (e.g. nothing to fetch). */
    fun endSyncing(context: Context) {
        prefs(context).edit().remove(KEY_SYNCING_SINCE).apply()
    }

    /** Call ONLY after a real AdMob fetch succeeded. */
    fun markSynced(context: Context) {
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .putLong(KEY_SYNCED_AT, now)
            .putLong(KEY_ATTEMPT_AT, now)
            .putBoolean(KEY_LAST_FAILED, false)
            .remove(KEY_SYNCING_SINCE)
            .apply()
    }

    /** Call when a fetch attempt failed (offline, auth, API error). Keeps the last good sync time. */
    fun markSyncFailed(context: Context) {
        prefs(context).edit()
            .putLong(KEY_ATTEMPT_AT, System.currentTimeMillis())
            .putBoolean(KEY_LAST_FAILED, true)
            .remove(KEY_SYNCING_SINCE)
            .apply()
    }

    /** Logout / account switch: the next account must not inherit the old sync state. */
    fun clearSync(context: Context) {
        prefs(context).edit().remove(KEY_SYNCED_AT).remove(KEY_ATTEMPT_AT).remove(KEY_LAST_FAILED)
            .remove(KEY_SYNCING_SINCE).apply()
    }

    /**
     * Absolute time ("Synced 3:42 PM", or "Synced Nov 26, 3:42 PM" if not today) instead of
     * "X min ago": widgets are only redrawn every ~30 min, so relative text would go stale.
     * Follows the device 12/24h setting and locale.
     */
    private fun syncedLabel(context: Context): String {
        if (isSyncing(context)) return "Syncing…"
        val at = syncedAt(context)
        val failed = prefs(context).getBoolean(KEY_LAST_FAILED, false)
        if (at == 0L) return if (failed) "Sync failed" else "Not synced yet"
        val now = System.currentTimeMillis()
        val time = android.text.format.DateUtils.formatSameDayTime(
            at, now, java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT
        ).toString()
        return if (failed) "Offline · synced $time" else "Synced $time"
    }

    private fun ids(context: Context, cls: Class<*>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, cls))

    fun hasAny(context: Context) =
        ids(context, EarningsWidgetSmall::class.java).isNotEmpty() || ids(context, EarningsWidgetLarge::class.java).isNotEmpty()

    /** Push the latest cached summary to every placed widget. Safe to call often. */
    fun updateAll(context: Context) {
        val app = context.applicationContext
        val repo = RepositoryHolder.get(app)
        update(app, repo.todaySummary.value, repo.session.value)
    }

    fun update(context: Context, summary: AccountSummary, session: UserSession) {
        val app = context.applicationContext
        val mgr = AppWidgetManager.getInstance(app)
        val small = ids(app, EarningsWidgetSmall::class.java)
        val large = ids(app, EarningsWidgetLarge::class.java)
        if (small.isEmpty() && large.isEmpty()) return

        val open = PendingIntent.getActivity(
            app, 202,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cur = session.currencySymbol.ifBlank { summary.currencySymbol }
        // Paid feature: shares the "Unlock forever" entitlement with the bubble + lockscreen.
        // While entitlement is still loading we keep showing data to avoid flicker.
        val access = BubbleEntitlementManager.access.value
        val locked = access.isKnown && !access.canUseBubble
        val loggedIn = session.isLoggedIn && !locked
        val today = when {
            locked -> "🔒 Unlock"
            !session.isLoggedIn -> "Sign in"
            else -> cur + formatAmount(summary.todayRevenue)
        }

        val refresh = PendingIntent.getBroadcast(
            app, 204,
            Intent(app, WidgetClockReceiver::class.java).setAction(WidgetClockReceiver.ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Hidden when signed out / locked: there is nothing to refresh, the widget tap opens the app instead.
        val refreshVisibility = if (loggedIn) android.view.View.VISIBLE else android.view.View.GONE
        val refreshAlpha = if (loggedIn && isSyncing(app)) 0.4f else 1f

        if (small.isNotEmpty()) {
            val v = RemoteViews(app.packageName, R.layout.widget_earnings_small).apply {
                setTextViewText(R.id.w_today, today)
                setTextViewText(R.id.w_sync, if (loggedIn) syncedLabel(app) else "")
                setOnClickPendingIntent(R.id.widget_root, open)
                setOnClickPendingIntent(R.id.w_refresh, refresh)
                setViewVisibility(R.id.w_refresh, refreshVisibility)
                setFloat(R.id.w_refresh, "setAlpha", refreshAlpha)
            }
            mgr.updateAppWidget(small, v)
        }

        if (large.isNotEmpty()) {
            val v = RemoteViews(app.packageName, R.layout.widget_earnings_large).apply {
                setTextViewText(R.id.w_today, today)
                setOnClickPendingIntent(R.id.widget_root, open)
                setOnClickPendingIntent(R.id.w_refresh, refresh)
                setViewVisibility(R.id.w_refresh, refreshVisibility)
                setFloat(R.id.w_refresh, "setAlpha", refreshAlpha)
                if (loggedIn) {
                    val pct = summary.deltaPercentage
                    val arrow = if (pct >= 0) "▲" else "▼"
                    setTextViewText(R.id.w_delta, "$arrow ${"%.1f".format(abs(pct))}%")
                    setTextViewText(R.id.w_yesterday, "Yesterday ${cur}${formatAmount(summary.yesterdayRevenue)}")
                    val progress = (summary.dailyGoalProgress * 100).roundToInt().coerceIn(0, 100)
                    setProgressBar(R.id.w_goal, 100, progress, false)
                    setTextViewText(R.id.w_goal_label, "$progress% of ${cur}${formatAmount(summary.dailyGoal)} goal")
                    setTextViewText(R.id.w_impr, "Impr\n${summary.impressions}")
                    setTextViewText(R.id.w_ecpm, "eCPM\n${cur}${"%.2f".format(summary.ecpm)}")
                    setTextViewText(R.id.w_clicks, "Clicks\n${summary.clicks}")
                    setTextViewText(R.id.w_sync, syncedLabel(app))
                } else {
                    setTextViewText(R.id.w_delta, "")
                    setTextViewText(R.id.w_yesterday, if (locked) "Unlock forever in AdDash to use this widget" else "Open AdDash to sign in")
                    setTextViewText(R.id.w_goal_label, "")
                    setTextViewText(R.id.w_impr, ""); setTextViewText(R.id.w_ecpm, ""); setTextViewText(R.id.w_clicks, "")
                    setTextViewText(R.id.w_sync, "")
                    setProgressBar(R.id.w_goal, 100, 0, false)
                }
            }
            mgr.updateAppWidget(large, v)
        }
    }

    fun scheduleRefresh(context: Context) {
        ensurePeriodic(context)
        updateAll(context)
    }

    /** Idempotent (KEEP): safe to call on every widget update. */
    fun ensurePeriodic(context: Context) {
        if (!hasAny(context.applicationContext)) return // no widgets placed: don't burn battery / API quota
        val req = PeriodicWorkRequestBuilder<EarningsWidgetWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        scheduleMidnightRedraw(context)
    }

    private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context.applicationContext, 203,
        Intent(context.applicationContext, WidgetClockReceiver::class.java).setAction(WidgetClockReceiver.ACTION_MIDNIGHT),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /**
     * Redraw just after the next local midnight so "Synced 11:58 PM" becomes "Synced Nov 26, 11:58 PM".
     * Inexact + allow-while-idle: needs no exact-alarm permission and still fires in Doze (may lag a few
     * minutes, which is fine). Re-armed on every fire and widget update; replacing the same
     * PendingIntent means there is never more than one pending alarm.
     */
    fun scheduleMidnightRedraw(context: Context) {
        val app = context.applicationContext
        if (!hasAny(app)) return
        val am = app.getSystemService(android.app.AlarmManager::class.java) ?: return
        val next = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 5); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        runCatching { am.setAndAllowWhileIdle(android.app.AlarmManager.RTC, next, midnightIntent(app)) }
    }

    /** Asks the launcher to pin a widget (Android 8+). Returns false if the launcher doesn't support it. */
    fun requestPin(context: Context, large: Boolean): Boolean {
        val mgr = AppWidgetManager.getInstance(context)
        if (!mgr.isRequestPinAppWidgetSupported) return false
        val cls = if (large) EarningsWidgetLarge::class.java else EarningsWidgetSmall::class.java
        return mgr.requestPinAppWidget(ComponentName(context, cls), null, null)
    }

    fun cancelIfNoWidgets(context: Context) {
        if (hasAny(context)) return
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        context.applicationContext.getSystemService(android.app.AlarmManager::class.java)
            ?.cancel(midnightIntent(context))
    }
}

/** Redraws widgets on day rollover and on clock / time-zone / locale changes (label is date-aware). */
class WidgetClockReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!EarningsWidgets.hasAny(context)) return
        if (intent.action == ACTION_REFRESH) {
            EarningsWidgets.manualRefresh(context)
            return
        }
        runCatching { EarningsWidgets.updateAll(context) }
        // Re-arm for the next midnight; also recalculates after a time / zone change.
        EarningsWidgets.scheduleMidnightRedraw(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.lavish.admob.widget.ACTION_MANUAL_REFRESH"
        const val ACTION_MIDNIGHT = "com.lavish.admob.widget.ACTION_MIDNIGHT_REDRAW"
    }
}

/** Fetches fresh numbers in the background so widgets stay current when the app is closed. */
class EarningsWidgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!EarningsWidgets.hasAny(ctx)) return Result.success()
        BubbleEntitlementManager.start(ctx)
        val repo = RepositoryHolder.get(ctx)
        val access = BubbleEntitlementManager.access.value
        if (access.isKnown && !access.canUseBubble) {
            EarningsWidgets.endSyncing(ctx)
            EarningsWidgets.update(ctx, repo.todaySummary.value, repo.session.value)
            return Result.success()
        }
        var ok = true
        if (!repo.session.value.isLoggedIn) EarningsWidgets.endSyncing(ctx)
        if (repo.session.value.isLoggedIn) {
            // Guard against a hung token renewal / request keeping the worker (and the repo's fetch
            // lock) stuck until WorkManager's 10-min kill, which also delays the next run.
            ok = runCatching {
                kotlinx.coroutines.withTimeoutOrNull(90_000L) { repo.refreshForBackground() } ?: false
            }.getOrDefault(false)
            // refreshForBackground can return false without reaching the repo hook (no token),
            // so record the failure here too. Success is stamped by the repo hook.
            if (!ok) EarningsWidgets.markSyncFailed(ctx)
        }
        EarningsWidgets.update(ctx, repo.todaySummary.value, repo.session.value)
        // Previously a failure returned success, so a one-off refresh that hit a dead network / flaky
        // DNS was simply dropped until the next 30-min slot (often hours later under Doze).
        // Retry with WorkManager's exponential backoff a few times instead.
        return if (ok || runAttemptCount >= 3) Result.success() else Result.retry()
    }
}
