package com.cpen321.usermanagement.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cpen321.usermanagement.data.remote.dto.Buddy
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.data.repository.BuddyRepository
import com.cpen321.usermanagement.data.repository.ChatRepository
import com.cpen321.usermanagement.ui.theme.ProvideSpacing
import com.cpen321.usermanagement.ui.theme.UserManagementTheme
import com.cpen321.usermanagement.ui.viewmodels.BuddyViewModel
import com.cpen321.usermanagement.ui.viewmodels.MatchViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Test for finding buddies use case:
 * - User navigates to buddies screen
 * - User clicks on "Match with Buddies" button
 * - User sees list of matches in MatchScreen
 * - User can navigate through the list of matches
 */
class BuddyTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testBuddyViewModel: TestBuddyViewModel
    private lateinit var testMatchViewModel: TestMatchViewModel
    private lateinit var fakeBuddyRepository: FakeBuddyRepository
    private var navigateToMatchInvoked = false

    @Before
    fun setup() {
        fakeBuddyRepository = FakeBuddyRepository()
        testBuddyViewModel = TestBuddyViewModel(fakeBuddyRepository)
        testMatchViewModel = TestMatchViewModel(FakeChatRepository())
        navigateToMatchInvoked = false
    }

    @Test
    fun success_scenario_finding_buddies_and_viewing_matches() {
        // Given: We have buddies available to match with
        val testBuddies = createTestBuddies()
        
        // State to control which screen is shown - using remember-like pattern
        val showMatchScreenState = mutableStateOf(false)
        
        // Set up the fake repository to return test buddies
        fakeBuddyRepository.setBuddies(testBuddies)

        // Set up navigation callback
        testBuddyViewModel.onNavigateToMatch = {
            navigateToMatchInvoked = true
            // Simulate navigation by showing MatchScreen
            testMatchViewModel.initializeWithBuddies(testBuddies)
            // Update state directly - Compose will observe and recompose
            showMatchScreenState.value = true
        }
        
        // When: Buddies screen is displayed (with conditional rendering)
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    if (showMatchScreenState.value) {
                        MatchScreen(viewModel = testMatchViewModel)
                    } else {
                        BuddiesScreen(viewModel = testBuddyViewModel)
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // Verify we're on the buddies screen
        composeTestRule.onNodeWithText("Buddies").assertIsDisplayed()
        composeTestRule.onNodeWithText("Match with Buddies").assertIsDisplayed()

        // Click on "Match with Buddies" button
        composeTestRule.onNodeWithText("Match with Buddies")
            .performClick()

        composeTestRule.waitForIdle()

        // Wait for async operation to complete and navigation to trigger
        composeTestRule.waitForIdle()
        
        // Wait for the state change to propagate and recomposition
        composeTestRule.waitForIdle()

        // Verify navigation was triggered
        assert(navigateToMatchInvoked) { "Navigation to MatchScreen should be invoked" }

        // Verify we're now on the MatchScreen and first buddy is displayed
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Age: 25").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level: Intermediate").assertIsDisplayed()
        composeTestRule.onNodeWithText("I love diving", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Vancouver, BC").assertIsDisplayed()

        // Verify navigation buttons are present
        composeTestRule.onNodeWithText("Match").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsDisplayed()

        // Navigate to the next buddy by clicking "Next"
        composeTestRule.onNodeWithText("Next").performClick()

        composeTestRule.waitForIdle()

        // Verify second buddy is displayed
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("Age: 30").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level: Advanced").assertIsDisplayed()
        composeTestRule.onNodeWithText("Experienced diver", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Toronto, ON").assertIsDisplayed()

        // Navigate to the third buddy
        composeTestRule.onNodeWithText("Next").performClick()

        composeTestRule.waitForIdle()

        // Verify third buddy is displayed
        composeTestRule.onNodeWithText("Charlie").assertIsDisplayed()
        composeTestRule.onNodeWithText("Age: 22").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level: Beginner").assertIsDisplayed()
        composeTestRule.onNodeWithText("New to diving", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Montreal, QC").assertIsDisplayed()

        // Verify we've successfully navigated through all three buddies
        // The last buddy (Charlie) is displayed, confirming we can view the full list
    }

    // Helper function to create test buddies
    private fun createTestBuddies(): List<Buddy> {
        return listOf(
            Buddy(
                user = User(
                    _id = "user1",
                    email = "alice@example.com",
                    name = "Alice",
                    bio = "I love diving and exploring underwater worlds!",
                    profilePicture = null,
                    age = 25,
                    skillLevel = "Intermediate",
                    location = "Vancouver, BC",
                    latitude = 49.2827,
                    longitude = -123.1207,
                    createdAt = null,
                    updatedAt = null
                ),
                distance = 5.2
            ),
            Buddy(
                user = User(
                    _id = "user2",
                    email = "bob@example.com",
                    name = "Bob",
                    bio = "Experienced diver looking for adventure buddies",
                    profilePicture = null,
                    age = 30,
                    skillLevel = "Advanced",
                    location = "Toronto, ON",
                    latitude = 43.6532,
                    longitude = -79.3832,
                    createdAt = null,
                    updatedAt = null
                ),
                distance = 10.5
            ),
            Buddy(
                user = User(
                    _id = "user3",
                    email = "charlie@example.com",
                    name = "Charlie",
                    bio = "New to diving but excited to learn!",
                    profilePicture = null,
                    age = 22,
                    skillLevel = "Beginner",
                    location = "Montreal, QC",
                    latitude = 45.5017,
                    longitude = -73.5673,
                    createdAt = null,
                    updatedAt = null
                ),
                distance = 15.8
            )
        )
    }

    /**
     * Test implementation of BuddyViewModel for testing purposes
     */
    private class TestBuddyViewModel(
        private val fakeRepository: FakeBuddyRepository
    ) : BuddyViewModel(
        buddyRepository = fakeRepository,
        savedStateHandle = androidx.lifecycle.SavedStateHandle()
    )

    /**
     * Test implementation of MatchViewModel for testing purposes
     */
    private class TestMatchViewModel(
        fakeChatRepository: FakeChatRepository
    ) : MatchViewModel(
        buddyRepository = FakeBuddyRepository(),
        chatRepository = fakeChatRepository,
        savedStateHandle = androidx.lifecycle.SavedStateHandle()
    )

    /**
     * Fake BuddyRepository implementation for testing
     */
    private class FakeBuddyRepository : BuddyRepository {
        private var buddiesToReturn: List<Buddy> = emptyList()

        fun setBuddies(buddies: List<Buddy>) {
            buddiesToReturn = buddies
        }

        override suspend fun getBuddies(
            targetMinLevel: Int?,
            targetMaxLevel: Int?,
            targetMinAge: Int?,
            targetMaxAge: Int?
        ): Result<List<Buddy>> {
            return Result.success(buddiesToReturn)
        }
    }

    /**
     * Fake ChatRepository implementation for testing
     */
    private class FakeChatRepository : ChatRepository {
        override suspend fun listChats(): Result<List<com.cpen321.usermanagement.data.remote.dto.Chat>> {
            return Result.success(emptyList())
        }

        override suspend fun createChat(peerId: String, name: String?): Result<String> {
            return Result.success("chat-$peerId")
        }

        override suspend fun getMessages(
            chatId: String,
            limit: Int?,
            before: String?
        ): Result<com.cpen321.usermanagement.data.remote.dto.MessagesResponse> {
            return Result.failure(Exception("Not implemented in test"))
        }

        override suspend fun sendMessage(chatId: String, content: String): Result<com.cpen321.usermanagement.data.remote.dto.Message> {
            return Result.failure(Exception("Not implemented in test"))
        }
    }
}
