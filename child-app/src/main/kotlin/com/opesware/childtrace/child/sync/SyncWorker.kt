package com.opesware.childtrace.child.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.opesware.childtrace.child.ChildApplication
import java.time.Duration

/**
 * Drains the local encrypted store to the backend. WorkManager runs it even
 * after the process is killed or the device reboots, only when a network is
 * available, and applies exponential backoff on failure — far more reliable
 * than the in-process interval loop it replaces.
 *
 * On failure the worker returns [Result.retry] so WorkManager reschedules with
 * backoff; records stay in the store (unmarked) until a run succeeds, so
 * nothing is lost and nothing is uploaded twice.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ChildApplication
        return try {
            app.syncManager.drainOnce()
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "childtrace-activity-sync"

        /** Schedules the periodic drain. Idempotent — safe to call on every app start. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(Duration.ofMinutes(15))
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(30))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
