package com.focustube.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.focustube.app.ui.theme.FocusBlue

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Discover : BottomNavItem(Screen.Discover.route, "Discover", Icons.Default.Home)
    object Search : BottomNavItem(Screen.Search.route, "Search", Icons.Default.Search)
    object Following : BottomNavItem(Screen.Following.route, "Following", Icons.Default.Subscriptions)
    object Library : BottomNavItem(Screen.Library.route, "Library", Icons.Default.VideoLibrary)
    object Focus : BottomNavItem(Screen.Focus.route, "Focus", Icons.Default.Shield)
    object Reels : BottomNavItem(Screen.Reels.route, "Reels", Icons.Default.MovieFilter)
}

@Composable
fun FocusBottomBar(
    currentRoute: String?,
    reelsEnabled: Boolean = false,
    onNavigate: (String) -> Unit
) {
    val items = buildList {
        add(BottomNavItem.Discover)
        add(BottomNavItem.Search)
        add(BottomNavItem.Following)
        add(BottomNavItem.Library)
        add(BottomNavItem.Focus)
        if (reelsEnabled) {
            add(BottomNavItem.Reels)
        }
    }

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = {
                        Text(
                            item.title,
                            fontSize = 10.sp,
                            maxLines = 1,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selected,
                    onClick = {
                        if (currentRoute != item.route) {
                            onNavigate(item.route)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = FocusBlue,
                        indicatorColor = FocusBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
