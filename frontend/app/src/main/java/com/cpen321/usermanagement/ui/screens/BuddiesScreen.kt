package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt
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
        errorMessage = uiState.errorMessage,
        targetMinLevel = uiState.targetMinLevel,
        targetMaxLevel = uiState.targetMaxLevel,
        targetMinAge = uiState.targetMinAge,
        targetMaxAge = uiState.targetMaxAge,
        onMatchClick = { 
            viewModel.fetchBuddies()
        },
        onFiltersChange = { minLevel, maxLevel, minAge, maxAge ->
            viewModel.setFilters(minLevel, maxLevel, minAge, maxAge)
        }
    )
}

@Composable
private fun BuddiesContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    targetMinLevel: Int? = null,
    targetMaxLevel: Int? = null,
    targetMinAge: Int? = null,
    targetMaxAge: Int? = null,
    onMatchClick: () -> Unit = {},
    onFiltersChange: (Int?, Int?, Int?, Int?) -> Unit = { _, _, _, _ -> }
) {
    val spacing = LocalSpacing.current

    var minLevel by remember { mutableStateOf(targetMinLevel ?: 1) }
    var maxLevel by remember { mutableStateOf(targetMaxLevel ?: 3) }
    var minAge by remember { mutableStateOf(targetMinAge ?: 13) }
    var maxAge by remember { mutableStateOf(targetMaxAge ?: 100) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.padding(top = spacing.large).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
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
                Column(
                    modifier = Modifier.padding(bottom = spacing.large),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Filters", style = MaterialTheme.typography.headlineSmall)

                    // Level range slider
                    Text(text = "Level: $minLevel - $maxLevel", style = MaterialTheme.typography.bodyLarge)
                    RangeSlider(
                        value = minLevel.toFloat()..maxLevel.toFloat(),
                        onValueChange = { range ->
                            val start = range.start.roundToInt().coerceIn(1, 3)
                            val end = range.endInclusive.roundToInt().coerceIn(1, 3)
                            minLevel = minOf(start, end)
                            maxLevel = maxOf(start, end)
                            onFiltersChange(minLevel, maxLevel, minAge, maxAge)
                        },
                        valueRange = 1f..3f,
                        steps = 1,
                        modifier = Modifier.padding(horizontal = spacing.large)
                    )

                    // Age range slider
                    Text(text = "Age: $minAge - $maxAge", style = MaterialTheme.typography.bodyLarge)
                    RangeSlider(
                        value = minAge.toFloat()..maxAge.toFloat(),
                        onValueChange = { range ->
                            val start = range.start.roundToInt().coerceIn(13, 100)
                            val end = range.endInclusive.roundToInt().coerceIn(13, 100)
                            minAge = minOf(start, end)
                            maxAge = maxOf(start, end)
                            onFiltersChange(minLevel, maxLevel, minAge, maxAge)
                        },
                        valueRange = 13f..100f,
                        modifier = Modifier.padding(horizontal = spacing.large)
                    )

                    Button(
                        onClick = onMatchClick,
                        fullWidth = false
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
                }
            }
        }
    }
}
