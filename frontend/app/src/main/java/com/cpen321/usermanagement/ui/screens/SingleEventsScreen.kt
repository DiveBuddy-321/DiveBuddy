package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
            // Navigate back immediately, success message will be shown in EventsScreen
            onBack()
            // Clear only the flags to prevent re-triggering, keep success message
            eventViewModel.clearJoinEventFlags()
        }
    }
    
    // Handle leave event success/error
    LaunchedEffect(uiState.eventLeft) {
        if (uiState.eventLeft) {
            // Navigate back immediately, success message will be shown in EventsScreen
            onBack()
            // Clear only the flags to prevent re-triggering, keep success message
            eventViewModel.clearLeaveEventFlags()
        }
    }
    
    // Check if user is attending the event
    val isUserAttending = eventViewModel.isUserAttendingEvent(event)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to events screen",
                )
            }

            // Event title
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

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
