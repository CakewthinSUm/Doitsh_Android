package com.doitsh.app.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null
)

@Composable
fun DoitshNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        windowInsets = NavigationBarDefaults.windowInsets,
        modifier = modifier
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (index == selectedIndex && item.selectedIcon != null)
                            item.selectedIcon
                        else
                            item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) }
            )
        }
    }
}

// Default navigation items for the app
val DefaultNavigationItems = listOf(
    NavigationItem(
        label = "Tasks",
        icon = Icons.Filled.Home
    ),
    NavigationItem(
        label = "Settings",
        icon = Icons.Filled.Settings
    )
)
