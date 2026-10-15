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

    // stringResource is a composable call, so every message has to be resolved here:
    // the SegmentedButton onClick below is a plain lambda, not a composable context.
    val restartHint = stringResource(R.string.settings_mode_applies_after_restart)
    val localMessage = stringResource(R.string.settings_switched_local, restartHint)
    val selfHostedMessage = stringResource(R.string.settings_switched_self_hosted, restartHint)
    val needsUrlMessage = stringResource(R.string.settings_self_hosted_needs_url)

    // Blank is not an error: the field starts empty. Anything non-blank that is not a
    // usable URL is.
    val urlIsInvalid = serverUrl.isNotEmpty() && !isValidServerUrl(serverUrl)

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
                        // `isConfigured` is read after the write above, so it already
                        // reflects the mode just selected (SharedPreferences reads are
                        // synchronous and hit the in-memory map).
                        onShowMessage(
                            when {
                                dataMode == DataMode.LOCAL -> localMessage
                                settings.isConfigured -> selfHostedMessage
                                else -> needsUrlMessage
                            }
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
                isError = urlIsInvalid,
                supportingText = if (urlIsInvalid) {
                    { Text(stringResource(R.string.settings_server_url_invalid)) }
                } else {
                    null
                },
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

/**
 * A server URL is only usable with a scheme and a host — Ktor's
 * `defaultRequest { url(baseUrl) }` throws on anything else. Deliberately permissive:
 * it catches the obvious mistakes, it does not police the user.
 */
private fun isValidServerUrl(raw: String): Boolean {
    val uri = runCatching { java.net.URI(raw.trim()) }.getOrNull() ?: return false
    return (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
}
