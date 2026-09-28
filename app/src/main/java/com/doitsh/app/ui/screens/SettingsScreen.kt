package com.doitsh.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.doitsh.app.data.settings.DataMode
import com.doitsh.app.data.settings.UserSettings
import com.doitsh.app.ui.components.SectionHeader
import com.doitsh.app.ui.theme.SpacingTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    userSettings: UserSettings = LocalContext.current.let {
        UserSettings(it)
    }
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(userSettings.dataMode) }
    var serverUrl by remember { mutableStateOf(userSettings.serverUrl ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(SpacingTokens.Default),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.XLarge)
    ) {
        // Data Mode section
        SectionHeader(title = "Data Mode")

        Row(horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Small)) {
            FilterChip(
                selected = mode == DataMode.LOCAL,
                onClick = {
                    mode = DataMode.LOCAL
                    userSettings.dataMode = mode
                    Toast.makeText(context, "Switched to Local mode", Toast.LENGTH_SHORT).show()
                },
                label = { Text("Local") },
                leadingIcon = if (mode == DataMode.LOCAL) {
                    { Icon(Icons.Filled.CheckCircle, contentDescription = null) }
                } else null,
                shape = MaterialTheme.shapes.small
            )
            FilterChip(
                selected = mode == DataMode.SELF_HOSTED,
                onClick = {
                    mode = DataMode.SELF_HOSTED
                    userSettings.dataMode = mode
                    Toast.makeText(context, "Switched to Self-hosted mode", Toast.LENGTH_SHORT).show()
                },
                label = { Text("Self-hosted") },
                leadingIcon = if (mode == DataMode.SELF_HOSTED) {
                    { Icon(Icons.Filled.CheckCircle, contentDescription = null) }
                } else null,
                shape = MaterialTheme.shapes.small
            )
        }

        // Server Connection section (animated visibility)
        AnimatedVisibility(
            visible = mode == DataMode.SELF_HOSTED,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.Small)) {
                SectionHeader(title = "Server Connection")

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        userSettings.serverUrl = it
                    },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.1.100:8080") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraSmall
                )

                Text(
                    text = "Point to your NAS or home server running the Doitsh backend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // About section
        SectionHeader(title = "About")

        Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.XSmall)) {
            Text(
                text = "Doitsh v1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Native task management · Material Design 3 Expressive",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
