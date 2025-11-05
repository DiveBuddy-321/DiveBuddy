package com.cpen321.usermanagement.data.socket

import android.util.Log
import com.cpen321.usermanagement.BuildConfig
import com.cpen321.usermanagement.data.remote.dto.Message
import com.cpen321.usermanagement.data.remote.dto.Sender
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

data class NewMessageEvent(
    val chatId: String,
    val message: Message
)

data class ChatUpdatedEvent(
    val chatId: String,
    val lastMessage: Message
)

data class JoinedRoomEvent(
    val chatId: String
)

data class SocketErrorEvent(
    val message: String
)

@Singleton
class SocketManager @Inject constructor() {
    private var socket: Socket? = null
    // We parse JSON manually to avoid Date adapter issues across threads/instances
    private val roomsToJoin: MutableSet<String> = mutableSetOf()
    private val joinedRooms: MutableSet<String> = mutableSetOf()
    
    private val _newMessageFlow = MutableSharedFlow<NewMessageEvent>(replay = 1, extraBufferCapacity = 16)
    val newMessageFlow: SharedFlow<NewMessageEvent> = _newMessageFlow.asSharedFlow()
    
    private val _chatUpdatedFlow = MutableSharedFlow<ChatUpdatedEvent>(replay = 0)
    val chatUpdatedFlow: SharedFlow<ChatUpdatedEvent> = _chatUpdatedFlow.asSharedFlow()
    
    private val _joinedRoomFlow = MutableSharedFlow<JoinedRoomEvent>(replay = 0)
    val joinedRoomFlow: SharedFlow<JoinedRoomEvent> = _joinedRoomFlow.asSharedFlow()
    
    private val _errorFlow = MutableSharedFlow<SocketErrorEvent>(replay = 0)
    val errorFlow: SharedFlow<SocketErrorEvent> = _errorFlow.asSharedFlow()
    
    private val _connectionStateFlow = MutableSharedFlow<Boolean>(replay = 1)
    val connectionStateFlow: SharedFlow<Boolean> = _connectionStateFlow.asSharedFlow()
    
    companion object {
        private const val TAG = "SocketManager"
    }
    fun connect(token: String) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Socket already connected")
            return
        }
        try {
            // Remove /api suffix if present - Socket.IO connects to root, not /api
            var serverUrl = BuildConfig.API_BASE_URL
            if (serverUrl.endsWith("/api")) {
                serverUrl = serverUrl.removeSuffix("/api")
            }
            if (serverUrl.endsWith("/api/")) {
                serverUrl = serverUrl.removeSuffix("/api/")
            }
            Log.d(TAG, "Connecting to WebSocket at $serverUrl")
            val options = IO.Options().apply {
                auth = mapOf("token" to token)
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionDelay = 1000
                reconnectionAttempts = 5
            }
            socket = IO.socket(serverUrl, options)
            setupEventListeners()
            socket?.connect()
        
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid server URL", e)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while connecting to socket", e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while connecting to socket", e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while connecting to socket", e)
        }
    }
    
    private fun setupEventListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected: ${id()}")
                _connectionStateFlow.tryEmit(true)
                // Auto-join any rooms that were requested before connection
                try {
                    val roomsSnapshot = roomsToJoin.toList()
                    for (roomChatId in roomsSnapshot) {
                        val data = JSONObject().apply { put("chatId", roomChatId) }
                        Log.d(TAG, "Auto-joining room after connect: $roomChatId")
                        emit("join_room", data)
                    }
                } catch (e: URISyntaxException) {
                    Log.e(TAG, "Invalid server URL", e)
                } catch (e: java.net.SocketTimeoutException) {
                    Log.e(TAG, "Network timeout while connecting to socket", e)
                } catch (e: java.net.UnknownHostException) {
                    Log.e(TAG, "Network connection failed while connecting to socket", e)
                } catch (e: java.io.IOException) {
                    Log.e(TAG, "IO error while connecting to socket", e)
                }
            }
            
            on(Socket.EVENT_DISCONNECT) { args ->
                val reason = if (args.isNotEmpty()) args[0].toString() else "unknown"
                Log.d(TAG, "Socket disconnected: $reason")
                _connectionStateFlow.tryEmit(false)
            }
            
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = if (args.isNotEmpty()) args[0].toString() else "Unknown error"
                Log.e(TAG, "Connection error: $error")
                _connectionStateFlow.tryEmit(false)
            }
            
            on("joined_room") { args ->
                try {
                    if (args.isNotEmpty()) {
                        val data = args[0] as JSONObject
                        val chatId = data.getString("chatId")
                        Log.d(TAG, "Joined room: $chatId")
                        synchronized(joinedRooms) {
                            joinedRooms.add(chatId)
                        }
                        _joinedRoomFlow.tryEmit(JoinedRoomEvent(chatId))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing joined_room event", e)
                }
            }
            
            on("left_room") { args ->
                try {
                    if (args.isNotEmpty()) {
                        val data = args[0] as JSONObject
                        val chatId = data.getString("chatId")
                        Log.d(TAG, "Left room: $chatId")
                        synchronized(joinedRooms) {
                            joinedRooms.remove(chatId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing left_room event", e)
                }
            }
            
            on("new_message") { args ->
                try {
                    if (args.isNotEmpty()) {
                        val data = args[0] as JSONObject
                        val chatId = data.getString("chatId")
                        val messageJson = data.getJSONObject("message")
                        val message = parseMessage(messageJson)
                        Log.d(TAG, "New message in chat $chatId: ${message.content}")
                        
                        _newMessageFlow.tryEmit(NewMessageEvent(chatId, message))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing new_message event", e)
                }
            }
            
            on("chat_updated") { args ->
                try {
                    if (args.isNotEmpty()) {
                        val data = args[0] as JSONObject
                        val chatId = data.getString("chatId")
                        val lastMessageJson = data.getJSONObject("lastMessage")
                        val lastMessage = parseMessage(lastMessageJson)
                        Log.d(TAG, "Chat updated: $chatId")
                        
                        _chatUpdatedFlow.tryEmit(ChatUpdatedEvent(chatId, lastMessage))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing chat_updated event", e)
                }
            }
            
            on("error") { args ->
                try {
                    if (args.isNotEmpty()) {
                        val data = args[0] as JSONObject
                        val message = data.getString("message")
                        Log.e(TAG, "Server error: $message")
                        _errorFlow.tryEmit(SocketErrorEvent(message))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing error event", e)
                }
            }
        }
    }
    
    fun joinRoom(chatId: String) {
        // Track requested room so we can join after reconnects too
        roomsToJoin.add(chatId)

        if (socket?.connected() != true) {
            Log.d(TAG, "Queueing join until connected: $chatId")
            return
        }

        val data = JSONObject().apply { put("chatId", chatId) }
        Log.d(TAG, "Joining room: $chatId")
        socket?.emit("join_room", data)
    }
    
    fun leaveRoom(chatId: String) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Cannot leave room: Socket not connected")
            // Still remove from desired rooms so we don't auto-join later
            roomsToJoin.remove(chatId)
            return
        }
        
        val data = JSONObject().apply {
            put("chatId", chatId)
        }
        
        Log.d(TAG, "Leaving room: $chatId")
        socket?.emit("leave_room", data)
        roomsToJoin.remove(chatId)
    }
    
    fun sendMessage(chatId: String, content: String) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Cannot send message: Socket not connected")
            return
        }
        
        val data = JSONObject().apply {
            put("chatId", chatId)
            put("content", content)
        }
        
        Log.d(TAG, "Sending message to room $chatId: $content")
        socket?.emit("send_message", data)
    }
    
    fun disconnect() {
        Log.d(TAG, "Disconnecting socket")
        socket?.disconnect()
        socket?.off()
        socket = null
        roomsToJoin.clear()
        synchronized(joinedRooms) { joinedRooms.clear() }
    }
    
    fun isConnected(): Boolean {
        return socket?.connected() == true
    }

    fun isInRoom(chatId: String): Boolean {
        synchronized(joinedRooms) {
            return joinedRooms.contains(chatId)
        }
    }

    private fun parseMessage(obj: JSONObject): Message {
        val id = obj.getString("_id")
        val chatId = obj.getString("chat")
        val content = obj.getString("content")
        val createdAtStr = obj.getString("createdAt")
        val createdAt = parseIsoDate(createdAtStr)
        val sender = if (obj.has("sender") && !obj.isNull("sender")) {
            val s = obj.getJSONObject("sender")
            val senderId = s.optString("_id", "")
            val name = s.optString("name", "Unknown")
            val avatar = if (s.has("avatar") && !s.isNull("avatar")) s.optString("avatar", null) else null
            Sender(_id = senderId, name = name, avatar = avatar)
        } else {
            null
        }
        return Message(
            _id = id,
            chat = chatId,
            sender = sender,
            content = content,
            createdAt = createdAt
        )
    }

    private fun parseIsoDate(value: String): java.util.Date {
        // Try multiple formats commonly seen from backend
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        for (pattern in formats) {
            try {
                val fmt = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return fmt.parse(value)!!
            } catch (_: Exception) {
            }
        }
        // Fallback to now to avoid crashes; logs help diagnose
        Log.w(TAG, "Failed to parse date: $value, defaulting to now")
        return java.util.Date()
    }
}

