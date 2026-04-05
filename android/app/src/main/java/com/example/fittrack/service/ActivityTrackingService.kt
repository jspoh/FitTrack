package com.example.fittrack.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.fittrack.MainActivity
import com.example.fittrack.core.utils.DateUtils
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
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class ActivityTrackingService : Service() {

    @Inject lateinit var activityRecognitionManager: ActivityRecognitionManager
    @Inject lateinit var stepCounterManager: StepCounterManager
    @Inject lateinit var trackingSessionManager: TrackingSessionManager
    @Inject lateinit var logActivityUseCase: LogActivityUseCase
    @Inject lateinit var syncStepsUseCase: SyncStepsUseCase
    @Inject lateinit var stepsRepository: StepsRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null
    private var inactivityJob: Job? = null
    private var autoSessionStartTime: LocalDateTime? = null
    private var lastKnownSessionSteps = 0

    companion object {
        private const val TAG = "ActivityAutoStart"
        const val CHANNEL_ID = "fittrack_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val ACTION_STOP_MANUAL = "ACTION_STOP_MANUAL_TRACKING"
        const val ACTION_AUTO_TRACK = "ACTION_AUTO_TRACK"
        const val ACTION_AUTO_START_SESSION = "ACTION_AUTO_START_SESSION"
        const val ACTION_AUTO_STOP_SESSION = "ACTION_AUTO_STOP_SESSION"
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

        fun autoStartSessionIntent(context: Context): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_AUTO_START_SESSION
            }

        fun autoStopSessionIntent(context: Context): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_AUTO_STOP_SESSION
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            TAG,
            "onStartCommand action=${intent?.action} manualActive=${trackingSessionManager.isManualTracking.value} " +
                "autoActive=${activityRecognitionManager.isAutoSessionActive.value}"
        )

        when (intent?.action) {
            ACTION_START -> {
                serviceScope.launch {
                    if (activityRecognitionManager.isAutoSessionActive.value) {
                        Log.d(TAG, "Manual tracking requested while auto session is active")
                        stopAutoSession(stopServiceWhenDone = false)
                    }
                    startManualTracking()
                }
            }

            ACTION_AUTO_TRACK -> {
                Log.d(TAG, "Received legacy auto-track action, re-registering transitions")
                activityRecognitionManager.registerAutoTransitions()
                stopIfIdle()
            }

            ACTION_AUTO_START_SESSION -> startAutoSession()

            ACTION_AUTO_STOP_SESSION -> {
                serviceScope.launch {
                    stopAutoSession()
                }
            }

            ACTION_STOP_MANUAL -> {
                serviceScope.launch {
                    stopManualTracking()
                }
            }

            ACTION_STOP -> {
                serviceScope.launch {
                    stopAutoTrackingFlow()
                }
            }

            else -> Log.w(TAG, "Ignoring unknown service action=${intent?.action}")
        }

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "App removed from recents, stopping service")
        serviceScope.launch {
            if (trackingSessionManager.isManualTracking.value) {
                stopManualTracking()
            }
            stopAutoSession()
        }
    }
    private suspend fun startManualTracking() {
        trackingSessionManager.startManualSession(LocalDateTime.now())
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("FitTrack is tracking your activity"))
        activityRecognitionManager.startTracking()
        stepCounterManager.startCounting()
        Log.d(TAG, "Manual tracking started")
    }

    private fun startAutoSession() {
        if (trackingSessionManager.isManualTracking.value) {
            Log.d(TAG, "Skipping auto-session start because manual tracking is active")
            stopIfIdle()
            return
        }

        if (activityRecognitionManager.isAutoSessionActive.value) {
            Log.d(TAG, "Skipping auto-session start because it is already active")
            updateNotification(buildAutoTrackingText())
            return
        }

        autoSessionStartTime = LocalDateTime.now()
        lastKnownSessionSteps = 0
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(buildAutoTrackingText()))
        activityRecognitionManager.setAutoSessionActive(true)
        activityRecognitionManager.startTracking()
        stepCounterManager.startCounting()
        startAutoStepMonitor()
        resetInactivityTimer()
        Log.d(
            TAG,
            "Auto session started currentActivity=${activityRecognitionManager.currentActivity.value}"
        )
    }

    private fun startAutoStepMonitor() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            stepCounterManager.stepCount.collect { steps ->
                if (!activityRecognitionManager.isAutoSessionActive.value) return@collect
                if (steps > lastKnownSessionSteps) {
                    lastKnownSessionSteps = steps
                    Log.d(TAG, "Auto session steps advanced to $steps, resetting inactivity timer")
                    resetInactivityTimer()
                }
            }
        }
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = serviceScope.launch {
            delay(INACTIVITY_TIMEOUT_MS)
            Log.d(TAG, "Inactivity timeout reached, stopping auto session")
            stopAutoSession()
        }
    }

    private suspend fun stopAutoSession(stopServiceWhenDone: Boolean = true) {
        val start = autoSessionStartTime
        val end = LocalDateTime.now()
        val activityType = activityRecognitionManager.currentActivity.value
        val finalSteps = stepCounterManager.stopCounting()

        monitorJob?.cancel()
        inactivityJob?.cancel()
        monitorJob = null
        inactivityJob = null
        lastKnownSessionSteps = 0

        activityRecognitionManager.stopTracking()
        activityRecognitionManager.setAutoSessionActive(false)
        autoSessionStartTime = null

        if (start != null && finalSteps > 0) {
            Log.d(TAG, "Saving auto session activity=$activityType steps=$finalSteps")
            val result = logActivityUseCase(
                start = DateUtils.formatDateTime(start),
                end = DateUtils.formatDateTime(end),
                activityType = activityType,
                stepsTaken = finalSteps,
                maxHr = 0,
                notes = ""
            )
            result.onSuccess {
                Log.d(TAG, "Saved auto session successfully")
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to save auto session", throwable)
            }
            if (result.isSuccess) {
                syncTodaySteps(finalSteps)
            }
        } else {
            Log.d(
                TAG,
                "Skipping auto-session save start=$start steps=$finalSteps activity=$activityType"
            )
        }

        if (stopServiceWhenDone) {
            stopIfIdle()
        }
    }

    private suspend fun stopManualTracking() {
        val start = trackingSessionManager.getManualSessionStartTime()
        val end = LocalDateTime.now()
        val activityType = activityRecognitionManager.currentActivity.value
        val finalSteps = stepCounterManager.stopCounting()

        monitorJob?.cancel()
        inactivityJob?.cancel()
        trackingSessionManager.stopManualSession()
        activityRecognitionManager.stopTracking()

        Log.d(TAG, "Manual tracking stopped start=$start end=$end type=$activityType steps=$finalSteps")
        stopIfIdle()
    }

    private suspend fun stopAutoTrackingFlow() {
        Log.d(TAG, "Stopping auto tracking flow")
        stopAutoSession(stopServiceWhenDone = false)
        if (!trackingSessionManager.isManualTracking.value) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopIfIdle() {
        if (trackingSessionManager.isManualTracking.value || activityRecognitionManager.isAutoSessionActive.value) {
            Log.d(TAG, "Service remains active because a session is still running")
            return
        }

        Log.d(TAG, "No active session remains, stopping foreground service")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun syncTodaySteps(sessionStepsFallback: Int) {
        val today = DateUtils.today()
        val syncedSteps = stepsRepository.getStepsForDate(today)?.steps ?: 0
        val sensorSteps = stepCounterManager.dailySteps.value
        val stepsToSync = maxOf(sensorSteps, syncedSteps + sessionStepsFallback)
        Log.d(
            TAG,
            "Syncing steps today=$today sensorSteps=$sensorSteps syncedSteps=$syncedSteps " +
                "sessionFallback=$sessionStepsFallback resolved=$stepsToSync"
        )
        syncStepsUseCase(today, stepsToSync)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
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
            Log.d(TAG, "Notification channel ensured")
        }
    }

    private fun buildNotification(text: String): android.app.Notification {
        if (!activityRecognitionManager.hasNotificationPermission()) {
            Log.w(TAG, "Notification permission is not granted; foreground notification visibility may be limited")
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntentAction = if (trackingSessionManager.isManualTracking.value) {
            stopManualIntent(this)
        } else {
            stopIntent(this)
        }
        val stopIntent = PendingIntent.getService(
            this, 1,
            stopIntentAction,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FitTrack")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
        Log.d(TAG, "Updated notification text=$text")
    }

    private fun buildAutoTrackingText(): String {
        val activityLabel = activityRecognitionManager.currentActivity.value
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        return "FitTrack detected $activityLabel. Tracking in progress"
    }
}
