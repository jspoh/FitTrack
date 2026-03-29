package com.example.fittrack.domain.repository

import com.example.fittrack.domain.model.Steps

interface StepsRepository {
    suspend fun syncSteps(date: String, steps: Int): Steps
    suspend fun getStepsForDate(date: String): Steps?
    suspend fun getStepsInRange(start: String, end: String): List<Steps>
    suspend fun updateGoal(date: String, goal: Int)
}
