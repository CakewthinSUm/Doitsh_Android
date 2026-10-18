package com.doitsh.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
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
 * These specs replace that with motion driven by the scheme the theme already installs
 * (`DoitshTheme` passes `MotionScheme.expressive()`), so navigation animates on the same
 * spring physics as the rest of the app.
 *
 * All transitions here are positional, so they use the *spatial* specs.
 *
 * ## The one rule: the animation is a function of the two scenes, and nothing else
 *
 * NavDisplay picks between its three specs on every frame, from state that changes
 * *during* a single gesture, so the same pair of screens can be animated by a different
 * spec at each stage. `NavDisplay.kt` decides:
 *
 * - `inPredictiveBack` (line 623) — true only while the gesture is still `InProgress`
 * - `isPop` (line 638) — recomputed from the *back stack*, which a cancelled gesture
 *   never changes
 * - otherwise the forward spec (line 804)
 *
 * A predictive back gesture walks through all three. While the finger is down the back
 * stack is untouched, so the drag runs `predictivePopTransitionSpec`. If the gesture
 * *completes* the stack finally pops and the release finishes under `popTransitionSpec`.
 * If it is *cancelled* the stack is still untouched, so `isPop` is false, and
 * `inPredictiveBack` is false the moment the finger lifts — so the rewind runs the
 * **forward** spec, over the very same segment the drag was seeking.
 *
 * One Settings → Home transition is therefore driven by up to three different specs, and
 * if they disagree the page jumps at the instant the user lets go — by an amount that
 * depends on the fraction the finger happened to release at, which is why the same
 * gesture looks fine one time and broken the next. This file exists to make that
 * impossible: [navigationMotion] and the transform it selects read only `initialState` and
 * `targetState`, and all three specs return the same function. Which spec Nav3 happens to
 * select is then unobservable.
 *
 * ## What a segment is
 *
 * NavDisplay never tells a transition *why* it is running, so the motion has to be read
 * off the scenes themselves. A destination that lives on the stack's base level publishes
 * its position in the navigation container as entry metadata ([navigationOrder]); Settings,
 * which is pushed on top, publishes none. That gives two cases, and [navigationMotion] is
 * the whole rule:
 *
 * - **A position to none** — a screen pushed on top of the base level. A slide-in from
 *   the trailing edge ([pushTransform]), the container's forward convention.
 * - **None to a position** — leaving the pushed screen. A reveal ([revealTransform]), and
 *   the geometry the predictive back drag seeks.
 *
 * The three top-level pages are not navigation segments at all: they sit in one
 * `HorizontalPager` inside the single `Home` destination (`DoitshApp`), so moving between
 * them is a scroll the pager animates, not a transition NavDisplay composes.
 *
 * Nothing in this file names a destination, so a different base level, or more than one,
 * needs no change to these specs.
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
 * What a transition segment is doing, decided purely by the two container positions.
 *
 * Kept as a separate value rather than folded into the transform so the rule is a pure
 * function that can be tested without a navigation host — the same reason
 * [navigationMotion] is split out. This is the rule the three specs have to agree on, so
 * it is the one worth pinning down in tests.
 */
internal sealed interface NavigationMotion {

    /**
     * A push onto the stack: the destination being pushed published no position, so it
     * enters from the trailing edge, which is the container's forward convention.
     */
    data object Push : NavigationMotion

    /**
     * Leaving a pushed screen and revealing whatever it was covering — a pop, a
     * non-predictive system back press, or the rewind of a cancelled predictive gesture.
     *
     * All three are the same motion, which is the point: the cancelled case is a pop that
     * is being undone, so it has to look like the pop it interrupted, not like a push.
     */
    data object Reveal : NavigationMotion
}

/**
 * Which motion a segment between two container positions is.
 *
 * A segment leaving a scene that published no position is a reveal. Only a pushed screen
 * publishes none, so this is exactly "is the screen being left the one that was pushed" —
 * and it holds whether the segment is being played forwards, played as a pop, or *seeked*
 * and rewound, because all three see the same two scenes.
 *
 * This is what makes a cancelled back gesture continue the pop it started. The rewind runs
 * under Nav3's forward spec, but the segment still reads (pushed screen → the screen it
 * covered), so it resolves to [NavigationMotion.Reveal] and gets the pop's geometry rather
 * than the push's.
 */
internal fun navigationMotion(from: Int?, to: Int?): NavigationMotion =
    when {
        from == null && to != null -> NavigationMotion.Reveal
        // Entering a pushed screen, or moving between two of them: forward, from the
        // trailing edge.
        else -> NavigationMotion.Push
    }

/**
 * The transition for [NavigationMotion.Push]: the incoming screen pushes in from the
 * trailing edge, while the outgoing screen slides a third of the way out.
 *
 * The outgoing screen deliberately does not travel the full width — it parallaxes behind
 * the incoming one, which is what gives the push a sense of depth rather than a flat wipe.
 *
 * The direction is fixed rather than derived from bar order: a push is a stack operation,
 * and the screen underneath is the one this screen was opened from, not a neighbouring
 * destination in the bar.
 */
private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.pushTransform(
    spatial: FiniteAnimationSpec<IntOffset>,
): ContentTransform =
    slideIntoContainer(
        towards = SlideDirection.Left,
        animationSpec = spatial,
    ) togetherWith
        slideOutOfContainer(
            towards = SlideDirection.Left,
            animationSpec = spatial,
            // A third of the full slide: the outgoing screen keeps its place in
            // the hierarchy instead of exiting the frame entirely.
            targetOffset = { it / 3 },
        )

/**
 * The transition for [NavigationMotion.Reveal]: the screen being dismissed slides away to
 * the trailing edge, and the screen underneath comes back in from the leading edge at a
 * third of the travel.
 *
 * The direction is fixed rather than derived. A reveal is a stack operation, not a move
 * between two positions in a navigation container: the screen underneath may not be a bar
 * destination at all (Settings has no bar position to compare against), and uncovering it
 * from the edge the user is gesturing towards is what makes back feel like back. Deriving
 * this from bar order would send a Settings pop the wrong way.
 *
 * Note which screen parallaxes, compared with [pushTransform]: here it is the one being
 * *revealed* that travels only a third, because it is already sitting in the frame behind
 * the one leaving. In a push it is the screen being *covered* that travels a third, because
 * the incoming screen is arriving over it. These are not the same motion with the sign
 * flipped, and swapping them is what made a cancelled gesture jump.
 */
private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.revealTransform(
    spatial: FiniteAnimationSpec<IntOffset>,
): ContentTransform =
    slideIntoContainer(
        towards = SlideDirection.Right,
        animationSpec = spatial,
        // A third of the full slide: the revealed screen is already sitting behind the
        // one leaving, so it should be uncovered rather than travel the whole width.
        initialOffset = { it / 3 },
    ) togetherWith
        slideOutOfContainer(
            towards = SlideDirection.Right,
            animationSpec = spatial,
        )

/**
 * The transform for one transition segment, decided by the two scenes alone.
 *
 * ## Why nothing here has opacity in it
 *
 * A *seeked* transition is evaluated by play time: fraction `f` means play time
 * `f * totalDuration`, and any opacity animation is finished once play time passes its
 * duration. At this spec's ~328 ms a fade would be over by around fraction 0.18 — so a
 * predictive drag would play it out in its first fifth and then spend the rest of the
 * gesture sliding a ghost. A slide has no such problem: it is proportional to the fraction,
 * so it tracks the finger for the whole gesture and is still mid-flight whenever the user
 * lets go.
 *
 * That is why [pushTransform] and [revealTransform] — the two that a predictive back
 * gesture can seek — are purely spatial, and why no spec swap can move an opacity value on
 * those segments: the cross-fade that used to appear when a cancelled gesture rewound under
 * a different spec is not a bug that can come back. The parallax already carries the depth
 * a fade would have added.
 *
 * Both are slides, so the whole file uses one spec family.
 *
 * The platform can hold a dismissed Activity opaque for the whole drag and fade it only on
 * release; in-app Nav3 reports no release event and hands the gesture the entire fraction
 * range, so there is no such moment to hang a fade on. `docs/navbar-comparison.png` is the
 * reference the card design was matched against; Todoist's Settings is a separate Activity,
 * so its release-fade is not reachable from here.
 */
private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.sceneTransform(
    spatial: FiniteAnimationSpec<IntOffset>,
): ContentTransform =
    when (navigationMotion(initialState.navigationOrder(), targetState.navigationOrder())) {
        NavigationMotion.Push -> pushTransform(spatial)
        NavigationMotion.Reveal -> revealTransform(spatial)
    }

/**
 * Builds the one transform all three specs return. See [sceneTransform].
 *
 * A plain lambda rather than a `@Composable` one: the motion scheme is read here, during
 * composition, and the spec Nav3 stores is only ever invoked from `remember` blocks in
 * `AnimatedContent` — not from a composable context.
 */
@Composable
private fun <T : Any> sceneTransformSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    return { sceneTransform(spatial) }
}

/**
 * The `transitionSpec`: a move between navigation container positions, *and* the rewind of a
 * cancelled predictive back gesture. Those are not two behaviours — a cancelled gesture is a
 * pop being undone, and the segment it hands this spec is the pop it interrupted, so
 * [navigationMotion] routes it back to the reveal. See the file header for why this spec has
 * to cover both.
 */
@Composable
fun <T : Any> expressiveSceneTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    sceneTransformSpec()

/**
 * The `popTransitionSpec`: a system back press that is not a predictive gesture (a hardware
 * key, or an older platform) runs this to completion on its own clock.
 *
 * Deliberately the same function as [expressiveSceneTransition] — both are
 * [sceneTransformSpec] — so a committed predictive pop continues the animation the drag
 * started rather than switching to a second description of it.
 */
@Composable
fun <T : Any> expressivePopTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    sceneTransformSpec()

/**
 * Predictive pop: used while the user is still dragging the system back gesture, so this
 * spec is driven by gesture progress rather than running on its own clock.
 *
 * Also the same function as the other two, for the same reason.
 *
 * The `Int` is the swipe edge the gesture came from; the animation is the same either way,
 * because a reveal always uncovers towards the edge the user is pulling from.
 */
@Composable
fun <T : Any> expressivePredictivePopTransition(): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform {
    val spec = sceneTransformSpec<T>()
    return { _ -> spec(this) }
}
