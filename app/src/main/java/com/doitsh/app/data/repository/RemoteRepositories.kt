package com.doitsh.app.data.repository

import com.doitsh.app.data.remote.DoitshApiClient
import com.doitsh.app.data.remote.dto.ProjectDto
import com.doitsh.app.data.remote.dto.TaskDto
import com.doitsh.app.domain.model.Project
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.repository.ProjectRepository
import com.doitsh.app.domain.repository.TaskRepository
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteTaskRepository @Inject constructor(
    private val apiClient: DoitshApiClient
) : TaskRepository {

    // In-memory cache; real implementation should persist to disk
    private val tasksCache = MutableStateFlow<List<Task>>(emptyList())
    private val mutex = Mutex()

    override fun getAllTasks(): Flow<List<Task>> = tasksCache.asStateFlow()

    override fun getTasksByProject(projectId: String): Flow<List<Task>> {
        // Would need per-project flow; simplified for now
        return tasksCache.asStateFlow()
    }

    override fun getActiveTasks(): Flow<List<Task>> {
        // Would filter; simplified
        return tasksCache.asStateFlow()
    }

    override suspend fun getTaskById(id: String): Task? =
        tasksCache.value.firstOrNull { it.id == id }

    override suspend fun addTask(task: Task): String {
        val client = apiClient.getClient()
        client.post("/api/tasks") {
            setBody(task.toDto())
        }
        mutex.withLock {
            tasksCache.value = tasksCache.value + task
        }
        return task.id
    }

    override suspend fun updateTask(task: Task) {
        val client = apiClient.getClient()
        client.put("/api/tasks/${task.id}") {
            setBody(task.toDto())
        }
        mutex.withLock {
            tasksCache.value = tasksCache.value.map {
                if (it.id == task.id) task else it
            }
        }
    }

    override suspend fun deleteTask(id: String) {
        val client = apiClient.getClient()
        client.delete("/api/tasks/$id")
        mutex.withLock {
            tasksCache.value = tasksCache.value.filter { it.id != id }
        }
    }
}

@Singleton
class RemoteProjectRepository @Inject constructor(
    private val apiClient: DoitshApiClient
) : ProjectRepository {

    private val projectsCache = MutableStateFlow<List<Project>>(emptyList())
    private val mutex = Mutex()

    override fun getAllProjects(): Flow<List<Project>> = projectsCache.asStateFlow()

    override suspend fun getProjectById(id: String): Project? =
        projectsCache.value.firstOrNull { it.id == id }

    override suspend fun addProject(project: Project): String {
        val client = apiClient.getClient()
        client.post("/api/projects") {
            setBody(project.toDto())
        }
        mutex.withLock {
            projectsCache.value = projectsCache.value + project
        }
        return project.id
    }

    override suspend fun updateProject(project: Project) {
        val client = apiClient.getClient()
        client.put("/api/projects/${project.id}") {
            setBody(project.toDto())
        }
        mutex.withLock {
            projectsCache.value = projectsCache.value.map {
                if (it.id == project.id) project else it
            }
        }
    }

    override suspend fun deleteProject(id: String) {
        val client = apiClient.getClient()
        client.delete("/api/projects/$id")
        mutex.withLock {
            projectsCache.value = projectsCache.value.filter { it.id != id }
        }
    }
}

// ---- DTO Mappers ----

private fun Task.toDto() = TaskDto(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    projectId = projectId,
    dueDate = dueDate?.toString(),
    priority = priority.value,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)

private fun Project.toDto() = ProjectDto(
    id = id,
    name = name,
    description = description,
    color = color,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)
