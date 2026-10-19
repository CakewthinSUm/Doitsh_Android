package com.doitsh.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.doitsh.app.R
import com.doitsh.app.data.repository.TaskGroup
import com.doitsh.app.data.repository.TaskPage
import com.doitsh.app.domain.model.Task
import com.doitsh.app.ui.components.TaskList

/**
 * A task list page: the Today or Inbox view over a list the caller has already derived.
 *
 * Today and Inbox differ in exactly two things — which tasks they show and what they say
 * when there are none — and both are decided by [page]. That is why this is one composable
 * with a parameter rather than two that happen to look alike: the two pages cannot drift
 * apart in their empty-state layout, their padding or their gestures.
 *
 * The filtering itself is not here: it is a data rule shared by both data modes, so it
 * lives next to the grouping in `TaskFilters.kt`.
 *
 * @param groups `null` while the data has not arrived yet — see [TaskListViewModel].
 * @param onEdit opens the task in the edit sheet, which the app shell hosts.
 */
@Composable
fun TaskListScreen(
    page: TaskPage,
    contentPadding: PaddingValues,
    groups: List<TaskGroup>?,
    onToggle: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit,
) {
    val (emptyTitle, emptySubtitle) = when (page) {
        TaskPage.TODAY -> stringResource(R.string.today_empty_title) to
            stringResource(R.string.today_empty_subtitle)
        TaskPage.INBOX -> stringResource(R.string.inbox_empty_title) to
            stringResource(R.string.inbox_empty_subtitle)
    }

    TaskList(
        groups = groups,
        contentPadding = contentPadding,
        emptyTitle = emptyTitle,
        emptySubtitle = emptySubtitle,
        onToggle = onToggle,
        onDelete = onDelete,
        onEdit = onEdit,
    )
}
