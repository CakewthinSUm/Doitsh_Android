package com.doitsh.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import com.doitsh.app.data.repository.TaskPage
import com.doitsh.app.domain.model.Task
import com.doitsh.app.ui.components.DoitshNavigationBar
import com.doitsh.app.ui.components.DoitshNavigationRail
import com.doitsh.app.ui.components.HomeTab
import com.doitsh.app.ui.components.TaskEditorSheet
import com.doitsh.app.ui.components.TaskListSortMenu
import com.doitsh.app.ui.components.hasTaskListOptions
import com.doitsh.app.ui.components.rememberUseNavigationRail
import com.doitsh.app.ui.screens.CreationViewModel
import com.doitsh.app.ui.screens.ProjectsScreen
import com.doitsh.app.ui.screens.SettingsScreen
import com.doitsh.app.ui.screens.TaskListOptionsViewModel
import com.doitsh.app.ui.screens.TaskListScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Navigation 3 destinations. Each key is `@Serializable` so the back stack can be
 * persisted and restored across process death by `rememberNavBackStack`.
 *
 * There is only one top-level destination. Today, Inbox and Projects are *pages* of the
 * pager inside it rather than entries of their own: they are three views of the same
 * level of the app, and putting them in one pager is what lets them be swiped between and
 * keeps their transitions out of the back stack entirely — see [HomePager].
 */
@Serializable
data object Home : NavKey

@Serializable
data object Settings : NavKey

/**
 * The shell: a navigation container whose pages sit under one [Home] destination.
 *
 * The top bar, the bottom bar / rail and the FAB menu belong to the shell rather than to
 * any page, which is why the pager's state is created here — the bar drives it and the
 * top bar's title reads it. See [HomePager] for what is inside it.
 */
@Composable
fun DoitshApp() {
    val backStack = rememberNavBackStack(Home)
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var creationDialog by remember { mutableStateOf<CreationDialog?>(null) }
    // What the task sheet is open for, or `null` when it is closed. One piece of state
    // rather than an open flag plus a task, because "open with no task" and "closed" would
    // then be two different states that both look like `task == null`.
    var taskSheet by remember { mutableStateOf<TaskSheet?>(null) }
    val creationViewModel: CreationViewModel = hiltViewModel()
    val optionsViewModel: TaskListOptionsViewModel = hiltViewModel()
    val taskListOptions by optionsViewModel.options.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // The one adaptive decision in the app, and it is the Material adaptive
    // navigation suite's own rule rather than an ad-hoc breakpoint — see
    // rememberUseNavigationRail. The app itself stays single-pane either way.
    val useNavigationRail = rememberUseNavigationRail()

    // The pager lives here rather than inside the Home entry because the bar, the rail
    // and the top bar's title are outside it: all three read and drive this one state.
    //
    // `targetPage` rather than `currentPage` for the selection: a swipe's current page
    // changes only once the drag has passed the halfway point, but a bar tap jumps
    // straight to the target, and `currentPage` would then animate through every page in
    // between — the pill and the title would flick through Inbox on the way to Projects.
    val pagerState = rememberPagerState { HomeTab.entries.size }
    val currentTab = HomeTab.entries[pagerState.targetPage]

    // Settings is not top level: it is a page pushed on top, and the navigation container
    // belongs to the level underneath it.
    val isTopLevel = backStack.last() == Home

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    Row(modifier = Modifier.fillMaxSize()) {
        if (useNavigationRail && isTopLevel) {
            DoitshNavigationRail(
                selectedTab = currentTab,
                onSelect = { tab -> scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
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
                                // A page is titled with the label its own navigation
                                // button carries, read from the same enum — so a page
                                // cannot end up labelled one thing in the bar and another
                                // in the top bar.
                                Home -> stringResource(currentTab.label)
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
                        // Only the two task list pages have a list to group and sort; the
                        // Projects page is a list of projects, so the button would be
                        // offering a choice it does not make. Keyed on `currentTab` — the
                        // pager's target — so the button matches the title, and it
                        // disappears as soon as a swipe settles on Projects.
                        if (isTopLevel && currentTab.hasTaskListOptions) {
                            TaskListSortMenu(
                                options = taskListOptions,
                                onUpdate = optionsViewModel::update,
                            )
                        }
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
                        selectedTab = currentTab,
                        onSelect = { tab -> scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
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
                        // Home publishes a container position so a Settings push is
                        // recognisable as a push and its pop as a reveal; see
                        // navigationMotion. It is the only destination at this level, so
                        // the position is a constant.
                        entry<Home>(metadata = navigationOrder(0)) {
                            SceneSurface {
                                HomePager(
                                    contentPadding = padding,
                                    state = pagerState,
                                    onEdit = { taskSheet = TaskSheet(it) },
                                )
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
 * The three top-level pages, side by side in one horizontal pager.
 *
 * ## Why one pager rather than three destinations
 *
 * Today, Inbox and Projects are the same level of the app — three views of one task list
 * — so moving between them is not a navigation step that deserves a back-stack entry.
 * As three Nav3 destinations they could only be *tapped*: switching meant replacing the
 * stack (`clear(); add(key)`), which Nav3 sees as a new stack rather than a pop or a
 * push, and the transition had to guess what the swap meant from the entries themselves.
 * It also meant each switch destroyed the page's ViewModelStore, so coming back to a page
 * reloaded it from the database.
 *
 * A pager owns the motion instead. A drag follows the finger, a release resolves by
 * velocity, and a second swipe mid-animation is just another scroll — there is no
 * transition to interrupt, so the interrupted case (a tap landing while the previous
 * switch is still running) needs no special geometry and cannot leave a blank frame
 * between two pages. A tap from the bar animates over exactly the same scroll state.
 *
 * ## The pager and the rows' swipe-to-delete
 *
 * Pointer events are dispatched to the deepest hit target first, and `SwipeToDismissBox`
 * on a task row is a child of this pager. It waits for touch slop and, when it sees a
 * horizontal drag, consumes it before the pager's nested-scroll handling is reached — so
 * a drag that starts on a card stays with the card, and a drag that starts anywhere else
 * (the list's side margins, the gaps between rows, the empty and loading states, the space
 * below the last row) reaches the pager.
 *
 * That is the intended division, and it has one consequence worth knowing: the row's
 * dismiss is one-directional, but it still claims drags in *both* directions, so a
 * rightward drag that starts on a card does nothing rather than paging back. Paging back
 * has to start outside the cards.
 *
 * ## Page state
 *
 * The pager itself keeps each page's scroll position, but it only saves the state of the
 * pages that are currently composed: `LazySaveableStateHolder` drops the keys of pages
 * that scrolled out of composition, and after process death the off-screen pages come
 * back with a fresh list position. [rememberSaveableStateHolder] wraps the pager and each
 * page provides its state under its tab, so all three survive either way.
 */
@Composable
private fun HomePager(
    contentPadding: PaddingValues,
    state: PagerState,
    onEdit: (Task) -> Unit,
) {
    val pageStateHolder = rememberSaveableStateHolder()

    HorizontalPager(
        state = state,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val tab = HomeTab.entries[page]
        pageStateHolder.SaveableStateProvider(tab) {
            // Opaque per page, for the same reason SceneSurface is opaque per entry: for
            // the length of a swipe two pages are on screen at once, and a page that
            // drew no background would composite straight onto its neighbour's text.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (tab) {
                    HomeTab.TODAY -> TaskListScreen(
                        page = TaskPage.TODAY,
                        contentPadding = contentPadding,
                        onEdit = onEdit,
                    )
                    HomeTab.INBOX -> TaskListScreen(
                        page = TaskPage.INBOX,
                        contentPadding = contentPadding,
                        onEdit = onEdit,
                    )
                    HomeTab.PROJECTS -> ProjectsScreen(contentPadding = contentPadding)
                }
            }
        }
    }
}

/**
 * Opens a destination on top of the current one, so system back returns to where
 * the user came from. Settings uses this: it is reached from a top bar action, not
 * from the navigation container, and replacing the stack there would make back exit
 * the app instead of returning to the list.
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
