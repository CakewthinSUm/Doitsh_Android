package com.doitsh.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.doitsh.app.R
import com.doitsh.app.data.repository.TaskPage
import com.doitsh.app.domain.model.Task
import com.doitsh.app.ui.components.TaskList

/**
 * A task list page — Today or Inbox, chosen by [page].
 *
 * The two pages are one screen over different tasks: they differ in which tasks they show,
 * what they say when there are none, and nothing else. All three come from [page], so the
 * pages cannot drift apart in their empty state, padding or gestures.
 *
 * Filtering is not here either: it is a data rule shared by both data modes and by both
 * pages, so it lives next to the grouping in `TaskFilters.kt`.
 *
 * @param onEdit opens the task in the edit sheet, which the app shell hosts because
 *   creating and editing go through one host and one ViewModel.
 */
@Composable
fun TaskListScreen(
    page: TaskPage,
    contentPadding: PaddingValues,
    onEdit: (Task) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val groups by viewModel.groups(page).collectAsState()

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
        onToggle = viewModel::toggleTask,
        onDelete = viewModel::deleteTask,
        onEdit = onEdit,
    )
}
