package com.example.fittrack.ui.activity

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.data.sensors.ActivityRecognitionManager
import com.example.fittrack.data.sensors.StepCounterManager
import com.example.fittrack.data.tracking.TrackingSessionManager
import com.example.fittrack.data.tracking.TrackingSessionSource
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
import java.time.Duration
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
    private val trackingSessionManager: TrackingSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var timerStartTime: LocalDateTime? = null
    private var pendingStopRequest = false

    init {
        viewModelScope.launch {
            activityRecognitionManager.currentActivity.collect { type ->
                _uiState.value = _uiState.value.copy(currentActivityType = type)
            }
        }
        viewModelScope.launch {
            trackingSessionManager.sessionState.collect { session ->
                _uiState.value = _uiState.value.copy(isTracking = session.isTracking)
                if (session.isTracking) {
                    _uiState.value = _uiState.value.copy(savedSuccess = false, error = null)
                    session.startTime?.let(::startTimer)
                } else {
                    timerJob?.cancel()
                    timerJob = null
                    timerStartTime = null
                    _uiState.value = _uiState.value.copy(elapsedSeconds = 0)
                    if (pendingStopRequest) {
                        pendingStopRequest = false
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            savedSuccess = true,
                            error = null
                        )
                    }
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
        pendingStopRequest = false
        _uiState.value = _uiState.value.copy(savedSuccess = false, isSaving = false, error = null)
        ContextCompat.startForegroundService(context, ActivityTrackingService.startIntent(context))
    }

    fun stopAndSave() {
        val session = trackingSessionManager.sessionState.value
        if (!session.isTracking) {
            _uiState.value = _uiState.value.copy(error = "No active activity to stop")
            return
        }

        pendingStopRequest = true
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)
        val stopIntent = when (session.source) {
            TrackingSessionSource.MANUAL -> ActivityTrackingService.stopManualIntent(context)
            TrackingSessionSource.AUTO -> ActivityTrackingService.stopIntent(context)
            null -> null
        }

        if (stopIntent == null) {
            pendingStopRequest = false
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                error = "Unable to determine how to stop this activity"
            )
            return
        }

        context.startService(stopIntent)
    }

    fun hasPermission() = activityRecognitionManager.hasPermission()

    fun hasNotificationPermission() = activityRecognitionManager.hasNotificationPermission()

    private fun startTimer(startTime: LocalDateTime) {
        if (timerJob?.isActive == true && timerStartTime == startTime) return

        timerJob?.cancel()
        timerStartTime = startTime
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = Duration.between(startTime, LocalDateTime.now()).seconds
                _uiState.value = _uiState.value.copy(elapsedSeconds = elapsed.coerceAtLeast(0))
                delay(1000)
            }
        }
    }
}
