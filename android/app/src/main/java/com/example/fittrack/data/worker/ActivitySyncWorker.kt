package com.example.fittrack.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fittrack.data.local.dao.ActivityDao
import com.example.fittrack.data.local.entity.ActivityEntity
import com.example.fittrack.data.remote.api.ActivityApiService
import com.example.fittrack.core.utils.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class ActivitySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val activityApiService: ActivityApiService,
    private val activityDao: ActivityDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val today = DateUtils.today()
            val yesterday = DateUtils.formatDate(DateUtils.parseDate(today).minusDays(1))

            val remoteActivities = activityApiService.getActivitiesInRange(yesterday, today)
            val localActivities = activityDao.getActivitiesInRange(yesterday, today)
            val localIds = localActivities.map { it.id }.toSet()

            val newRemoteActivities = remoteActivities.filter { it.id !in localIds }
            if (newRemoteActivities.isNotEmpty()) {
                activityDao.insertAll(newRemoteActivities.map { it.toEntity() })
            }

            val unsyncedLocalActivities = localActivities.filter { it.id == 0 }
            for (activity in unsyncedLocalActivities) {
                try {
                    val response = activityApiService.logActivity(
                        com.example.fittrack.data.remote.dto.ActivityLogPayload(
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
                    // Continue with next activity
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "activity_sync_worker"
    }
}

private fun com.example.fittrack.data.remote.dto.ActivityResponse.toEntity() = ActivityEntity(
    id = id, start = start, end = end, activityType = activityType,
    stepsTaken = stepsTaken, maxHr = maxHr, notes = notes
)
