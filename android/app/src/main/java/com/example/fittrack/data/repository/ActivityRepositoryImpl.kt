package com.example.fittrack.data.repository

import com.example.fittrack.data.local.dao.ActivityDao
import com.example.fittrack.data.local.entity.ActivityEntity
import com.example.fittrack.data.remote.api.ActivityApiService
import com.example.fittrack.data.remote.dto.ActivityLogPayload
import com.example.fittrack.data.remote.dto.ActivityUpdatePayload
import com.example.fittrack.domain.model.Activity
import com.example.fittrack.domain.repository.ActivityRepository
import javax.inject.Inject

class ActivityRepositoryImpl @Inject constructor(
    private val activityApiService: ActivityApiService,
    private val activityDao: ActivityDao
) : ActivityRepository {

    override suspend fun logActivity(
        start: String,
        end: String,
        activityType: String,
        stepsTaken: Int,
        maxHr: Int,
        notes: String,
        activityName: String
    ): Activity {
        val localEntity = ActivityEntity(
            activityName = activityName,
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
            val serverId = activityApiService.logActivity(
                ActivityLogPayload(activityName, start, end, activityType, stepsTaken, maxHr, notes)
            )
            activityDao.markAsSynced(localId.toInt(), serverId.toInt())
            Activity(
                id = serverId.toInt(),
                activityName = activityName,
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

    override suspend fun updateActivity(id: Int, activityName: String) {
        activityDao.updateActivityName(id, activityName)
        try {
            activityApiService.updateActivity(
                ActivityUpdatePayload(id = id, activityName = activityName)
            )
        } catch (e: Exception) {
            // stays updated locally, sync will retry later
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
            // deleted locally, server will catch up
        }
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

private fun com.example.fittrack.data.remote.dto.ActivityResponse.toDomain() = Activity(
    id = id,
    activityName = activityName ?: "",
    start = start,
    end = end,
    activityType = activityType,
    stepsTaken = stepsTaken,
    maxHr = maxHr,
    notes = notes
)

private fun ActivityEntity.toDomain() = Activity(
    id = serverId ?: id,
    activityName = activityName,
    start = start,
    end = end,
    activityType = activityType,
    stepsTaken = stepsTaken,
    maxHr = maxHr,
    notes = notes
)