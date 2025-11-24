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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.cpen321.usermanagement.ui.components.DetailsRow
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.components.profile.ProfileDetailsCard
import com.cpen321.usermanagement.ui.components.profile.ProfileLocation
import com.cpen321.usermanagement.ui.components.profile.ProfileName
import com.cpen321.usermanagement.ui.components.profile.ProfilePictureDisplay

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
        ProfileDetailsCard(age = state.age, skillLevel = state.skillLevel, bio = state.bio)
        Spacer(modifier = Modifier.height(spacing.small))
    }
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
            horizontalArrangement = Arrangement.spacedBy(spacing.extraLarge2)
        ) {
            Button(
                onClick = callbacks.onChatClick,
                fullWidth = false
            ) {
                Text(text = "Match")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Button(
                    type = "secondary",
                    onClick = callbacks.onBackClick,
                    fullWidth = false,
                ) {
                    Text(text = "Back")
                }
                Button(
                    type = "secondary",
                    onClick = callbacks.onRejectClick,
                    fullWidth = false,
                    enabled = hasMoreProfiles
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
