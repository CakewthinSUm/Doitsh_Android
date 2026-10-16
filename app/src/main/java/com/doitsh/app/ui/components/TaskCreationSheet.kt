package com.doitsh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.doitsh.app.R
import com.doitsh.app.domain.model.Priority
import com.doitsh.app.domain.model.Project
import com.doitsh.app.ui.DueDatePreset
import com.doitsh.app.ui.dragListTop
import com.doitsh.app.ui.dragTargetIndex
import com.doitsh.app.ui.dueDateForPreset
import com.doitsh.app.ui.presetForDueDate
import com.doitsh.app.ui.toEpochMillisUtc
import com.doitsh.app.ui.toLocalDateUtc
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/**
 * The "New task" modal bottom sheet.
 *
 * A sheet rather than a dialog because creating a task is a fast, contextual capture:
 * the list underneath stays visible (dimmed, not hidden), the sheet is dismissible by
 * dragging it down, and the form is laid out at the bottom of the screen where the
 * keyboard is about to appear rather than floating in the middle of it.
 *
 * ## Only the form scrolls; Save is pinned
 *
 * The sheet is inset above the keyboard (`contentWindowInsets`), which can leave it
 * roughly 270dp shorter than the screen. If the Save button sat at the end of the
 * content it would be pushed off-screen exactly when the user has finished typing and
 * wants it most, so the body scrolls inside a `weight` and the footer sits outside that
 * scroll region. See the mockup, `docs/task-sheet-mockup.html`, state A.
 *
 * ## What it fills in
 *
 * Title and description, plus three fields the model has always carried but no screen
 * ever set: due date, [Priority] and the owning project. Both the last two existed in
 * `Task`, in the Room entity, in the DTO and in the server table, and both were
 * silently defaulted on every task ever created from this app.
 *
 * @param projects `null` while the project list has not loaded yet, so the picker can
 *   tell "still loading" apart from "no projects exist" — the same convention as the
 *   list screens. An empty list and a loading list both show only Inbox, which is why
 *   the distinction does not change the menu, only whether it is worth opening.
 * @param onDismiss called for the drag-down gesture, the scrim tap and system back —
 *   every way out of the sheet that is not Save.
 * @param onCreate receives the assembled task. The caller is responsible for actually
 *   persisting it and for closing the sheet.
 */
@Composable
fun TaskCreationSheet(
    projects: List<Project>?,
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String?, dueDate: LocalDateTime?, priority: Priority, projectId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Hidden is the initial value and PartiallyExpanded is deliberately absent from
    // enabledValues: this sheet has one resting position, fully open, so a half-height
    // stop would only be a place for a drag to get stuck. This is the supported spelling
    // in material3 1.5.0-beta01 — `rememberModalBottomSheetState(skipPartiallyExpanded =
    // true)` is deprecated in favour of exactly this.
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    // Held as an ISO date string rather than a LocalDate: `rememberSaveable` can only
    // persist types its Saver knows, and a String is one of them. The picker works in
    // whole days anyway, so no time of day is lost.
    var dueDateIso by rememberSaveable { mutableStateOf<String?>(null) }
    var priorityName by rememberSaveable { mutableStateOf(Priority.NONE.name) }
    var projectId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val priority = Priority.entries.firstOrNull { it.name == priorityName } ?: Priority.NONE
    val dueDate = dueDateIso?.let(LocalDate::parse)

    // Read once so every comparison below is against a single day. The sheet is open
    // for seconds, so a recomposition reading a different `today` is not a real case.
    val today = remember { LocalDate.now() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        // The sheet's own container is the app's row colour, so it reads as a
        // continuation of the list underneath rather than an unrelated panel.
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.dialog_new_task),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            )

            // The scrollable part. `weight(1f, fill = false)` lets it shrink when the
            // keyboard takes the space, but keeps the sheet wrap-content when it does
            // not — so a short form does not stretch to fill the screen.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                TaskTitleField(
                    value = title,
                    onValueChange = { title = it },
                    isError = title.isNotEmpty() && title.isBlank(),
                )
                SheetTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.label_description_optional),
                )
                DueDateSection(
                    dueDate = dueDate,
                    today = today,
                    onPreset = { preset -> dueDateIso = dueDateForPreset(preset, today).toString() },
                    onPickDate = { showDatePicker = true },
                    onClear = { dueDateIso = null },
                )
                PrioritySection(
                    selected = priority,
                    onSelect = { priorityName = it.name },
                )
                ProjectSection(
                    projects = projects,
                    selectedId = projectId,
                    onSelect = { projectId = it },
                )
            }

            // Outside the scroll region, so it stays reachable with the keyboard up.
            Button(
                onClick = {
                    onCreate(
                        title.trim(),
                        description.trim().takeIf { it.isNotEmpty() },
                        // Stored at the start of the picked day: the app compares due
                        // dates by calendar day, so the time of day is not part of the
                        // value the user chose. See `Task.isDueTodayOrEarlier`.
                        dueDate?.atStartOfDay(),
                        priority,
                        projectId,
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

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
 * The title field, which takes focus as soon as the sheet appears.
 *
 * Focusing it is what makes this a capture flow rather than a form: the user pressed
 * "+ → New task" to *type something*, so the cursor should already be waiting. The
 * keyboard coming up at the same moment is not a side effect to work around — it is
 * what the sheet's pinned footer and scroll region above are designed around.
 */
@Composable
private fun TaskTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    SheetTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.label_task_title),
        isError = isError,
        modifier = Modifier.focusRequester(focusRequester),
    )
}

/**
 * A filled text field in the sheet's own idiom.
 *
 * Filled rather than outlined, and with a `surfaceContainerHighest` container: the sheet
 * behind it is `surfaceContainerLow`, which is one step darker, so the field needs the
 * brighter container to sit *on* the sheet rather than dissolve into it. The default
 * underline indicator is replaced by a focused outline in `primary`, so the whole box
 * reads as the active field.
 */
@Composable
private fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    minLines: Int = 1,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        minLines = minLines,
        shape = MaterialTheme.shapes.large,
        colors = sheetFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The colours every field in the sheet shares.
 *
 * Extracted so the several fields cannot drift apart, and so the two container colours
 * that define this sheet's look are stated once. `errorIndicatorColor` is left at the
 * default so an empty title still reads as an error through the normal error colour.
 */
@Composable
private fun sheetFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return TextFieldDefaults.colors(
        focusedContainerColor = scheme.surfaceContainerHighest,
        unfocusedContainerColor = scheme.surfaceContainerHighest,
        // The container does not change on focus, so the indicator is what has to carry
        // the state; a transparent unfocused indicator removes the default underline.
        focusedIndicatorColor = scheme.primary,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )
}

/**
 * The due date row: the three one-tap presets, a calendar action, and — only when the
 * chosen date is one the presets cannot describe — the date itself with a clear button.
 *
 * The three presets and the calendar button share a `ButtonGroup` because they are one
 * decision ("when is this due"), but they are not the same *kind* of thing: the presets
 * are `toggleableItem`s, state the group shows as selected, while the calendar is a
 * `clickableItem`, an action that opens a dialog. Modelling that honestly is why the
 * calendar button never shows as selected itself — when a custom date is in play, the
 * calendar button takes the group's selected treatment instead, so the row always shows
 * *where* the current date came from.
 *
 * A date picked from the calendar that happens to land on a preset day is reported as
 * that preset rather than as a custom date — see [presetForDueDate]. The user cannot
 * tell which control produced today's date and should not have to.
 */
@Composable
private fun DueDateSection(
    dueDate: LocalDate?,
    today: LocalDate,
    onPreset: (DueDatePreset) -> Unit,
    onPickDate: () -> Unit,
    onClear: () -> Unit,
) {
    val activePreset = dueDate?.let { presetForDueDate(it, today) }
    val isCustomDate = dueDate != null && activePreset == null

    // The calendar action sits in the section header rather than beside the presets.
    // Measured at a phone's width the three preset labels alone come to ~265dp of the
    // ~321dp available, so a fourth item — labelled or icon-only — always wraps to a
    // second line and pushes the rest of the form down. Putting it in the header also
    // says the right thing: the presets are the primary one-tap answers, and the
    // calendar is the escape hatch from them.
    //
    // A plain IconButton rather than an IconToggleButton: the toggle carries a 48dp
    // minimum touch target, which inflated this one header row to nearly three times the
    // height of the other section labels and left a conspicuous gap above the chips. The
    // button opens a dialog every time it is pressed — including when a custom date is
    // already set, which is how the user changes it — so it is an action, and the tint
    // is what shows whether the current date came from the calendar.
    SheetSection(
        label = stringResource(R.string.label_due_date),
        trailing = {
            IconButton(
                onClick = onPickDate,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = stringResource(R.string.action_pick_date),
                    tint = if (isCustomDate) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
    ) {
        // Deliberately *not* a `ButtonGroup`. The three presets are one decision, but
        // they are not interchangeable segments: the labels are different lengths, and
        // a ButtonGroup sizes its items to equal weights, which truncates "Tomorrow" to
        // "Tomorro" at a phone's width. A wrapping row of chips lets each item take the
        // width its own label needs and moves any overflow onto a second line instead
        // of cutting a word in half — the same reason the priority row is a chip row.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            DueDatePreset.entries.forEach { preset ->
                val isSelected = activePreset == preset
                FilterChip(
                    selected = isSelected,
                    // Tapping the active preset clears it: the button is a toggle, and
                    // "I did not mean to set a date" should not require hunting for a
                    // separate clear affordance.
                    onClick = { if (isSelected) onClear() else onPreset(preset) },
                    label = { Text(stringResource(preset.labelRes)) },
                    shapes = FilterChipDefaults.shapes(),
                )
            }
        }

        if (isCustomDate) {
            Spacer(Modifier.height(8.dp))
            CustomDateRow(dueDate = dueDate, onClear = onClear)
        }
    }
}

/** The picked date, shown as text with a way to clear it. */
@Composable
private fun CustomDateRow(dueDate: LocalDate, onClear: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val formatted = dueDate.format(formatter)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatted,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_clear_date),
                )
            }
        }
    }
}

/**
 * `Priority` as an Expressive **connected** button group: one continuous rounded
 * container whose selected item is a solid filled pill, the shape the reference shows.
 *
 * ## Why this is a Row of ToggleButtons and not `ButtonGroup { toggleableItem(...) }`
 *
 * `ButtonGroup`'s scope API cannot express the connected variant. `toggleableItem`
 * hardcodes its own `ToggleButton` with no `shapes` argument, so every item comes out
 * as a standalone pill, and the group's default `horizontalArrangement` spaces items by
 * `ButtonGroupSmallTokens.BetweenSpace` — 12dp of visible gap, the *disconnected*
 * token. The two together are what rendered as three separate lozenges the last time
 * this was tried. The connected geometry lives in `ButtonGroupDefaults`
 * (`ConnectedSpaceBetween` — 2dp — plus `connectedLeadingButtonShapes`,
 * `connectedMiddleButtonShapes` and `connectedTrailingButtonShapes`, which share edges
 * between neighbours), and the only supported way to reach it is what the official
 * `SingleSelectConnectedButtonGroupSample` does: a `Row` spaced by
 * `ConnectedSpaceBetween`, holding `ToggleButton`s that pick their shape by position.
 * This follows that sample, minus its icons.
 *
 * ## There is no "None" item
 *
 * Un-set is the absence of a selection, not a selection named "None": a second tap on
 * the checked item clears it back to [Priority.NONE]. That also keeps the row to three
 * items, which is what lets it fit without any item being squeezed.
 */
@Composable
private fun PrioritySection(
    selected: Priority,
    onSelect: (Priority) -> Unit,
) {
    val choices = remember { Priority.entries.filter { it != Priority.NONE } }
    // requireNotNull pairs with the filter above: both state the same invariant, one
    // visibly, one in the type system. If NONE ever slipped back into `choices` the
    // build would fail here rather than render an empty button.
    val labels = choices.associateWith { stringResource(requireNotNull(it.labelRes)) }

    SheetSection(label = stringResource(R.string.label_priority)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            choices.forEachIndexed { index, priority ->
                val isSelected = priority == selected
                ToggleButton(
                    checked = isSelected,
                    // Tapping the checked item clears the choice; there is no
                    // separate item to tap for "none".
                    onCheckedChange = {
                        onSelect(if (isSelected) Priority.NONE else priority)
                    },
                    modifier = Modifier.weight(1f),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        choices.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Text(labels.getValue(priority))
                }
            }
        }
    }
}

/**
 * Which project the task is filed under, defaulting to the Inbox.
 *
 * Two ways in, for two different situations:
 *
 * - **Tap** opens the dropdown menu. This is the browsable path: it scrolls, and it lists
 *   every project, so it is how you find one by reading its name.
 * - **Press and hold** opens the drag picker: a short list that appears under your finger
 *   with the current choice already held, and a slide up or down moves the highlight
 *   until you lift. This is the fast path for the few projects you use constantly, and
 *   it exists because picking one should not require aiming at a menu row.
 *
 * The difference is not cosmetic. A tap has to be a menu because the finger arrives
 * somewhere unrelated and must then travel to a target; a held drag arrives *already on*
 * the target and only needs to nudge it, which is a far cheaper correction.
 *
 * ## Why this is a Surface and not the read-only TextField it used to be
 *
 * A read-only `TextField` looks right but is a text field: it carries its own long-press
 * gesture for selecting the text it displays, which would compete with the picker's
 * long-press. Two gestures racing for the same event make the result depend on dispatch
 * order rather than on what this code says, so the field is replaced by a plain `Surface`
 * with the same shape and colours. It is no longer editable in any sense, which is exactly
 * what it always was in fact — a display for a value chosen elsewhere.
 *
 * The Inbox is always the first entry and is the `null` `projectId` — see
 * `TaskFilters.asInbox`, which *defines* the Inbox as "belongs to no project".
 *
 * While [projects] is null this offers nothing beyond the Inbox, which is also what it
 * offers when the user simply has no projects. The two are indistinguishable to the user
 * and the difference does not matter: there is nothing to pick either way.
 */
@Composable
private fun ProjectSection(
    projects: List<Project>?,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val inboxLabel = stringResource(R.string.project_inbox)
    val selectedName = projects?.firstOrNull { it.id == selectedId }?.name ?: inboxLabel
    val selectedColor = projects?.firstOrNull { it.id == selectedId }?.color

    // The drag picker's rows, in the order they are drawn: Inbox first, then the
    // projects. Shared with the menu below so the two can never disagree about what the
    // choices are, only about how they are presented.
    val choices: List<Pair<String?, String>> =
        remember(projects, inboxLabel) {
            buildList {
                add(null to inboxLabel)
                projects.orEmpty().forEach { add(it.id to it.name) }
            }
        }
    val selectedIndex = choices.indexOfFirst { it.first == selectedId }.coerceAtLeast(0)

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    // Already in pixels: `containerSize` is a raw window size, not a dp measurement, so
    // putting it through the density would scale it a second time.
    val windowHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()

    // Where the drag is pointing, and the top edge the list was seated at when the
    // gesture began. Both are state because the popup is created *by* the gesture
    // starting: the drag handler has to publish the list's position for the popup to
    // read, and the two must agree exactly or the highlight drifts away from the finger.
    var dragTarget by remember { mutableIntStateOf(selectedIndex) }
    var dragListTopPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var fieldBounds by remember { mutableStateOf<Rect?>(null) }

    val rowHeight = 56.dp
    val rowHeightPx = with(density) { rowHeight.toPx() }

    SheetSection(label = stringResource(R.string.label_project)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            ProjectField(
                name = selectedName,
                color = selectedColor,
                modifier = Modifier
                    .fillMaxWidth()
                    // The anchor for the menu, as well as the node the drag positions
                    // are measured against: the gesture's coordinates are relative to
                    // this node and the popups are placed in window coordinates, so
                    // this bounds value is the offset between the two.
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .onGloballyPositioned { fieldBounds = it.boundsInWindow() }
                    .pointerInput(choices.size, selectedIndex) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Long-press first. If the finger lifts before the threshold
                            // this returns null — but *because the finger already went
                            // up*, the up event having been consumed by this very call.
                            // So the tap case cannot wait for another up: that would
                            // block until some later gesture and open the menu at the
                            // wrong moment. It reads the state off the current event,
                            // which is the one that ended the press.
                            val longPress = awaitLongPressOrCancellation(down.id)
                            if (longPress == null) {
                                // A plain tap: open the browsable menu.
                                if (currentEvent.changes.any { it.id == down.id && !it.pressed }) {
                                    expanded = true
                                }
                            } else {
                                val fieldTop = fieldBounds?.top ?: 0f
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                dragTarget = selectedIndex
                                // Seated once, from where the finger actually landed, and
                                // then held constant: `selectedIndex` cannot change during
                                // the gesture, so recomputing it per move would only risk
                                // the popup and the hit test drifting apart.
                                dragListTopPx = dragListTop(
                                    anchorFingerY = down.position.y + fieldTop,
                                    selectedIndex = selectedIndex,
                                    itemCount = choices.size,
                                    itemHeight = rowHeightPx,
                                    windowHeight = windowHeightPx,
                                )
                                dragging = true
                                // Follow the finger until it lifts. Consuming the changes
                                // is what stops the sheet behind from scrolling or being
                                // dragged down to dismiss while the picker is in use.
                                drag(longPress.id) { change ->
                                    change.consume()
                                    val index = dragTargetIndex(
                                        fingerY = change.position.y + fieldTop,
                                        listTop = dragListTopPx,
                                        itemHeight = rowHeightPx,
                                        itemCount = choices.size,
                                    )
                                    if (index != dragTarget) {
                                        dragTarget = index
                                        haptics.performHapticFeedback(
                                            HapticFeedbackType.SegmentFrequentTick
                                        )
                                    }
                                }
                                // Released: commit whatever is highlighted. A drag that
                                // ends anywhere has still been aiming at something, so
                                // there is no cancel path and the field is never left
                                // showing a value nobody chose.
                                choices.getOrNull(dragTarget)?.let { onSelect(it.first) }
                                dragging = false
                            }
                        }
                    },
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                choices.forEach { (id, name) ->
                    val color = projects?.firstOrNull { it.id == id }?.color
                    DropdownMenuItem(
                        text = { Text(name) },
                        // Supplied on every row, coloured or not, and carrying the tick as
                        // well as the dot: a leading glyph that is present on only some
                        // rows makes the labels step in and out of line, and a menu that
                        // does not mark the current value disagrees with the drag picker
                        // directly above it. See [ProjectRowLeading], which both use.
                        leadingIcon = {
                            ProjectRowLeading(selected = id == selectedId, color = color)
                        },
                        onClick = {
                            onSelect(id)
                            expanded = false
                        },
                    )
                }
            }
        }

        if (dragging) {
            ProjectDragList(
                choices = choices,
                projects = projects,
                highlighted = dragTarget,
                selectedIndex = selectedIndex,
                rowHeight = rowHeight,
                // Sized from the field it drops from rather than a fixed width, so the
                // two line up on any screen instead of only on the one this was written
                // against.
                widthPx = fieldBounds?.width ?: 0f,
                leftPx = fieldBounds?.left ?: 0f,
                topPx = dragListTopPx,
            )
        }
    }
}

/**
 * The project field: a display for a value chosen elsewhere, drawn to match the sheet's
 * text fields without being one. See [ProjectSection] for why it is not a `TextField`.
 */
@Composable
private fun ProjectField(
    name: String,
    color: Int?,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            color?.let {
                ProjectDot(it)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The list the held gesture slides over.
 *
 * A `Popup` rather than a composable inside the sheet, for the reason the reference
 * interaction works at all: the sheet scrolls and can be dragged down to dismiss, and both
 * of those read vertical drags. Drawing the list in a window of its own takes it out of
 * that hierarchy, so the same gesture that moves the highlight cannot also scroll the form
 * or close the sheet on its way past.
 *
 * It is positioned from precomputed pixels rather than from an anchor node, because the
 * hit testing lives in the gesture and uses the same numbers: `[topPx]` is exactly the
 * value `dragTargetIndex` was given. Recomputing the position here — from an anchor
 * bounds, say — would be a second source of truth for the same rectangle, and any
 * disagreement between the two would show up as the highlight sliding out from under the
 * finger.
 */
@Composable
private fun ProjectDragList(
    choices: List<Pair<String?, String>>,
    projects: List<Project>?,
    highlighted: Int,
    selectedIndex: Int,
    rowHeight: Dp,
    widthPx: Float,
    leftPx: Float,
    topPx: Float,
) {
    val widthDp = with(LocalDensity.current) { widthPx.toDp() }
    val positionProvider = remember(leftPx, topPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset = IntOffset(leftPx.roundToInt(), topPx.roundToInt())
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Column(modifier = Modifier.width(widthDp)) {
                choices.forEachIndexed { index, (id, name) ->
                    val color = projects?.firstOrNull { it.id == id }?.color
                    val isHighlighted = index == highlighted
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .background(
                                if (isHighlighted) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The tick marks the value that was already chosen, so the list
                        // shows where you started as well as where you are.
                        ProjectRowLeading(selected = index == selectedIndex, color = color)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The slot at the start of a project row: a tick when this row is the current value, and
 * the project's colour dot beside it when it has one.
 *
 * Both the browsable menu and the drag picker draw their rows through this, which is the
 * point — they are the same list seen two ways, and when they rendered it separately the
 * Inbox row's label sat a dot's width to the left of every other row's, because the slot
 * simply disappeared on the one row with no colour to draw.
 *
 * Every part of the slot holds its width whether or not it draws anything, so all four
 * labels start at the same x. The tick is the leftmost element rather than the dot for
 * the same reason: putting it after the dot would indent the current row's label by the
 * tick's width instead.
 *
 * The slot ends at the dot; the gap to the label belongs to the caller, because the menu
 * and the drag list space their contents differently and each already has its own.
 */
@Composable
private fun ProjectRowLeading(selected: Boolean, color: Int?) {
    Row(
        modifier = Modifier.width(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.size(12.dp)) {
            color?.let { ProjectDot(it) }
        }
    }
}

/**
 * A project's colour as a dot.
 *
 * `Project.color` is a packed ARGB int and is nullable — a project created without one
 * gets no dot rather than a default colour, because a default would imply a choice the
 * user never made.
 */
@Composable
private fun ProjectDot(color: Int) {
    Surface(
        color = Color(color),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.size(12.dp),
        content = {},
    )
}

/**
 * A captioned group of controls.
 *
 * The caption is bound to its own content here rather than being the first child of the
 * sheet's column: the column spaces its children by 20dp, which is the gap *between*
 * sections, and a caption that merely sat in that column would get the same 20dp to the
 * controls it labels as to the section above it. Wrapping the pair keeps the two
 * distances independent — 4dp from caption to content, 20dp between sections.
 *
 * @param trailing an optional action pinned to the end of the caption row. It is offset
 *   to the full width of the row so the caption stays left-aligned with the controls
 *   below it.
 */
@Composable
private fun SheetSection(
    label: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
        content()
    }
}

private val DueDatePreset.labelRes: Int
    get() = when (this) {
        DueDatePreset.TODAY -> R.string.due_today
        DueDatePreset.TOMORROW -> R.string.due_tomorrow
        DueDatePreset.NEXT_WEEK -> R.string.due_next_week
    }

/**
 * The user-facing name of a priority, or `null` for [Priority.NONE].
 *
 * NONE is deliberately unlabelled. It is the absence of a choice rather than a choice
 * named "None", so nothing renders it: the group shows it by nothing being checked, and
 * a second tap on the checked button returns to it. A string for it would be one no user
 * can ever see, in every locale the app ships.
 */
private val Priority.labelRes: Int?
    get() = when (this) {
        Priority.NONE -> null
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
    }
