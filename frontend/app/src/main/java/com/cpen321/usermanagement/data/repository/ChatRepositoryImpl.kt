package com.cpen321.usermanagement.data.repository

import android.util.Log
import com.cpen321.usermanagement.data.remote.api.ChatInterface
import com.cpen321.usermanagement.data.remote.dto.CreateChatRequest
import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.MessagesResponse
import com.google.gson.JsonSyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatInterface: ChatInterface
) : ChatRepository {
    override suspend fun listChats(): Result<List<Chat>> {
        return try {
            val response = chatInterface.getChats(authHeader = "") // handled by interceptor
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                val err = response.errorBody()?.string()
                Log.e(TAG, "Failed to list chats: ${'$'}err")
                Result.failure(IllegalStateException("Failed to list chats"))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while listing chats", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while listing chats", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while listing chats", e)
            Result.failure(e)
        } 
    }

    override suspend fun getMessages(chatId: String, limit: Int?, before: String?): Result<MessagesResponse> {
        return try {
            val response = chatInterface.getMessages(authHeader = "", chatId = chatId, limit = limit, before = before)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = response.errorBody()?.string()
                Log.e(TAG, "Failed to get messages: ${'$'}err")
                Result.failure(IllegalStateException("Failed to get messages"))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting messages", e)
            return Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting messages", e)
            return Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting messages", e)
            return Result.failure(e)
        }
    }
    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }
    override suspend fun createChat(peerId: String, name: String?): Result<String> {
        return try {
            val response = chatInterface.createChat(
                authHeader = "",
                request = CreateChatRequest(peerId = peerId, name = name)
            ) // Auth header is added by interceptor
            if (response.isSuccessful) {
                val chat = response.body()
                val chatId = chat?._id
                if (!chatId.isNullOrEmpty()) {
                    Result.success(chatId)
                } else {
                    Log.e(TAG, "Missing chat id in response")
                    Result.failure(IllegalStateException("Missing chat id"))
                }
            } else {
                val errorText = response.errorBody()?.string()
                Log.e(TAG, "Failed to create chat (code=${'$'}{response.code()}): ${'$'}errorText")
                // If backend signals duplicate chat (e.g., 409), try to find existing chat
                if (response.code() == 409 || (errorText?.contains("exists", ignoreCase = true) == true)) {
                    findExistingDirectChatId(peerId)?.let { return Result.success(it) }
                }
                Result.failure(IllegalStateException("Failed to create chat"))
            }
        } catch (e: JsonSyntaxException) {
            // Chat may already exist, so try to find it
            Log.w(TAG, "JSON parsing failed for createChat; attempting fallback lookup", e)
            findExistingDirectChatId(peerId)?.let { return Result.success(it) }
            Result.failure(e)
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Result.failure(e)
        } catch (e: java.io.IOException) {
           Result.failure(e)
        }
    }

    private suspend fun findExistingDirectChatId(peerId: String): String? {
        return try {
            val listResponse = chatInterface.getChats(authHeader = "")
            if (listResponse.isSuccessful) {
                val chats = listResponse.body().orEmpty()
                chats.firstOrNull { chat ->
                    !chat.isGroup && chat.participants.contains(peerId)
                }?._id
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun sendMessage(chatId: String, content: String): Result<com.cpen321.usermanagement.data.remote.dto.Message> {
        return try {
            val response = chatInterface.sendMessage(
                authHeader = "",
                chatId = chatId,
                request = com.cpen321.usermanagement.data.remote.dto.SendMessageRequest(content = content)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = response.errorBody()?.string()
                Log.e(TAG, "Failed to send message: ${'$'}err")
                Result.failure(IllegalStateException("Failed to send message"))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Result.failure(e)
        } catch (e: java.io.IOException) {
           Result.failure(e)
        }
    }
}


