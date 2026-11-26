package com.lavish.admob.lockscreen

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.lavish.admob.RepositoryHolder
import com.lavish.admob.bubble.BubbleEntitlementManager
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager job that refreshes Today's AdMob earnings for the lockscreen notification.
 */
class LockscreenEarningsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "lockscreen_earnings_periodic_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<LockscreenEarningsWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!LockscreenEarningsNotification.isEnabled(ctx)) return Result.success()
        BubbleEntitlementManager.start(ctx)
        val access = BubbleEntitlementManager.access.value
        if (access.isKnown && !access.canUseBubble) {
            LockscreenEarningsNotification.enforceEntitlement(ctx, access)
            return Result.success()
        }
        GoogleSignIn.getLastSignedInAccount(ctx) ?: return Result.success()
        val repo = RepositoryHolder.get(ctx)
        if (!repo.session.value.isLoggedIn) return Result.success()

        repo.refreshForBackground()
        LockscreenEarningsNotification.updateIfEnabled(ctx, repo.todaySummary.value, repo.session.value)
        return Result.success()
    }
}
