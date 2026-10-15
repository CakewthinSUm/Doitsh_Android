package com.doitsh.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavMetadataKey
// `get` is the operator that reads a NavMetadataKey back out of an entry's metadata map.
// Kotlin needs it imported explicitly; without it, `map[key]` silently falls back to the
// built-in Map.get and the value comes back as `Any?` instead of the key's type.
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene

/**
 * Expressive navigation transitions.
 *
 * Navigation 3 ships its own defaults, and they do not read the theme's motion
 * scheme: `NavDisplayKt.DEFAULT_TRANSITION_DURATION_MILLISECOND` is a hard-coded
 * 700, and both the forward and pop specs are a bare cross-fade at that duration
 * (`tween(700)` fadeIn/fadeOut, no translation). On top of `MotionScheme.expressive()`
 * that reads as sluggish and flat — two opaque screens dissolving into each other
 * rather than one screen replacing another.
 *
 * These specs replace that with a horizontal shared-axis push driven by the motion
 * scheme the theme already installs (`DoitshTheme` passes `MotionScheme.expressive()`),
 * so navigation finally animates on the same spring physics as the rest of the app.
 *
 * Spatial motion (position, scale) uses the *spatial* specs; opacity uses the
 * *effects* specs. That split is the point of the two families in `MotionScheme` —
 * using one for both is what makes M3 Expressive motion look wrong.
 *
 * ## Which way a screen travels
 *
 * NavDisplay never tells a transition *why* it is running. It computes one `isPop` flag
 * from the shape of the back stack — `NavDisplay.kt`'s `isPop()`: same first element, a
 * strictly shorter stack, no divergence — and runs `popTransitionSpec` when it is true
 * and `transitionSpec` otherwise. Selecting a bottom bar destination never looks like a
 * pop, because the top-level select helper replaces the stack rather than pushing onto
 * it. So *both* bar taps run `transitionSpec`, and a direction hard-coded there sends
 * Tasks → Projects and Projects → Tasks the same way, whichever button was pressed.
 *
 * The direction is therefore read from the destinations themselves rather than chosen
 * here. Each destination publishes its position in the navigation container as entry
 * metadata ([navigationOrder]), and [navigationSlideDirection] compares the outgoing and
 * incoming positions: a destination further along the bar is entered from the trailing
 * edge, and one further back from the leading edge. Nothing in this file names a
 * destination, so adding, removing or reordering bar buttons — or building a custom set
 * of them — needs no change to these specs.
 */

/**
 * The metadata key under which a destination publishes its position in the navigation
 * container. An `object`, not an anonymous one, so `toString()` — which is how the
 * metadata map is keyed — is stable.
 */
internal object NavigationOrderKey : NavMetadataKey<Int>

/**
 * Publishes [index] as a destination's position in the navigation container. Pass the
 * result to the `metadata` parameter of `entry { }`; see `DoitshApp`.
 */
internal fun navigationOrder(index: Int): Map<String, Any> =
    metadata { put(NavigationOrderKey, index) }

/** The position this scene's destination published, or null if it published none. */
private fun Scene<*>.navigationOrder(): Int? = this.metadata[NavigationOrderKey]

/**
 * The direction to slide when moving between two positions in the navigation container.
 *
 * `SlideDirection.Left` means the content travels leftward, so the incoming screen
 * enters from the trailing edge — the direction you would expect when the button you
 * pressed is to the right of the one you came from. `Right` is its mirror.
 *
 * Either position being `null` — anything pushed on top of the stack instead of selected
 * from the container, such as Settings — keeps the container's forward convention and
 * enters from the trailing edge, which is what a push should look like.
 *
 * Split out from the transition itself, and taking positions rather than scenes, so the
 * rule is a pure function that can be tested without a navigation host.
 */
internal fun navigationSlideDirection(
    from: Int?,
    to: Int?,
): AnimatedContentTransitionScope.SlideDirection {
    val movedBackward = from != null && to != null && to < from
    return if (movedBackward) {
        AnimatedContentTransitionScope.SlideDirection.Right
    } else {
        AnimatedContentTransitionScope.SlideDirection.Left
    }
}

/**
 * The `transitionSpec`: the incoming screen pushes in from the side the destination sits
 * on, while the outgoing screen slides a third of the way out and fades. Which side that
 * is comes from [navigationSlideDirection], so a bar tap animates in the direction of the
 * button that was pressed.
 *
 * The outgoing screen deliberately does not travel the full width — it parallaxes behind
 * the incoming one, which is what gives the push a sense of depth rather than a flat wipe.
 */
@Composable
fun <T : Any> expressiveSceneTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    return {
        val direction = navigationSlideDirection(
            from = initialState.navigationOrder(),
            to = targetState.navigationOrder(),
        )
        slideIntoContainer(
            towards = direction,
            animationSpec = spatial
        ) + fadeIn(animationSpec = effects) togetherWith
            slideOutOfContainer(
                towards = direction,
                animationSpec = spatial,
                // A third of the full slide: the outgoing screen keeps its place in
                // the hierarchy instead of exiting the frame entirely.
                targetOffset = { it / 3 }
            ) + fadeOut(animationSpec = effects)
    }
}

/**
 * The `popTransitionSpec`: the screen being revealed comes back in from the leading edge
 * and the screen being dismissed slides away to the trailing edge.
 *
 * This direction is fixed on purpose, and it is a different question from the one
 * [navigationSlideDirection] answers. A pop is a stack operation, not a move between two
 * positions in a navigation container: the screen underneath may not be a bar destination
 * at all (Settings has no bar position to compare against), and revealing it from the
 * edge the user is gesturing towards is what makes back feel like back. Deriving this from
 * bar order would send a Settings pop the wrong way.
 */
@Composable
fun <T : Any> expressivePopTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    return {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = spatial,
            initialOffset = { it / 3 }
        ) + fadeIn(animationSpec = effects) togetherWith
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = spatial
            ) + fadeOut(animationSpec = effects)
    }
}

/**
 * Predictive pop: used while the user is still dragging the system back gesture, so
 * this spec is driven by gesture progress rather than running on its own clock. The
 * outgoing screen shrinks towards the centre as the finger travels, which is the
 * Expressive "the screen recedes because you are leaving it" cue.
 *
 * The scale animation uses the spatial spec (it is a spatial transform) while the
 * fade uses the effects spec.
 */
@Composable
fun <T : Any> expressivePredictivePopTransition(): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    return { _ ->
        fadeIn(animationSpec = effects) togetherWith
            scaleOut(animationSpec = spatial, targetScale = 0.9f) +
            fadeOut(animationSpec = effects)
    }
}
