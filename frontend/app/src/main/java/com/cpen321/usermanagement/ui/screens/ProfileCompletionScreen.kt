package com.cpen321.usermanagement.ui.screens
import com.cpen321.usermanagement.ui.components.ExperienceLevel


import Button
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
}

@Composable
private fun createScreenContentData(
    uiState: ProfileUiState,
    formState: ProfileCompletionFormState,
    snackBarHostState: SnackbarHostState,
    suggestions: List<com.cpen321.usermanagement.ui.viewmodels.ProfileViewModel.CitySuggestion>,
    scope: kotlinx.coroutines.CoroutineScope,
    cityJob: Job?,
    profileViewModel: ProfileViewModel,
    successMessage: String,
    onFormStateChange: (ProfileCompletionFormState) -> Unit,
    onCityJobChange: (Job?) -> Unit,
    onProfileCompleted: () -> Unit,
    onProfileCompletedWithMessage: (String) -> Unit
): ProfileCompletionScreenContentData = ProfileCompletionScreenContentData(
    uiState = uiState,
    formState = formState,
    snackBarHostState = snackBarHostState,
    citySuggestions = suggestions.map { it.label },
    onNameChange = { onFormStateChange(formState.copy(name = it)) },
    onAgeChange = { v -> 
        if (v.length <= 3 && v.all(Char::isDigit)) {
            onFormStateChange(formState.copy(ageText = v))
        }
    },
    onExperienceSelect = { onFormStateChange(formState.copy(experience = it)) },
    onBioChange = { onFormStateChange(formState.copy(bioText = it.take(Constants.MAX_BIO_LENGTH))) },
    onCityQueryChange = { query ->
        handleCityQueryChange(query, formState, cityJob, scope, profileViewModel, onFormStateChange, onCityJobChange)
    },
    onCitySelect = { label ->
        val match = suggestions.firstOrNull { it.label == label }
        onFormStateChange(formState.copy(selectedCity = label, selectedCityPlaceId = match?.placeId, cityQuery = ""))
    },
    onSkipClick = onProfileCompleted,
    onSaveClick = {
        handleSaveClick(formState, scope, snackBarHostState, profileViewModel, successMessage, onFormStateChange, onProfileCompletedWithMessage)
    },
    onErrorMessageShown = profileViewModel::clearError
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
    scope: kotlinx.coroutines.CoroutineScope,
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
    scope: kotlinx.coroutines.CoroutineScope,
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
            
            profileViewModel.updateProfileFull(
                name = formState.name.trim(),
                bio = formState.bioText.take(Constants.MAX_BIO_LENGTH).trim(),
                age = formState.ageOrNull,
                location = resolved.display,
                latitude = resolved.lat,
                longitude = resolved.lng,
                skillLevel = formState.experience?.label
            ) {
                onProfileCompletedWithMessage(successMessage)
            }
        } 
        catch (e: java.net.SocketTimeoutException) {
            snackBarHostState.showSnackbar("Couldn't verify city. Please try again.")
        } catch (e: java.net.UnknownHostException) {
            snackBarHostState.showSnackbar("Couldn't verify city. Please try again.")
        } catch (e: java.io.IOException) {
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
        state = CityAutocompleteState(
            query = data.formState.cityQuery,
            selectedCity = data.formState.selectedCity,
            suggestions = data.citySuggestions,
            isEnabled = !data.isSavingProfile,
            error = data.formState.cityError
        ),
        callbacks = CityAutocompleteCallbacks(
            onQueryChange = data.onCityQueryChange,
            onSelect = data.onCitySelect,
            onClearSelection = { data.onCityQueryChange("") }
        )
    )
    SectionSpacer()
    
    ExperienceDropdown(
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
private fun ProfileCompletionHeader(
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WelcomeTitle()

        Spacer(modifier = Modifier.height(spacing.medium))

        BioDescription()
    }
}

@Composable
private fun WelcomeTitle(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.complete_profile),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
private fun BioDescription(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.bio_description),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
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

private data class CityAutocompleteState(
    val query: String,
    val selectedCity: String?,
    val suggestions: List<String>,
    val isEnabled: Boolean,
    val error: String?
)

private data class CityAutocompleteCallbacks(
    val onQueryChange: (String) -> Unit,
    val onSelect: (String) -> Unit,
    val onClearSelection: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityAutocompleteField(
    state: CityAutocompleteState,
    callbacks: CityAutocompleteCallbacks,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    val textValue = if (state.selectedCity != null && state.query.isBlank()) {
        state.selectedCity
    } else {
        state.query
    }

    val shouldShowMenu = state.isEnabled && hasFocus && 
        state.query.isNotBlank() && state.suggestions.isNotEmpty()

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded && shouldShowMenu,
            onExpandedChange = { if (state.isEnabled) expanded = it && hasFocus },
            modifier = Modifier.fillMaxWidth()
        ) {
            CityTextField(
                uiState = CityTextFieldUiState(
                    textValue = textValue,
                    hasFocus = hasFocus,
                    shouldShowMenu = shouldShowMenu,
                    expanded = expanded
                ),
                state = state,
                callbacks = callbacks,
                textFieldCallbacks = CityTextFieldCallbacks(
                    onExpandedChange = { expanded = it },
                    onFocusChanged = { focused ->
                        hasFocus = focused
                        expanded = focused && state.query.isNotBlank() && state.suggestions.isNotEmpty()
                    }
                ),
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded && shouldShowMenu,
                onDismissRequest = { expanded = false }
            ) {
                state.suggestions.take(Constants.DEFAULT_PAGE_SIZE / 2).forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            callbacks.onSelect(item)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (state.error != null) {
            CityErrorText(state.error)
        }
    }
}

@Composable
private fun CityErrorText(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private data class CityTextFieldUiState(
    val textValue: String,
    val hasFocus: Boolean,
    val shouldShowMenu: Boolean,
    val expanded: Boolean
)

private data class CityTextFieldCallbacks(
    val onExpandedChange: (Boolean) -> Unit,
    val onFocusChanged: (Boolean) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityTextField(
    uiState: CityTextFieldUiState,
    state: CityAutocompleteState,
    callbacks: CityAutocompleteCallbacks,
    textFieldCallbacks: CityTextFieldCallbacks,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = uiState.textValue,
        onValueChange = { newText ->
            if (state.selectedCity != null) callbacks.onClearSelection()
            callbacks.onQueryChange(newText)
            textFieldCallbacks.onExpandedChange(true)
        },
        label = { Text("City") },
        placeholder = { Text("Start typing…") },
        singleLine = true,
        isError = state.error != null,
        enabled = state.isEnabled,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next, 
            autoCorrectEnabled = false
        ),
        trailingIcon = {
            CityFieldTrailingIcon(
                selectedCity = state.selectedCity,
                shouldShowMenu = uiState.shouldShowMenu,
                expanded = uiState.expanded,
                hasFocus = uiState.hasFocus,
                query = state.query,
                onClear = callbacks.onClearSelection,
                onExpandedChange = textFieldCallbacks.onExpandedChange
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { textFieldCallbacks.onFocusChanged(it.isFocused) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityFieldTrailingIcon(
    selectedCity: String?,
    shouldShowMenu: Boolean,
    expanded: Boolean,
    hasFocus: Boolean,
    query: String,
    onClear: () -> Unit,
    onExpandedChange: (Boolean) -> Unit
) {
    when {
        selectedCity != null -> IconButton(onClick = {
            onClear()
            onExpandedChange(hasFocus && query.isNotBlank())
        }) {
            Icon(Icons.Default.Close, contentDescription = "Clear city")
        }
        shouldShowMenu -> ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperienceDropdown(
    selected: ExperienceLevel?, // your enum in domain or UI layer
    isEnabled: Boolean,
    error: String?,
    onSelect: (ExperienceLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selected?.label ?: "Select experience"

    Column(modifier) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (isEnabled) expanded = !expanded }) {
            OutlinedTextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Experience Level") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                isError = error != null,
                enabled = isEnabled,
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ExperienceLevel.values().forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level.label) },
                        onClick = { onSelect(level); expanded = false }
                    )
                }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
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
            onClick = onSaveClick,
            modifier = Modifier.weight(1f)
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

@Composable
private fun SaveButton(
    isSaving: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Box(modifier = modifier) {
        Button(
            onClick = onClick,
            enabled = isEnabled
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(spacing.medium),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.save),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
