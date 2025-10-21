package com.cpen321.usermanagement.ui.screens

import Button
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

private enum class ExperienceLevel(val label: String) {
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), EXPERT("Expert")
}

private enum class DivingCondition(val label: String) {
    POOL("Pool"),
    OPEN_WATER("Open Water"),
    NIGHT("Night"),
    LOW_VIS("Low-Vis"),
    DEEP("Deep"),
    CURRENT("Current");
}

private data class ProfileCompletionFormState(
    val name: String = "",
    val ageText: String = "",
    val cityQuery: String = "",
    val selectedCity: String? = null, // label shown after selection
    val selectedCityPlaceId: String? = null, // <-- NEW: store the Google placeId
    val experience: ExperienceLevel? = null,
    val conditions: Set<DivingCondition> = emptySet(),
    val bioText: String = "",
    val hasSavedBio: Boolean = false
) {
    val ageOrNull: Int? get() = ageText.toIntOrNull()

    // --- errors ---
    val nameError: String? get() = if (name.isBlank()) "Required" else null
    val ageError: String?
        get() = when (val a = ageOrNull) {
            null -> "Enter a number"
            in 13..100 -> null
            else -> "Age must be 13–100"
        }
    // inside ProfileCompletionFormState
    val cityError: String? get() = if (selectedCityPlaceId.isNullOrBlank()) "Pick a city from suggestions" else null
    val expError: String? get() = if (experience == null) "Select level" else null
    val conditionsError: String? get() = if (conditions.isEmpty()) "Select at least one" else null
    val bioError: String? get() = if (bioText.length > 1000) "Max 1000 characters" else null

    fun canSave(): Boolean =
        nameError == null &&
                ageError == null &&
                cityError == null &&
                expError == null &&
                //conditionsError == null &&
                bioError == null
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
    val onConditionToggle: (DivingCondition) -> Unit = {},
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
    val onConditionToggle: (DivingCondition) -> Unit = {},
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
    val snackBarHostState = remember { SnackbarHostState() }

    // collect live city suggestions from the ViewModel
    val suggestions by profileViewModel.citySuggestions.collectAsState()

// used to debounce typing
    val scope = rememberCoroutineScope()
    var cityJob by remember { mutableStateOf<Job?>(null) }

    val context = LocalContext.current
    val placesClient = remember { com.google.android.libraries.places.api.Places.createClient(context) }

    LaunchedEffect(Unit) {
        profileViewModel.attachPlacesClient(placesClient)
    }

    val successfulBioUpdateMessage = stringResource(R.string.successful_bio_update)

    // Form state
    var formState by remember {
        mutableStateOf(ProfileCompletionFormState())
    }

    // Side effects
    LaunchedEffect(Unit) {
        if (uiState.user == null) {
            profileViewModel.loadProfile()
        }
    }

    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            if (user.bio != null && user.bio.isNotBlank() && !formState.hasSavedBio) {
                onProfileCompleted()
            }
        }
    }

    ProfileCompletionContent(
        data = ProfileCompletionScreenContentData(
            uiState = uiState,
            formState = formState,
            snackBarHostState = snackBarHostState,

            // NEW handlers so fields actually update state
            onNameChange = { formState = formState.copy(name = it) },
            onAgeChange  = { v -> if (v.length <= 3 && v.all(Char::isDigit)) formState = formState.copy(ageText = v) },
            onExperienceSelect = { lvl -> formState = formState.copy(experience = lvl) },
            onConditionToggle = { cond ->
                val next = formState.conditions.toMutableSet().apply {
                    if (contains(cond)) remove(cond) else add(cond)
                }
                formState = formState.copy(conditions = next)
            },

            // keep bio but clamp to 1000 to be safe
            onBioChange = { formState = formState.copy(bioText = it.take(1000)) },

            // --- City autocomplete wiring ---
            citySuggestions = suggestions.map { it.label },

            onCityQueryChange = { q ->
                formState = formState.copy(cityQuery = q, selectedCity = null, selectedCityPlaceId = null)
                cityJob?.cancel()
                cityJob = scope.launch {
                    delay(200) // debounce typing
                    profileViewModel.queryCities(q)
                }
            },
            onCitySelect = { label ->
                val match = suggestions.firstOrNull { it.label == label }
                formState = formState.copy(
                    selectedCity = label,
                    selectedCityPlaceId = match?.placeId,
                    cityQuery = ""
                )
            },


            onSkipClick = onProfileCompleted,
            onSaveClick = {
                // Local validation first (you already have canSave())
                if (!formState.canSave()) {
                    scope.launch { snackBarHostState.showSnackbar("Please complete all required fields") }
                    return@ProfileCompletionScreenContentData
                }
                val pid = formState.selectedCityPlaceId
                if (pid == null) {
                    scope.launch { snackBarHostState.showSnackbar("Please pick a city from suggestions") }
                    return@ProfileCompletionScreenContentData
                }

                scope.launch {
                    try {
                        val resolved = profileViewModel.resolveCity(pid)

                        val safeName = formState.name.trim()
                        val safeBio = formState.bioText.take(500).trim()                 // backend max 500
                        val safeAge = formState.ageOrNull                                 // nullable Int
                        val skill = formState.experience?.name?.lowercase()               // "beginner|intermediate|advanced"

                        // Call the full-profile updater in your ViewModel
                        profileViewModel.updateProfileFull(
                            name = safeName,                                              // keep if your VM/repo accepts it
                            bio = safeBio,
                            age = safeAge,
                            location = resolved.display,                                  // human-readable city
                            latitude = resolved.lat,
                            longitude = resolved.lng,
                            skillLevel = skill
                        ) {
                            onProfileCompletedWithMessage(successfulBioUpdateMessage)
                        }
                    } catch (e: Exception) {
                        snackBarHostState.showSnackbar("Couldn’t verify city. Please try again.")
                    }
                }
            },
            onErrorMessageShown = profileViewModel::clearError
        )
    )

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
                onConditionToggle = data.onConditionToggle,
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
        verticalArrangement = Arrangement.Center
    ) {
        ProfileCompletionHeader()

        Spacer(modifier = Modifier.height(spacing.extraLarge2))

        // --- Name ---
        NameInputField(
            name = data.formState.name,
            isEnabled = !data.isSavingProfile,
            error = data.formState.nameError,
            onNameChange = data.onNameChange
        )

        Spacer(modifier = Modifier.height(spacing.extraLarge))

        // --- Age ---
        AgeInputField(
            ageText = data.formState.ageText,
            isEnabled = !data.isSavingProfile,
            error = data.formState.ageError,
            onAgeChange = data.onAgeChange
        )

        Spacer(modifier = Modifier.height(spacing.extraLarge))

        // --- City (autocomplete suggestions list shown as a dropdown) ---
        CityAutocompleteField(
            query = data.formState.cityQuery.takeIf { data.formState.selectedCity == null }
                ?: (data.formState.selectedCity ?: ""),
            suggestions = data.citySuggestions,
            isEnabled = !data.isSavingProfile,
            error = data.formState.cityError,
            onQueryChange = data.onCityQueryChange,
            onSelect = data.onCitySelect
        )

        Spacer(modifier = Modifier.height(spacing.extraLarge))

        // --- Experience Level ---
        ExperienceDropdown(
            selected = data.formState.experience,
            isEnabled = !data.isSavingProfile,
            error = data.formState.expError,
            onSelect = data.onExperienceSelect
        )

        Spacer(modifier = Modifier.height(spacing.extraLarge))

        // --- Preferred Diving Conditions ---
//        PreferredConditionsField(
//            selected = data.formState.conditions,
//            isEnabled = !data.isSavingProfile,
//            error = data.formState.conditionsError,
//            onToggle = data.onConditionToggle
//        )
//
//        Spacer(modifier = Modifier.height(spacing.extraLarge))

        // --- Bio (<= 1000) ---
        BioInputField(
            bioText = data.formState.bioText,
            isEnabled = !data.isSavingProfile,
            onBioChange = data.onBioChange
        )

        Spacer(modifier = Modifier.height(spacing.extraLarge))

        // DEBUG: show what's blocking save (remove in prod)
        val blockers = listOf(
            "nameError" to data.formState.nameError,
            "ageError" to data.formState.ageError,
            "cityError" to data.formState.cityError,
            "expError" to data.formState.expError,
            "conditionsError" to data.formState.conditionsError,
            "bioError" to data.formState.bioError
        ).filter { it.second != null }

        if (blockers.isNotEmpty()) {
            Text(
                text = "Blocking: " + blockers.joinToString { it.first },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // --- Actions ---
        ActionButtons(
            isSavingProfile = data.isSavingProfile,
            isSaveEnabled = data.formState.canSave(),
            onSkipClick = data.onSkipClick,
            onSaveClick = data.onSaveClick
        )
    }
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
        onValueChange = { v -> if (v.length <= 3 && v.all(Char::isDigit)) onAgeChange(v) },
        label = { Text("Age") },
        placeholder = { Text("e.g., 22") },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        enabled = isEnabled,
    )
}

@Composable
private fun CityAutocompleteField(
    query: String,
    suggestions: List<String>,
    isEnabled: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                showMenu = it.isNotBlank()
            },
            label = { Text("City") },
            placeholder = { Text("Start typing…") },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            enabled = isEnabled,
        )
        DropdownMenu(
            expanded = showMenu && suggestions.isNotEmpty(),
            onDismissRequest = { showMenu = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.take(8).forEach { s ->
                DropdownMenuItem(
                    text = { Text(s) },
                    onClick = { onSelect(s); showMenu = false }
                )
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
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
private fun PreferredConditionsField(
    selected: Set<DivingCondition>, // your enum or sealed class
    isEnabled: Boolean,
    error: String?,
    onToggle: (DivingCondition) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Preferred Diving Conditions", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(spacing.small))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            DivingCondition.entries.forEach { cond ->
                FilterChip(
                    selected = cond in selected,
                    onClick = { if (isEnabled) onToggle(cond) },
                    label = { Text(cond.label) },
                    enabled = isEnabled
                )
            }
        }
        if (error != null) {
            Spacer(Modifier.height(spacing.small))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
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
