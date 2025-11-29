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
import com.cpen321.usermanagement.ui.screens.profile.ManageProfileScreen
import com.cpen321.usermanagement.ui.screens.profile.ProfileCompletionScreen
import com.cpen321.usermanagement.ui.theme.ProvideSpacing
import com.cpen321.usermanagement.ui.theme.UserManagementTheme
import com.cpen321.usermanagement.ui.viewmodels.profile.ProfileViewModel
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

    private fun waitForProfileLoading(maxWaitTime: Long = 5000) {
        var loadingCompleted = false
        val startTime = System.currentTimeMillis()
        while (!loadingCompleted && (System.currentTimeMillis() - startTime) < maxWaitTime) {
            Thread.sleep(200)
            composeTestRule.waitForIdle()
            if (!profileViewModel.uiState.value.isLoadingProfile) {
                loadingCompleted = true
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun setupProfileCompletionScreen() {
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
    }

    private fun setupManageProfileScreen(onBackClick: () -> Unit = {}) {
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    ManageProfileScreen(
                        profileViewModel = profileViewModel,
                        onBackClick = onBackClick
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun fillNameField(name: String) {
        composeTestRule.onNodeWithText("Name", substring = true)
            .performScrollTo()
            .performTextReplacement(name)
        composeTestRule.waitForIdle()
    }

    private fun fillAgeField(age: String) {
        composeTestRule.onNodeWithText("Age", substring = true)
            .performScrollTo()
            .performTextReplacement(age)
        composeTestRule.waitForIdle()
    }

    private fun fillBioField(bio: String) {
        composeTestRule.onNodeWithText("Bio", substring = true)
            .performScrollTo()
            .performTextReplacement(bio)
        composeTestRule.waitForIdle()
    }

    private fun selectExperienceLevel(experience: ExperienceLevel) {
        composeTestRule.onNodeWithText("Experience Level", substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(experience.label)
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun selectCityFromSuggestions(cityQuery: String, preferredCity: String? = null) {
        composeTestRule.onNodeWithText("City", substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(300)

        composeTestRule.onNodeWithText("City", substring = true)
            .performTextReplacement(cityQuery)
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        runBlocking {
            val placesClient = Places.createClient(context)
            profileViewModel.attachPlacesClient(placesClient)
            kotlinx.coroutines.delay(500)
            profileViewModel.queryCities(cityQuery)
        }
        composeTestRule.waitForIdle()

        waitForCitySuggestions()

        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        val availableSuggestions = profileViewModel.citySuggestions.value.map { it.label }
        val targetSuggestion = preferredCity ?: findPreferredCitySuggestion(availableSuggestions, cityQuery)
        
        if (targetSuggestion != null) {
            clickCitySuggestion(targetSuggestion, availableSuggestions)
        } else {
            throw AssertionError(
                "Could not select city suggestion. Available: [${availableSuggestions.joinToString(", ")}]"
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun waitForCitySuggestions(maxWaitTime: Long = 15000) {
        var suggestionsAvailable = false
        val startTime = System.currentTimeMillis()
        while (!suggestionsAvailable && (System.currentTimeMillis() - startTime) < maxWaitTime) {
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
                "City autocomplete suggestions not available after ${maxWaitTime}ms. " +
                "API key configured: ${mapsKey.isNotBlank()}, Places initialized: $placesInitialized. " +
                "Ensure Places API is configured, initialized, network is available, and API key has Places API enabled."
            )
        }
    }

    private fun findPreferredCitySuggestion(suggestions: List<String>, query: String): String? {
        return suggestions.firstOrNull { suggestion ->
            suggestion.contains(query, ignoreCase = true) &&
            suggestion.contains("BC") &&
            !suggestion.contains("West") &&
            !suggestion.contains("North")
        } ?: suggestions.firstOrNull()
    }

    private fun clickCitySuggestion(suggestion: String, allSuggestions: List<String>) {
        if (tryClickSuggestionWithUnmergedTree(suggestion)) {
            return
        }
        if (tryClickSuggestion(suggestion)) {
            return
        }
        tryClickAlternativeSuggestions(suggestion, allSuggestions)
    }

    private fun tryClickSuggestionWithUnmergedTree(suggestion: String): Boolean {
        return try {
            composeTestRule.onAllNodesWithText(suggestion, substring = false, useUnmergedTree = true)
                .get(0)
                .performClick()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun tryClickSuggestion(suggestion: String): Boolean {
        return try {
            composeTestRule.onAllNodesWithText(suggestion, substring = false)
                .get(0)
                .performClick()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun tryClickAlternativeSuggestions(suggestion: String, allSuggestions: List<String>) {
        val filteredSuggestions = allSuggestions.filter {
            it == suggestion || (it.contains(suggestion.split(",").first()) &&
                !it.contains("West") && !it.contains("North"))
        }
        for (altSuggestion in filteredSuggestions) {
            if (tryClickSuggestionWithUnmergedTree(altSuggestion)) {
                return
            }
        }
        throw AssertionError("Could not click any city suggestion")
    }

    private fun waitForSaveToComplete(maxWaitTime: Long = 10000) {
        var saveCompleted = false
        val startTime = System.currentTimeMillis()
        while (!saveCompleted && (System.currentTimeMillis() - startTime) < maxWaitTime) {
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
    }

    private fun verifySavedProfile(
        expectedName: String,
        expectedAge: Int,
        expectedExperience: ExperienceLevel,
        expectedBio: String
    ) {
        runBlocking {
            val reloadResult = profileRepository.getProfile()
            assert(reloadResult.isSuccess) {
                "Failed to reload profile: ${reloadResult.exceptionOrNull()?.message}"
            }

            val reloadedUser = reloadResult.getOrNull()
            assert(reloadedUser != null) { "Reloaded user should not be null" }
            assert(reloadedUser?.name == expectedName) {
                "Name mismatch: expected '$expectedName', but got '${reloadedUser?.name}'"
            }
            assert(reloadedUser?.age == expectedAge) {
                "Age mismatch: expected $expectedAge, but got ${reloadedUser?.age}"
            }
            assert(reloadedUser?.skillLevel?.lowercase() == expectedExperience.label.lowercase()) {
                "Experience level mismatch: expected '${expectedExperience.label}', but got '${reloadedUser?.skillLevel}'"
            }
            assert(reloadedUser?.bio == expectedBio) {
                "Bio mismatch: expected '$expectedBio', but got '${reloadedUser?.bio}'"
            }
            profileViewModel.loadProfile()
        }
    }

    private fun clickSaveButton() {
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_01_success_scenario_profile_completion() {
        ensureAuthenticated()

        val testName = "Test User E2E"
        val testAge = "25"
        val testCityQuery = "Vancouver"
        val testExperience = ExperienceLevel.BEGINNER
        val testBio = "I love diving and exploring underwater worlds! This is my E2E test profile."

        runBlocking {
            profileViewModel.loadProfile()
        }
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        setupProfileCompletionScreen()
        waitForProfileLoading()

        composeTestRule.onNodeWithText("Complete Your Profile", substring = true).assertIsDisplayed()

        fillNameField(testName)
        fillAgeField(testAge)
        selectCityFromSuggestions(testCityQuery, "Vancouver, BC, Canada")
        selectExperienceLevel(testExperience)
        fillBioField(testBio)
        clickSaveButton()
        waitForSaveToComplete()
        verifySavedProfile(testName, testAge.toInt(), testExperience, testBio)
    }

    @Test
    fun test_02_success_scenario_profile_update() {
        ensureAuthenticated()

        runBlocking {
            profileViewModel.loadProfile()
        }
        composeTestRule.waitForIdle()

        setupManageProfileScreen()
        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Manage Profile", substring = true).assertIsDisplayed()

        val updatedName = "Updated Test User"
        val updatedAge = "26"
        val updatedExperience = ExperienceLevel.INTERMEDIATE
        val updatedBio = "Updated bio: I'm passionate about scuba diving and exploring the ocean!"

        fillNameField(updatedName)
        Thread.sleep(300)
        fillAgeField(updatedAge)
        Thread.sleep(300)
        selectExperienceLevel(updatedExperience)
        Thread.sleep(300)
        fillBioField(updatedBio)
        Thread.sleep(300)

        try {
            selectCityFromSuggestions("Toronto", "Toronto, ON, Canada")
        } catch (e: Exception) {
            // City selection is optional in ManageProfileScreen
        }

        Thread.sleep(500)
        clickSaveButton()
        waitForSaveToComplete()

        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        verifySavedProfile(updatedName, updatedAge.toInt(), updatedExperience, updatedBio)

        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        verifyViewModelState(updatedName, updatedAge.toInt(), updatedExperience, updatedBio)
    }

    private fun verifyViewModelState(
        expectedName: String,
        expectedAge: Int,
        expectedExperience: ExperienceLevel,
        expectedBio: String
    ) {
        runBlocking {
            val user = profileViewModel.uiState.value.user
            assert(user != null) { "User profile should be loaded in ViewModel" }
            assert(user?.name == expectedName) {
                "ViewModel name mismatch: expected '$expectedName', but got '${user?.name}'"
            }
            assert(user?.age == expectedAge) {
                "ViewModel age mismatch: expected $expectedAge, but got ${user?.age}"
            }
            assert(user?.skillLevel?.lowercase() == expectedExperience.label.lowercase()) {
                "ViewModel experience level mismatch: expected '${expectedExperience.label}', but got '${user?.skillLevel}'"
            }
            assert(user?.bio == expectedBio) {
                "ViewModel bio mismatch: expected '$expectedBio', but got '${user?.bio}'"
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
        ensureAuthenticated()

        runBlocking {
            profileViewModel.loadProfile()
        }
        composeTestRule.waitForIdle()

        setupManageProfileScreen()
        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Manage Profile", substring = true).assertIsDisplayed()

        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .assertExists()

        fillNameField("New Name")
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .assertIsEnabled()

        setInvalidAge("5")
        setInvalidCity("SomeCity")

        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()
        Thread.sleep(1500)

        verifyValidationErrors()

        runBlocking {
            Thread.sleep(500)
            assert(!profileViewModel.uiState.value.isSavingProfile) {
                "Save should not proceed with invalid data"
            }
        }
    }

    private fun setInvalidAge(age: String) {
        composeTestRule.onNodeWithText("Age", substring = true)
            .performScrollTo()
            .performTextReplacement(age)
        composeTestRule.waitForIdle()
        Thread.sleep(300)
    }

    private fun setInvalidCity(city: String) {
        composeTestRule.onNodeWithText("City", substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(300)
        composeTestRule.onNodeWithText("City", substring = true)
            .performTextReplacement(city)
        composeTestRule.waitForIdle()
        Thread.sleep(500)
    }

    private fun verifyValidationErrors() {
        composeTestRule.onNodeWithText("Enter a valid age", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("City", substring = true)
            .performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Pick a city from suggestions", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun test_profile_completion_validation_all_fields() {
        ensureAuthenticated()

        runBlocking {
            profileViewModel.loadProfile()
        }
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        setupProfileCompletionScreen()
        waitForProfileLoading()

        composeTestRule.onNodeWithText("Complete Your Profile", substring = true).assertIsDisplayed()

        verifyEmptyFieldsValidation()

        setInvalidFieldsForValidation()

        verifyInvalidFieldsValidation()
    }

    private fun verifyEmptyFieldsValidation() {
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        Thread.sleep(1500)

        composeTestRule.onNodeWithText("Required", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Pick a city from suggestions", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Select experience level", substring = true)
            .assertIsDisplayed()

        runBlocking {
            Thread.sleep(500)
            assert(!profileViewModel.uiState.value.isSavingProfile) {
                "Save should not proceed with invalid data"
            }
        }
    }

    private fun setInvalidFieldsForValidation() {
        fillNameField("")
        Thread.sleep(300)
        setInvalidAge("5")
        val longBio = "a".repeat(501)
        fillBioField(longBio)
        Thread.sleep(300)
        fillNameField("Test Name")
        Thread.sleep(300)
    }

    private fun verifyInvalidFieldsValidation() {
        composeTestRule.onNodeWithText("Save", substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        Thread.sleep(1500)

        composeTestRule.onNodeWithText("Enter a valid age", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Pick a city from suggestions", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Select experience level", substring = true)
            .assertIsDisplayed()

        runBlocking {
            Thread.sleep(500)
            assert(!profileViewModel.uiState.value.isSavingProfile) {
                "Save should not proceed with invalid data"
            }
        }
    }
}

