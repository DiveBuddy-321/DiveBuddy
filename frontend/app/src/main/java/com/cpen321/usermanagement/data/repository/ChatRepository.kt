package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.Message
import com.cpen321.usermanagement.data.remote.dto.MessagesResponse

interface ChatRepository {
    suspend fun listChats(): Result<List<Chat>>
    suspend fun createChat(peerId: String, name: String? = null): Result<String>
    suspend fun getMessages(chatId: String, limit: Int? = null, before: String? = null): Result<MessagesResponse>
    suspend fun sendMessage(chatId: String, content: String): Result<Message>
}


