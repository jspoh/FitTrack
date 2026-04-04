package com.example.fittrack.domain.usecase.activity

import com.example.fittrack.core.utils.generateActivityName
import com.example.fittrack.domain.model.Activity
import com.example.fittrack.domain.repository.ActivityRepository
import javax.inject.Inject

class LogActivityUseCase @Inject constructor(
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(
        start: String,
        end: String,
        activityType: String,
        stepsTaken: Int,
        maxHr: Int,
        notes: String,
        activityName: String = generateActivityName(activityType)  // ← default generated here
    ): Result<Activity> = runCatching {
        activityRepository.logActivity(
            activityName = activityName,
            start = start,
            end = end,
            activityType = activityType,
            stepsTaken = stepsTaken,
            maxHr = maxHr,
            notes = notes
        )
    }
}
