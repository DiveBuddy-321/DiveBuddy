package com.cpen321.usermanagement.data.repository

import android.util.Log
import com.cpen321.usermanagement.data.remote.api.ChatInterface
import com.cpen321.usermanagement.data.remote.dto.CreateChatRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatInterface: ChatInterface
) : ChatRepository {

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


