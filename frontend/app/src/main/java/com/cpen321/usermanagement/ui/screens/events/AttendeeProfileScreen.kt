package com.cpen321.usermanagement.ui.screens.events

import Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.events.AttendeeProfileViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.ui.components.profile.ProfileDetailsCard
import com.cpen321.usermanagement.ui.components.profile.ProfileLocation
import com.cpen321.usermanagement.ui.components.profile.ProfileName
import com.cpen321.usermanagement.ui.components.profile.ProfilePictureDisplay

@Composable
fun AttendeeProfileScreen(
    user: User,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    attendeeProfileViewModel: AttendeeProfileViewModel = hiltViewModel()
) {
    val uiState by attendeeProfileViewModel.uiState.collectAsState()
    val spacing = LocalSpacing.current

    // Set the user when the screen is first displayed
    androidx.compose.runtime.LaunchedEffect(user) {
        attendeeProfileViewModel.setUser(user)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to attendees"
                )
            }
            Spacer(Modifier.width(spacing.medium))
            Text(
                text = "User Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Profile content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val displayUser = uiState.user ?: user
            ProfileInfoSection(
                user = displayUser,
                isCreatingChat = uiState.isCreatingChat,
                onChatClick = { attendeeProfileViewModel.onChatClick() }
            )
        }
    }
}

@Composable
private fun ProfileInfoSection(
    user: User,
    isCreatingChat: Boolean,
    onChatClick: () -> Unit
) {
    val spacing = LocalSpacing.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Profile picture
        ProfilePictureDisplay(
            profilePicture = user.profilePicture,
            modifier = Modifier.size(spacing.extraLarge3 * 3)
        )

        // Name
        ProfileName(name = user.name)

        // Location
        if (!user.location.isNullOrEmpty()) {
            ProfileLocation(location = user.location)
        }

        // Details card
        ProfileDetailsCard(
            age = user.age,
            skillLevel = user.skillLevel,
            bio = user.bio
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Chat button
        Button(
            onClick = onChatClick,
            enabled = !isCreatingChat,
            fullWidth = true
        ) {
            if (isCreatingChat) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(text = "Chat")
            }
        }
    }
}
