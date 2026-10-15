package com.doitsh.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    // the onCheckedChange lambdas below are plain lambdas, not composable contexts.
    val restartHint = stringResource(R.string.settings_mode_applies_after_restart)
    val localMessage = stringResource(R.string.settings_switched_local, restartHint)
    val selfHostedMessage = stringResource(R.string.settings_switched_self_hosted, restartHint)
    val needsUrlMessage = stringResource(R.string.settings_self_hosted_needs_url)
    // Same reason: ButtonGroup's content lambda is a plain ButtonGroupScope.() -> Unit.
    val modeLabels = mapOf(
        DataMode.LOCAL to stringResource(R.string.settings_mode_local),
        DataMode.SELF_HOSTED to stringResource(R.string.settings_mode_self_hosted)
    )

    // Blank is not an error: the field starts empty. Anything non-blank that is not a
    // usable URL is.
    val urlIsInvalid = serverUrl.isNotEmpty() && !isValidServerUrl(serverUrl)

    fun select(dataMode: DataMode) {
        mode = dataMode
        settings.dataMode = dataMode
        // `isConfigured` is read after the write above, so it already reflects the mode
        // just selected (SharedPreferences reads are synchronous and hit the in-memory
        // map). It is also read only here, inside a callback — never during composition —
        // so its lack of Compose state does not matter.
        onShowMessage(
            when {
                dataMode == DataMode.LOCAL -> localMessage
                settings.isConfigured -> selfHostedMessage
                else -> needsUrlMessage
            }
        )
    }

    // Same contentPadding convention as Home/Projects, minus the extra FAB clearance:
    // Settings has no floating action button (DoitshApp gates the FAB on !Settings).
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = stringResource(R.string.settings_data_mode)) {
                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DataMode.entries.forEach { dataMode ->
                        toggleableItem(
                            checked = mode == dataMode,
                            label = modeLabels.getValue(dataMode),
                            onCheckedChange = { select(dataMode) },
                            weight = 1f,
                        )
                    }
                }
            }
        }

        if (mode == DataMode.SELF_HOSTED) {
            item {
                SettingsSection(title = stringResource(R.string.settings_server_connection)) {
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
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_about)) {
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A titled card grouping related settings. The container colour matches the ListItem
 * rows on the Tasks/Projects screens rather than the Card default
 * (surfaceContainerHighest), which would sit darker than the page behind it.
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
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
