package com.doitsh.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.doitsh.app.ui.screens.ProjectsScreen
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
data object Projects : NavKey

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

/**
 * Two destinations only: Material 3 Expressive asks a navigation bar to hold
 * three to five, and Settings is reached from the top bar instead.
 */
private val TopLevelDestinations = listOf(
    TopLevelDestination(Home, Icons.Filled.Home, R.string.nav_tasks),
    TopLevelDestination(Projects, Icons.Outlined.Folder, R.string.nav_projects),
)

@Composable
fun DoitshApp() {
    val backStack = rememberNavBackStack(Home)
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var creationDialog by remember { mutableStateOf<CreationDialog?>(null) }
    val creationViewModel: CreationViewModel = hiltViewModel()
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

        // Each destination brings its own Scaffold rather than sharing one around the
        // NavDisplay. That is what makes Settings animate as a whole page: its top bar
        // is inside the scene, so it is the *card* that recedes during a predictive
        // back drag, not just the body under a fixed app bar. It also lets Settings
        // carry a back arrow, which a shared bar could not — the shared bar has no way
        // to know whether the current destination wants a back affordance or a
        // settings action.
        NavDisplay(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            // NavDisplay's built-in specs are a hard-coded 700ms cross-fade that
            // ignores MotionScheme; these use the theme's Expressive motion instead.
            // See NavTransitions.kt.
            transitionSpec = expressiveForwardTransition(),
            popTransitionSpec = expressivePopTransition(),
            predictivePopTransitionSpec = expressivePredictivePopTransition(),
            entryDecorators = listOf(
                // Saves each entry's `rememberSaveable` state, and gives every
                // entry its own ViewModelStore so ViewModels are scoped to the
                // destination rather than to the Activity.
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<Home> {
                    PredictiveBackCard(
                        isTopDestination = backStack.last() == Home,
                        isInBackStack = Home in backStack,
                    ) {
                        TopLevelScaffold(
                            title = stringResource(R.string.app_name),
                            currentDestination = Home,
                            useNavigationRail = useNavigationRail,
                            snackbarHostState = snackbarHostState,
                            onSelect = { backStack.selectTopLevel(it) },
                            onOpenSettings = { backStack.navigateTo(Settings) },
                            floatingActionButton = {
                                CreationFab(
                                    expanded = fabMenuExpanded,
                                    onExpandedChange = { fabMenuExpanded = it },
                                    onCreateProject = {
                                        fabMenuExpanded = false
                                        creationDialog = CreationDialog.Project
                                    },
                                    onCreateTask = {
                                        fabMenuExpanded = false
                                        creationDialog = CreationDialog.Task
                                    }
                                )
                            }
                        ) { padding ->
                            HomeScreen(contentPadding = padding)
                        }
                    }
                }
                entry<Projects> {
                    PredictiveBackCard(
                        isTopDestination = backStack.last() == Projects,
                        isInBackStack = Projects in backStack,
                    ) {
                        TopLevelScaffold(
                            title = stringResource(R.string.nav_projects),
                            currentDestination = Projects,
                            useNavigationRail = useNavigationRail,
                            snackbarHostState = snackbarHostState,
                            onSelect = { backStack.selectTopLevel(it) },
                            onOpenSettings = { backStack.navigateTo(Settings) },
                            floatingActionButton = {
                                CreationFab(
                                    expanded = fabMenuExpanded,
                                    onExpandedChange = { fabMenuExpanded = it },
                                    onCreateProject = {
                                        fabMenuExpanded = false
                                        creationDialog = CreationDialog.Project
                                    },
                                    onCreateTask = {
                                        fabMenuExpanded = false
                                        creationDialog = CreationDialog.Task
                                    }
                                )
                            }
                        ) { padding ->
                            ProjectsScreen(contentPadding = padding)
                        }
                    }
                }
                entry<Settings> {
                    PredictiveBackCard(
                        isTopDestination = backStack.last() == Settings,
                        isInBackStack = Settings in backStack,
                    ) {
                        SettingsScaffold(
                            snackbarHostState = snackbarHostState,
                            onBack = { backStack.removeLastOrNull() }
                        ) { padding ->
                            SettingsScreen(
                                contentPadding = padding,
                                onShowMessage = { message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            )
                        }
                    }
                }
            }
        )
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
 * The chrome shared by the two top-level destinations: app bar with the Settings
 * action, the adaptive navigation container, and the creation FAB.
 */
@Composable
private fun TopLevelScaffold(
    title: String,
    currentDestination: NavKey,
    useNavigationRail: Boolean,
    snackbarHostState: SnackbarHostState,
    onSelect: (NavKey) -> Unit,
    onOpenSettings: () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    // Settings is not a top-level destination — it is reached from
                    // here, which keeps the navigation bar at two items.
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
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
                            selected = currentDestination == destination.key,
                            onClick = { onSelect(destination.key) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.label)) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
        content = content
    )
}

/**
 * Settings as a full page of its own: a back arrow and no bottom bar, so it reads as
 * a screen pushed on top of the app rather than a tab inside it. The back arrow is
 * the affordance the shared bar could not offer, and it is what the system back
 * gesture's predictive animation previews.
 */
@Composable
private fun SettingsScaffold(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content
    )
}

/**
 * The Expressive FAB menu. It lives in the Scaffold's `floatingActionButton` slot
 * rather than in a Box over the content, so the Scaffold keeps it clear of the
 * bottom bar and it travels with the page during navigation.
 */
@Composable
private fun CreationFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreateProject: () -> Unit,
    onCreateTask: () -> Unit,
) {
    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange
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
            onClick = onCreateProject,
            icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
            text = { Text(stringResource(R.string.fab_new_project)) }
        )
        FloatingActionButtonMenuItem(
            onClick = onCreateTask,
            icon = { Icon(Icons.Filled.AddTask, contentDescription = null) },
            text = { Text(stringResource(R.string.fab_new_task)) }
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

/**
 * Opens a destination on top of the current one, so system back returns to where
 * the user came from. Settings uses this rather than [selectTopLevel]: it is
 * reached from a top bar action, not from the navigation bar, and clearing the
 * stack there would make back exit the app instead of returning to the list.
 */
private fun NavBackStack<NavKey>.navigateTo(key: NavKey) {
    if (lastOrNull() == key) return
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
