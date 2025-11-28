package com.cpen321.usermanagement.ui.components.events

import android.util.Log
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LocationResult(
    val placeId: String,
    val address: String,
    val name: String? = null,
    val coordinates: LatLng? = null
)

data class LocationTextFieldState(
    val searchQuery: String,
    val hasSelectedLocation: Boolean,
    val isLoading: Boolean
)

data class LocationTextFieldCallbacks(
    val onSearchQueryChange: (String) -> Unit,
    val onFocusChanged: (Boolean) -> Unit,
    val onClear: () -> Unit
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
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }
    var searchQuery by remember { mutableStateOf(value) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    var hasSelectedLocation by remember { mutableStateOf(value.isNotEmpty()) }
    LaunchedEffect(value) {
        if (value != searchQuery) {
            searchQuery = value
            hasSelectedLocation = value.isNotEmpty()
        }
    }
    PredictionsFetcher(
        query = searchQuery,
        placesClient = placesClient,
        onLoadingChange = { isLoading = it },
        onPredictionsChange = { predictions = it },
        enabled = hasFocus && !hasSelectedLocation
    )
    val showDropdown = hasFocus && searchQuery.length >= 2 && 
                       predictions.isNotEmpty() && !hasSelectedLocation 
    Column(modifier = modifier.fillMaxWidth()) {
        LocationTextField(
            state = LocationTextFieldState(
                searchQuery = searchQuery,
                hasSelectedLocation = hasSelectedLocation,
                isLoading = isLoading
            ),
            callbacks = LocationTextFieldCallbacks(
                onSearchQueryChange = { newValue ->
                    searchQuery = newValue
                    onValueChange(newValue)
                    hasSelectedLocation = false
                },
                onFocusChanged = { hasFocus = it },
                onClear = {
                    searchQuery = ""
                    onValueChange("")
                    hasSelectedLocation = false
                }
            ),
            label = label,
            placeholder = placeholder
        )
        if (showDropdown) {
            LocationDropdown(
                predictions = predictions,
                placesClient = placesClient,
                onLocationSelected = { locationResult ->
                    searchQuery = locationResult.address
                    onValueChange(locationResult.address)
                    hasSelectedLocation = true
                    onLocationSelected(locationResult)
                }
            )
        }
    }
}

@Composable
private fun LocationTextField(
    state: LocationTextFieldState,
    callbacks: LocationTextFieldCallbacks,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = state.searchQuery,
        onValueChange = callbacks.onSearchQueryChange,
        label = { RequiredTextLabel(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { callbacks.onFocusChanged(it.isFocused) },
        singleLine = true,
        trailingIcon = {
            LocationTextFieldIcon(
                hasSelectedLocation = state.hasSelectedLocation,
                searchQuery = state.searchQuery,
                isLoading = state.isLoading,
                onClear = callbacks.onClear
            )
        }
    )
}

@Composable
private fun LocationTextFieldIcon(
    hasSelectedLocation: Boolean,
    searchQuery: String,
    isLoading: Boolean,
    onClear: () -> Unit
) {
    when {
        hasSelectedLocation && searchQuery.isNotEmpty() -> {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear")
            }
        }
        isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier.padding(12.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun LocationDropdown(
    predictions: List<AutocompletePrediction>,
    placesClient: PlacesClient,
    onLocationSelected: (LocationResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .padding(top = 4.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
    ) {
        items(predictions.take(10)) { prediction ->
            PredictionDropdownItem(
                prediction = prediction,
                onClick = {
                    fetchPlaceDetails(placesClient, prediction.placeId, onLocationSelected)
                }
            )
            if (prediction != predictions.take(10).last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun PredictionsFetcher(
    query: String,
    placesClient: PlacesClient,
    onLoadingChange: (Boolean) -> Unit,
    onPredictionsChange: (List<AutocompletePrediction>) -> Unit,
    enabled: Boolean
) {
    LaunchedEffect(query, enabled) {
        if (enabled && query.length >= 2) {
            onLoadingChange(true)
            try {
                val results = withContext(Dispatchers.IO) {
                    findAutocompletePredictions(placesClient, query)
                }
                Log.d("LocationAutocomplete", "Predictions: $results")
                onPredictionsChange(results)
            } catch (e: SocketTimeoutException) {
                Log.e("LocationAutocomplete", "Network timeout while finding predictions", e)
                onPredictionsChange(emptyList())
            } catch (e: UnknownHostException) {
                Log.e("LocationAutocomplete", "Network connection failed while finding predictions", e)
                onPredictionsChange(emptyList())
            } catch (e: IOException) {
                Log.e("LocationAutocomplete", "IO error while finding predictions", e)
                onPredictionsChange(emptyList())
            } finally {
                onLoadingChange(false)
            }
        } else {
            onPredictionsChange(emptyList())
        }
    }
}

@Composable
private fun PredictionDropdownItem(
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

private suspend fun findAutocompletePredictions(
    placesClient: PlacesClient,
    query: String
): List<AutocompletePrediction> {
    val request = FindAutocompletePredictionsRequest.builder()
        .setQuery(query)
        .setSessionToken(AutocompleteSessionToken.newInstance())
        .build()

    val response = suspendCancellableCoroutine<FindAutocompletePredictionsResponse> { continuation ->
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { result ->
                continuation.resume(result)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    return response.autocompletePredictions
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