package com.doitsh.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.repository.TaskRepository
import com.doitsh.app.ui.components.EmptyState
import com.doitsh.app.ui.components.TaskCard
import com.doitsh.app.ui.theme.SpacingTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {
    val tasks by lazy {
        repository.getAllTasks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel = hiltViewModel()
) {
    val allTasks by viewModel.tasks.collectAsState()
    val inboxTasks = allTasks.filter { it.projectId == null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = SpacingTokens.Default,
            bottom = SpacingTokens.DisplayXXL,
            start = SpacingTokens.Default,
            end = SpacingTokens.Default
        ),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.Small)
    ) {
        if (inboxTasks.isEmpty()) {
            item {
                EmptyState(
                    title = "Inbox is empty",
                    subtitle = "Tasks without a project will appear here"
                )
            }
        } else {
            itemsIndexed(inboxTasks, key = { _, task -> task.id }) { index, task ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                ) {
                    TaskCard(
                        task = task,
                        onToggle = { viewModel.toggleTask(task) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
    }
}
