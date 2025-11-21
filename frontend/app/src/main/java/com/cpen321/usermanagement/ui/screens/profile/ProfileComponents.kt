@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.cpen321.usermanagement.ui.screens

import Button
import Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.ui.components.ExperienceLevel
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.ui.window.PopupProperties

@Composable
fun ProfilePictureCard(
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
fun DefaultProfilePicture(
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Box(modifier = modifier.size(spacing.extraLarge)) {
        Icon(
            name = R.drawable.ic_account_circle,
            type = "regular"
        )
    }
}

@Composable
fun ProfilePictureWithEdit(
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
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            DefaultProfilePicture(modifier)
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
fun SaveButton(
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
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ExposedDropdownMenuBoxScope.CitySuggestionsDropdown(
    expanded: Boolean,
    showMenu: Boolean,
    suggestions: List<String>,
    onSelect: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
    ) {
    DropdownMenu(
        expanded = expanded && showMenu,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = PopupProperties(focusable = false)
    ) {
        suggestions.take(8).forEach { s ->
            DropdownMenuItem(text = { Text(s) }, onClick = { onSelect(s) })
        }
    }
}

@Composable
fun ErrorText(error: String?, modifier: Modifier = Modifier, textStyle: TextStyle = MaterialTheme.typography.bodySmall) {
    if (error != null) Text(text = error, color = MaterialTheme.colorScheme.error, style = textStyle, modifier = modifier)
}

data class ProfileCityAutocompleteData(
    val query: String,
    val selectedCity: String?,
    val suggestions: List<String>,
    val isEnabled: Boolean,
    val error: String?,
)

data class ProfileCityAutocompleteCallbacks(
    val onQueryChange: (String) -> Unit,
    val onSelect: (String) -> Unit,
    val onClearSelection: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityAutocompleteField(data: ProfileCityAutocompleteData, 
                            callbacks: ProfileCityAutocompleteCallbacks, 
                            modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    val textValue = when {
        hasFocus && data.query.isNotEmpty() -> data.query
        !data.selectedCity.isNullOrEmpty() -> data.selectedCity
        else -> data.query
    }
    val showMenu = data.isEnabled && hasFocus && data.query.length >= 2 && data.suggestions.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded && showMenu,
        onExpandedChange = { if (data.isEnabled) expanded = it && hasFocus },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = {
                if (data.selectedCity != null) callbacks.onClearSelection()
                callbacks.onQueryChange(it)
                expanded = true
            },
            label = { Text("City") },
            placeholder = { Text("Start typing…") },
            singleLine = true,
            isError = data.error != null,
            enabled = data.isEnabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
            trailingIcon = {
                when {
                    data.selectedCity != null -> IconButton(onClick = {
                        callbacks.onClearSelection()
                        expanded = hasFocus && data.query.isNotBlank()
                    }) { M3Icon(Icons.Default.Close, contentDescription = "Clear") }
                    showMenu -> ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .onFocusChanged {
                    hasFocus = it.isFocused
                    expanded = it.isFocused && data.query.length >= 2 && data.suggestions.isNotEmpty()
                }
        )
        CitySuggestionsDropdown(
            expanded = expanded,
            showMenu = showMenu,
            suggestions = data.suggestions,
            onSelect = callbacks.onSelect,
            onDismissRequest = { expanded = false },
            modifier = modifier
        )
    }

    ErrorText(error = data.error)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileExperienceDropdown(
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
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                com.cpen321.usermanagement.ui.components.ExperienceLevel.values().forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level.label) },
                        onClick = { if (isEnabled) onSelect(level) }
                    )
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ProfileCompletionHeader(
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
fun WelcomeTitle(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.complete_profile),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
fun BioDescription(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.bio_description),
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
    )
}


