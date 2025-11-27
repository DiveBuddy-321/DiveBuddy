package com.cpen321.usermanagement.ui.screens.chat

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.cpen321.usermanagement.data.remote.dto.Chat
import com.cpen321.usermanagement.data.remote.dto.Message
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.ui.viewmodels.chat.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import coil.compose.AsyncImage
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.util.TimeZone
import androidx.compose.ui.layout.ContentScale

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
    val otherUserId = chatVm.getOtherUserId(chat)
    val isUserBlocked = otherUserId?.let { chatVm.isUserBlocked(it) } ?: false
    val context = LocalContext.current
    
    // Show toast when there's an error (triggered when error changes)
    LaunchedEffect(uiState.connectionState.error) {
        uiState.connectionState.error?.let { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }
	
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
							timeZone = TimeZone.getTimeZone("UTC")
						}.format(it)
					}
					chat._id?.let { id ->
						chatVm.loadMessages(id, limit = 20, before = beforeTimestamp, append = true)
					}
                    delay(1000)
					isLoadingMore.value = false
				}
			}
	}
    ChatContent(
        profilePicture = chatVm.getOtherUserProfilePicture(chat),
        otherUserName = otherUserName,
        messages = messages.value,
        currentUserId = uiState.userData.currentUserId,
        listState = listState,
        inputState = messageText,
        onSend = {
            if (messageText.value.trim().isNotEmpty()) {
                chat._id?.let { chatId ->
                    chatVm.sendMessage(chatId, messageText.value.trim(), otherUserId)
                    messageText.value = ""
                }
            }
        },
        onBack = onBack,
        otherUserId = otherUserId,
        isUserBlocked = isUserBlocked,
        onBlockUser = {
            otherUserId?.let { chatVm.blockUser(it) }
        },
        onUnblockUser = {
            otherUserId?.let { chatVm.unblockUser(it) }
        }
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
private fun ChatTopBar(
    onBack: () -> Unit, 
    otherUserName: String, 
    spacing: Spacing,
    otherUserId: String?,
    isUserBlocked: Boolean,
    onBlockUser: () -> Unit,
    onUnblockUser: () -> Unit
) {
	var showMenu by remember { mutableStateOf(false) }
	Row(
		verticalAlignment = Alignment.Companion.CenterVertically,
		modifier = Modifier.Companion.fillMaxWidth()
	) {
		IconButton(onClick = onBack) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.ArrowBack,
				contentDescription = "Back to chats"
			)
		}
		if (!profilePicture.isNullOrEmpty()) {
			AsyncImage(
				model = RetrofitClient.getPictureUri(profilePicture),
				contentDescription = stringResource(R.string.profile_picture),
				contentScale = ContentScale.Crop,
				modifier = Modifier.Companion
					.size(32.dp)
					.clip(CircleShape)
			)
		} else {
			Icon(
				imageVector = Icons.Default.Person,
				contentDescription = "Direct message",
				modifier = Modifier.Companion.size(32.dp),
				tint = MaterialTheme.colorScheme.onSurface
			)
		}
		Spacer(modifier = Modifier.Companion.width(spacing.small))
		Text(
			text = otherUserName,
			style = MaterialTheme.typography.titleLarge,
			fontWeight = FontWeight.Companion.SemiBold,
			modifier = Modifier.Companion.weight(1f)
		)
		
		// Three-dot menu
		IconButton(onClick = { showMenu = true }) {
			Icon(
				imageVector = Icons.Default.MoreVert,
				contentDescription = "More options"
			)
		}
		DropdownMenu(
			expanded = showMenu,
			onDismissRequest = { showMenu = false },
			modifier = Modifier.Companion.fillMaxWidth()
		) {
			if (otherUserId != null) {
				if (isUserBlocked) {
					DropdownMenuItem(
						text = { Text("Unblock User") },
						onClick = {
							onUnblockUser()
							showMenu = false
						}
					)
				} else {
					DropdownMenuItem(
						text = { Text("Block User") },
						onClick = {
							onBlockUser()
							showMenu = false
						}
					)
				}
			}
		}
	}
}

@Composable
private fun MessagesList(
	messages: List<Message>,
	currentUserId: String?,
	spacing: Spacing,
	listState: LazyListState,
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
private fun MessageBubbleMine(msg: Message, spacing: Spacing) {
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
}

@Composable
private fun MessageBubbleOther(msg: Message, spacing: Spacing) {
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
	spacing: Spacing,
	isBlocked: Boolean = false
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
			placeholder = { Text(if (isBlocked) "You have blocked this user" else "Type a message...") },
			modifier = Modifier.Companion
				.weight(1f)
				.padding(end = spacing.small),
			shape = RoundedCornerShape(24.dp),
			maxLines = 4,
			enabled = !isBlocked
		)
		IconButton(
			onClick = onSend,
			enabled = message.trim().isNotEmpty() && !isBlocked
		) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.Send,
				contentDescription = "Send message",
				tint = if (message.trim().isNotEmpty() && !isBlocked)
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
    listState: LazyListState,
    inputState: MutableState<String>,
    onSend: () -> Unit,
    onBack: () -> Unit,
    otherUserId: String?,
    isUserBlocked: Boolean,
    onBlockUser: () -> Unit,
    onUnblockUser: () -> Unit
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ChatTopBar(
            onBack = onBack, 
            otherUserName = otherUserName, 
            spacing = spacing,
            otherUserId = otherUserId,
            isUserBlocked = isUserBlocked,
            onBlockUser = onBlockUser,
            onUnblockUser = onUnblockUser
        )
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
            spacing = spacing,
            isBlocked = isUserBlocked
        )
    }
}