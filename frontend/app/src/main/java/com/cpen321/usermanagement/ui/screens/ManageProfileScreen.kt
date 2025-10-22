package com.cpen321.usermanagement.ui.screens
import com.cpen321.usermanagement.ui.components.ExperienceLevel

import Button
import Icon
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.ui.components.ImagePicker
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.viewmodels.ProfileUiState
import com.cpen321.usermanagement.ui.viewmodels.ProfileViewModel
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

private data class ProfileFormState(
    val name: String = "",
    val email: String = "",
    val ageText: String = "",
    val cityQuery: String = "",
    val selectedCity: String? = null,
    val selectedCityPlaceId: String? = null,
    val experience: ExperienceLevel? = null,
    val bioText: String = "",
    val showErrors: Boolean = false,
    val originalCityDisplay: String? = null
) {
    private fun isEditingCity(): Boolean =
        cityQuery.isNotBlank() || (selectedCity ?: "") != (originalCityDisplay ?: "")

    private fun isCityValid(): Boolean =
        if (!isEditingCity()) true else !selectedCityPlaceId.isNullOrBlank()

    private fun isNameValid() = name.isNotBlank()
    private fun isAgeValid(): Boolean {
        if (ageText.isBlank()) return true
        val a = ageText.toIntOrNull() ?: return false
        return a in 13..120
    }
    private fun isExpValid() = true
    private fun isBioValid() = bioText.length <= 500

    fun canSave(): Boolean =
        isNameValid() && isAgeValid() && isCityValid() && isExpValid() && isBioValid()

    val nameError: String? get() = if (!showErrors) null else if (!isNameValid()) "Required" else null
    val ageError: String? get() = if (!showErrors) null else if (!isAgeValid()) "Enter a valid age (13–120)" else null
    val cityError: String?
        get() = if (!showErrors) null
        else if (isEditingCity() && selectedCityPlaceId.isNullOrBlank()) "Pick a city from suggestions" else null
    val expError: String? get() = null
    val bioError: String? get() = if (!showErrors) null else if (!isBioValid()) "Max 500 characters" else null

    val ageOrNull: Int? get() = ageText.toIntOrNull()

    // Keep Save visible; the button enable can ignore this if you prefer
    fun hasChanges(): Boolean = true
}


private data class ManageProfileScreenActions(
    val onBackClick: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onBioChange: (String) -> Unit,
    val onEditPictureClick: () -> Unit,
    val onSaveClick: () -> Unit,
    val onImagePickerDismiss: () -> Unit,
    val onImageSelected: (Uri) -> Unit,
    val onLoadingPhotoChange: (Boolean) -> Unit,
    val onSuccessMessageShown: () -> Unit,
    val onErrorMessageShown: () -> Unit
)

private data class ProfileFormData(
    val user: User,
    val formState: ProfileFormState,
    val isLoadingPhoto: Boolean,
    val isSavingProfile: Boolean,
    val onNameChange: (String) -> Unit,
    val onBioChange: (String) -> Unit,
    val onEditPictureClick: () -> Unit,
    val onSaveClick: () -> Unit,
    val onLoadingPhotoChange: (Boolean) -> Unit,
    val citySuggestions: List<String>,
    val onAgeChange: (String) -> Unit,
    val onCityQueryChange: (String) -> Unit,
    val onCitySelect: (String) -> Unit,
    val onExperienceSelect: (ExperienceLevel) -> Unit
)


private data class ProfileBodyData(
    val uiState: ProfileUiState,
    val formState: ProfileFormState,
    val onNameChange: (String) -> Unit,
    val onBioChange: (String) -> Unit,
    val onEditPictureClick: () -> Unit,
    val onSaveClick: () -> Unit,
    val onLoadingPhotoChange: (Boolean) -> Unit,
    val citySuggestions: List<String>,
    val onAgeChange: (String) -> Unit,
    val onCityQueryChange: (String) -> Unit,
    val onCitySelect: (String) -> Unit,
    val onExperienceSelect: (ExperienceLevel) -> Unit
)
private data class ProfileFieldsData(
    val name: String,
    val email: String,
    val bio: String,
    val onNameChange: (String) -> Unit,
    val onBioChange: (String) -> Unit
)

@Composable
fun ManageProfileScreen(
    profileViewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    // NEW: suggestions & debounce like Complete Profile
    val suggestions by profileViewModel.citySuggestions.collectAsState()
    val scope = rememberCoroutineScope()
    var cityJob by remember { mutableStateOf<Job?>(null) }

    // Places client
    val context = LocalContext.current
    val placesClient = remember { com.google.android.libraries.places.api.Places.createClient(context) }


    var showImagePickerDialog by remember { mutableStateOf(false) }

    // Form state
    var formState by remember {
        mutableStateOf(ProfileFormState())
    }

    LaunchedEffect(Unit) {
        profileViewModel.clearSuccessMessage()
        profileViewModel.clearError()
        profileViewModel.attachPlacesClient(placesClient) // NEW
        if (uiState.user == null) {
            profileViewModel.loadProfile()
        }
    }

    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            formState = ProfileFormState(
                name = user.name,
                email = user.email,
                bioText = user.bio ?: "",
                ageText = user.age?.toString() ?: "",
                // show existing city as the label, but no placeId (since we didn't resolve it)
                selectedCity = user.location ?: "",
                selectedCityPlaceId = null,
                cityQuery = "", // keep empty so the field shows `selectedCity`
                originalCityDisplay = user.location, // NEW: remember original
                experience = when ((user.skillLevel ?: "").lowercase()) {
                    "beginner" -> ExperienceLevel.BEGINNER
                    "intermediate" -> ExperienceLevel.INTERMEDIATE
                    "expert" -> ExperienceLevel.EXPERT
                    else -> null
                }
            )
        }
    }

    // NEW handlers (mirror Complete Profile)
    val onAgeChange: (String) -> Unit = { v ->
        if (v.length <= 3 && v.all(Char::isDigit)) formState = formState.copy(ageText = v)
    }
    val onCityQueryChange: (String) -> Unit = { q ->
        formState = formState.copy(
            cityQuery = q,
            selectedCity = null,
            selectedCityPlaceId = null
        )
        cityJob?.cancel()
        cityJob = scope.launch {
            delay(200) // debounce
            if (q.length >= 2) {
                profileViewModel.queryCities(q)
            } else {
                // Prefer clearing suggestions instead of querying ""
                profileViewModel.queryCities("#~clear~#") // <-- add this in your VM (or see Note below)
            }
        }
    }

    val onCitySelect: (String) -> Unit = { label ->
        val match = profileViewModel.citySuggestions.value.firstOrNull { it.label == label }
        formState = formState.copy(
            selectedCity = label,
            selectedCityPlaceId = match?.placeId,
            cityQuery = "" // collapse
        )
    }
    val onExperienceSelect: (ExperienceLevel) -> Unit = { lvl ->
        formState = formState.copy(experience = lvl)
    }


    val actions = ManageProfileScreenActions(
        onBackClick = onBackClick,
        onNameChange = { formState = formState.copy(name = it) },
        onBioChange = { formState = formState.copy(bioText = it) },
        onEditPictureClick = { showImagePickerDialog = true },
        onSaveClick = {
            if (!formState.canSave()) {
                formState = formState.copy(showErrors = true)
                return@ManageProfileScreenActions
            }
            scope.launch {
                try {
                    val isEditingCity = formState.cityQuery.isNotBlank() ||
                            (formState.selectedCity ?: "") != (formState.originalCityDisplay ?: "")

                    val pid = formState.selectedCityPlaceId
                    val resolved = if (isEditingCity && pid != null) profileViewModel.resolveCity(pid) else null

                    val safeName = formState.name.trim()
                    val safeBio = formState.bioText.trim()
                    val safeAge = formState.ageOrNull
                    val skill = formState.experience?.label

                    profileViewModel.updateProfileFull(
                        name = safeName,
                        bio = safeBio.ifEmpty { null },
                        age = safeAge,
                        // Only send city if user actually changed it *and* it was resolved
                        location = resolved?.display,
                        latitude = resolved?.lat,
                        longitude = resolved?.lng,
                        skillLevel = skill
                    )
                } catch (_: Exception) { /* snackbar if you want */ }
            }
        },
        onImagePickerDismiss = { showImagePickerDialog = false },
        onImageSelected = { uri ->
            showImagePickerDialog = false
            profileViewModel.uploadProfilePicture(uri)
        },
        onLoadingPhotoChange = profileViewModel::setLoadingPhoto,
        onSuccessMessageShown = profileViewModel::clearSuccessMessage,
        onErrorMessageShown = profileViewModel::clearError
    )


    ManageProfileContent(
        uiState = uiState,
        formState = formState,
        snackBarHostState = snackBarHostState,
        showImagePickerDialog = showImagePickerDialog,
        actions = actions,
        citySuggestions = suggestions.map { it.label },
        onAgeChange = onAgeChange,
        onCityQueryChange = onCityQueryChange,
        onCitySelect = onCitySelect,
        onExperienceSelect = onExperienceSelect
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageProfileContent(
    uiState: ProfileUiState,
    formState: ProfileFormState,
    snackBarHostState: SnackbarHostState,
    showImagePickerDialog: Boolean,
    actions: ManageProfileScreenActions,
    citySuggestions: List<String>,
    onAgeChange: (String) -> Unit,
    onCityQueryChange: (String) -> Unit,
    onCitySelect: (String) -> Unit,
    onExperienceSelect: (ExperienceLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ProfileTopBar(onBackClick = actions.onBackClick)
        },
        snackbarHost = {
            MessageSnackbar(
                hostState = snackBarHostState,
                messageState = MessageSnackbarState(
                    successMessage = uiState.successMessage,
                    errorMessage = uiState.errorMessage,
                    onSuccessMessageShown = actions.onSuccessMessageShown,
                    onErrorMessageShown = actions.onErrorMessageShown
                )
            )
        }
    ) { paddingValues ->
        ProfileBody(
            paddingValues = paddingValues,
            data = ProfileBodyData(
                uiState = uiState,
                formState = formState,
                onNameChange = actions.onNameChange,
                onBioChange = actions.onBioChange,
                onEditPictureClick = actions.onEditPictureClick,
                onSaveClick = actions.onSaveClick,
                onLoadingPhotoChange = actions.onLoadingPhotoChange,
                citySuggestions = citySuggestions,
                onAgeChange = onAgeChange,
                onCityQueryChange = onCityQueryChange,
                onCitySelect = onCitySelect,
                onExperienceSelect = onExperienceSelect
            )
        )

    }

    if (showImagePickerDialog) {
        ImagePicker(
            onDismiss = actions.onImagePickerDismiss,
            onImageSelected = actions.onImageSelected
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.manage_profile),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(name = R.drawable.ic_arrow_back)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun ProfileBody(
    paddingValues: PaddingValues,
    data: ProfileBodyData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when {
            data.uiState.isLoadingProfile -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            data.uiState.user != null -> {
                ProfileForm(
                    data = ProfileFormData(
                        user = data.uiState.user,
                        formState = data.formState,
                        isLoadingPhoto = data.uiState.isLoadingPhoto,
                        isSavingProfile = data.uiState.isSavingProfile,
                        onNameChange = data.onNameChange,
                        onBioChange = data.onBioChange,
                        onEditPictureClick = data.onEditPictureClick,
                        onSaveClick = data.onSaveClick,
                        onLoadingPhotoChange = data.onLoadingPhotoChange,
                        citySuggestions = data.citySuggestions,
                        onAgeChange = data.onAgeChange,
                        onCityQueryChange = data.onCityQueryChange,
                        onCitySelect = data.onCitySelect,
                        onExperienceSelect = data.onExperienceSelect
                    )
                )
            }
        }
    }
}

@Composable
private fun ProfileForm(
    data: ProfileFormData,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        ProfilePictureCard(
            profilePicture = data.user.profilePicture,
            isLoadingPhoto = data.isLoadingPhoto,
            onEditClick = data.onEditPictureClick,
            onLoadingChange = data.onLoadingPhotoChange
        )

        OutlinedTextField(
            value = data.formState.name,
            onValueChange = data.onNameChange,
            label = { Text(stringResource(R.string.name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // NEW: Age (matches Complete Profile)
        OutlinedTextField(
            value = data.formState.ageText,
            onValueChange = data.onAgeChange,
            label = { Text("Age") },
            placeholder = { Text("e.g., 22") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = data.formState.ageError != null,
            supportingText = { data.formState.ageError?.let { Text(it) } }
        )

// NEW: City autocomplete (matches Complete Profile)
        CityAutocompleteField(
            query = data.formState.cityQuery,
            selectedCity = data.formState.selectedCity,
            suggestions = data.citySuggestions,
            isEnabled = !data.isSavingProfile,
            error = data.formState.cityError,
            onQueryChange = data.onCityQueryChange,
            onSelect = data.onCitySelect,
            onClearSelection = {
                data.onCityQueryChange("") // clear field & suggestions
            }
        )

// NEW: Experience dropdown (matches Complete Profile)
        ExperienceDropdown(
            selected = data.formState.experience,
            isEnabled = !data.isSavingProfile,
            error = data.formState.expError,
            onSelect = data.onExperienceSelect
        )

        OutlinedTextField(
            value = data.formState.bioText,
            onValueChange = data.onBioChange,
            label = { Text(stringResource(R.string.bio)) },
            placeholder = { Text(stringResource(R.string.bio_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
        )

        SaveButton(
            isSaving = data.isSavingProfile,
            isEnabled = !data.isSavingProfile,
            onClick = data.onSaveClick
        )
    }
}

@Composable
private fun ProfilePictureCard(
    profilePicture: String?,
    isLoadingPhoto: Boolean,
    onEditClick: () -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfilePictureWithEdit(
                profilePicture = profilePicture,
                isLoadingPhoto = isLoadingPhoto,
                onEditClick = onEditClick,
                onLoadingChange = onLoadingChange
            )
        }
    }
}

@Composable
private fun ProfilePictureWithEdit(
    profilePicture: String?,
    isLoadingPhoto: Boolean,
    onEditClick: () -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier.size(spacing.extraLarge5)
    ) {
        if (!profilePicture.isNullOrEmpty()) {
            AsyncImage(
                model = RetrofitClient.getPictureUri(profilePicture),
                onLoading = { onLoadingChange(true) },
                onSuccess = { onLoadingChange(false) },
                onError = { onLoadingChange(false) },
                contentDescription = stringResource(R.string.profile_picture),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            // Show default icon when no profile picture is available
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    name = R.drawable.ic_account_circle,
                    type = "regular"
                )
            }
        }

        if (isLoadingPhoto) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(spacing.large),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }

        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(spacing.extraLarge)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        ) {
            Icon(
                name = R.drawable.ic_edit,
                type = "light"
            )
        }
    }
}

@Composable
private fun SaveButton(
    isSaving: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Button(
        onClick = onClick,
        enabled = !isSaving && isEnabled,
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(spacing.medium),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.height(spacing.small))
        }
        Text(
            text = stringResource(if (isSaving) R.string.saving else R.string.save),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityAutocompleteField(
    query: String,
    selectedCity: String?,
    suggestions: List<String>,
    isEnabled: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    val textValue = if (hasFocus) query else if (selectedCity != null && query.isBlank()) selectedCity else query
    val showMenu = isEnabled && hasFocus && query.length >= 2 && suggestions.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded && showMenu,
        onExpandedChange = { if (isEnabled) expanded = it && hasFocus },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = {
                if (selectedCity != null) onClearSelection()
                onQueryChange(it)
                expanded = true
            },
            label = { Text("City") },
            placeholder = { Text("Start typing…") },
            singleLine = true,
            isError = error != null,
            enabled = isEnabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
            trailingIcon = {
                when {
                    selectedCity != null -> IconButton(onClick = {
                        onClearSelection()
                        expanded = hasFocus && query.isNotBlank()
                    }) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                    showMenu -> ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .onFocusChanged {
                    hasFocus = it.isFocused
                    expanded = it.isFocused && query.length >= 2 && suggestions.isNotEmpty()
                }
        )
        ExposedDropdownMenu(
            expanded = expanded && showMenu,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.take(8).forEach { s ->
                DropdownMenuItem(
                    text = { Text(s) },
                    onClick = {
                        onSelect(s)
                        expanded = false
                    }
                )
            }
        }
    }

    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperienceDropdown(
    selected: ExperienceLevel?,
    isEnabled: Boolean,
    error: String?,
    onSelect: (ExperienceLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selected?.label ?: "Select experience"

    Column(modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (isEnabled) expanded = !expanded }
        ) {
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
                        onClick = { if (isEnabled) onSelect(level) }
                    )
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
