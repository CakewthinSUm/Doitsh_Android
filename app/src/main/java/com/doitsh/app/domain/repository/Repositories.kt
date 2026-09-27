package com.doitsh.app.domain.repository

import com.doitsh.app.domain.model.Project
import com.doitsh.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Core abstraction for dual-mode data access.
 *
 * Implementations:
 * - [LocalTaskRepository]: stores data in Room (local-only mode)
 * - [RemoteTaskRepository]: stores data on self-hosted Ktor server (NAS mode)
 *
 * The user's selected mode decides which implementation Hilt injects.
 */
interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByProject(projectId: String): Flow<List<Task>>
    fun getActiveTasks(): Flow<List<Task>>
    suspend fun getTaskById(id: String): Task?
    suspend fun addTask(task: Task): String
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: String)
}

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    suspend fun getProjectById(id: String): Project?
    suspend fun addProject(project: Project): String
    suspend fun updateProject(project: Project)
    suspend fun deleteProject(id: String)
}
