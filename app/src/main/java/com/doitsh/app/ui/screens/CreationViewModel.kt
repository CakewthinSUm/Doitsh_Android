package com.doitsh.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doitsh.app.domain.model.Project
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.repository.ProjectRepository
import com.doitsh.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreationViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    fun createTask(title: String, description: String?) {
        viewModelScope.launch {
            taskRepository.addTask(
                Task(
                    title = title,
                    description = description
                )
            )
        }
    }

    fun createProject(name: String, description: String?) {
        viewModelScope.launch {
            projectRepository.addProject(
                Project(
                    name = name,
                    description = description
                )
            )
        }
    }
}
