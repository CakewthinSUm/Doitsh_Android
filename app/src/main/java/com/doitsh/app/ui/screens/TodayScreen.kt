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
 * Today: open tasks due today or earlier, most overdue first.
 *
 * Unlike the Inbox — which shows everything that has not been filed — this page is a
 * deadline view, so a task with no due date never appears here. The list is sorted by
 * due date, so the tasks that have been waiting longest are the ones at the top.
 *
 * @param onEdit opens the task in the edit sheet, which is hosted by the app shell.
 */
@Composable
fun TodayScreen(
    contentPadding: PaddingValues,
    onEdit: (Task) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsState()

    TaskList(
        groups = groups,
        contentPadding = contentPadding,
        emptyTitle = stringResource(R.string.today_empty_title),
        emptySubtitle = stringResource(R.string.today_empty_subtitle),
        onToggle = viewModel::toggleTask,
        onDelete = viewModel::deleteTask,
        onEdit = onEdit,
    )
}
