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

            // --- Upload: push unsynced local activities to server ---
            val unsyncedActivities = activityDao.getUnsyncedActivities()
            if (unsyncedActivities.isNotEmpty()) {
                Log.d(TAG, "Uploading ${unsyncedActivities.size} unsynced activities")
            }
            for (activity in unsyncedActivities) {
                try {
                    val response = activityApiService.logActivity(
                        ActivityLogPayload(
                            activityName = activity.activityName,
                            start = activity.start,
                            end = activity.end,
                            activityType = activity.activityType,
                            stepsTaken = activity.stepsTaken,
                            maxHr = activity.maxHr,
                            notes = activity.notes
                        )
                    )
                    activityDao.markAsSynced(activity.id, response.id)
                    Log.d(TAG, "Uploaded activity ${activity.id} -> server id ${response.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload activity ${activity.id}", e)
                }
            }

            // --- Download: pull remote activities for last 7 days and insert missing ones ---
            val today = DateUtils.today()
            val weekAgo = DateUtils.formatDate(DateUtils.parseDate(today).minusDays(7))
            Log.d(TAG, "Fetching remote activities from $weekAgo to $today")

            val remoteActivities = activityApiService.getActivitiesInRange(weekAgo, today)
            Log.d(TAG, "Received ${remoteActivities.size} remote activities")

            var downloaded = 0
            for (remoteActivity in remoteActivities) {
                val existing = activityDao.getActivityByServerId(remoteActivity.id)
                if (existing == null) {
                    activityDao.insert(remoteActivity.toEntity())
                    downloaded++
                }
            }
            if (downloaded > 0) {
                Log.d(TAG, "Downloaded $downloaded new activities from server")
            }

            Log.d(TAG, "ActivitySyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ActivitySyncWorker failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val TAG = "ActivitySyncWorker"
        const val WORK_NAME = "activity_sync_worker"
    }
}

private fun com.example.fittrack.data.remote.dto.ActivityResponse.toEntity() = ActivityEntity(
    serverId = id,
    activityName = activityName ?: "",
    start = start,
    end = end,
    activityType = activityType,
    stepsTaken = stepsTaken,
    maxHr = maxHr,
    notes = notes,
    synced = true
)
