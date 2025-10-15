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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.components.RequiredTextLabel
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var eventTitle by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var requiredLevel by remember { mutableStateOf("") }
    var maxParticipants by remember { mutableStateOf("") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var expandedExpDropdown by remember { mutableStateOf(false) }
    
    val levelOptions = listOf(
        stringResource(R.string.beginner),
        stringResource(R.string.intermediate),
        stringResource(R.string.advanced)
    )
    
    val spacing = LocalSpacing.current

    // FIXME: too much white space at the top of the screen
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CreateEventTopBar(onBackClick = onDismiss)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.large)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
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
            // TODO: Implement Google Maps integration
            OutlinedTextField(
                value = eventLocation,
                onValueChange = { eventLocation = it },
                label = { RequiredTextLabel(label = stringResource(R.string.location)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date Picker
            // FIXME: date in text field does not match to date selected in modal (one day before)
            OutlinedTextField(
                value = selectedDate?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) ?: "",
                onValueChange = { },
                label = { RequiredTextLabel(label = stringResource(R.string.date)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                placeholder = { Text(stringResource(R.string.select_date)) },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Select")
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
                        Text("Select")
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
                        // TODO: Handle event creation logic here
                        println("Event Title: $eventTitle")
                        println("Event Description: $eventDescription")
                        println("Event Location: $eventLocation")
                        println("Event Date: $selectedDate")
                        println("Event Time: $selectedTime")
                        println("Required Level: $requiredLevel")
                        println("Max Participants: $maxParticipants")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = eventTitle.isNotBlank() && 
                             eventDescription.isNotBlank() &&
                             eventLocation.isNotBlank() &&
                             selectedDate != null && 
                             selectedTime != null
                ) {
                    Text(
                        text = "Create Event",
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
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = it
                    selectedDate = LocalDate.of(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
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
private fun CreateEventTopBar(
    onBackClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.create_event),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(name = R.drawable.ic_arrow_back)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
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
                onClick = {onTimeSelected(timePickerState.hour, timePickerState.minute)}
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


