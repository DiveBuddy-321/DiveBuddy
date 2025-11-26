package com.cpen321.usermanagement.ui.components.events

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class EventFilter {
    ALL,
    JOINED,
    CREATED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFilterDropdown(
    selectedFilter: EventFilter,
    onFilterChange: (EventFilter) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val filterLabels = mapOf(
        EventFilter.ALL to "All Events",
        EventFilter.JOINED to "Joined",
        EventFilter.CREATED to "Created"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = filterLabels[selectedFilter] ?: "All Events",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true),
            label = { Text("Filter") }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            EventFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filterLabels[filter] ?: filter.name) },
                    onClick = { onFilterChange(filter) }
                )
            }
        }
    }
}