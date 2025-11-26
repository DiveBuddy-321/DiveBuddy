package com.cpen321.usermanagement.ui.components.events

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

enum class EventSort {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSortDropdown(
    selectedSort: EventSort,
    onSortChange: (EventSort) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val sortLabels = mapOf(
        EventSort.NAME_ASC to "Name (A-Z)",
        EventSort.NAME_DESC to "Name (Z-A)",
        EventSort.DATE_ASC to "Date (Earliest)",
        EventSort.DATE_DESC to "Date (Latest)"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = sortLabels[selectedSort] ?: "Name (A-Z)",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodySmall,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryEditable, enabled),
            label = { Text("Sort") }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            EventSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            sortLabels[sort] ?: sort.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    onClick = { onSortChange(sort) },
                    enabled = enabled
                )
            }
        }
    }
}

