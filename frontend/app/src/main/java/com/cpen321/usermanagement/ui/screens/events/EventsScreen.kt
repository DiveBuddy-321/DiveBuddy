package com.cpen321.usermanagement.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.ui.components.events.EventsMapContent
import com.cpen321.usermanagement.ui.components.events.EventFilter
import com.cpen321.usermanagement.ui.components.events.EventFilterDropdown
import com.cpen321.usermanagement.ui.components.events.EventSort
import com.cpen321.usermanagement.ui.components.events.EventSortDropdown
import com.cpen321.usermanagement.ui.components.events.EventsList
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.events.EventViewModel
import com.cpen321.usermanagement.ui.viewmodels.events.EventUiState

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
    modifier: Modifier = Modifier,
    navigationState: EventNavigationState,
    uiState: EventUiState,
    eventViewModel: EventViewModel,
    onNavigationChange: (EventNavigationState) -> Unit
) {
    when {
        navigationState.showCreateEventForm -> {
            ShowCreateEventForm(eventViewModel, navigationState, onNavigationChange)
        }
        navigationState.showEditEventForm != null -> {
            ShowEditEventForm(navigationState, eventViewModel, onNavigationChange)
        }
        navigationState.showUserProfile != null -> {
            ShowUserProfile(navigationState, onNavigationChange)
        }
        navigationState.showAttendees != null -> {
            ShowAttendees(navigationState, onNavigationChange)
        }
        navigationState.selectedEvent != null -> {
            ShowSingleEvent(navigationState, eventViewModel, onNavigationChange)
        }
        else -> {
            ShowEventsList(modifier, uiState, navigationState, eventViewModel, onNavigationChange)
        }
    }
}

@Composable
private fun ShowCreateEventForm(
    eventViewModel: EventViewModel,
    navigationState: EventNavigationState,
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
private fun ShowEditEventForm(
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
private fun ShowUserProfile(
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
private fun ShowAttendees(
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
private fun ShowSingleEvent(
    navigationState: EventNavigationState,
    eventViewModel: EventViewModel,
    onNavigationChange: (EventNavigationState) -> Unit
) {
    SingleEventScreen(
        event = navigationState.selectedEvent!!,
        onBack = {
            onNavigationChange(navigationState.copy(
                selectedEvent = null,
                isMapView = navigationState.isMapView
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
private fun ShowEventsList(
    modifier: Modifier,
    uiState: EventUiState,
    navigationState: EventNavigationState,
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
                isMapView = navigationState.isMapView
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
private fun EventsContent(
    modifier: Modifier = Modifier,
    uiState: EventUiState,
    onCreateEventClick: () -> Unit,
    onEventClick: (Event) -> Unit,
    onRefresh: () -> Unit,
    initialIsMapView: Boolean = false,
    onViewStateChange: (Boolean) -> Unit
) {
    val eventsState = rememberEventsState(
        uiState = uiState,
        initialIsMapView = initialIsMapView,
        onViewStateChange = onViewStateChange
    )

    EventsMainContent(
        uiState = uiState,
        eventsState = eventsState,
        onCreateEventClick = onCreateEventClick,
        onEventClick = onEventClick,
        onRefresh = onRefresh
    )
}

@Composable
private fun rememberEventsState(
    uiState: EventUiState,
    initialIsMapView: Boolean,
    onViewStateChange: (Boolean) -> Unit
): EventsState {
    var isMapView by remember { mutableStateOf(initialIsMapView) }
    var selectedFilter by remember { mutableStateOf(EventFilter.ALL) }
    var selectedSort by remember { mutableStateOf(EventSort.DATE_ASC) }
    
    LaunchedEffect(initialIsMapView) {
        isMapView = initialIsMapView
    }

    val filteredEvents = remember(uiState.events, uiState.currentUser, selectedFilter) {
        filterEventsByType(uiState.events, uiState.currentUser, selectedFilter)
    }
    
    val sortedEvents = remember(filteredEvents, selectedSort) {
        sortEventsByType(filteredEvents, selectedSort)
    }

    return EventsState(
        isMapView = isMapView,
        selectedFilter = selectedFilter,
        selectedSort = selectedSort,
        filteredEvents = filteredEvents,
        sortedEvents = sortedEvents,
        onIsMapViewChange = { newValue ->
            isMapView = newValue
            onViewStateChange(newValue)
        },
        onFilterChange = { selectedFilter = it },
        onSortChange = { selectedSort = it }
    )
}

private data class EventsState(
    val isMapView: Boolean,
    val selectedFilter: EventFilter,
    val selectedSort: EventSort,
    val filteredEvents: List<Event>,
    val sortedEvents: List<Event>,
    val onIsMapViewChange: (Boolean) -> Unit,
    val onFilterChange: (EventFilter) -> Unit,
    val onSortChange: (EventSort) -> Unit
)

@Composable
private fun EventsMainContent(
    uiState: EventUiState,
    eventsState: EventsState,
    onCreateEventClick: () -> Unit,
    onEventClick: (Event) -> Unit,
    onRefresh: () -> Unit
) {
    val spacing = LocalSpacing.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            EventsHeader(
                viewState = EventsHeaderViewState(
                    isMapView = eventsState.isMapView,
                    selectedFilter = eventsState.selectedFilter,
                    selectedSort = eventsState.selectedSort
                ),
                actions = EventsHeaderActions(
                    onCreateEventClick = onCreateEventClick,
                    onRefresh = onRefresh,
                    onViewToggle = {
                        eventsState.onIsMapViewChange(!eventsState.isMapView)
                    },
                    onFilterChange = eventsState.onFilterChange,
                    onSortChange = eventsState.onSortChange
                )
            )

            if (eventsState.isMapView) {
                EventsMapContent(
                    events = eventsState.filteredEvents,
                    uiState = uiState,
                    onEventClick = onEventClick,
                    onRefresh = onRefresh
                )
            } else {
                EventsList(
                    events = eventsState.sortedEvents,
                    uiState = uiState,
                    onEventClick = onEventClick,
                    onRefresh = onRefresh
                )
            }
        }

        FloatingActionButton(
            onClick = onCreateEventClick,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(spacing.medium)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create Event"
            )
        }
    }
}

@Composable
private fun EventTitle(
    onViewToggle: () -> Unit,
    isMapView: Boolean
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
                imageVector = if (isMapView) Icons.AutoMirrored.Filled.List else Icons.Filled.LocationOn,
                contentDescription = if (isMapView) "Switch to List View" else "Switch to Map View",
            )
        }
    }
}

@Composable
private fun EventFilterSort(
    isMapView: Boolean,
    onRefresh: () -> Unit,
    selectedFilter: EventFilter,
    onFilterChange: (EventFilter) -> Unit,
    selectedSort: EventSort,
    onSortChange: (EventSort) -> Unit
) {
    var filterExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        EventFilterDropdown(
            selectedFilter = selectedFilter,
            onFilterChange = { 
                onFilterChange(it)
                filterExpanded = false
            },
            expanded = filterExpanded,
            onExpandedChange = { filterExpanded = it },
            modifier = Modifier.width(150.dp)
        )
        
        // Only enable sort dropdown in list view
        EventSortDropdown(
            selectedSort = selectedSort,
            onSortChange = {
                onSortChange(it)
                sortExpanded = false
            },
            expanded = sortExpanded,
            onExpandedChange = { sortExpanded = it },
            enabled = !isMapView,
            modifier = Modifier.width(175.dp)
        )
        
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Refresh Events List",
            )
        }
    }
}
