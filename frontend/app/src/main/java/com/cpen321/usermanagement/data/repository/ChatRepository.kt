package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.remote.dto.Chat

interface ChatRepository {
    suspend fun listChats(): Result<List<Chat>>
    suspend fun createChat(peerId: String, name: String? = null): Result<String>
}