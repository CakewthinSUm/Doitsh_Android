package com.doitsh.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doitsh.app.R
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    /**
     * `null` until Room has actually emitted, so the screen can tell "still loading"
     * apart from "genuinely empty" — an `initialValue = emptyList()` would render the
     * empty state for a frame on every cold start.
     */
    val tasks: StateFlow<List<Task>?> by lazy {
        repository.getAllTasks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
        }
    }
}

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
            tasks!!.isEmpty() -> item { EmptyState() }
            else -> items(tasks!!, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onDelete = { viewModel.deleteTask(task) }
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
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.home_empty_subtitle),
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
