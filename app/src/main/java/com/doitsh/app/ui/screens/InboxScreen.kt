package com.doitsh.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.doitsh.app.R
import com.doitsh.app.ui.components.TaskList

/**
 * The Inbox: tasks that have not been filed into a project yet.
 *
 * This is the app's landing page — the first destination in the navigation container —
 * because a task captured without a project is the case that needs no decision from the
 * user, and everything else is a refinement of it.
 */
@Composable
fun InboxScreen(
    contentPadding: PaddingValues,
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
    )
}
