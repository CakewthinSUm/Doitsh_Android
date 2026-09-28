package com.doitsh.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.doitsh.app.data.settings.DataMode
import com.doitsh.app.data.settings.UserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    userSettings: UserSettings = androidx.compose.ui.platform.LocalContext.current.let {
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Data Mode",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                } else null
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
                } else null
            )
        }

        if (mode == DataMode.SELF_HOSTED) {
            Text(
                text = "Server Connection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    userSettings.serverUrl = it
                },
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.100:8080") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "Point to your NAS or home server running the Doitsh backend.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Doitsh v1.0.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Native task management · Material Design Express",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
