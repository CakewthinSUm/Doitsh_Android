package com.doitsh.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Which tasks belong on the Today page.
 *
 * The rule is compared by calendar day, not by instant, and that distinction is the
 * whole point of these tests: the creation dialog stores a due date as local midnight,
 * so an instant comparison would drop a task due today the moment its own midnight
 * passed — the task would disappear from Today at 00:00 on the day it was due.
 */
class TaskDueDateTest {

    private val today = LocalDate.of(2026, 10, 16)

    private fun task(
        dueDate: LocalDateTime?,
        isCompleted: Boolean = false,
    ) = Task(title = "t", isCompleted = isCompleted, dueDate = dueDate)

    @Test
    fun aTaskDueTodayIsDueTodayAtAnyHourOfThatDay() {
        // The two ends of the day, both of which the dialog's midnight-only value has to
        // survive. A due date of today's 00:00 is in the past for the whole day.
        assertTrue(task(today.atStartOfDay()).isDueTodayOrEarlier(today))
        assertTrue(task(today.atTime(23, 59)).isDueTodayOrEarlier(today))
    }

    @Test
    fun overdueTasksCount() {
        assertTrue(task(today.minusDays(1).atStartOfDay()).isDueTodayOrEarlier(today))
        assertTrue(task(today.minusYears(1).atStartOfDay()).isDueTodayOrEarlier(today))
    }

    @Test
    fun futureTasksDoNot() {
        assertFalse(task(today.plusDays(1).atStartOfDay()).isDueTodayOrEarlier(today))
    }

    @Test
    fun anUndatedTaskIsNotDueToday() {
        // Today is a deadline view. A task with no date is not due, it is unscheduled —
        // it lives in the Inbox until it is given one.
        assertFalse(task(dueDate = null).isDueTodayOrEarlier(today))
    }

    @Test
    fun completedTasksAreNotDueToday() {
        // Completing a task is how it leaves this list; there is no separate archive.
        assertFalse(task(today.minusDays(1).atStartOfDay(), isCompleted = true).isDueTodayOrEarlier(today))
        assertFalse(task(today.atStartOfDay(), isCompleted = true).isDueTodayOrEarlier(today))
    }

    @Test
    fun theBoundaryIsTheCalendarDayNotTwentyFourHours() {
        // Yesterday at 23:00 is overdue, not "a few hours ago" — a duration-based rule
        // measured from the current time would call it due, and would call tomorrow's
        // 00:00 due as well if the clock were late enough in the day.
        assertTrue(task(LocalDateTime.of(2026, 10, 15, 23, 0)).isDueTodayOrEarlier(today))
        assertFalse(task(LocalDateTime.of(2026, 10, 17, 0, 0)).isDueTodayOrEarlier(today))
    }
}
