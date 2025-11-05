package com.cpen321.usermanagement.ui.screens
import com.cpen321.usermanagement.ui.components.ExperienceLevel

import Button
import Icon
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.ui.components.ImagePicker
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.ProfileUiState
import com.cpen321.usermanagement.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cpen321.usermanagement.data.repository.ProfileUpdateParams

// State holder moved to its own file to keep this file lean
import com.cpen321.usermanagement.ui.screens.ProfileFormState


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

private data class ManageProfileContentData(
    val uiState: ProfileUiState,
    val formState: ProfileFormState,
    val snackBarHostState: SnackbarHostState,
    val showImagePickerDialog: Boolean,
    val actions: ManageProfileScreenActions,
    val citySuggestions: List<String>,
    val onAgeChange: (String) -> Unit,
    val onCityQueryChange: (String) -> Unit,
    val onCitySelect: (String) -> Unit,
    val onExperienceSelect: (ExperienceLevel) -> Unit
)

private data class ProfileInputHandlers(
    val onAgeChange: (String) -> Unit,
    val onCityQueryChange: (String) -> Unit,
    val onCitySelect: (String) -> Unit,
    val onExperienceSelect: (ExperienceLevel) -> Unit
)

@Composable
fun ManageProfileScreen(
    profileViewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    val suggestions by profileViewModel.citySuggestions.collectAsState()
    val scope = rememberCoroutineScope()
    var cityJob by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current
    val placesClient = remember { com.google.android.libraries.places.api.Places.createClient(context) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var formState by remember { mutableStateOf(ProfileFormState()) }

    ProfileInitEffects(
        uiState = uiState,
        profileViewModel = profileViewModel,
        placesClient = placesClient,
        setFormState = { formState = it }
    )

    val handlers = buildProfileInputHandlers(
        profileViewModel = profileViewModel,
        formState = formState,
        setFormState = { formState = it },
        scope = scope,
        cityJob = cityJob,
        setCityJob = { cityJob = it }
    )

    val actions = buildManageProfileActions(
        onBackClick = onBackClick,
        formState = formState,
        setFormState = { formState = it },
        scope = scope,
        profileViewModel = profileViewModel,
        setShowImagePickerDialog = { showImagePickerDialog = it }
    )

    val contentData = ManageProfileContentData(
        uiState = uiState,
        formState = formState,
        snackBarHostState = snackBarHostState,
        showImagePickerDialog = showImagePickerDialog,
        actions = actions,
        citySuggestions = suggestions.map { it.label },
        onAgeChange = handlers.onAgeChange,
        onCityQueryChange = handlers.onCityQueryChange,
        onCitySelect = handlers.onCitySelect,
        onExperienceSelect = handlers.onExperienceSelect
    )

    ManageProfileContent(data = contentData)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageProfileContent(
    data: ManageProfileContentData,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ProfileTopBar(onBackClick = data.actions.onBackClick)
        },
        snackbarHost = {
            MessageSnackbar(
                hostState = data.snackBarHostState,
                messageState = MessageSnackbarState(
                    successMessage = data.uiState.successMessage,
                    errorMessage = data.uiState.errorMessage,
                    onSuccessMessageShown = data.actions.onSuccessMessageShown,
                    onErrorMessageShown = data.actions.onErrorMessageShown
                )
            )
        }
    ) { paddingValues ->
        ProfileBody(
            paddingValues = paddingValues,
            data = ProfileBodyData(
                uiState = data.uiState,
                formState = data.formState,
                onNameChange = data.actions.onNameChange,
                onBioChange = data.actions.onBioChange,
                onEditPictureClick = data.actions.onEditPictureClick,
                onSaveClick = data.actions.onSaveClick,
                onLoadingPhotoChange = data.actions.onLoadingPhotoChange,
                citySuggestions = data.citySuggestions,
                onAgeChange = data.onAgeChange,
                onCityQueryChange = data.onCityQueryChange,
                onCitySelect = data.onCitySelect,
                onExperienceSelect = data.onExperienceSelect
            )
        )

    }

    if (data.showImagePickerDialog) {
        ImagePicker(
            onDismiss = data.actions.onImagePickerDismiss,
            onImageSelected = data.actions.onImageSelected
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

        NameAndAgeFields(data)

        LocationAndExperienceFields(data)

        BioField(data)

        SaveButton(
            isSaving = data.isSavingProfile,
            isEnabled = !data.isSavingProfile && data.formState.hasChanges(),
            onClick = data.onSaveClick
        )
    }
}

@Composable
private fun NameAndAgeFields(data: ProfileFormData) {
    OutlinedTextField(
        value = data.formState.name,
        onValueChange = data.onNameChange,
        label = { Text(stringResource(R.string.name)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

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
}

@Composable
private fun LocationAndExperienceFields(data: ProfileFormData) {
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

        ProfileExperienceDropdown(
        selected = data.formState.experience,
        isEnabled = !data.isSavingProfile,
        error = data.formState.expError,
        onSelect = data.onExperienceSelect
    )
}

@Composable
private fun BioField(data: ProfileFormData) {
    OutlinedTextField(
        value = data.formState.bioText,
        onValueChange = data.onBioChange,
        label = { Text(stringResource(R.string.bio)) },
        placeholder = { Text(stringResource(R.string.bio_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 5,
    )
}

@Composable
private fun ProfileInitEffects(
    uiState: ProfileUiState,
    profileViewModel: ProfileViewModel,
    placesClient: com.google.android.libraries.places.api.net.PlacesClient,
    setFormState: (ProfileFormState) -> Unit
) {
    LaunchedEffect(Unit) {
        profileViewModel.clearSuccessMessage()
        profileViewModel.clearError()
        profileViewModel.attachPlacesClient(placesClient)
        if (uiState.user == null) profileViewModel.loadProfile()
    }

    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            val originalExp = when ((user.skillLevel ?: "").lowercase()) {
                "beginner" -> ExperienceLevel.BEGINNER
                "intermediate" -> ExperienceLevel.INTERMEDIATE
                "expert" -> ExperienceLevel.EXPERT
                else -> null
            }
            setFormState(
                ProfileFormState(
                    name = user.name,
                    email = user.email,
                    bioText = user.bio ?: "",
                    ageText = user.age?.toString() ?: "",
                    selectedCity = user.location ?: "",
                    selectedCityPlaceId = null,
                    cityQuery = "",
                    experience = originalExp,
                    originalName = user.name,
                    originalAge = user.age,
                    originalCityDisplay = user.location,
                    originalExperience = originalExp,
                    originalBio = user.bio ?: ""
                )
            )
        }
    }
}

private fun buildProfileInputHandlers(
    profileViewModel: ProfileViewModel,
    formState: ProfileFormState,
    setFormState: (ProfileFormState) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    cityJob: Job?,
    setCityJob: (Job?) -> Unit
): ProfileInputHandlers {
    val onAgeChange: (String) -> Unit = { v ->
        if (v.length <= 3 && v.all(Char::isDigit)) setFormState(formState.copy(ageText = v))
    }
    val onCityQueryChange: (String) -> Unit = { q ->
        setFormState(
            formState.copy(
                cityQuery = q,
                selectedCity = null,
                selectedCityPlaceId = null
            )
        )
        cityJob?.cancel()
        setCityJob(
            scope.launch {
                delay(200)
                if (q.length >= 2) profileViewModel.queryCities(q) else profileViewModel.clearCitySuggestions()
            }
        )
    }
    val onCitySelect: (String) -> Unit = { label ->
        val match = profileViewModel.citySuggestions.value.firstOrNull { it.label == label }
        setFormState(
            formState.copy(
                selectedCity = label,
                selectedCityPlaceId = match?.placeId,
                cityQuery = label
            )
        )
        profileViewModel.clearCitySuggestions()
    }
    val onExperienceSelect: (ExperienceLevel) -> Unit = { lvl ->
        setFormState(formState.copy(experience = lvl))
    }
    return ProfileInputHandlers(onAgeChange, onCityQueryChange, onCitySelect, onExperienceSelect)
}

private fun buildManageProfileActions(
    onBackClick: () -> Unit,
    formState: ProfileFormState,
    setFormState: (ProfileFormState) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    profileViewModel: ProfileViewModel,
    setShowImagePickerDialog: (Boolean) -> Unit
): ManageProfileScreenActions {
    return ManageProfileScreenActions(
        onBackClick = onBackClick,
        onNameChange = { setFormState(formState.copy(name = it)) },
        onBioChange = { setFormState(formState.copy(bioText = it)) },
        onEditPictureClick = { setShowImagePickerDialog(true) },
        onSaveClick = {
            if (formState.canSave()) {
                scope.launch {
                    try {
                        val editingCity = formState.cityQuery.isNotBlank() ||
                                formState.selectedCity?.trim() != formState.originalCityDisplay?.trim()
                        val pid = formState.selectedCityPlaceId
                        val resolved = if (editingCity && pid != null) profileViewModel.resolveCity(pid) else null
                        val safeName = formState.name.trim()
                        val safeBio  = formState.bioText.trim()
                        val safeAge  = formState.ageOrNull
                        val skill    = formState.experience?.label
                        profileViewModel.updateProfile(
                            ProfileUpdateParams(
                                name = safeName,
                                bio = safeBio.ifEmpty { null },
                                age = safeAge,
                                location  = resolved?.display,
                                latitude  = resolved?.lat,
                                longitude = resolved?.lng,
                                skillLevel = skill
                            )
                        )
                    } catch (_: Exception) { }
                }
            } else {
                setFormState(formState.copy(showErrors = true))
            }
            
        },
        onImagePickerDismiss = { setShowImagePickerDialog(false) },
        onImageSelected = { uri: Uri ->
            setShowImagePickerDialog(false)
            profileViewModel.uploadProfilePicture(uri)
        },
        onLoadingPhotoChange = profileViewModel::setLoadingPhoto,
        onSuccessMessageShown = profileViewModel::clearSuccessMessage,
        onErrorMessageShown = profileViewModel::clearError
    )
}
