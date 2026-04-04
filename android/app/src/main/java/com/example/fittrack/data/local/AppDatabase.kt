package com.example.fittrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fittrack.data.local.dao.ActivityDao
import com.example.fittrack.data.local.dao.StepsDao
import com.example.fittrack.data.local.entity.ActivityEntity
import com.example.fittrack.data.local.entity.StepsEntity

@Database(
    entities = [ActivityEntity::class, StepsEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun stepsDao(): StepsDao
}
