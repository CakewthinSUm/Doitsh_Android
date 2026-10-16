package com.doitsh.app.data.repository

import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.model.isDueTodayOrEarlier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The two derived task lists the app's first two pages show.
 *
 * They are derived from [com.doitsh.app.domain.repository.TaskRepository.getAllTasks]
 * rather than implemented as queries of their own, because both repository
 * implementations have to answer them identically: the local one reads Room, while the
 * remote one reads an in-memory cache that only ever holds the full list. A DAO query
 * would have to be written twice, and the remote half would be a stub — which is exactly
 * the trap `getTasksByProject` and `getActiveTasks` fell into, where the remote
 * implementation returns every task regardless of the filter it was asked for. Deriving
 * both lists here instead means the two data modes cannot disagree about what Inbox or
 * Today means.
 *
 * Filtering in memory is honest about the current scale: a personal task list is
 * hundreds of rows, not millions, and both modes already hold the whole list.
 */

/**
 * The Inbox: every task that has not been filed into a project.
 *
 * Completed tasks are deliberately *kept*. The app has no completed-tasks view, so
 * hiding them here would make them unreachable — and with them the row's toggle, which
 * is the only way to un-complete a task that was ticked by mistake. Today filters
 * completed tasks out because it is a list of what still needs doing; the Inbox is a
 * list of what has not been filed, which is a question completion does not answer.
 */
internal fun Flow<List<Task>>.asInbox(): Flow<List<Task>> =
    map { tasks -> tasks.filter { it.projectId == null } }

/**
 * Open tasks due today or earlier, most overdue first.
 *
 * Sorted by due date rather than by creation, because the point of the page is what
 * needs attention next. [isDueTodayOrEarlier] drops completed and undated tasks, so the
 * sort key is never null.
 *
 * The "today" boundary is read when the list is computed, and the screen's
 * `WhileSubscribed(5000)` restarts this flow whenever it comes back into view — so
 * returning to the page picks up a new day. A user who leaves the app sitting on Today
 * across midnight keeps yesterday's list until something else changes; that is not worth
 * a clock-driven flow for a page nobody watches at 00:00.
 */
internal fun Flow<List<Task>>.asToday(): Flow<List<Task>> =
    map { tasks ->
        tasks.filter { it.isDueTodayOrEarlier() }.sortedBy { it.dueDate }
    }
