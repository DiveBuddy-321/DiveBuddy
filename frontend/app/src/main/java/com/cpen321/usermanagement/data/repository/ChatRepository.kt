package com.cpen321.usermanagement.data.repository

interface ChatRepository {
    suspend fun listChats(): Result<List<com.cpen321.usermanagement.data.remote.dto.Chat>>
    suspend fun createChat(peerId: String, name: String? = null): Result<String>
    suspend fun getMessages(chatId: String, limit: Int? = null, before: String? = null): Result<com.cpen321.usermanagement.data.remote.dto.MessagesResponse>
}


