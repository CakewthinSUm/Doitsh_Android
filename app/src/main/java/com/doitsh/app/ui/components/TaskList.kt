package com.doitsh.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doitsh.app.R
import com.doitsh.app.data.repository.TaskGroup
import com.doitsh.app.data.repository.TaskSection
import com.doitsh.app.domain.model.Priority
import com.doitsh.app.domain.model.Task
import com.doitsh.app.ui.theme.priorityHigh
import com.doitsh.app.ui.theme.priorityLow
import com.doitsh.app.ui.theme.priorityMedium
import com.doitsh.app.ui.theme.taskRowContainer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A scrollable list of tasks, shared by the Inbox and Today pages.
 *
 * The two pages differ only in which tasks they are handed and what they say when there
 * are none, so they differ only in this call's arguments. The rows, the swipe-to-dismiss
 * behaviour, the loading state and the empty-state layout are one implementation.
 *
 * ## One container per section
 *
 * Rows are no longer independent cards: tasks arrive grouped by due date
 * ([TaskGroup]) and each group is drawn as a single segmented container — rounded top on
 * its first row, rounded bottom on its last, a 2dp seam between them. The shapes come
 * from [ListItemDefaults.segmentedShapes], which is the same rule the reference
 * implementation uses, rather than from hand-rolled corner maths here.
 *
 * Segments, not a merged row list, because every row must keep its own
 * [SwipeToDismissBox]: merging the group into one surface would mean one dismiss state
 * for a whole section, and a swipe would no longer mean "delete this task".
 *
 * @param groups `null` while the database has not emitted yet, so the list can tell
 *   "still loading" apart from "genuinely empty" — see [com.doitsh.app.ui.screens.TaskListViewModel].
 * @param emptyTitle what to say when there are no tasks; the pages pass their own copy
 *   because "your inbox is empty" and "nothing due today" are not the same statement.
 * @param contentPadding the surrounding scaffold's padding, so the list clears the top
 *   bar and the navigation container. The extra 80dp at the bottom is FAB clearance:
 *   both pages are top-level, so both have the FAB menu over their last row.
 * @param onEdit called when a row is tapped. Tapping the row is the one gesture left
 *   that does nothing else — the checkbox completes and the horizontal swipe deletes —
 *   and it is the natural way to open the task itself.
 */
@Composable
fun TaskList(
    groups: List<TaskGroup>?,
    contentPadding: PaddingValues,
    emptyTitle: String,
    emptySubtitle: String,
    onToggle: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Read once per composition of the list, not per row: every row's date label and
    // its "is this today/overdue" colour have to agree with the section it is drawn
    // under, and two LocalDate.now() calls either side of midnight could disagree.
    val today = remember { LocalDate.now() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
            start = 16.dp,
            end = 16.dp
        ),
        // No `verticalArrangement`: a LazyColumn applies its arrangement between *every*
        // pair of items, which would put the same gap inside a group as between groups
        // and break the container apart. The two distances are set per item instead —
        // 2dp between the segments of one group (the seam), 12dp above every other
        // group's header.
        verticalArrangement = Arrangement.Top
    ) {
        when {
            groups == null -> item { LoadingState() }
            groups.isEmpty() -> item { EmptyState(emptyTitle, emptySubtitle) }
            else -> groups.forEachIndexed { groupIndex, group ->
                // An ungrouped list ("Group by: None") is one container with no caption —
                // a header reading "None" or the sort field would be noise. It is still a
                // group of tasks, because the segmented shapes need a count to round the
                // top and bottom of.
                val section = group.section
                if (section != null) {
                    item(key = "section:${section.name}") {
                        SectionHeader(
                            section = section,
                            count = group.tasks.size,
                            topPadding = if (groupIndex == 0) 0.dp else 12.dp,
                        )
                    }
                }
                itemsIndexed(
                    items = group.tasks,
                    key = { _, task -> task.id },
                ) { index, task ->
                    TaskRow(
                        task = task,
                        today = today,
                        shapes = ListItemDefaults.segmentedShapes(
                            index = index,
                            count = group.tasks.size,
                        ),
                        // The seam above every segment but the first of its group.
                        topGap = if (index == 0) 4.dp else ListItemDefaults.SegmentedGap,
                        onToggle = { onToggle(task) },
                        onDelete = { onDelete(task) },
                        onEdit = { onEdit(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 96.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The caption above a group: its name, and how many tasks are in it.
 *
 * The count is part of the label rather than a separate badge because it is read as one
 * phrase ("Overdue, 2"), and a badge would give it a shape that implies it is tappable.
 * `labelMedium` matches the other small section captions in the app — see
 * `TaskEditorSheet.SheetSection`.
 */
@Composable
private fun SectionHeader(section: TaskSection, count: Int, topPadding: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = topPadding, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(section.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
    }
}

/**
 * One task, as one segment of its group.
 *
 * Not `SegmentedListItem` (absent from material3 1.5.0-beta01) and not plain [ListItem]
 * either: the former does not exist here, and the latter's 72dp two-line minimum cannot
 * be padded away. The segmented *shapes* it would have used
 * ([ListItemDefaults.segmentedShapes]) are kept, and they are the whole of what makes a
 * group read as one container.
 *
 * The row is two lines when the task has a due date — title, then a calendar icon and
 * the date underneath — and one line when it does not, so a group of undated tasks does
 * not carry an empty second line in every row.
 */
@Composable
private fun TaskRow(
    task: Task,
    today: LocalDate,
    shapes: ListItemShapes,
    topGap: Dp,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val deleteLabel = stringResource(R.string.action_delete_task)
    val editLabel = stringResource(R.string.action_edit_task)
    val dismissState = rememberSwipeToDismissBoxState()

    // Delete is part of the swipe, not part of the row: there is no trailing button at
    // rest, and the trash can appears only once the row has started to move.
    //
    // `dismissDirection` is derived from the sign of the drag offset, so it changes
    // twice per gesture — start, and settle back — and not once per frame. Reading it
    // through `derivedStateOf` is what keeps that true: subscribing to it directly would
    // invalidate this row on every offset change and recompose it for the whole drag.
    val isSwiping by remember(dismissState) {
        derivedStateOf { dismissState.dismissDirection != SwipeToDismissBoxValue.Settled }
    }
    val revealAlpha by animateFloatAsState(
        targetValue = if (isSwiping) 1f else 0f,
        label = "task-row-delete-reveal",
    )

    // A swipe is invisible to a screen reader, and it used to have a trailing button as
    // its accessible equivalent. With that button gone, delete is published as a custom
    // accessibility action on the row instead, so it is still reachable without the
    // gesture.
    //
    // The row also needs a label of its own, and that is not cosmetic: a semantics node
    // whose only property is a custom action is not "important for accessibility", so
    // Compose drops it from the accessibility tree entirely and the action goes with it
    // (verified by dumping the tree — the node is absent until it carries a label). The
    // label is the same thing the row shows — title, then the due date — so the merged
    // node announces as one task rather than as loose fragments.
    //
    // `mergeDescendants` stops at the checkbox, which merges itself, so ticking stays a
    // separate stop with its own "Not completed" description.
    //
    // The row's click is published by the `clickable` below, with its label passed there
    // rather than redeclared here: a second `onClick` in this block would be a duplicate
    // action on the same merged node. Without the label a screen reader would announce
    // "double tap to activate", leaving the user to guess what activating a task does.
    val rowDescription = buildString {
        append(task.title)
        task.dueDate?.let {
            append(", ")
            append(formatDueDate(it.toLocalDate(), today))
        }
    }
    val rowSemantics = Modifier.semantics(mergeDescendants = true) {
        this.contentDescription = rowDescription
        customActions = listOf(
            CustomAccessibilityAction(
                label = deleteLabel,
                action = {
                    onDelete()
                    true
                },
            )
        )
    }

    // The seam is outside the swipe container, so the red dismiss background spans the
    // row itself and never shows through the gap to its neighbour: `backgroundContent`
    // is drawn behind the row at rest too, and a background that covered the gap would
    // put a 2dp red line between every pair of rows until something was swiped.
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.padding(top = topGap),
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete() },
        backgroundContent = {
            // The dismiss background has to carry the segment's own shape: it is
            // painted behind the row, and the row is no longer a standalone rounded
            // card, so a single rounded rectangle here would show square red corners
            // past the container's rounded ends. `clip` rather than `background(shape)`
            // so the icon is clipped by the same corner as it slides out.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shapes.shape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                // 20dp in a 48dp row, inset 16dp from the edge — the same 24dp centre
                // a 48dp button would have, so the icon appears where a trailing action
                // would be rather than hugging the corner.
                //
                // Fades in with the drag instead of being there from the first frame: at
                // rest the red is entirely covered by the row, so fading the icon is
                // equivalent to fading it against the row, and a row that has barely
                // moved should not already claim to be deletable.
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .alpha(revealAlpha)
                        .padding(end = 16.dp)
                        .size(20.dp),
                )
            }
        }
    ) {
        // A hand-built row, not a `ListItem`.
        //
        // `ListItem` measures itself against a component minimum — 72dp for two lines,
        // 88dp for three — as `max(constraints.minHeight, token)` in its intrinsic
        // measure policy. No `contentPadding` can go below that: shrinking the padding
        // only moves the content around inside a row that stays 72dp tall, which is
        // visible as a row whose checkbox sits high and leaves dead air under the date.
        // The component's layout is the thing that has to go, not its padding.
        //
        // What is lost is `ListItem`'s leading/supporting/trailing slot arrangement,
        // which this row does not use: it is a checkbox, a two-line text column and a
        // delete button, in that order. What is kept is the segmented shape token, so
        // the group still reads as one container.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Opaque: the swipe background is painted behind this row.
                //
                // The top of the container ramp, not the bottom. At `surfaceContainerLow`
                // the row sits at 1.05:1 against the page background (measured on
                // device), which is below the point where the container is visible as a
                // container at all — and a segmented group whose segments cannot be seen
                // is just a list with unexplained gaps. `surfaceContainerHighest` is
                // 1.24:1.
                .background(
                    color = MaterialTheme.colorScheme.taskRowContainer,
                    shape = shapes.shape,
                )
                // The tap target is the row, not the swipe container: a `clickable` on
                // the `SwipeToDismissBox` would compete with the horizontal dismiss
                // gesture for the same pointer stream. Here the dismiss gesture owns
                // horizontal drags, so a tap that stays put is unambiguously a tap.
                //
                // After the background, not before: `clickable` draws its ripple at its
                // own position in the chain, so a background below it would paint over
                // the ripple and the row would appear not to react at all. The `clip`
                // before it keeps the ripple inside the group's rounded end.
                .clip(shapes.shape)
                .clickable(onClickLabel = editLabel, onClick = onEdit)
                .then(rowSemantics)
                // Horizontal only: the tall element is the checkbox's 48dp touch
                // target, so the row is already as tall as the platform requires and
                // vertical padding would only add to it. The 16dp end inset replaces
                // the air the delete button used to provide, so a long title still
                // stops short of the container's edge.
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A fixed 32dp square inside a 48dp touch target.
            //
            // `buttonSize` cannot do this on its own: it is applied as
            // `defaultMinSize(minWidth = 0, minHeight = size)`, so width still comes from
            // the content padding, and a zero-padded 18dp glyph would render as an 18x32
            // capsule rather than a circle. Sizing the modifier is the only way to get a
            // square. `minimumInteractiveComponentSize` stays outside the size, so the
            // row is as small as the visuals allow and as tappable as the platform
            // requires.
            //
            // The unchecked container is the *bottom* of the surface ramp rather than
            // material3's default (`surfaceContainer`, one step below the row but only
            // 1.14:1 against it). On a `surfaceContainerHighest` row the ramp bottom is
            // the step that reads as a well: white on the light card, near-black on the
            // dark one — the same "this is a slot, not a label" signal at both ends.
            ToggleButton(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(32.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                content = {
                    Icon(
                        imageVector = if (task.isCompleted) {
                            Icons.Filled.Check
                        } else {
                            Icons.Outlined.Circle
                        },
                        contentDescription = stringResource(
                            if (task.isCompleted) {
                                R.string.task_completed
                            } else {
                                R.string.task_not_completed
                            }
                        ),
                        modifier = Modifier.size(18.dp),
                    )
                }
            )

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                // Priority as a leading accent rather than a word: the three levels are
                // ordered, so a colour reads faster than "High" and survives a long
                // title being truncated. NONE draws nothing at all — un-set is the
                // absence of a choice, the same rule the new-task sheet's button group
                // follows.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriorityBar(priority = task.priority)
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                task.dueDate?.let { dueDate ->
                    DueDateLabel(
                        dueDate = dueDate,
                        today = today,
                        isCompleted = task.isCompleted,
                    )
                }
            }

            // No trailing delete button: the row's own affordance is the swipe, and a
            // permanent trash can on every row both repeated the reveal and made the
            // row read as a control strip rather than as a line of text. The action is
            // still reachable without the gesture through the custom accessibility
            // action set on the swipe container above.
        }
    }
}

/**
 * The priority accent at the start of a title.
 *
 * It reserves its width even for [Priority.NONE], so every title in a group starts at
 * the same x whether or not the task has a priority. `Spacer` rather than a transparent
 * `Box` for the empty case: nothing is drawn either way, but only one of them is
 * obviously meant to be invisible.
 */
@Composable
private fun PriorityBar(priority: Priority) {
    val color: Color? = when (priority) {
        Priority.NONE -> null
        Priority.LOW -> MaterialTheme.colorScheme.priorityLow
        Priority.MEDIUM -> MaterialTheme.colorScheme.priorityMedium
        Priority.HIGH -> MaterialTheme.colorScheme.priorityHigh
    }

    if (color == null) {
        Spacer(Modifier.width(PriorityBarWidth))
    } else {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(width = 3.dp, height = 16.dp)
                .background(color, MaterialTheme.shapes.extraSmall)
        )
    }
}

private val PriorityBarWidth = 11.dp

/**
 * The due date, under the title: a calendar glyph and the date itself.
 *
 * The icon is what makes the line read as a date rather than as a stray second line of
 * text — "Tomorrow" on its own could be a subtitle. It is `onSurfaceVariant` at the
 * same weight as the text, so it labels the value instead of competing with it.
 *
 * Colour carries the only urgency the row has: overdue in `error`, today in `primary`,
 * everything else in the supporting text colour. Completed tasks are never coloured even
 * when their date has passed — the deadline stopped mattering when the task was ticked.
 */
@Composable
private fun DueDateLabel(dueDate: LocalDateTime, today: LocalDate, isCompleted: Boolean) {
    val date = dueDate.toLocalDate()
    val color = when {
        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        date.isBefore(today) -> MaterialTheme.colorScheme.error
        date == today -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = formatDueDate(date, today),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * How a due date is written in a row.
 *
 * Relative wording for the days around now — that is the only thing anyone acts on —
 * and an absolute date for anything further out, where "in 4 days" is harder to place
 * in a week than "Fri, Oct 23".
 *
 * @param today passed in rather than read here so every row in one list agrees, and so
 *   the wording is a pure function.
 */
internal fun formatDueDate(date: LocalDate, today: LocalDate): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when (days) {
        0L -> "Today"
        1L -> "Tomorrow"
        -1L -> "Yesterday"
        in Long.MIN_VALUE..-2L -> "${-days} days overdue"
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

private val TaskSection.labelRes: Int
    get() = when (this) {
        TaskSection.OVERDUE -> R.string.section_overdue
        TaskSection.TODAY -> R.string.section_today
        TaskSection.LATER -> R.string.section_later
        TaskSection.NO_DATE -> R.string.section_no_date
        TaskSection.PRIORITY_HIGH -> R.string.section_priority_high
        TaskSection.PRIORITY_MEDIUM -> R.string.section_priority_medium
        TaskSection.PRIORITY_LOW -> R.string.section_priority_low
        TaskSection.PRIORITY_NONE -> R.string.section_priority_none
        TaskSection.COMPLETED -> R.string.section_completed
    }
