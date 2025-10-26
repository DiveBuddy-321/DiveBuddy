package com.cpen321.usermanagement.data.remote.dto

import java.util.Date

data class LastMessage(
    val _id: String,
    val content: String,
    val sender: Sender?,
    val createdAt: Date
)

data class Chat(
    val _id: String? = null,
    val isGroup: Boolean,
    val name: String? = null,
    val participants: List<String>,
    val createdBy: String? = null,
    val lastMessage: LastMessage? = null,
    val lastMessageAt: Date? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class ChatListData(
    val chats: List<Chat>
)

data class CreateChatRequest(
    val peerId: String,
    val name: String? = null
)
