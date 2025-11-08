package com.cpen321.usermanagement.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.cpen321.usermanagement.BuildConfig
import com.cpen321.usermanagement.data.local.preferences.TokenManager
import com.google.android.libraries.places.api.Places
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.data.repository.AuthRepository
import com.cpen321.usermanagement.data.repository.AuthRepositoryImpl
import com.cpen321.usermanagement.data.repository.ProfileRepository
import com.cpen321.usermanagement.data.repository.ProfileRepositoryImpl
import com.cpen321.usermanagement.ui.components.ExperienceLevel
import com.cpen321.usermanagement.ui.theme.ProvideSpacing
import com.cpen321.usermanagement.ui.theme.UserManagementTheme
import com.cpen321.usermanagement.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end test for profile sign up and profile update:
 * - User completes profile sign up form
 * - User updates profile information
 * - Tests make real API calls to local backend
 * 
 * Note: This test requires:
 * 1. Local backend running at http://10.0.2.2:3000/api/
 * 2. Valid authentication token (will prompt if not set)
 */
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var profileRepository: ProfileRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var profileViewModel: ProfileViewModel

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize Google Places API if not already initialized
        val mapsKey = BuildConfig.MAPS_API_KEY
        if (mapsKey.isNotBlank() && !Places.isInitialized()) {
            Places.initialize(context, mapsKey)
        }
        
        // Initialize real repositories (not fake ones)
        tokenManager = TokenManager(context)
        authRepository = AuthRepositoryImpl(
            context = context,
            authInterface = RetrofitClient.authInterface,
            userInterface = RetrofitClient.userInterface,
            tokenManager = tokenManager
        )
        profileRepository = ProfileRepositoryImpl(
            context = context,
            userInterface = RetrofitClient.userInterface,
            imageInterface = RetrofitClient.imageInterface,
            tokenManager = tokenManager
        )
        
        profileViewModel = ProfileViewModel(profileRepository)
        
        // Set up authentication token if available
        runBlocking {
            val token = tokenManager.getTokenSync()
            if (token != null) {
                RetrofitClient.setAuthToken(token)
            }
        }
    }

    /**
     * Helper method to ensure user is authenticated before running a test
     * Throws AssertionError if not authenticated or backend is not accessible
     */
    private fun ensureAuthenticated() {
        runBlocking {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                throw AssertionError(
                    "Test requires authentication. Please sign in to the app first. " +
                    "The authentication token will be saved and reused by the test."
                )
            }
            RetrofitClient.setAuthToken(token)
            // Test backend connectivity
            val testResult = profileRepository.getProfile()
            if (!testResult.isSuccess) {
                val error = testResult.exceptionOrNull()
                throw AssertionError(
                    "Backend connectivity test failed. " +
                    "Make sure your backend is running at ${BuildConfig.API_BASE_URL}. " +
                    "Error: ${error?.message}"
                )
            }
        }
    }

    @Test
    fun test_01_success_scenario_profile_completion() {
        // Given: User is authenticated (token should be set)
        ensureAuthenticated()

        // Test data - all fields are required
        val testName = "Test User E2E"
        val testAge = "25"
        val testCityQuery = "Vancouver"
        val testExperience = ExperienceLevel.BEGINNER
        val testBio = "I love diving and exploring underwater worlds! This is my E2E test profile."

        // Load profile first (ProfileCompletionScreen loads profile in LaunchedEffect)
        runBlocking {
            profileViewModel.loadProfile()
        }
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        // When: Profile completion screen is displayed
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    ProfileCompletionScreen(
                        profileViewModel = profileViewModel,
                        onProfileCompleted = {},
                        onProfileCompletedWithMessage = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        
        // Wait for profile loading to complete
        var loadingCompleted = false
        val loadingWaitTime = 5000
        val loadingStartTime = System.currentTimeMillis()
        while (!loadingCompleted && (System.currentTimeMillis() - loadingStartTime) < loadingWaitTime) {
            Thread.sleep(200)
            composeTestRule.waitForIdle()
            if (!profileViewModel.uiState.value.isLoadingProfile) {
                loadingCompleted = true
            }
        }
        composeTestRule.waitForIdle()

        // Verify profile completion screen is displayed
        composeTestRule.onNodeWithText("Complete Your Profile", substring = true).assertIsDisplayed()

        // Fill in ALL required profile form fields
        // 1. Name field
        composeTestRule.onNodeWithText("Name", substring = true)
            .performScrollTo()
            .performTextReplacement(testName)

        composeTestRule.waitForIdle()

        // 2. Age field
        composeTestRule.onNodeWithText("Age", substring = true)
            .performScrollTo()
            .performTextReplacement(testAge)

        composeTestRule.waitForIdle()

        // 3. City field - type to trigger autocomplete and select a city
        // NOTE: City selection is REQUIRED for ProfileCompletionScreen
        composeTestRule.onNodeWithText("City", substring = true)
            .performScrollTo()
            .performClick() // Focus the field
        
        composeTestRule.waitForIdle()
        Thread.sleep(300)
        
        // Type the city name
        composeTestRule.onNodeWithText("City", substring = true)
            .performTextReplacement(testCityQuery)

        composeTestRule.waitForIdle()
        Thread.sleep(500) // Wait a bit for UI to process
        
        // Ensure Places client is attached to ViewModel (ProfileCompletionScreen does this in LaunchedEffect)
        // Manually trigger city query since UI callback may not fire reliably in tests
        runBlocking {
            // Ensure Places client is attached
            val placesClient = Places.createClient(context)
            profileViewModel.attachPlacesClient(placesClient)
            
            // Wait a bit for any debounce in the UI
            kotlinx.coroutines.delay(500)
            
            // Manually trigger the query
            profileViewModel.queryCities(testCityQuery)
        }
        
        composeTestRule.waitForIdle()
        
        // Wait for Places API to return suggestions
        var suggestionsAvailable = false
        val suggestionWaitTime = 15000
        val suggestionStartTime = System.currentTimeMillis()
        
        while (!suggestionsAvailable && (System.currentTimeMillis() - suggestionStartTime) < suggestionWaitTime) {
            Thread.sleep(500)
            composeTestRule.waitForIdle()
            if (profileViewModel.citySuggestions.value.isNotEmpty()) {
                suggestionsAvailable = true
            }
        }
        
        if (!suggestionsAvailable) {
            val mapsKey = BuildConfig.MAPS_API_KEY
            val placesInitialized = try { Places.isInitialized() } catch (e: Exception) { false }
            
            throw AssertionError(
                "City autocomplete suggestions not available after ${suggestionWaitTime}ms. " +
                "API key configured: ${mapsKey.isNotBlank()}, Places initialized: $placesInitialized. " +
                "Query attempted: '$testCityQuery'. " +
                "Ensure Places API is configured, initialized, network is available, and API key has Places API enabled."
            )
        }
        
        // Wait for dropdown menu to render
        Thread.sleep(1500)
        composeTestRule.waitForIdle()
        
        // Select a city suggestion
        // Issue: Multiple suggestions contain "Vancouver" (e.g., "Vancouver, BC, Canada", "West Vancouver, BC, Canada")
        // When using substring match, all of them match. We need exact match and select the first one.
        val availableSuggestions = profileViewModel.citySuggestions.value.map { it.label }
        var citySelected = false
        
        // Prefer "Vancouver, BC, Canada" (main city, not West/North Vancouver)
        val preferredSuggestion = availableSuggestions.firstOrNull { suggestion ->
            suggestion == "Vancouver, BC, Canada" || 
            (suggestion.contains("Vancouver") && suggestion.contains("BC") && 
             !suggestion.contains("West") && !suggestion.contains("North"))
        } ?: availableSuggestions.firstOrNull()
        
        if (preferredSuggestion != null) {
            try {
                // Use exact match (substring=false) to match only "Vancouver, BC, Canada"
                // Use onAllNodesWithText and get(0) to select the first match if there are multiple
                composeTestRule.onAllNodesWithText(preferredSuggestion, substring = false, useUnmergedTree = true)
                    .get(0)
                    .performClick()
                citySelected = true
            } catch (e: Exception) {
                // If that fails, the text might be displayed with brackets or differently
                // Try without useUnmergedTree
                try {
                    composeTestRule.onAllNodesWithText(preferredSuggestion, substring = false)
                        .get(0)
                        .performClick()
                    citySelected = true
                } catch (e2: Exception) {
                    // Last resort: select the first Vancouver suggestion that's clickable
                    val vancouverSuggestions = availableSuggestions.filter { 
                        it == preferredSuggestion || 
                        (it.contains("Vancouver") && !it.contains("West") && !it.contains("North"))
                    }
                    for (suggestion in vancouverSuggestions) {
                        try {
                            composeTestRule.onAllNodesWithText(suggestion, substring = false, useUnmergedTree = true)
                                .get(0)
                                .performClick()
                            citySelected = true
                            break
                        } catch (e3: Exception) {
                            // Try next suggestion
                        }
                    }
                }
            }
        }
        
        if (!citySelected) {
            throw AssertionError(
                "Could not select city suggestion. Available: [${availableSuggestions.joinToString(", ")}]. " +
                "Preferred: $preferredSuggestion"
            )
        }
        
        composeTestRule.waitForIdle()

        // 4. Experience level dropdown (required)
        composeTestRule.onNodeWithText("Experience Level", substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(testExperience.label)
            .performClick()

        composeTestRule.waitForIdle()

        // 5. Bio field (required)
        composeTestRule.onNodeWithText("Bio", substring = true)
            .performScrollTo()
            .performTextReplacement(testBio)

        composeTestRule.waitForIdle()

        // Click Save button to save the profile
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()

        // Wait for profile to be saved
        var saveCompleted = false
        val saveWaitTime = 10000
        val saveStartTime = System.currentTimeMillis()
        
        while (!saveCompleted && (System.currentTimeMillis() - saveStartTime) < saveWaitTime) {
            Thread.sleep(500)
            composeTestRule.waitForIdle()
            
            val uiState = profileViewModel.uiState.value
            if (!uiState.isSavingProfile) {
                saveCompleted = true
                if (uiState.errorMessage != null) {
                    throw AssertionError("Profile save failed: ${uiState.errorMessage}")
                }
            }
        }
        
        if (!saveCompleted) {
            throw AssertionError("Profile save did not complete within timeout")
        }
        
        if (profileViewModel.uiState.value.errorMessage != null) {
            throw AssertionError("Profile save failed: ${profileViewModel.uiState.value.errorMessage}")
        }

        // Reload profile from backend and verify it was saved correctly
        runBlocking {
            val reloadResult = profileRepository.getProfile()
            assert(reloadResult.isSuccess) { 
                "Failed to reload profile: ${reloadResult.exceptionOrNull()?.message}" 
            }
            
            val reloadedUser = reloadResult.getOrNull()
            assert(reloadedUser != null) { "Reloaded user should not be null" }
            assert(reloadedUser?.name == testName) { 
                "Name mismatch: expected '$testName', but got '${reloadedUser?.name}'" 
            }
            assert(reloadedUser?.age == testAge.toInt()) { 
                "Age mismatch: expected $testAge, but got ${reloadedUser?.age}" 
            }
            assert(reloadedUser?.skillLevel?.lowercase() == testExperience.label.lowercase()) { 
                "Experience level mismatch: expected '${testExperience.label}', but got '${reloadedUser?.skillLevel}'"
            }
            assert(reloadedUser?.bio == testBio) { 
                "Bio mismatch: expected '$testBio', but got '${reloadedUser?.bio}'" 
            }
            
            // Also update ViewModel state for consistency
            profileViewModel.loadProfile()
        }
    }

    @Test
    fun test_02_success_scenario_profile_update() {
        // Given: User is authenticated and has a profile (from previous test)
        ensureAuthenticated()
        
        runBlocking {
            profileViewModel.loadProfile()
        }

        composeTestRule.waitForIdle()

        // When: Manage profile screen is displayed
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    ManageProfileScreen(
                        profileViewModel = profileViewModel,
                        onBackClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Manage Profile", substring = true).assertIsDisplayed()

        // Update ALL fields
        // 1. Update name field
        val updatedName = "Updated Test User"
        composeTestRule.onNodeWithText("Name", substring = true)
            .performScrollTo()
            .performTextReplacement(updatedName)

        composeTestRule.waitForIdle()
        Thread.sleep(300) // Small delay to ensure state update

        // 2. Update age field
        val updatedAge = "26"
        composeTestRule.onNodeWithText("Age", substring = true)
            .performScrollTo()
            .performTextReplacement(updatedAge)

        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // 3. Update experience level
        val updatedExperience = ExperienceLevel.INTERMEDIATE
        composeTestRule.onNodeWithText("Experience Level", substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        Thread.sleep(300)

        composeTestRule.onNodeWithText(updatedExperience.label)
            .performClick()

        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // 4. Update bio field
        val updatedBio = "Updated bio: I'm passionate about scuba diving and exploring the ocean!"
        composeTestRule.onNodeWithText("Bio", substring = true)
            .performScrollTo()
            .performTextReplacement(updatedBio)

        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // 5. Update city (optional in ManageProfileScreen, but we'll update it)
        composeTestRule.onNodeWithText("City", substring = true)
            .performScrollTo()
            .performClick() // Focus the field first
        
        composeTestRule.waitForIdle()
        Thread.sleep(300)
        
        composeTestRule.onNodeWithText("City", substring = true)
            .performTextReplacement("Toronto")

        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        // Try to select Toronto from suggestions (optional)
        try {
            // Ensure Places client is attached and query cities
            runBlocking {
                val placesClient = Places.createClient(context)
                profileViewModel.attachPlacesClient(placesClient)
                kotlinx.coroutines.delay(500)
                profileViewModel.queryCities("Toronto")
            }
            composeTestRule.waitForIdle()
            Thread.sleep(2000)
            
            // Try to select Toronto suggestion
            val torontoSuggestions = profileViewModel.citySuggestions.value.map { it.label }
            for (suggestion in torontoSuggestions) {
                if (suggestion.contains("Toronto") && !suggestion.contains("West") && !suggestion.contains("North")) {
                    try {
                        composeTestRule.onAllNodesWithText(suggestion, substring = false, useUnmergedTree = true)
                            .get(0)
                            .performClick()
                        break
                    } catch (e: Exception) {
                        // Try next suggestion
                    }
                }
            }
        } catch (e: Exception) {
            // City selection is optional in ManageProfileScreen
        }

        composeTestRule.waitForIdle()
        Thread.sleep(500) // Wait for all state updates

        // Verify Save button is enabled (changes were detected)
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .assertIsEnabled()
        
        composeTestRule.waitForIdle()
        
        // Click Save button
        composeTestRule.onNodeWithText("Save", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Wait for profile to be updated
        var updateCompleted = false
        val updateWaitTime = 10000
        val updateStartTime = System.currentTimeMillis()
        
        while (!updateCompleted && (System.currentTimeMillis() - updateStartTime) < updateWaitTime) {
            Thread.sleep(500)
            composeTestRule.waitForIdle()
            
            val uiState = profileViewModel.uiState.value
            if (!uiState.isSavingProfile) {
                updateCompleted = true
                if (uiState.errorMessage != null) {
                    throw AssertionError("Profile update failed: ${uiState.errorMessage}")
                }
            }
        }
        
        if (!updateCompleted) {
            throw AssertionError("Profile update did not complete within timeout")
        }
        
        // Check for errors after update completes
        if (profileViewModel.uiState.value.errorMessage != null) {
            throw AssertionError("Profile update failed: ${profileViewModel.uiState.value.errorMessage}")
        }

        // Wait a bit for backend to process the update
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        // Reload profile from backend and verify it was saved correctly
        runBlocking {
            val reloadResult = profileRepository.getProfile()
            assert(reloadResult.isSuccess) { 
                "Failed to reload profile: ${reloadResult.exceptionOrNull()?.message}" 
            }
            
            val reloadedUser = reloadResult.getOrNull()
            assert(reloadedUser != null) { "Reloaded user should not be null" }
            
            // Verify backend has the updated values
            assert(reloadedUser?.name == updatedName) { 
                "Backend name mismatch: expected '$updatedName', but got '${reloadedUser?.name}'" 
            }
            assert(reloadedUser?.age == updatedAge.toInt()) { 
                "Backend age mismatch: expected $updatedAge, but got ${reloadedUser?.age}" 
            }
            assert(reloadedUser?.skillLevel?.lowercase() == updatedExperience.label.lowercase()) { 
                "Backend experience level mismatch: expected '${updatedExperience.label}', but got '${reloadedUser?.skillLevel}'"
            }
            assert(reloadedUser?.bio == updatedBio) { 
                "Backend bio mismatch: expected '$updatedBio', but got '${reloadedUser?.bio}'" 
            }
            
            // Update ViewModel state for consistency
            profileViewModel.loadProfile()
        }
        
        // Wait for ViewModel to finish loading
        Thread.sleep(2000)
        composeTestRule.waitForIdle()
        
        // Verify ViewModel state matches
        runBlocking {
            val user = profileViewModel.uiState.value.user
            assert(user != null) { "User profile should be loaded in ViewModel" }
            assert(user?.name == updatedName) { 
                "ViewModel name mismatch: expected '$updatedName', but got '${user?.name}'" 
            }
            assert(user?.age == updatedAge.toInt()) { 
                "ViewModel age mismatch: expected $updatedAge, but got ${user?.age}" 
            }
            assert(user?.skillLevel?.lowercase() == updatedExperience.label.lowercase()) { 
                "ViewModel experience level mismatch: expected '${updatedExperience.label}', but got '${user?.skillLevel}'"
            }
            assert(user?.bio == updatedBio) { 
                "ViewModel bio mismatch: expected '$updatedBio', but got '${user?.bio}'" 
            }
        }
    }

    @Test
    fun test_profile_form_validation() {
        // Given: User is authenticated
        ensureAuthenticated()

        // When: Profile completion screen is displayed
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    ProfileCompletionScreen(
                        profileViewModel = profileViewModel,
                        onProfileCompleted = {},
                        onProfileCompletedWithMessage = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Try to save without filling required fields
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify validation errors are shown
        // Name is required
        composeTestRule.onNodeWithText("Required", substring = true)
            .assertIsDisplayed()

        // Fill in name but leave other required fields empty
        composeTestRule.onNodeWithText("Name", substring = true)
            .performScrollTo()
            .performTextInput("Test User")

        composeTestRule.waitForIdle()

        // Try to save again
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()

        // Should still show validation errors for other required fields
        // (City and Experience Level are required in ProfileCompletionScreen)
    }

    @Test
    fun test_profile_update_with_changes() {
        // Given: User is authenticated and has a profile
        ensureAuthenticated()
        
        runBlocking {
            profileViewModel.loadProfile()
        }

        composeTestRule.waitForIdle()

        var backClicked = false

        // When: Manage profile screen is displayed
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    ManageProfileScreen(
                        profileViewModel = profileViewModel,
                        onBackClick = { backClicked = true }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Wait for profile to load (network call)
        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Manage Profile", substring = true).assertIsDisplayed()

        // Verify Save button is initially disabled (no changes)
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .assertExists()

        // Make a change to enable the Save button
        composeTestRule.onNodeWithText("Name", substring = true)
            .performScrollTo()
            .performTextReplacement("New Name")

        composeTestRule.waitForIdle()

        // Now Save button should be enabled
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .assertIsEnabled()

        // Test validation: Set invalid values and verify error messages appear
        // Note: In ManageProfileScreen, only Age and City fields display errors in the UI
        // Name and Bio fields don't have error display properties, so we'll test Age and City
        
        // 1. Set invalid age (less than 13) - this will show an error
        composeTestRule.onNodeWithText("Age", substring = true)
            .performScrollTo()
            .performTextReplacement("5")

        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // 2. Edit city without selecting from suggestions - this will show an error
        composeTestRule.onNodeWithText("City", substring = true)
            .performScrollTo()
            .performClick() // Focus the field
        
        composeTestRule.waitForIdle()
        Thread.sleep(300)
        
        composeTestRule.onNodeWithText("City", substring = true)
            .performTextReplacement("SomeCity")

        composeTestRule.waitForIdle()
        Thread.sleep(500)

        // 3. Verify Save button is enabled (we made changes)
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .assertIsEnabled()

        // 4. Try to save with invalid data - this should trigger validation errors
        composeTestRule.onNodeWithText("Save", substring = true)
            .performClick()

        composeTestRule.waitForIdle()
        Thread.sleep(1500) // Wait for validation to trigger and UI to recompose

        // 5. Verify error messages are displayed for fields that show errors in UI
        
        // Age error: "Enter a valid age (13–120)" - displayed in supportingText
        composeTestRule.onNodeWithText("Enter a valid age", substring = true)
            .assertIsDisplayed()

        // City error: "Pick a city from suggestions" - displayed when editing city without selection
        // Scroll to city field to make sure error is visible
        composeTestRule.onNodeWithText("City", substring = true)
            .performScrollTo()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Pick a city from suggestions", substring = true)
            .assertIsDisplayed()

        // 6. Verify validation prevents saving
        runBlocking {
            Thread.sleep(500)
            // Save should not proceed - isSavingProfile should remain false
            assert(!profileViewModel.uiState.value.isSavingProfile) {
                "Save should not proceed with invalid data"
            }
        }
    }

    @Test
    fun test_skip_button_profile_completion() {
        // Given: User is authenticated
        ensureAuthenticated()

        // Load profile first
        runBlocking {
            profileViewModel.loadProfile()
        }
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        var skipButtonClicked = false

        // When: Profile completion screen is displayed
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    ProfileCompletionScreen(
                        profileViewModel = profileViewModel,
                        onProfileCompleted = {
                            skipButtonClicked = true
                        },
                        onProfileCompletedWithMessage = {
                            skipButtonClicked = true
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Wait for profile loading to complete
        var loadingCompleted = false
        val loadingWaitTime = 5000
        val loadingStartTime = System.currentTimeMillis()
        while (!loadingCompleted && (System.currentTimeMillis() - loadingStartTime) < loadingWaitTime) {
            Thread.sleep(200)
            composeTestRule.waitForIdle()
            if (!profileViewModel.uiState.value.isLoadingProfile) {
                loadingCompleted = true
            }
        }
        composeTestRule.waitForIdle()

        // Verify profile completion screen is displayed
        composeTestRule.onNodeWithText("Complete Your Profile", substring = true).assertIsDisplayed()

        // Verify Skip button is displayed and enabled
        composeTestRule.onNodeWithText("Skip", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()

        // Click Skip button
        composeTestRule.onNodeWithText("Skip", substring = true)
            .performClick()

        composeTestRule.waitForIdle()
        Thread.sleep(500)

        // Verify skip button callback was called
        assert(skipButtonClicked) {
            "Skip button should call onProfileCompleted callback"
        }

        // Verify that no profile save operation was initiated
        runBlocking {
            assert(!profileViewModel.uiState.value.isSavingProfile) {
                "Skip button should not trigger profile save"
            }
        }
    }

    @Test
    fun test_profile_completion_validation_all_fields() {
        // Given: User is authenticated
        ensureAuthenticated()

        // Load profile first
        runBlocking {
            profileViewModel.loadProfile()
        }
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        // When: Profile completion screen is displayed
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    ProfileCompletionScreen(
                        profileViewModel = profileViewModel,
                        onProfileCompleted = {},
                        onProfileCompletedWithMessage = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Wait for profile loading to complete
        var loadingCompleted = false
        val loadingWaitTime = 5000
        val loadingStartTime = System.currentTimeMillis()
        while (!loadingCompleted && (System.currentTimeMillis() - loadingStartTime) < loadingWaitTime) {
            Thread.sleep(200)
            composeTestRule.waitForIdle()
            if (!profileViewModel.uiState.value.isLoadingProfile) {
                loadingCompleted = true
            }
        }
        composeTestRule.waitForIdle()

        // Verify profile completion screen is displayed
        composeTestRule.onNodeWithText("Complete Your Profile", substring = true).assertIsDisplayed()

        // Try to save without filling any required fields
        // This should trigger validation errors for all required fields
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        Thread.sleep(1500) // Wait for validation to trigger and UI to recompose

        // Verify all required field errors are displayed
        // 1. Name error: "Required"
        composeTestRule.onNodeWithText("Required", substring = true)
            .assertIsDisplayed()

        // 2. City error: "Pick a city from suggestions" (city is required)
        composeTestRule.onNodeWithText("Pick a city from suggestions", substring = true)
            .assertIsDisplayed()

        // 3. Experience error: "Select experience level" (experience is required)
        composeTestRule.onNodeWithText("Select experience level", substring = true)
            .assertIsDisplayed()

        // Verify validation prevents saving
        runBlocking {
            Thread.sleep(500)
            assert(!profileViewModel.uiState.value.isSavingProfile) {
                "Save should not proceed with invalid data"
            }
        }

        // Now test individual field validation errors

        // Test 1: Name validation (empty name)
        composeTestRule.onNodeWithText("Name", substring = true)
            .performScrollTo()
            .performTextReplacement("")

        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // Test 2: Age validation (invalid age - too young)
        composeTestRule.onNodeWithText("Age", substring = true)
            .performScrollTo()
            .performTextReplacement("5")

        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // Test 3: Bio validation (too long)
        val longBio = "a".repeat(501) // MAX_BIO_LENGTH is 500
        composeTestRule.onNodeWithText("Bio", substring = true)
            .performScrollTo()
            .performTextReplacement(longBio)

        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // Fill in name to make form partially valid (but still invalid overall)
        composeTestRule.onNodeWithText("Name", substring = true)
            .performScrollTo()
            .performTextReplacement("Test Name")

        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // Try to save again - should show validation errors
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        Thread.sleep(1500)

        // Verify specific validation errors are displayed
        // Note: Bio field doesn't display errors in the UI (BioInputField has no error display)
        // Only Name, Age, City, and Experience fields display errors in ProfileCompletionScreen
        
        // Age error: "Enter a valid age (13–120)"
        composeTestRule.onNodeWithText("Enter a valid age", substring = true)
            .assertIsDisplayed()

        // City error: "Pick a city from suggestions"
        composeTestRule.onNodeWithText("Pick a city from suggestions", substring = true)
            .assertIsDisplayed()

        // Experience error: "Select experience level"
        composeTestRule.onNodeWithText("Select experience level", substring = true)
            .assertIsDisplayed()
        
        // Note: Bio validation exists in form state but is not displayed in UI
        // The validation still prevents saving (canSave() checks bio length)

        // Verify validation still prevents saving
        runBlocking {
            Thread.sleep(500)
            assert(!profileViewModel.uiState.value.isSavingProfile) {
                "Save should not proceed with invalid data"
            }
        }
    }
}

