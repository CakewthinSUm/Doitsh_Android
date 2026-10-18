package com.doitsh.app.domain.repository

import com.doitsh.app.data.repository.TaskGroup
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

    /**
     * The Inbox as sections: unfiled tasks grouped by due date, completed ones last.
     *
     * Returns [TaskGroup]s rather than a flat list because the page draws one container
     * per section, and the grouping rule belongs next to the filter that selects the
     * tasks — see `TaskFilters.kt`.
     */
    fun getInboxTasks(): Flow<List<TaskGroup>>

    /** The Today page as sections: overdue, then due today. */
    fun getTodayTasks(): Flow<List<TaskGroup>>

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
