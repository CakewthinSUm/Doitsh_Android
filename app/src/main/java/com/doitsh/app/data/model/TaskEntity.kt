package com.doitsh.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val projectId: String? = null,
    val dueDate: LocalDateTime? = null,
    val priority: Int = 0, // 0: none, 1: low, 2: medium, 3: high
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val syncStatus: SyncStatus = SyncStatus.LOCAL
)

enum class SyncStatus {
    LOCAL,      // Only exists locally
    SYNCED,     // Synced with server
    PENDING,    // Waiting to sync
    CONFLICT    // Has sync conflicts
}
