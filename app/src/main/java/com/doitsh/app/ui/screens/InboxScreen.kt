package com.doitsh.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.doitsh.app.R
import com.doitsh.app.domain.model.Task
import com.doitsh.app.ui.components.TaskList

/**
 * The Inbox: tasks that have not been filed into a project yet.
 *
 * One page of the home pager, beside Today and Projects — see `DoitshApp`'s `HomePager`.
 * A task captured without a project is the case that needs no decision from the user, and
 * everything else is a refinement of it, which is why nothing here asks for one.
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

    TaskList(
        groups = groups,
        contentPadding = contentPadding,
        emptyTitle = stringResource(R.string.inbox_empty_title),
        emptySubtitle = stringResource(R.string.inbox_empty_subtitle),
        onToggle = viewModel::toggleTask,
        onDelete = viewModel::deleteTask,
        onEdit = onEdit,
    )
}
