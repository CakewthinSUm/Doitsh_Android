package com.doitsh.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Hand-built schemes seeded from a single blue (#1B6EF3) rather than
 * `expressiveLightColorScheme()`: material3 1.5.0-beta01 ships no
 * `expressiveDarkColorScheme()`, so using the Expressive light scheme alone would
 * make the two modes inconsistent. Both modes are therefore tonally matched here,
 * including the full surface-container ramp the M3 components read.
 */

private val LightPrimary = Color(0xFF1B6EF3)
private val LightOnPrimary = Color.White
private val LightPrimaryContainer = Color(0xFFD6E3FF)
private val LightOnPrimaryContainer = Color(0xFF001A40)

private val DarkPrimary = Color(0xFFAAC7FF)
private val DarkOnPrimary = Color(0xFF002F65)
private val DarkPrimaryContainer = Color(0xFF00458E)
private val DarkOnPrimaryContainer = Color(0xFFD6E3FF)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = Color(0xFF575E71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBE2F9),
    onSecondaryContainer = Color(0xFF141B2C),
    tertiary = Color(0xFF725572),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCD7FA),
    onTertiaryContainer = Color(0xFF2A132C),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1B1F),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    surfaceTint = LightPrimary,
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = DarkPrimary,
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    scrim = Color.Black,
    // The container ramp the Expressive components (ListItem, NavigationRail,
    // ShortNavigationBar) pick their backgrounds from.
    surfaceDim = Color(0xFFDCD9DF),
    surfaceBright = Color(0xFFFCFCFF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F6FA),
    surfaceContainer = Color(0xFFF0F0F4),
    surfaceContainerHigh = Color(0xFFEAEAEF),
    surfaceContainerHighest = Color(0xFFE5E4E9),
)

val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293041),
    secondaryContainer = Color(0xFF3F4759),
    onSecondaryContainer = Color(0xFFDBE2F9),
    tertiary = Color(0xFFDFBBDD),
    onTertiary = Color(0xFF402842),
    tertiaryContainer = Color(0xFF583E59),
    onTertiaryContainer = Color(0xFFFCD7FA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1B1F),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1B1F),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceTint = DarkPrimary,
    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF1A1B1F),
    inversePrimary = LightPrimary,
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    scrim = Color.Black,
    surfaceDim = Color(0xFF1A1B1F),
    surfaceBright = Color(0xFF404246),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1A1B1F),
    surfaceContainer = Color(0xFF1E1F23),
    surfaceContainerHigh = Color(0xFF282A2E),
    surfaceContainerHighest = Color(0xFF333539),
)
