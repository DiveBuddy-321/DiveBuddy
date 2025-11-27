package com.cpen321.usermanagement.data.remote.dto

import java.util.Date

data class Sender(
    val _id: String,
    val name: String,
    val profilePicture: String? = null
)

data class Message(
    val _id: String,
    val chat: String,
    val sender: Sender?,
    val content: String,
    val createdAt: Date
)

data class MessagesResponse(
    val messages: List<Message>,
    val chatId: String,
    val limit: Int,
    val count: Int,
    val hasMore: Boolean
)

data class SendMessageRequest(
    val content: String
)

