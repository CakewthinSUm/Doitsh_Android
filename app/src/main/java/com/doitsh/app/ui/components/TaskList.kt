package com.doitsh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doitsh.app.R
import com.doitsh.app.domain.model.Task

/**
 * A scrollable list of tasks, shared by the Inbox and Today pages.
 *
 * The two pages differ only in which tasks they are handed and what they say when there
 * are none, so they differ only in this call's arguments. The rows, the swipe-to-dismiss
 * behaviour, the loading state and the empty-state layout are one implementation.
 *
 * @param tasks `null` while the database has not emitted yet, so the list can tell
 *   "still loading" apart from "genuinely empty" — see [com.doitsh.app.ui.screens.TaskListViewModel].
 * @param emptyTitle what to say when there are no tasks; the pages pass their own copy
 *   because "your inbox is empty" and "nothing due today" are not the same statement.
 * @param contentPadding the surrounding scaffold's padding, so the list clears the top
 *   bar and the navigation container. The extra 80dp at the bottom is FAB clearance:
 *   both pages are top-level, so both have the FAB menu over their last row.
 */
@Composable
fun TaskList(
    tasks: List<Task>?,
    contentPadding: PaddingValues,
    emptyTitle: String,
    emptySubtitle: String,
    onToggle: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            tasks == null -> item { LoadingState() }
            tasks.isEmpty() -> item { EmptyState(emptyTitle, emptySubtitle) }
            else -> items(tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { onToggle(task) },
                    onDelete = { onDelete(task) }
                )
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

@Composable
private fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete() },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        // The modern ListItem overload takes the headline as its trailing `content`
        // lambda; `headlineContent` belongs to the older overload, which has no
        // `shapes`/`elevation` and so cannot express the Expressive container.
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            // An opaque container is required here: the swipe background is painted
            // behind the row, so a transparent ListItem would show it through.
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shapes = ListItemDefaults.shapes(),
            leadingContent = {
                // `content` is the one required lambda on ToggleButton (there is no
                // default), so the icon goes there rather than in the `icon` slot.
                ToggleButton(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
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
                            )
                        )
                    }
                )
            },
            supportingContent = task.description?.let { description ->
                {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete_task)
                    )
                }
            }
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
