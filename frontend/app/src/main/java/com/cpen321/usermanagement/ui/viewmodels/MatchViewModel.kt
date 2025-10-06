package com.cpen321.usermanagement.ui.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
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

data class MatchUiState(
    val isLoading: Boolean = false,
    val currentIndex: Int = 0,
    val name: String = "",
    val age: Int = 0,
    val level: Int = 0,
    val bio: String = "",
    val profilePicture: String? = null,
    val location: String = "",
    val distance: Double = 0.0,
    val hasMoreProfiles: Boolean = false
)

@HiltViewModel
class MatchViewModel @Inject constructor(
    private val buddyRepository: BuddyRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "MatchViewModel"
        private const val KEY_BUDDIES = "buddies"
        private const val KEY_CURRENT_INDEX = "current_index"
    }

    private val _uiState = MutableStateFlow(MatchUiState())
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private var buddies: List<Buddy> = emptyList()
    var onNavigateBack: (() -> Unit)? = null
    var onNavigateToChat: ((String) -> Unit)? = null

    init {
        // Try to restore state if available
        savedStateHandle.get<Int>(KEY_CURRENT_INDEX)?.let { index ->
            loadBuddiesAndShowProfile(index)
        }
    }

    fun initializeWithBuddies(buddiesList: List<Buddy>) {
        buddies = buddiesList
        savedStateHandle[KEY_CURRENT_INDEX] = 0
        showCurrentProfile()
    }

    fun loadBuddiesAndShowProfile(startIndex: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = buddyRepository.getBuddies()
            
            if (result.isSuccess) {
                buddies = result.getOrNull() ?: emptyList()
                savedStateHandle[KEY_CURRENT_INDEX] = startIndex
                showCurrentProfile()
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to load buddies", error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    name = ""
                )
            }
        }
    }

    private fun showCurrentProfile() {
        val currentIndex = savedStateHandle.get<Int>(KEY_CURRENT_INDEX) ?: 0
        
        if (currentIndex < buddies.size) {
            val buddy = buddies[currentIndex]
            val user = buddy.user
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentIndex = currentIndex,
                name = user.name,
                age = user.age ?: 0,
                level = user.level ?: 0,
                bio = user.bio ?: "",
                profilePicture = user.profilePicture,
                location = formatLocation(user.lat, user.long),
                distance = buddy.distance,
                hasMoreProfiles = currentIndex < buddies.size - 1
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentIndex = currentIndex,
                name = "",
                hasMoreProfiles = false
            )
        }
    }

    fun onRejectClick() {
        val currentIndex = savedStateHandle.get<Int>(KEY_CURRENT_INDEX) ?: 0
        val nextIndex = currentIndex + 1
        
        if (nextIndex < buddies.size) {
            savedStateHandle[KEY_CURRENT_INDEX] = nextIndex
            showCurrentProfile()
        } else {
            // No more profiles
            _uiState.value = _uiState.value.copy(
                name = "",
                hasMoreProfiles = false
            )
        }
    }

    fun onBackClick() {
        clearState()
        onNavigateBack?.invoke()
    }
    
    fun clearState() {
        buddies = emptyList()
        _uiState.value = MatchUiState()
    }

    fun onChatClick() {
        val currentIndex = _uiState.value.currentIndex
        if (currentIndex < buddies.size) {
            val userId = buddies[currentIndex].user._id
            onNavigateToChat?.invoke(userId)
            // For now, just log since chat is not implemented
            Log.d(TAG, "Chat clicked for user: $userId")
        }
    }

    private fun formatLocation(lat: Double?, long: Double?): String {
        return if (lat != null && long != null) {
            "%.2f, %.2f".format(lat, long)
        } else {
            "Unknown"
        }
    }
}

