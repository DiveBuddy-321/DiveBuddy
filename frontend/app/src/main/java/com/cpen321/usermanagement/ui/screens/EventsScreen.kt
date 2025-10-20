package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.ui.theme.LocalFontSizes
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import java.text.DateFormat.getDateInstance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier
) {
    var showCreateEventForm by remember { mutableStateOf(false) }

    if (showCreateEventForm) {
        Text(
            text = "Create Event",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        EventsContent(
            modifier = modifier,
            onCreateEventClick = {
                showCreateEventForm = true
            },
        )
    }
}

@Composable
private fun EventsContent(
    modifier: Modifier = Modifier,
    onCreateEventClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    
    // Dummy data for events
    val dummyEvents = listOf(
        Event(
            _id = "1",
            title = "Basketball Tournament",
            description = "Join us for an exciting basketball tournament! All skill levels welcome.",
            date = Date(System.currentTimeMillis() + 86400000), // Tomorrow
            capacity = 20,
            skillLevel = "Intermediate",
            location = "UBC Thunderbird Sports Centre",
            latitude = 49.2606,
            longitude = -123.2460,
            createdBy = "user1",
            attendees = listOf("user2", "user3", "user4"),
            photo = null,
            createdAt = Date(),
            updatedAt = Date()
        ),
        Event(
            _id = "2",
            title = "Soccer Practice",
            description = "Weekly soccer practice session. Focus on technique and teamwork.",
            date = Date(System.currentTimeMillis() + 172800000), // Day after tomorrow
            capacity = 15,
            skillLevel = "Beginner",
            location = "UBC Soccer Field",
            latitude = 49.2611,
            longitude = -123.2450,
            createdBy = "user2",
            attendees = listOf("user1", "user3"),
            photo = null,
            createdAt = Date(),
            updatedAt = Date()
        ),
        Event(
            _id = "3",
            title = "Tennis Doubles",
            description = "Friendly tennis doubles match. Bring your racket and competitive spirit!",
            date = Date(System.currentTimeMillis() + 259200000), // 3 days from now
            capacity = 4,
            skillLevel = "Advanced",
            location = "UBC Tennis Courts",
            latitude = 49.2620,
            longitude = -123.2440,
            createdBy = "user3",
            attendees = listOf("user1", "user2"),
            photo = null,
            createdAt = Date(),
            updatedAt = Date()
        )
    )

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

        Text(
            text = "Upcoming Events",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = spacing.small)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            modifier = Modifier.padding(horizontal = spacing.large)
        ) {
            items(dummyEvents) { event ->
                EventCard(event = event)
            }
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.US);
    
    Card(
        modifier = modifier,
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
            
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
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
            }
            
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