package com.doitsh.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.doitsh.app.ui.components.*
import com.doitsh.app.ui.screens.AddTaskViewModel
import com.doitsh.app.ui.screens.InboxScreen
import com.doitsh.app.ui.screens.MoreScreen
import com.doitsh.app.ui.screens.ProjectsScreen
import com.doitsh.app.ui.screens.TodayScreen
import com.doitsh.app.ui.theme.SpacingTokens
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoitshApp(
    addTaskViewModel: AddTaskViewModel = hiltViewModel()
) {
    var currentScreen by remember { mutableStateOf(AppScreen.Today) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val screenIndex = when (currentScreen) {
        AppScreen.Today -> 0
        AppScreen.Projects -> 1
        AppScreen.Inbox -> 2
        AppScreen.More -> 3
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DoitshTopBar(
                title = when (currentScreen) {
                    AppScreen.Today -> "Doitsh"
                    AppScreen.Projects -> "Projects"
                    AppScreen.Inbox -> "Inbox"
                    AppScreen.More -> "More"
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            FloatingBottomBar(
                navItems = DefaultNavItems,
                selectedIndex = screenIndex,
                onItemSelected = { index ->
                    currentScreen = when (index) {
                        0 -> AppScreen.Today
                        1 -> AppScreen.Projects
                        2 -> AppScreen.Inbox
                        3 -> AppScreen.More
                        else -> AppScreen.Today
                    }
                },
                onAddClick = { showAddTaskDialog = true }
            )
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (screen) {
                    AppScreen.Today -> TodayScreen()
                    AppScreen.Projects -> ProjectsScreen()
                    AppScreen.Inbox -> InboxScreen()
                    AppScreen.More -> MoreScreen()
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onTaskAdded = { /* Refresh will happen automatically via Flow */ },
            submitTask = { title, projectId, dueDateLabel, priority ->
                addTaskViewModel.addTask(
                    title = title,
                    projectId = projectId,
                    dueDate = if (dueDateLabel == "今天") LocalDateTime.now() else null,
                    priority = priority,
                    onSuccess = { /* Task added successfully */ }
                )
            }
        )
    }
}

enum class AppScreen { Today, Projects, Inbox, More }
