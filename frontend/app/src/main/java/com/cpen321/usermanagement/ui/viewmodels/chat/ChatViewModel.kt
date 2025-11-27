package com.cpen321.usermanagement.ui.viewmodels.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.Message
import com.cpen321.usermanagement.data.repository.AuthRepository
import com.cpen321.usermanagement.data.repository.BlockRepository
import com.cpen321.usermanagement.data.repository.BlockedException
import com.cpen321.usermanagement.data.repository.ChatDeletedException
import com.cpen321.usermanagement.data.repository.ChatRepository
import com.cpen321.usermanagement.data.repository.ProfileRepository
import com.cpen321.usermanagement.data.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

//UserData groups ui state variables related to user info together
data class UserData(
    val currentUserId: String? = null,
    val userNames: Map<String, String> = emptyMap(), // userId -> userName mapping
    val profilePictures: Map<String, String?> = emptyMap() // userId -> profilePicture path
)

//BlockData groups ui state variables related to blocking a user
data class BlockData(
    val blockedUsers: Set<String> = emptySet(),
    val actionInProgress: Boolean = false
)

//ConnectionState groups ui state variables related to socket connections
data class ConnectionState(
    val isSocketConnected: Boolean = false,
    val error: String? = null,
    val errorTimestamp: Long = 0L // Used to trigger toast even with same error message
)

data class ChatUiState(
    val isLoading: Boolean = false,
    val chats: List<Chat> = emptyList(),
    val messagesByChat: Map<String, List<Message>> = emptyMap(),
    val userData: UserData = UserData(),
    val blockData: BlockData = BlockData(),
    val connectionState: ConnectionState = ConnectionState()

)

@HiltViewModel
class ChatViewModel @Inject constructor(
    val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    private val blockRepository: BlockRepository,
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
        
        // Load blocked users on initialization
        viewModelScope.launch {
            loadBlockedUsers()
        }

        // Listen to socket connection state
        viewModelScope.launch {
            socketManager.connectionStateFlow.collect { isConnected ->
                _uiState.value = _uiState.value.copy(
                    connectionState = _uiState.value.connectionState.copy(isSocketConnected = isConnected)
                )
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
                val friendlyMessage =
                // special case for users attempting to access deleted chats/chat with deleted users
                    if (event.message.contains("Chat not found") || event.message.contains("access denied") || event.message.contains("not a participant")) {
                        "This chat has been deleted."
                    } else {
                        event.message
                    }
                _uiState.value = _uiState.value.copy(
                    connectionState = _uiState.value.connectionState.copy(
                        error = friendlyMessage,
                        errorTimestamp = System.currentTimeMillis()
                    )
                )
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
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                connectionState = _uiState.value.connectionState.copy(error = null)
            )
            val profile = profileRepository.getProfile().getOrNull()
            val result = chatRepository.listChats()
            
            result.fold(
                onSuccess = { chatList ->
                    // Get all unique participant IDs (excluding current user)
                    val participantIds = chatList.flatMap { chat ->
                        chat.participants.filter { it != profile?._id }
                    }.distinct()
                    
                    // Fetch user names and profile pictures for all participants
                    val userNamesMap = mutableMapOf<String, String>()
                    val profilePicturesMap = mutableMapOf<String, String?>()
                    participantIds.forEach { userId ->
                        profileRepository.getProfileById(userId).getOrNull()?.let { user ->
                            userNamesMap[userId] = user.name
                            profilePicturesMap[userId] = user.profilePicture
                        }
                    }
                 
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        chats = chatList,
                        userData = UserData(
                            currentUserId = profile?._id,
                            userNames = userNamesMap,
                            profilePictures = profilePicturesMap
                        )

                    )
                },
                onFailure = { e -> 
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        chats = emptyList(),
                        connectionState = _uiState.value.connectionState.copy(
                            error = e.message,
                            errorTimestamp = System.currentTimeMillis()
                        ),
                        userData = UserData(currentUserId = profile?._id)
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

    fun sendMessage(chatId: String, content: String, otherUserId: String? = null) {
        // Check if we have blocked the other user
        if (otherUserId != null && isUserBlocked(otherUserId)) {
            _uiState.value = _uiState.value.copy(
                connectionState = _uiState.value.connectionState.copy(
                    error = "You have blocked this user. You cannot send messages.",
                    errorTimestamp = System.currentTimeMillis()
                )
            )
            return
        }
        
        // Check if we are blocked by the other user before sending
        if (otherUserId != null) {
            viewModelScope.launch {
                val blockCheckResult = blockRepository.checkIfBlockedBy(otherUserId)
                blockCheckResult.onSuccess { isBlocked ->
                    if (isBlocked) {
                        _uiState.value = _uiState.value.copy(
                            connectionState = _uiState.value.connectionState.copy(
                                error = "You cannot send messages to this user. You have been blocked.",
                                errorTimestamp = System.currentTimeMillis()
                            )
                        )
                    } else {
                        // Not blocked, proceed with sending
                        actualSendMessage(chatId, content)
                    }
                }.onFailure {
                    // If the check fails, proceed anyway and let backend handle it
                    actualSendMessage(chatId, content)
                }
            }
        } else {
            // No otherUserId provided, proceed with sending
            actualSendMessage(chatId, content)
        }
    }
    
    private fun actualSendMessage(chatId: String, content: String) {
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
                    val errorMessage = when (e) {
                        is BlockedException -> "You cannot send messages to this user. You have been blocked."
                        is ChatDeletedException -> "This chat has been deleted."
                        else -> e.message ?: "Failed to send message"
                    }
                    _uiState.value = _uiState.value.copy(
                        connectionState = _uiState.value.connectionState.copy(
                            error = errorMessage,
                            errorTimestamp = System.currentTimeMillis()
                        )
                    )
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
        val currentUserId = _uiState.value.userData.currentUserId
        val otherUserId = chat.participants.find { it != currentUserId }
        return otherUserId?.let { _uiState.value.userData.userNames[it] } ?: "Unknown User"
    }
    
    fun getChatDisplayName(chat: Chat): String {
        // If chat has a name field, it's a group chat (event) - use that name directly
        if (!chat.name.isNullOrEmpty()) {
            return chat.name
        }
        // Otherwise it's a direct message - display the other user's name
        return getOtherUserName(chat)
    }
    
    fun getOtherUserId(chat: Chat): String? {
        val currentUserId = _uiState.value.userData.currentUserId
        return chat.participants.find { it != currentUserId }
    }
    
    fun isUserBlocked(userId: String): Boolean {
        return _uiState.value.blockData.blockedUsers.contains(userId)
    }
    
    private fun loadBlockedUsers() {
        viewModelScope.launch {
            val result = blockRepository.getBlockedUsers()
            result.onSuccess { blockedUserIds ->
                _uiState.value = _uiState.value.copy(
                    blockData = _uiState.value.blockData.copy(
                        blockedUsers = blockedUserIds.toSet()
                    )
                )
            }.onFailure { e ->
                // Silently fail - blocked users will just be empty
                _uiState.value = _uiState.value.copy(
                    connectionState = _uiState.value.connectionState.copy(
                        error = "Failed to load blocked users: ${e.message}",
                        errorTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    fun blockUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                blockData = _uiState.value.blockData.copy(actionInProgress = true),
                connectionState = _uiState.value.connectionState.copy(error = null)
            )
            val result = blockRepository.blockUser(userId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        blockData = _uiState.value.blockData.copy(
                            blockedUsers = _uiState.value.blockData.blockedUsers + userId,
                            actionInProgress = false
                        )
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        connectionState = _uiState.value.connectionState.copy(
                            error = e.message ?: "Failed to block user",
                            errorTimestamp = System.currentTimeMillis()
                        ),
                        blockData = _uiState.value.blockData.copy(actionInProgress = false)
                    )
                }
            )
        }
    }
    
    fun unblockUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                blockData = _uiState.value.blockData.copy(actionInProgress = true),
                connectionState = _uiState.value.connectionState.copy(error = null)
            )
            val result = blockRepository.unblockUser(userId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        blockData = _uiState.value.blockData.copy(
                            blockedUsers = _uiState.value.blockData.blockedUsers - userId,
                            actionInProgress = false
                        )
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        connectionState = _uiState.value.connectionState.copy(
                            error = e.message ?: "Failed to unblock user",
                            errorTimestamp = System.currentTimeMillis()
                        ),
                        blockData = _uiState.value.blockData.copy(actionInProgress = false)
                    )
                }
            )
        }
    }
    
    fun getOtherUserProfilePicture(chat: Chat): String? {
		val currentUserId = _uiState.value.userData.currentUserId
		val otherUserId = chat.participants.find { it != currentUserId }
		return otherUserId?.let { _uiState.value.userData.profilePictures[it] }
    }
    
    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            connectionState = _uiState.value.connectionState.copy(error = null)
        )
    }
}


