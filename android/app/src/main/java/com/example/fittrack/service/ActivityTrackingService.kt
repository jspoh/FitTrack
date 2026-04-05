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
import com.example.fittrack.data.tracking.TrackingSessionSource
import com.example.fittrack.data.tracking.TrackingSessionState
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
import java.time.Duration
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
    private var lastObservedDailySteps: Int? = null
    private var lastAutoActivityType: String? = null

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
        const val ACTION_RESUME_AUTO_SESSION = "ACTION_RESUME_AUTO_SESSION"
        const val INACTIVITY_TIMEOUT_MS = 60_000L

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

        fun resumeAutoSessionIntent(context: Context): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_RESUME_AUTO_SESSION
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val session = trackingSessionManager.sessionState.value
        Log.d(
            TAG,
            "onStartCommand action=${intent?.action} tracking=${session.isTracking} " +
                "source=${session.source} autoFlow=${activityRecognitionManager.isAutoSessionActive.value}"
        )

        when (intent?.action) {
            ACTION_START -> {
                serviceScope.launch {
                    if (activityRecognitionManager.isAutoSessionActive.value) {
                        Log.d(TAG, "Manual tracking requested while auto flow is active")
                        stopAutoFlow(rearmAfterStop = false, stopServiceWhenDone = false)
                    }
                    startManualTracking()
                }
            }

            ACTION_AUTO_TRACK -> {
                Log.d(TAG, "Received legacy auto-track action, re-registering transitions")
                activityRecognitionManager.registerAutoTransitions()
                stopIfIdle()
            }

            ACTION_AUTO_START_SESSION -> armAutoTracking()

            ACTION_RESUME_AUTO_SESSION -> resumeAutoSession()

            ACTION_AUTO_STOP_SESSION -> {
                serviceScope.launch {
                    stopAutoFlow(rearmAfterStop = false, stopServiceWhenDone = true)
                }
            }

            ACTION_STOP_MANUAL -> {
                serviceScope.launch {
                    stopManualTracking()
                }
            }

            ACTION_STOP -> {
                serviceScope.launch {
                    stopAutoFlow(rearmAfterStop = false, stopServiceWhenDone = true)
                }
            }

            else -> Log.w(TAG, "Ignoring unknown service action=${intent?.action}")
        }

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        val session = trackingSessionManager.sessionState.value
        when {
            session.isTracking && session.source == TrackingSessionSource.MANUAL -> {
                Log.d(TAG, "App removed from recents during manual tracking, stopping manual session")
                serviceScope.launch {
                    stopManualTracking()
                }
            }

            activityRecognitionManager.isAutoSessionActive.value -> {
                Log.d(TAG, "App removed from recents while auto tracking is armed, keeping service alive")
            }

            else -> {
                Log.d(TAG, "App removed from recents while idle, stopping service")
                stopIfIdle()
            }
        }
    }

    private suspend fun startManualTracking() {
        val currentSession = trackingSessionManager.sessionState.value
        if (currentSession.isTracking) {
            Log.d(TAG, "Skipping manual start because a workout is already active source=${currentSession.source}")
            updateNotification(buildNotificationTextForCurrentState())
            return
        }

        trackingSessionManager.startSession(
            source = TrackingSessionSource.MANUAL,
            startTime = LocalDateTime.now(),
            lastStepAt = LocalDateTime.now()
        )
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("FitTrack is tracking your activity"))
        activityRecognitionManager.startTracking()
        stepCounterManager.startCounting()
        Log.d(TAG, "Manual tracking started")
    }

    private fun armAutoTracking() {
        val currentSession = trackingSessionManager.sessionState.value
        if (currentSession.isTracking) {
            Log.d(TAG, "Skipping auto arm because a workout is already active source=${currentSession.source}")
            stopIfIdle()
            return
        }

        if (activityRecognitionManager.isAutoSessionActive.value && monitorJob?.isActive == true) {
            Log.d(TAG, "Auto flow is already armed, refreshing pending notification")
            updateNotification(buildAutoPendingText())
            return
        }

        lastObservedDailySteps = stepCounterManager.dailySteps.value
        updateLastAutoActivityType()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(buildAutoPendingText()))
        activityRecognitionManager.setAutoSessionActive(true)
        activityRecognitionManager.startTracking()
        stepCounterManager.startDailyTracking()
        startAutoStepMonitor()
        Log.d(
            TAG,
            "Auto tracking armed currentActivity=${activityRecognitionManager.currentActivity.value} " +
                "baselineDailySteps=$lastObservedDailySteps"
        )
    }

    private fun resumeAutoSession() {
        val session = trackingSessionManager.sessionState.value
        if (!session.isFreshAutoSession(INACTIVITY_TIMEOUT_MS)) {
            Log.w(TAG, "Ignoring auto-session resume because the persisted session is stale")
            if (session.isTracking && session.source == TrackingSessionSource.AUTO) {
                trackingSessionManager.stopSession()
            }
            activityRecognitionManager.resetAutoSessionState()
            stopIfIdle()
            return
        }

        if (activityRecognitionManager.isAutoSessionActive.value && monitorJob?.isActive == true) {
            Log.d(TAG, "Auto flow already resumed, refreshing inactivity timer")
            session.lastStepAt?.let(::resetInactivityTimer)
            updateNotification(buildAutoTrackingText())
            return
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(buildAutoTrackingText()))
        activityRecognitionManager.setAutoSessionActive(true)
        activityRecognitionManager.startTracking()
        stepCounterManager.startDailyTracking()
        stepCounterManager.startCounting(initialSteps = session.recordedSteps)
        lastObservedDailySteps = stepCounterManager.dailySteps.value
        startAutoStepMonitor()
        session.lastStepAt?.let(::resetInactivityTimer)
        Log.d(
            TAG,
            "Resumed auto session start=${session.startTime} recordedSteps=${session.recordedSteps}"
        )
    }

    private fun startAutoStepMonitor() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            stepCounterManager.dailySteps.collect { currentDailySteps ->
                if (!activityRecognitionManager.isAutoSessionActive.value) return@collect

                val previousDailySteps = lastObservedDailySteps
                lastObservedDailySteps = currentDailySteps
                if (previousDailySteps == null) return@collect

                val delta = (currentDailySteps - previousDailySteps).coerceAtLeast(0)
                if (delta <= 0) return@collect

                handleAutoStepDelta(delta)
            }
        }
    }

    private fun handleAutoStepDelta(delta: Int) {
        val currentSession = trackingSessionManager.sessionState.value
        val now = LocalDateTime.now()

        when {
            !currentSession.isTracking -> {
                updateLastAutoActivityType()
                trackingSessionManager.startSession(
                    source = TrackingSessionSource.AUTO,
                    startTime = now,
                    lastStepAt = now,
                    recordedSteps = delta
                )
                stepCounterManager.startCounting(initialSteps = delta)
                updateNotification(buildAutoTrackingText())
                resetInactivityTimer(now)
                Log.d(TAG, "Started auto workout from background steps delta=$delta")
            }

            currentSession.source == TrackingSessionSource.AUTO -> {
                updateLastAutoActivityType()
                val updatedSession = trackingSessionManager.incrementRecordedSteps(delta, now)
                resetInactivityTimer(updatedSession.lastStepAt ?: now)
                Log.d(
                    TAG,
                    "Auto workout steps advanced delta=$delta total=${updatedSession.recordedSteps}"
                )
            }

            else -> {
                Log.d(TAG, "Ignoring auto step delta because a manual workout is active")
            }
        }
    }

    private fun resetInactivityTimer(lastStepAt: LocalDateTime) {
        inactivityJob?.cancel()
        val elapsedMs = Duration.between(lastStepAt, LocalDateTime.now()).toMillis()
        val remainingMs = INACTIVITY_TIMEOUT_MS - elapsedMs

        if (remainingMs <= 0L) {
            serviceScope.launch {
                handleAutoInactivityTimeout()
            }
            return
        }

        inactivityJob = serviceScope.launch {
            delay(remainingMs)
            Log.d(TAG, "Inactivity timeout reached, finishing auto workout")
            handleAutoInactivityTimeout()
        }
    }

    private suspend fun handleAutoInactivityTimeout() {
        val shouldRearm =
            activityRecognitionManager.isAutoTrackingEnabled() &&
                activityRecognitionManager.hasActiveAutoActivities() &&
                trackingSessionManager.sessionState.value.source == TrackingSessionSource.AUTO

        stopAutoFlow(
            rearmAfterStop = shouldRearm,
            stopServiceWhenDone = !shouldRearm
        )
    }

    private suspend fun stopAutoFlow(
        rearmAfterStop: Boolean,
        stopServiceWhenDone: Boolean
    ) {
        val session = trackingSessionManager.sessionState.value
        val hasActiveAutoWorkout =
            session.isTracking && session.source == TrackingSessionSource.AUTO
        val hasAutoFlowActive = activityRecognitionManager.isAutoSessionActive.value || hasActiveAutoWorkout

        if (!hasAutoFlowActive) {
            Log.d(TAG, "Ignoring auto stop because no auto flow is active")
            if (stopServiceWhenDone) {
                stopIfIdle()
            }
            return
        }

        if (hasActiveAutoWorkout) {
            val finalSteps = stepCounterManager.stopCounting()
            val completedSession = trackingSessionManager.stopSession()
            saveCompletedSession(
                session = completedSession.copy(recordedSteps = maxOf(finalSteps, completedSession.recordedSteps)),
                activityType = resolveAutoActivityType(),
                allowZeroStepSave = false
            )
        }

        inactivityJob?.cancel()
        inactivityJob = null

        val shouldKeepArmed =
            rearmAfterStop &&
                activityRecognitionManager.isAutoTrackingEnabled() &&
                activityRecognitionManager.hasActiveAutoActivities() &&
                !trackingSessionManager.sessionState.value.isTracking

        if (shouldKeepArmed) {
            lastObservedDailySteps = stepCounterManager.dailySteps.value
            updateNotification(buildAutoPendingText())
            Log.d(TAG, "Auto workout finished after inactivity; re-arming background monitoring")
            return
        }

        teardownAutoTracking(stopServiceWhenDone)
    }

    private suspend fun stopManualTracking() {
        val session = trackingSessionManager.sessionState.value
        if (!session.isTracking || session.source != TrackingSessionSource.MANUAL) {
            Log.d(TAG, "Ignoring manual stop because no manual session is active")
            stopIfIdle()
            return
        }

        val activityType = activityRecognitionManager.currentActivity.value
        val finalSteps = stepCounterManager.stopCounting()
        val completedSession = trackingSessionManager.stopSession()

        saveCompletedSession(
            session = completedSession.copy(recordedSteps = maxOf(finalSteps, completedSession.recordedSteps)),
            activityType = activityType,
            allowZeroStepSave = true
        )
        activityRecognitionManager.stopTracking()

        Log.d(
            TAG,
            "Manual tracking stopped start=${completedSession.startTime} " +
                "steps=${maxOf(finalSteps, completedSession.recordedSteps)}"
        )
        stopIfIdle()
    }

    private suspend fun saveCompletedSession(
        session: TrackingSessionState,
        activityType: String,
        allowZeroStepSave: Boolean
    ) {
        val start = session.startTime
        val finalSteps = session.recordedSteps
        val end = LocalDateTime.now()

        if (start == null || session.source == null) {
            Log.d(TAG, "Skipping session save because start or source is missing: $session")
            return
        }

        if (finalSteps <= 0 && !allowZeroStepSave) {
            Log.d(TAG, "Skipping auto-session save because no steps were recorded")
            return
        }

        Log.d(
            TAG,
            "Saving ${session.source} session activity=$activityType steps=$finalSteps start=$start end=$end"
        )
        val result = logActivityUseCase(
            start = DateUtils.formatDateTime(start),
            end = DateUtils.formatDateTime(end),
            activityType = activityType,
            stepsTaken = finalSteps,
            maxHr = 0,
            notes = ""
        )
        result.onSuccess {
            Log.d(TAG, "Saved ${session.source} session successfully")
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to save ${session.source} session", throwable)
        }
        if (result.isSuccess && finalSteps > 0) {
            syncTodaySteps(finalSteps)
        }
    }

    private fun teardownAutoTracking(stopServiceWhenDone: Boolean) {
        monitorJob?.cancel()
        inactivityJob?.cancel()
        monitorJob = null
        inactivityJob = null
        lastObservedDailySteps = null

        stepCounterManager.stopDailyTracking()
        activityRecognitionManager.stopTracking()
        activityRecognitionManager.setAutoSessionActive(false)
        lastAutoActivityType = null

        val session = trackingSessionManager.sessionState.value
        if (session.isTracking && session.source == TrackingSessionSource.AUTO) {
            trackingSessionManager.stopSession()
        }

        if (stopServiceWhenDone) {
            stopIfIdle()
        }
    }

    private fun stopIfIdle() {
        val session = trackingSessionManager.sessionState.value
        if (session.isTracking || activityRecognitionManager.isAutoSessionActive.value) {
            Log.d(TAG, "Service remains active because tracking is still running")
            return
        }

        Log.d(TAG, "No active tracking remains, stopping foreground service")
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
        val stopIntentAction =
            if (trackingSessionManager.sessionState.value.source == TrackingSessionSource.MANUAL) {
                stopManualIntent(this)
            } else {
                stopIntent(this)
            }
        val stopIntent = PendingIntent.getService(
            this,
            1,
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

    private fun buildNotificationTextForCurrentState(): String {
        val session = trackingSessionManager.sessionState.value
        return when {
            session.isTracking && session.source == TrackingSessionSource.AUTO -> buildAutoTrackingText()
            activityRecognitionManager.isAutoSessionActive.value -> buildAutoPendingText()
            else -> "FitTrack is tracking your activity"
        }
    }

    private fun updateLastAutoActivityType() {
        val currentActivity = activityRecognitionManager.currentActivity.value
        if (currentActivity != "UNKNOWN") {
            lastAutoActivityType = currentActivity
        }
    }

    private fun resolveAutoActivityType(): String =
        activityRecognitionManager.currentActivity.value
            .takeUnless { it == "UNKNOWN" }
            ?: lastAutoActivityType
            ?: "UNKNOWN"

    private fun buildAutoPendingText(): String =
        "FitTrack detected ${formatCurrentActivityLabel()}. Waiting for movement to start a workout"

    private fun buildAutoTrackingText(): String =
        "FitTrack detected ${formatCurrentActivityLabel()}. Tracking in progress"

    private fun formatCurrentActivityLabel(): String =
        activityRecognitionManager.currentActivity.value
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
}
