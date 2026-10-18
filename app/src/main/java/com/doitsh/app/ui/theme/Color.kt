package com.doitsh.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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

/**
 * The container colour of a task row.
 *
 * `surfaceContainerHigh` in both schemes, which the light scheme provides directly but
 * the dark one has to be told about: in [DarkColorScheme] `surfaceContainerLow` is the
 * *same colour as `background`*, so a row using it would be invisible rather than
 * merely faint. One named accessor means the list does not have to branch on the theme,
 * and the value can be re-tuned here without touching the row.
 *
 * The ramp is intentionally gentle — `surface` to `surfaceContainerHighest` is 1.24:1 in
 * light — because M3 containers express elevation with colour *plus* shape, not colour
 * alone. A segmented group is the case where that assumption needs the top of the ramp:
 * the segments have to be distinguishable from the page for the shared container to
 * read, and the container itself has to stay lighter than the content it holds.
 */
val androidx.compose.material3.ColorScheme.taskRowContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = surfaceContainerHighest

/**
 * Priority accents: the 3dp bar at the start of a task row.
 *
 * These are not part of [androidx.compose.material3.ColorScheme] — M3 has no "priority"
 * role — so they live here as a separate, deliberately small palette rather than being
 * wedged into an unrelated slot. Each is a colour that reads on both the light and the
 * dark row container, which is why the dark values are lighter and desaturated rather
 * than the same hues at full strength.
 *
 * They are *not* derived from `error`/`tertiary`/`primary`: priority is not an error and
 * is not the brand colour, and borrowing those roles would make a high-priority task
 * look like a failure state.
 */
private val LightPriorityHigh = Color(0xFFC0392B)
private val LightPriorityMedium = Color(0xFFC77C00)
private val LightPriorityLow = Color(0xFF5E8C4A)

private val DarkPriorityHigh = Color(0xFFFF8A80)
private val DarkPriorityMedium = Color(0xFFFFC46B)
private val DarkPriorityLow = Color(0xFFA5D18E)

/**
 * The current scheme's priority colours.
 *
 * A `@Composable` accessor rather than colour-scheme roles, so a caller reads them the
 * same way it reads `MaterialTheme.colorScheme.error`. Chosen by the scheme's luminance
 * instead of by a separate dark-theme flag: the theme already decided which scheme is
 * in play, and asking it again here would be a second source of truth that can disagree.
 */
val androidx.compose.material3.ColorScheme.priorityHigh: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLightScheme()) LightPriorityHigh else DarkPriorityHigh

val androidx.compose.material3.ColorScheme.priorityMedium: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLightScheme()) LightPriorityMedium else DarkPriorityMedium

val androidx.compose.material3.ColorScheme.priorityLow: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLightScheme()) LightPriorityLow else DarkPriorityLow

/**
 * Whether the scheme in play is the light one.
 *
 * `surface` is white-ish in the light scheme and near-black in the dark one, so its
 * luminance is the honest question — unlike `isSystemInDarkTheme()`, which would be
 * wrong the moment the app is shown with an explicit `darkTheme = false`.
 */
@Composable
@ReadOnlyComposable
private fun isLightScheme(): Boolean =
    MaterialTheme.colorScheme.surface.luminance() > 0.5f

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
