package com.example.fittrack.data.repository

import com.example.fittrack.data.local.dao.StepsDao
import com.example.fittrack.data.local.entity.StepsEntity
import com.example.fittrack.data.remote.api.StepsApiService
import com.example.fittrack.data.remote.dto.StepsSyncPayload
import com.example.fittrack.domain.model.Steps
import com.example.fittrack.domain.repository.StepsRepository
import javax.inject.Inject

class StepsRepositoryImpl @Inject constructor(
    private val stepsApiService: StepsApiService,
    private val stepsDao: StepsDao
) : StepsRepository {

    override suspend fun syncSteps(date: String, steps: Int): Steps {
        val response = stepsApiService.syncSteps(StepsSyncPayload(date, steps))
        val goal = response.dailyGoal ?: 1000
        stepsDao.insert(StepsEntity(response.date, response.steps, goal))
        return Steps(response.date, response.steps, goal)
    }

    override suspend fun getStepsForDate(date: String): Steps? {
        return try {
            val response = stepsApiService.getStepsForDate(date)
            val goal = response.dailyGoal ?: 1000

            stepsDao.insert(StepsEntity(response.date, response.steps, goal))
            Steps(response.date, response.steps, goal)
        } catch (e: Exception) {
            stepsDao.getStepsForDate(date)?.let { Steps(it.date, it.steps, it.dailyGoal) }
        }
    }

    override suspend fun getStepsInRange(start: String, end: String): List<Steps> {
        return try {
            val response = stepsApiService.getStepsInRange(start, end)
            stepsDao.let { dao ->
                response.forEach { item ->
                    val goal = item.dailyGoal ?: 1000
                    dao.insert(StepsEntity(item.date, item.steps, goal))
                }
            }
            response.map { Steps(it.date, it.steps, it.dailyGoal ?: 10000) }
        } catch (e: Exception) {
            stepsDao.getStepsInRange(start, end).map { Steps(it.date, it.steps, it.dailyGoal) }
        }
    }

    override suspend fun updateGoal(date: String, goal: Int) {
        stepsDao.updateGoal(date, goal) // You'll need to add @Query update to your DAO
    }
}
