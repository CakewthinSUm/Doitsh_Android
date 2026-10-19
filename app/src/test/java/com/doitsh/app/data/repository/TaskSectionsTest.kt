package com.doitsh.app.data.repository

import com.doitsh.app.data.settings.SortDirection
import com.doitsh.app.data.settings.TaskGrouping
import com.doitsh.app.data.settings.TaskListOptions
import com.doitsh.app.data.settings.TaskSort
import com.doitsh.app.domain.model.Priority
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.model.isDueTodayOrEarlier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * How the list pages group tasks into sections.
 *
 * The grouping is a pure function over a list of tasks so that it can be tested without
 * a Compose runtime or a database — and so both data modes, which share it, cannot
 * disagree about what "overdue" or "no date" means. See `TaskFilters.kt`.
 */
class TaskSectionsTest {

    private val today = LocalDate.of(2026, 10, 16)

    private fun task(
        title: String,
        dueDate: LocalDate? = null,
        isCompleted: Boolean = false,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 10, 1, 9, 0),
        priority: Priority = Priority.NONE,
        // Explicit and derived from the title, because Task's default id is a random
        // UUID: the comparator's last tie-break is the id, so random ids would make any
        // test whose tasks tie on every other key flaky.
        id: String = title,
    ) = Task(
        id = id,
        title = title,
        dueDate = dueDate?.atStartOfDay(),
        isCompleted = isCompleted,
        createdAt = createdAt,
        priority = priority,
    )

    @Test
    fun sectionsAreOrderedOverdueTodayLaterNoDateCompleted() {
        val groups = listOf(
            task("done", dueDate = today.minusDays(3), isCompleted = true),
            task("someday"),
            task("next week", dueDate = today.plusWeeks(1)),
            task("today", dueDate = today),
            task("yesterday", dueDate = today.minusDays(1)),
        ).sections(today)

        assertEquals(
            listOf(
                TaskSection.OVERDUE,
                TaskSection.TODAY,
                TaskSection.LATER,
                TaskSection.NO_DATE,
                TaskSection.COMPLETED,
            ),
            groups.map { it.section },
        )
    }

    @Test
    fun emptySectionsAreDropped() {
        // The Today page has no undated or completed tasks by construction, so it must
        // not draw an empty "No date" header under the list.
        val groups = listOf(
            task("today", dueDate = today),
            task("later", dueDate = today.plusDays(2)),
        ).sections(today)

        assertEquals(listOf(TaskSection.TODAY, TaskSection.LATER), groups.map { it.section })
    }

    @Test
    fun aCompletedTaskIgnoresItsDueDate() {
        // A ticked task has no meaningful place among live deadlines; it belongs in the
        // completed section whether it was due yesterday or next month.
        val groups = listOf(
            task("done but overdue", dueDate = today.minusDays(5), isCompleted = true),
            task("done and undated", isCompleted = true),
        ).sections(today)

        assertEquals(listOf(TaskSection.COMPLETED), groups.map { it.section })
        assertEquals(2, groups.single().tasks.size)
    }

    @Test
    fun tasksDueOnTheSameDayAreOrderedByCreation() {
        // All three are due tomorrow, so only the second key — creation time — orders
        // them; it is then deliberately out of order in the input.
        val first = task("A", dueDate = today.plusDays(1), createdAt = LocalDateTime.of(2026, 10, 2, 9, 0))
        val second = task("B", dueDate = today.plusDays(1), createdAt = LocalDateTime.of(2026, 10, 3, 9, 0))
        val earlier = task("C", dueDate = today.plusDays(1), createdAt = LocalDateTime.of(2026, 10, 9, 9, 0))

        val group = listOf(second, earlier, first).sections(today).single()

        assertEquals(TaskSection.LATER, group.section)
        assertEquals(listOf("A", "B", "C"), group.tasks.map { it.title })
    }

    @Test
    fun theTodayPageSeesOnlyOverdueAndToday() {
        // `asTodayGroups` filters first and then sections. Completed and undated tasks
        // never reach it, which is why the Today page can never show those headers.
        val groups = listOf(
            task("overdue", dueDate = today.minusDays(2)),
            task("today", dueDate = today),
            task("later", dueDate = today.plusDays(2)),
            task("undated"),
            task("done", dueDate = today, isCompleted = true),
        ).filter { it.isDueTodayOrEarlier(today) }
            .sections(today)

        assertEquals(listOf(TaskSection.OVERDUE, TaskSection.TODAY), groups.map { it.section })
        assertTrue(groups.none { it.section == TaskSection.LATER })
    }

    // The user's options. Everything above this line uses the defaulted parameter, which
    // is what proves the menu's defaults reproduce the list the app shipped with.

    private fun options(
        grouping: TaskGrouping = TaskGrouping.DUE_DATE,
        sort: TaskSort = TaskSort.DUE_DATE,
        direction: SortDirection = SortDirection.ASCENDING,
    ) = TaskListOptions(grouping, sort, direction)

    @Test
    fun groupingNoneReturnsOneGroupWithNullSection() {
        val groups = listOf(
            task("later", dueDate = today.plusDays(2)),
            task("done", isCompleted = true),
            task("undated"),
        ).sections(today, options(grouping = TaskGrouping.NONE))

        val group = groups.single()
        assertEquals(null, group.section)
        assertEquals(setOf("later", "done", "undated"), group.tasks.map { it.title }.toSet())
    }

    @Test
    fun dueDateDescendingKeepsUndatedLast() {
        // Descending reverses the dated values only. Reversing the whole comparator would
        // put the undated task first, because null is the smallest key.
        val groups = listOf(
            task("undated"),
            task("soon", dueDate = today.plusDays(1)),
            task("later", dueDate = today.plusDays(5)),
        ).sections(today, options(grouping = TaskGrouping.NONE, direction = SortDirection.DESCENDING))

        assertEquals(listOf("later", "soon", "undated"), groups.single().tasks.map { it.title })
    }

    @Test
    fun priorityGroupingOrdersHighToNoneWithCompletedLast() {
        val groups = listOf(
            task("done high", priority = Priority.HIGH, isCompleted = true),
            task("none"),
            task("low", priority = Priority.LOW),
            task("high", priority = Priority.HIGH),
            task("medium", priority = Priority.MEDIUM),
        ).sections(today, options(grouping = TaskGrouping.PRIORITY))

        assertEquals(
            listOf(
                TaskSection.PRIORITY_HIGH,
                TaskSection.PRIORITY_MEDIUM,
                TaskSection.PRIORITY_LOW,
                TaskSection.PRIORITY_NONE,
                TaskSection.COMPLETED,
            ),
            groups.map { it.section },
        )
        // A ticked task ignores its priority, exactly as it ignores its due date under
        // date grouping.
        assertEquals(listOf("done high"), groups.last().tasks.map { it.title })
        assertEquals(listOf("high"), groups.first().tasks.map { it.title })
    }

    @Test
    fun prioritySortPutsNoPriorityLastInBothDirections() {
        // No priority is the absence of a choice, like no due date — not the lowest value.
        val tasks = listOf(
            task("none"),
            task("low", priority = Priority.LOW),
            task("high", priority = Priority.HIGH),
        )

        val ascending = tasks.sections(
            today,
            options(grouping = TaskGrouping.NONE, sort = TaskSort.PRIORITY),
        ).single().tasks.map { it.title }
        val descending = tasks.sections(
            today,
            options(
                grouping = TaskGrouping.NONE,
                sort = TaskSort.PRIORITY,
                direction = SortDirection.DESCENDING,
            ),
        ).single().tasks.map { it.title }

        assertEquals(listOf("low", "high", "none"), ascending)
        assertEquals(listOf("high", "low", "none"), descending)
    }

    @Test
    fun titleSortIsCaseInsensitiveAndTieBreaksByCreation() {
        val early = LocalDateTime.of(2026, 10, 2, 9, 0)
        val late = LocalDateTime.of(2026, 10, 3, 9, 0)
        val tasks = listOf(
            task("beta", createdAt = late),
            task("Alpha", createdAt = early),
            task("Beta", createdAt = early),
        )
        // The two betas tie under a case-insensitive comparison, so creation time decides.

        val sorted = tasks.sections(
            today,
            options(grouping = TaskGrouping.NONE, sort = TaskSort.TITLE),
        ).single().tasks.map { it.title }

        // "Alpha" first; then the two betas, which tie case-insensitively and are settled
        // by creation time (Beta was created before beta).
        assertEquals(listOf("Alpha", "Beta", "beta"), sorted)
    }

    @Test
    fun directionDoesNotReverseTheTieBreak() {
        // Two tasks share the only meaningful sort key, so the direction must not reorder
        // them: the tie-break stays ascending either way, which keeps the order total.
        val early = task("early", dueDate = today.plusDays(1), createdAt = LocalDateTime.of(2026, 10, 2, 9, 0))
        val late = task("late", dueDate = today.plusDays(1), createdAt = LocalDateTime.of(2026, 10, 3, 9, 0))
        val tasks = listOf(late, early)

        val ascending = tasks.sections(
            today,
            options(grouping = TaskGrouping.NONE),
        ).single().tasks.map { it.title }
        val descending = tasks.sections(
            today,
            options(grouping = TaskGrouping.NONE, direction = SortDirection.DESCENDING),
        ).single().tasks.map { it.title }

        assertEquals(listOf("early", "late"), ascending)
        assertEquals(listOf("early", "late"), descending)
    }
}
