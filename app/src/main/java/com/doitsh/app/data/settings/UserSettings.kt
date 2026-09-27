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
    }
}
