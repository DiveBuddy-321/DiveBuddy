package com.cpen321.usermanagement.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.cpen321.usermanagement.data.remote.dto.AuthData
import com.cpen321.usermanagement.data.remote.dto.CreateEventRequest
import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.data.repository.AuthRepository
import com.cpen321.usermanagement.data.repository.EventRepository
import com.cpen321.usermanagement.ui.theme.ProvideSpacing
import com.cpen321.usermanagement.ui.theme.UserManagementTheme
import com.cpen321.usermanagement.ui.viewmodels.EventViewModel
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * Test for browsing events use case:
 * - User navigates to events screen
 * - User sees list of events
 * - User clicks on an event
 * - User views event details
 */
class EventsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testViewModel: TestEventViewModel

    @Before
    fun setup() {
        testViewModel = TestEventViewModel()
    }

    @Test
    fun eventsScreen_displaysEventsList() {
        // Given: We have events to display
        val testEvents = createTestEvents()
        testViewModel.setEvents(testEvents)

        // When: Events screen is displayed
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    EventsScreen(eventViewModel = testViewModel)
                }
            }
        }

        // Wait for composition to complete
        composeTestRule.waitForIdle()

        // Then: Verify events are displayed
        composeTestRule.onNodeWithText("Upcoming Events").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Event 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Event 2").assertIsDisplayed()
    }

    @Test
    fun eventsScreen_clickingEvent_showsEventDetails() {
        // Given: We have events and one is selected
        val testEvents = createTestEvents()
        testViewModel.setEvents(testEvents)
        val selectedEvent = testEvents[0]

        // When: Events screen is displayed
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    EventsScreen(eventViewModel = testViewModel)
                }
            }
        }

        composeTestRule.waitForIdle()

        // Click on the first event card
        composeTestRule.onNodeWithText(selectedEvent.title, substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Event details screen should be displayed
        composeTestRule.onNodeWithText("Event Details").assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedEvent.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedEvent.description).assertIsDisplayed()
        
        // Verify event details are shown
        if (selectedEvent.location != null) {
            composeTestRule.onNodeWithText(selectedEvent.location, substring = true).assertIsDisplayed()
        }
        
        // Verify back button is present
        composeTestRule.onNodeWithContentDescription("Back to events screen").assertIsDisplayed()
    }

    @Test
    fun eventsScreen_clickingEvent_displaysAllEventDetails() {
        // Given: We have an event with all details filled
        val testEvents = createTestEventsWithFullDetails()
        testViewModel.setEvents(testEvents)
        val selectedEvent = testEvents[0]

        // When: Events screen is displayed and event is clicked
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    EventsScreen(eventViewModel = testViewModel)
                }
            }
        }

        composeTestRule.waitForIdle()

        // Click on the event
        composeTestRule.onNodeWithText(selectedEvent.title, substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()

        // Then: All event details should be displayed
        composeTestRule.onNodeWithText("Event Details").assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedEvent.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedEvent.description).assertIsDisplayed()
        
        if (selectedEvent.location != null) {
            composeTestRule.onNodeWithText("Location", substring = true).assertIsDisplayed()
            composeTestRule.onNodeWithText(selectedEvent.location, substring = true).assertIsDisplayed()
        }
        
        if (selectedEvent.skillLevel != null) {
            composeTestRule.onNodeWithText("Skill Level", substring = true).assertIsDisplayed()
            composeTestRule.onNodeWithText(selectedEvent.skillLevel, substring = true).assertIsDisplayed()
        }
        
        composeTestRule.onNodeWithText("Attendees", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Date & Time", substring = true).assertIsDisplayed()
    }

    @Test
    fun eventsScreen_clickingBackFromDetails_returnsToEventsList() {
        // Given: We have events
        val testEvents = createTestEvents()
        testViewModel.setEvents(testEvents)
        val selectedEvent = testEvents[0]

        // When: Events screen is displayed
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    EventsScreen(eventViewModel = testViewModel)
                }
            }
        }

        composeTestRule.waitForIdle()

        // Click on an event to view details
        composeTestRule.onNodeWithText(selectedEvent.title, substring = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we're on details screen
        composeTestRule.onNodeWithText("Event Details").assertIsDisplayed()

        // Click back button
        composeTestRule.onNodeWithContentDescription("Back to events screen")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Should return to events list
        composeTestRule.onNodeWithText("Upcoming Events").assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedEvent.title).assertIsDisplayed()
    }

    // Helper functions to create test data
    private fun createTestEvents(): List<Event> {
        val now = Date()
        val futureDate = Date(now.time + 86400000) // 1 day from now
        
        return listOf(
            Event(
                _id = "event1",
                title = "Test Event 1",
                description = "This is a test event description",
                date = futureDate,
                capacity = 10,
                skillLevel = "Beginner",
                location = "Test Location",
                latitude = 49.2827,
                longitude = -123.1207,
                createdBy = "user1",
                attendees = emptyList(),
                photo = null,
                createdAt = now,
                updatedAt = now
            ),
            Event(
                _id = "event2",
                title = "Test Event 2",
                description = "Another test event",
                date = Date(futureDate.time + 86400000),
                capacity = 20,
                skillLevel = null,
                location = null,
                latitude = null,
                longitude = null,
                createdBy = "user2",
                attendees = listOf("user3", "user4"),
                photo = null,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun createTestEventsWithFullDetails(): List<Event> {
        val now = Date()
        val futureDate = Date(now.time + 86400000)
        
        return listOf(
            Event(
                _id = "event1",
                title = "Diving Event",
                description = "Join us for an amazing diving experience",
                date = futureDate,
                capacity = 15,
                skillLevel = "Advanced",
                location = "Vancouver, BC",
                latitude = 49.2827,
                longitude = -123.1207,
                createdBy = "user1",
                attendees = listOf("user2", "user3"),
                photo = null,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    /**
     * Test implementation of EventViewModel for testing purposes
     */
    private class TestEventViewModel : EventViewModel(
        eventRepository = FakeEventRepository(),
        authRepository = FakeAuthRepository()
    ) {
        fun setEvents(events: List<Event>) {
            // Update the parent ViewModel's state using reflection
            @Suppress("UNCHECKED_CAST")
            val stateFlowField = EventViewModel::class.java.getDeclaredField("_uiState")
            stateFlowField.isAccessible = true
            val stateFlow = stateFlowField.get(this) as MutableStateFlow<com.cpen321.usermanagement.ui.viewmodels.EventUiState>
            val currentState = stateFlow.value
            stateFlow.value = currentState.copy(
                isLoading = false,
                events = events,
                error = null
            )
        }
    }

    /**
     * Fake EventRepository implementation for testing
     */
    private class FakeEventRepository : EventRepository {
        override suspend fun getAllEvents(): Result<List<Event>> {
            return Result.success(emptyList())
        }

        override suspend fun createEvent(request: CreateEventRequest): Result<Event> {
            return Result.failure(Exception("Not implemented in test"))
        }

        override suspend fun updateEvent(eventId: String, request: CreateEventRequest): Result<Event> {
            return Result.failure(Exception("Not implemented in test"))
        }

        override suspend fun joinEvent(eventId: String): Result<Event> {
            return Result.failure(Exception("Not implemented in test"))
        }

        override suspend fun leaveEvent(eventId: String): Result<Event> {
            return Result.failure(Exception("Not implemented in test"))
        }

        override suspend fun deleteEvent(eventId: String): Result<Unit> {
            return Result.failure(Exception("Not implemented in test"))
        }
    }

    /**
     * Fake AuthRepository implementation for testing
     */
    private class FakeAuthRepository : AuthRepository {
        override suspend fun signInWithGoogle(context: Context): Result<GoogleIdTokenCredential> {
            return Result.failure(Exception("Not implemented in test"))
        }

        override suspend fun googleSignIn(tokenId: String): Result<AuthData> {
            return Result.failure(Exception("Not implemented in test"))
        }

        override suspend fun googleSignUp(tokenId: String): Result<AuthData> {
            return Result.failure(Exception("Not implemented in test"))
        }

        override suspend fun clearToken(): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun doesTokenExist(): Boolean {
            return false
        }

        override suspend fun getStoredToken(): String? {
            return null
        }

        override suspend fun getCurrentUser(): User? {
            return null
        }

        override suspend fun isUserAuthenticated(): Boolean {
            return false
        }
    }
}

