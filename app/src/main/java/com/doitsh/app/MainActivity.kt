package com.doitsh.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.doitsh.app.data.settings.ThemeMode
import com.doitsh.app.data.settings.ThemePreferences
import com.doitsh.app.data.settings.UserSettings
import com.doitsh.app.ui.DoitshApp
import com.doitsh.app.ui.theme.DoitshTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Field-injected rather than held in a ViewModel: the theme is process-wide state and
    // the Activity only reads it. The Settings screen writes through the same singleton.
    @Inject lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate, because that is when the window is created from the
        // manifest theme and painted for the first time. Until now the launch window has
        // followed the system (values/ vs values-night/); a pinned Light or Dark has to
        // carry over, or the app opens with a flash of the opposite scheme. Read straight
        // from SharedPreferences because the window exists before Hilt has injected
        // anything — hence a throwaway instance rather than the singleton below.
        setTheme(
            when (UserSettings(this).themeMode) {
                ThemeMode.LIGHT -> R.style.Theme_Doitsh_Light
                ThemeMode.DARK -> R.style.Theme_Doitsh_Dark
                ThemeMode.SYSTEM -> R.style.Theme_Doitsh
            }
        )
        super.onCreate(savedInstanceState)
        // Called before setContent so the first frame is already edge-to-edge; the
        // explicit styles are re-applied below once the theme is known.
        enableEdgeToEdge()
        setContent {
            val themeMode by themePreferences.themeMode.collectAsState()
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())

            // The navigation and status bar icons have to contrast with the surface the
            // app actually draws, not with the device's night mode — picking Light while
            // the system is dark would otherwise leave white icons on a white bar. The
            // default `auto` style reads the system configuration, so it is replaced here
            // whenever the chosen mode resolves to a different scheme.
            SideEffect {
                val barStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            DoitshTheme(darkTheme = darkTheme) {
                DoitshApp()
            }
        }
    }
}
