package com.example.fittrack.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val AUTO_TRACKING_KEY = booleanPreferencesKey("auto_tracking_enabled")
    }

    val autoTrackingEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[AUTO_TRACKING_KEY] ?: false }

    suspend fun setAutoTracking(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_TRACKING_KEY] = enabled
        }
    }
}
