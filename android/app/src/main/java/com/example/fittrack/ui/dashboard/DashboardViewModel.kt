package com.example.fittrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.core.utils.DateUtils
import com.example.fittrack.data.sensors.StepCounterManager
import com.example.fittrack.data.tracking.TrackingSessionManager
import com.example.fittrack.domain.model.Activity
import com.example.fittrack.domain.model.Steps
import com.example.fittrack.domain.repository.ActivityRepository
import com.example.fittrack.domain.repository.StepsRepository
import com.example.fittrack.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isTracking: Boolean = false,
    val todaySteps: Steps? = null,
    val dailyGoal: Int = 10000,
    val recentActivities: List<Activity> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val stepCounterManager: StepCounterManager,
    private val trackingSessionManager: TrackingSessionManager,
    private val stepsRepository: StepsRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var syncedTodaySteps = 0

    init {
        stepCounterManager.startDailyTracking()

        viewModelScope.launch {
            trackingSessionManager.isManualTracking.collect { isTracking ->
                _uiState.value = _uiState.value.copy(isTracking = isTracking)
            }
        }

        viewModelScope.launch {
            stepCounterManager.dailySteps.collect { sensorSteps ->
                updateTodaySteps(sensorSteps)
            }
        }
        loadDashboard()
    }

    override fun onCleared() {
        super.onCleared()
        stepCounterManager.stopDailyTracking()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val today = DateUtils.today()
                val activities = activityRepository.getActivitiesForDate(today)
                    .sortedByDescending { it.start }
                val stepsData = stepsRepository.getStepsForDate(today)
                syncedTodaySteps = stepsRepository.getStepsForDate(today)?.steps ?: 0
                val dailyGoal = stepsData?.dailyGoal ?: 0

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    todaySteps = Steps(today, maxOf(stepCounterManager.dailySteps.value, syncedTodaySteps), dailyGoal = dailyGoal),
                    recentActivities = activities

                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard"
                )
            }
        }
    }

    private fun updateTodaySteps(sensorSteps: Int) {
        val displaySteps = maxOf(sensorSteps, syncedTodaySteps)
        val currentGoal = _uiState.value.todaySteps?.dailyGoal ?: 0
        if (_uiState.value.todaySteps?.steps != displaySteps) {
            _uiState.value = _uiState.value.copy(
                todaySteps = Steps(DateUtils.today(), displaySteps, dailyGoal = currentGoal)
            )
        }
    }

    fun updateDailyGoal(newGoal: Int) {
        viewModelScope.launch {
            try {
                val today = DateUtils.today()

                // 1. Update the Repository (Database)
                // We fetch the current steps so we don't overwrite them with 0
                val currentSteps = _uiState.value.todaySteps?.steps ?: 0
                stepsRepository.syncSteps(today, currentSteps) // Assuming your sync accepts goal or you have a specific updateGoal method

                // 2. Update the UI State immediately
                _uiState.value = _uiState.value.copy(
                    todaySteps = _uiState.value.todaySteps?.copy(dailyGoal = newGoal)
                        ?: Steps(today, currentSteps, newGoal)
                )

                // Optional: If your repository has a specific local update:
                // stepsRepository.updateGoal(today, newGoal)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update goal")
            }
        }
    }
    fun updateActivity(id: Int, name: String) {
        viewModelScope.launch {
            try {
                activityRepository.updateActivity(id, name)
                _uiState.value = _uiState.value.copy(
                    recentActivities = _uiState.value.recentActivities.map { activity ->
                        if (activity.id == id) activity.copy(activityName = name)
                        else activity
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    fun syncNow() {
        syncManager.triggerImmediateSync()
    }
}
