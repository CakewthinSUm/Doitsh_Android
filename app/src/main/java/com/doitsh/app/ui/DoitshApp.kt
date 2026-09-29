package com.doitsh.app.ui

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doitsh.app.ui.screens.CreationViewModel
import com.doitsh.app.ui.screens.HomeScreen
import com.doitsh.app.ui.screens.SettingsScreen
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoitshApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var creationDialog by remember { mutableStateOf<CreationDialog?>(null) }
    val creationViewModel: CreationViewModel = hiltViewModel()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

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
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Tasks") },
                    selected = currentScreen == AppScreen.Home,
                    onClick = { currentScreen = AppScreen.Home }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = currentScreen == AppScreen.Settings,
                    onClick = { currentScreen = AppScreen.Settings }
                )
            }
        },
        floatingActionButton = {}
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                AppScreen.Home -> HomeScreen(contentPadding = padding)
                AppScreen.Settings -> SettingsScreen(contentPadding = padding)
            }

            if (currentScreen == AppScreen.Home) {
                FloatingActionButtonMenu(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = padding.calculateBottomPadding()),
                    expanded = fabMenuExpanded,
                    button = {
                        ToggleFloatingActionButton(
                            modifier = Modifier
                                .animateFloatingActionButton(
                                    visible = true,
                                    alignment = Alignment.BottomEnd
                                ),
                            checked = fabMenuExpanded,
                            onCheckedChange = { fabMenuExpanded = it }
                        ) {
                            val imageVector by remember {
                                derivedStateOf {
                                    if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                                }
                            }
                            Icon(
                                painter = rememberVectorPainter(imageVector),
                                contentDescription = null,
                                modifier = Modifier.animateIcon({ checkedProgress })
                            )
                        }
                    },
                ) {
                    FloatingActionButtonMenuItem(
                        onClick = {
                            fabMenuExpanded = false
                            creationDialog = CreationDialog.Project
                        },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("新建项目") }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = {
                            fabMenuExpanded = false
                            creationDialog = CreationDialog.Task
                        },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("新建任务") }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = { fabMenuExpanded = false },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("导入任务") }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = { fabMenuExpanded = false },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("分享") }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = { fabMenuExpanded = false },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("设置") }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = { fabMenuExpanded = false },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("帮助") }
                    )
                }
            }
        }
    }

    creationDialog?.let { dialog ->
        CreationDialogContent(
            dialog = dialog,
            onDismiss = { creationDialog = null },
            onCreateTask = { title, description ->
                creationDialog = null
            },
            onCreateProject = { name, description ->
                creationDialog = null
            },
        )
    }
}

enum class AppScreen { Home, Settings }

private enum class CreationDialog { Project, Task }

@Composable
private fun CreationDialogContent(
    dialog: CreationDialog,
    onDismiss: () -> Unit,
    onCreateTask: (String, String?) -> Unit,
    onCreateProject: (String, String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val title = if (dialog == CreationDialog.Project) "新建项目" else "新建任务"
    val nameLabel = if (dialog == CreationDialog.Project) "项目名称" else "任务标题"
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    singleLine = true,
                    isError = name.isNotEmpty() && trimmedName.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = trimmedName.isNotEmpty(),
                onClick = {
                    val trimmedDescription = description.trim().takeIf { it.isNotEmpty() }
                    if (dialog == CreationDialog.Project) {
                        onCreateProject(trimmedName, trimmedDescription)
                    } else {
                        onCreateTask(trimmedName, trimmedDescription)
                    }
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
