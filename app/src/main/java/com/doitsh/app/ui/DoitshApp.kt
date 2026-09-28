package com.doitsh.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.doitsh.app.ui.screens.HomeScreen
import com.doitsh.app.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoitshApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            AppScreen.Home -> "Doitsh"
                            AppScreen.Settings -> "Settings"
                        }
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                // Let the top bar extend behind the status bar
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            BottomDock(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it },
                onAddTask = { /* TODO: show add task dialog */ }
            )
        }
    ) { padding ->
        when (currentScreen) {
            AppScreen.Home -> HomeScreen(contentPadding = padding)
            AppScreen.Settings -> SettingsScreen(contentPadding = padding)
        }
    }
}

enum class AppScreen { Home, Settings }

@Composable
private fun BottomDock(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    onAddTask: () -> Unit
) {
    val dockShape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(top = 8.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockNavigation(
                currentScreen = currentScreen,
                onScreenSelected = onScreenSelected,
                shape = dockShape
            )

            Surface(
                modifier = Modifier.size(88.dp),
                onClick = onAddTask,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add task",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DockNavigation(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    shape: Shape
) {
    Surface(
        modifier = Modifier.height(56.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                selected = currentScreen == AppScreen.Home,
                onClick = { onScreenSelected(AppScreen.Home) },
                icon = { Icon(Icons.Outlined.Home, contentDescription = "Tasks") }
            )
            DockItem(
                selected = currentScreen == AppScreen.Settings,
                onClick = { onScreenSelected(AppScreen.Settings) },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
            )
        }
    }
}

@Composable
private fun DockItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.size(48.dp),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
