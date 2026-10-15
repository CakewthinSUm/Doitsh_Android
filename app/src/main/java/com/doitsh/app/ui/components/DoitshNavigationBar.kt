package com.doitsh.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass

/**
 * A top-level destination, shared by the bottom bar and the navigation rail so
 * the two containers can never drift apart.
 *
 * A destination carries its own [title] and [content] as well as its icon and
 * label, so the list of them is the single place a navigation container is
 * described: adding a button means adding one entry here, and the bar, the rail,
 * the ordering that drives the transition direction, and the entry provider all
 * follow from it.
 *
 * @param title the app bar title while this destination is showing. Distinct from
 *   [label], which is the short caption under the bar icon — the Tasks destination
 *   is labelled "Tasks" but titles the bar with the app name.
 * @param content what to render for this destination, given the content padding of
 *   the surrounding scaffold. A `@Composable (PaddingValues) -> Unit` rather than a
 *   `@Composable () -> Unit` because every screen here needs that padding to clear
 *   the top bar and the navigation container.
 */
class DoitshNavigationDestination(
    val key: NavKey,
    val icon: ImageVector,
    @StringRes val label: Int,
    @StringRes val title: Int,
    val content: @Composable (PaddingValues) -> Unit,
)

/**
 * Whether the top-level destinations belong in a rail rather than the bottom bar.
 *
 * This is a faithful port of the Material adaptive navigation suite's own rule
 * (`NavigationSuiteScaffoldDefaults.navigationSuiteType`), which is the normative
 * answer to "which navigation container for this window" — as executable code
 * rather than prose. The order of the checks is the part that matters:
 *
 * 1. **Compact width** (< 600dp) → bar. A phone in portrait, whatever its height.
 * 2. **Tabletop posture** → bar. A half-folded device has a hinge splitting the
 *    window, so a bar reads better than a rail pinned to one side.
 * 3. **Compact height** (< 480dp) → bar. This is the one that surprises people:
 *    a phone in *landscape* is wide but short, and the spec still asks for the
 *    bar (the `ShortNavigationBarMedium` configuration) rather than a rail,
 *    because a rail would eat the width the landscape layout wants.
 * 4. Otherwise → rail.
 *
 * Only when both width and height clear their medium breakpoints does a rail win.
 * The breakpoints are read through the public `is*AtLeastBreakpoint` helpers
 * because the suite's own `WidthSizeClasses`/`HeightSizeClasses` are `internal`.
 */
@Composable
fun rememberUseNavigationRail(): Boolean {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val sizeClass = adaptiveInfo.windowSizeClass
    val isCompactWidth =
        !sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isCompactHeight =
        !sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

    return !isCompactWidth && !adaptiveInfo.windowPosture.isTabletop && !isCompactHeight
}

/**
 * The Material 3 Expressive short navigation bar, in both of the configurations
 * the adaptive suite uses for it.
 *
 * - **Compact width** (below 600dp) puts the icons above their labels.
 * - **Medium width** (600dp and up) puts them beside the labels, which is the
 *   suite's `ShortNavigationBarMedium` configuration.
 *
 * Both keep the default `EqualWeight` arrangement. `ShortNavigationBar`'s own
 * KDoc suggests `Centered` for medium width, but the adaptive suite does not pass
 * an arrangement, and this component follows the suite.
 *
 * Render this only when [rememberUseNavigationRail] is false; the caller owns
 * that decision so the bar and the rail can never both appear.
 */
@Composable
fun DoitshNavigationBar(
    destinations: List<DoitshNavigationDestination>,
    selectedKey: NavKey,
    onSelect: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isMediumWidth =
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    ShortNavigationBar(modifier = modifier) {
        destinations.forEach { destination ->
            ShortNavigationBarItem(
                selected = destination.key == selectedKey,
                onClick = { onSelect(destination.key) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.label)) },
                iconPosition = if (isMediumWidth) {
                    NavigationItemIconPosition.Start
                } else {
                    NavigationItemIconPosition.Top
                },
                colors = DoitshNavigationItemColors,
            )
        }
    }
}

/**
 * The Expressive wide navigation rail, for windows wide *and* tall enough to
 * afford a side rail. See [rememberUseNavigationRail].
 *
 * This is `WideNavigationRail`, not the older `NavigationRail`: the adaptive suite
 * prefers it, and it is the rail that belongs to the same Expressive generation as
 * the bar. It stays collapsed — the app has no affordance for expanding it, and a
 * two-item rail has nothing to gain from the extra width.
 */
@Composable
fun DoitshNavigationRail(
    destinations: List<DoitshNavigationDestination>,
    selectedKey: NavKey,
    onSelect: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val railState = rememberWideNavigationRailState()
    val isExpanded = railState.currentValue == WideNavigationRailValue.Expanded

    WideNavigationRail(modifier = modifier, state = railState) {
        destinations.forEach { destination ->
            WideNavigationRailItem(
                selected = destination.key == selectedKey,
                onClick = { onSelect(destination.key) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.label)) },
                railExpanded = isExpanded,
                colors = DoitshNavigationItemColors,
            )
        }
    }
}

/**
 * Item colors for the bar.
 *
 * The selected pill uses `secondaryContainer` rather than the default
 * `secondary`-toned indicator: the bar sits directly on the app's `surface`, and
 * the container tone is the one that reads as a distinct pill against it in both
 * light and dark. The unselected icon and label are both `onSurfaceVariant` so a
 * destination reads as one unit when it is not selected.
 */
private val DoitshNavigationItemColors: NavigationItemColors
    @Composable get() = ShortNavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColorTopIconPosition = MaterialTheme.colorScheme.onSurface,
        selectedTextColorStartIconPosition = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
