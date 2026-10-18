package com.doitsh.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inbox
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
import com.doitsh.app.domain.model.Task
import com.doitsh.app.ui.components.DoitshNavigationBar
import com.doitsh.app.ui.components.DoitshNavigationDestination
import com.doitsh.app.ui.components.DoitshNavigationRail
import com.doitsh.app.ui.components.TaskEditorSheet
import com.doitsh.app.ui.components.rememberUseNavigationRail
import com.doitsh.app.ui.screens.CreationViewModel
import com.doitsh.app.ui.screens.InboxScreen
import com.doitsh.app.ui.screens.ProjectsScreen
import com.doitsh.app.ui.screens.SettingsScreen
import com.doitsh.app.ui.screens.TodayScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Navigation 3 destinations. Each key is `@Serializable` so the back stack can be
 * persisted and restored across process death by `rememberNavBackStack`.
 */
@Serializable
data object Today : NavKey

@Serializable
data object Inbox : NavKey

@Serializable
data object Projects : NavKey

@Serializable
data object Settings : NavKey

/**
 * Three destinations: Material 3 Expressive asks a navigation bar to hold three to
 * five, and Settings is reached from the top bar instead.
 *
 * The order is the order they appear in the bar, the order the transitions slide in,
 * and the order they are registered with NavDisplay — all of it read from this one
 * list, so nothing here has to be kept in step by hand.
 *
 * Today leads and Inbox follows, which is the arrangement that makes the landing page
 * the one you open the app to *do* something about rather than the one you file things
 * into. Shared by the bottom bar and the wide-screen navigation rail so the two
 * containers can never drift apart.
 *
 * The two list pages also need a way to hand a tapped task to the edit sheet, which lives
 * in the shell above them ([DoitshApp]). Their content takes an `onEdit` the shell
 * supplies at render time, rather than capturing one here: this list is a constant, and
 * a lambda holding shell state would make it one that has to be rebuilt on every
 * recomposition of the shell. Projects does not take it — nothing there edits a task yet.
 */
private val TopLevelDestinations = listOf(
    DoitshNavigationDestination(Today, Icons.Filled.Today, R.string.nav_today) { padding, onEdit ->
        TodayScreen(contentPadding = padding, onEdit = onEdit)
    },
    DoitshNavigationDestination(Inbox, Icons.Outlined.Inbox, R.string.nav_inbox) { padding, onEdit ->
        InboxScreen(contentPadding = padding, onEdit = onEdit)
    },
    DoitshNavigationDestination(Projects, Icons.Outlined.Folder, R.string.nav_projects) { padding, _ ->
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
    val backStack = rememberNavBackStack(Today)
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var creationDialog by remember { mutableStateOf<CreationDialog?>(null) }
    // What the task sheet is open for, or `null` when it is closed. One piece of state
    // rather than an open flag plus a task, because "open with no task" and "closed" would
    // then be two different states that both look like `task == null`.
    var taskSheet by remember { mutableStateOf<TaskSheet?>(null) }
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
                            text = when (val current = backStack.last()) {
                                // A top-level page is titled with the label its own
                                // navigation button carries, read from the destination
                                // list — so a page cannot end up labelled one thing in
                                // the bar and another in the top bar.
                                in TopLevelKeys -> stringResource(
                                    TopLevelDestinations
                                        .first { it.key == current }
                                        .label
                                )
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
                            ) {
                                SceneSurface {
                                    destination.content(padding) { taskSheet = TaskSheet(it) }
                                }
                            }
                        }
                        entry<Settings> {
                            SceneSurface {
                                SettingsScreen(
                                    contentPadding = padding,
                                    onShowMessage = { message ->
                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                    }
                                )
                            }
                        }
                    }
                )

                // Every top-level destination creates things, so the FAB menu is not
                // gated on any one page — but it does belong to the top level, for the
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
                                taskSheet = TaskSheet(task = null)
                            },
                            icon = { Icon(Icons.Filled.AddTask, contentDescription = null) },
                            text = { Text(stringResource(R.string.fab_new_task)) }
                        )
                    }
                }
            }
        }
    }

    // Task creation is a bottom sheet and project creation is a dialog: a task has five
    // fields and is created constantly, so it earns the larger surface and the
    // drag-to-dismiss gesture; a project is a name and a description, which a dialog
    // says in one glance and a sheet would pad out with empty space.
    //
    // Task editing is the same sheet, so it is drawn from the same place whether the
    // request came from the FAB (creating) or from a row tap (editing). One host is what
    // keeps the two flows from growing two forms that drift apart.
    taskSheet?.let { request ->
        val projects by creationViewModel.projects.collectAsState()
        TaskEditorSheet(
            task = request.task,
            projects = projects,
            onDismiss = { taskSheet = null },
            onSubmit = { existing, title, description, dueDate, priority, projectId ->
                if (existing == null) {
                    creationViewModel.createTask(
                        title = title,
                        description = description,
                        dueDate = dueDate,
                        priority = priority,
                        projectId = projectId,
                    )
                } else {
                    // `copy` over the task the sheet was opened on, so the fields the
                    // form does not show — completion, creation time, id — survive the
                    // edit.
                    creationViewModel.updateTask(
                        existing.copy(
                            title = title,
                            description = description,
                            dueDate = dueDate,
                            priority = priority,
                            projectId = projectId,
                        )
                    )
                }
                taskSheet = null
            },
        )
    }

    if (creationDialog == CreationDialog.Project) {
        ProjectCreationDialog(
            onDismiss = { creationDialog = null },
            onCreate = { name, description ->
                creationViewModel.createProject(name, description)
                creationDialog = null
            },
        )
    }
}

/**
 * The opaque backdrop each destination is drawn on.
 *
 * NavDisplay renders the outgoing and incoming scenes *simultaneously* and slides them
 * past each other, so for the length of every transition two screens occupy the same
 * frame. Each destination here is a `LazyColumn` with no background of its own, and the
 * single `Scaffold` above draws its surface once, behind both of them — so without this
 * the two screens are composited straight onto each other and their text superimposes.
 *
 * That is not a cosmetic detail, it is what the reported "predictive back" bug actually
 * was. A list page's empty state showing through a Settings page that had not moved
 * looked like a cross-fade, and a screen apparently mid-fade reads as a scale-down; both were
 * the same missing backdrop. It also made the drag look inconsistent, because how legible
 * the bleed-through is depends on how much of the two screens happens to overlap at the
 * fraction the user released at.
 *
 * Painting the theme's `background` — the same colour the `Scaffold` would have drawn
 * under a single screen — makes the top scene cover the one behind it, which is what makes
 * the transitions read as one screen replacing another. It has to be inside the scene
 * rather than around the NavDisplay: a single backdrop behind both scenes is exactly the
 * arrangement that does not work.
 *
 * Applied per entry rather than through `SceneDecoratorStrategy`, which is the other way
 * to wrap every scene. A decorator has to return a new `Scene`, and Nav3 identifies scenes
 * for its transitions by class plus key (`AnimatedSceneKey`) — so a decorator replaces the
 * identity that `SeekableTransitionState.seekTo` matches a predictive drag's previous scene
 * against. Wrapping the content leaves that bookkeeping untouched, and the cost is only
 * that a new destination has to be added inside this same `entryProvider` block, where the
 * other entries already are.
 */
@Composable
private fun SceneSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
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

/**
 * A request to open the task sheet: [task] is the row that was tapped, or `null` for a
 * new task. A type rather than a bare `Task?` because "open, creating" and "closed" would
 * otherwise both be `null`, and the sheet would have no state to be open in.
 */
private data class TaskSheet(val task: Task?)

/**
 * The dialog the FAB menu can open. Tasks are not in this enum: they go to [TaskSheet]
 * above, because a created task and an edited one are the same sheet and should not be
 * two states that each have to remember the other is closed.
 */
private enum class CreationDialog { Project }

/**
 * Creating a project: a name and an optional description.
 *
 * Still a dialog, unlike the task sheet. A project is two fields and is created rarely;
 * the sheet's extra height and its pinned footer would be padding around empty space,
 * and a modal choice with one obvious "save" is exactly what a dialog is for.
 */
@Composable
private fun ProjectCreationDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_new_project)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_project_name)) },
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
                    onCreate(trimmedName, description.trim().takeIf { it.isNotEmpty() })
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
