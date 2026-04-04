package com.example.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fittrack.data.local.entity.StepsEntity

@Dao
interface StepsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(steps: StepsEntity)

    @Query("SELECT * FROM steps WHERE date = :date")
    suspend fun getStepsForDate(date: String): StepsEntity?

    @Query("SELECT * FROM steps WHERE date >= :start AND date <= :end ORDER BY date DESC")
    suspend fun getStepsInRange(start: String, end: String): List<StepsEntity>

    @Query("UPDATE steps SET dailyGoal = :goal WHERE date = :date")
    suspend fun updateGoal(date: String, goal: Int)
}
