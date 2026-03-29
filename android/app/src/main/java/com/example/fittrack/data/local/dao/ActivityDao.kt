package com.example.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fittrack.data.local.entity.ActivityEntity

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<ActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityEntity): Long

    @Update
    suspend fun update(activity: ActivityEntity)

    @Query("SELECT * FROM activities WHERE start LIKE :date || '%' ORDER BY start DESC")
    suspend fun getActivitiesForDate(date: String): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE start >= :start AND start <= :end ORDER BY start DESC")
    suspend fun getActivitiesInRange(start: String, end: String): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE synced = 0 ORDER BY start DESC")
    suspend fun getUnsyncedActivities(): List<ActivityEntity>

    @Query("UPDATE activities SET synced = 1, server_id = :serverId WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, serverId: Int)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM activities WHERE synced = 0 AND id = :id")
    suspend fun deleteUnsyncedById(id: Int)

    @Query("SELECT * FROM activities ORDER BY start DESC LIMIT :limit")
    suspend fun getRecentActivities(limit: Int = 10): List<ActivityEntity>
}
