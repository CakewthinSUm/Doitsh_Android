package com.doitsh.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.doitsh.app.domain.model.Priority
import com.doitsh.app.domain.model.Task
import com.doitsh.app.ui.theme.MotionTokens
import com.doitsh.app.ui.theme.SpacingTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    true
                } else false
            }
        ),
        backgroundContent = {
            // Delete background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = SpacingTokens.XLarge)
                )
            }
        },
        content = {
            TaskCardSurface(
                task = task,
                onToggle = onToggle,
                modifier = modifier
            )
        }
    )
}

@Composable
private fun TaskCardSurface(
    task: Task,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Priority color strip
    val stripColor = when (task.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.error
        Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
        Priority.LOW -> MaterialTheme.colorScheme.primary
        Priority.NONE -> Color.Transparent
    }

    val elevation by animateDpAsState(
        targetValue = if (task.isCompleted) 0.dp else 1.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_elevation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority strip
            if (task.priority != Priority.NONE) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .background(
                            color = stripColor,
                            shape = MaterialTheme.shapes.extraSmall
                        )
                )
                Spacer(modifier = Modifier.width(SpacingTokens.Small))
            }

            // Check icon with animated transition
            AnimatedContent(
                targetState = task.isCompleted,
                transitionSpec = {
                    fadeIn(animationSpec = MotionTokens.DefaultSpring) togetherWith
                            fadeOut(animationSpec = MotionTokens.DefaultSpring)
                },
                label = "task_check"
            ) { completed ->
                Icon(
                    imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (completed) "Completed" else "Not completed",
                    tint = if (completed)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onToggle() }
                        .padding(SpacingTokens.Default)
                )
            }

            // Task content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggle() }
                    .padding(
                        top = SpacingTokens.Default,
                        bottom = SpacingTokens.Default,
                        end = SpacingTokens.Default
                    )
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                task.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
