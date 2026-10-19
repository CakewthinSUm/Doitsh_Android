package com.doitsh.app.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** How the task list pages group their rows. */
enum class TaskGrouping {
    /** One list, no section headers. Completed tasks are not set apart. */
    NONE,

    /** Overdue / today / later / no date, with completed last. */
    DUE_DATE,

    /** High / medium / low / no priority, with completed last. */
    PRIORITY,
}

/** The key tasks inside a section are ordered by. */
enum class TaskSort {
    DUE_DATE,
    PRIORITY,
    TITLE,
    CREATED_AT,
}

/** Which way [TaskSort] runs. Missing values (no date, no priority) stay last either way. */
enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

/**
 * How the Today and Inbox pages draw their lists.
 *
 * The two pages share one set of options rather than keeping a copy each: they are the
 * same list over different tasks, so a user who sorts the Inbox by priority means the
 * same thing on Today. The defaults reproduce the list the app shipped with before the
 * menu existed, so an install that never opens the menu looks unchanged.
 */
data class TaskListOptions(
    val grouping: TaskGrouping = TaskGrouping.DUE_DATE,
    val sort: TaskSort = TaskSort.DUE_DATE,
    val direction: SortDirection = SortDirection.ASCENDING,
)

/**
 * The list options, shared by every page and every ViewModel.
 *
 * A `StateFlow` rather than a plain property because the menu, the shell and both list
 * pages all read it: the pages have to re-group the moment the menu writes, and the menu
 * has to show what the pages are actually drawing. Writes go through here rather than
 * straight to [UserSettings] so that this one flow is the single source of truth.
 */
@Singleton
class TaskListPreferences @Inject constructor(
    private val settings: UserSettings,
) {
    private val _options = MutableStateFlow(settings.taskListOptions)
    val options: StateFlow<TaskListOptions> = _options.asStateFlow()

    fun update(options: TaskListOptions) {
        settings.taskListOptions = options
        _options.value = options
    }
}
