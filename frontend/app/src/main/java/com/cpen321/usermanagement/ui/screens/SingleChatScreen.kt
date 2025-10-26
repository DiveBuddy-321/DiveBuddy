package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.utils.ChatUtils.formatLastMessageTime
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SingleChatScreen(
    chat: Chat,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

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
                        text = chat.lastMessage,
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

        // Placeholder for future chat messages
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(spacing.medium),
            contentAlignment = Alignment.Companion.Center
        ) {
            Column(
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Chat icon",
                    modifier = Modifier.Companion.size(64.dp),
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