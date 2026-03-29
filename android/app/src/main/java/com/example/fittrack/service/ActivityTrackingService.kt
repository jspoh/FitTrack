package com.example.fittrack.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.fittrack.core.utils.DateUtils
import com.example.fittrack.data.preferences.SettingsRepository
import com.example.fittrack.data.sensors.ActivityRecognitionManager
import com.example.fittrack.data.sensors.StepCounterManager
import com.example.fittrack.data.tracking.TrackingSessionManager
import com.example.fittrack.domain.repository.StepsRepository
import com.example.fittrack.domain.usecase.activity.LogActivityUseCase
import com.example.fittrack.domain.usecase.steps.SyncStepsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class ActivityTrackingService : Service() {

    @Inject lateinit var activityRecognitionManager: ActivityRecognitionManager
    @Inject lateinit var stepCounterManager: StepCounterManager
    @Inject lateinit var trackingSessionManager: TrackingSessionManager
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var logActivityUseCase: LogActivityUseCase
    @Inject lateinit var syncStepsUseCase: SyncStepsUseCase
    @Inject lateinit var stepsRepository: StepsRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null
    private var inactivityJob: Job? = null
    private var autoSessionActive = false
    private var autoSessionStartTime: LocalDateTime? = null
    private var lastKnownDailySteps = 0

    companion object {
        const val CHANNEL_ID = "fittrack_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val ACTION_STOP_MANUAL = "ACTION_STOP_MANUAL_TRACKING"
        const val ACTION_AUTO_TRACK = "ACTION_AUTO_TRACK"
        private const val INACTIVITY_TIMEOUT_MS = 60_000L

        fun startIntent(context: Context): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_START
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_STOP
            }

        fun stopManualIntent(context: Context): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_STOP_MANUAL
            }

        fun autoTrackIntent(context: Context): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_AUTO_TRACK
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                cancelAutoMonitoring()
                trackingSessionManager.startManualSession(LocalDateTime.now())
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, buildNotification("FitTrack is tracking your activity"))
                activityRecognitionManager.startTracking()
                stepCounterManager.startCounting()
            }
            ACTION_AUTO_TRACK -> {
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, buildNotification("FitTrack is monitoring for activity"))
                startAutoMonitoring()
            }
            ACTION_STOP_MANUAL -> {
                serviceScope.launch {
                    stopManualTracking()
                    if (settingsRepository.autoTrackingEnabled.first()) {
                        updateNotification("FitTrack is monitoring for activity")
                        startAutoMonitoring()
                    } else {
                        stepCounterManager.stopDailyTracking()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
            ACTION_STOP -> {
                serviceScope.launch {
                    stopAllTracking()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun startAutoMonitoring() {
        monitorJob?.cancel()
        inactivityJob?.cancel()
        stepCounterManager.startDailyTracking()
        lastKnownDailySteps = stepCounterManager.dailySteps.value
        monitorJob = serviceScope.launch {
            stepCounterManager.dailySteps.collect { steps ->
                if (steps > lastKnownDailySteps) {
                    lastKnownDailySteps = steps
                    if (!autoSessionActive) {
                        startAutoSession()
                    }
                    resetInactivityTimer()
                }
            }
        }
    }

    private fun startAutoSession() {
        autoSessionActive = true
        autoSessionStartTime = LocalDateTime.now()
        activityRecognitionManager.startTracking()
        stepCounterManager.startCounting()
        updateNotification("FitTrack is tracking your activity")
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = serviceScope.launch {
            delay(INACTIVITY_TIMEOUT_MS)
            stopAutoSession()
        }
    }

    private suspend fun stopAutoSession() {
        if (!autoSessionActive) return
        val start = autoSessionStartTime ?: return
        val end = LocalDateTime.now()
        val finalSteps = stepCounterManager.stopCounting()
        activityRecognitionManager.stopTracking()
        autoSessionActive = false

        if (finalSteps > 0) {
            val activityType = activityRecognitionManager.currentActivity.value
            logActivityUseCase(
                start = DateUtils.formatDateTime(start),
                end = DateUtils.formatDateTime(end),
                activityType = activityType,
                stepsTaken = finalSteps,
                maxHr = 0,
                notes = ""
            ).onSuccess {
                syncTodaySteps(finalSteps)
            }
        }

        updateNotification("FitTrack is monitoring for activity")
        lastKnownDailySteps = stepCounterManager.dailySteps.value
    }

    private fun cancelAutoMonitoring() {
        monitorJob?.cancel()
        inactivityJob?.cancel()
        if (autoSessionActive) {
            activityRecognitionManager.stopTracking()
            stepCounterManager.stopCounting()
            autoSessionActive = false
        }
        stepCounterManager.stopDailyTracking()
    }

    private suspend fun stopManualTracking() {
        monitorJob?.cancel()
        inactivityJob?.cancel()
        trackingSessionManager.stopManualSession()
        activityRecognitionManager.stopTracking()
        stepCounterManager.stopCounting()
    }

    private suspend fun stopAllTracking() {
        monitorJob?.cancel()
        inactivityJob?.cancel()
        if (autoSessionActive) {
            stopAutoSession()
        }
        trackingSessionManager.stopManualSession()
        activityRecognitionManager.stopTracking()
        stepCounterManager.stopCounting()
        stepCounterManager.stopDailyTracking()
    }

    private suspend fun syncTodaySteps(sessionStepsFallback: Int) {
        val today = DateUtils.today()
        val syncedSteps = stepsRepository.getStepsForDate(today)?.steps ?: 0
        val sensorSteps = stepCounterManager.dailySteps.value
        val stepsToSync = maxOf(sensorSteps, syncedSteps + sessionStepsFallback)
        syncStepsUseCase(today, stepsToSync)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Activity Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FitTrack")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
