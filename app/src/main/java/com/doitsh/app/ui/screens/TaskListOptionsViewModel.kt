package com.doitsh.app.ui.screens

import androidx.lifecycle.ViewModel
import com.doitsh.app.data.settings.TaskListOptions
import com.doitsh.app.data.settings.TaskListPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * The top bar menu's state: the one set of list options both pages draw with.
 *
 * Injected by the shell, so the menu belongs to the top bar rather than to either page —
 * switching to the Inbox and back must not reset the choice, and the button must show the
 * options the visible list is actually using. All three read the same singleton
 * [TaskListPreferences], which is what keeps them in sync without any plumbing here.
 */
@HiltViewModel
class TaskListOptionsViewModel @Inject constructor(
    private val preferences: TaskListPreferences,
) : ViewModel() {

    val options: StateFlow<TaskListOptions> = preferences.options

    fun update(options: TaskListOptions) {
        preferences.update(options)
    }
}
