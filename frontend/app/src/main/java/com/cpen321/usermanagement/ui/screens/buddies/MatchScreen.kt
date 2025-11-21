package com.cpen321.usermanagement.ui.screens.buddies

import coil.compose.AsyncImage
import Button
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.MatchViewModel
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState

@Composable
fun MatchScreen(
    modifier: Modifier = Modifier,
    viewModel: MatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val (errorMessage, setErrorMessage) = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.hasMoreProfiles, uiState.name) {
        if (uiState.name.isEmpty()) {
            setErrorMessage("No more matches")
        }
    }
    
    Scaffold(
        snackbarHost = {
            MessageSnackbar(
                hostState = snackBarHostState,
                messageState = MessageSnackbarState(
                    successMessage = null,
                    errorMessage = errorMessage,
                    onSuccessMessageShown = {},
                    onErrorMessageShown = { setErrorMessage(null) }
                )
            )
        }
    ) { padding ->
        MatchContent(
            modifier = modifier.padding(padding),
            isLoading = uiState.isLoading,
            state = MatchContentState(
                name = uiState.name,
                age = uiState.age,
                skillLevel = uiState.skillLevel,
                bio = uiState.bio,
                profilePicture = uiState.profilePicture,
                location = uiState.location,
                distance = uiState.distance,
                hasMoreProfiles = uiState.hasMoreProfiles
            ),
            callbacks = MatchContentCallbacks(
                onChatClick = { viewModel.onChatClick() },
                onBackClick = { viewModel.onBackClick() },
                onRejectClick = { viewModel.onRejectClick() }
            )
        )
    }
}

class MatchContentState(
    val name: String = "",
    val age: Int = 0,
    val skillLevel: String = "",
    val bio: String = "",
    val profilePicture: String? = null,
    val location: String = "",
    val distance: Double = 0.0,
    val hasMoreProfiles: Boolean = false
)

class MatchContentCallbacks(
    val onChatClick: () -> Unit = {},
    val onBackClick: () -> Unit = {},
    val onRejectClick: () -> Unit = {}
)

@Composable
private fun MatchContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    state: MatchContentState,
    callbacks: MatchContentCallbacks
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            isLoading -> CircularProgressIndicator()
            state.name.isNotEmpty() -> ProfileView(state = state, callbacks = callbacks)
            else -> EmptyMatchState(onBackClick = callbacks.onBackClick)
        }
    }
}

@Composable
private fun ProfileView(
    state: MatchContentState,
    callbacks: MatchContentCallbacks
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = Modifier
            .padding(top = spacing.large)
            .fillMaxHeight()
            .padding(horizontal = spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ProfileInfoSection(state = state)
        MatchActionButtons(
            hasMoreProfiles = state.hasMoreProfiles,
            callbacks = callbacks
        )
    }
}

@Composable
private fun ProfileInfoSection(state: MatchContentState) {
    val spacing = LocalSpacing.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        ProfilePictureDisplay(
            profilePicture = state.profilePicture,
            modifier = Modifier.size(spacing.extraLarge3 * 3)
        )
        
        ProfileName(name = state.name)
        ProfileLocation(location = state.location)
        ProfileAgeAndSkill(age = state.age, skillLevel = state.skillLevel)
        Spacer(modifier = Modifier.height(spacing.small))
        ProfileBio(bio = state.bio)
    }
}

@Composable
private fun ProfileName(name: String) {
    val spacing = LocalSpacing.current
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(bottom = spacing.medium)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ProfileLocation(location: String) {
    val spacing = LocalSpacing.current
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(bottom = spacing.medium)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_location),
            contentDescription = null
        )
        Text(
            text = location,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileAgeAndSkill(age: Int, skillLevel: String) {
    val spacing = LocalSpacing.current
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        ProfileInfoItem(
            iconRes = R.drawable.ic_person,
            text = "Age: $age",
            textColor = MaterialTheme.colorScheme.onSurface
        )
        ProfileInfoItem(
            iconRes = R.drawable.ic_level,
            text = "Level: ${if (skillLevel.isNotBlank()) skillLevel else "-"}",
            textColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ProfileInfoItem(iconRes: Int, text: String, textColor: Color) {
    val spacing = LocalSpacing.current
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null
        )
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

@Composable
private fun ProfileBio(bio: String) {
    val spacing = LocalSpacing.current
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_bio),
            contentDescription = null
        )
        Text(
            text = "Bio:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    
    Text(
        text = bio.ifEmpty { "No bio available" },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun MatchActionButtons(
    hasMoreProfiles: Boolean,
    callbacks: MatchContentCallbacks
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = Modifier.padding(bottom = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Button(
                onClick = callbacks.onChatClick,
                fullWidth = false
            ) {
                Text(text = "Match")
            }
            Button(
                onClick = callbacks.onBackClick,
                fullWidth = false
            ) {
                Text(text = "Back")
            }
            if (hasMoreProfiles) {
                Button(
                    onClick = callbacks.onRejectClick,
                    fullWidth = false
                ) {
                    Text(text = "Next")
                }
            }
        }
    }
}

@Composable
private fun EmptyMatchState(onBackClick: () -> Unit) {
    val spacing = LocalSpacing.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Text(
            text = "No more profiles to show",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Button(onClick = onBackClick) {
            Text(text = "Back to Buddies")
        }
    }
}

@Composable
private fun ProfilePictureDisplay(
    profilePicture: String?,
    modifier: Modifier = Modifier
) {
    if (!profilePicture.isNullOrEmpty()) {
        AsyncImage(
            model = RetrofitClient.getPictureUri(profilePicture),
            contentDescription = "Profile picture",
            modifier = modifier.clip(CircleShape)
        )
    } else {
        // Show default profile icon when no picture is available
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_account_circle),
                contentDescription = "Default profile picture",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
