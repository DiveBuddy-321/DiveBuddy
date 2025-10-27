package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.ui.screens.CreateEventScreen
import com.cpen321.usermanagement.ui.screens.SingleEventScreen
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.EventViewModel
import com.cpen321.usermanagement.ui.viewmodels.EventUiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel = hiltViewModel()
) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var showCreateEventForm by remember { mutableStateOf(false) }
    var showEditEventForm by remember { mutableStateOf<Event?>(null) }
    val uiState by eventViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle success messages
    LaunchedEffect(uiState.joinSuccessMessage) {
        uiState.joinSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            // Clear only the success message, not the entire state
            eventViewModel.clearJoinEventState()
        }
    }
    
    LaunchedEffect(uiState.leaveSuccessMessage) {
        uiState.leaveSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            // Clear only the success message, not the entire state
            eventViewModel.clearLeaveEventState()
        }
    }
    
    LaunchedEffect(uiState.updateSuccessMessage) {
        uiState.updateSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            // Clear only the success message, not the entire state
            eventViewModel.clearUpdateEventState()
        }
    }
    
    LaunchedEffect(uiState.deleteSuccessMessage) {
        uiState.deleteSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            // Clear only the success message, not the entire state
            eventViewModel.clearDeleteEventState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            showCreateEventForm -> {
                CreateEventScreen(
                    onDismiss = {
                        showCreateEventForm = false
                    },
                    eventViewModel = eventViewModel
                )
            }
            showEditEventForm != null -> {
                CreateEventScreen(
                    event = showEditEventForm!!,
                    onDismiss = {
                        showEditEventForm = null
                    },
                    eventViewModel = eventViewModel
                )
            }
            selectedEvent != null -> {
                SingleEventScreen(
                    event = selectedEvent!!,
                    onBack = {
                        selectedEvent = null
                    },
                    onEditEvent = { event ->
                        showEditEventForm = event
                    },
                    eventViewModel = eventViewModel
                )
            }
            else -> {
                EventsContent(
                    modifier = modifier.padding(paddingValues),
                    uiState = uiState,
                    onCreateEventClick = {
                        showCreateEventForm = true
                    },
                    onEventClick = { event ->
                        selectedEvent = event
                    },
                    onRefresh = {
                        eventViewModel.refreshEvents()
                    }
                )
            }
        }
    }
}

@Composable
private fun EventsContent(
    modifier: Modifier = Modifier,
    uiState: EventUiState,
    onCreateEventClick: () -> Unit,
    onEventClick: (Event) -> Unit,
    onRefresh: () -> Unit
) {
    val spacing = LocalSpacing.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        modifier = modifier
            .fillMaxSize()
    ) {
        Button(
            onClick = {
                onCreateEventClick()
            },
            modifier = Modifier.padding(spacing.large, spacing.medium),
        ) {
            Text(
                text = "Create Event",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Text(
                text = "Upcoming Events",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = spacing.small)
            )
            Button(
                onClick = onRefresh
            ) {
                Text(
                    text = "Refresh Events",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
            }
            uiState.events.isEmpty() -> {
                Text(
                    text = "No events available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                EventsColumn(
                    onEventClick = onEventClick,
                    uiState = uiState
                )
            }
        }
    }
}

@Composable
private fun EventsColumn(
    onEventClick: (Event) -> Unit,
    uiState: EventUiState
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        modifier = Modifier.padding(horizontal = spacing.large)
    ) {
        items(uiState.events.reversed()) { event ->
            EventCard(
                event = event,
                onClick = { onEventClick(event) }
            )
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (event.location != null) {
                Text(
                    text = "📍 ${event.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Text(
                    text = "📅 ${dateFormatter.format(event.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "👥 ${event.attendees.size}/${event.capacity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (event.skillLevel != null) {
                    Text(
                        text = "🤿 Level: ${event.skillLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
