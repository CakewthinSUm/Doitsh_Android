package com.doitsh.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String = "",
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val projectId: String? = null,
    val dueDate: String? = null, // ISO-8601
    val priority: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ProjectDto(
    val id: String = "",
    val name: String,
    val description: String? = null,
    val color: Int? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class HealthResponse(
    val status: String,
    val version: String
)

@Serializable
data class SyncRequest(
    val tasks: List<TaskDto> = emptyList(),
    val projects: List<ProjectDto> = emptyList(),
    val lastSyncTimestamp: String? = null
)

@Serializable
data class SyncResponse(
    val tasks: List<TaskDto> = emptyList(),
    val projects: List<ProjectDto> = emptyList(),
    val serverTimestamp: String
)
