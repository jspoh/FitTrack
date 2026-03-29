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

            val today = DateUtils.today()
            val yesterday = DateUtils.formatDate(DateUtils.parseDate(today).minusDays(1))

            Log.d(TAG, "Fetching activities from $yesterday to $today")
            val remoteActivities = activityApiService.getActivitiesInRange(yesterday, today)
            val localActivities = activityDao.getActivitiesInRange(yesterday, today)
            val localIds = localActivities.map { it.id }.toSet()

            val newRemoteActivities = remoteActivities.filter { it.id !in localIds }
            if (newRemoteActivities.isNotEmpty()) {
                Log.d(TAG, "Downloading ${newRemoteActivities.size} new activities from server")
                activityDao.insertAll(newRemoteActivities.map { it.toEntity() })
            }

            val unsyncedLocalActivities = localActivities.filter { it.id == 0 }
            if (unsyncedLocalActivities.isNotEmpty()) {
                Log.d(TAG, "Uploading ${unsyncedLocalActivities.size} local activities")
            }
            for (activity in unsyncedLocalActivities) {
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
                    activityDao.deleteById(activity.id)
                    activityDao.insertAll(listOf(response.toEntity()))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync activity", e)
                }
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
    id = id, start = start, end = end, activityType = activityType,
    stepsTaken = stepsTaken, maxHr = maxHr, notes = notes
)
