package com.doitsh.app.data.repository

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
    ) = Task(
        title = title,
        dueDate = dueDate?.atStartOfDay(),
        isCompleted = isCompleted,
        createdAt = createdAt,
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
}
