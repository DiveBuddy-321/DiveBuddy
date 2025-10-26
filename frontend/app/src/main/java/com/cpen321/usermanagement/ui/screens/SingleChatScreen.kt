package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.Message
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.utils.ChatUtils.formatLastMessageTime
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.ui.viewmodels.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import coil.compose.AsyncImage
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import com.cpen321.usermanagement.R

@Composable
fun SingleChatScreen(
    chat: Chat,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val chatVm: ChatViewModel = hiltViewModel()
    val uiState by chatVm.uiState.collectAsState()
    val messages = remember { mutableStateOf<List<Message>>(emptyList()) }
    val listState = rememberLazyListState()
    val isLoadingMore = remember { mutableStateOf(false) }
    val messageText = remember { mutableStateOf("") }

    LaunchedEffect(chat._id) {
        chat._id?.let { id ->
            // Load initial messages
            chatVm.loadMessages(id, limit = 20)
            // Join the chat room for real-time updates
            chatVm.joinChatRoom(id)
        }
    }
    
    LaunchedEffect(uiState.messagesByChat[chat._id]) {
        messages.value = uiState.messagesByChat[chat._id] ?: emptyList()
    }
    
    // Leave chat room when screen is disposed
    androidx.compose.runtime.DisposableEffect(chat._id) {
        onDispose {
            chat._id?.let { id -> chatVm.leaveChatRoom(id) }
        }
    }

    // Detect when user scrolls near the top to load more older messages
    // With reverseLayout, index 0 is at bottom (newest), high indices are at top (oldest)
    // Backend returns messages sorted newest first, so lastOrNull() gives us the oldest
    LaunchedEffect(listState) {
        snapshotFlow { 
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val total = listState.layoutInfo.totalItemsCount
            Pair(lastVisible, total)
        }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (lastVisibleIndex != null && 
                    totalItems > 0 && 
                    lastVisibleIndex >= totalItems - 3 && 
                    !isLoadingMore.value && 
                    messages.value.isNotEmpty()) {
                    
                    isLoadingMore.value = true
                    // Backend returns messages newest first, so last item is the oldest
                    val oldestMessage = messages.value.lastOrNull()
                    val beforeTimestamp = oldestMessage?.createdAt?.let {
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }.format(it)
                    }
                    
                    chat._id?.let { id ->
                        chatVm.loadMessages(id, limit = 20, before = beforeTimestamp, append = true)
                    }
                    
                    kotlinx.coroutines.delay(1000)
                    isLoadingMore.value = false
                }
            }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {

        Row(
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to chats"
                )
            }
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Direct message",
                modifier = Modifier.Companion.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.Companion.width(spacing.small))
            Text(
                text = chat.name ?: "Unknown Chat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Companion.SemiBold
            )
        }

        // Chat info section
        Card(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(spacing.medium),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.Companion.padding(spacing.medium)
            ) {
                // Chat type and name
                Row(
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Direct message",
                        modifier = Modifier.Companion.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.Companion.width(spacing.medium))
                    Column {
                        Text(
                            text = "Direct Message",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = chat.name ?: "Unknown Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Companion.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.Companion.height(spacing.medium))

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
                    Spacer(modifier = Modifier.Companion.height(spacing.small))
                    Text(
                        text = "Last message:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Companion.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = chat.lastMessage?.content ?: "No messages yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (chat.lastMessageAt != null) {
                    Spacer(modifier = Modifier.Companion.height(spacing.small))
                    Text(
                        text = "Last activity: ${formatLastMessageTime(chat.lastMessageAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (chat.createdAt != null) {
                    Text(
                        text = "Created: ${
                            SimpleDateFormat(
                                "MMM dd, yyyy",
                                Locale.getDefault()
                            ).format(chat.createdAt)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Messages list
        // Backend returns messages newest first, reverseLayout puts index 0 (newest) at bottom
        LazyColumn(
            state = listState,
            modifier = Modifier.Companion
                .weight(1f)
                .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            reverseLayout = true
        ) {
            items(messages.value) { msg ->
                val isMine = uiState.currentUserId != null && msg.sender?._id == uiState.currentUserId
                if (isMine) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (msg.sender?.avatar != null) {
                        AsyncImage(
                            model = RetrofitClient.getPictureUri(msg.sender.avatar),
                            contentDescription = stringResource(R.string.profile_picture),
                            modifier = Modifier.size(12.dp).clip(CircleShape)
                            )
                        }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary 
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        Text(
                            text = msg.content,
                            modifier = Modifier.Companion.padding(horizontal = spacing.medium, vertical = spacing.small)
                        )
                    }
                }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (msg.sender?.avatar != null) {
                        AsyncImage(
                            model = RetrofitClient.getPictureUri(msg.sender.avatar),
                            contentDescription = stringResource(R.string.profile_picture),
                            modifier = Modifier.size(12.dp).clip(CircleShape)
                            )
                        }
                        Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor =  MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        Text(
                            text = msg.content,
                            modifier = Modifier.Companion.padding(horizontal = spacing.medium, vertical = spacing.small)
                        )
                    }
                }
                }
            }
        }

        // Message input field
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText.value,
                onValueChange = { messageText.value = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.Companion
                    .weight(1f)
                    .padding(end = spacing.small),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            IconButton(
                onClick = {
                    if (messageText.value.trim().isNotEmpty()) {
                        chat._id?.let { chatId ->
                            chatVm.sendMessage(chatId, messageText.value.trim())
                            messageText.value = ""
                        }
                    }
                },
                enabled = messageText.value.trim().isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (messageText.value.trim().isNotEmpty()) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}