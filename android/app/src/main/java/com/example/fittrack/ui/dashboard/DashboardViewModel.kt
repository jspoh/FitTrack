package com.example.fittrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.core.utils.DateUtils
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
    val todaySteps: Steps? = null,
    val recentActivities: List<Activity> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val stepsRepository: StepsRepository,
    private val activityRepository: ActivityRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState(isLoading = true)
            try {
                val today = DateUtils.today()
                val steps = stepsRepository.getStepsForDate(today)
                val activities = activityRepository.getActivitiesForDate(today)
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    todaySteps = steps,
                    recentActivities = activities
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard"
                )
            }
        }
    }

    fun syncNow() {
        syncManager.triggerImmediateSync()
    }
}
