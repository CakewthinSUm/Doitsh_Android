package com.doitsh.app.ui.components

import android.app.Activity
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.doitsh.app.domain.model.Priority
import com.doitsh.app.ui.theme.SpacingTokens
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onTaskAdded: () -> Unit,
    submitTask: (String, String?, String, Priority) -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }
    var selectedProjectName by remember { mutableStateOf("收件箱") }
    var dueDateLabel by remember { mutableStateOf("今天") }
    var priority by remember { mutableStateOf(Priority.NONE) }
    var showProjectPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPriorityPicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    // Blur the activity content behind the dialog (API 31+)
    val activityDecorView = (LocalContext.current as? Activity)?.window?.decorView
    DisposableEffect(activityDecorView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activityDecorView?.setRenderEffect(
                RenderEffect.createBlurEffect(14f, 14f, Shader.TileMode.CLAMP)
            )
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activityDecorView?.setRenderEffect(null)
            }
        }
    }

    LaunchedEffect(Unit) {
        // Wait for the dialog window to be attached and focused before requesting
        delay(150)
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        // Remove the platform dim so only our own scrim darkens the screen,
        // uniformly across the status/navigation bar areas too.
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            (dialogView.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
            onDispose { }
        }

        // Background scrim covering the full screen (including system bars)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f))
                .clickable { onDismiss() }
        ) {
            // Dialog card positioned at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    // Sit above the navigation bar, and above the keyboard when it shows
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = SpacingTokens.Default)
                    .padding(bottom = SpacingTokens.XLarge)
                    .clickable { } // Consume clicks to prevent scrim dismiss
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(SpacingTokens.Default)
                    ) {
                        // Title input
                        BasicTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .padding(bottom = SpacingTokens.Default),
                            textStyle = TextStyle(
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (taskTitle.isEmpty()) {
                                    Text(
                                        text = "任务名称",
                                        style = TextStyle(
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                innerTextField()
                            },
                            singleLine = true
                        )

                        // Bottom action buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: action chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Small)
                            ) {
                                // Add more options button
                                IconButton(
                                    onClick = { /* TODO: show more options */ },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "More options",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Project selector
                                FilterChip(
                                    selected = false,
                                    onClick = { showProjectPicker = true },
                                    label = { Text(selectedProjectName, fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(32.dp)
                                )

                                // Date selector
                                FilterChip(
                                    selected = dueDateLabel == "今天",
                                    onClick = { showDatePicker = true },
                                    label = { Text(dueDateLabel, fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(32.dp)
                                )

                                // Priority selector
                                FilterChip(
                                    selected = priority != Priority.NONE,
                                    onClick = { showPriorityPicker = true },
                                    label = {
                                        Text(
                                            when (priority) {
                                                Priority.NONE -> "优先级"
                                                Priority.LOW -> "低"
                                                Priority.MEDIUM -> "中"
                                                Priority.HIGH -> "高"
                                            },
                                            fontSize = 13.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Flag,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(32.dp)
                                )
                            }

                            // Right side: submit button
                            FilledIconButton(
                                onClick = {
                                    if (taskTitle.isNotBlank()) {
                                        submitTask(
                                            taskTitle.trim(),
                                            null,
                                            dueDateLabel,
                                            priority
                                        )
                                        onTaskAdded()
                                        onDismiss()
                                    }
                                },
                                enabled = taskTitle.isNotBlank(),
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Add task",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Project picker dialog
    if (showProjectPicker) {
        AlertDialog(
            onDismissRequest = { showProjectPicker = false },
            title = { Text("选择项目") },
            text = {
                Column {
                    Text(
                        text = "收件箱",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedProjectName = "收件箱"
                                showProjectPicker = false
                            }
                            .padding(SpacingTokens.Small)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectPicker = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("选择日期") },
            text = {
                Column {
                    Text(
                        text = "今天",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dueDateLabel = "今天"
                                showDatePicker = false
                            }
                            .padding(SpacingTokens.Small)
                    )
                    Text(
                        text = "明天",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dueDateLabel = "明天"
                                showDatePicker = false
                            }
                            .padding(SpacingTokens.Small)
                    )
                    Text(
                        text = "无日期",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dueDateLabel = "无日期"
                                showDatePicker = false
                            }
                            .padding(SpacingTokens.Small)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Priority picker dialog
    if (showPriorityPicker) {
        AlertDialog(
            onDismissRequest = { showPriorityPicker = false },
            title = { Text("选择优先级") },
            text = {
                Column {
                    Priority.entries.forEach { p ->
                        Text(
                            text = when (p) {
                                Priority.NONE -> "无"
                                Priority.LOW -> "低"
                                Priority.MEDIUM -> "中"
                                Priority.HIGH -> "高"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    priority = p
                                    showPriorityPicker = false
                                }
                                .padding(SpacingTokens.Small)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPriorityPicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}
