package com.doitsh.app.domain.model

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

enum class Priority(val value: Int) {
    NONE(0), LOW(1), MEDIUM(2), HIGH(3);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: NONE
    }
}
