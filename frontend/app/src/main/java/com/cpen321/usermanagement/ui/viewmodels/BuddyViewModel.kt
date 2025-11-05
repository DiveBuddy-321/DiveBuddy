package com.cpen321.usermanagement.ui.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.Buddy
import com.cpen321.usermanagement.data.repository.BuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.cpen321.usermanagement.common.Constants

data class BuddyUiState(
    val isLoading: Boolean = false,
    val buddies: List<Buddy> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showMatches: Boolean = false,
    val targetMinLevel: Int? = null,
    val targetMaxLevel: Int? = null,
    val targetMinAge: Int? = null,
    val targetMaxAge: Int? = null
)

@HiltViewModel
open class BuddyViewModel @Inject constructor(
    private val buddyRepository: BuddyRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "BuddyViewModel"
        private const val KEY_MIN_LEVEL = "target_min_level"
        private const val KEY_MAX_LEVEL = "target_max_level"
        private const val KEY_MIN_AGE = "target_min_age"
        private const val KEY_MAX_AGE = "target_max_age"
    }

    private val _uiState = MutableStateFlow(BuddyUiState())
    val uiState: StateFlow<BuddyUiState> = _uiState.asStateFlow()
    
    var onNavigateToMatch: (() -> Unit)? = null

    init {
        val minLevel: Int? = savedStateHandle.get<Int?>(KEY_MIN_LEVEL)
        val maxLevel: Int? = savedStateHandle.get<Int?>(KEY_MAX_LEVEL)
        val minAge: Int? = savedStateHandle.get<Int?>(KEY_MIN_AGE)
        val maxAge: Int? = savedStateHandle.get<Int?>(KEY_MAX_AGE)
        if (minLevel != null || maxLevel != null || minAge != null || maxAge != null) {
            _uiState.value = _uiState.value.copy(
                targetMinLevel = minLevel,
                targetMaxLevel = maxLevel,
                targetMinAge = minAge,
                targetMaxAge = maxAge
            )
        }
    }

    fun setFilters(
        targetMinLevel: Int?,
        targetMaxLevel: Int?,
        targetMinAge: Int?,
        targetMaxAge: Int?
    ) {
        _uiState.value = _uiState.value.copy(
            targetMinLevel = targetMinLevel,
            targetMaxLevel = targetMaxLevel,
            targetMinAge = targetMinAge,
            targetMaxAge = targetMaxAge
        )
        savedStateHandle[KEY_MIN_LEVEL] = targetMinLevel
        savedStateHandle[KEY_MAX_LEVEL] = targetMaxLevel
        savedStateHandle[KEY_MIN_AGE] = targetMinAge
        savedStateHandle[KEY_MAX_AGE] = targetMaxAge
    }

    private fun validateFilters(): String? {
        val s = _uiState.value
        val minLevel = s.targetMinLevel
        val maxLevel = s.targetMaxLevel
        val minAge = s.targetMinAge
        val maxAge = s.targetMaxAge

        if ((minLevel != null && (minLevel < Constants.BEGINNER_LEVEL || minLevel > Constants.ADVANCED_LEVEL)) ||
            (maxLevel != null && (maxLevel < Constants.BEGINNER_LEVEL || maxLevel > Constants.ADVANCED_LEVEL))) {
            return "Level must be between ${Constants.BEGINNER_LEVEL} and ${Constants.ADVANCED_LEVEL}"
        }
        if (minLevel != null && maxLevel != null && minLevel > maxLevel) {
            return "Min level cannot exceed max level"
        }
        if ((minAge != null && (minAge < Constants.MIN_AGE || minAge > Constants.MAX_AGE)) ||
            (maxAge != null && (maxAge < Constants.MIN_AGE || maxAge > Constants.MAX_AGE))) {
            return "Age must be between ${Constants.MIN_AGE} and ${Constants.MAX_AGE}"
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            return "Min age cannot exceed max age"
        }
        return null
    }

    fun fetchBuddies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            // validate filters before calling API
            validateFilters()?.let { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err
                )
                return@launch
            }

            val s = _uiState.value
            val result = buddyRepository.getBuddies(
                targetMinLevel = s.targetMinLevel,
                targetMaxLevel = s.targetMaxLevel,
                targetMinAge = s.targetMinAge,
                targetMaxAge = s.targetMaxAge
            )

            if (result.isSuccess) {
                val buddies = result.getOrNull()!!
                if (buddies.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        buddies = emptyList(),
                        errorMessage = "No buddies found",
                        successMessage = null,
                        showMatches = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        buddies = buddies,
                        successMessage = "Found ${buddies.size} buddies!",
                        showMatches = true
                    )
                    // Immediately navigate to match screen if we have buddies
                    onNavigateToMatch?.invoke()
                }
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to fetch buddies", error)
                val errorMessage = error?.message ?: "Failed to fetch buddies"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun clearState() {
        _uiState.value = BuddyUiState()
    }
}

