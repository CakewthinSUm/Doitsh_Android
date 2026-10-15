package com.doitsh.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.doitsh.app.R
import com.doitsh.app.ui.components.DoitshNavigationBar
import com.doitsh.app.ui.components.DoitshNavigationDestination
import com.doitsh.app.ui.components.DoitshNavigationRail
import com.doitsh.app.ui.components.rememberUseNavigationRail
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
 * Two destinations only: Material 3 Expressive asks a navigation bar to hold
 * three to five, and Settings is reached from the top bar instead.
 *
 * Shared by the bottom bar and the wide-screen navigation rail so the two
 * containers can never drift apart.
 */
private val TopLevelDestinations = listOf(
    DoitshNavigationDestination(Home, Icons.Filled.Home, R.string.nav_tasks) { padding ->
        HomeScreen(contentPadding = padding)
    },
    DoitshNavigationDestination(Projects, Icons.Outlined.Folder, R.string.nav_projects) { padding ->
        ProjectsScreen(contentPadding = padding)
    },
)

/**
 * The keys of those destinations, so "is this page one of the bar's own?" is a lookup
 * rather than a comparison against a named page.
 *
 * This is the hierarchy the navigation container belongs to. Settings is reached from
 * the top bar and pushed on top of the top-level pages rather than being one of them,
 * so while it is open the bar would be showing a selected destination that no longer
 * matches the screen — and tapping it would throw the page away. The shell therefore
 * renders its chrome for the top level only.
 */
private val TopLevelKeys: Set<NavKey> = TopLevelDestinations.map { it.key }.toSet()

@Composable
fun DoitshApp() {
    val backStack = rememberNavBackStack(Home)
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var creationDialog by remember { mutableStateOf<CreationDialog?>(null) }
    val creationViewModel: CreationViewModel = hiltViewModel()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // The one adaptive decision in the app, and it is the Material adaptive
    // navigation suite's own rule rather than an ad-hoc breakpoint — see
    // rememberUseNavigationRail. The app itself stays single-pane either way.
    val useNavigationRail = rememberUseNavigationRail()

    // Read once per frame and passed to the three places that hang off the top level
    // (the rail, the bottom bar, the FAB menu), so they can never disagree about which
    // page is on screen. Settings is not top level: it is a page pushed on top, and the
    // navigation container belongs to the level underneath it.
    val isTopLevel = backStack.last() in TopLevelKeys

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    Row(modifier = Modifier.fillMaxSize()) {
        if (useNavigationRail && isTopLevel) {
            DoitshNavigationRail(
                destinations = TopLevelDestinations,
                selectedKey = backStack.last(),
                onSelect = { backStack.selectTopLevel(it) },
            )
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
                                Projects -> stringResource(R.string.nav_projects)
                                Settings -> stringResource(R.string.nav_settings)
                                else -> stringResource(R.string.app_name)
                            }
                        )
                    },
                    // A pushed page carries its own way back, because the navigation
                    // container that would otherwise show where it sits in the app is
                    // hidden while it is open.
                    navigationIcon = {
                        if (!isTopLevel) {
                            IconButton(onClick = { backStack.removeLastOrNull() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                    },
                    actions = {
                        // Settings is not a top-level destination — it is reached from
                        // here, which keeps the navigation bar at two items.
                        if (isTopLevel) {
                            IconButton(onClick = { backStack.navigateTo(Settings) }) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = stringResource(R.string.nav_settings)
                                )
                            }
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
                if (!useNavigationRail && isTopLevel) {
                    DoitshNavigationBar(
                        destinations = TopLevelDestinations,
                        selectedKey = backStack.last(),
                        onSelect = { backStack.selectTopLevel(it) },
                    )
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
                    // NavDisplay's built-in specs are a hard-coded 700ms cross-fade
                    // that ignores MotionScheme; these use the theme's Expressive
                    // motion instead. See NavTransitions.kt.
                    transitionSpec = expressiveSceneTransition(),
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
                        // Each top-level destination publishes its position in the bar as
                        // metadata, which is how the transitions learn which way to slide
                        // — see navigationSlideDirection. Position and content both come
                        // from the destination list, so adding, reordering or replacing a
                        // bar button needs no change here. Settings publishes no position:
                        // it is pushed onto the stack, not selected from the container.
                        TopLevelDestinations.forEachIndexed { index, destination ->
                            entry(
                                key = destination.key,
                                metadata = navigationOrder(index),
                            ) { destination.content(padding) }
                        }
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

                // Both top-level destinations create things, so the FAB menu is not
                // gated on Home alone — but it does belong to the top level, for the
                // same reason the bar does.
                if (isTopLevel) {
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
