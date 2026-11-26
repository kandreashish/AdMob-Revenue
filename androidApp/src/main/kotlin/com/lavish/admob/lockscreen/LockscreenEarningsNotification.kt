package com.lavish.admob.lockscreen

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lavish.admob.MainActivity
import com.lavish.admob.RepositoryHolder
import com.lavish.admob.bubble.BubbleAccess
import com.lavish.admob.bubble.BubbleEntitlementManager
import com.lavish.admob.model.AccountSummary
import com.lavish.admob.model.UserSession
import com.lavish.admob.ui.utils.formatAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Displays Today's AdMob earnings and % change vs. yesterday as a silent,
 * public-visibility notification on the Android lockscreen.
 */
object LockscreenEarningsNotification {
    private const val PREFS = "lockscreen_earnings_prefs"
    private const val KEY_ENABLED = "lockscreen_enabled"
    private const val CHANNEL_ID = "lockscreen_earnings"
    private const val NOTIF_ID = 4301

    private val _enabled = MutableStateFlow<Boolean?>(null)

    fun isEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun enabledFlow(context: Context): StateFlow<Boolean?> {
        if (_enabled.value == null) {
            _enabled.value = isEnabled(context)
        }
        return _enabled
    }

    fun enable(context: Context) {
        val app = context.applicationContext
        val access = BubbleEntitlementManager.access.value
        if (access.isKnown && !access.canUseBubble) {
            NotificationManagerCompat.from(app).cancel(NOTIF_ID)
            return
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, true).apply()
        _enabled.value = true
        val repo = RepositoryHolder.get(app)
        show(app, repo.todaySummary.value, repo.session.value)
        LockscreenEarningsWorker.schedule(app)
    }

    fun disable(context: Context) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, false).apply()
        _enabled.value = false
        LockscreenEarningsWorker.cancel(app)
        NotificationManagerCompat.from(app).cancel(NOTIF_ID)
    }

    fun enforceEntitlement(context: Context, access: BubbleAccess) {
        val app = context.applicationContext
        if (access.isKnown && !access.canUseBubble) {
            NotificationManagerCompat.from(app).cancel(NOTIF_ID)
        } else if (access.canUseBubble && isEnabled(app)) {
            val repo = RepositoryHolder.get(app)
            show(app, repo.todaySummary.value, repo.session.value)
        }
    }

    fun updateIfEnabled(context: Context, summary: AccountSummary, session: UserSession) {
        val app = context.applicationContext
        if (!isEnabled(app)) return
        val access = BubbleEntitlementManager.access.value
        if ((access.isKnown && !access.canUseBubble) || !session.isLoggedIn) {
            NotificationManagerCompat.from(app).cancel(NOTIF_ID)
            return
        }
        show(app, summary, session)
    }

    private fun show(context: Context, summary: AccountSummary, session: UserSession) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!session.isLoggedIn) return

        ensureChannel(context)

        val currency = session.currencySymbol.ifBlank { summary.currencySymbol }
        val amountText = currency + formatAmount(summary.todayRevenue)

        val openAppIntent = PendingIntent.getActivity(
            context,
            101,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.lavish.admob.android.R.drawable.ic_stat_bubble)
            .setContentTitle("Earnings: $amountText")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppIntent)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_ID, notification) }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Lockscreen earnings",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows today's AdMob earnings and % change on the lockscreen."
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }
}
