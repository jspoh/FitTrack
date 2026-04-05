package com.example.fittrack.data.sensors

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.fittrack.service.ActivityTrackingService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ActivityRecognitionEntryPoint {
    fun activityRecognitionManager(): ActivityRecognitionManager
}

@Singleton
class ActivityRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentActivity = MutableStateFlow(loadCurrentActivity())
    val currentActivity: StateFlow<String> = _currentActivity.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _isAutoSessionActive = MutableStateFlow(loadAutoSessionActive())
    val isAutoSessionActive: StateFlow<Boolean> = _isAutoSessionActive.asStateFlow()

    private val updatesPendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = ACTION_ACTIVITY_UPDATES
        }
        PendingIntent.getBroadcast(
            context,
            ACTIVITY_UPDATES_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private val transitionsPendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = ACTION_ACTIVITY_TRANSITIONS
        }
        PendingIntent.getBroadcast(
            context,
            ACTIVITY_TRANSITIONS_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun hasPermission(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        Log.d(TAG, "Activity recognition permission granted=$granted sdk=${Build.VERSION.SDK_INT}")
        return granted
    }

    fun hasNotificationPermission(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        Log.d(TAG, "Notification permission granted=$granted sdk=${Build.VERSION.SDK_INT}")
        return granted
    }

    fun setAutoTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_TRACKING_ENABLED, enabled).apply()
        Log.d(TAG, "Persisted auto tracking enabled=$enabled")
        if (!enabled) {
            persistActiveAutoActivities(emptySet())
            if (!_isAutoSessionActive.value) {
                updateActivity(UNKNOWN_ACTIVITY)
            }
        }
    }

    fun isAutoTrackingEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_TRACKING_ENABLED, false)

    fun reconcileAutoSessionState() {
        if (_isAutoSessionActive.value) {
            Log.w(TAG, "Auto session was marked active but app was restarted; resetting stale state")
            setAutoSessionActive(false)
            persistActiveAutoActivities(emptySet())
            updateActivity(UNKNOWN_ACTIVITY)
        }
    }

    fun registerAutoTransitions() {
        if (!hasPermission()) {
            Log.w(TAG, "Skipping transition registration because permission is missing")
            return
        }

        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(buildTransitionRequest(), transitionsPendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Registered transition updates for $AUTO_TRIGGER_ACTIVITY_NAMES")
            }
            .addOnFailureListener { throwable ->
                logTaskFailure("Failed to register transition updates", throwable)
            }
    }

    fun unregisterAutoTransitions() {
        ActivityRecognition.getClient(context)
            .removeActivityTransitionUpdates(transitionsPendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Removed transition updates")
            }
            .addOnFailureListener { throwable ->
                logTaskFailure("Failed to remove transition updates", throwable)
            }
    }

    fun startTracking() {
        if (!hasPermission()) {
            Log.w(TAG, "Skipping live activity updates because permission is missing")
            return
        }

        ActivityRecognition.getClient(context)
            .requestActivityUpdates(LIVE_ACTIVITY_UPDATES_MS, updatesPendingIntent)
            .addOnSuccessListener {
                _isTracking.value = true
                Log.d(TAG, "Registered activity updates for live session labeling")
            }
            .addOnFailureListener { throwable ->
                logTaskFailure("Failed to register activity updates", throwable)
            }
    }

    fun stopTracking() {
        ActivityRecognition.getClient(context)
            .removeActivityUpdates(updatesPendingIntent)
            .addOnSuccessListener {
                _isTracking.value = false
                Log.d(TAG, "Removed activity updates")
            }
            .addOnFailureListener { throwable ->
                logTaskFailure("Failed to remove activity updates", throwable)
            }
        updateActivity(UNKNOWN_ACTIVITY)
    }

    fun updateActivity(activityType: String) {
        prefs.edit().putString(KEY_CURRENT_ACTIVITY, activityType).apply()
        _currentActivity.value = activityType
        Log.d(TAG, "Updated current activity=$activityType")
    }

    fun setAutoSessionActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SESSION_ACTIVE, active).apply()
        _isAutoSessionActive.value = active
        Log.d(TAG, "Persisted auto session active=$active")
    }

    fun handleReceiverIntent(receiverContext: Context, intent: Intent?) {
        if (intent == null) {
            Log.w(TAG, "Receiver invoked with null intent")
            return
        }

        Log.d(TAG, "Receiver invoked action=${intent.action}")

        when {
            ActivityTransitionResult.hasResult(intent) -> {
                val result = ActivityTransitionResult.extractResult(intent)
                if (result == null) {
                    Log.w(TAG, "Transition callback had no extractable result")
                    return
                }
                handleTransitionResult(receiverContext, result)
            }

            ActivityRecognitionResult.hasResult(intent) -> {
                val result = ActivityRecognitionResult.extractResult(intent)
                if (result == null) {
                    Log.w(TAG, "Activity update callback had no extractable result")
                    return
                }
                handleActivityRecognitionResult(result)
            }

            else -> {
                Log.w(TAG, "Receiver invoked without activity payload action=${intent.action}")
            }
        }
    }

    private fun handleActivityRecognitionResult(result: ActivityRecognitionResult) {
        val mostProbable = result.mostProbableActivity
        val activityType = activityTypeToString(mostProbable.type)
        Log.d(
            TAG,
            "Activity update received type=$activityType confidence=${mostProbable.confidence}"
        )
        updateActivity(activityType)
    }

    private fun handleTransitionResult(receiverContext: Context, result: ActivityTransitionResult) {
        if (!isAutoTrackingEnabled()) {
            Log.d(TAG, "Ignoring transition callback because auto tracking is disabled")
            return
        }

        val activeActivities = loadActiveAutoActivities().toMutableSet()
        var lastEnteredActivity: String? = null

        result.transitionEvents.forEach { event ->
            if (event.activityType !in AUTO_TRIGGER_ACTIVITY_TYPES) {
                Log.d(
                    TAG,
                    "Ignoring unsupported transition type=${event.activityType} " +
                        "transition=${transitionTypeToString(event.transitionType)}"
                )
                return@forEach
            }

            val activityName = activityTypeToString(event.activityType)
            when (event.transitionType) {
                ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                    val added = activeActivities.add(activityName)
                    lastEnteredActivity = activityName
                    Log.d(
                        TAG,
                        "Transition ENTER activity=$activityName added=$added active=$activeActivities"
                    )
                }

                ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                    val removed = activeActivities.remove(activityName)
                    Log.d(
                        TAG,
                        "Transition EXIT activity=$activityName removed=$removed active=$activeActivities"
                    )
                }

                else -> {
                    Log.w(
                        TAG,
                        "Ignoring unsupported transition value=${event.transitionType} " +
                            "activity=$activityName"
                    )
                }
            }
        }

        persistActiveAutoActivities(activeActivities)

        if (_isAutoSessionActive.value && result.transitionEvents.isNotEmpty()) {
            Log.d(TAG, "Transition received during active auto session, resetting inactivity timer")
            ActivityTrackingService.resetInactivityTimerIntent(receiverContext).also {
                startServiceSafely(receiverContext, it)
            }
        }

        when {
            activeActivities.isEmpty() -> {
                updateActivity(UNKNOWN_ACTIVITY)
                if (_isAutoSessionActive.value) {
                    Log.d(TAG, "All tracked activities exited, requesting auto-session stop")
                    startServiceSafely(
                        receiverContext,
                        ActivityTrackingService.autoStopSessionIntent(receiverContext)
                    )
                } else {
                    Log.d(TAG, "All tracked activities exited, but no auto session is active")
                }
            }

            else -> {
                lastEnteredActivity?.let(::updateActivity)
                if (_isAutoSessionActive.value) {
                    Log.d(TAG, "Tracked activity still active, auto session already running")
                } else {
                    Log.d(TAG, "Tracked activity entered, requesting auto-session start")
                    startForegroundServiceSafely(
                        receiverContext,
                        ActivityTrackingService.autoStartSessionIntent(receiverContext)
                    )
                }
            }
        }
    }

    private fun buildTransitionRequest(): ActivityTransitionRequest {
        val transitions = AUTO_TRIGGER_ACTIVITY_TYPES.flatMap { activityType ->
            listOf(
                ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }
        return ActivityTransitionRequest(transitions)
    }

    private fun persistActiveAutoActivities(activities: Set<String>) {
        prefs.edit().putStringSet(KEY_ACTIVE_AUTO_ACTIVITIES, activities).apply()
        Log.d(TAG, "Persisted active auto activities=$activities")
    }

    private fun loadActiveAutoActivities(): Set<String> =
        prefs.getStringSet(KEY_ACTIVE_AUTO_ACTIVITIES, emptySet()).orEmpty()

    private fun loadCurrentActivity(): String =
        prefs.getString(KEY_CURRENT_ACTIVITY, UNKNOWN_ACTIVITY) ?: UNKNOWN_ACTIVITY

    private fun loadAutoSessionActive(): Boolean =
        prefs.getBoolean(KEY_AUTO_SESSION_ACTIVE, false)

    private fun startForegroundServiceSafely(receiverContext: Context, intent: Intent) {
        try {
            ContextCompat.startForegroundService(receiverContext, intent)
            Log.d(TAG, "Requested foreground service action=${intent.action}")
        } catch (throwable: Throwable) {
            Log.e(TAG, "Failed to start foreground service action=${intent.action}", throwable)
        }
    }

    private fun startServiceSafely(receiverContext: Context, intent: Intent) {
        try {
            receiverContext.startService(intent)
            Log.d(TAG, "Requested service action=${intent.action}")
        } catch (throwable: Throwable) {
            Log.e(TAG, "Failed to start service action=${intent.action}", throwable)
        }
    }

    private fun logTaskFailure(message: String, throwable: Throwable) {
        val apiCode = (throwable as? ApiException)?.statusCode
        Log.e(TAG, "$message statusCode=$apiCode", throwable)
    }

    companion object {
        private const val TAG = "ActivityAutoStart"
        private const val PREFS_NAME = "fittrack_activity_recognition"
        private const val KEY_CURRENT_ACTIVITY = "current_activity"
        private const val KEY_ACTIVE_AUTO_ACTIVITIES = "active_auto_activities"
        private const val KEY_AUTO_SESSION_ACTIVE = "auto_session_active"
        private const val KEY_AUTO_TRACKING_ENABLED = "auto_tracking_enabled"
        private const val LIVE_ACTIVITY_UPDATES_MS = 2_000L
        private const val ACTIVITY_UPDATES_REQUEST_CODE = 1001
        private const val ACTIVITY_TRANSITIONS_REQUEST_CODE = 1002
        private const val ACTION_ACTIVITY_UPDATES = "com.example.fittrack.action.ACTIVITY_UPDATES"
        private const val ACTION_ACTIVITY_TRANSITIONS =
            "com.example.fittrack.action.ACTIVITY_TRANSITIONS"
        private const val UNKNOWN_ACTIVITY = "UNKNOWN"

        private val AUTO_TRIGGER_ACTIVITY_TYPES = listOf(
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_BICYCLE
        )

        private val AUTO_TRIGGER_ACTIVITY_NAMES = listOf(
            "WALKING",
            "RUNNING",
            "CYCLING"
        )

        fun activityTypeToString(type: Int): String = when (type) {
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.RUNNING -> "RUNNING"
            DetectedActivity.ON_BICYCLE -> "CYCLING"
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            DetectedActivity.STILL -> "STILL"
            else -> UNKNOWN_ACTIVITY
        }

        private fun transitionTypeToString(type: Int): String = when (type) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN"
        }
    }
}

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ActivityRecognitionEntryPoint::class.java
        )
        entryPoint.activityRecognitionManager().handleReceiverIntent(context, intent)
    }
}
