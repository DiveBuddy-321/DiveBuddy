package com.cpen321.usermanagement.ui.screens.events

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.ui.components.EventsMapView
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.events.EventViewModel
import com.cpen321.usermanagement.ui.viewmodels.events.EventUiState
import java.text.SimpleDateFormat
import java.util.Locale


data class EventNavigationState(
    val selectedEvent: Event? = null,
    val showCreateEventForm: Boolean = false,
    val showEditEventForm: Event? = null,
    val showAttendees: Event? = null,
    val showUserProfile: User? = null,
    val isMapView: Boolean = false
)

@Composable
private fun HandleEventMessages(
    uiState: EventUiState,
    snackbarHostState: SnackbarHostState,
    eventViewModel: EventViewModel
) {
    LaunchedEffect(uiState.joinSuccessMessage) {
        uiState.joinSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            eventViewModel.clearJoinEventState()
        }
    }
    
    LaunchedEffect(uiState.leaveSuccessMessage) {
        uiState.leaveSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            eventViewModel.clearLeaveEventState()
        }
    }
    
    LaunchedEffect(uiState.updateSuccessMessage) {
        uiState.updateSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            eventViewModel.clearUpdateEventState()
        }
    }
    
    LaunchedEffect(uiState.deleteSuccessMessage) {
        uiState.deleteSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            eventViewModel.clearDeleteEventState()
        }
    }
}

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel = hiltViewModel()
) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var showCreateEventForm by remember { mutableStateOf(false) }
    var showEditEventForm by remember { mutableStateOf<Event?>(null) }
    var showAttendees by remember { mutableStateOf<Event?>(null) }
    var showUserProfile by remember { mutableStateOf<User?>(null) }
    var isMapView by remember { mutableStateOf(false) }
    val uiState by eventViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    HandleEventMessages(uiState, snackbarHostState, eventViewModel)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        EventNavigationContent(
            modifier = modifier.padding(paddingValues),
            navigationState = EventNavigationState(
                selectedEvent = selectedEvent,
                showCreateEventForm = showCreateEventForm,
                showEditEventForm = showEditEventForm,
                showAttendees = showAttendees,
                showUserProfile = showUserProfile,
                isMapView = isMapView
            ),
            uiState = uiState,
            eventViewModel = eventViewModel,
            onNavigationChange = { navState ->
                selectedEvent = navState.selectedEvent
                showCreateEventForm = navState.showCreateEventForm
                showEditEventForm = navState.showEditEventForm
                showAttendees = navState.showAttendees
                showUserProfile = navState.showUserProfile
                isMapView = navState.isMapView
            }
        )
    }
}

@Composable
private fun EventNavigationContent(
    modifier: Modifier,
    navigationState: EventNavigationState,
    uiState: EventUiState,
    eventViewModel: EventViewModel,
    onNavigationChange: (EventNavigationState) -> Unit
) {
    when {
        navigationState.showCreateEventForm -> {
            CreateEventFormContent(
                navigationState = navigationState,
                eventViewModel = eventViewModel,
                onNavigationChange = onNavigationChange
            )
        }
        navigationState.showEditEventForm != null -> {
            EditEventFormContent(
                navigationState = navigationState,
                eventViewModel = eventViewModel,
                onNavigationChange = onNavigationChange
            )
        }
        navigationState.showUserProfile != null -> {
            UserProfileContent(
                navigationState = navigationState,
                onNavigationChange = onNavigationChange
            )
        }
        navigationState.showAttendees != null -> {
            AttendeesContent(
                navigationState = navigationState,
                onNavigationChange = onNavigationChange
            )
        }
        navigationState.selectedEvent != null -> {
            SingleEventContent(
                navigationState = navigationState,
                eventViewModel = eventViewModel,
                onNavigationChange = onNavigationChange
            )
        }
        else -> {
            DefaultEventsContent(
                modifier = modifier,
                navigationState = navigationState,
                uiState = uiState,
                eventViewModel = eventViewModel,
                onNavigationChange = onNavigationChange
            )
        }
    }
}

@Composable
private fun CreateEventFormContent(
    navigationState: EventNavigationState,
    eventViewModel: EventViewModel,
    onNavigationChange: (EventNavigationState) -> Unit
) {
    CreateEventScreen(
        onDismiss = {
            onNavigationChange(navigationState.copy(showCreateEventForm = false))
        },
        eventViewModel = eventViewModel
    )
}

@Composable
private fun EditEventFormContent(
    navigationState: EventNavigationState,
    eventViewModel: EventViewModel,
    onNavigationChange: (EventNavigationState) -> Unit
) {
    CreateEventScreen(
        event = navigationState.showEditEventForm,
        onDismiss = {
            onNavigationChange(navigationState.copy(showEditEventForm = null))
        },
        eventViewModel = eventViewModel
    )
}

@Composable
private fun UserProfileContent(
    navigationState: EventNavigationState,
    onNavigationChange: (EventNavigationState) -> Unit
) {
    AttendeeProfileScreen(
        user = navigationState.showUserProfile!!,
        onBack = {
            onNavigationChange(navigationState.copy(showUserProfile = null))
        }
    )
}

@Composable
private fun AttendeesContent(
    navigationState: EventNavigationState,
    onNavigationChange: (EventNavigationState) -> Unit
) {
    AttendeesScreen(
        attendeeIds = navigationState.showAttendees!!.attendees,
        onBack = {
            onNavigationChange(navigationState.copy(showAttendees = null))
        },
        onUserClick = { user ->
            onNavigationChange(navigationState.copy(showUserProfile = user))
        }
    )
}

@Composable
private fun SingleEventContent(
    navigationState: EventNavigationState,
    eventViewModel: EventViewModel,
    onNavigationChange: (EventNavigationState) -> Unit
) {
    SingleEventScreen(
        event = navigationState.selectedEvent!!,
        onBack = {
            onNavigationChange(navigationState.copy(
                selectedEvent = null,
                isMapView = navigationState.isMapView // Preserve view state
            ))
        },
        onEditEvent = { event ->
            onNavigationChange(navigationState.copy(showEditEventForm = event))
        },
        onShowAttendees = { event ->
            onNavigationChange(navigationState.copy(showAttendees = event))
        },
        eventViewModel = eventViewModel
    )
}

@Composable
private fun DefaultEventsContent(
    modifier: Modifier,
    navigationState: EventNavigationState,
    uiState: EventUiState,
    eventViewModel: EventViewModel,
    onNavigationChange: (EventNavigationState) -> Unit
) {
    EventsContent(
        modifier = modifier,
        uiState = uiState,
        onCreateEventClick = {
            onNavigationChange(navigationState.copy(showCreateEventForm = true))
        },
        onEventClick = { event ->
            onNavigationChange(navigationState.copy(
                selectedEvent = event,
                isMapView = navigationState.isMapView // Preserve current view state
            ))
        },
        onRefresh = {
            eventViewModel.refreshEvents()
        },
        initialIsMapView = navigationState.isMapView,
        onViewStateChange = { isMap ->
            onNavigationChange(navigationState.copy(isMapView = isMap))
        }
    )
}

@Composable
private fun CreateEventButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = "Create Event",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = "Error: $error",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onClick) {
            Text("Retry")
        }
    }
}

@Composable
private fun NoEventsMessage(
    modifier: Modifier = Modifier
) {
    Text(
        text = "No events available",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun EventsContent(
    modifier: Modifier = Modifier,
    uiState: EventUiState,
    onCreateEventClick: () -> Unit,
    onEventClick: (Event) -> Unit,
    onRefresh: () -> Unit,
    initialIsMapView: Boolean = false,
    onViewStateChange: (Boolean) -> Unit
) {
    var isMapView by remember { mutableStateOf(initialIsMapView) }
    
    // Update when initialIsMapView changes (when coming back from event detail)
    LaunchedEffect(initialIsMapView) {
        isMapView = initialIsMapView
    }

    Column(
    ) {
        EventsHeader(
            onCreateEventClick = onCreateEventClick,
            onRefresh = onRefresh,
            isMapView = isMapView,
            onViewToggle = {
                isMapView = !isMapView
                onViewStateChange(isMapView)
            }
        )

        if (isMapView) {
            EventsMapContent(
                uiState = uiState,
                onEventClick = onEventClick,
                onRefresh = onRefresh
            )
        } else {
            EventsListContent(
                uiState = uiState,
                onEventClick = onEventClick,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun EventsHeader(
    onCreateEventClick: () -> Unit,
    onRefresh: () -> Unit,
    isMapView: Boolean,
    onViewToggle: () -> Unit
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium)
            .padding(top = spacing.small, bottom = spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Events",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            IconButton(onClick = onViewToggle) {
                Icon(
                    imageVector = if (isMapView) Icons.Filled.List else Icons.Filled.LocationOn,
                    contentDescription = if (isMapView) "Switch to List View" else "Switch to Map View",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            CreateEventButton(
                onClick = onCreateEventClick,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onRefresh) {
                Text(
                    text = "Refresh",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun EventsListContent(
    uiState: EventUiState,
    onEventClick: (Event) -> Unit,
    onRefresh: () -> Unit
) {
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
            ErrorMessage(error = uiState.error, onClick = onRefresh)
        }
        uiState.events.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NoEventsMessage()
            }
        }
        else -> {
            EventsColumn(
                onEventClick = onEventClick,
                uiState = uiState
            )
        }
    }
}

@Composable
private fun EventsMapContent(
    uiState: EventUiState,
    onEventClick: (Event) -> Unit,
    onRefresh: () -> Unit
) {
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
            ErrorMessage(error = uiState.error, onClick = onRefresh)
        }
        uiState.events.isEmpty() -> {
            NoEventsMessage()
        }
        else -> {
            EventsMapView(
                events = uiState.events,
                onEventClick = onEventClick,
                modifier = Modifier.fillMaxSize()
            )
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
            modifier = Modifier
                .padding(spacing.medium),
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
