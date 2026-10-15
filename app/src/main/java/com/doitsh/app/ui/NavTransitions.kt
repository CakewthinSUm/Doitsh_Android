package com.doitsh.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
 * The one animation behind both `popTransitionSpec` and `predictivePopTransitionSpec`: the
 * screen being dismissed slides away to the trailing edge, revealing the screen underneath,
 * which comes back in from the leading edge at a third of the travel.
 *
 * The direction is fixed on purpose, and it is a different question from the one
 * [navigationSlideDirection] answers. A pop is a stack operation, not a move between two
 * positions in a navigation container: the screen underneath may not be a bar destination
 * at all (Settings has no bar position to compare against), and revealing it from the edge
 * the user is gesturing towards is what makes back feel like back. Deriving this from bar
 * order would send a Settings pop the wrong way.
 *
 * ## Why pop and predictive back share this, rather than each having a spec
 *
 * A predictive back gesture does not play the pop transition, it *seeks* it: NavDisplay maps
 * the gesture's progress straight onto the transition's fraction. When the finger lifts,
 * `inPredictiveBack` goes false and the very same transition is played the rest of the way
 * under `popTransitionSpec` — NavDisplay re-derives which spec to use on every frame, so the
 * hand-off swaps the spec mid-flight. If the two specs describe different animations, the
 * page jumps at exactly the moment the user expects the gesture to finish what it started.
 *
 * That is what a scale-out predictive spec against a slide-out pop spec looked like: a drag
 * shrank the page towards its centre, and the release snapped it back to full size and sent
 * it off sideways. Both halves read as bugs from the outside, and which one you noticed
 * depended only on how far the gesture got.
 *
 * Returning one spec from one function is what makes them impossible to drift apart again.
 *
 * ## Why there is no fade
 *
 * A seeked transition is evaluated by *play time*: fraction f means play time
 * `f * totalDuration`, and any opacity transition is finished once play time passes its
 * duration. At this spec's ~328 ms, a fade is over by around fraction 0.18 — so it plays out
 * in the first fifth of the drag and the page then spends the rest of the gesture sliding as
 * a ghost. Reaching past that means a delay eating most of the duration, which leaves a fade
 * too short to read.
 *
 * A slide has no such problem: it is proportional to the fraction, so it tracks the finger
 * for the whole gesture and is still mid-flight at any point the user lets go. That is also
 * why the released animation needs no fade of its own — the page is already on its way out.
 *
 * The platform can hold a dismissed Activity opaque for the whole drag and fade it only on
 * release; in-app Nav3 reports no release event and hands the gesture the entire fraction
 * range, so there is no such moment to hang a fade on. A fade in a spec this long is
 * therefore not a small fade — it is a fade the user watches end before they have finished
 * dragging. `docs/navbar-comparison.png` is the reference the card design was matched
 * against; Todoist's Settings is a separate Activity, so its release-fade is not reachable
 * from here.
 */
@Composable
private fun <T : Any> expressivePopSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    return {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = spatial,
            // A third of the full slide: the revealed screen is already sitting behind the
            // one leaving, so it should be uncovered rather than travel the whole width.
            initialOffset = { it / 3 }
        ) togetherWith
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = spatial
            )
    }
}

/**
 * The `popTransitionSpec`: a system back press that is not a predictive gesture (a hardware
 * key, or an older platform) runs this to completion on its own clock.
 */
@Composable
fun <T : Any> expressivePopTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    expressivePopSpec()

/**
 * Predictive pop: used while the user is still dragging the system back gesture, so this spec
 * is driven by gesture progress rather than running on its own clock.
 *
 * It is deliberately **identical** to [expressivePopTransition] — both are [expressivePopSpec]
 * — so the release continues the animation the drag started. See that function for why.
 *
 * The `Int` is the swipe edge the gesture came from; the animation is the same either way,
 * because a pop always reveals towards the edge the user is pulling from.
 */
@Composable
fun <T : Any> expressivePredictivePopTransition(): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform {
    val pop = expressivePopSpec<T>()
    return { _ -> pop(this) }
}
