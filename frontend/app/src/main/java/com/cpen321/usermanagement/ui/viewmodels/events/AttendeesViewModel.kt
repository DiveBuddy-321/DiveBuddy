package com.cpen321.usermanagement.ui.viewmodels.events

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AttendeesUiState(
    val attendees: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AttendeesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AttendeesViewModel"
    }

    private val _uiState = MutableStateFlow(AttendeesUiState())
    val uiState: StateFlow<AttendeesUiState> = _uiState.asStateFlow()

    fun loadAttendees(attendeeIds: List<String>) {
        if (attendeeIds.isEmpty()) {
            _uiState.value = AttendeesUiState(attendees = emptyList())
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val attendeesList = mutableListOf<User>()
            var errorOccurred = false
            var errorMessage: String? = null

            for (attendeeId in attendeeIds) {
                val result = profileRepository.getProfileById(attendeeId)
                if (result.isSuccess) {
                    result.getOrNull()?.let { user ->
                        attendeesList.add(user)
                    }
                } else {
                    errorOccurred = true
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Failed to load attendee $attendeeId", error)
                    errorMessage = error?.message ?: "Failed to load some attendees"
                }
            }

            _uiState.value = AttendeesUiState(
                attendees = attendeesList,
                isLoading = false,
                error = if (errorOccurred && attendeesList.isEmpty()) errorMessage else null
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

