package com.doitsh.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The state behind a task list page: one flow of tasks, plus the two mutations every
 * task row can perform.
 *
 * The Inbox and Today pages are the same screen over different data, so the parts that
 * do not depend on *which* data live here and the subclasses supply only the source.
 * Hilt cannot inject a `(TaskRepository) -> Flow<List<Task>>` without an assisted-factory
 * ceremony that would buy nothing here, and a single ViewModel with a mode parameter
 * would put the choice of list in the UI layer rather than next to the query it names.
 */
abstract class TaskListViewModel(
    private val repository: TaskRepository,
    private val source: Flow<List<Task>>,
) : ViewModel() {

    /**
     * `null` until Room has actually emitted, so the screen can tell "still loading"
     * apart from "genuinely empty" — an `initialValue = emptyList()` would render the
     * empty state for a frame on every cold start.
     *
     * `WhileSubscribed(5000)` also gives the Today page its refresh: the filter is
     * evaluated when the flow restarts, so coming back to the page re-reads the clock
     * and a task that became due overnight appears without a restart.
     */
    val tasks: StateFlow<List<Task>?> by lazy {
        source.stateIn(
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

/** The Inbox: open tasks that belong to no project. */
@HiltViewModel
class InboxViewModel @Inject constructor(
    repository: TaskRepository
) : TaskListViewModel(repository, repository.getInboxTasks())

/** The Today page: open tasks due today or earlier. */
@HiltViewModel
class TodayViewModel @Inject constructor(
    repository: TaskRepository
) : TaskListViewModel(repository, repository.getTodayTasks())
