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
 * One ViewModel for both Today and Inbox, exposing a flow per [TaskPage] rather than a
 * subclass per page. The two pages are one screen over different tasks, so they differ in
 * nothing this class would have to branch on except the page itself — and the page is
 * already a value, so there is nothing for a subclass to hold. Both pages sit in the same
 * navigation entry's `ViewModelStore`, where `hiltViewModel<T>()` keys by type, so they
 * share this one instance and each reads its own flow from it.
 *
 * `null` until the database (and the first options emission) has arrived, so a screen can
 * tell "still loading" apart from "genuinely empty" — an `initialValue = emptyList()`
 * would render the empty state for a frame on every cold start.
 *
 * `WhileSubscribed(5000)` stops the database read while a page is off screen (the pager
 * keeps neighbouring pages composed but a page's subscription is the only thing keeping
 * its query live), and restarts it on return. The ViewModel survives tab switches, so the
 * date is read per emission rather than captured at construction; the filter and the
 * sections share that one reading.
 *
 * Grouping and sorting are rules shared by both data modes and both pages — they live in
 * `TaskFilters.kt` and take the options as an argument, so this flow only has to combine
 * the two inputs.
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val preferences: TaskListPreferences,
) : ViewModel() {

    private val flows = mutableMapOf<TaskPage, StateFlow<List<TaskGroup>?>>()

    /**
     * The grouped rows for one page.
     *
     * Built once per page and cached, so the two pages can share this ViewModel without
     * either one restarting the other's flow, and asking twice returns the same instance.
     */
    fun groups(page: TaskPage): StateFlow<List<TaskGroup>?> =
        flows.getOrPut(page) {
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
