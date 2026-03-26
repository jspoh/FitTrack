package com.example.fittrack.data.sensors

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
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

    private val _currentActivity = MutableStateFlow("UNKNOWN")
    val currentActivity: StateFlow<String> = _currentActivity.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun startTracking() {
        if (!hasPermission()) return
        ActivityRecognition.getClient(context)
            .requestActivityUpdates(2000L, pendingIntent)
            .addOnSuccessListener { _isTracking.value = true }
    }

    fun stopTracking() {
        ActivityRecognition.getClient(context)
            .removeActivityUpdates(pendingIntent)
            .addOnSuccessListener { _isTracking.value = false }
        _currentActivity.value = "UNKNOWN"
    }

    fun updateActivity(activityType: String) {
        _currentActivity.value = activityType
    }

    companion object {
        fun activityTypeToString(type: Int): String = when (type) {
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.RUNNING -> "RUNNING"
            DetectedActivity.ON_BICYCLE -> "CYCLING"
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            DetectedActivity.STILL -> "STILL"
            else -> "UNKNOWN"
        }
    }
}

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent) ?: return
            val mostProbable = result.mostProbableActivity
            val activityType = ActivityRecognitionManager.activityTypeToString(mostProbable.type)
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                ActivityRecognitionEntryPoint::class.java
            )
            entryPoint.activityRecognitionManager().updateActivity(activityType)
        }
    }
}
