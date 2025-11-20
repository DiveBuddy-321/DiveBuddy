package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.DisposableEffect
import com.cpen321.usermanagement.R

@Composable
fun SingleChatScreen(
    chat: Chat,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val chatVm: ChatViewModel = chatViewModel
    val uiState by chatVm.uiState.collectAsState()
    val messages = remember { mutableStateOf<List<Message>>(emptyList()) }
    val listState = rememberLazyListState()
    val isLoadingMore = remember { mutableStateOf(false) }
    val messageText = remember { mutableStateOf("") }
    val otherUserName = chatVm.getOtherUserName(chat)
	ChatInitializationEffects(chatId = chat._id, chatVm = chatVm)
	MessagesCollector(
		chatId = chat._id,
		messagesByChat = uiState.messagesByChat,
		onMessagesChanged = { newMessages -> messages.value = newMessages }
	)
	// Detect when user scrolls near the top to load more older messages
	LaunchedEffect(listState) {
		snapshotFlow {
			val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
			val total = listState.layoutInfo.totalItemsCount
			Pair(lastVisible, total)
		}
			.distinctUntilChanged()
			.collect { (lastVisibleIndex, totalItems) ->
                val canLoadMore = !isLoadingMore.value && messages.value.isNotEmpty() && totalItems > 0;
                val isItemsOK = lastVisibleIndex != null && lastVisibleIndex >= totalItems - 3 && canLoadMore;
				if (isItemsOK && canLoadMore) {
					isLoadingMore.value = true
					// Backend returns messages newest first, so last item is the oldest
					val oldestMessage = messages.value.lastOrNull()
					val beforeTimestamp = oldestMessage?.createdAt?.let {
						SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
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
    ChatContent(
        otherUserName = otherUserName,
        messages = messages.value,
        currentUserId = uiState.currentUserId,
        listState = listState,
        inputState = messageText,
        onSend = {
            if (messageText.value.trim().isNotEmpty()) {
                chat._id?.let { chatId ->
                    chatVm.sendMessage(chatId, messageText.value.trim())
                    messageText.value = ""
                }
            }
        },
        onBack = onBack
    )
}

@Composable
private fun ChatInitializationEffects(chatId: String?, chatVm: ChatViewModel) {
	LaunchedEffect(chatId) {
		chatId?.let { id ->
			chatVm.loadMessages(id, limit = 20)
			chatVm.joinChatRoom(id)
		}
	}

	DisposableEffect(chatId) {
		onDispose {
			chatId?.let { id -> chatVm.leaveChatRoom(id) }
		}
	}
}

@Composable
private fun MessagesCollector(
	chatId: String?,
	messagesByChat: Map<String, List<Message>>,
	onMessagesChanged: (List<Message>) -> Unit
) {
	LaunchedEffect(chatId, messagesByChat) {
		onMessagesChanged(chatId?.let { messagesByChat[it] } ?: emptyList())
	}
}

@Composable
private fun ChatTopBar(onBack: () -> Unit, otherUserName: String, spacing: com.cpen321.usermanagement.ui.theme.Spacing) {
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
			text = otherUserName,
			style = MaterialTheme.typography.titleLarge,
			fontWeight = FontWeight.Companion.SemiBold
		)
	}
}

@Composable
private fun MessagesList(
	messages: List<Message>,
	currentUserId: String?,
	spacing: com.cpen321.usermanagement.ui.theme.Spacing,
	listState: androidx.compose.foundation.lazy.LazyListState,
	modifier: Modifier
) {
	LazyColumn(
		state = listState,
		modifier = modifier
			.padding(spacing.medium),
		verticalArrangement = Arrangement.spacedBy(spacing.small),
		reverseLayout = true
	) {
		items(messages) { msg ->
			val isMine = currentUserId != null && msg.sender?._id == currentUserId
			if (isMine) {
				MessageBubbleMine(msg = msg, spacing = spacing)
			} else {
				MessageBubbleOther(msg = msg, spacing = spacing)
			}
		}
	}
}

@Composable
private fun MessageBubbleMine(msg: Message, spacing: com.cpen321.usermanagement.ui.theme.Spacing) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.End
	) {
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
		Spacer(modifier = Modifier.width(spacing.extraSmall))
		if (msg.sender?.avatar != null) {
			AsyncImage(
				model = RetrofitClient.getPictureUri(msg.sender.avatar),
				contentDescription = stringResource(R.string.profile_picture),
				modifier = Modifier.size(32.dp).clip(CircleShape)
			)
		}
	}
}

@Composable
private fun MessageBubbleOther(msg: Message, spacing: com.cpen321.usermanagement.ui.theme.Spacing) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Start
	) {
		if (msg.sender?.avatar != null) {
			AsyncImage(
				model = RetrofitClient.getPictureUri(msg.sender.avatar),
				contentDescription = stringResource(R.string.profile_picture),
				modifier = Modifier.size(32.dp).clip(CircleShape)
			)
		}
		Spacer(modifier = Modifier.width(spacing.extraSmall))
		Card(
			colors = CardDefaults.cardColors(
				containerColor = MaterialTheme.colorScheme.secondary,
				contentColor = MaterialTheme.colorScheme.onSecondary
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

@Composable
private fun MessageInputBar(
	message: String,
	onMessageChange: (String) -> Unit,
	onSend: () -> Unit,
	spacing: com.cpen321.usermanagement.ui.theme.Spacing
) {
	Row(
		modifier = Modifier.Companion
			.fillMaxWidth()
			.padding(spacing.medium),
		verticalAlignment = Alignment.Companion.CenterVertically
	) {
		OutlinedTextField(
			value = message,
			onValueChange = onMessageChange,
			placeholder = { Text("Type a message...") },
			modifier = Modifier.Companion
				.weight(1f)
				.padding(end = spacing.small),
			shape = RoundedCornerShape(24.dp),
			maxLines = 4
		)
		IconButton(
			onClick = onSend,
			enabled = message.trim().isNotEmpty()
		) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.Send,
				contentDescription = "Send message",
				tint = if (message.trim().isNotEmpty())
					MaterialTheme.colorScheme.primary
				else
					MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
			)
		}
	}
}

@Composable
private fun ChatContent(
    otherUserName: String,
    messages: List<Message>,
    currentUserId: String?,
    listState: androidx.compose.foundation.lazy.LazyListState,
    inputState: androidx.compose.runtime.MutableState<String>,
    onSend: () -> Unit,
    onBack: () -> Unit
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ChatTopBar(onBack = onBack, otherUserName = otherUserName, spacing = spacing)
        MessagesList(
            messages = messages,
            currentUserId = currentUserId,
            spacing = spacing,
            listState = listState,
            modifier = Modifier.weight(1f)
        )
        MessageInputBar(
            message = inputState.value,
            onMessageChange = { inputState.value = it },
            onSend = onSend,
            spacing = spacing
        )
    }
}