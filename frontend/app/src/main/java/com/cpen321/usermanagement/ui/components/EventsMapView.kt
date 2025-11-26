package com.cpen321.usermanagement.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.cpen321.usermanagement.data.remote.dto.Event
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.tasks.await

@Composable
fun EventsMapView(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    
    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    
    // Check and request location permission
    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        hasLocationPermission = fineLocationGranted || coarseLocationGranted
        
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // Get user location
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val fusedLocationClient: FusedLocationProviderClient = 
                    LocationServices.getFusedLocationProviderClient(context)
                
                val locationResult = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
                
                locationResult?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                    Log.d("EventsMapView", "User location: ${it.latitude}, ${it.longitude}")
                }
            } catch (e: Exception) {
                Log.e("EventsMapView", "Error getting location: ${e.message}")
            }

        }
    }
    
    // Filter events with coordinates
    val eventsWithCoordinates = events.filter { 
        it.latitude != null && it.longitude != null 
    }
    
    // Show loading state while waiting for location
    if (hasLocationPermission && userLocation == null) {
        LoadingMessage(
            message = "Getting your location...",
            modifier = modifier
        )
        return
    }
    
    // Determine center location (always prefer user location)
    val centerLocation = userLocation ?: calculateEventsCenter(eventsWithCoordinates)
    
    // Calculate zoom level to include user location and all events
    val zoomLevel = calculateZoomLevel(userLocation, eventsWithCoordinates)
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerLocation, zoomLevel)
    }
    
    // Update camera when user location becomes available
    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            val finalZoom = calculateZoomLevel(userLocation, eventsWithCoordinates)
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(userLocation!!, finalZoom)
            )
            cameraPositionState.animate(cameraUpdate)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            ),
            onMapLoaded = {
                Log.d("EventsMapView", "Map loaded with ${eventsWithCoordinates.size} events")
            }
        ) {
            // 50km radius circle around user location
            if (userLocation != null) {
                Circle(
                    center = userLocation!!,
                    radius = 50000.0, // 50km in meters
                    fillColor = androidx.compose.ui.graphics.Color(0x1E4285F4),
                    strokeColor = androidx.compose.ui.graphics.Color(0x964285F4),
                    strokeWidth = 3f
                )
            }
            
            // Event markers
            eventsWithCoordinates.forEach { event ->
                Marker(
                    state = MarkerState(
                        position = LatLng(event.latitude!!, event.longitude!!)
                    ),
                    title = event.title,
                    snippet = event.location,
                    onClick = {
                        onEventClick(event)
                        true
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun calculateEventsCenter(events: List<Event>): LatLng {
    return if (events.isNotEmpty()) {
        val centerLat = events.mapNotNull { it.latitude }.average()
        val centerLng = events.mapNotNull { it.longitude }.average()
        LatLng(centerLat, centerLng)
    } else {
        LatLng(49.2827, -123.1207) // Vancouver, BC default
    }
}

private fun calculateZoomLevel(
    userLocation: LatLng?,
    events: List<Event>
): Float {
    if (userLocation == null) {
        return when {
            events.size == 1 -> 15f
            events.size <= 5 -> 12f
            else -> 10f
        }
    }
    
    if (events.isEmpty()) {
        return 10f // 50km radius
    }
    
    // Calculate distance to farthest event
    val maxDistance = events.mapNotNull { event ->
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            userLocation.latitude,
            userLocation.longitude,
            event.latitude!!,
            event.longitude!!,
            results
        )
        results[0]
    }.maxOrNull()?.toDouble() ?: 50000.0
    
    // Determine zoom based on distance
    return when {
        maxDistance < 10000 -> 12f  // < 10km
        maxDistance < 25000 -> 11f  // < 25km
        maxDistance < 50000 -> 10f  // < 50km
        maxDistance < 100000 -> 9f  // < 100km
        else -> 8f                  // > 100km
    }
}

