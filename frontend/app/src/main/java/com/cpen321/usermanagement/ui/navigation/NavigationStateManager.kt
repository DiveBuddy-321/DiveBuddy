package com.cpen321.usermanagement.ui.navigation

import android.util.Log
import com.cpen321.usermanagement.data.repository.AuthRepository
import com.cpen321.usermanagement.data.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class NavigationEvent {
    object NavigateToAuth : NavigationEvent()
    object NavigateToMain : NavigationEvent()
    object NavigateToProfileCompletion : NavigationEvent()
    object NavigateToProfile : NavigationEvent()
    object NavigateToManageProfile : NavigationEvent()
    object NavigateToManageHobbies : NavigationEvent()
    data class NavigateToAuthWithMessage(val message: String) : NavigationEvent()
    data class NavigateToMainWithMessage(val message: String) : NavigationEvent()
    object NavigateBack : NavigationEvent()
    object ClearBackStack : NavigationEvent()
    object NoNavigation : NavigationEvent()
}

data class NavigationState(
    val currentRoute: String = NavRoutes.LOADING,
    val isAuthenticated: Boolean = false,
    val needsProfileCompletion: Boolean = false,
    val isLoading: Boolean = true,
    val isNavigating: Boolean = false
)

@Singleton
class NavigationStateManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {

    companion object {
        private const val TAG = "NavigationStateManager"
    }

    // Create a coroutine scope for this singleton
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _navigationEvent = MutableStateFlow<NavigationEvent>(NavigationEvent.NoNavigation)
    val navigationEvent: StateFlow<NavigationEvent> = _navigationEvent.asStateFlow()

    private val _navigationState = MutableStateFlow(NavigationState())

    /**
     * Updates the authentication state and triggers appropriate navigation
     */
    fun updateAuthenticationState(
        isAuthenticated: Boolean,
        needsProfileCompletion: Boolean,
        isLoading: Boolean = false,
        currentRoute: String = _navigationState.value.currentRoute
    ) {
        val newState = _navigationState.value.copy(
            isAuthenticated = isAuthenticated,
            needsProfileCompletion = needsProfileCompletion,
            isLoading = isLoading,
            currentRoute = currentRoute
        )
        _navigationState.value = newState

        // Trigger navigation based on state
        if (!isLoading) {
            handleAuthenticationNavigation(currentRoute, isAuthenticated, needsProfileCompletion)
        }
    }

    /**
     * Handle navigation decisions based on authentication state
     */
    private fun handleAuthenticationNavigation(
        currentRoute: String,
        isAuthenticated: Boolean,
        needsProfileCompletion: Boolean
    ) {
        when {
            // From loading screen after auth check
            currentRoute == NavRoutes.LOADING -> {
                if (isAuthenticated) {
                    if (needsProfileCompletion) {
                        navigateToProfileCompletion()
                    } else {
                        navigateToMain()
                    }
                } else {
                    navigateToAuth()
                }
            }
            // From auth screen after successful login
            currentRoute.startsWith(NavRoutes.AUTH) && isAuthenticated -> {
                if (needsProfileCompletion) {
                    navigateToProfileCompletion()
                } else {
                    navigateToMain()
                }
            }
        }
    }

    /**
     * Navigate to auth screen
     */
    fun navigateToAuth() {
        _navigationEvent.value = NavigationEvent.NavigateToAuth
        _navigationState.value = _navigationState.value.copy(currentRoute = NavRoutes.AUTH)
    }

    /**
     * Navigate to auth screen with success message
     */
    fun navigateToAuthWithMessage(message: String) {
        _navigationEvent.value = NavigationEvent.NavigateToAuthWithMessage(message)
        _navigationState.value = _navigationState.value.copy(currentRoute = NavRoutes.AUTH)
    }

    /**
     * Navigate to main screen
     */
    fun navigateToMain() {
        _navigationEvent.value = NavigationEvent.NavigateToMain
        _navigationState.value = _navigationState.value.copy(currentRoute = NavRoutes.MAIN)
    }

    /**
     * Navigate to main screen with success message
     */
    fun navigateToMainWithMessage(message: String) {
        _navigationEvent.value = NavigationEvent.NavigateToMainWithMessage(message)
        _navigationState.value = _navigationState.value.copy(currentRoute = NavRoutes.MAIN)
    }

    /**
     * Navigate to profile completion screen
     */
    fun navigateToProfileCompletion() {
        _navigationEvent.value = NavigationEvent.NavigateToProfileCompletion
        _navigationState.value =
            _navigationState.value.copy(currentRoute = NavRoutes.PROFILE_COMPLETION)
    }

    /**
     * Navigate to profile screen
     */
    fun navigateToProfile() {
        _navigationEvent.value = NavigationEvent.NavigateToProfile
        _navigationState.value = _navigationState.value.copy(currentRoute = NavRoutes.PROFILE)
    }

    /**
     * Navigate to manage profile screen
     */
    fun navigateToManageProfile() {
        _navigationEvent.value = NavigationEvent.NavigateToManageProfile
        _navigationState.value =
            _navigationState.value.copy(currentRoute = NavRoutes.MANAGE_PROFILE)
    }

    /**
     * Navigate to manage hobbies screen
     */
    fun navigateToManageHobbies() {
        _navigationEvent.value = NavigationEvent.NavigateToManageHobbies
        _navigationState.value =
            _navigationState.value.copy(currentRoute = NavRoutes.MANAGE_HOBBIES)
    }

    /**
     * Navigate back
     */
    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    /**
     * Handle logout
     */
    fun handleLogout() {
        _navigationState.value = _navigationState.value.copy(isNavigating = true)

        scope.launch {
            try {
                authRepository.clearToken()
                Log.d(TAG, "User logged out successfully")
                updateAuthenticationState(
                    isAuthenticated = false,
                    needsProfileCompletion = false,
                    isLoading = false
                )
                navigateToAuthWithMessage("Logged out successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Exception while logging out", e)
                _navigationState.value = _navigationState.value.copy(isNavigating = false)
                // Could add error handling here, for now just reset navigation state
            }
        }
    }

    /**
     * Handle account deletion
     */
    fun handleAccountDeletion() {
        _navigationState.value = _navigationState.value.copy(isNavigating = true)

        scope.launch {
            try {
                val result = profileRepository.deleteAccount()
                if (result.isSuccess) {
                    Log.d(TAG, "Account deleted successfully")
                    updateAuthenticationState(
                        isAuthenticated = false,
                        needsProfileCompletion = false,
                        isLoading = false
                    )
                    navigateToAuthWithMessage("Account deleted successfully!")
                } else {
                    Log.e(TAG, "Failed to delete account", result.exceptionOrNull())
                    _navigationState.value = _navigationState.value.copy(isNavigating = false)
                    // Could add error handling here, for now just reset navigation state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while deleting account", e)
                _navigationState.value = _navigationState.value.copy(isNavigating = false)
                // Could add error handling here, for now just reset navigation state
            }
        }
    }

    /**
     * Handle profile completion
     */
    fun handleProfileCompletion() {
        _navigationState.value = _navigationState.value.copy(needsProfileCompletion = false)
        navigateToMain()
    }

    /**
     * Handle profile completion with success message
     */
    fun handleProfileCompletionWithMessage(message: String) {
        _navigationState.value = _navigationState.value.copy(needsProfileCompletion = false)
        navigateToMainWithMessage(message)
    }

    /**
     * Reset navigation events after handling
     */
    fun clearNavigationEvent() {
        _navigationEvent.value = NavigationEvent.NoNavigation
        // Clear navigating flag when navigation is complete
        _navigationState.value = _navigationState.value.copy(isNavigating = false)
    }
}
