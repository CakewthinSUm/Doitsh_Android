package com.doitsh.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.doitsh.app.ui.components.*
import com.doitsh.app.ui.screens.HomeScreen
import com.doitsh.app.ui.screens.SettingsScreen
import com.doitsh.app.ui.theme.MotionTokens
import com.doitsh.app.ui.theme.SpacingTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoitshApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val screenIndex = if (currentScreen == AppScreen.Home) 0 else 1

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DoitshTopBar(
                title = when (currentScreen) {
                    AppScreen.Home -> "Doitsh"
                    AppScreen.Settings -> "Settings"
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            DoitshNavigationBar(
                items = DefaultNavigationItems,
                selectedIndex = screenIndex,
                onItemSelected = { index ->
                    currentScreen = when (index) {
                        0 -> AppScreen.Home
                        1 -> AppScreen.Settings
                        else -> AppScreen.Home
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentScreen == AppScreen.Home) {
                DoitshFab(
                    onClick = { /* TODO: show add task dialog */ }
                )
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) togetherWith fadeOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AppScreen.Home -> HomeScreen(contentPadding = padding)
                AppScreen.Settings -> SettingsScreen(contentPadding = padding)
            }
        }
    }
}

enum class AppScreen { Home, Settings }
