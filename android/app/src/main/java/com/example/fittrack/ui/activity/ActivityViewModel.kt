package com.example.fittrack.ui.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.example.fittrack.core.utils.DateUtils
import com.example.fittrack.data.sensors.ActivityRecognitionManager
import com.example.fittrack.data.sensors.StepCounterManager
import com.example.fittrack.data.tracking.TrackingSessionManager
import com.example.fittrack.domain.repository.StepsRepository
import com.example.fittrack.domain.usecase.activity.LogActivityUseCase
import com.example.fittrack.domain.usecase.steps.SyncStepsUseCase
import com.example.fittrack.service.ActivityTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class ActivityUiState(
    val isTracking: Boolean = false,
    val currentActivityType: String = "UNKNOWN",
    val stepCount: Int = 0,
    val elapsedSeconds: Long = 0,
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityRecognitionManager: ActivityRecognitionManager,
    private val stepCounterManager: StepCounterManager,
    private val trackingSessionManager: TrackingSessionManager,
    private val logActivityUseCase: LogActivityUseCase,
    private val syncStepsUseCase: SyncStepsUseCase,
    private val stepsRepository: StepsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            activityRecognitionManager.currentActivity.collect { type ->
                _uiState.value = _uiState.value.copy(currentActivityType = type)
            }
        }
        viewModelScope.launch {
            trackingSessionManager.isManualTracking.collect { tracking ->
                val isTracking = tracking || trackingSessionManager.isAutoTracking.value
                _uiState.value = _uiState.value.copy(isTracking = isTracking)
                if (!isTracking) {
                    timerJob?.cancel()
                    _uiState.value = _uiState.value.copy(elapsedSeconds = 0)
                }
            }
        }
        viewModelScope.launch {
            trackingSessionManager.isAutoTracking.collect { tracking ->
                val isTracking = tracking || trackingSessionManager.isManualTracking.value
                _uiState.value = _uiState.value.copy(isTracking = isTracking)
                if (!isTracking) {
                    timerJob?.cancel()
                    _uiState.value = _uiState.value.copy(elapsedSeconds = 0)
                }
            }
        }
        viewModelScope.launch {
            trackingSessionManager.manualSessionStartTime.collect { startTime ->
                if (startTime != null) {
                    startTimer(startTime)
                }
            }
        }
        viewModelScope.launch {
            trackingSessionManager.autoSessionStartTime.collect { startTime ->
                if (startTime != null) {
                    startTimer(startTime)
                }
            }
        }
        viewModelScope.launch {
            stepCounterManager.stepCount.collect { steps ->
                _uiState.value = _uiState.value.copy(stepCount = steps)
            }
        }
    }

    fun startTracking() {
        _uiState.value = _uiState.value.copy(savedSuccess = false, error = null)
        ContextCompat.startForegroundService(context, ActivityTrackingService.startIntent(context))
    }

    fun stopAndSave() {
        if (trackingSessionManager.isAutoTracking.value) {
            // Auto session — the service handles saving; just request stop and navigate away
            context.startService(ActivityTrackingService.stopIntent(context))
            _uiState.value = _uiState.value.copy(savedSuccess = true)
            return
        }
        val start = trackingSessionManager.getManualSessionStartTime() ?: run {
            _uiState.value = _uiState.value.copy(error = "No active activity to stop")
            return
        }
        val end = LocalDateTime.now()
        val finalSteps = stepCounterManager.stopCounting()
        context.startService(ActivityTrackingService.stopManualIntent(context))

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            logActivityUseCase(
                start = DateUtils.formatDateTime(start),
                end = DateUtils.formatDateTime(end),
                activityType = _uiState.value.currentActivityType,
                stepsTaken = finalSteps,
                maxHr = 0,
                notes = ""
            ).onSuccess {
                syncTodaySteps(finalSteps)
                _uiState.value = _uiState.value.copy(isSaving = false, savedSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = it.message)
            }
        }
    }

    fun hasPermission() = activityRecognitionManager.hasPermission()

    fun hasNotificationPermission() = activityRecognitionManager.hasNotificationPermission()

    private fun startTimer(startTime: LocalDateTime) {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = java.time.Duration.between(startTime, LocalDateTime.now()).seconds
                _uiState.value = _uiState.value.copy(elapsedSeconds = elapsed.coerceAtLeast(0))
                delay(1000)
            }
        }
    }

    private suspend fun syncTodaySteps(sessionStepsFallback: Int) {
        val today = DateUtils.today()
        val syncedSteps = stepsRepository.getStepsForDate(today)?.steps ?: 0
        val sensorSteps = stepCounterManager.dailySteps.value
        val stepsToSync = maxOf(sensorSteps, syncedSteps + sessionStepsFallback)
        syncStepsUseCase(today, stepsToSync)
    }
}
