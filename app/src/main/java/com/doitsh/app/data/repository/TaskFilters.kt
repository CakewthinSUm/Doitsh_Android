package com.doitsh.app.data.repository

import com.doitsh.app.data.settings.SortDirection
import com.doitsh.app.data.settings.TaskGrouping
import com.doitsh.app.data.settings.TaskListOptions
import com.doitsh.app.data.settings.TaskSort
import com.doitsh.app.domain.model.Priority
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.model.isDueTodayOrEarlier
import java.time.LocalDate

/**
 * The two task list pages the app shows at the top level.
 *
 * They are one screen over different tasks — see `TaskListScreen` — so what distinguishes
 * them lives here, next to the section rules, rather than in either page. Deriving both
 * from [com.doitsh.app.domain.repository.TaskRepository.getAllTasks] rather than
 * implementing a query each is what keeps the local and self-hosted data modes answering
 * identically: the remote repository only ever holds the full list, so a DAO query would
 * have to be written twice and the remote half would be a stub.
 *
 * Filtering in memory is honest about the current scale: a personal task list is
 * hundreds of rows, not millions, and both modes already hold the whole list.
 */
enum class TaskPage {
    /**
     * Open tasks due today or earlier.
     *
     * [isDueTodayOrEarlier] drops completed and undated tasks, so this page never shows
     * the completed or "no date" sections.
     */
    TODAY,

    /**
     * Every task that has not been filed into a project.
     *
     * Completed tasks are deliberately *kept*. The app has no completed-tasks view, so
     * hiding them here would make them unreachable — and with them the row's toggle,
     * which is the only way to un-complete a task that was ticked by mistake. Today
     * filters completed tasks out because it is a list of what still needs doing; the
     * Inbox is a list of what has not been filed, which is a question completion does not
     * answer.
     */
    INBOX,
}

/**
 * The tasks one page shows, before grouping.
 *
 * [today] is passed in rather than read here so the filter and the sections that follow
 * it share one clock reading — two `LocalDate.now()` calls either side of midnight would
 * disagree about what "today" means within a single list.
 */
internal fun List<Task>.forPage(page: TaskPage, today: LocalDate): List<Task> = when (page) {
    TaskPage.TODAY -> filter { it.isDueTodayOrEarlier(today) }
    TaskPage.INBOX -> filter { it.projectId == null }
}

/**
 * The section a task is drawn under on the two list pages.
 *
 * The list is not a flat sequence of equal rows by default: tasks are grouped by *when
 * they are due*, because "what is overdue", "what is due today" and "what is only an
 * idea" are three different kinds of obligation and a single sorted list makes the reader
 * work that out row by row. The groups are computed here, next to the page filters, so
 * both data modes and both pages agree — and so the rule can be unit-tested without a
 * Compose runtime.
 *
 * Date sections and priority sections never appear together — [TaskListOptions.grouping]
 * picks one rule or the other — but they share one enum so a group is always one type, and
 * [COMPLETED] stays last under both. Each rule's display order is its own list below,
 * rather than this enum's declaration order, because the two rules interleave here.
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

    PRIORITY_HIGH,
    PRIORITY_MEDIUM,
    PRIORITY_LOW,
    PRIORITY_NONE,

    /**
     * Completed tasks, always last.
     *
     * A section of their own rather than being mixed into the other groups: the Inbox
     * deliberately keeps completed tasks (there is no completed-tasks view, so hiding
     * them would make a mistaken tick impossible to undo), and a ticked task has no
     * meaningful place among things still to do. Keeping them together also leaves the
     * live groups free of struck-through rows.
     */
    COMPLETED,
}

/**
 * One section of a list page: its identity and the tasks drawn under it.
 *
 * [section] is `null` when the user chose [TaskGrouping.NONE]: the list is one container
 * and no header is drawn above it.
 */
data class TaskGroup(
    val section: TaskSection?,
    val tasks: List<Task>,
)

/**
 * Groups an already-filtered list into the sections it should be drawn under, dropping
 * empty sections and ordering tasks inside each one.
 *
 * A task with no due date sorts into NO_DATE rather than being treated as due today:
 * undated work is not a deadline, which is the same rule `isDueTodayOrEarlier` uses to
 * keep it off the Today page.
 *
 * With [TaskGrouping.NONE] the list is a single unlabelled group and completed tasks are
 * *not* set apart — that is what "no grouping" means. Under the two grouped modes they
 * are always last.
 *
 * Sort within a section is the user's [TaskListOptions.sort] and direction, with missing
 * values (no date, no priority) kept last in both directions, then creation time and id as
 * tie-breaks so the order is total and stable across the two data modes rather than
 * depending on the order the repository happened to emit.
 *
 * The `options` default is the pre-menu behaviour, which is what the existing tests pin.
 */
internal fun List<Task>.sections(
    today: LocalDate,
    options: TaskListOptions = TaskListOptions(),
): List<TaskGroup> {
    if (isEmpty()) return emptyList()

    val comparator = sortComparator(options.sort, options.direction)

    return when (options.grouping) {
        TaskGrouping.NONE ->
            listOf(TaskGroup(section = null, tasks = sortedWith(comparator)))

        TaskGrouping.DUE_DATE -> sectionsBy(DATE_SECTIONS, comparator) { task ->
            when {
                task.isCompleted -> TaskSection.COMPLETED
                task.dueDate == null -> TaskSection.NO_DATE
                task.dueDate.toLocalDate().isBefore(today) -> TaskSection.OVERDUE
                task.dueDate.toLocalDate() == today -> TaskSection.TODAY
                else -> TaskSection.LATER
            }
        }

        TaskGrouping.PRIORITY -> sectionsBy(PRIORITY_SECTIONS, comparator) { task ->
            if (task.isCompleted) TaskSection.COMPLETED else task.priority.section
        }
    }
}

private val DATE_SECTIONS = listOf(
    TaskSection.OVERDUE,
    TaskSection.TODAY,
    TaskSection.LATER,
    TaskSection.NO_DATE,
    TaskSection.COMPLETED,
)

private val PRIORITY_SECTIONS = listOf(
    TaskSection.PRIORITY_HIGH,
    TaskSection.PRIORITY_MEDIUM,
    TaskSection.PRIORITY_LOW,
    TaskSection.PRIORITY_NONE,
    TaskSection.COMPLETED,
)

private val Priority.section: TaskSection
    get() = when (this) {
        Priority.HIGH -> TaskSection.PRIORITY_HIGH
        Priority.MEDIUM -> TaskSection.PRIORITY_MEDIUM
        Priority.LOW -> TaskSection.PRIORITY_LOW
        Priority.NONE -> TaskSection.PRIORITY_NONE
    }

/** Groups by [sectionOf], drops the sections nobody landed in, and orders each group. */
private fun List<Task>.sectionsBy(
    displayOrder: List<TaskSection>,
    comparator: Comparator<Task>,
    sectionOf: (Task) -> TaskSection,
): List<TaskGroup> {
    val bySection = groupBy(sectionOf)
    return displayOrder.mapNotNull { section ->
        bySection[section]?.let { tasks ->
            TaskGroup(section = section, tasks = tasks.sortedWith(comparator))
        }
    }
}

/**
 * The full ordering of tasks within a section.
 *
 * Direction applies only to the primary key. The tie-breaks stay ascending in both
 * directions, so "the same idea, in the order it was written down" means the same thing
 * whichever way the list runs — and so the order is total even when two tasks share every
 * primary key.
 */
private fun sortComparator(sort: TaskSort, direction: SortDirection): Comparator<Task> {
    val descending = direction == SortDirection.DESCENDING
    val primary = when (sort) {
        TaskSort.DUE_DATE -> nullsLastComparator({ it.dueDate }, descending)
        // A task with no priority is an absence of a choice, like no due date, so it goes
        // last in both directions rather than counting as the lowest priority.
        TaskSort.PRIORITY -> nullsLastComparator(
            key = { task -> task.priority.takeIf { it != Priority.NONE }?.value },
            descending = descending,
        )
        TaskSort.TITLE -> Comparator<Task> { first, second ->
            if (descending) {
                second.title.compareTo(first.title, ignoreCase = true)
            } else {
                first.title.compareTo(second.title, ignoreCase = true)
            }
        }
        TaskSort.CREATED_AT -> Comparator<Task> { first, second ->
            if (descending) {
                second.createdAt.compareTo(first.createdAt)
            } else {
                first.createdAt.compareTo(second.createdAt)
            }
        }
    }
    return primary.thenBy { it.createdAt }.thenBy { it.id }
}

/**
 * Compares a nullable key, keeping nulls last in either direction.
 *
 * The direction is applied to the two values rather than to the whole comparator, because
 * `Comparator.reversed()` would move nulls to the front — null is the smallest value.
 */
private fun <K : Comparable<K>> nullsLastComparator(
    key: (Task) -> K?,
    descending: Boolean,
): Comparator<Task> = Comparator { first, second ->
    val a = key(first)
    val b = key(second)
    when {
        a == null && b == null -> 0
        a == null -> 1
        b == null -> -1
        descending -> b.compareTo(a)
        else -> a.compareTo(b)
    }
}
