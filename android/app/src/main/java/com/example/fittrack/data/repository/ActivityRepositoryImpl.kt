package com.example.fittrack.data.repository

import com.example.fittrack.data.local.dao.ActivityDao
import com.example.fittrack.data.local.entity.ActivityEntity
import com.example.fittrack.data.remote.api.ActivityApiService
import com.example.fittrack.data.remote.dto.ActivityLogPayload
import com.example.fittrack.domain.model.Activity
import com.example.fittrack.domain.repository.ActivityRepository
import javax.inject.Inject

class ActivityRepositoryImpl @Inject constructor(
    private val activityApiService: ActivityApiService,
    private val activityDao: ActivityDao
) : ActivityRepository {

    override suspend fun logActivity(
        start: String, end: String, activityType: String,
        stepsTaken: Int, maxHr: Int, notes: String
    ): Activity {
        val localEntity = ActivityEntity(
            start = start,
            end = end,
            activityType = activityType,
            stepsTaken = stepsTaken,
            maxHr = maxHr,
            notes = notes,
            synced = false
        )
        val localId = activityDao.insert(localEntity)

        return try {
            val response = activityApiService.logActivity(
                ActivityLogPayload(start, end, activityType, stepsTaken, maxHr, notes)
            )
            activityDao.markAsSynced(localId.toInt(), response.id)
            Activity(
                id = response.id,
                start = start,
                end = end,
                activityType = activityType,
                stepsTaken = stepsTaken,
                maxHr = maxHr,
                notes = notes
            )
        } catch (e: Exception) {
            Activity(
                id = localId.toInt(),
                start = start,
                end = end,
                activityType = activityType,
                stepsTaken = stepsTaken,
                maxHr = maxHr,
                notes = notes
            )
        }
    }

    override suspend fun getActivitiesForDate(date: String): List<Activity> {
        return try {
            val response = activityApiService.getActivitiesForDate(date)
            activityDao.insertAll(response.map { it.toEntity() })
            response.map { it.toDomain() }
        } catch (e: Exception) {
            activityDao.getActivitiesForDate(date).map { it.toDomain() }
        }
    }

    override suspend fun getActivitiesInRange(start: String, end: String): List<Activity> {
        return try {
            val response = activityApiService.getActivitiesInRange(start, end)
            activityDao.insertAll(response.map { it.toEntity() })
            response.map { it.toDomain() }
        } catch (e: Exception) {
            activityDao.getActivitiesInRange(start, end).map { it.toDomain() }
        }
    }

    override suspend fun deleteActivity(id: Int) {
        activityDao.deleteById(id)
        try {
            activityApiService.deleteActivity(id)
        } catch (e: Exception) {
        }
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

private fun com.example.fittrack.data.remote.dto.ActivityResponse.toDomain() = Activity(
    id = id, start = start, end = end, activityType = activityType,
    stepsTaken = stepsTaken, maxHr = maxHr, notes = notes
)

private fun ActivityEntity.toDomain() = Activity(
    id = serverId ?: id, start = start, end = end, activityType = activityType,
    stepsTaken = stepsTaken, maxHr = maxHr, notes = notes
)
