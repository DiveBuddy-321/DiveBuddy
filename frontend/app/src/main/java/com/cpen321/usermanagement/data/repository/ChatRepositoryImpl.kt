package com.cpen321.usermanagement.data.repository

import android.util.Log
import com.cpen321.usermanagement.data.remote.api.ChatInterface
import com.cpen321.usermanagement.data.remote.dto.CreateChatRequest
import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.MessagesResponse
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
        } catch (e: Exception) {
            Log.e(TAG, "Error listing chats", e)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages", e)
            Result.failure(e)
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
                Log.e(TAG, "Failed to create chat: ${'$'}errorText")
                Result.failure(IllegalStateException("Failed to create chat"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat", e)
            Result.failure(e)
        }
    }
}


