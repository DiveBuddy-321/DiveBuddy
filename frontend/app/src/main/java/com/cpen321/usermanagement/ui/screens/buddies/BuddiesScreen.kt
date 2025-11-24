package com.cpen321.usermanagement.ui.screens.buddies

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.buddies.BuddyViewModel
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
            state = BuddiesContentState(
                targetMinLevel = uiState.targetMinLevel ?: Constants.BEGINNER_LEVEL,
                targetMaxLevel = uiState.targetMaxLevel ?: Constants.ADVANCED_LEVEL,
                targetMinAge = uiState.targetMinAge ?: Constants.MIN_AGE,
                targetMaxAge = uiState.targetMaxAge ?: Constants.MAX_AGE,
                errorMessage = uiState.errorMessage
            ),
            callbacks = BuddiesContentCallbacks(
                onMatchClick = { viewModel.fetchBuddies() },
                onFiltersChange = { minLevel, maxLevel, minAge, maxAge ->
                    viewModel.setFilters(minLevel, maxLevel, minAge, maxAge)
                }
            )
        )
    }
}

private data class BuddiesContentState(
    val targetMinLevel: Int = Constants.BEGINNER_LEVEL,
    val targetMaxLevel: Int = Constants.ADVANCED_LEVEL,
    val targetMinAge: Int = Constants.MIN_AGE,
    val targetMaxAge: Int = Constants.MAX_AGE,
    val errorMessage: String? = null
)

private data class BuddiesContentCallbacks(
    val onMatchClick: () -> Unit = {},
    val onFiltersChange: (Int?, Int?, Int?, Int?) -> Unit = { _, _, _, _ -> }
)

@Composable
private fun FilterSlider(
    label: String,
    minValue: Int,
    maxValue: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$label: $minValue - $maxValue",
            style = MaterialTheme.typography.bodyLarge
        )
        RangeSlider(
            value = minValue.toFloat()..maxValue.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(horizontal = spacing.large)
        )
    }
}

@Composable
private fun MatchButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        fullWidth = false
    ) {
        Text(text = "Match with Buddies")
    }
}

@Composable
private fun BuddiesContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    state: BuddiesContentState,
    callbacks: BuddiesContentCallbacks
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(top = spacing.large)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BuddiesHeader()
            
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                BuddiesFiltersSection(
                    state = state,
                    callbacks = callbacks
                )
            }
            
            MatchButton(onClick = callbacks.onMatchClick)
        }
    }
}

@Composable
private fun BuddiesHeader() {
    Text(
        text = "Buddies",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun BuddiesFiltersSection(
    state: BuddiesContentState,
    callbacks: BuddiesContentCallbacks
) {
    val spacing = LocalSpacing.current
    var minLevel by remember { mutableStateOf(state.targetMinLevel) }
    var maxLevel by remember { mutableStateOf(state.targetMaxLevel) }
    var minAge by remember { mutableStateOf(state.targetMinAge) }
    var maxAge by remember { mutableStateOf(state.targetMaxAge) }

    Column(
        modifier = Modifier.padding(bottom = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Filters",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        FilterSlider(
            label = "Experience Level",
            minValue = minLevel,
            maxValue = maxLevel,
            valueRange = Constants.BEGINNER_LEVEL.toFloat()..Constants.ADVANCED_LEVEL.toFloat(),
            onValueChange = { range ->
                minLevel = range.start.roundToInt()
                    .coerceIn(Constants.BEGINNER_LEVEL, Constants.ADVANCED_LEVEL)
                maxLevel = range.endInclusive.roundToInt()
                    .coerceIn(Constants.BEGINNER_LEVEL, Constants.ADVANCED_LEVEL)
                callbacks.onFiltersChange(minLevel, maxLevel, minAge, maxAge)
            }
        )

        Text(
            text = getLevelLabel(minLevel, maxLevel),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        FilterSlider(
            label = "Age Range",
            minValue = minAge,
            maxValue = maxAge,
            valueRange = Constants.MIN_AGE.toFloat()..Constants.MAX_AGE.toFloat(),
            onValueChange = { range ->
                minAge = range.start.roundToInt()
                    .coerceIn(Constants.MIN_AGE, Constants.MAX_AGE)
                maxAge = range.endInclusive.roundToInt()
                    .coerceIn(Constants.MIN_AGE, Constants.MAX_AGE)
                callbacks.onFiltersChange(minLevel, maxLevel, minAge, maxAge)
            }
        )
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
