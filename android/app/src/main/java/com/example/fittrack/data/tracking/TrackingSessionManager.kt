package com.example.fittrack.data.tracking

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "fittrack_tracking_session"
        private const val KEY_MANUAL_START_TIME = "manual_start_time"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _manualSessionStartTime = MutableStateFlow(loadManualSessionStartTime())
    val manualSessionStartTime: StateFlow<LocalDateTime?> = _manualSessionStartTime.asStateFlow()

    private val _isManualTracking = MutableStateFlow(_manualSessionStartTime.value != null)
    val isManualTracking: StateFlow<Boolean> = _isManualTracking.asStateFlow()

    fun startManualSession(startTime: LocalDateTime = LocalDateTime.now()) {
        prefs.edit()
            .putString(KEY_MANUAL_START_TIME, startTime.toString())
            .apply()
        _manualSessionStartTime.value = startTime
        _isManualTracking.value = true
    }

    fun stopManualSession(): LocalDateTime? {
        val startTime = _manualSessionStartTime.value
        prefs.edit().remove(KEY_MANUAL_START_TIME).apply()
        _manualSessionStartTime.value = null
        _isManualTracking.value = false
        return startTime
    }

    fun getManualSessionStartTime(): LocalDateTime? = _manualSessionStartTime.value

    private fun loadManualSessionStartTime(): LocalDateTime? {
        val stored = prefs.getString(KEY_MANUAL_START_TIME, null) ?: return null
        return runCatching { LocalDateTime.parse(stored) }.getOrNull()
    }
}
