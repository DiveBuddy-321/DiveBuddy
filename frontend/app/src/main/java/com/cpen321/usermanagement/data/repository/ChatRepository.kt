package com.cpen321.usermanagement.data.repository

interface ChatRepository {
    suspend fun createChat(peerId: String, name: String? = null): Result<String>
}


