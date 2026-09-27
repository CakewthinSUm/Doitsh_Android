package com.doitsh.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1B6EF3),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD6E3FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001A40),
    secondary = androidx.compose.ui.graphics.Color(0xFF575E71),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFDBE2F9),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF141B2C),
    tertiary = androidx.compose.ui.graphics.Color(0xFF725572),
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFCD7FA),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF2A132C),
    surface = androidx.compose.ui.graphics.Color(0xFFFCFCFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1B1F),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE0E2EC),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF44474E),
    outline = androidx.compose.ui.graphics.Color(0xFF74777F),
    background = androidx.compose.ui.graphics.Color(0xFFFCFCFF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1B1F),
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
    onError = androidx.compose.ui.graphics.Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFAAC7FF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF002F65),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF00458E),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFD6E3FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFBFC6DC),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF293041),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF3F4759),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFDBE2F9),
    tertiary = androidx.compose.ui.graphics.Color(0xFFDFBBDD),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF402842),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF583E59),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFCD7FA),
    surface = androidx.compose.ui.graphics.Color(0xFF1A1B1F),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE3E2E6),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF44474E),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC4C6D0),
    outline = androidx.compose.ui.graphics.Color(0xFF8E9099),
    background = androidx.compose.ui.graphics.Color(0xFF1A1B1F),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE3E2E6),
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    onError = androidx.compose.ui.graphics.Color(0xFF690005),
)

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
