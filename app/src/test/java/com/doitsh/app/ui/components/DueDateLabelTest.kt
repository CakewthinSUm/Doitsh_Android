package com.doitsh.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The wording of the due date under a task title.
 *
 * Relative wording only where it is actionable — the days either side of today — and an
 * absolute date further out, where "in 9 days" is harder to place in a week than a
 * weekday and a date.
 */
class DueDateLabelTest {

    private val today = LocalDate.of(2026, 10, 16)

    @Test
    fun theDaysAroundTodayAreRelative() {
        assertEquals("Today", formatDueDate(today, today))
        assertEquals("Tomorrow", formatDueDate(today.plusDays(1), today))
        assertEquals("Yesterday", formatDueDate(today.minusDays(1), today))
    }

    @Test
    fun overdueByMoreThanADayCountsDays() {
        assertEquals("2 days overdue", formatDueDate(today.minusDays(2), today))
        assertEquals("30 days overdue", formatDueDate(today.minusDays(30), today))
    }

    @Test
    fun laterDatesAreAbsolute() {
        // Formatted through the platform's locale format rather than a hard-coded
        // pattern, so this asserts the shape a user would recognise, not a literal that
        // would only hold in one locale.
        val formatted = formatDueDate(today.plusDays(2), today)
        assertEquals(false, formatted.startsWith("in "))
        assertEquals(true, formatted.contains("2026") || formatted.contains("Oct"))
    }
}
