package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.CreateChatRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST

interface ChatInterface {
    @GET("chats/")
    suspend fun getChats(
        @Header("Authorization") authHeader: String
    ): Response<List<Chat>>

    @POST("chats/")
    suspend fun createChat(
        @Header("Authorization") authHeader: String,
        @Body request: CreateChatRequest
    ): Response<Chat>
}


