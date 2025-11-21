package com.cpen321.usermanagement.ui.screens

import Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.components.BottomNavigationBar
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.screens.buddies.BuddiesScreen
import com.cpen321.usermanagement.ui.screens.buddies.MatchScreen
import com.cpen321.usermanagement.ui.screens.chat.ChatScreen
import com.cpen321.usermanagement.ui.screens.events.EventsScreen
import com.cpen321.usermanagement.ui.viewmodels.BuddyViewModel
import com.cpen321.usermanagement.ui.viewmodels.MainUiState
import com.cpen321.usermanagement.ui.viewmodels.MainViewModel
import com.cpen321.usermanagement.ui.viewmodels.MatchViewModel
import com.cpen321.usermanagement.ui.theme.LocalFontSizes
import com.cpen321.usermanagement.ui.theme.LocalSpacing

private data class MainCallbacks(
    val onProfileClick: () -> Unit,
    val onSuccessMessageShown: () -> Unit,
    val onBottomNavItemClick: (String) -> Unit,
    val onNavigateToMatch: () -> Unit,
    val onNavigateBackFromMatch: () -> Unit,
    val onOpenChat: (String) -> Unit
)

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onProfileClick: () -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    MainContent(
        uiState = uiState,
        snackBarHostState = snackBarHostState,
        callbacks = MainCallbacks(
            onProfileClick = onProfileClick,
            onSuccessMessageShown = mainViewModel::clearSuccessMessage,
            onBottomNavItemClick = mainViewModel::setCurrentScreen,
            onNavigateToMatch = mainViewModel::navigateToMatchScreen,
            onNavigateBackFromMatch = mainViewModel::navigateBackFromMatchScreen,
            onOpenChat = mainViewModel::openChat
        )
    )
}

@Composable
private fun MainContent(
    uiState: MainUiState,
    snackBarHostState: SnackbarHostState,
    callbacks: MainCallbacks,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MainTopBar(onProfileClick = callbacks.onProfileClick)
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = uiState.currentScreen,
                onItemClick = callbacks.onBottomNavItemClick
            )
        },
        snackbarHost = {
            MainSnackbarHost(
                hostState = snackBarHostState,
                successMessage = uiState.successMessage,
                onSuccessMessageShown = callbacks.onSuccessMessageShown
            )
        }
    ) { paddingValues ->
        MainBody(
            paddingValues = paddingValues,
            uiState = uiState,
            callbacks = callbacks
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            AppTitle()
        },
        actions = {
            ProfileActionButton(onClick = onProfileClick)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun AppTitle(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

@Composable
private fun ProfileActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    IconButton(
        onClick = onClick,
        modifier = modifier.size(spacing.extraLarge2)
    ) {
        ProfileIcon()
    }
}

@Composable
private fun ProfileIcon() {
    Icon(
        type = "light",
        name = R.drawable.ic_account_circle,
    )
}

@Composable
private fun MainSnackbarHost(
    hostState: SnackbarHostState,
    successMessage: String?,
    onSuccessMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    MessageSnackbar(
        hostState = hostState,
        messageState = MessageSnackbarState(
            successMessage = successMessage,
            errorMessage = null,
            onSuccessMessageShown = onSuccessMessageShown,
            onErrorMessageShown = { }
        ),
        modifier = modifier
    )
}

@Composable
private fun MainBody(
    paddingValues: PaddingValues,
    uiState: MainUiState,
    callbacks: MainCallbacks,
    modifier: Modifier = Modifier
) {
    val buddyViewModel: BuddyViewModel = hiltViewModel()
    val matchViewModel: MatchViewModel = hiltViewModel()
    
    // Set up callbacks
    LaunchedEffect(Unit) {
        buddyViewModel.onNavigateToMatch = callbacks.onNavigateToMatch
        matchViewModel.onNavigateBack = {
            // Clear states when going back
            buddyViewModel.clearState()
            matchViewModel.clearState()
            callbacks.onNavigateBackFromMatch()
        }
        matchViewModel.onNavigateToChat = { chatId ->
            callbacks.onOpenChat(chatId)
        }
    }
    
    // When navigating to match screen, initialize MatchViewModel with buddies
    LaunchedEffect(uiState.showMatchScreen) {
        if (uiState.showMatchScreen) {
            val buddies = buddyViewModel.uiState.value.buddies
            matchViewModel.initializeWithBuddies(buddies)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (uiState.showMatchScreen) {
            MatchScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = matchViewModel
            )
        } else {
            when (uiState.currentScreen) {
                "events" -> EventsScreen()
                "buddies" -> BuddiesScreen(
                    viewModel = buddyViewModel
                )
                "chat" -> ChatScreen(
                    modifier = Modifier,
                )
                else -> EventsScreen()
            }
        }
    }
}

@Composable
private fun WelcomeMessage(
    modifier: Modifier = Modifier
) {
    val fontSizes = LocalFontSizes.current

    Text(
        text = stringResource(R.string.welcome),
        style = MaterialTheme.typography.bodyLarge,
        fontSize = fontSizes.extraLarge3,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}