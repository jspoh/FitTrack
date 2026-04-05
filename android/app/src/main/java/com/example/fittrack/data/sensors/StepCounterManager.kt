package com.example.fittrack.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepCounterManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    companion object {
        private const val KEY_LAST_SENSOR_DATE = "last_sensor_date"
        private const val KEY_LAST_SENSOR_TOTAL = "last_sensor_total"
        private const val KEY_LAST_DAILY_STEPS = "last_daily_steps"
        private const val LEGACY_KEY_BASELINE_DATE = "baseline_date"
        private const val LEGACY_KEY_BASELINE_STEPS = "baseline_steps"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("fittrack_step_counter", Context.MODE_PRIVATE)

    // Session tracking (during an active activity)
    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()
    private var sessionBaseline: Int? = null
    private var sessionSeedSteps = 0
    private var sessionActive = false

    // Daily total tracking (phone's total steps today)
    private val _dailySteps = MutableStateFlow(loadLastDailySteps())
    val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()
    private var dailyTrackingConsumers = 0

    private var isListening = false

    fun isAvailable(): Boolean = stepSensor != null

    fun startCounting(initialSteps: Int = 0) {
        sessionActive = true
        sessionBaseline = loadLastSensorTotal()
        sessionSeedSteps = initialSteps.coerceAtLeast(0)
        _stepCount.value = sessionSeedSteps
        registerListenerIfNeeded()
    }

    fun stopCounting(): Int {
        val finalSteps = _stepCount.value
        sessionActive = false
        sessionSeedSteps = 0
        unregisterIfNotNeeded()
        return finalSteps
    }

    fun startDailyTracking() {
        dailyTrackingConsumers += 1
        registerListenerIfNeeded()
    }

    fun stopDailyTracking() {
        dailyTrackingConsumers = (dailyTrackingConsumers - 1).coerceAtLeast(0)
        unregisterIfNotNeeded()
    }

    private fun registerListenerIfNeeded() {
        if (isListening) return
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            isListening = true
        }
    }

    private fun unregisterIfNotNeeded() {
        if (!sessionActive && dailyTrackingConsumers == 0) {
            sensorManager.unregisterListener(this)
            isListening = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val totalSteps = event.values[0].toInt()

        // Session tracking (only during active session)
        if (sessionActive) {
            if (sessionBaseline == null || totalSteps < (sessionBaseline ?: 0)) {
                sessionBaseline = if (totalSteps < (sessionBaseline ?: 0)) 0 else totalSteps
            }
            _stepCount.value =
                (sessionSeedSteps + totalSteps - (sessionBaseline ?: totalSteps)).coerceAtLeast(0)
        }

        // Daily tracking
        updateDailySteps(totalSteps)
    }

    private fun updateDailySteps(currentSensorSteps: Int) {
        val today = LocalDate.now().toString()
        val lastSensorDate = prefs.getString(KEY_LAST_SENSOR_DATE, null)
        val lastSensorTotal = prefs.getInt(KEY_LAST_SENSOR_TOTAL, -1)

        if (lastSensorDate == null || lastSensorTotal < 0) {
            val migratedDailySteps = migrateLegacyDailySteps(today, currentSensorSteps)
            persistDailyState(today, currentSensorSteps, migratedDailySteps)
            _dailySteps.value = migratedDailySteps
            return
        }

        val lastDailySteps = prefs.getInt(KEY_LAST_DAILY_STEPS, 0)
        val updatedDailySteps = when {
            lastSensorDate == today && currentSensorSteps >= lastSensorTotal ->
                lastDailySteps + (currentSensorSteps - lastSensorTotal)
            lastSensorDate == today ->
                lastDailySteps + currentSensorSteps
            currentSensorSteps >= lastSensorTotal ->
                currentSensorSteps - lastSensorTotal
            else ->
                currentSensorSteps
        }.coerceAtLeast(0)

        persistDailyState(today, currentSensorSteps, updatedDailySteps)
        _dailySteps.value = updatedDailySteps
    }

    private fun migrateLegacyDailySteps(today: String, currentSensorSteps: Int): Int {
        val legacyDate = prefs.getString(LEGACY_KEY_BASELINE_DATE, null)
        val legacyBaseline = prefs.getInt(LEGACY_KEY_BASELINE_STEPS, -1)
        val legacyDailySteps = prefs.getInt(KEY_LAST_DAILY_STEPS, 0)

        return if (legacyDate == today && legacyBaseline >= 0 && currentSensorSteps >= legacyBaseline) {
            maxOf(legacyDailySteps, currentSensorSteps - legacyBaseline)
        } else {
            0
        }
    }

    private fun persistDailyState(date: String, sensorTotal: Int, dailySteps: Int) {
        prefs.edit()
            .putString(KEY_LAST_SENSOR_DATE, date)
            .putInt(KEY_LAST_SENSOR_TOTAL, sensorTotal)
            .putInt(KEY_LAST_DAILY_STEPS, dailySteps)
            .apply()
    }

    private fun loadLastSensorTotal(): Int? {
        val lastSensorTotal = prefs.getInt(KEY_LAST_SENSOR_TOTAL, -1)
        return lastSensorTotal.takeIf { it >= 0 }
    }

    private fun loadLastDailySteps(): Int {
        val today = LocalDate.now().toString()
        return when {
            prefs.getString(KEY_LAST_SENSOR_DATE, null) == today ->
                prefs.getInt(KEY_LAST_DAILY_STEPS, 0)
            prefs.getString(LEGACY_KEY_BASELINE_DATE, null) == today ->
                prefs.getInt(KEY_LAST_DAILY_STEPS, 0)
            else -> 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }
}
