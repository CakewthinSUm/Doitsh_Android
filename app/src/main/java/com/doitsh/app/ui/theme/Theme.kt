package com.doitsh.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive motion: springy spatial specs and emphasized easing for
 * every default animation (FAB menu, NavDisplay transitions, dialogs, ripples).
 * `MotionScheme.expressive()` is not `@Composable`, so it is a top-level val
 * rather than something rebuilt on each recomposition.
 */
private val ExpressiveMotionScheme = MotionScheme.expressive()

@Composable
fun DoitshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // The 5-argument overload — note MotionScheme is the *second* parameter.
    MaterialTheme(
        colorScheme = colorScheme,
        motionScheme = ExpressiveMotionScheme,
        shapes = ExpressiveShapes,
        typography = DoitshTypography,
        content = content
    )
}
