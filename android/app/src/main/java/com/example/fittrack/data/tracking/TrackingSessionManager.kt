package com.example.fittrack.data.tracking

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

enum class TrackingSessionSource {
    MANUAL,
    AUTO
}

data class TrackingSessionState(
    val isTracking: Boolean = false,
    val startTime: LocalDateTime? = null,
    val source: TrackingSessionSource? = null,
    val lastStepAt: LocalDateTime? = null,
    val recordedSteps: Int = 0
) {
    fun isFreshAutoSession(
        timeoutMs: Long,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        if (!isTracking || source != TrackingSessionSource.AUTO) return false
        val lastRecordedStep = lastStepAt ?: return false
        return Duration.between(lastRecordedStep, now).toMillis() < timeoutMs
    }
}

@Singleton
class TrackingSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "fittrack_tracking_session"
        private const val KEY_SESSION_ACTIVE = "session_active"
        private const val KEY_SESSION_START_TIME = "session_start_time"
        private const val KEY_SESSION_SOURCE = "session_source"
        private const val KEY_LAST_STEP_AT = "last_step_at"
        private const val KEY_RECORDED_STEPS = "recorded_steps"
        private const val LEGACY_KEY_MANUAL_START_TIME = "manual_start_time"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow(loadSessionState())
    val sessionState: StateFlow<TrackingSessionState> = _sessionState.asStateFlow()

    fun startSession(
        source: TrackingSessionSource,
        startTime: LocalDateTime = LocalDateTime.now(),
        lastStepAt: LocalDateTime? = null,
        recordedSteps: Int = 0
    ) {
        persistSession(
            TrackingSessionState(
                isTracking = true,
                startTime = startTime,
                source = source,
                lastStepAt = lastStepAt,
                recordedSteps = recordedSteps.coerceAtLeast(0)
            )
        )
    }

    fun updateLastStepTimestamp(timestamp: LocalDateTime = LocalDateTime.now()): TrackingSessionState {
        val current = _sessionState.value
        if (!current.isTracking) return current

        val updated = current.copy(lastStepAt = timestamp)
        persistSession(updated)
        return updated
    }

    fun updateRecordedSteps(recordedSteps: Int): TrackingSessionState {
        val current = _sessionState.value
        if (!current.isTracking) return current

        val updated = current.copy(recordedSteps = recordedSteps.coerceAtLeast(0))
        persistSession(updated)
        return updated
    }

    fun incrementRecordedSteps(
        delta: Int,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): TrackingSessionState {
        val current = _sessionState.value
        if (!current.isTracking || delta <= 0) return current

        val updated = current.copy(
            lastStepAt = timestamp,
            recordedSteps = current.recordedSteps + delta
        )
        persistSession(updated)
        return updated
    }

    fun stopSession(): TrackingSessionState {
        val previous = _sessionState.value
        clearPersistedSession()
        _sessionState.value = TrackingSessionState()
        return previous
    }

    fun getSessionState(): TrackingSessionState = _sessionState.value

    fun shouldResumeFreshAutoSession(
        timeoutMs: Long,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean = _sessionState.value.isFreshAutoSession(timeoutMs, now)

    private fun persistSession(state: TrackingSessionState) {
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, state.isTracking)
            .putString(KEY_SESSION_START_TIME, state.startTime?.toString())
            .putString(KEY_SESSION_SOURCE, state.source?.name)
            .putString(KEY_LAST_STEP_AT, state.lastStepAt?.toString())
            .putInt(KEY_RECORDED_STEPS, state.recordedSteps)
            .remove(LEGACY_KEY_MANUAL_START_TIME)
            .apply()
        _sessionState.value = state
    }

    private fun clearPersistedSession() {
        prefs.edit()
            .remove(KEY_SESSION_ACTIVE)
            .remove(KEY_SESSION_START_TIME)
            .remove(KEY_SESSION_SOURCE)
            .remove(KEY_LAST_STEP_AT)
            .remove(KEY_RECORDED_STEPS)
            .remove(LEGACY_KEY_MANUAL_START_TIME)
            .apply()
    }

    private fun loadSessionState(): TrackingSessionState {
        val legacyManualStart = prefs.getString(LEGACY_KEY_MANUAL_START_TIME, null)
        if (legacyManualStart != null) {
            val parsedStart = legacyManualStart.parseDateTime() ?: return TrackingSessionState()
            return TrackingSessionState(
                isTracking = true,
                startTime = parsedStart,
                source = TrackingSessionSource.MANUAL
            )
        }

        val isActive = prefs.getBoolean(KEY_SESSION_ACTIVE, false)
        if (!isActive) return TrackingSessionState()

        val startTime = prefs.getString(KEY_SESSION_START_TIME, null).parseDateTime()
        val source = prefs.getString(KEY_SESSION_SOURCE, null)
            ?.let { value -> runCatching { TrackingSessionSource.valueOf(value) }.getOrNull() }

        if (startTime == null || source == null) {
            clearPersistedSession()
            return TrackingSessionState()
        }

        return TrackingSessionState(
            isTracking = true,
            startTime = startTime,
            source = source,
            lastStepAt = prefs.getString(KEY_LAST_STEP_AT, null).parseDateTime(),
            recordedSteps = prefs.getInt(KEY_RECORDED_STEPS, 0).coerceAtLeast(0)
        )
    }
}

private fun String?.parseDateTime(): LocalDateTime? {
    if (this == null) return null
    return runCatching { LocalDateTime.parse(this) }.getOrNull()
}
