package com.example.fittrack.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fittrack.data.local.entity.StepsEntity
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
            val weekAgo = DateUtils.formatDate(DateUtils.parseDate(today).minusDays(7))

            // --- Upload: sync all local step records for the last 7 days ---
            val localStepsInRange = stepsDao.getStepsInRange(weekAgo, today).filter { it.steps > 0 }
            if (localStepsInRange.isNotEmpty()) {
                Log.d(TAG, "Uploading ${localStepsInRange.size} local step records")
            }
            for (stepsEntity in localStepsInRange) {
                try {
                    val response = stepsApiService.syncSteps(
                        StepsSyncPayload(stepsEntity.date, stepsEntity.steps)
                    )
                    stepsDao.insert(
                        StepsEntity(response.date, response.steps, dailyGoal = response.dailyGoal ?: stepsEntity.dailyGoal)
                    )
                    Log.d(TAG, "Synced steps for ${stepsEntity.date}: ${stepsEntity.steps} -> server ${response.steps}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync steps for ${stepsEntity.date}", e)
                }
            }

            // --- Download: pull remote steps and insert any dates missing locally ---
            Log.d(TAG, "Fetching remote steps from $weekAgo to $today")
            val remoteStepsList = stepsApiService.getStepsInRange(weekAgo, today)
            Log.d(TAG, "Received ${remoteStepsList.size} remote step records")

            var downloaded = 0
            for (remoteSteps in remoteStepsList) {
                if (remoteSteps.steps <= 0) continue
                val local = stepsDao.getStepsForDate(remoteSteps.date)
                if (local == null) {
                    stepsDao.insert(
                        StepsEntity(remoteSteps.date, remoteSteps.steps, dailyGoal = remoteSteps.dailyGoal ?: 0)
                    )
                    downloaded++
                    Log.d(TAG, "Downloaded steps for ${remoteSteps.date}: ${remoteSteps.steps}")
                }
            }
            if (downloaded > 0) {
                Log.d(TAG, "Downloaded $downloaded missing step records from server")
            }

            Log.d(TAG, "StepsSyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "StepsSyncWorker failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val TAG = "StepsSyncWorker"
        const val WORK_NAME = "steps_sync_worker"
    }
}
