package com.lavish.admob.bubble

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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.lavish.admob.MainActivity
import java.util.concurrent.TimeUnit

/** Local "trial ends soon" reminder, fired 4h before the trial ends. No server needed. */
object BubbleReminder {
    private const val WORK_NAME = "bubble_trial_reminder"
    private const val LEAD_MS = 4 * 3_600_000L
    private const val PREFS = "earnings_bubble"
    private const val KEY_SCHEDULED_FOR = "reminder_for_trial_end"

    fun schedule(context: Context, trialEndsAtMs: Long, serverNowMs: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_SCHEDULED_FOR, 0L) == trialEndsAtMs) return // already scheduled
        // Normally 4h before the end; for short (test) trials, fire with a third of the trial left.
        val lead = minOf(LEAD_MS, BubbleFirebase.config().trialDurationMs / 3)
        val delay = trialEndsAtMs - lead - serverNowMs
        if (delay <= 0) return
        val req = OneTimeWorkRequestBuilder<TrialReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, req)
        prefs.edit().putLong(KEY_SCHEDULED_FOR, trialEndsAtMs).apply()
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

class TrialReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val channelId = "bubble_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(channelId, "Earnings bubble reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = PendingIntent.getActivity(
            ctx, 5,
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EarningsBubbleService.EXTRA_OPEN_PROMO, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(com.lavish.admob.android.R.drawable.ic_stat_bubble)
            .setContentTitle("Your free trial ends soon")
            .setContentText("Keep your earnings bubble forever with a one-time unlock.")
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(4202, n) }
        BubbleFirebase.log(ctx, "bubble_trial_reminder_sent")
        return Result.success()
    }
}
