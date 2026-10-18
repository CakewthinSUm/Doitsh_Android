package com.doitsh.app.data.repository

import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.model.isDueTodayOrEarlier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

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

/**
 * The section a task is drawn under on the two list pages.
 *
 * The list is no longer a flat sequence of equal rows: tasks are grouped by *when they
 * are due*, because "what is overdue", "what is due today" and "what is only an idea"
 * are three different kinds of obligation and a single sorted list makes the reader
 * work that out row by row. The groups are computed here, next to the two filters, so
 * both data modes and both pages agree — and so the rule can be unit-tested without a
 * Compose runtime.
 *
 * Declaration order is display order.
 */
enum class TaskSection {
    /** Due before today. Only the Today page can produce these from open tasks. */
    OVERDUE,

    /** Due today. */
    TODAY,

    /** Due after today. */
    LATER,

    /** No due date at all. */
    NO_DATE,

    /**
     * Completed tasks, always last.
     *
     * A section of their own rather than being mixed into the date groups: the Inbox
     * deliberately keeps completed tasks (there is no completed-tasks view, so hiding
     * them would make a mistaken tick impossible to undo), and a ticked task has no
     * meaningful place among things still to do. Keeping them together also leaves the
     * live groups free of struck-through rows.
     */
    COMPLETED,
}

/** One section of a list page: its identity and the tasks drawn under it. */
data class TaskGroup(
    val section: TaskSection,
    val tasks: List<Task>,
)

/**
 * The Inbox as sections: open tasks by date, then the completed ones.
 *
 * Every open Inbox task is due-later, due-today or undated — never overdue, because
 * [asInbox] does not filter by date and a task due last week is still simply "in the
 * inbox". An overdue Inbox task is reported as [TaskSection.OVERDUE] rather than LATER
 * so the section means the same thing on both pages.
 */
internal fun Flow<List<Task>>.asInboxGroups(today: LocalDate = LocalDate.now()): Flow<List<TaskGroup>> =
    map { tasks -> tasks.sections(today) }

/**
 * Today as sections: overdue first, then due today.
 *
 * [asToday] already drops completed and undated tasks, so only the first two sections
 * can appear here — but the sectioning is the same function, which is what keeps the
 * two pages from disagreeing about what "overdue" means.
 */
internal fun Flow<List<Task>>.asTodayGroups(today: LocalDate = LocalDate.now()): Flow<List<TaskGroup>> =
    map { tasks -> tasks.filter { it.isDueTodayOrEarlier(today) }.sections(today) }

/**
 * Groups an already-filtered list into the sections it should be drawn under, dropping
 * empty sections and ordering tasks inside each one.
 *
 * A task with no due date sorts into NO_DATE rather than being treated as due today:
 * undated work is not a deadline, which is the same rule `isDueTodayOrEarlier` uses to
 * keep it off the Today page.
 *
 * Sort within a section: by due date (`null` last), then by creation, so the order is
 * total and stable across the two data modes rather than depending on the order the
 * repository happened to emit.
 */
internal fun List<Task>.sections(today: LocalDate): List<TaskGroup> {
    val bySection = groupBy { task ->
        when {
            task.isCompleted -> TaskSection.COMPLETED
            task.dueDate == null -> TaskSection.NO_DATE
            task.dueDate.toLocalDate().isBefore(today) -> TaskSection.OVERDUE
            task.dueDate.toLocalDate() == today -> TaskSection.TODAY
            else -> TaskSection.LATER
        }
    }

    return TaskSection.entries.mapNotNull { section ->
        bySection[section]?.let { tasks ->
            TaskGroup(
                section = section,
                tasks = tasks.sortedWith(
                    compareBy<Task> { it.dueDate }.thenBy { it.createdAt }
                ),
            )
        }
    }
}
