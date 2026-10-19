package com.doitsh.app.data.repository

import com.doitsh.app.data.local.ProjectDao
import com.doitsh.app.data.local.TaskDao
import com.doitsh.app.data.model.ProjectEntity
import com.doitsh.app.data.model.TaskEntity
import com.doitsh.app.domain.model.Priority
import com.doitsh.app.domain.model.Project
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.repository.ProjectRepository
import com.doitsh.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTaskRepository @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { entities -> entities.map { it.toDomain() } }

    override fun getTasksByProject(projectId: String): Flow<List<Task>> =
        taskDao.getTasksByProject(projectId).map { entities -> entities.map { it.toDomain() } }

    override fun getActiveTasks(): Flow<List<Task>> =
        taskDao.getActiveTasks().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTaskById(id: String): Task? =
        taskDao.getTaskById(id)?.toDomain()

    override suspend fun addTask(task: Task): String {
        taskDao.insertTask(task.toEntity())
        return task.id
    }

    override suspend fun updateTask(task: Task) =
        taskDao.updateTask(task.toEntity().copy(updatedAt = LocalDateTime.now()))

    override suspend fun deleteTask(id: String) =
        taskDao.deleteTaskById(id)
}

@Singleton
class LocalProjectRepository @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {

    override fun getAllProjects(): Flow<List<Project>> =
        projectDao.getAllProjects().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getProjectById(id: String): Project? =
        projectDao.getProjectById(id)?.toDomain()

    override suspend fun addProject(project: Project): String {
        projectDao.insertProject(project.toEntity())
        return project.id
    }

    override suspend fun updateProject(project: Project) =
        projectDao.updateProject(project.toEntity().copy(updatedAt = LocalDateTime.now()))

    override suspend fun deleteProject(id: String) =
        projectDao.deleteProjectById(id)
}

// ---- Mappers ----

private fun TaskEntity.toDomain() = Task(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    projectId = projectId,
    dueDate = dueDate,
    priority = Priority.fromValue(priority),
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    projectId = projectId,
    dueDate = dueDate,
    priority = priority.value,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun ProjectEntity.toDomain() = Project(
    id = id,
    name = name,
    description = description,
    color = color,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Project.toEntity() = ProjectEntity(
    id = id,
    name = name,
    description = description,
    color = color,
    createdAt = createdAt,
    updatedAt = updatedAt
)
