package com.cpen321.usermanagement.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
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
import com.cpen321.usermanagement.ui.viewmodels.chat.ChatViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.test.core.app.ApplicationProvider
import com.cpen321.usermanagement.ui.screens.chat.ChatScreen

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

    private fun setupChatScreen() {
        runBlocking {
            chatViewModel.loadChats()
        }
        
        composeTestRule.setContent {
            UserManagementTheme {
                ProvideSpacing {
                    ChatScreen(chatViewModel = chatViewModel)
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    private fun waitForChatsToLoad() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText("No messages yet", substring = true).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                println("Warning: 'No messages yet' text not found: ${e.message}")
                try {
                    composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty()
                } catch (e2: Exception) {
                    println("Error: Failed to find clickable nodes while waiting for chats: ${e2.message}")
                    false
                }
            }
        }

        Thread.sleep(2000)
        composeTestRule.waitForIdle()
    }

    private fun navigateToChat() {
        try {
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                try {
                    composeTestRule.onAllNodes(hasClickAction()).onFirst().assertIsDisplayed()
                    true
                } catch (e: Exception) {
                    println("Warning: Failed to find clickable chat card: ${e.message}")
                    false
                }
            }
        } catch (e: Exception) {
            println("Error: Timeout waiting for chat cards to appear: ${e.message}")
            throw AssertionError(
                "No chats found. Please ensure there is at least one existing chat. " +
                "You can create a chat by matching with another user through the Buddies/Match feature."
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onAllNodes(hasClickAction()).onFirst().assertIsDisplayed()
                true
            } catch (e: Exception) {
                println("Warning: Failed to verify chat card before clicking: ${e.message}")
                false
            }
        }

        composeTestRule.onAllNodes(hasClickAction()).onFirst().performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
    }

    private fun verifyInChatScreen() {
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("Type a message...", substring = true).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                println("Warning: Message input field not found: ${e.message}")
                false
            }
        }
    }

    private fun sendMessageAndVerify(message: String, messageLabel: String) {
        println("Sending $messageLabel")

        composeTestRule.onNodeWithText("Type a message...", substring = true)
            .performTextReplacement(message)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Send message").performClick()
        composeTestRule.waitForIdle()

        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        verifyMessageDisplayed(message)
        println("✓ $messageLabel sent")
    }

    private fun verifyMessageDisplayed(message: String) {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(message, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                println("Error: Failed to verify message displayed: '$message' - ${e.message}")
                false
            }
        }
    }

    private fun verifyAllMessagesDisplayed(messages: List<String>) {
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        messages.forEach { message ->
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                    val nodes = composeTestRule.onAllNodesWithText(message, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                println("Error: Failed to verify message displayed in list: '$message' - ${e.message}")
                false
            }
            }
        }
    }

    private fun waitForReceivedMessage(message: String, timeoutMillis: Long = 60000) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(message, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                println("Warning: Error while waiting for received message: '$message' - ${e.message}")
                false
            }
        }
    }

    private fun navigateToChatWithError(errorMessage: String) {
        try {
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                try {
                    composeTestRule.onAllNodes(hasClickAction()).onFirst().assertIsDisplayed()
                    true
                } catch (e: Exception) {
                    println("Warning: Failed to find clickable chat card: ${e.message}")
                    false
                }
            }
        } catch (e: Exception) {
            println("Error: Timeout waiting for chat cards in bidirectional test: ${e.message}")
            throw AssertionError(errorMessage)
        }

        composeTestRule.onAllNodes(hasClickAction()).onFirst().performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
    }

    private fun sendUserAMessage(message: String, messageLabel: String) {
        println("User A sending $messageLabel")
        
        composeTestRule.onNodeWithText("Type a message...", substring = true)
            .performTextReplacement(message)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Send message").performClick()
        composeTestRule.waitForIdle()

        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        verifyMessageDisplayed(message)
        println("✓ User A $messageLabel sent")
    }

    private fun verifyBidirectionalMessages(userAMessages: List<String>, userBMessage: String) {
        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        println("Verifying messages...")
        
        userAMessages.forEach { message ->
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                    val nodes = composeTestRule.onAllNodesWithText(message, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                println("Error: Failed to verify User A message in bidirectional test: '$message' - ${e.message}")
                false
            }
        }
        }
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(userBMessage, substring = true)
                val semanticsNodes = nodes.fetchSemanticsNodes()
                semanticsNodes.any { node ->
                    !node.config.contains(SemanticsProperties.EditableText)
                }
            } catch (e: Exception) {
                println("Error: Failed to verify User B message in bidirectional test: '$userBMessage' - ${e.message}")
                false
            }
        }
    }

    private fun printBidirectionalTestInstructions() {
        println("=== BIDIRECTIONAL CHAT TEST ===")
        println("User B: Open chat, wait for User A messages, then send:")
        println("'Hello User A! This is User B's response message.'")
        println("Waiting 10s for User B to prepare...")
    }

    private fun setupBidirectionalTest() {
        Thread.sleep(10000)
        setupChatScreen()
        waitForChatsToLoad()
        
        navigateToChatWithError(
            "No chats found. Please ensure there is at least one existing chat between User A and User B. " +
            "You can create a chat by matching with another user through the Buddies/Match feature."
        )
        verifyInChatScreen()

        println("User A ready, sending messages...")

        runBlocking {
            val chats = chatViewModel.uiState.value.chats
            chats.firstOrNull()?._id
        } ?: throw AssertionError("No chat found. Please ensure there is an existing chat between User A and User B.")
    }

    // Given: User is logged in and wants to send messages in a chat
    // Prerequisites:
    // - There should be at least one existing chat available
    //   (user can create a chat through the app before running this test)
    @Test
    fun success_scenario_sending_messages() {
        println("=== SENDING MESSAGES TEST ===")
        
        setupChatScreen()
        waitForChatsToLoad()
        navigateToChat()
        verifyInChatScreen()

        val message1 = "Hello! This is my first test message."
        val message2 = "This is my second test message."
        val message3 = "This is my third test message. Testing message sending functionality!"

        sendMessageAndVerify(message1, "message 1")
        sendMessageAndVerify(message2, "message 2")
        sendMessageAndVerify(message3, "message 3")

        verifyAllMessagesDisplayed(listOf(message1, message2, message3))

        println("✓ Test passed - all messages sent and displayed")
    }

    // TWO-USER BIDIRECTIONAL CHAT TEST
    // Verifies bidirectional messaging: User A sends/receives messages to/from User B
    // Prerequisites: Both users logged in, existing chat, backend running, internet connected
    // Manual tester (User B) must: open chat, wait for User A's messages, send exact response:
    // "Hello User A! This is User B's response message."
    @Test
    fun success_scenario_receiving_messages_from_other_user() {
        printBidirectionalTestInstructions()
        setupBidirectionalTest()

        println("Phase 1: User A sending messages")
        val userAMessage1 = "Hello User B! This is User A's first test message."
        val userAMessage2 = "This is User A's second test message. Testing bidirectional chat!"
        val userAMessages = listOf(userAMessage1, userAMessage2)
        sendUserAMessage(userAMessage1, "message 1")
        sendUserAMessage(userAMessage2, "message 2")
        println("Waiting for User B response...")

        println("Phase 2: Waiting for User B response")
        val userBMessage = "Hello User A! This is User B's response message."
        waitForReceivedMessage(userBMessage)
        println("✓ Received: '$userBMessage'")

        verifyBidirectionalMessages(userAMessages, userBMessage)
        println("✓ Test passed - bidirectional messaging working")
    }
}

