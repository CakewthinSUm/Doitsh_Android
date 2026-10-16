package com.doitsh.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val projectId: String? = null,
    val dueDate: LocalDateTime? = null,
    val priority: Priority = Priority.NONE,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Whether this task belongs on the Today page: still open, and due today or earlier.
 *
 * Compared by *calendar day* rather than by instant, because [dueDate] is a date the
 * user picked — the dialog stores it as local midnight — and "due today" has to mean
 * the same thing at 09:00 and at 23:00 on that day. An instant comparison against
 * `now` would drop a task due today from the list the moment its midnight passed.
 *
 * A task with no due date is not "due today": Today is a deadline view, not a
 * everything view. Undated tasks live in the Inbox until they are given a date.
 *
 * `today` is a parameter rather than a `LocalDate.now()` call so the rule is a pure
 * function that can be tested without freezing the clock.
 */
fun Task.isDueTodayOrEarlier(today: LocalDate = LocalDate.now()): Boolean =
    !isCompleted && dueDate != null && !dueDate.toLocalDate().isAfter(today)

enum class Priority(val value: Int) {
    NONE(0), LOW(1), MEDIUM(2), HIGH(3);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: NONE
    }
}
