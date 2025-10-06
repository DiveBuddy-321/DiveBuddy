package com.cpen321.usermanagement.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.Buddy
import com.cpen321.usermanagement.data.repository.BuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuddyUiState(
    val isLoading: Boolean = false,
    val buddies: List<Buddy> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class BuddyViewModel @Inject constructor(
    private val buddyRepository: BuddyRepository
) : ViewModel() {

    companion object {
        private const val TAG = "BuddyViewModel"
    }

    private val _uiState = MutableStateFlow(BuddyUiState())
    val uiState: StateFlow<BuddyUiState> = _uiState.asStateFlow()

    fun fetchBuddies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val result = buddyRepository.getBuddies()

            if (result.isSuccess) {
                val buddies = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    buddies = buddies,
                    successMessage = "Found ${buddies.size} buddies!"
                )
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to fetch buddies", error)
                val errorMessage = error?.message ?: "Failed to fetch buddies"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

