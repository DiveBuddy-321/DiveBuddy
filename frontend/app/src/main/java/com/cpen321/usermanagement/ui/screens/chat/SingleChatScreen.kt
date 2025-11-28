package com.cpen321.usermanagement.ui.screens.chat

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
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
    val chatVm: ChatViewModel = chatViewModel
    val uiState by chatVm.uiState.collectAsState()
    val messages = remember { mutableStateOf<List<Message>>(emptyList()) }
    val listState = rememberLazyListState()
    val isLoadingMore = remember { mutableStateOf(false) }
    val messageText = remember { mutableStateOf("") }
    val context = LocalContext.current
    // Show toast when there's an error; use timestamp to trigger even for repeated messages
    LaunchedEffect(uiState.connectionState.errorTimestamp) {
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
	LaunchedEffect(listState) {
		snapshotFlow {
			val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
			val total = listState.layoutInfo.totalItemsCount
			Pair(lastVisible, total)
		}
			.distinctUntilChanged()
			.collect { (lastVisibleIndex, totalItems) ->
				if (shouldLoadMoreMessages(lastVisibleIndex, totalItems, isLoadingMore.value, messages.value)) {
					isLoadingMore.value = true
					val beforeTimestamp = formatMessageTimestamp(messages.value.lastOrNull())
					chat._id?.let { id ->
						chatVm.loadMessages(id, limit = 20, before = beforeTimestamp, append = true)
					}
					delay(1000)
					isLoadingMore.value = false
				}
			}
	}
	val header = buildHeaderState(chat = chat, chatVm = chatVm)
	val body = buildBodyState(
		messages = messages.value,
		currentUserId = uiState.userData.currentUserId,
		listState = listState,
		inputState = messageText
	)
	val actions = buildChatActions(
		onBack = onBack,
		chatVm = chatVm,
		otherUserId = header.otherUserId,
		messageText = messageText,
		chatId = chat._id
	)
	ChatContent(header = header, body = body,actions = actions)
}

private fun shouldLoadMoreMessages(
	lastVisibleIndex: Int?,
	totalItems: Int,
	isLoadingMore: Boolean,
	messages: List<Message>
): Boolean {
	val canLoadMore = !isLoadingMore && messages.isNotEmpty() && totalItems > 0
	val isNearEnd = lastVisibleIndex != null && lastVisibleIndex >= totalItems - 3
	return isNearEnd && canLoadMore
}

private fun formatMessageTimestamp(message: Message?): String? {
	return message?.createdAt?.let {
		SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
			timeZone = TimeZone.getTimeZone("UTC")
		}.format(it)
	}
}

private fun buildHeaderState(chat: Chat, chatVm: ChatViewModel): ChatHeaderState {
	val otherUserId = chatVm.getOtherUserId(chat)
	return ChatHeaderState(
		profilePicture = if (!chat.name.isNullOrEmpty()) null else chatVm.getOtherUserProfilePicture(chat),
		chatDisplayName = chatVm.getChatDisplayName(chat),
		isGroupChat = !chat.name.isNullOrEmpty(),
		otherUserId = otherUserId,
		isUserBlocked = otherUserId?.let { chatVm.isUserBlocked(it) } ?: false
	)
}

private fun buildBodyState(
	messages: List<Message>,
	currentUserId: String?,
	listState: LazyListState,
	inputState: MutableState<String>
): ChatBodyState {
	return ChatBodyState(
		messages = messages,
		currentUserId = currentUserId,
		listState = listState,
		inputState = inputState
	)
}

private fun buildChatActions(
	onBack: () -> Unit,
	chatVm: ChatViewModel,
	otherUserId: String?,
	messageText: MutableState<String>,
	chatId: String?
): ChatActions {
	return ChatActions(
		onBack = onBack,
		onBlockUser = { otherUserId?.let { chatVm.blockUser(it) } },
		onUnblockUser = { otherUserId?.let { chatVm.unblockUser(it) } },
		onSend = {
			if (messageText.value.trim().isNotEmpty()) {
				chatId?.let { cid ->
					chatVm.sendMessage(cid, messageText.value.trim(), otherUserId)
					messageText.value = ""
				}
			}
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

data class ChatHeaderState(
	val profilePicture: String?,
	val chatDisplayName: String,
	val isGroupChat: Boolean,
	val otherUserId: String?,
	val isUserBlocked: Boolean
)

data class ChatBodyState(
	val messages: List<Message>,
	val currentUserId: String?,
	val listState: LazyListState,
	val inputState: MutableState<String>
)

data class ChatActions(
	val onBack: () -> Unit,
	val onBlockUser: () -> Unit,
	val onUnblockUser: () -> Unit,
	val onSend: () -> Unit
)

@Composable
private fun ChatTopBar(
	header: ChatHeaderState,
	actions: ChatActions,
	spacing: Spacing
) {
	var showMenu by remember { mutableStateOf(false) }
	Row(
		verticalAlignment = Alignment.Companion.CenterVertically,
		modifier = Modifier.Companion.fillMaxWidth()
	) {
		IconButton(onClick = actions.onBack) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.ArrowBack,
				contentDescription = "Back to chats"
			)
		}
		ProfileAvatarSmall(profilePicture = header.profilePicture, isGroupChat = header.isGroupChat)
		Spacer(modifier = Modifier.Companion.width(spacing.small))
		Text(
			text = header.chatDisplayName,
			style = MaterialTheme.typography.titleLarge,
			fontWeight = FontWeight.Companion.SemiBold,
			modifier = Modifier.Companion.weight(1f)
		)
		
		// Three-dot menu (only show for direct messages, not group chats)
		if (!header.isGroupChat) {
			IconButton(onClick = { showMenu = true }) {
				Icon(
					imageVector = Icons.Default.MoreVert,
					contentDescription = "More options"
				)
			}
			TopBarMenu(
				expanded = showMenu,
				onDismiss = { showMenu = false },
				header = header,
				actions = actions
			)
		}
	}
}

@Composable
private fun ProfileAvatarSmall(profilePicture: String?, isGroupChat: Boolean) {
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
			contentDescription = if (isGroupChat) "Group chat" else "Direct message",
			modifier = Modifier.Companion.size(32.dp),
			tint = MaterialTheme.colorScheme.onSurface
		)
	}
}

@Composable
private fun TopBarMenu(
	expanded: Boolean,
	onDismiss: () -> Unit,
	header: ChatHeaderState,
	actions: ChatActions
) {
	DropdownMenu(
		expanded = expanded,
		onDismissRequest = onDismiss,
		modifier = Modifier.Companion.fillMaxWidth()
	) {
		if (header.otherUserId != null) {
			if (header.isUserBlocked) {
				DropdownMenuItem(
					text = { Text("Unblock User") },
					onClick = {
						actions.onUnblockUser()
						onDismiss()
					}
				)
			} else {
				DropdownMenuItem(
					text = { Text("Block User") },
					onClick = {
						actions.onBlockUser()
						onDismiss()
					}
				)
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
		horizontalArrangement = Arrangement.End,
		verticalAlignment = Alignment.Bottom
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
		Spacer(modifier = Modifier.Companion.width(spacing.small))
		// Profile picture and name on the right for own messages
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
			modifier = Modifier.width(48.dp)
		) {
			if (!msg.sender?.profilePicture.isNullOrEmpty()) {
				AsyncImage(
					model = RetrofitClient.getPictureUri(msg.sender?.profilePicture ?: ""),
					contentDescription = stringResource(R.string.profile_picture),
					contentScale = ContentScale.Crop,
					modifier = Modifier.size(32.dp).clip(CircleShape)
				)
			} else {
				Icon(
					imageVector = Icons.Default.Person,
					contentDescription = stringResource(R.string.profile_picture),
					modifier = Modifier.size(32.dp)
						.clip(CircleShape)
						.background(MaterialTheme.colorScheme.primaryContainer)
						.padding(6.dp),
					tint = MaterialTheme.colorScheme.onPrimaryContainer
				)
			}
			Text(
				text = msg.sender?.name ?: "Unknown",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}


@Composable
private fun MessageBubbleOther(msg: Message, spacing: Spacing) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Start,
		verticalAlignment = Alignment.Bottom
	) {
		// Profile picture and name on the left for other users' messages
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
			modifier = Modifier.width(48.dp)
		) {
			if (!msg.sender?.profilePicture.isNullOrEmpty()) {
				AsyncImage(
					model = RetrofitClient.getPictureUri(msg.sender?.profilePicture ?: ""),
					contentDescription = stringResource(R.string.profile_picture),
					contentScale = ContentScale.Crop,
					modifier = Modifier.size(32.dp).clip(CircleShape)
				)
			} else {
				Icon(
					imageVector = Icons.Default.Person,
					contentDescription = stringResource(R.string.profile_picture),
					modifier = Modifier.size(32.dp)
						.clip(CircleShape)
						.background(MaterialTheme.colorScheme.secondaryContainer)
						.padding(6.dp),
					tint = MaterialTheme.colorScheme.onSecondaryContainer
				)
			}
			Text(
				text = msg.sender?.name ?: "Unknown",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
		Spacer(modifier = Modifier.Companion.width(spacing.small))
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
	header: ChatHeaderState,
	body: ChatBodyState,
	actions: ChatActions
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ChatTopBar(
			header = header,
			actions = actions,
			spacing = spacing
        )
        MessagesList(
            messages = body.messages,
            currentUserId = body.currentUserId,
            spacing = spacing,
            listState = body.listState,
            modifier = Modifier.weight(1f)
        )
        MessageInputBar(
            message = body.inputState.value,
            onMessageChange = { body.inputState.value = it },
            onSend = actions.onSend,
            spacing = spacing,
            isBlocked = header.isUserBlocked
        )
    }
}