package com.example.fittrack.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fittrack.data.local.dao.StepsDao
import com.example.fittrack.data.local.entity.StepsEntity
import com.example.fittrack.data.remote.api.StepsApiService
import com.example.fittrack.data.remote.dto.StepsSyncPayload
import com.example.fittrack.core.utils.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class StepsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val stepsApiService: StepsApiService,
    private val stepsDao: StepsDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val today = DateUtils.today()
            val localSteps = stepsDao.getStepsForDate(today)

            if (localSteps != null) {
                val response = stepsApiService.syncSteps(StepsSyncPayload(today, localSteps.steps))
                stepsDao.insert(StepsEntity(response.date, response.steps))
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
                    // Continue with next date
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
        const val WORK_NAME = "steps_sync_worker"
    }
}
