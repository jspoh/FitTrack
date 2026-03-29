package com.example.fittrack.data.worker

import com.example.fittrack.data.local.dao.ActivityDao
import com.example.fittrack.data.local.dao.StepsDao
import com.example.fittrack.data.remote.api.ActivityApiService
import com.example.fittrack.data.remote.api.StepsApiService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun stepsDao(): StepsDao
    fun stepsApiService(): StepsApiService
    fun activityDao(): ActivityDao
    fun activityApiService(): ActivityApiService
}
