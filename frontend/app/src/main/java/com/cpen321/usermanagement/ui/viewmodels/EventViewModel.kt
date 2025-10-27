package com.cpen321.usermanagement.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.data.remote.dto.CreateEventRequest
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.data.repository.EventRepository
import com.cpen321.usermanagement.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventUiState(
    val isLoading: Boolean = false,
    val events: List<Event> = emptyList(),
    val error: String? = null,
    val isCreatingEvent: Boolean = false,
    val createEventError: String? = null,
    val eventCreated: Boolean = false,
    val isUpdatingEvent: Boolean = false,
    val updateEventError: String? = null,
    val eventUpdated: Boolean = false,
    val isJoiningEvent: Boolean = false,
    val joinEventError: String? = null,
    val eventJoined: Boolean = false,
    val isLeavingEvent: Boolean = false,
    val leaveEventError: String? = null,
    val eventLeft: Boolean = false,
    val isDeletingEvent: Boolean = false,
    val deleteEventError: String? = null,
    val eventDeleted: Boolean = false,
    val currentUser: User? = null,
    val joinSuccessMessage: String? = null,
    val leaveSuccessMessage: String? = null,
    val deleteSuccessMessage: String? = null,
    val updateSuccessMessage: String? = null
)

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "EventViewModel"
    }

    private val _uiState = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
        loadCurrentUser()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Load current user first to ensure we have the latest user info
            loadCurrentUser()
            
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
        loadCurrentUser() // Refresh current user when refreshing events
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                _uiState.value = _uiState.value.copy(currentUser = user)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load current user", e)
            }
        }
    }

    fun isUserAttendingEvent(event: Event): Boolean {
        val currentUser = _uiState.value.currentUser
        val isAttending = currentUser != null && event.attendees.contains(currentUser._id)
        
        Log.d(TAG, "Checking attendance for event ${event.title}:")
        Log.d(TAG, "  Current user: ${currentUser?._id}")
        Log.d(TAG, "  Event attendees: ${event.attendees}")
        Log.d(TAG, "  Is attending: $isAttending")
        
        return isAttending
    }

    fun createEvent(request: CreateEventRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCreatingEvent = true, 
                createEventError = null,
                eventCreated = false
            )
            
            eventRepository.createEvent(request)
                .onSuccess { event ->
                    Log.d(TAG, "Event created successfully: ${event.title}")
                    _uiState.value = _uiState.value.copy(
                        isCreatingEvent = false,
                        eventCreated = true,
                        createEventError = null
                    )
                    // Refresh the events list to include the new event
                    loadEvents()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to create event", error)
                    _uiState.value = _uiState.value.copy(
                        isCreatingEvent = false,
                        createEventError = error.message ?: "Failed to create event"
                    )
                }
        }
    }

    fun clearCreateEventState() {
        _uiState.value = _uiState.value.copy(
            isCreatingEvent = false,
            createEventError = null,
            eventCreated = false
        )
    }

    fun updateEvent(eventId: String, request: CreateEventRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingEvent = true,
                updateEventError = null,
                eventUpdated = false
            )
            
            eventRepository.updateEvent(eventId, request)
                .onSuccess { event ->
                    Log.d(TAG, "Event updated successfully: ${event.title}")
                    _uiState.value = _uiState.value.copy(
                        isUpdatingEvent = false,
                        eventUpdated = true,
                        updateEventError = null,
                        updateSuccessMessage = "Event updated successfully!"
                    )
                    // Refresh the events list to show updated event
                    loadEvents()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to update event", error)
                    _uiState.value = _uiState.value.copy(
                        isUpdatingEvent = false,
                        updateEventError = error.message ?: "Failed to update event"
                    )
                }
        }
    }

    fun clearUpdateEventState() {
        _uiState.value = _uiState.value.copy(
            isUpdatingEvent = false,
            updateEventError = null,
            eventUpdated = false,
            updateSuccessMessage = null
        )
    }

    fun clearUpdateEventFlags() {
        _uiState.value = _uiState.value.copy(
            isUpdatingEvent = false,
            updateEventError = null,
            eventUpdated = false
        )
    }

    fun joinEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isJoiningEvent = true,
                joinEventError = null,
                eventJoined = false
            )
            
            eventRepository.joinEvent(eventId)
                .onSuccess { event ->
                    Log.d(TAG, "Successfully joined event: ${event.title}")
                    _uiState.value = _uiState.value.copy(
                        isJoiningEvent = false,
                        eventJoined = true,
                        joinEventError = null,
                        joinSuccessMessage = "Successfully registered for ${event.title}!"
                    )
                    // Refresh the events list to show updated attendee count
                    loadEvents()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to join event", error)
                    _uiState.value = _uiState.value.copy(
                        isJoiningEvent = false,
                        joinEventError = error.message ?: "Failed to join event"
                    )
                }
        }
    }

    fun clearJoinEventState() {
        _uiState.value = _uiState.value.copy(
            isJoiningEvent = false,
            joinEventError = null,
            eventJoined = false,
            joinSuccessMessage = null
        )
    }

    fun leaveEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLeavingEvent = true,
                leaveEventError = null,
                eventLeft = false
            )
            
            eventRepository.leaveEvent(eventId)
                .onSuccess { event ->
                    Log.d(TAG, "Successfully left event: ${event.title}")
                    _uiState.value = _uiState.value.copy(
                        isLeavingEvent = false,
                        eventLeft = true,
                        leaveEventError = null,
                        leaveSuccessMessage = "Successfully left ${event.title}!"
                    )
                    // Refresh the events list to show updated attendee count
                    loadEvents()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to leave event", error)
                    _uiState.value = _uiState.value.copy(
                        isLeavingEvent = false,
                        leaveEventError = error.message ?: "Failed to leave event"
                    )
                }
        }
    }

    fun clearLeaveEventState() {
        _uiState.value = _uiState.value.copy(
            isLeavingEvent = false,
            leaveEventError = null,
            eventLeft = false,
            leaveSuccessMessage = null
        )
    }

    fun clearJoinEventFlags() {
        _uiState.value = _uiState.value.copy(
            isJoiningEvent = false,
            joinEventError = null,
            eventJoined = false
        )
    }

    fun clearLeaveEventFlags() {
        _uiState.value = _uiState.value.copy(
            isLeavingEvent = false,
            leaveEventError = null,
            eventLeft = false
        )
    }

    fun refreshCurrentUser() {
        loadCurrentUser()
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeletingEvent = true,
                deleteEventError = null,
                eventDeleted = false
            )
            
            eventRepository.deleteEvent(eventId)
                .onSuccess {
                    Log.d(TAG, "Successfully deleted event")
                    _uiState.value = _uiState.value.copy(
                        isDeletingEvent = false,
                        eventDeleted = true,
                        deleteEventError = null,
                        deleteSuccessMessage = "Event deleted successfully!"
                    )
                    // Refresh the events list to remove the deleted event
                    loadEvents()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to delete event", error)
                    _uiState.value = _uiState.value.copy(
                        isDeletingEvent = false,
                        deleteEventError = error.message ?: "Failed to delete event"
                    )
                }
        }
    }

    fun clearDeleteEventState() {
        _uiState.value = _uiState.value.copy(
            isDeletingEvent = false,
            deleteEventError = null,
            eventDeleted = false,
            deleteSuccessMessage = null
        )
    }

    fun clearDeleteEventFlags() {
        _uiState.value = _uiState.value.copy(
            isDeletingEvent = false,
            deleteEventError = null,
            eventDeleted = false
        )
    }

    fun isUserEventCreator(event: Event): Boolean {
        val currentUser = _uiState.value.currentUser
        return currentUser != null && event.createdBy == currentUser._id
    }
}
