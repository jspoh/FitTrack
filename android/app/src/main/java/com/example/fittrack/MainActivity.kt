package com.example.fittrack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.fittrack.core.constants.ApiConstants
import com.example.fittrack.data.preferences.SettingsRepository
import com.example.fittrack.data.sensors.ActivityRecognitionManager
import com.example.fittrack.ui.navigation.FitTrackNavGraph
import com.example.fittrack.ui.theme.FitTrackTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ActivityAutoStart"
    }

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var activityRecognitionManager: ActivityRecognitionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(ApiConstants.BASE_URL).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val body = connection.inputStream.bufferedReader().readText()
                Log.d("FitTrack-API", "Status: ${connection.responseCode} - $body")
                connection.disconnect()
            } catch (e: Exception) {
                Log.d("FitTrack-API", "Unreachable: ${e.message}")
            }
        }

        lifecycleScope.launch {
            val autoTrackingEnabled = settingsRepository.autoTrackingEnabled.first()
            activityRecognitionManager.setAutoTrackingEnabled(autoTrackingEnabled)
            if (autoTrackingEnabled) {
                if (activityRecognitionManager.hasPermission()) {
                    Log.d(TAG, "Self-healing transition registration on app launch")
                    activityRecognitionManager.registerAutoTransitions()
                } else {
                    Log.w(TAG, "Auto tracking enabled, but activity recognition permission is missing")
                }
            }
        }

        setContent {
            FitTrackTheme {
                FitTrackNavGraph()
            }
        }
    }
}
