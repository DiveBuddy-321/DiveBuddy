package com.cpen321.usermanagement.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.data.repository.AuthRepository
import com.cpen321.usermanagement.data.repository.ChatRepository
import com.cpen321.usermanagement.data.repository.ProfileRepository
import com.cpen321.usermanagement.utils.ChatUtils.getOtherParticipantId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = false,
    val chats: List<Chat> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private val userCache = mutableMapOf<String, User>()

    init {
        loadCurrentUserId()
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                currentUserId = user?._id
                Log.d(TAG, "Current user ID loaded: $currentUserId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load current user ID", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load user information"
                )
            }
        }
    }

    fun loadChats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = chatRepository.listChats()
                result.fold(
                    onSuccess = { chats ->
                        val chatsWithNames = resolveChatNames(chats)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            chats = chatsWithNames
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to load chats", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load chats"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading chats", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private suspend fun resolveChatNames(chats: List<Chat>): List<Chat> {
        if (currentUserId == null) {
            Log.w(TAG, "Current user ID is null, cannot resolve chat names")
            return chats
        }

        return chats.map { chat ->
            if (chat.name != null) {
                // Chat already has a name, return as is
                chat
            } else {
                // Resolve the other participant's name
                val otherParticipantId = getOtherParticipantId(chat.participants, currentUserId!!)
                if (otherParticipantId != null) {
                    val otherUser = getUserFromCache(otherParticipantId)
                    if (otherUser != null) {
                        chat.copy(name = otherUser.name)
                    } else {
                        chat
                    }
                } else {
                    chat
                }
            }
        }
    }

    private suspend fun getUserFromCache(userId: String): User? {
        return userCache[userId] ?: run {
            try {
                val result = profileRepository.getProfileById(userId)
                result.fold(
                    onSuccess = { user ->
                        userCache[userId] = user
                        user
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to fetch user $userId", error)
                        null
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception while fetching user $userId", e)
                null
            }
        }
    }
}