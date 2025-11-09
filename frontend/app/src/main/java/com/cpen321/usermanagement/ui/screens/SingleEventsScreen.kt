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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.EventViewModel
import com.cpen321.usermanagement.ui.viewmodels.EventUiState
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.layout.wrapContentWidth


@Composable
private fun HandleEventMessages(
    uiState: EventUiState,
    onBack: () -> Unit,
    eventViewModel: EventViewModel
) {
    LaunchedEffect(uiState.eventJoined) {
        if (uiState.eventJoined) {
            onBack()
            eventViewModel.clearJoinEventFlags()
        }
    }
    LaunchedEffect(uiState.eventLeft) {
        if (uiState.eventLeft) {
            onBack()
            eventViewModel.clearLeaveEventFlags()
        }
    }
    LaunchedEffect(uiState.eventDeleted) {
        if (uiState.eventDeleted) {
            onBack()
            eventViewModel.clearDeleteEventFlags()
        }
    }
}

@Composable
fun SingleEventScreen(
    event: Event,
    onBack: () -> Unit,
    onEditEvent: (Event) -> Unit,
    eventViewModel: EventViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by eventViewModel.uiState.collectAsState()
    HandleEventMessages(uiState, onBack, eventViewModel)
    val spacing = LocalSpacing.current
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.US)

    // Get the updated event from the ViewModel if it exists, otherwise use the original event
    val updatedEvent = uiState.events.find { it._id == event._id } ?: event

    // Check user state
    val isUserAttending = eventViewModel.isUserAttendingEvent(event)
    val isUserCreator = eventViewModel.isUserEventCreator(event)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        SingleEventTopBar(
            onBack = onBack,
            isUserCreator = isUserCreator,
            event = updatedEvent,
            eventViewModel = eventViewModel,
            onEditEvent = onEditEvent
        )
        EventTitle(title = updatedEvent.title)

        EventDescriptionCard(description = updatedEvent.description)

        EventDetailsCard(
            dateText = dateFormatter.format(updatedEvent.date),
            location = updatedEvent.location,
            attendeesText = "${updatedEvent.attendees.size} / ${updatedEvent.capacity} people",
            skillLevel = updatedEvent.skillLevel
        )
        Spacer(modifier = Modifier.weight(1f))

        RegisterLeaveButton(
            isUserAttending = isUserAttending,
            isBusy = uiState.isJoiningEvent || uiState.isLeavingEvent,
            enabled = !uiState.isJoiningEvent && !uiState.isLeavingEvent,
            onClick = {
                if (isUserAttending) eventViewModel.leaveEvent(updatedEvent._id)
                else eventViewModel.joinEvent(updatedEvent._id)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        ErrorMessages(
            joinError = uiState.joinEventError,
            leaveError = uiState.leaveEventError
        )
    }
}

@Composable
private fun SingleEventTopBar(
    onBack: () -> Unit,
    isUserCreator: Boolean,
    event: Event,
    eventViewModel: EventViewModel,
    onEditEvent: (Event) -> Unit,
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to events screen",
            )
        }

        Text(
            text = "Event Details",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (isUserCreator) {
            OptionsMenu(
                event = event,
                eventViewModel = eventViewModel,
                onEditEvent = onEditEvent,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = spacing.medium)
            )
        }
    }
}

@Composable
private fun EventTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun EventDescriptionCard(description: String) {
    val spacing = LocalSpacing.current
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(spacing.medium)
        )
    }
}

@Composable
private fun EventDetailsCard(
    dateText: String,
    location: String?,
    attendeesText: String,
    skillLevel: String?
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            DetailsRow(icon = "📅", label = "Date & Time", value = dateText)
            if (location != null) {
                DetailsRow(icon = "📍", label = "Location", value = location)
            }
            DetailsRow(icon = "👥", label = "Attendees", value = attendeesText)
            if (skillLevel != null) {
                DetailsRow(icon = "🤿", label = "Skill Level", value = skillLevel)
            }
        }
    }
}

@Composable
private fun DetailsRow(icon: String, label: String, value: String) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RegisterLeaveButton(
    isUserAttending: Boolean,
    isBusy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .width(200.dp)
            .height(56.dp),
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.width(20.dp).height(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = if (isUserAttending) "Leave Event" else "Register Event",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun ErrorMessages(
    joinError: String?,
    leaveError: String?,
) {
    if (joinError != null) {
        Text(
            text = joinError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }

    if (leaveError != null) {
        Text(
            text = leaveError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun OptionsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
                onDismissRequest()
                onEdit()
            }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDismissRequest()
                onDelete()
            }
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    eventTitle: String,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Event") },
        text = { Text("Are you sure you want to delete \"$eventTitle\"? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp).height(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(errorMessage) },
        confirmButton = {
            Button(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
private fun OptionsMenu(
    event: Event,
    eventViewModel: EventViewModel,
    onEditEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    var showOptionMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val spacing = LocalSpacing.current
    val uiState by eventViewModel.uiState.collectAsState()

    Box(
        modifier = modifier.padding(spacing.medium)
    ) {
        IconButton(onClick = { showOptionMenu = !showOptionMenu }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
            )
        }
        OptionsDropdownMenu(
            expanded = showOptionMenu,
            onDismissRequest = { showOptionMenu = false },
            onEdit = { onEditEvent(event) },
            onDelete = { showDeleteDialog = true }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            eventTitle = event.title,
            isDeleting = uiState.isDeletingEvent,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                eventViewModel.deleteEvent(event._id)
            }
        )
    }

    // Show error message if delete failed
    if (uiState.deleteEventError != null) {
        DeleteErrorDialog(
            errorMessage = uiState.deleteEventError!!,
            onDismiss = { eventViewModel.clearDeleteEventState() }
        )
    }
}
