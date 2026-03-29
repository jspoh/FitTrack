package com.example.fittrack.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fittrack.data.worker.ActivitySyncWorker
import com.example.fittrack.data.worker.StepsSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync() {
        Log.d(TAG, "Scheduling periodic sync: Steps every 15min, Activities every 30min")

        val stepsSyncRequest = PeriodicWorkRequestBuilder<StepsSyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .build()

        val activitySyncRequest = PeriodicWorkRequestBuilder<ActivitySyncWorker>(
            repeatInterval = 30,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            StepsSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            stepsSyncRequest
        )

        workManager.enqueueUniquePeriodicWork(
            ActivitySyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            activitySyncRequest
        )

        Log.d(TAG, "Periodic sync scheduled successfully")
    }

    fun triggerImmediateSync() {
        Log.d(TAG, "Triggering immediate sync")

        val stepsSyncRequest = OneTimeWorkRequestBuilder<StepsSyncWorker>()
            .setConstraints(networkConstraints)
            .build()

        val activitySyncRequest = OneTimeWorkRequestBuilder<ActivitySyncWorker>()
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniqueWork(
            "${StepsSyncWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            stepsSyncRequest
        )

        workManager.enqueueUniqueWork(
            "${ActivitySyncWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            activitySyncRequest
        )
    }

    fun triggerStepsSync() {
        Log.d(TAG, "Triggering immediate steps sync")

        val stepsSyncRequest = OneTimeWorkRequestBuilder<StepsSyncWorker>()
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniqueWork(
            "${StepsSyncWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            stepsSyncRequest
        )
    }

    fun triggerActivitySync() {
        Log.d(TAG, "Triggering immediate activity sync")

        val activitySyncRequest = OneTimeWorkRequestBuilder<ActivitySyncWorker>()
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniqueWork(
            "${ActivitySyncWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            activitySyncRequest
        )
    }

    fun cancelAllSync() {
        Log.d(TAG, "Cancelling all sync")
        workManager.cancelUniqueWork(StepsSyncWorker.WORK_NAME)
        workManager.cancelUniqueWork(ActivitySyncWorker.WORK_NAME)
    }

    companion object {
        const val TAG = "SyncManager"
    }
}
