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

            if (!response.isSuccessful) {
                val errorText = response.errorBody()?.string()
                Log.e(TAG, "Failed to create chat (code=${'$'}{response.code()}): ${'$'}errorText")
                
                //try finding existing chat for all instances of errors 
                val existing = findExistingDirectChatId(peerId)
                if (existing != null) return Result.success(existing)
                
                return Result.failure(IllegalStateException("Failed to create chat"))
            }

            val chatId = response.body()?._id
            if (chatId.isNullOrEmpty()) {
                Log.e(TAG, "Missing chat id in response")
                return Result.failure(IllegalStateException("Missing chat id"))
            }
            Result.success(chatId)
        } catch (e: JsonSyntaxException) {
            Log.w(TAG, "JSON parsing failed for createChat; attempting fallback lookup", e)
            val existing = findExistingDirectChatId(peerId)
            if (existing != null) return Result.success(existing)
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
                Log.e(TAG, "Failed to send message (code=${response.code()}): $err")
                
                // Check if the error is due to being blocked
                if (response.code() == 403 && err?.contains("blocked") == true) {
                    Result.failure(BlockedException("You have been blocked by this user"))
                } else {
                    Result.failure(IllegalStateException("Failed to send message"))
                }
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

class BlockedException(message: String) : Exception(message)
