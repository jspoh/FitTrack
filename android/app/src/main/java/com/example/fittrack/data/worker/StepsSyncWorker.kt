package com.example.fittrack.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fittrack.data.local.dao.StepsDao
import com.example.fittrack.data.local.entity.StepsEntity
import com.example.fittrack.data.remote.api.StepsApiService
import com.example.fittrack.data.remote.dto.StepsSyncPayload
import com.example.fittrack.core.utils.DateUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StepsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "StepsSyncWorker started. Attempt: ${runAttemptCount + 1}")
        try {
            val stepsDao = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java
            ).stepsDao()

            val stepsApiService = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java
            ).stepsApiService()

            val today = DateUtils.today()
            val localSteps = stepsDao.getStepsForDate(today)

            if (localSteps != null) {
                Log.d(TAG, "Syncing $today: ${localSteps.steps} steps")
                val response = stepsApiService.syncSteps(StepsSyncPayload(today, localSteps.steps))
                stepsDao.insert(StepsEntity(response.date, response.steps))
                Log.d(TAG, "Sync successful for $today")
            } else {
                Log.d(TAG, "No local steps data for $today")
            }

            val yesterday = DateUtils.formatDate(DateUtils.parseDate(today).minusDays(1))
            val unsyncedSteps = stepsDao.getStepsInRange(yesterday, today)
                .filter { it.steps > 0 }

            for (stepsEntity in unsyncedSteps) {
                try {
                    val response = stepsApiService.syncSteps(
                        StepsSyncPayload(stepsEntity.date, stepsEntity.steps)
                    )
                    stepsDao.insert(StepsEntity(response.date, response.steps))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync steps for ${stepsEntity.date}", e)
                }
            }

            Log.d(TAG, "StepsSyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "StepsSyncWorker failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "StepsSyncWorker"
        const val WORK_NAME = "steps_sync_worker"
    }
}
