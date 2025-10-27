package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.cpen321.usermanagement.ui.viewmodels.ChatViewModel
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.utils.ChatUtils.formatLastMessageTime

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    chatId: String? = null
) {
    val vm: ChatViewModel = hiltViewModel()
    val uiState = vm.uiState.collectAsState()
    LaunchedEffect(Unit) { vm.loadChats() }
    ChatContent(
        modifier = modifier,
        isLoading = uiState.value.isLoading,
        chats = uiState.value.chats,
        viewModel = vm
    )
}

@Composable
private fun ChatContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    chats: List<Chat> = emptyList(),
    viewModel: ChatViewModel
) {
    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    var showSelectedChat by remember { mutableStateOf(false) }

    val spacing = LocalSpacing.current

    if (showSelectedChat) {
        SingleChatScreen(
            chat = selectedChat!!,
            onBack = {
                showSelectedChat = false
            }
        )
    } else {
        if (isLoading) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                items(chats) { chat ->
                    ChatCard(
                        chat = chat,
                        viewModel = viewModel,
                        onClick = { 
                            selectedChat = chat
                            showSelectedChat = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatCard(
    chat: Chat,
    viewModel: ChatViewModel,
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
                    text = viewModel.getOtherUserName(chat),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Last message
                Text(
                    text = chat.lastMessage?.content ?: "No messages yet",
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
