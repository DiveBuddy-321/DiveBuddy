package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    chatId: String? = null
) {
    ChatContent(modifier = modifier)
}

@Composable
private fun ChatContent(
    modifier: Modifier = Modifier
) {
    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    var showSelectedChat by remember { mutableStateOf(false) }

    val spacing = LocalSpacing.current

    // Dummy data for chats
    val dummyChats = listOf(
        Chat(
            _id = "1",
            isGroup = false,
            name = "John Doe",
            participants = listOf("user1", "user2"),
            createdBy = "user1",
            lastMessage = "Hey! How's your tennis game going?",
            lastMessageAt = Date(System.currentTimeMillis() - 1000 * 60 * 30), // 30 minutes ago
            createdAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24), // 1 day ago
            updatedAt = Date(System.currentTimeMillis() - 1000 * 60 * 30)
        ),
        Chat(
            _id = "3",
            isGroup = false,
            name = "Sarah Wilson",
            participants = listOf("user1", "user5"),
            createdBy = "user1",
            lastMessage = "Thanks for the great match yesterday!",
            lastMessageAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24), // 1 day ago
            createdAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7), // 1 week ago
            updatedAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24)
        ),
        Chat(
            _id = "4",
            isGroup = false,
            name = "Mike Chen",
            participants = listOf("user1", "user6"),
            createdBy = "user1",
            lastMessage = "Let's schedule another practice session",
            lastMessageAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2), // 2 days ago
            createdAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 14), // 2 weeks ago
            updatedAt = Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2)
        )
    )

    if (showSelectedChat) {
        SingleChatScreen(
            chat = selectedChat!!,
            onBack = {
                showSelectedChat = false
            }
        )
    } else {
        // Chat list
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            items(dummyChats) { chat ->
                ChatCard(
                    chat = chat,
                    onClick = { 
                        selectedChat = chat
                        showSelectedChat = true
                    }
                )
            }
        }   
    }
}

@Composable
private fun ChatCard(
    chat: Chat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat icon
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Direct message",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.width(spacing.medium))
            
            // Chat content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Chat name
                Text(
                    text = chat.name ?: "Unknown Chat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Last message
                Text(
                    text = chat.lastMessage ?: "No messages yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(spacing.small))
            
            // Timestamp
            Text(
                text = formatLastMessageTime(chat.lastMessageAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SingleChatScreen(
    chat: Chat,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to chats"
                )
            }
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Direct message",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(spacing.small))
            Text(
                text = chat.name ?: "Unknown Chat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Chat info section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(spacing.medium)
            ) {
                // Chat type and name
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Direct message",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(spacing.medium))
                    Column {
                        Text(
                            text = "Direct Message",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = chat.name ?: "Unknown Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(spacing.medium))
                
                // Participants info
                Text(
                    text = "Participants: ${chat.participants.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (chat.createdBy != null) {
                    Text(
                        text = "Created by: ${chat.createdBy}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (chat.lastMessage != null) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(
                        text = "Last message:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (chat.lastMessageAt != null) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(
                        text = "Last activity: ${formatLastMessageTime(chat.lastMessageAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (chat.createdAt != null) {
                    Text(
                        text = "Created: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(chat.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Placeholder for future chat messages
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.medium),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Chat icon",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Chat messages will appear here",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "This is where the chat UI will be implemented",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatLastMessageTime(lastMessageAt: Date?): String {
    if (lastMessageAt == null) return ""
    
    val now = Date()
    val diff = now.time - lastMessageAt.time
    
    return when {
        diff < 60 * 1000 -> "now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(lastMessageAt)
    }
}
