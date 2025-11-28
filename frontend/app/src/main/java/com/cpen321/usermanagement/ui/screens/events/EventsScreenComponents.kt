package com.cpen321.usermanagement.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.ui.components.events.EventFilter
import com.cpen321.usermanagement.ui.components.events.EventFilterDropdown
import com.cpen321.usermanagement.ui.components.events.EventSort
import com.cpen321.usermanagement.ui.components.events.EventSortDropdown
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.theme.Spacing

data class EventsHeaderViewState(
    val isMapView: Boolean,
    val selectedFilter: EventFilter,
    val selectedSort: EventSort
)

data class EventsHeaderActions(
    val onCreateEventClick: () -> Unit,
    val onRefresh: () -> Unit,
    val onViewToggle: () -> Unit,
    val onFilterChange: (EventFilter) -> Unit,
    val onSortChange: (EventSort) -> Unit
)

@Composable
fun NoEventsMessage(
    modifier: Modifier = Modifier
) {
    Text(
        text = "No events available",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsHeader(
    viewState: EventsHeaderViewState,
    actions: EventsHeaderActions
) {
    val spacing = LocalSpacing.current
    var filterExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        EventsHeaderTitle(
            isMapView = viewState.isMapView,
            onViewToggle = actions.onViewToggle
        )
        EventsHeaderControls(
            viewState = viewState,
            actions = actions,
            filterExpanded = filterExpanded,
            sortExpanded = sortExpanded,
            onFilterExpandedChange = { filterExpanded = it },
            onSortExpandedChange = { sortExpanded = it }
        )
    }
}

@Composable
private fun EventsHeaderTitle(
    isMapView: Boolean,
    onViewToggle: () -> Unit
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
private fun EventsHeaderControls(
    viewState: EventsHeaderViewState,
    actions: EventsHeaderActions,
    filterExpanded: Boolean,
    sortExpanded: Boolean,
    onFilterExpandedChange: (Boolean) -> Unit,
    onSortExpandedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        EventFilterDropdown(
            selectedFilter = viewState.selectedFilter,
            onFilterChange = { 
                actions.onFilterChange(it)
                onFilterExpandedChange(false)
            },
            expanded = filterExpanded,
            onExpandedChange = onFilterExpandedChange,
            modifier = Modifier.width(150.dp)
        )
        EventSortDropdown(
            selectedSort = viewState.selectedSort,
            onSortChange = {
                actions.onSortChange(it)
                onSortExpandedChange(false)
            },
            expanded = sortExpanded,
            onExpandedChange = onSortExpandedChange,
            enabled = !viewState.isMapView,
            modifier = Modifier.width(175.dp)
        )
        IconButton(onClick = actions.onRefresh) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Refresh Events List",
            )
        }
    }
}

fun filterEventsByType(
    events: List<Event>,
    currentUser: User?,
    filter: EventFilter
): List<Event> {
    return when (filter) {
        EventFilter.ALL -> events
        EventFilter.JOINED -> {
            val joinedIds = currentUser?.eventsJoined ?: emptyList()
            events.filter { it._id in joinedIds }
        }
        EventFilter.CREATED -> {
            val createdIds = currentUser?.eventsCreated ?: emptyList()
            events.filter { it._id in createdIds }
        }
    }
}

fun sortEventsByType(events: List<Event>, sort: EventSort): List<Event> {
    return when (sort) {
        EventSort.NAME_ASC -> events.sortedBy { it.title.lowercase() }
        EventSort.NAME_DESC -> events.sortedByDescending { it.title.lowercase() }
        EventSort.DATE_ASC -> events.sortedBy { it.date }
        EventSort.DATE_DESC -> events.sortedByDescending { it.date }
    }
}

