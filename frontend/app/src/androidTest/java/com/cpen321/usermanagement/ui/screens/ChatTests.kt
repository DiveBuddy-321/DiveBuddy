package com.cpen321.usermanagement.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.semantics.SemanticsProperties
import com.cpen321.usermanagement.data.local.preferences.TokenManager
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.data.repository.AuthRepositoryImpl
import com.cpen321.usermanagement.data.repository.ChatRepositoryImpl
import com.cpen321.usermanagement.data.repository.ProfileRepositoryImpl
import com.cpen321.usermanagement.data.repository.ChatRepository
import com.cpen321.usermanagement.data.repository.ProfileRepository
import com.cpen321.usermanagement.data.socket.SocketManager
import com.cpen321.usermanagement.ui.theme.ProvideSpacing
import com.cpen321.usermanagement.ui.theme.UserManagementTheme
import com.cpen321.usermanagement.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.test.core.app.ApplicationProvider

/**
 * End-to-end tests for chatting functionality that actually call the backend server.
 * 
 * Test scenarios (single user perspective):
 * - User navigates to chat screen
 * - User sees list of existing chats
 * - User opens a chat (or uses an existing one)
 * - User sends multiple messages
 * - User verifies that sent messages appear in the chat UI
 * 
 * Assumptions:
 * - User is already logged in (auth token is set up in @Before)
 * - Backend server is running and accessible
 * - There is at least one existing chat available, OR
 *   the user can create a chat through the app before running this test
 */
class ChatTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var tokenManager: TokenManager
    private lateinit var chatRepository: ChatRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var chatViewModel: ChatViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        tokenManager = TokenManager(context)
        
        // Initialize real repositories that will call the backend
        chatRepository = ChatRepositoryImpl(RetrofitClient.chatInterface)
        profileRepository = ProfileRepositoryImpl(
            context = context,
            userInterface = RetrofitClient.userInterface,
            imageInterface = RetrofitClient.imageInterface,
            tokenManager = tokenManager
        )
        
        // Set up authentication token - assume user is already logged in
        runBlocking {
            val existingToken = tokenManager.getTokenSync()
            if (existingToken != null) {
                RetrofitClient.setAuthToken(existingToken)
            } else {
                throw IllegalStateException(
                    "No authentication token found. " +
                    "Please ensure the test user is logged in before running end-to-end tests. " +
                    "The token should be set up in the test environment or via a login flow."
                )
            }
        }

        // Construct dependencies for ChatViewModel
        val socketManager = SocketManager()
        val authRepository = AuthRepositoryImpl(
            context = context,
            authInterface = RetrofitClient.authInterface,
            userInterface = RetrofitClient.userInterface,
            tokenManager = tokenManager
        )
        
        chatViewModel = ChatViewModel(
            chatRepository = chatRepository,
            profileRepository = profileRepository,
            socketManager = socketManager,
            authRepository = authRepository
        )
    }

    @Test
    fun success_scenario_sending_messages() {
        // Given: User is logged in and wants to send messages in a chat
        // Prerequisites:
        // - There should be at least one existing chat available
        //   (user can create a chat through the app before running this test)
        
        println("==========================================")
        println("CHAT TEST - SENDING MESSAGES")
        println("==========================================")
        println("This test verifies that a user can send messages in a chat.")
        println("==========================================")
        
        // Load chats first
        runBlocking {
            chatViewModel.loadChats()
        }
        
        // When: Chat screen is displayed (using real components with viewModel parameter)
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    ChatScreen(chatViewModel = chatViewModel)
                }
            }
        }

        composeTestRule.waitForIdle()

        // Wait for chats to load - the ChatScreen will load chats automatically via LaunchedEffect
        // Wait until we see either chat cards or "No messages yet"
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                // Check if we see "No messages yet" or any chat-related text
                composeTestRule.onNodeWithText("No messages yet", substring = true).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                // If not "No messages yet", check if we have any clickable nodes (potential chat cards)
                try {
                    composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty()
                } catch (e2: Exception) {
                    false
                }
            }
        }

        // Wait a bit more for the UI to stabilize
        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        // Try to find a chat card
        try {
            // Look for any clickable card (chat cards are clickable)
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                try {
                    // Try to find any node that's clickable
                    composeTestRule.onAllNodes(hasClickAction()).onFirst().assertIsDisplayed()
                    true
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            throw AssertionError(
                "No chats found. Please ensure there is at least one existing chat. " +
                "You can create a chat by matching with another user through the Buddies/Match feature."
            )
        }

        // Find and click on a chat card
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onAllNodes(hasClickAction()).onFirst().assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        // Click on the first clickable chat card we find
        composeTestRule.onAllNodes(hasClickAction()).onFirst().performClick()
        
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // Verify we're in the single chat screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("Direct Message", substring = true).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify the message input field is present
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("Type a message...", substring = true).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Step 1: Send the first message
        val message1 = "Hello! This is my first test message."
        println("Sending message 1: $message1")

        // Use performTextReplacement to replace any existing text
        composeTestRule.onNodeWithText("Type a message...", substring = true)
            .performTextReplacement(message1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Send message").performClick()
        composeTestRule.waitForIdle()

        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        // Verify the first message appears in the chat (wait for it to appear)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                // Find the message in the chat list (not in input field)
                // Use onAllNodesWithText and filter out editable nodes
                val nodes = composeTestRule.onAllNodesWithText(message1, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                // Check if any node contains the text and is NOT editable
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }
        println("✓ Message 1 sent and displayed")

        // Step 2: Send a second message
        val message2 = "This is my second test message."
        println("Sending message 2: $message2")

        composeTestRule.onNodeWithText("Type a message...", substring = true)
            .performTextReplacement(message2)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Send message").performClick()
        composeTestRule.waitForIdle()

        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        // Verify the second message appears
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(message2, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }
        println("✓ Message 2 sent and displayed")

        // Step 3: Send a third message
        val message3 = "This is my third test message. Testing message sending functionality!"
        println("Sending message 3: $message3")

        composeTestRule.onNodeWithText("Type a message...", substring = true)
            .performTextReplacement(message3)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Send message").performClick()
        composeTestRule.waitForIdle()

        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        // Verify the third message appears
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(message3, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }
        println("✓ Message 3 sent and displayed")

        // Final verification: Verify all messages are visible in the chat
        // Wait a bit more to ensure all messages are loaded and UI is stable
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        // Verify each message is displayed (excluding editable text fields)
        // Find non-editable nodes containing each message
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(message1, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(message2, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(message3, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }

        println("==========================================")
        println("TEST COMPLETED SUCCESSFULLY")
        println("==========================================")
        println("All messages were sent and displayed correctly.")
        println("Message sending functionality is working!")
        println("==========================================")
    }

    @Test
    fun success_scenario_receiving_messages_from_other_user() {
        /**
         * TWO-USER BIDIRECTIONAL CHAT TEST
         * 
         * This test verifies bidirectional messaging between User A and User B:
         * - User A (automated test) sends messages to User B
         * - User A (automated test) receives messages from User B
         * 
         * TEST SETUP:
         * - User A (automated test): This test runs on User A's device
         * - User B (manual tester): A second person must act as User B and follow the instructions below
         * 
         * PREREQUISITES:
         * 1. User A and User B must both be logged into the app on separate devices
         * 2. User A and User B must have an existing chat between them
         *    (they can create a chat by matching through the Buddies/Match feature)
         * 3. Backend server must be running and accessible
         * 4. Both users must be connected to the internet
         * 
         * MANUAL TESTER INSTRUCTIONS (User B):
         * ======================================
         * 
         * STEP 1: Prepare Your Device
         * ---------------------------
         * 1. Open the app on your device (User B's device)
         * 2. Ensure you are logged in with User B's account
         * 3. Navigate to the Chat screen
         * 4. Find and open the chat with User A
         * 5. Wait for the automated test to start (you'll see User A's device begin the test)
         * 
         * STEP 2: Wait for User A's Messages
         * ----------------------------------
         * 1. User A will send messages first (automated)
         * 2. Watch for these messages to appear in your chat:
         *    - "Hello User B! This is User A's first test message."
         *    - "This is User A's second test message. Testing bidirectional chat!"
         * 3. Verify these messages appear in your chat (User B's view)
         * 
         * STEP 3: Send Response Messages to User A
         * -------------------------------------------
         * 1. After User A's messages appear, type the following EXACT message:
         *    "Hello User A! This is User B's response message."
         * 2. Tap the Send button
         * 3. Wait 3-5 seconds
         * 
         * STEP 4: Verification
         * ---------------------
         * 1. Verify that all messages (both from User A and your own) appear in your chat (User B's view)
         * 2. The automated test on User A's device will verify that all messages appear there too
         * 3. If the test passes, you should see a success message in the test output
         * 
         * IMPORTANT NOTES:
         * - Use the EXACT text as specified (case-sensitive)
         * - Wait 3-5 seconds between actions to allow time for real-time updates
         * - Do NOT send any other messages during the test
         * - Keep the chat screen open on User B's device throughout the test
         * 
         * TROUBLESHOOTING:
         * - If messages don't appear, check:
         *   * Both devices are connected to the internet
         *   * Backend server is running
         *   * WebSocket connection is established (check app logs)
         *   * Both users are in the same chat room
         */
        
        println("==========================================")
        println("TWO-USER BIDIRECTIONAL CHAT TEST")
        println("==========================================")
        println("This test verifies bidirectional messaging:")
        println("- User A sends messages to User B (automated)")
        println("- User A receives messages from User B (automated)")
        println("")
        println("MANUAL TESTER (User B) INSTRUCTIONS:")
        println("=====================================")
        println("1. Open the app on your device (User B)")
        println("2. Navigate to Chat screen")
        println("3. Open the chat with User A")
        println("4. Wait for User A to send messages (you'll see them appear)")
        println("5. After User A's messages appear, send this EXACT message:")
        println("")
        println("   'Hello User A! This is User B's response message.'")
        println("")
        println("6. Keep the chat open on your device throughout the test")
        println("==========================================")
        println("")
        println("Waiting 10 seconds for User B to prepare...")
        println("(Please ensure User B has opened the chat before proceeding)")
        println("==========================================")
        
        // Give User B time to prepare
        Thread.sleep(10000)
        
        // Load chats first
        runBlocking {
            chatViewModel.loadChats()
        }
        
        // When: Chat screen is displayed
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    ChatScreen(chatViewModel = chatViewModel)
                }
            }
        }

        composeTestRule.waitForIdle()

        // Wait for chats to load
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText("No messages yet", substring = true).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                try {
                    composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty()
                } catch (e2: Exception) {
                    false
                }
            }
        }

        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        // Find and click on a chat card (should be the chat with User B)
        try {
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                try {
                    composeTestRule.onAllNodes(hasClickAction()).onFirst().assertIsDisplayed()
                    true
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            throw AssertionError(
                "No chats found. Please ensure there is at least one existing chat between User A and User B. " +
                "You can create a chat by matching with another user through the Buddies/Match feature."
            )
        }

        // Click on the first clickable chat card
        composeTestRule.onAllNodes(hasClickAction()).onFirst().performClick()
        
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // Verify we're in the single chat screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("Direct Message", substring = true).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify the message input field is present
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("Type a message...", substring = true).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        println("==========================================")
        println("User A is now in the chat screen.")
        println("User A will send messages first...")
        println("==========================================")

        // Get the chat ID for sending messages
        val chatId = runBlocking {
            val chats = chatViewModel.uiState.value.chats
            chats.firstOrNull()?._id
        } ?: throw AssertionError("No chat found. Please ensure there is an existing chat between User A and User B.")

        // PHASE 1: User A sends messages to User B
        println("")
        println("PHASE 1: User A sending messages to User B")
        println("==========================================")

        // Message 1 from User A
        val userAMessage1 = "Hello User B! This is User A's first test message."
        println("User A sending message 1: '$userAMessage1'")
        
        composeTestRule.onNodeWithText("Type a message...", substring = true)
            .performTextReplacement(userAMessage1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Send message").performClick()
        composeTestRule.waitForIdle()

        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        // Verify User A's first message appears
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(userAMessage1, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }
        println("✓ User A's message 1 sent and displayed")

        // Message 2 from User A
        val userAMessage2 = "This is User A's second test message. Testing bidirectional chat!"
        println("User A sending message 2: '$userAMessage2'")
        
        composeTestRule.onNodeWithText("Type a message...", substring = true)
            .performTextReplacement(userAMessage2)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Send message").performClick()
        composeTestRule.waitForIdle()

        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        // Verify User A's second message appears
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(userAMessage2, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }
        println("✓ User A's message 2 sent and displayed")

        println("")
        println("==========================================")
        println("User A has sent messages. Waiting for User B to respond...")
        println("(User B should see the messages and send a response)")
        println("==========================================")

        // PHASE 2: User A receives message from User B
        println("")
        println("PHASE 2: User A waiting for message from User B")
        println("==========================================")

        // Expected message from User B (must match exactly what User B sends)
        val userBMessage = "Hello User A! This is User B's response message."

        // Wait for message from User B
        println("Waiting for response message from User B...")
        composeTestRule.waitUntil(timeoutMillis = 60000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(userBMessage, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }
        println("✓ Message received from User B: '$userBMessage'")

        // Final verification: Verify all messages are visible (both from User A and User B)
        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        println("")
        println("Verifying all messages are displayed...")
        
        // Verify User A's messages
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(userAMessage1, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(userAMessage2, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }
        
        // Verify User B's message
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(userBMessage, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                false
            }
        }

        println("==========================================")
        println("TEST COMPLETED SUCCESSFULLY")
        println("==========================================")
        println("Bidirectional messaging test passed!")
        println("- User A sent messages to User B: ✓")
        println("- User A received message from User B: ✓")
        println("- All messages displayed correctly: ✓")
        println("Real-time bidirectional chat functionality is working!")
        println("==========================================")
    }
}

