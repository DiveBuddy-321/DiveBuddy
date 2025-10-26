package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.CreateChatRequest
import com.cpen321.usermanagement.data.remote.dto.Message
import com.cpen321.usermanagement.data.remote.dto.MessagesResponse
import com.cpen321.usermanagement.data.remote.dto.SendMessageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

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

    @GET("chats/messages/{chatId}")
    suspend fun getMessages(
        @Header("Authorization") authHeader: String,
        @Path("chatId") chatId: String,
        @Query("limit") limit: Int? = null,
        @Query("before") before: String? = null
    ): Response<MessagesResponse>

    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Header("Authorization") authHeader: String,
        @Path("chatId") chatId: String,
        @Body request: SendMessageRequest
    ): Response<Message>
}


