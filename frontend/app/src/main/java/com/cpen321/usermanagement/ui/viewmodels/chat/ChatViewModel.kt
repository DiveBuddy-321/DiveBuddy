package com.cpen321.usermanagement.ui.viewmodels.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.Message
import com.cpen321.usermanagement.data.repository.AuthRepository
import com.cpen321.usermanagement.data.repository.ChatRepository
import com.cpen321.usermanagement.data.repository.ProfileRepository
import com.cpen321.usermanagement.data.socket.SocketManager
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
    val userNames: Map<String, String> = emptyMap(), // userId -> userName mapping
    val profilePictures: Map<String, String?> = emptyMap(), // userId -> profilePicture path
    val error: String? = null,
    val isSocketConnected: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    private val socketManager: SocketManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    init {
        // Connect to WebSocket on ViewModel creation
        viewModelScope.launch {
            authRepository.getStoredToken()?.let { token ->
                socketManager.connect(token)
            }
        }
        

        // Listen to socket connection state
        viewModelScope.launch {
            socketManager.connectionStateFlow.collect { isConnected ->
                _uiState.value = _uiState.value.copy(isSocketConnected = isConnected)
            }
        }
        
        // Listen to new messages
        viewModelScope.launch {
            socketManager.newMessageFlow.collect { event ->
                handleNewMessage(event.chatId, event.message)
            }
        }
        
        // Listen to chat updates (for chats not currently open)
        viewModelScope.launch {
            socketManager.chatUpdatedFlow.collect { event ->
                // Could update the chat list here if needed
            }
        }
        
        // Listen to socket errors
        viewModelScope.launch {
            socketManager.errorFlow.collect { event ->
                _uiState.value = _uiState.value.copy(error = event.message)
            }
        }
    }
    
    private fun handleNewMessage(chatId: String, message: Message) {
        val updated = _uiState.value.messagesByChat.toMutableMap()
        val existing = updated[chatId] ?: emptyList()
        
        // Check if message already exists (to avoid duplicates)
        if (existing.any { it._id == message._id }) {
            return
        }
        
        // Add new message to the front (newest first)
        updated[chatId] = listOf(message) + existing
        _uiState.value = _uiState.value.copy(messagesByChat = updated)
    }

    fun loadChats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val profile = profileRepository.getProfile().getOrNull()
            val result = chatRepository.listChats()
            
            result.fold(
                onSuccess = { chatList ->
                    // Get all unique participant IDs (excluding current user)
                    val participantIds = chatList.flatMap { chat ->
                        chat.participants.filter { it != profile?._id }
                    }.distinct()
                    
                    // Fetch user names for all participants
                    val userNamesMap = mutableMapOf<String, String>()
                    participantIds.forEach { userId ->
                        profileRepository.getProfileById(userId).getOrNull()?.let { user ->
                            userNamesMap[userId] = user.name
                        }
                    }

                    //Fetch profile pictures for all participants
                    val profilePicturesMap = mutableMapOf<String, String?>()
                    participantIds.forEach { userId ->
                        profileRepository.getProfileById(userId).getOrNull()?.let { user ->
                            profilePicturesMap[userId] = user.profilePicture
                        }
                    }
                    
                    _uiState.value = ChatUiState(
                        isLoading = false, 
                        chats = chatList, 
                        currentUserId = profile?._id,
                        userNames = userNamesMap,
                        profilePictures = profilePicturesMap
                    )
                },
                onFailure = { e -> 
                    _uiState.value = ChatUiState(
                        isLoading = false, 
                        chats = emptyList(), 
                        error = e.message, 
                        currentUserId = profile?._id
                    ) 
                }
            )
        }
    }

    fun loadMessages(chatId: String, limit: Int? = 20, before: String? = null, append: Boolean = false) {
        viewModelScope.launch {
            val result = chatRepository.getMessages(chatId, limit, before)
            result.onSuccess { resp ->
                val updated = _uiState.value.messagesByChat.toMutableMap()
                val existing = updated[chatId] ?: emptyList()

                val merged = if (append) {
                    // Append older page to the end, removing duplicates by _id
                    (existing + resp.messages).distinctBy { it._id }
                } else {
                    // Merge server page with any live WS messages already present, keeping order
                    val byId = existing.associateBy { it._id }.toMutableMap()
                    // Start with existing (may include live messages not yet in server page)
                    val resultList = existing.toMutableList()
                    // Add any messages from server page that are not already present
                    for (m in resp.messages) {
                        if (!byId.containsKey(m._id)) {
                            resultList.add(m)
                            byId[m._id] = m
                        }
                    }
                    resultList
                }

                updated[chatId] = merged
                _uiState.value = _uiState.value.copy(messagesByChat = updated)
            }
        }
    }

    fun sendMessage(chatId: String, content: String) {
        if (socketManager.isConnected() && socketManager.isInRoom(chatId)) {
            // Send via WebSocket only; server persists and echoes back to all clients
            socketManager.sendMessage(chatId, content)
        } else {
            // Fallback to HTTP when socket is not connected or not in room
            viewModelScope.launch {
                val result = chatRepository.sendMessage(chatId, content)
                result.onSuccess { message ->
                    // Add the new message to the front of the list (newest-first)
                    val updated = _uiState.value.messagesByChat.toMutableMap()
                    val existing = updated[chatId] ?: emptyList()
                    if (existing.none { it._id == message._id }) {
                        updated[chatId] = listOf(message) + existing
                        _uiState.value = _uiState.value.copy(messagesByChat = updated)
                    }
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
        }
    }
    
    fun connectSocket(token: String) {
        socketManager.connect(token)
    }
    
    fun disconnectSocket() {
        socketManager.disconnect()
    }
    
    fun joinChatRoom(chatId: String) {
        socketManager.joinRoom(chatId)
    }
    
    fun leaveChatRoom(chatId: String) {
        socketManager.leaveRoom(chatId)
    }
    
    fun getOtherUserName(chat: Chat): String {
        val currentUserId = _uiState.value.currentUserId
        val otherUserId = chat.participants.find { it != currentUserId }
        return otherUserId?.let { _uiState.value.userNames[it] } ?: "Unknown User"
    }
    
    fun getOtherUserProfilePicture(chat: Chat): String? {
        val currentUserId = _uiState.value.currentUserId
        val otherUserId = chat.participants.find { it != currentUserId }
        return otherUserId?.let { _uiState.value.profilePictures[it] }
    }
    
    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }
}


