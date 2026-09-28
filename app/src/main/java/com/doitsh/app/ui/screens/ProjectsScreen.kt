package com.doitsh.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doitsh.app.domain.model.Project
import com.doitsh.app.domain.repository.ProjectRepository
import com.doitsh.app.domain.repository.TaskRepository
import com.doitsh.app.ui.components.EmptyState
import com.doitsh.app.ui.components.SectionHeader
import com.doitsh.app.ui.theme.SpacingTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectWithCount(
    val project: Project,
    val taskCount: Int
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    val projects by lazy {
        projectRepository.getAllProjects().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    val allTasks by lazy {
        taskRepository.getAllTasks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getTaskCount(projectId: String): Int {
        return allTasks.value.count { it.projectId == projectId }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val projects by viewModel.projects.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    val projectsWithCounts = projects.map { project ->
        ProjectWithCount(project, allTasks.count { it.projectId == project.id })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = SpacingTokens.Default,
            bottom = SpacingTokens.DisplayXXL,
            start = SpacingTokens.Default,
            end = SpacingTokens.Default
        ),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.Small)
    ) {
        if (projectsWithCounts.isEmpty()) {
            item {
                EmptyState(
                    title = "No projects yet",
                    subtitle = "Create your first project to organize tasks"
                )
            }
        } else {
            itemsIndexed(projectsWithCounts, key = { _, pwc -> pwc.project.id }) { index, pwc ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                ) {
                    ProjectCard(
                        project = pwc.project,
                        taskCount = pwc.taskCount
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCard(
    project: Project,
    taskCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.Default),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Default)
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$taskCount tasks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
