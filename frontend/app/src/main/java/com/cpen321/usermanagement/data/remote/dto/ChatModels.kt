package com.cpen321.usermanagement.data.remote.dto

import java.util.Date

data class Chat(
    val _id: String? = null,
    val isGroup: Boolean,
    val name: String? = null,
    val participants: List<String>,
    val createdBy: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Date? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class ChatListData(
    val chats: List<Chat>
)
