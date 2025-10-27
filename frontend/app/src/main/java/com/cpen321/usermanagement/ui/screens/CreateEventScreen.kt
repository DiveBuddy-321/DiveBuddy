package com.cpen321.usermanagement.ui.screens

import Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType.Companion.PrimaryEditable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.dto.CreateEventRequest
import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.ui.components.LocationAutocomplete
import com.cpen321.usermanagement.ui.components.LocationResult
import com.cpen321.usermanagement.ui.components.RequiredTextLabel
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.EventViewModel
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.time.format.DateTimeFormatter
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel = hiltViewModel(),
    event: Event? = null // Optional event for editing
) {
    val isEditing = event != null
    
    // Initialize form fields with event data if editing, empty if creating
    var eventTitle by remember { mutableStateOf(event?.title ?: "") }
    var eventDescription by remember { mutableStateOf(event?.description ?: "") }
    var eventLocation by remember { mutableStateOf(event?.location ?: "") }
    var selectedLocation by remember { mutableStateOf<LocationResult?>(null) }
    
    // Parse event date to LocalDate and LocalTime if editing
    val eventDate = event?.date?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
    val eventTime = event?.date?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime()
    
    var selectedDate by remember { mutableStateOf(eventDate) }
    var selectedTime by remember { mutableStateOf(eventTime) }
    var requiredLevel by remember { mutableStateOf(event?.skillLevel ?: "") }
    var maxParticipants by remember { mutableStateOf(event?.capacity?.toString() ?: "") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var expandedExpDropdown by remember { mutableStateOf(false) }
    
    val levelOptions = listOf(
        stringResource(R.string.beginner),
        stringResource(R.string.intermediate),
        stringResource(R.string.expert)
    )
    
    val spacing = LocalSpacing.current
    
    // Collect ViewModel state
    val uiState by eventViewModel.uiState.collectAsState()
    
    // Handle successful event creation/update
    LaunchedEffect(uiState.eventCreated, uiState.eventUpdated) {
        if (uiState.eventCreated || uiState.eventUpdated) {
            onDismiss()
            if (isEditing) {
                eventViewModel.clearUpdateEventState()
            } else {
                eventViewModel.clearCreateEventState()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.large)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        // Header w/ Back Button
        Row (
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(name = R.drawable.ic_arrow_back)
            }
            Text(
                text = if (isEditing) "Edit Event" else stringResource(R.string.create_event),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Event Title
        OutlinedTextField(
            value = eventTitle,
            onValueChange = { eventTitle = it },
            label = { RequiredTextLabel(label = stringResource(R.string.event_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Event Description
        OutlinedTextField(
            value = eventDescription,
            onValueChange = { eventDescription = it },
            label = { RequiredTextLabel(label = stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        // Event Location
        LocationAutocomplete(
            value = eventLocation,
            onValueChange = { eventLocation = it },
            onLocationSelected = { locationResult ->
                selectedLocation = locationResult
                eventLocation = locationResult.address
            },
            label = stringResource(R.string.location),
            placeholder = stringResource(R.string.search_location),
            modifier = Modifier.fillMaxWidth()
        )

        // Date Picker
        OutlinedTextField(
            value = selectedDate?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) ?: "",
            onValueChange = { },
            label = { RequiredTextLabel(label = stringResource(R.string.date)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            placeholder = { Text(stringResource(R.string.select_date)) },
            trailingIcon = {
                TextButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.select))
                }
            }
        )

        // Time Picker
        OutlinedTextField(
            value = selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "",
            onValueChange = { },
            label = { RequiredTextLabel(label = stringResource(R.string.time)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            placeholder = { Text(stringResource(R.string.select_time)) },
            trailingIcon = {
                TextButton(onClick = { showTimePicker = true }) {
                    Text(stringResource(R.string.select))
                }
            }
        )

        // Minimum Experience Level Dropdown
        ExposedDropdownMenuBox(
            expanded = expandedExpDropdown,
            onExpandedChange = { expandedExpDropdown = !expandedExpDropdown }
        ) {
            OutlinedTextField(
                value = requiredLevel,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.min_exp_level)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExpDropdown) },
                modifier = Modifier.fillMaxWidth().menuAnchor(type = PrimaryEditable, enabled = true)
            )
            ExposedDropdownMenu(
                expanded = expandedExpDropdown,
                onDismissRequest = { expandedExpDropdown = false }
            ) {
                levelOptions.forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level) },
                        onClick = {
                            requiredLevel = level
                            expandedExpDropdown = false
                        }
                    )
                }
            }
        }

        // Max Participants
        OutlinedTextField(
            value = maxParticipants,
            onValueChange = { maxParticipants = it },
            label = { Text(stringResource(R.string.max_participants)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // TODO: Add image upload field
        
        // Error message display
        val errorMessage = if (isEditing) uiState.updateEventError else uiState.createEventError
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = spacing.small)
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.large, bottom = spacing.large),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    if (selectedDate != null && selectedTime != null) {
                        // Combine date and time into a single Date object
                        val dateTime = selectedDate!!.atTime(selectedTime!!)
                        val eventDate = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
                        
                        val eventRequest = CreateEventRequest(
                            title = eventTitle,
                            description = eventDescription,
                            date = eventDate,
                            capacity = maxParticipants.toIntOrNull() ?: 1,
                            skillLevel = requiredLevel.takeIf { it.isNotBlank() },
                            location = eventLocation,
                            latitude = selectedLocation?.coordinates?.latitude,
                            longitude = selectedLocation?.coordinates?.longitude,
                            attendees = if (isEditing) event.attendees else emptyList(), // Keep existing attendees when editing
                            photo = if (isEditing) event.photo else null // Keep existing photo when editing
                        )
                        
                        if (isEditing) {
                            eventViewModel.updateEvent(event._id, eventRequest)
                        } else {
                            eventViewModel.createEvent(eventRequest)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = eventTitle.isNotBlank() &&
                         eventDescription.isNotBlank() &&
                         eventLocation.isNotBlank() &&
                         selectedDate != null &&
                         selectedTime != null &&
                         (!uiState.isCreatingEvent && !uiState.isUpdatingEvent)
            ) {
                val isLoading = if (isEditing) uiState.isUpdatingEvent else uiState.isCreatingEvent
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (isEditing) "Update Event" else stringResource(R.string.create_event),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { dateMillis ->
                dateMillis?.let {
                    selectedDate = Instant.ofEpochMilli(it)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                }
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerModal(
            onTimeSelected = { hour, minute ->
                selectedTime = LocalTime.of(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModal(
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.select_time))
        },
        text = {
            TimeInput(state = timePickerState)
        },
        confirmButton = {
            Button(
                onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
