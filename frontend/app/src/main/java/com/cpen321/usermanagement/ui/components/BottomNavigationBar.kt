package com.cpen321.usermanagement.ui.components

import Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cpen321.usermanagement.R

sealed class BottomNavItem(
    val route: String,
    val title: Int,
    val icon: Int
) {
    object Home : BottomNavItem("events", R.string.events, R.drawable.ic_event)
    object Buddies : BottomNavItem("buddies", R.string.buddies, R.drawable.ic_buddies)
    object Chat : BottomNavItem("chat", R.string.chat, R.drawable.ic_chat)
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Buddies,
        BottomNavItem.Chat
    )

    NavigationBar(
        modifier = modifier
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(name = item.icon)
                },
                label = {
                    Text(text = stringResource(item.title))
                },
                selected = currentRoute == item.route,
                onClick = { onItemClick(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

