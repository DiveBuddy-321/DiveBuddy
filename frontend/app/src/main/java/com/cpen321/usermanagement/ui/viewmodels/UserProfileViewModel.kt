package com.cpen321.usermanagement.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val isLoading: Boolean = false,
    val isCreatingChat: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    companion object {
        private const val TAG = "UserProfileViewModel"
    }

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    var onNavigateToChat: ((String) -> Unit)? = null

    fun setUser(user: User) {
        _uiState.value = UserProfileUiState(user = user)
    }

    fun clearState() {
        _uiState.value = UserProfileUiState()
    }

    fun onChatClick() {
        val userId = _uiState.value.user?._id ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingChat = true, error = null)
            
            val result = chatRepository.createChat(peerId = userId, name = null)
            _uiState.value = _uiState.value.copy(isCreatingChat = false)
            
            result.onSuccess { chatId ->
                navigateToChat(chatId)
            }.onFailure { error ->
                Log.e(TAG, "Failed to create chat", error)
                // Fall back to navigate by userId if backend failed
                navigateToChat(userId)
            }
        }
    }

    private fun navigateToChat(chatId: String) {
        onNavigateToChat?.invoke(chatId)
    }
}

