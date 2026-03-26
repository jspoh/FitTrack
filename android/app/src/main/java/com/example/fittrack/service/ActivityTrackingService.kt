package com.example.fittrack.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.fittrack.data.sensors.ActivityRecognitionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActivityTrackingService : Service() {

    @Inject
    lateinit var activityRecognitionManager: ActivityRecognitionManager

    companion object {
        const val CHANNEL_ID = "fittrack_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"

        fun startIntent(context: Context): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_START
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_STOP
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                createNotificationChannel()
                val notification = buildNotification()
                startForeground(NOTIFICATION_ID, notification)
                activityRecognitionManager.startTracking()
            }
            ACTION_STOP -> {
                activityRecognitionManager.stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

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

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FitTrack")
            .setContentText("FitTrack is tracking your activity")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
}
