package com.doitsh.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.doitsh.app.R
import com.doitsh.app.ui.screens.CreationViewModel
import com.doitsh.app.ui.screens.HomeScreen
import com.doitsh.app.ui.screens.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Navigation 3 destinations. Each key is `@Serializable` so the back stack can be
 * persisted and restored across process death by `rememberNavBackStack`.
 */
@Serializable
data object Home : NavKey

@Serializable
data object Settings : NavKey

/**
 * A top-level destination, shared by the compact bottom bar and the wide-screen
 * navigation rail so the two containers can never drift apart.
 */
private class TopLevelDestination(
    val key: NavKey,
    val icon: ImageVector,
    @StringRes val label: Int,
)

private val TopLevelDestinations = listOf(
    TopLevelDestination(Home, Icons.Filled.Home, R.string.nav_tasks),
    TopLevelDestination(Settings, Icons.Filled.Settings, R.string.nav_settings),
)

@Composable
fun DoitshApp() {
    val backStack = rememberNavBackStack(Home)
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var creationDialog by remember { mutableStateOf<CreationDialog?>(null) }
    val creationViewModel: CreationViewModel = hiltViewModel()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // The one adaptive decision in the app: below the medium width breakpoint the
    // top-level destinations live in a bottom bar, at or above it in a side rail.
    // The app itself stays single-pane.
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val useNavigationRail =
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    Row(modifier = Modifier.fillMaxSize()) {
        if (useNavigationRail) {
            NavigationRail {
                TopLevelDestinations.forEach { destination ->
                    NavigationRailItem(
                        selected = backStack.last() == destination.key,
                        onClick = { backStack.selectTopLevel(destination.key) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) }
                    )
                }
            }
        }

        Scaffold(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (backStack.last()) {
                                Settings -> stringResource(R.string.nav_settings)
                                else -> stringResource(R.string.app_name)
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
                if (!useNavigationRail) {
                    ShortNavigationBar {
                        TopLevelDestinations.forEach { destination ->
                            ShortNavigationBarItem(
                                selected = backStack.last() == destination.key,
                                onClick = { backStack.selectTopLevel(destination.key) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.label)) }
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {}
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        // Saves each entry's `rememberSaveable` state, and gives every
                        // entry its own ViewModelStore so ViewModels are scoped to the
                        // destination rather than to the Activity.
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entry<Home> { HomeScreen(contentPadding = padding) }
                        entry<Settings> {
                            SettingsScreen(
                                contentPadding = padding,
                                onShowMessage = { message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            )
                        }
                    }
                )

                if (backStack.last() == Home) {
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
                            icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                            text = { Text(stringResource(R.string.fab_new_project)) }
                        )
                        FloatingActionButtonMenuItem(
                            onClick = {
                                fabMenuExpanded = false
                                creationDialog = CreationDialog.Task
                            },
                            icon = { Icon(Icons.Filled.AddTask, contentDescription = null) },
                            text = { Text(stringResource(R.string.fab_new_task)) }
                        )
                        FloatingActionButtonMenuItem(
                            onClick = {
                                fabMenuExpanded = false
                                backStack.selectTopLevel(Settings)
                            },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            text = { Text(stringResource(R.string.fab_settings)) }
                        )
                    }
                }
            }
        }
    }

    creationDialog?.let { dialog ->
        CreationDialogContent(
            dialog = dialog,
            onDismiss = { creationDialog = null },
            onCreateTask = { title, description ->
                creationViewModel.createTask(title, description)
                creationDialog = null
            },
            onCreateProject = { name, description ->
                creationViewModel.createProject(name, description)
                creationDialog = null
            },
        )
    }
}

/**
 * Switches top-level destinations without growing the back stack: the current
 * destination is replaced rather than pushed.
 */
private fun NavBackStack<NavKey>.selectTopLevel(key: NavKey) {
    if (lastOrNull() == key) return
    clear()
    add(key)
}

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
    val isProject = dialog == CreationDialog.Project
    val title = stringResource(
        if (isProject) R.string.dialog_new_project else R.string.dialog_new_task
    )
    val nameLabel = stringResource(
        if (isProject) R.string.label_project_name else R.string.label_task_title
    )
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
                    label = { Text(stringResource(R.string.label_description_optional)) },
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
                    if (isProject) {
                        onCreateProject(trimmedName, trimmedDescription)
                    } else {
                        onCreateTask(trimmedName, trimmedDescription)
                    }
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
