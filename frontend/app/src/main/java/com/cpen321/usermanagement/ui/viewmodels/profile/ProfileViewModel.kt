package com.cpen321.usermanagement.ui.viewmodels.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.data.repository.ProfileRepository
import com.cpen321.usermanagement.data.repository.ProfileUpdateParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
// --- Kotlin and coroutine basics ---
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// --- Google Maps / Places ---
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.suspendCancellableCoroutine


data class ProfileUiState(
    // Loading states
    val isLoadingProfile: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isLoadingPhoto: Boolean = false,

    // Data states
    val user: User? = null,

    // Message states
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // -------------------------
    // Google Places: City field
    // -------------------------

    data class CitySuggestion(val label: String, val placeId: String)

    private val _citySuggestions = MutableStateFlow<List<CitySuggestion>>(emptyList())
    val citySuggestions: StateFlow<List<CitySuggestion>> = _citySuggestions.asStateFlow()

    private var placesClient: PlacesClient? = null
    private var sessionToken: AutocompleteSessionToken? = null

    fun attachPlacesClient(client: PlacesClient) {
        placesClient = client
    }

    /** Call when the user types in the City box (debounce in UI). */
    fun queryCities(query: String) {
        val client = placesClient ?: return
        if (query.isBlank()) {
            _citySuggestions.value = emptyList()
            return
        }
        if (sessionToken == null) sessionToken = AutocompleteSessionToken.newInstance()

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            // SDK 3.x uses string place types; "locality" = city/town
            .setTypesFilter(listOf("locality"))
            // .setCountries(listOf("CA","US")) // optional: restrict to countries
            .setSessionToken(sessionToken)
            .build()

        client.findAutocompletePredictions(request)
            .addOnSuccessListener { resp ->
                _citySuggestions.value = resp.autocompletePredictions.map {
                    CitySuggestion(
                        label = it.getFullText(null).toString(),
                        placeId = it.placeId
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.w("ProfileViewModel", "Places predictions failed: ${e.message}")
                _citySuggestions.value = emptyList()
            }
    }

    data class ResolvedCity(
        val display: String,
        val placeId: String,
        val lat: Double,
        val lng: Double
    )

    /** Call on submit after a suggestion is selected (we have placeId). */
    suspend fun resolveCity(placeId: String): ResolvedCity {
        val client = placesClient ?: throw IllegalStateException("PlacesClient not attached")
        val fields = listOf(
            Place.Field.ID,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.DISPLAY_NAME,
            Place.Field.LOCATION
        )
        val request = FetchPlaceRequest.builder(placeId, fields)
            .setSessionToken(sessionToken)
            .build()

        return suspendCancellableCoroutine { cont ->
            client.fetchPlace(request)
                .addOnSuccessListener { r ->
                    val p = r.place
                    val label = p.formattedAddress ?: p.displayName
                    val loc = p.location
                    if (label == null) {
                        cont.resumeWithException(IllegalStateException("Could not resolve city label"))
                    } else {
                        cont.resume(
                            ResolvedCity(
                                display = label,
                                placeId = p.id ?: placeId,
                                lat = loc?.latitude ?: 0.0,
                                lng = loc?.longitude ?: 0.0
                            )
                        )
                        sessionToken = null // end autocomplete session after success
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    fun clearCitySuggestions() {
        _citySuggestions.value = emptyList()
    }


    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingProfile = true, errorMessage = null)

            val profileResult = profileRepository.getProfile()

            if (profileResult.isSuccess) {
                val user = profileResult.getOrNull()!!

                _uiState.value = _uiState.value.copy(
                    isLoadingProfile = false,
                    user = user
                )
            } else {
                val errorMessage = when {
                    profileResult.isFailure -> {
                        val error = profileResult.exceptionOrNull()
                        Log.e(TAG, "Failed to load profile", error)
                        error?.message ?: "Failed to load profile"
                    }

                    else -> {
                        Log.e(TAG, "Failed to load data")
                        "Failed to load data"
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingProfile = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun setLoadingPhoto(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoadingPhoto = isLoading)
    }

    fun uploadProfilePicture(pictureUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPhoto = true, errorMessage = null, successMessage = null)
            
            val result = profileRepository.uploadProfilePicture(pictureUri)
            if (result.isSuccess) {
                val updatedUser = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isLoadingPhoto = false, 
                    user = updatedUser, 
                    successMessage = "Profile picture updated successfully!"
                )
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to upload profile picture", error)
                val errorMessage = error?.message ?: "Failed to upload profile picture"
                _uiState.value = _uiState.value.copy(
                    isLoadingPhoto = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun updateProfile(params: ProfileUpdateParams, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isSavingProfile = true,
                    errorMessage = null,
                    successMessage = null
                )

            val result = profileRepository.updateProfile(params)
            if (result.isSuccess) {
                val updatedUser = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    user = updatedUser,
                    successMessage = "Profile updated successfully!"
                )
                onSuccess()
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to update profile", error)
                val errorMessage = error?.message ?: "Failed to update profile"
                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    errorMessage = errorMessage
                )
            }
        }
    }
    /**
     * Clear all cached profile data (used when account is deleted)
     */
    fun clearUserData() {
        _uiState.value = ProfileUiState()
    }
}
