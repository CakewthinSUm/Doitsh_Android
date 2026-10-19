package com.doitsh.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.doitsh.app.data.repository.TaskPage
import com.doitsh.app.domain.model.Task

/**
 * Today: open tasks due today or earlier.
 *
 * A deadline view, so a task with no due date never appears here — see
 * [com.doitsh.app.data.repository.TaskPage.TODAY]. How the rows are grouped and sorted is
 * the user's choice from the top bar; the screen itself is the shared [TaskListScreen].
 *
 * The ViewModel exists only so this page gets its own instance — see [TaskListViewModel].
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

    TaskListScreen(
        page = TaskPage.TODAY,
        contentPadding = contentPadding,
        groups = groups,
        onToggle = viewModel::toggleTask,
        onDelete = viewModel::deleteTask,
        onEdit = onEdit,
    )
}
