package com.cpen321.usermanagement.ui.screens.profile
import com.cpen321.usermanagement.ui.components.ExperienceLevel


import Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.viewmodels.ProfileUiState
import com.cpen321.usermanagement.ui.viewmodels.ProfileViewModel
import com.cpen321.usermanagement.ui.theme.LocalFontSizes
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.cpen321.usermanagement.common.Constants
import com.google.android.libraries.places.api.Places
import com.cpen321.usermanagement.data.repository.ProfileUpdateParams
import com.cpen321.usermanagement.ui.screens.CityAutocompleteField
import com.cpen321.usermanagement.ui.screens.ProfileCityAutocompleteCallbacks
import com.cpen321.usermanagement.ui.screens.ProfileCityAutocompleteData
import com.cpen321.usermanagement.ui.screens.ProfileCompletionHeader
import com.cpen321.usermanagement.ui.screens.ProfileExperienceDropdown
import com.cpen321.usermanagement.ui.screens.SaveButton
import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


private data class ProfileCompletionFormState(
    val name: String = "",
    val ageText: String = "",
    val cityQuery: String = "",
    val selectedCity: String? = null,
    val selectedCityPlaceId: String? = null,
    val experience: ExperienceLevel? = null,
    val bioText: String = "",
    val showErrors: Boolean = false      // NEW: controls when to display errors
) {
    // --- hard validity (used by canSave) ---
    private fun isNameValid() = name.isNotBlank()

    private fun isAgeValid(): Boolean {
        if (ageText.isBlank()) return true            // age optional
        val a = ageText.toIntOrNull() ?: return false
        return a in Constants.MIN_AGE..Constants.MAX_AGE                            // adjust if you want 13..100
    }

    private fun isCityValid() = !selectedCityPlaceId.isNullOrBlank()
    private fun isExpValid() = experience != null
    private fun isBioValid() = bioText.length <= Constants.MAX_BIO_LENGTH // backend max

    fun canSave(): Boolean =
        isNameValid() && isAgeValid() && isCityValid() && isExpValid() && isBioValid()

    val ageOrNull: Int?
        get() = ageText.toIntOrNull()
    // --- UI error messages (only when showErrors = true) ---
    val nameError: String?
        get() = if (!showErrors) null else if (!isNameValid()) "Required" else null

    val ageError: String?
        get() = if (!showErrors) null else if (!isAgeValid()) "Enter a valid age (${Constants.MIN_AGE}–${Constants.MAX_AGE})" else null

    val cityError: String?
        get() = if (!showErrors) null else if (!isCityValid()) "Pick a city from suggestions" else null

    val expError: String?
        get() = if (!showErrors) null else if (!isExpValid()) "Select experience level" else null

    // Ignore conditions for now
    val conditionsError: String? get() = null

    val bioError: String?
        get() = if (!showErrors) null else if (!isBioValid()) "Max ${Constants.MAX_BIO_LENGTH} characters" else null
}



private data class ProfileCompletionScreenData(
    val formState: ProfileCompletionFormState,
    val isSavingProfile: Boolean,
    val citySuggestions: List<String> = emptyList(),
    // ADD ↓ with a safe default
    val onNameChange: (String) -> Unit = {},
    // and your other callbacks:
    val onAgeChange: (String) -> Unit = {},
    val onCityQueryChange: (String) -> Unit = {},
    val onCitySelect: (String) -> Unit = {},
    val onExperienceSelect: (ExperienceLevel) -> Unit = {},
    val onBioChange: (String) -> Unit,
    val onSkipClick: () -> Unit,
    val onSaveClick: () -> Unit
)

private data class ProfileCompletionScreenContentData(
    val uiState: ProfileUiState,
    val formState: ProfileCompletionFormState,
    val snackBarHostState: SnackbarHostState,
    val citySuggestions: List<String> = emptyList(),
    // ADD ↓ with a safe default
    val onNameChange: (String) -> Unit = {},
    // (you’ll likely have these too)
    val onAgeChange: (String) -> Unit = {},
    val onCityQueryChange: (String) -> Unit = {},
    val onCitySelect: (String) -> Unit = {},
    val onExperienceSelect: (ExperienceLevel) -> Unit = {},
    val onBioChange: (String) -> Unit,
    val onSkipClick: () -> Unit,
    val onSaveClick: () -> Unit,
    val onErrorMessageShown: () -> Unit
)

@Composable
fun ProfileCompletionScreen(
    profileViewModel: ProfileViewModel,
    onProfileCompleted: () -> Unit,
    onProfileCompletedWithMessage: (String) -> Unit = { onProfileCompleted() }
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val suggestions by profileViewModel.citySuggestions.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var cityJob by remember { mutableStateOf<Job?>(null) }
    var formState by remember { mutableStateOf(ProfileCompletionFormState()) }

    InitializePlacesClient(profileViewModel)
    LoadProfileOnMount(profileViewModel, uiState)

    ProfileCompletionContent(
        data = createScreenContentData(
            CreateScreenContentArgs(
                uiState = uiState,
                formState = formState,
                snackBarHostState = snackBarHostState,
                suggestions = suggestions,
                scope = scope,
                cityJob = cityJob,
                profileViewModel = profileViewModel,
                successMessage = stringResource(R.string.successful_bio_update),
                onFormStateChange = { formState = it },
                onCityJobChange = { cityJob = it },
                onProfileCompleted = onProfileCompleted,
                onProfileCompletedWithMessage = onProfileCompletedWithMessage
            )
        )
    )
}

private data class CreateScreenContentArgs(
    val uiState: ProfileUiState,
    val formState: ProfileCompletionFormState,
    val snackBarHostState: SnackbarHostState,
    val suggestions: List<ProfileViewModel.CitySuggestion>,
    val scope: CoroutineScope,
    val cityJob: Job?,
    val profileViewModel: ProfileViewModel,
    val successMessage: String,
    val onFormStateChange: (ProfileCompletionFormState) -> Unit,
    val onCityJobChange: (Job?) -> Unit,
    val onProfileCompleted: () -> Unit,
    val onProfileCompletedWithMessage: (String) -> Unit
)

@Composable
private fun createScreenContentData(
    args: CreateScreenContentArgs
): ProfileCompletionScreenContentData = ProfileCompletionScreenContentData(
    uiState = args.uiState,
    formState = args.formState,
    snackBarHostState = args.snackBarHostState,
    citySuggestions = args.suggestions.map { it.label },
    onNameChange = { args.onFormStateChange(args.formState.copy(name = it)) },
    onAgeChange = { v -> 
        if (v.length <= 3 && v.all(Char::isDigit)) {
            args.onFormStateChange(args.formState.copy(ageText = v))
        }
    },
    onExperienceSelect = { args.onFormStateChange(args.formState.copy(experience = it)) },
    onBioChange = { args.onFormStateChange(args.formState.copy(bioText = it.take(Constants.MAX_BIO_LENGTH))) },
    onCityQueryChange = { query ->
        handleCityQueryChange(query, args.formState, args.cityJob, args.scope, args.profileViewModel, args.onFormStateChange, args.onCityJobChange)
    },
    onCitySelect = { label ->
        val match = args.suggestions.firstOrNull { it.label == label }
        args.onFormStateChange(args.formState.copy(selectedCity = label, selectedCityPlaceId = match?.placeId, cityQuery = ""))
    },
    onSkipClick = args.onProfileCompleted,
    onSaveClick = {
        handleSaveClick(args.formState, args.scope, args.snackBarHostState, args.profileViewModel, args.successMessage, 
        args.onFormStateChange, args.onProfileCompletedWithMessage)
    },
    onErrorMessageShown = args.profileViewModel::clearError
)

@Composable
private fun InitializePlacesClient(profileViewModel: ProfileViewModel) {
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }
    
    LaunchedEffect(Unit) {
        profileViewModel.attachPlacesClient(placesClient)
    }
}

@Composable
private fun LoadProfileOnMount(
    profileViewModel: ProfileViewModel,
    uiState: ProfileUiState
) {
    LaunchedEffect(Unit) {
        if (uiState.user == null) {
            profileViewModel.loadProfile()
        }
    }
}

private fun handleCityQueryChange(
    query: String,
    formState: ProfileCompletionFormState,
    cityJob: Job?,
    scope: CoroutineScope,
    profileViewModel: ProfileViewModel,
    onFormStateChange: (ProfileCompletionFormState) -> Unit,
    onCityJobChange: (Job?) -> Unit
) {
    onFormStateChange(
        formState.copy(
            cityQuery = query,
            selectedCity = null,
            selectedCityPlaceId = null
        )
    )
    
    cityJob?.cancel()
    val newJob = scope.launch {
        delay(Constants.DEFAULT_DEBOUNCE_MS.toLong())
        if (query.length >= 2) {
            profileViewModel.queryCities(query)
        } else {
            profileViewModel.queryCities("")
        }
    }
    onCityJobChange(newJob)
}

private fun handleSaveClick(
    formState: ProfileCompletionFormState,
    scope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    profileViewModel: ProfileViewModel,
    successMessage: String,
    onFormStateChange: (ProfileCompletionFormState) -> Unit,
    onProfileCompletedWithMessage: (String) -> Unit
) {
    if (!formState.canSave()) {
        onFormStateChange(formState.copy(showErrors = true))
        scope.launch { 
            snackBarHostState.showSnackbar("Please complete all required fields") 
        }
        return
    }

    val placeId = formState.selectedCityPlaceId
    if (placeId == null) {
        onFormStateChange(formState.copy(showErrors = true))
        scope.launch { 
            snackBarHostState.showSnackbar("Please pick a city from suggestions") 
        }
        return
    }

    scope.launch {
        try {
            val resolved = profileViewModel.resolveCity(placeId)
            
            profileViewModel.updateProfile(
                ProfileUpdateParams(
                    name = formState.name.trim(),
                    bio = formState.bioText.take(Constants.MAX_BIO_LENGTH).trim(),
                    age = formState.ageOrNull,
                    location = resolved.display,
                    latitude = resolved.lat,
                    longitude = resolved.lng,
                    skillLevel = formState.experience?.label
                )
            ) {
                onProfileCompletedWithMessage(successMessage)
            }
        } 
        catch (e: SocketTimeoutException) {
            snackBarHostState.showSnackbar("Couldn't verify city. Please try again.")
        } catch (e: UnknownHostException) {
            snackBarHostState.showSnackbar("Couldn't verify city. Please try again.")
        } catch (e: IOException) {
            snackBarHostState.showSnackbar("Couldn't verify city. Please try again.")
        }
    }
}

@Composable
private fun ProfileCompletionContent(
    data: ProfileCompletionScreenContentData,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = {
            MessageSnackbar(
                hostState = data.snackBarHostState,
                messageState = MessageSnackbarState(
                    successMessage = null,
                    errorMessage = data.uiState.errorMessage,
                    onSuccessMessageShown = { },
                    onErrorMessageShown = data.onErrorMessageShown
                )
            )
        }
    ) { paddingValues ->
        ProfileCompletionBody(
            paddingValues = paddingValues,
            data = ProfileCompletionScreenData(
                formState = data.formState,
                isSavingProfile = data.uiState.isSavingProfile,
                citySuggestions = data.citySuggestions,
                onNameChange = data.onNameChange,
                onAgeChange = data.onAgeChange,
                onCityQueryChange = data.onCityQueryChange,
                onCitySelect = data.onCitySelect,
                onExperienceSelect = data.onExperienceSelect,
//                onConditionToggle = data.onConditionToggle,
                onBioChange = data.onBioChange,
                onSkipClick = data.onSkipClick,
                onSaveClick = data.onSaveClick
            )
        )
    }
}

@Composable
private fun ProfileCompletionBody(
    paddingValues: PaddingValues,
    data: ProfileCompletionScreenData,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(spacing.extraLarge)
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        ProfileCompletionHeader()
        HeaderSpacer()
        ProfileForm(data)
    }
}

@Composable
private fun HeaderSpacer() {
    val spacing = LocalSpacing.current
    Spacer(modifier = Modifier.height(spacing.extraLarge2))
}

@Composable
private fun SectionSpacer() {
    val spacing = LocalSpacing.current
    Spacer(modifier = Modifier.height(spacing.extraLarge))
}

@Composable
private fun ProfileForm(data: ProfileCompletionScreenData) {
    
    NameInputField(
        name = data.formState.name,
        isEnabled = !data.isSavingProfile,
        error = data.formState.nameError,
        onNameChange = data.onNameChange
    )
    SectionSpacer()
    
    AgeInputField(
        ageText = data.formState.ageText,
        isEnabled = !data.isSavingProfile,
        error = data.formState.ageError,
        onAgeChange = data.onAgeChange
    )
    SectionSpacer()

    CityAutocompleteField(
        data = ProfileCityAutocompleteData(
            query = data.formState.cityQuery,
            selectedCity = data.formState.selectedCity,
            suggestions = data.citySuggestions,
            isEnabled = !data.isSavingProfile,
            error = data.formState.cityError
        ),
        callbacks = ProfileCityAutocompleteCallbacks(
            onQueryChange = data.onCityQueryChange,
            onSelect = data.onCitySelect,
            onClearSelection = { data.onCityQueryChange("") }
        )
    )
    SectionSpacer()

    ProfileExperienceDropdown(
        selected = data.formState.experience,
        isEnabled = !data.isSavingProfile,
        error = data.formState.expError,
        onSelect = data.onExperienceSelect
    )
    SectionSpacer()
    
    BioInputField(
        bioText = data.formState.bioText,
        isEnabled = !data.isSavingProfile,
        onBioChange = data.onBioChange
    )
    SectionSpacer()

    ActionButtons(
        isSavingProfile = data.isSavingProfile,
        isSaveEnabled = true,
        onSkipClick = data.onSkipClick,
        onSaveClick = data.onSaveClick
    )
}


 

@Composable
private fun BioInputField(
    bioText: String,
    isEnabled: Boolean,
    onBioChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    OutlinedTextField(
        value = bioText,
        onValueChange = onBioChange,
        label = { Text(stringResource(R.string.bio)) },
        placeholder = { Text(stringResource(R.string.bio_placeholder)) },
        modifier = modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 5,
        shape = RoundedCornerShape(spacing.medium),
        enabled = isEnabled
    )
}

@Composable
private fun NameInputField(
    name: String,
    isEnabled: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Name") },
        placeholder = { Text("Your name") },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        enabled = isEnabled
    )
}

@Composable
private fun AgeInputField(
    ageText: String,
    isEnabled: Boolean,
    error: String?,
    onAgeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = ageText,
        onValueChange = onAgeChange,
        label = { Text("Age") },
        placeholder = { Text("e.g., 22") },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        enabled = isEnabled,
    )
}


@Composable
private fun ActionButtons(
    isSavingProfile: Boolean,
    isSaveEnabled: Boolean,
    onSkipClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        SkipButton(
            isEnabled = !isSavingProfile,
            onClick = onSkipClick,
            modifier = Modifier.weight(1f)
        )

        SaveButton(
            isSaving = isSavingProfile,
            isEnabled = isSaveEnabled && !isSavingProfile,
            onClick = onSaveClick
        )
    }
}

@Composable
private fun SkipButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSizes = LocalFontSizes.current

    Box(modifier = modifier) {
        Button(
            type = "secondary",
            onClick = onClick,
            enabled = isEnabled
        ) {
            Text(
                text = stringResource(R.string.skip),
                fontSize = fontSizes.medium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Save button moved to components
