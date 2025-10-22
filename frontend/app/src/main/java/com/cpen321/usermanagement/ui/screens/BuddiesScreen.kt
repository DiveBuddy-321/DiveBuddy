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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.BuddyViewModel
import Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.common.Constants

@Composable
fun BuddiesScreen(
    modifier: Modifier = Modifier,
    viewModel: BuddyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            MessageSnackbar(
                hostState = snackBarHostState,
                messageState = MessageSnackbarState(
                    successMessage = uiState.successMessage,
                    errorMessage = uiState.errorMessage,
                    onSuccessMessageShown = viewModel::clearSuccessMessage,
                    onErrorMessageShown = viewModel::clearError
                )
            )
        }
    ) { padding ->
        BuddiesContent(
            modifier = modifier.padding(padding),
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

    var minLevel by remember { mutableStateOf(targetMinLevel ?: Constants.BEGINNER_LEVEL) }
    var maxLevel by remember { mutableStateOf(targetMaxLevel ?: Constants.ADVANCED_LEVEL) }
    var minAge by remember { mutableStateOf(targetMinAge ?: Constants.MIN_AGE) }
    var maxAge by remember { mutableStateOf(targetMaxAge ?: Constants.MAX_AGE) }

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
                    Text(text = "Level: ${getLevelLabel(minLevel, maxLevel)}", style = MaterialTheme.typography.bodyLarge)
                    RangeSlider(
                        value = minLevel.toFloat()..maxLevel.toFloat(),
                        onValueChange = { range ->
                            val start = range.start.roundToInt().coerceIn(Constants.BEGINNER_LEVEL, Constants.ADVANCED_LEVEL)
                            val end = range.endInclusive.roundToInt().coerceIn(Constants.BEGINNER_LEVEL, Constants.ADVANCED_LEVEL)
                            minLevel = minOf(start, end)
                            maxLevel = maxOf(start, end)
                            onFiltersChange(minLevel, maxLevel, minAge, maxAge)
                        },
                        valueRange = Constants.BEGINNER_LEVEL.toFloat()..Constants.ADVANCED_LEVEL.toFloat(),
                        steps = 1,
                        modifier = Modifier.padding(horizontal = spacing.large)
                    )

                    // Age range slider
                    Text(text = "Age: $minAge - $maxAge", style = MaterialTheme.typography.bodyLarge)
                    RangeSlider(
                        value = minAge.toFloat()..maxAge.toFloat(),
                        onValueChange = { range ->
                            val start = range.start.roundToInt().coerceIn(Constants.MIN_AGE, Constants.MAX_AGE)
                            val end = range.endInclusive.roundToInt().coerceIn(Constants.MIN_AGE, Constants.MAX_AGE)
                            minAge = minOf(start, end)
                            maxAge = maxOf(start, end)
                            onFiltersChange(minLevel, maxLevel, minAge, maxAge)
                        },
                        valueRange = Constants.MIN_AGE.toFloat()..Constants.MAX_AGE.toFloat(),
                        modifier = Modifier.padding(horizontal = spacing.large)
                    )

                    Button(
                        onClick = onMatchClick,
                        fullWidth = false
                    ) {
                        Text(text = "Match with Buddies")
                    }
                    
                }
            }
        }
    }
}

private fun getLevelLabel(minLevel: Int, maxLevel: Int): String {
    val levelNames = listOf("Beginner", "Intermediate", "Advanced")
    if (minLevel < Constants.BEGINNER_LEVEL || maxLevel > Constants.ADVANCED_LEVEL || minLevel > maxLevel) {
        return "Invalid level range"
    }
    return if (minLevel == maxLevel) {
        "${levelNames[minLevel - Constants.BEGINNER_LEVEL]} only"
    } else if (minLevel == Constants.BEGINNER_LEVEL && maxLevel == Constants.ADVANCED_LEVEL) {
        "All levels"
    } else {
        levelNames.subList(minLevel - Constants.BEGINNER_LEVEL, maxLevel - Constants.BEGINNER_LEVEL + 1).joinToString(" and ")
    }
}
