package com.doitsh.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doitsh.app.domain.model.Priority
import com.doitsh.app.domain.model.Project
import com.doitsh.app.domain.model.Task
import com.doitsh.app.domain.repository.ProjectRepository
import com.doitsh.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CreationViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    /**
     * The projects the new-task sheet can file a task into.
     *
     * `null` until the data source has actually emitted, so the sheet can tell "still
     * loading" apart from "no projects yet" — the same convention as
     * [ProjectsViewModel.projects] and [TaskListViewModel.tasks]. Without the
     * distinction a cold start would briefly offer an empty project menu.
     */
    val projects: StateFlow<List<Project>?> by lazy {
        projectRepository.getAllProjects().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    /**
     * @param dueDate when the task is due, or `null` for one with no deadline. A task
     *   with no due date stays out of Today — see `Task.isDueTodayOrEarlier`. The sheet
     *   stores it at the start of the chosen day, which is the only shape this field
     *   ever takes.
     * @param priority defaults to [Priority.NONE] rather than being required, so a
     *   caller that does not care about priority (there is none today) stays one
     *   argument shorter.
     * @param projectId the project to file the task under, or `null` to leave it in the
     *   Inbox. The Inbox is defined as `projectId == null` — see `TaskFilters.asInbox` —
     *   so this argument is what moves a task between the Inbox and a project.
     */
    fun createTask(
        title: String,
        description: String?,
        dueDate: LocalDateTime? = null,
        priority: Priority = Priority.NONE,
        projectId: String? = null,
    ) {
        viewModelScope.launch {
            taskRepository.addTask(
                Task(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    priority = priority,
                    projectId = projectId,
                )
            )
        }
    }

    /**
     * Persists an edited task.
     *
     * Takes the whole `Task` rather than the five fields the sheet shows, because the
     * sheet's caller is what assembles it — `existing.copy(title = ...)` keeps the
     * fields the form never displays (`id`, `isCompleted`, `createdAt`). `updatedAt`
     * is stamped by the repository, not here.
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
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
