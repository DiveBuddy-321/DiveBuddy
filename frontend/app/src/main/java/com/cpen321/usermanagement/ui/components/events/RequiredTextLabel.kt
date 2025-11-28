package com.cpen321.usermanagement.ui.components.events

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight

@Composable
fun RequiredTextLabel(
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Text(
            text = "*",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}