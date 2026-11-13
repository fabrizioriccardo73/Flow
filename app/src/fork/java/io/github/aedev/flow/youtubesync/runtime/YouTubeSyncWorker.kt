package io.github.aedev.flow.youtubesync.runtime

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.aedev.flow.data.profile.ProfileWork
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** One automatic YouTube Sync of the running profile, with network. Transient failures retry with a growing wait. */
class YouTubeSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun runner(): YouTubeSyncRunner
    }

    override suspend fun doWork(): Result {
        // Queued by another profile before a switch: nothing to do in this one.
        if (!ProfileWork.belongsHere(this)) return Result.success()
        val runner = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).runner()
        return when (runner.runAutomatically()) {
            AutoOutcome.RetryLater -> if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}

/** [SyncScheduling] with WorkManager. Work carries the profile, so a profile switch cancels it. */
@Singleton
class WorkManagerSyncScheduling
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SyncScheduling {
        private val workManager get() = WorkManager.getInstance(context)

        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        override fun ensurePeriodic() {
            val request =
                PeriodicWorkRequestBuilder<YouTubeSyncWorker>(PERIOD_HOURS, TimeUnit.HOURS)
                    .setConstraints(network)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                    .setInputData(ProfileWork.data(context))
                    .addTag(ProfileWork.tag(context))
                    .build()
            workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        override fun runSoon() = enqueueOnce(SOON, delayMs = SOON_DELAY_MS)

        override fun runAt(timeMs: Long) = enqueueOnce(AFTER_QUOTA, delayMs = (timeMs - System.currentTimeMillis()).coerceAtLeast(0))

        override fun cancelAll() {
            listOf(PERIODIC, SOON, AFTER_QUOTA).forEach(workManager::cancelUniqueWork)
        }

        private fun enqueueOnce(
            name: String,
            delayMs: Long,
        ) {
            val request =
                OneTimeWorkRequestBuilder<YouTubeSyncWorker>()
                    .setConstraints(network)
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                    .setInputData(ProfileWork.data(context))
                    .addTag(ProfileWork.tag(context))
                    .build()
            workManager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
        }

        private companion object {
            const val PERIODIC = "youtube_sync_periodic"
            const val SOON = "youtube_sync_soon"
            const val AFTER_QUOTA = "youtube_sync_after_quota"
            const val PERIOD_HOURS = 6L
            const val BACKOFF_SECONDS = 60L

            /** Lets a burst of edits settle into one run. */
            const val SOON_DELAY_MS = 3_000L
        }
    }
