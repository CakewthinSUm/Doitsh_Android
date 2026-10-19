package com.doitsh.app.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

enum class DataMode { LOCAL, SELF_HOSTED }

class UserSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("doitsh_prefs", Context.MODE_PRIVATE)

    var dataMode: DataMode
        get() = DataMode.entries.firstOrNull { it.name == prefs.getString(KEY_MODE, null) } ?: DataMode.LOCAL
        set(value) = prefs.edit { putString(KEY_MODE, value.name) }

    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        set(value) = prefs.edit { putString(KEY_SERVER_URL, value) }

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_AUTH_TOKEN, value) }

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    var passwordHash: String?
        get() = prefs.getString(KEY_PASSWORD_HASH, null)
        set(value) = prefs.edit { putString(KEY_PASSWORD_HASH, value) }

    /**
     * How the Today and Inbox lists are grouped and sorted.
     *
     * Decoded by name with a fallback per field rather than `valueOf`, for the same
     * reason [dataMode] does it that way: a value written by an older build, or a key
     * that was never written, must not throw. One transaction for the three keys so the
     * options can never be read half-updated.
     */
    var taskListOptions: TaskListOptions
        get() = TaskListOptions(
            grouping = TaskGrouping.entries.firstOrNull {
                it.name == prefs.getString(KEY_TASK_GROUPING, null)
            } ?: TaskListOptions().grouping,
            sort = TaskSort.entries.firstOrNull {
                it.name == prefs.getString(KEY_TASK_SORT, null)
            } ?: TaskListOptions().sort,
            direction = SortDirection.entries.firstOrNull {
                it.name == prefs.getString(KEY_TASK_DIRECTION, null)
            } ?: TaskListOptions().direction,
        )
        set(value) = prefs.edit {
            putString(KEY_TASK_GROUPING, value.grouping.name)
            putString(KEY_TASK_SORT, value.sort.name)
            putString(KEY_TASK_DIRECTION, value.direction.name)
        }

    val isConfigured: Boolean
        get() = when (dataMode) {
            DataMode.LOCAL -> true
            DataMode.SELF_HOSTED -> !serverUrl.isNullOrBlank()
        }

    companion object {
        private const val KEY_MODE = "data_mode"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_TASK_GROUPING = "task_grouping"
        private const val KEY_TASK_SORT = "task_sort"
        private const val KEY_TASK_DIRECTION = "task_direction"
    }
}
