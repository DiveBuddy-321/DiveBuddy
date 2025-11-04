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
import com.cpen321.usermanagement.ui.viewmodels.EventUiState
import java.time.LocalDate
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.time.format.DateTimeFormatter
import java.util.Date

class EventFormState(initialEvent: Event?) {
    var eventTitle by mutableStateOf(initialEvent?.title ?: "")
    var eventDescription by mutableStateOf(initialEvent?.description ?: "")
    var eventLocation by mutableStateOf(initialEvent?.location ?: "")
    var selectedLocation by mutableStateOf<LocationResult?>(null)
    var selectedDate by mutableStateOf(initialEvent?.date?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate())
    var selectedTime by mutableStateOf(initialEvent?.date?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime())
    var requiredLevel by mutableStateOf(initialEvent?.skillLevel ?: "")
    var maxParticipants by mutableStateOf(initialEvent?.capacity?.toString() ?: "")
    var showDatePicker by mutableStateOf(false)
    var showTimePicker by mutableStateOf(false)
    var expandedExpDropdown by mutableStateOf(false)

    fun onLocationSelected(locationResult: LocationResult) {
        selectedLocation = locationResult
        eventLocation = locationResult.address
    }
}

@Composable
fun rememberEventFormState(event: Event?): EventFormState {
    return remember(event) { EventFormState(event) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel = hiltViewModel(),
    event: Event? = null // Optional event for editing
) {
    val isEditing = event != null
    val form = rememberEventFormState(event)
    val uiState by eventViewModel.uiState.collectAsState()
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
    val levelOptions = listOf(
        stringResource(R.string.beginner),
        stringResource(R.string.intermediate),
        stringResource(R.string.expert)
    )
    CreateEventContent(
        modifier = modifier,
        isEditing = isEditing,
        form = form,
        uiState = uiState,
        levelOptions = levelOptions,
        onBack = onDismiss,
        onSubmit = { submitEvent(form, event, isEditing, eventViewModel) }
    )
}

@Composable
private fun CreateEventContent(
    modifier: Modifier,
    isEditing: Boolean,
    form: EventFormState,
    uiState: EventUiState,
    levelOptions: List<String>,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.large)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        CreateEventHeader(modifier, isEditing, onBack)
        TitleField(form)
        DescriptionField(form)
        LocationField(form)
        DateField(form)
        TimeField(form)
        ExperienceDropdown(form, levelOptions)
        ParticipantsField(form)
        ErrorMessage(uiState, isEditing)
        FormActions(isEditing, form, uiState, onBack, onSubmit)
    }
    if (form.showDatePicker) {
        DatePickerModal(
            onDateSelected = { dateMillis ->
                dateMillis?.let {
                    form.selectedDate = Instant.ofEpochMilli(it)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                }
            },
            onDismiss = { form.showDatePicker = false }
        )
    }
    if (form.showTimePicker) {
        TimePickerModal(
            onTimeSelected = { hour, minute ->
                form.selectedTime = LocalTime.of(hour, minute)
                form.showTimePicker = false
            },
            onDismiss = { form.showTimePicker = false }
        )
    }
}

@Composable
private fun CreateEventHeader(
    modifier: Modifier,
    isEditing: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(name = R.drawable.ic_arrow_back)
        }
        Text(
            text = if (isEditing) "Edit Event" else stringResource(R.string.create_event),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TitleField(form: EventFormState) {
    OutlinedTextField(
        value = form.eventTitle,
        onValueChange = { form.eventTitle = it },
        label = { RequiredTextLabel(label = stringResource(R.string.event_title)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun DescriptionField(form: EventFormState) {
    OutlinedTextField(
        value = form.eventDescription,
        onValueChange = { form.eventDescription = it },
        label = { RequiredTextLabel(label = stringResource(R.string.description)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 5
    )
}

@Composable
private fun LocationField(form: EventFormState) {
    LocationAutocomplete(
        value = form.eventLocation,
        onValueChange = { form.eventLocation = it },
        onLocationSelected = { form.onLocationSelected(it) },
        label = stringResource(R.string.location),
        placeholder = stringResource(R.string.search_location),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DateField(form: EventFormState) {
    OutlinedTextField(
        value = form.selectedDate?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) ?: "",
        onValueChange = { },
        label = { RequiredTextLabel(label = stringResource(R.string.date)) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        placeholder = { Text(stringResource(R.string.select_date)) },
        trailingIcon = {
            TextButton(onClick = { form.showDatePicker = true }) {
                Text(stringResource(R.string.select))
            }
        }
    )
}

@Composable
private fun TimeField(form: EventFormState) {
    OutlinedTextField(
        value = form.selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "",
        onValueChange = { },
        label = { RequiredTextLabel(label = stringResource(R.string.time)) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        placeholder = { Text(stringResource(R.string.select_time)) },
        trailingIcon = {
            TextButton(onClick = { form.showTimePicker = true }) {
                Text(stringResource(R.string.select))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperienceDropdown(
    form: EventFormState,
    levelOptions: List<String>
) {
    ExposedDropdownMenuBox(
        expanded = form.expandedExpDropdown,
        onExpandedChange = { form.expandedExpDropdown = !form.expandedExpDropdown }
    ) {
        OutlinedTextField(
            value = form.requiredLevel,
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(R.string.min_exp_level)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = form.expandedExpDropdown) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = PrimaryEditable, enabled = true)
        )
        ExposedDropdownMenu(
            expanded = form.expandedExpDropdown,
            onDismissRequest = { form.expandedExpDropdown = false }
        ) {
            levelOptions.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level) },
                    onClick = {
                        form.requiredLevel = level
                        form.expandedExpDropdown = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ParticipantsField(form: EventFormState) {
    OutlinedTextField(
        value = form.maxParticipants,
        onValueChange = { form.maxParticipants = it },
        label = { Text(stringResource(R.string.max_participants)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun ErrorMessage(
    uiState: EventUiState,
    isEditing: Boolean
) {
    val spacing = LocalSpacing.current
    val errorMessage = if (isEditing) uiState.updateEventError else uiState.createEventError
    if (errorMessage != null) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = spacing.small)
        )
    }
}

@Composable
private fun FormActions(
    isEditing: Boolean,
    form: EventFormState,
    uiState: EventUiState,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.large, bottom = spacing.large),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.cancel),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Button(
            onClick = onSubmit,
            modifier = Modifier.weight(1f),
            enabled = form.eventTitle.isNotBlank() &&
                    form.eventDescription.isNotBlank() &&
                    form.eventLocation.isNotBlank() &&
                    form.selectedDate != null &&
                    form.selectedTime != null &&
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

private fun submitEvent(
    form: EventFormState,
    event: Event?,
    isEditing: Boolean,
    eventViewModel: EventViewModel
) {
    val date: LocalDate? = form.selectedDate
    val time: LocalTime? = form.selectedTime
    if (date != null && time != null) {
        val dateTime = date.atTime(time)
        val eventDate = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
        val request = CreateEventRequest(
            title = form.eventTitle,
            description = form.eventDescription,
            date = eventDate,
            capacity = form.maxParticipants.toIntOrNull() ?: 1,
            skillLevel = form.requiredLevel.takeIf { it.isNotBlank() },
            location = form.eventLocation,
            latitude = form.selectedLocation?.coordinates?.latitude,
            longitude = form.selectedLocation?.coordinates?.longitude,
            attendees = if (isEditing) event!!.attendees else emptyList(),
            photo = if (isEditing) event!!.photo else null
        )
        if (isEditing) {
            eventViewModel.updateEvent(event!!._id, request)
        } else {
            eventViewModel.createEvent(request)
        }
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
