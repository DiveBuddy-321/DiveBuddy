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
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SingleEventScreen(
    event: Event,
    onBack: () -> Unit,
    eventViewModel: EventViewModel,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.US)
    val uiState by eventViewModel.uiState.collectAsState()
    
    // Handle join event success/error
    LaunchedEffect(uiState.eventJoined) {
        if (uiState.eventJoined) {
            onBack()
            eventViewModel.clearJoinEventFlags()
        }
    }
    
    // Handle leave event success/error
    LaunchedEffect(uiState.eventLeft) {
        if (uiState.eventLeft) {
            onBack()
            eventViewModel.clearLeaveEventFlags()
        }
    }
    
    // Handle delete event success/error
    LaunchedEffect(uiState.eventDeleted) {
        if (uiState.eventDeleted) {
            onBack()
            eventViewModel.clearDeleteEventFlags()
        }
    }
    
    // Check if user is attending the event
    val isUserAttending = eventViewModel.isUserAttendingEvent(event)
    
    // Check if user is the creator of the event
    val isUserCreator = eventViewModel.isUserEventCreator(event)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
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
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        // Event title
        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Event description
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(spacing.medium)
            )
        }

        // Event details card
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Date and time
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(spacing.medium))
                    Column {
                        Text(
                            text = "Date & Time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormatter.format(event.date),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Location
                if (event.location != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.width(spacing.medium))
                        Column {
                            Text(
                                text = "Location",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Capacity and attendees
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(spacing.medium))
                    Column {
                        Text(
                            text = "Attendees",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${event.attendees.size} / ${event.capacity} people",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Skill level
                if (event.skillLevel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🤿",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.width(spacing.medium))
                        Column {
                            Text(
                                text = "Skill Level",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = event.skillLevel,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Register/Leave button
        Button(
            onClick = { 
                if (isUserAttending) {
                    eventViewModel.leaveEvent(event._id)
                } else {
                    eventViewModel.joinEvent(event._id)
                }
            },
            enabled = !uiState.isJoiningEvent && !uiState.isLeavingEvent,
            modifier = Modifier
                .width(200.dp)
                .height(56.dp)
                .align(Alignment.CenterHorizontally),
        ) {
            if (uiState.isJoiningEvent || uiState.isLeavingEvent) {
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
        
        // Show error message if join/leave failed
        if (uiState.joinEventError != null) {
            Text(
                text = uiState.joinEventError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        
        if (uiState.leaveEventError != null) {
            Text(
                text = uiState.leaveEventError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun OptionsMenu(
    event: Event,
    eventViewModel: EventViewModel,
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
        DropdownMenu(
            expanded = showOptionMenu,
            onDismissRequest = { showOptionMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { 
                    showOptionMenu = false
                    /* Handle edit event - TODO: Implement edit functionality */
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { 
                    showOptionMenu = false
                    showDeleteDialog = true
                }
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete \"${event.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        eventViewModel.deleteEvent(event._id)
                    },
                    enabled = !uiState.isDeletingEvent,
                    colors = buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (uiState.isDeletingEvent) {
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
                Button(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show error message if delete failed
    if (uiState.deleteEventError != null) {
        AlertDialog(
            onDismissRequest = { eventViewModel.clearDeleteEventState() },
            title = { Text("Error") },
            text = { Text(uiState.deleteEventError!!) },
            confirmButton = {
                Button(
                    onClick = { eventViewModel.clearDeleteEventState() }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
