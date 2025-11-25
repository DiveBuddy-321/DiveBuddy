package com.cpen321.usermanagement.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cpen321.usermanagement.data.remote.dto.Event
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun EventsMapView(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter events that have coordinates
    val eventsWithCoordinates = events.filter { 
        it.latitude != null && it.longitude != null 
    }
    
    if (eventsWithCoordinates.isEmpty()) {
        // Show message if no events have coordinates
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No events with location data available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    // Calculate center point for camera (average of all event locations)
    val centerLat = eventsWithCoordinates.mapNotNull { it.latitude }.average()
    val centerLng = eventsWithCoordinates.mapNotNull { it.longitude }.average()
    val centerLocation = LatLng(centerLat, centerLng)
    
    // Determine zoom level based on number of events
    val zoomLevel = when {
        eventsWithCoordinates.size == 1 -> 15f
        eventsWithCoordinates.size <= 5 -> 12f
        else -> 10f
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerLocation, zoomLevel)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapLoaded = {
                Log.d("EventsMapView", "Map loaded with ${eventsWithCoordinates.size} events")
            }
        ) {
            // Add markers for each event
            eventsWithCoordinates.forEach { event ->
                val location = LatLng(
                    event.latitude!!,
                    event.longitude!!
                )
                
                Marker(
                    state = MarkerState(position = location),
                    title = event.title,
                    snippet = event.location,
                    onClick = {
                        onEventClick(event)
                        true // Return true to consume the click event
                    }
                )
            }
        }
    }
}

