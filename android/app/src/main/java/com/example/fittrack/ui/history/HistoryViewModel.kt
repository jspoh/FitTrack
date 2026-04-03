package com.example.fittrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.domain.model.Activity
import com.example.fittrack.domain.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = false,
    val activities: List<Activity> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        val end = LocalDate.now()
        val start = end.minusDays(7)
        loadActivitiesInRange(start.format(formatter), end.format(formatter))
    }

    fun loadActivitiesInRange(start: String, end: String) {
        viewModelScope.launch {
            _uiState.value = HistoryUiState(isLoading = true)
            try {
                val activities = activityRepository.getActivitiesInRange(start, end)
                _uiState.value = HistoryUiState(
                    activities = activities.sortedByDescending { it.start }
                )
            } catch (e: Exception) {
                _uiState.value = HistoryUiState(error = e.message ?: "Failed to load history")
            }
        }
    }

    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            try {
                activityRepository.deleteActivity(id)
                _uiState.value = _uiState.value.copy(
                    activities = _uiState.value.activities
                        .filter { it.id != id }
                        .sortedByDescending { it.start }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
