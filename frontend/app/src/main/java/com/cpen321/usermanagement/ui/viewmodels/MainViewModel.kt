package com.cpen321.usermanagement.ui.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class MainUiState(
    val successMessage: String? = null,
    val currentScreen: String = "events",
    val showMatchScreen: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun setSuccessMessage(message: String) {
        _uiState.value = _uiState.value.copy(successMessage = message)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun setCurrentScreen(screen: String) {
        _uiState.value = _uiState.value.copy(
            currentScreen = screen,
            showMatchScreen = false
        )
    }

    fun navigateToMatchScreen() {
        _uiState.value = _uiState.value.copy(showMatchScreen = true)
    }

    fun navigateBackFromMatchScreen() {
        _uiState.value = _uiState.value.copy(showMatchScreen = false)
    }
}
