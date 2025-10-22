package com.cpen321.usermanagement.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LocationResult(
    val placeId: String,
    val address: String,
    val name: String? = null,
    val coordinates: LatLng? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    onLocationSelected: (LocationResult) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String,
) {
    var showLocationDialog by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = value,
        onValueChange = { },
        label = { RequiredTextLabel(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showLocationDialog = true },
        readOnly = true,
        trailingIcon = {
            TextButton(onClick = { showLocationDialog = true }) {
                Text("Search")
            }
        }
    )
    
    // Location Search Dialog
    if (showLocationDialog) {
        LocationSearchDialog(
            onLocationSelected = { locationResult ->
                onValueChange(locationResult.address)
                onLocationSelected(locationResult)
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSearchDialog(
    onLocationSelected: (LocationResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val placesClient = remember { Places.createClient(context) }
    
    // Search for predictions when query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isLoading = true
            try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(searchQuery)
                    .setSessionToken(AutocompleteSessionToken.newInstance())
                    .build()
                
                val response = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine { continuation ->
                        placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener { result ->
                                continuation.resume(result)
                            }
                            .addOnFailureListener { exception ->
                                continuation.resumeWithException(exception)
                            }
                    }
                }
                
                predictions = response.autocompletePredictions
                Log.d("LocationSearchDialog", "Predictions: $predictions")
            } catch (e: Exception) {
                Log.e("LocationSearchDialog", "Error finding predictions: ${e.message}")
                predictions = emptyList()
            } finally {
                isLoading = false
            }
        } else {
            predictions = emptyList()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Search Location")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Enter location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                )
            
                if (isLoading) {
                    Text(
                        text = "Searching...",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (predictions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(predictions.take(10)) { prediction ->
                            PredictionItem(
                                prediction = prediction,
                                onClick = {
                                    // Fetch place details
                                    fetchPlaceDetails(placesClient, prediction.placeId) { locationResult ->
                                        onLocationSelected(locationResult)
                                    }
                                }
                            )
                            
                            if (prediction != predictions.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                } else if (searchQuery.length >= 2) {
                    Text(
                        text = "No locations found",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PredictionItem(
    prediction: AutocompletePrediction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location",
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = prediction.getPrimaryText(null).toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (prediction.getSecondaryText(null) != null) {
                Text(
                    text = prediction.getSecondaryText(null).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun fetchPlaceDetails(
    placesClient: PlacesClient,
    placeId: String,
    onResult: (LocationResult) -> Unit
) {
    val placeFields = listOf(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.ADDRESS,
        Place.Field.LAT_LNG
    )
    
    val request = FetchPlaceRequest.newInstance(placeId, placeFields)
    
    placesClient.fetchPlace(request)
        .addOnSuccessListener { response ->
            val place = response.place
            val locationResult = LocationResult(
                placeId = place.id ?: "",
                address = place.address ?: "",
                name = place.name,
                coordinates = place.latLng
            )
            onResult(locationResult)
        }
        .addOnFailureListener { exception ->
            Log.e("LocationAutocomplete", "Error fetching place details: ${exception.message}")
        }
}