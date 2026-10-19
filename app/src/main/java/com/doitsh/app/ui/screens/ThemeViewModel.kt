package com.doitsh.app.ui.screens

import androidx.lifecycle.ViewModel
import com.doitsh.app.data.settings.ThemeMode
import com.doitsh.app.data.settings.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * The Settings screen's theme choice.
 *
 * Only the screen needs a ViewModel here: the theme itself is read by `MainActivity`
 * straight from the singleton [ThemePreferences], so choosing a mode redraws the whole
 * window without any state having to travel back up through the shell.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferences: ThemePreferences,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode

    fun select(mode: ThemeMode) {
        preferences.update(mode)
    }
}
