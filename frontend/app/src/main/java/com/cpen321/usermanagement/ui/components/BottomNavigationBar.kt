package com.cpen321.usermanagement.ui.components

import Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                modifier = if (selected) {
                    Modifier
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    Modifier
                },
                icon = {
                    Icon(type = "light", name = item.icon)
                },
                label = {
                    Text(text = stringResource(item.title))
                },
                selected = selected,
                onClick = { onItemClick(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    }
}

