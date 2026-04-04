package com.example.fittrack.domain.repository

import com.example.fittrack.domain.model.Activity

interface ActivityRepository {
    suspend fun logActivity(
        start: String,
        end: String,
        activityType: String,
        stepsTaken: Int,
        maxHr: Int,
        notes: String,
        activityName: String = ""
    ): Activity

    suspend fun updateActivity(id: Int, activityName: String)
    suspend fun getActivitiesForDate(date: String): List<Activity>

    suspend fun getActivitiesInRange(start: String, end: String): List<Activity>

    suspend fun deleteActivity(id: Int)
}
