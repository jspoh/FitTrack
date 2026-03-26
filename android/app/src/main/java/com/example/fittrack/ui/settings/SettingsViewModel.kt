package com.example.fittrack.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.data.preferences.SettingsRepository
import com.example.fittrack.service.ActivityTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val autoTrackingEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        settingsRepository.autoTrackingEnabled
            .map { SettingsUiState(autoTrackingEnabled = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SettingsUiState()
            )

    fun setAutoTracking(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoTracking(enabled)
            if (enabled) {
                context.startForegroundService(ActivityTrackingService.startIntent(context))
            } else {
                context.startService(ActivityTrackingService.stopIntent(context))
            }
        }
    }
}
