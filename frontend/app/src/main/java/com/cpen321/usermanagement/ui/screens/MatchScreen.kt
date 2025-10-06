package com.cpen321.usermanagement.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.MatchViewModel

@Composable
fun MatchScreen(
    modifier: Modifier = Modifier,
    viewModel: MatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    MatchContent(
        modifier = modifier,
        isLoading = uiState.isLoading,
        name = uiState.name,
        age = uiState.age,
        level = uiState.level,
        bio = uiState.bio,
        profilePicture = uiState.profilePicture,
        location = uiState.location,
        distance = uiState.distance,
        hasMoreProfiles = uiState.hasMoreProfiles,
        onChatClick = { viewModel.onChatClick() },
        onBackClick = { viewModel.onBackClick() },
        onRejectClick = { viewModel.onRejectClick() }
    )
}

@Composable
private fun MatchContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    name: String = "",
    age: Int = 0,
    level: Int = 0,
    bio: String = "",
    profilePicture: String? = null,
    location: String = "",
    distance: Double = 0.0,
    hasMoreProfiles: Boolean = false,
    onChatClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onRejectClick: () -> Unit = {}
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (name.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(top = spacing.large)
                    .fillMaxHeight()
                    .padding(horizontal = spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    ProfilePictureDisplay(
                        profilePicture = profilePicture,
                        modifier = Modifier.size(spacing.extraLarge3 * 3)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        Text(
                            text = "Age: $age",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Level: $level",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(spacing.small))
                    
                    Text(
                        text = bio.ifEmpty { "No bio available" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column(
                    modifier = Modifier.padding(bottom = spacing.large),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Button(
                            onClick = onChatClick,
                            fullWidth = false
                        ) {
                            Text(text = "Chat")
                        }
                        Button(
                            onClick = onBackClick,
                            fullWidth = false
                        ) {
                            Text(text = "Back")
                        }
                        if (hasMoreProfiles) {
                            Button(
                                onClick = onRejectClick,
                                fullWidth = false
                            ) {
                                Text(text = "Next")
                            }   
                        }
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Text(
                    text = "No more profiles to show",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onBackClick
                ) {
                    Text(text = "Back to Buddies")
                }
            }
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
