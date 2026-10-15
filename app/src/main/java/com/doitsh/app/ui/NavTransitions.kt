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
 */

/**
 * Forward: the incoming screen pushes in from the trailing edge while the outgoing
 * screen slides a third of the way out and fades. The outgoing screen deliberately
 * does not travel the full width — it parallaxes behind the incoming one, which is
 * what gives the push a sense of depth rather than a flat wipe.
 */
@Composable
fun <T : Any> expressiveForwardTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    return {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = spatial
        ) + fadeIn(animationSpec = effects) togetherWith
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = spatial,
                // A third of the full slide: the outgoing screen keeps its place in
                // the hierarchy instead of exiting the frame entirely.
                targetOffset = { it / 3 }
            ) + fadeOut(animationSpec = effects)
    }
}

/**
 * Pop: the mirror of [expressiveForwardTransition] — the screen being revealed comes
 * back in from the leading edge and the screen being dismissed slides away to the
 * trailing edge.
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
