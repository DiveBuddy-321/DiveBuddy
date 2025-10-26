package com.cpen321.usermanagement.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.Message
import com.cpen321.usermanagement.data.repository.ChatRepository
import com.cpen321.usermanagement.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = false,
    val chats: List<Chat> = emptyList(),
    val messagesByChat: Map<String, List<Message>> = emptyMap(),
    val currentUserId: String? = null,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadChats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val profile = profileRepository.getProfile().getOrNull()
            val result = chatRepository.listChats()
            _uiState.value = result.fold(
                onSuccess = { list -> ChatUiState(isLoading = false, chats = list, currentUserId = profile?._id) },
                onFailure = { e -> ChatUiState(isLoading = false, chats = emptyList(), error = e.message, currentUserId = profile?._id) }
            )
        }
    }

    fun loadMessages(chatId: String, limit: Int? = 20, before: String? = null, append: Boolean = false) {
        viewModelScope.launch {
            val result = chatRepository.getMessages(chatId, limit, before)
            result.onSuccess { resp ->
                val updated = _uiState.value.messagesByChat.toMutableMap()
                val existing = updated[chatId] ?: emptyList()
                updated[chatId] = if (append) {
                    existing + resp.messages
                } else {
                    resp.messages
                }
                _uiState.value = _uiState.value.copy(messagesByChat = updated)
            }
        }
    }
}


