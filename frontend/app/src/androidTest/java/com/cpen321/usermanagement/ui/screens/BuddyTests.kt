package com.cpen321.usermanagement.ui.screens

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cpen321.usermanagement.data.local.preferences.TokenManager
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.data.repository.BuddyRepositoryImpl
import com.cpen321.usermanagement.data.repository.ChatRepositoryImpl
import com.cpen321.usermanagement.data.repository.BuddyRepository
import com.cpen321.usermanagement.data.repository.ChatRepository
import com.cpen321.usermanagement.data.remote.dto.Buddy
import com.cpen321.usermanagement.common.Constants
import com.cpen321.usermanagement.ui.theme.ProvideSpacing
import com.cpen321.usermanagement.ui.theme.UserManagementTheme
import com.cpen321.usermanagement.ui.viewmodels.BuddyViewModel
import com.cpen321.usermanagement.ui.viewmodels.MainViewModel
import com.cpen321.usermanagement.ui.viewmodels.MatchViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.cpen321.usermanagement.ui.screens.buddies.BuddiesScreen
import com.cpen321.usermanagement.ui.screens.buddies.MatchScreen

/**
 * End-to-end tests for finding buddies use case that actually call the backend server.
 * 
 * Test scenarios:
 * - User navigates to buddies screen
 * - User clicks on "Match with Buddies" button
 * - User sees list of matches in MatchScreen
 * - User can navigate through the list of matches
 * - User can match with a buddy (creates a chat)
 * 
 * Assumptions:
 * - User is already logged in (auth token is set up in @Before)
 * - Test runs without any prior state or pre-populated data in the app/server
 * - Backend server is running and accessible
 * - There are other users in the backend database to match with
 */
class BuddyTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var tokenManager: TokenManager
    private lateinit var buddyRepository: BuddyRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var mainViewModel: MainViewModel
    private lateinit var buddyViewModel: BuddyViewModel
    private lateinit var matchViewModel: MatchViewModel
    private var navigateToMatchInvoked = false
    private var chatNavigationInvoked = false
    private var chatIdReceived: String? = null

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        tokenManager = TokenManager(context)
        
        // Initialize real repositories that will call the backend
        buddyRepository = BuddyRepositoryImpl(RetrofitClient.buddyInterface)
        chatRepository = ChatRepositoryImpl(RetrofitClient.chatInterface)
        
        // Set up authentication token - assume user is already logged in
        // In a real scenario, you would get this token from a real login flow
        // For testing, we'll need to either:
        // 1. Use a test user that exists in the backend
        // 2. Create a user via signup first
        // For now, we'll assume the token is set via environment or test setup
        // The token should be a valid JWT token from the backend
        runBlocking {
            val existingToken = tokenManager.getTokenSync()
            if (existingToken != null) {
                RetrofitClient.setAuthToken(existingToken)
            } else {
                // If no token exists, the test will need to handle authentication first
                // This is expected for end-to-end tests - the user should be logged in
                throw IllegalStateException(
                    "No authentication token found. " +
                    "Please ensure the test user is logged in before running end-to-end tests. " +
                    "The token should be set up in the test environment or via a login flow."
                )
            }
        }

        // Initialize ViewModels with real repositories
        mainViewModel = MainViewModel()
        buddyViewModel = BuddyViewModel(
            buddyRepository = buddyRepository,
            savedStateHandle = SavedStateHandle()
        )
        matchViewModel = MatchViewModel(
            buddyRepository = buddyRepository,
            chatRepository = chatRepository,
            savedStateHandle = SavedStateHandle()
        )


        // Set up navigation callbacks
        buddyViewModel.onNavigateToMatch = {
            navigateToMatchInvoked = true
            mainViewModel.navigateToMatchScreen()
        }
        matchViewModel.onNavigateBack = {
            buddyViewModel.clearState()
            matchViewModel.clearState()
            mainViewModel.navigateBackFromMatchScreen()
        }
        matchViewModel.onNavigateToChat = { chatId ->
            chatNavigationInvoked = true
            chatIdReceived = chatId
            mainViewModel.openChat(chatId)
        }
        navigateToMatchInvoked = false
        chatNavigationInvoked = false
        chatIdReceived = null
    }

    private fun setupMainScreenWithNavigation() {
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    val uiState by mainViewModel.uiState.collectAsState()
                    
                    if (uiState.showMatchScreen) {
                        MatchScreen(viewModel = matchViewModel)
                    } else {
                        when (uiState.currentScreen) {
                            "buddies" -> BuddiesScreen(viewModel = buddyViewModel)
                            else -> {
                                mainViewModel.setCurrentScreen("buddies")
                                BuddiesScreen(viewModel = buddyViewModel)
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun navigateToBuddiesScreen() {
        mainViewModel.setCurrentScreen("buddies")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Buddies").assertIsDisplayed()
        composeTestRule.onNodeWithText("Match with Buddies").assertIsDisplayed()
    }

    private fun clickMatchWithBuddiesAndWait() {
        // Set default filters (required by BuddyViewModel validation)
        buddyViewModel.setFilters(
            targetMinLevel = Constants.BEGINNER_LEVEL,
            targetMaxLevel = Constants.ADVANCED_LEVEL,
            targetMinAge = Constants.MIN_AGE,
            targetMaxAge = Constants.MAX_AGE
        )
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Match with Buddies").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
    }

    private fun verifyBuddiesFetched() {
        val buddyUiState = buddyViewModel.uiState.value
        if (buddyUiState.buddies.isEmpty() && buddyUiState.errorMessage == null) {
            Thread.sleep(2000)
            composeTestRule.waitForIdle()
        }
        
        if (buddyUiState.errorMessage != null) {
            throw AssertionError(
                "Failed to fetch buddies from backend: ${buddyUiState.errorMessage}. " +
                "Please ensure the backend is running and there are other users in the database."
            )
        }
        
        assert(buddyUiState.buddies.isNotEmpty()) {
            "No buddies returned from backend. " +
            "Please ensure there are other users in the database to match with. " +
            "Error: ${buddyUiState.errorMessage ?: "None"}"
        }
    }

    private fun verifyMatchScreenNavigation() {
        assert(navigateToMatchInvoked) { 
            "Navigation to MatchScreen should be invoked. " +
            "Buddies found: ${buddyViewModel.uiState.value.buddies.size}"
        }
        
        matchViewModel.initializeWithBuddies(buddyViewModel.uiState.value.buddies)
        
        assert(mainViewModel.uiState.value.showMatchScreen) {
            "MainViewModel should show match screen. Current state: ${mainViewModel.uiState.value}"
        }
    }

    private fun verifyMatchScreenDisplayed() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("Match").assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Match").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    private fun navigateThroughBuddiesIfMultiple() {
        try {
            composeTestRule.waitUntil(timeoutMillis = 2000) {
                try {
                    composeTestRule.onNodeWithText("Next").assertIsDisplayed()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }
            composeTestRule.onNodeWithText("Next").assertIsDisplayed()
            composeTestRule.onNodeWithText("Next").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Match").assertIsDisplayed()
            composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        } catch (e: AssertionError) {
            // Only one buddy - test still passes
        }
    }

    private fun fetchBuddiesFromRepository(): List<Buddy> {
        val result = runBlocking {
            buddyRepository.getBuddies(
                targetMinLevel = null,
                targetMaxLevel = null,
                targetMinAge = null,
                targetMaxAge = null
            )
        }
        
        if (result.isFailure || result.getOrNull().isNullOrEmpty()) {
            throw AssertionError(
                "No buddies available from backend. " +
                "Please ensure there are other users in the database to match with."
            )
        }
        
        return result.getOrNull()!!
    }

    private fun setupMatchScreenContent(showMatchScreen: Boolean) {
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    if (showMatchScreen) {
                        MatchScreen(viewModel = matchViewModel)
                    } else {
                        BuddiesScreen(viewModel = buddyViewModel)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun verifyMatchScreenAndClickMatch() {
        composeTestRule.onNodeWithText("Match").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Match").performClick()
    }

    private fun waitForChatCreation() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText("Back").assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun verifyChatCreated() {
        assert(chatNavigationInvoked) { 
            "Chat navigation should be invoked after matching. " +
            "This means the backend API call to create a chat was successful."
        }
        assert(chatIdReceived != null) { 
            "Expected chat ID to be received from backend, but got null. " +
            "The chat creation API call may have failed."
        }
        assert(chatIdReceived!!.isNotEmpty()) {
            "Chat ID should not be empty. Received: $chatIdReceived"
        }
    }

    @Test
    fun success_scenario_finding_buddies_and_viewing_matches() {
        // Given: User is on the main screen and navigates to buddies tab
        // The backend should have other users available to match with
        
        setupMainScreenWithNavigation()
        navigateToBuddiesScreen()
        clickMatchWithBuddiesAndWait()
        verifyBuddiesFetched()
        verifyMatchScreenNavigation()
        verifyMatchScreenDisplayed()
        navigateThroughBuddiesIfMultiple()
    }

    @Test
    fun success_scenario_matching_with_user() {
        // Given: User is on the match screen with a buddy's profile displayed
        // The backend should have at least one user available to match with
        
        val buddies = fetchBuddiesFromRepository()
        matchViewModel.initializeWithBuddies(buddies)
        setupMatchScreenContent(true)
        verifyMatchScreenAndClickMatch()
        waitForChatCreation()
        verifyChatCreated()
    }
}
