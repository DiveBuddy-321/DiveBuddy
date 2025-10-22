package com.cpen321.usermanagement.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventUiState(
    val isLoading: Boolean = false,
    val events: List<Event> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    companion object {
        private const val TAG = "EventViewModel"
    }

    private val _uiState = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            eventRepository.getAllEvents()
                .onSuccess { events ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        events = events,
                        error = null
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load events", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load events"
                    )
                }
        }
    }

    fun refreshEvents() {
        loadEvents()
    }
}
