package com.doitsh.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.doitsh.app.ui.screens.InboxScreen
import com.doitsh.app.ui.screens.ProjectsScreen
import com.doitsh.app.ui.screens.SettingsScreen
import com.doitsh.app.ui.screens.TodayScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
 */
private val TopLevelDestinations = listOf(
    DoitshNavigationDestination(Today, Icons.Filled.Today, R.string.nav_today) { padding ->
        TodayScreen(contentPadding = padding)
    },
    DoitshNavigationDestination(Inbox, Icons.Outlined.Inbox, R.string.nav_inbox) { padding ->
        InboxScreen(contentPadding = padding)
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
    val backStack = rememberNavBackStack(Today)
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
                            ) { SceneSurface { destination.content(padding) } }
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
            onCreateTask = { title, description, dueDate ->
                creationViewModel.createTask(title, description, dueDate)
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

private enum class CreationDialog { Project, Task }

@Composable
private fun CreationDialogContent(
    dialog: CreationDialog,
    onDismiss: () -> Unit,
    onCreateTask: (String, String?, LocalDateTime?) -> Unit,
    onCreateProject: (String, String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    // Held as an ISO date string rather than a LocalDate: `rememberSaveable` can only
    // persist types its Saver knows, and a String is one of them. The picker works in
    // whole days anyway, so no time of day is lost.
    var dueDateIso by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val isProject = dialog == CreationDialog.Project
    val title = stringResource(
        if (isProject) R.string.dialog_new_project else R.string.dialog_new_task
    )
    val nameLabel = stringResource(
        if (isProject) R.string.label_project_name else R.string.label_task_title
    )
    val trimmedName = name.trim()
    val dueDate = dueDateIso?.let(LocalDate::parse)

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
                // Only a task has a due date — a project is a container, not something
                // that comes due.
                if (!isProject) {
                    DueDateField(
                        dueDate = dueDate,
                        onPick = { showDatePicker = true },
                        onClear = { dueDateIso = null },
                    )
                }
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
                        // Stored at the start of the picked day: the app compares due
                        // dates by calendar day, so the time of day is not part of the
                        // value the user chose. See `Task.isDueTodayOrEarlier`.
                        onCreateTask(trimmedName, trimmedDescription, dueDate?.atStartOfDay())
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

    // A second dialog rather than a sheet: this is a modal choice with its own OK, and
    // nesting it here keeps the picked date in this dialog's own state — the field above
    // and the picker can never disagree about what is selected.
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.toEpochMillisUtc()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDateIso = datePickerState.selectedDateMillis?.toLocalDateUtc()?.toString()
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * The task dialog's due-date row: a field that opens the picker, plus a clear button
 * once a date has been chosen.
 *
 * The text field is `readOnly` and an invisible tap target is laid over it, rather than
 * making the field itself clickable. A read-only `OutlinedTextField` still consumes
 * pointer input — it can be focused and its cursor moved — so a click handler on the
 * field would fight that; the overlay is the pattern the Material date-field guidance
 * describes. The clear button sits *outside* the overlay so it stays tappable.
 *
 * The overlay is what the user taps, so it is also what has to carry the accessible
 * name. The field underneath describes itself as an editable text box ("edit box,
 * blank"), which is both wrong and the only thing a screen reader would find; so its
 * own semantics are cleared and the overlay announces the label and the current value
 * instead. The description goes on the overlay rather than on the `Box` around it: a
 * node with a description but no action of its own is not exposed, and the overlay is
 * the node that is actually clickable.
 */
@Composable
private fun DueDateField(
    dueDate: LocalDate?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val label = stringResource(R.string.label_due_date)
    val setDateHint = stringResource(R.string.action_set_due_date)
    val formattedDate = dueDate?.format(formatter)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = formattedDate.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                placeholder = { Text(setDateHint) },
                trailingIcon = {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                },
                // The field is only a display; the overlay above it takes the tap.
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onPick)
                    .semantics {
                        contentDescription = "$label, ${formattedDate ?: setDateHint}"
                        role = Role.Button
                    }
            )
        }
        if (dueDate != null) {
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_clear_due_date)
                )
            }
        }
    }
}

/**
 * `DatePicker` works in UTC: `selectedDateMillis` is the epoch instant of *midnight UTC*
 * on the day the user tapped, and it expects the same when seeding an initial selection.
 *
 * Converting through the local zone — the intuitive reading of a bare epoch millis —
 * shifts the date by a day for every user west of Greenwich, so a date picked in
 * California would be stored as the day before. These two helpers are the only place
 * that conversion happens, and they are unit-tested.
 */
internal fun LocalDate.toEpochMillisUtc(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
