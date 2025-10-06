package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.BuddyViewModel
import Button

@Composable
fun BuddiesScreen(
    modifier: Modifier = Modifier,
    viewModel: BuddyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    BuddiesContent(
        modifier = modifier,
        isLoading = uiState.isLoading,
        buddiesCount = uiState.buddies.size,
        errorMessage = uiState.errorMessage,
        successMessage = uiState.successMessage,
        onMatchClick = { viewModel.fetchBuddies() }
    )
}

@Composable
private fun BuddiesContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    buddiesCount: Int = 0,
    errorMessage: String? = null,
    successMessage: String? = null,
    onMatchClick: () -> Unit = {}
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Text(
                text = "Buddies",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = onMatchClick
                ) {
                    Text(text = "Match with Buddies")
                }
                
                errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                successMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (buddiesCount > 0) {
                    Text(
                        text = "Showing $buddiesCount matches",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
