package com.doitsh.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doitsh.app.domain.model.Priority
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.repository.ProjectRepository
import com.doitsh.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AddTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    fun addTask(
        title: String,
        projectId: String? = null,
        dueDate: LocalDateTime? = null,
        priority: Priority = Priority.NONE,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val task = Task(
                    title = title.trim(),
                    projectId = projectId,
                    dueDate = dueDate,
                    priority = priority
                )
                taskRepository.addTask(task)
                onSuccess()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    suspend fun getAllProjects() = projectRepository.getAllProjects()
}
