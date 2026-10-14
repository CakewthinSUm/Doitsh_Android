package com.doitsh.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doitsh.app.BuildConfig
import com.doitsh.app.R
import com.doitsh.app.data.settings.DataMode
import com.doitsh.app.data.settings.UserSettings

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onShowMessage: (String) -> Unit,
    // Defaulted to null rather than built in the default expression: `remember` is a
    // composable call, and a default argument is not a composable context.
    userSettings: UserSettings? = null
) {
    val context = LocalContext.current
    val settings = userSettings ?: remember(context) { UserSettings(context) }
    var mode by remember { mutableStateOf(settings.dataMode) }
    var serverUrl by remember { mutableStateOf(settings.serverUrl ?: "") }

    val localMessage = stringResource(R.string.settings_switched_local)
    val selfHostedMessage = stringResource(R.string.settings_switched_self_hosted)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_data_mode),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        val modes = listOf(DataMode.LOCAL to R.string.settings_mode_local,
            DataMode.SELF_HOSTED to R.string.settings_mode_self_hosted)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, (dataMode, labelRes) ->
                SegmentedButton(
                    selected = mode == dataMode,
                    onClick = {
                        mode = dataMode
                        settings.dataMode = dataMode
                        onShowMessage(
                            if (dataMode == DataMode.LOCAL) localMessage else selfHostedMessage
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = modes.size
                    ),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = mode == dataMode) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }

        if (mode == DataMode.SELF_HOSTED) {
            Text(
                text = stringResource(R.string.settings_server_connection),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    settings.serverUrl = it
                },
                label = { Text(stringResource(R.string.settings_server_url)) },
                placeholder = { Text(stringResource(R.string.settings_server_url_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = stringResource(R.string.settings_server_url_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.settings_about),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.settings_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
