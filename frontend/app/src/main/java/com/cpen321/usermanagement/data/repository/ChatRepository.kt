package com.cpen321.usermanagement.data.repository

interface ChatRepository {
    suspend fun listChats(): Result<List<com.cpen321.usermanagement.data.remote.dto.Chat>>
    suspend fun createChat(peerId: String, name: String? = null): Result<String>
}


