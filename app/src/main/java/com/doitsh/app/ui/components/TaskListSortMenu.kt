package com.doitsh.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.doitsh.app.R
import com.doitsh.app.data.settings.SortDirection
import com.doitsh.app.data.settings.TaskGrouping
import com.doitsh.app.data.settings.TaskListOptions
import com.doitsh.app.data.settings.TaskSort

/**
 * The top bar's sort and grouping menu.
 *
 * Three independent questions, drawn as three labelled groups in one menu rather than as
 * a submenu each: the list is a few hundred tasks, every option is one tap away, and a
 * nested menu would make changing both grouping and sort a two-level dance. The headings
 * are section labels rather than items — they are not tappable, so they carry no selected
 * state.
 *
 * The anchor is the button itself, inside a [Box]: an anchored `DropdownMenu` is
 * positioned relative to its parent, so the popup opens under the icon rather than over
 * the whole screen. The top bar's `enterAlwaysScrollBehavior` does not interfere — an
 * open popup takes the touch that would have scrolled the bar, and is dismissed first.
 */
@Composable
fun TaskListSortMenu(
    options: TaskListOptions,
    onUpdate: (TaskListOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.action_sort_and_group),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MenuSectionLabel(stringResource(R.string.menu_group_by))
            TaskGrouping.entries.forEach { grouping ->
                MenuChoice(
                    label = stringResource(grouping.labelRes),
                    selected = options.grouping == grouping,
                    onClick = {
                        onUpdate(options.copy(grouping = grouping))
                        expanded = false
                    },
                )
            }

            HorizontalDivider()

            MenuSectionLabel(stringResource(R.string.menu_sort_by))
            TaskSort.entries.forEach { sort ->
                MenuChoice(
                    label = stringResource(sort.labelRes),
                    selected = options.sort == sort,
                    onClick = {
                        onUpdate(options.copy(sort = sort))
                        expanded = false
                    },
                )
            }

            HorizontalDivider()

            MenuSectionLabel(stringResource(R.string.menu_direction))
            SortDirection.entries.forEach { direction ->
                MenuChoice(
                    label = stringResource(direction.labelRes),
                    selected = options.direction == direction,
                    onClick = {
                        onUpdate(options.copy(direction = direction))
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * A heading inside the menu, not an item: no ripple, no tap target, so it cannot be
 * mistaken for an option. The padding matches [DropdownMenuItem]'s own horizontal inset.
 */
@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

/**
 * One option. The tick is a trailing icon rather than a leading radio because the menu mixes
 * three groups in one column and a radio column would imply one choice across all three.
 *
 * material3's `DropdownMenuItem` has no `selected` parameter, so the check mark is drawn
 * here and the state is exposed to accessibility through semantics.
 */
@Composable
private fun MenuChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = {
            if (selected) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null)
            }
        },
        modifier = Modifier.semantics { this.selected = selected },
    )
}

private val TaskGrouping.labelRes: Int
    get() = when (this) {
        TaskGrouping.NONE -> R.string.grouping_none
        TaskGrouping.DUE_DATE -> R.string.grouping_due_date
        TaskGrouping.PRIORITY -> R.string.grouping_priority
    }

private val TaskSort.labelRes: Int
    get() = when (this) {
        TaskSort.DUE_DATE -> R.string.sort_due_date
        TaskSort.PRIORITY -> R.string.sort_priority
        TaskSort.TITLE -> R.string.sort_title
        TaskSort.CREATED_AT -> R.string.sort_created_at
    }

private val SortDirection.labelRes: Int
    get() = when (this) {
        SortDirection.ASCENDING -> R.string.direction_ascending
        SortDirection.DESCENDING -> R.string.direction_descending
    }
