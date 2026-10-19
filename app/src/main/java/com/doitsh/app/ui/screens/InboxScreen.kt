package com.doitsh.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.doitsh.app.data.repository.TaskPage
import com.doitsh.app.domain.model.Task

/**
 * The Inbox: tasks that have not been filed into a project yet.
 *
 * One page of the home pager, beside Today and Projects — see `DoitshApp`'s `HomePager`.
 * A task captured without a project is the case that needs no decision from the user, and
 * everything else is a refinement of it, which is why nothing here asks for one. Completed
 * tasks stay — see [com.doitsh.app.data.repository.TaskPage.INBOX] — and the grouping and
 * sorting come from the top bar's menu.
 *
 * The ViewModel exists only so this page gets its own instance — see [TaskListViewModel].
 *
 * @param onEdit opens the task in the edit sheet. The sheet belongs to the app shell
 *   rather than to this screen, so that creating and editing go through one host and one
 *   ViewModel — see `DoitshApp`.
 */
@Composable
fun InboxScreen(
    contentPadding: PaddingValues,
    onEdit: (Task) -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsState()

    TaskListScreen(
        page = TaskPage.INBOX,
        contentPadding = contentPadding,
        groups = groups,
        onToggle = viewModel::toggleTask,
        onDelete = viewModel::deleteTask,
        onEdit = onEdit,
    )
}
