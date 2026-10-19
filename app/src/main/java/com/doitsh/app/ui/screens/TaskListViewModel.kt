package com.doitsh.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doitsh.app.data.repository.TaskGroup
import com.doitsh.app.data.repository.TaskPage
import com.doitsh.app.data.repository.forPage
import com.doitsh.app.data.repository.sections
import com.doitsh.app.data.settings.TaskListPreferences
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * The state behind a task list page: the grouped rows, plus the two mutations every task
 * row can perform.
 *
 * Today and Inbox are one screen over different tasks, so everything that does not depend
 * on *which* tasks live here; a subclass supplies only its [TaskPage]. Hilt cannot inject
 * a `TaskPage` without an assisted-factory ceremony that would buy nothing, and one
 * ViewModel shared by both pages is not an option either: they sit in the same navigation
 * entry's `ViewModelStore`, where `hiltViewModel<T>()` keys by type, so the second page
 * would be handed the first page's instance. The subclasses exist for that identity; the
 * behaviour is all here.
 */
abstract class TaskListViewModel(
    private val repository: TaskRepository,
    preferences: TaskListPreferences,
    private val page: TaskPage,
) : ViewModel() {

    /**
     * `null` until the database (and the first options emission) has arrived, so the
     * screen can tell "still loading" apart from "genuinely empty" — an
     * `initialValue = emptyList()` would render the empty state for a frame on every cold
     * start.
     *
     * `WhileSubscribed(5000)` stops the database read while the page is off screen (the
     * pager keeps neighbouring pages composed but this page's subscription is the only
     * thing keeping the query live), and restarts it on return. The ViewModel survives tab
     * switches, so the date is read per emission here rather than captured at
     * construction; the filter and the sections share that one reading.
     *
     * Grouping and sorting are rules shared by both data modes and both pages — they live
     * in `TaskFilters.kt` and take the options as an argument, so this flow only has to
     * combine the two inputs.
     */
    val groups: StateFlow<List<TaskGroup>?> by lazy {
        combine(repository.getAllTasks(), preferences.options) { tasks, options ->
            val today = LocalDate.now()
            tasks.forPage(page, today).sections(today, options)
        }.stateIn(
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

/** The Inbox: unfiled tasks, completed ones included. */
@HiltViewModel
class InboxViewModel @Inject constructor(
    repository: TaskRepository,
    preferences: TaskListPreferences,
) : TaskListViewModel(repository, preferences, TaskPage.INBOX)

/** The Today page: open tasks due today or earlier. */
@HiltViewModel
class TodayViewModel @Inject constructor(
    repository: TaskRepository,
    preferences: TaskListPreferences,
) : TaskListViewModel(repository, preferences, TaskPage.TODAY)
