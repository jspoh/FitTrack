package com.example.fittrack.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fittrack.data.local.entity.ActivityEntity
import com.example.fittrack.data.remote.api.ActivityApiService
import com.example.fittrack.data.remote.dto.ActivityLogPayload
import com.example.fittrack.core.utils.DateUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActivitySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "ActivitySyncWorker started. Attempt: ${runAttemptCount + 1}")
        try {
            val activityDao = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java
            ).activityDao()

            val activityApiService = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java
            ).activityApiService()

            val unsyncedActivities = activityDao.getUnsyncedActivities()
            if (unsyncedActivities.isNotEmpty()) {
                Log.d(TAG, "Uploading ${unsyncedActivities.size} unsynced activities")
            }
            for (activity in unsyncedActivities) {
                try {
                    val response = activityApiService.logActivity(
                        ActivityLogPayload(
                            start = activity.start,
                            end = activity.end,
                            activityType = activity.activityType,
                            stepsTaken = activity.stepsTaken,
                            maxHr = activity.maxHr,
                            notes = activity.notes
                        )
                    )
                    activityDao.markAsSynced(activity.id, response.id)
                    Log.d(TAG, "Synced activity ${activity.id} -> server id ${response.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync activity ${activity.id}", e)
                }
            }

            val today = DateUtils.today()
            val yesterday = DateUtils.formatDate(DateUtils.parseDate(today).minusDays(1))

            Log.d(TAG, "Fetching activities from $yesterday to $today")
            val remoteActivities = activityApiService.getActivitiesInRange(yesterday, today)
            if (remoteActivities.isNotEmpty()) {
                Log.d(TAG, "Downloading ${remoteActivities.size} activities from server")
                activityDao.insertAll(remoteActivities.map { it.toEntity() })
            }

            Log.d(TAG, "ActivitySyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ActivitySyncWorker failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "ActivitySyncWorker"
        const val WORK_NAME = "activity_sync_worker"
    }
}

private fun com.example.fittrack.data.remote.dto.ActivityResponse.toEntity() = ActivityEntity(
    serverId = id,
    start = start,
    end = end,
    activityType = activityType,
    stepsTaken = stepsTaken,
    maxHr = maxHr,
    notes = notes,
    synced = true
)
