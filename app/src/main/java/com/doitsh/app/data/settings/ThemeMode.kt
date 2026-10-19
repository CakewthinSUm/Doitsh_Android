package com.doitsh.app.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which colour scheme the app draws in.
 *
 * Declaration order is the order the Settings screen draws the three buttons in.
 *
 * [SYSTEM] is the default and the behaviour the app shipped with: the scheme follows the
 * device's night mode — including if the device switches while the app is running, since
 * `isSystemInDarkTheme()` is re-read on every recomposition.
 */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM;

    /**
     * Whether this mode wants the dark scheme, given whether the device is in night mode.
     *
     * A plain function taking `systemInDarkTheme` as a parameter rather than calling the
     * `@Composable` `isSystemInDarkTheme()` inside, so the rule can be read and tested
     * without a device or a composition.
     */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        DARK -> true
        LIGHT -> false
        SYSTEM -> systemInDarkTheme
    }

    companion object {
        /**
         * Decoded by name rather than `valueOf`: a key that was never written, or a value
         * written by another build, must fall back to [SYSTEM] instead of throwing.
         */
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * The theme choice, shared by the Activity (which draws the theme and the system bars) and
 * the Settings screen (which changes it).
 *
 * Same shape and same reason as [TaskListPreferences]: writer and reader sit in different
 * parts of the tree, and the new scheme has to reach the whole window the moment it is
 * chosen — by then the Activity has already composed, so a SharedPreferences read taken
 * once at startup would only apply on the next launch.
 */
@Singleton
class ThemePreferences @Inject constructor(
    private val settings: UserSettings,
) {
    private val _themeMode = MutableStateFlow(settings.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun update(mode: ThemeMode) {
        settings.themeMode = mode
        _themeMode.value = mode
    }
}
