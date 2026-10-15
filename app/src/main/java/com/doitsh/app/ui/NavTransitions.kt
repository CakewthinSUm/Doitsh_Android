package com.doitsh.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavMetadataKey
// `get` is the operator that reads a NavMetadataKey back out of an entry's metadata map.
// Kotlin needs it imported explicitly; without it, `map[key]` silently falls back to the
// built-in Map.get and the value comes back as `Any?` instead of the key's type.
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.LocalNavAnimatedContentScope

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
 *
 * The pop specs are a separate matter and carry no direction at all: see
 * [expressivePopTransition].
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
 * Forward: the incoming screen pushes in from the side the destination sits on, while the
 * outgoing screen slides a third of the way out and fades. Which side that is comes from
 * [navigationSlideDirection], so a bar tap animates in the direction of the button that
 * was pressed.
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
 * Pop: deliberately **identical** to [expressivePredictivePopTransition].
 *
 * A predictive back gesture does not play the pop transition, it *seeks* it: Navigation 3
 * maps the gesture's progress straight onto the transition's fraction. When the finger
 * lifts it stops seeking and animates that same transition the rest of the way from
 * wherever the gesture left it.
 *
 * That is why these two must be one animation described twice. If they differ, the
 * hand-off swaps specs mid-flight and the page jumps — it reads as a hard cut at exactly
 * the moment the user expects the gesture to finish what it started.
 *
 * It also means the gesture consumes the *whole* range: a full-length drag lands on
 * fraction 1.0, so whatever the transition's end state is, that is what the user is
 * looking at when they let go. There is no distance left for the pop to travel, and the
 * end state cannot be "a card at 0.9" — that is just a card that then vanishes. It has to
 * be a card on its way out, which is why the fade is part of the gesture rather than a
 * separate animation. See [PredictiveBackCard].
 *
 * Note that this carries no horizontal direction, unlike [expressiveSceneTransition]: the
 * page recedes towards its own centre rather than sliding, so there is no "which way" to
 * derive. A pop is a stack operation anyway, not a move between two positions in the
 * navigation container — the screen underneath may not be a bar destination at all.
 */
@Composable
fun <T : Any> expressivePopTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    val exit = predictivePopExit()
    return {
        EnterTransition.None togetherWith exit
    }
}

/**
 * Predictive pop: used while the user is still dragging the system back gesture, so
 * this spec is driven by gesture progress rather than running on its own clock.
 *
 * The page being left shrinks towards its centre as the finger travels, so it reads as a
 * card receding over the page underneath. Two things about it are deliberate:
 *
 * - No `fadeIn` on the revealed screen. It is already sitting behind the card; fading it
 *   up from nothing would show the window background through it for the whole first half
 *   of the drag. It should be opaque from the first frame it is composed.
 * - No `fadeOut` here either, even though the card does fade — just not on this clock.
 *   See [predictivePopExit] for why a fade in this spec cannot survive being seeked.
 *
 * `scaleOut` is a spatial transform, so it takes the spatial spec.
 */
@Composable
fun <T : Any> expressivePredictivePopTransition(): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform {
    val exit = predictivePopExit()
    return { _ ->
        EnterTransition.None togetherWith exit
    }
}

/**
 * How far the page recedes during a predictive back drag. Matches the platform's own
 * back-to-previous-screen animation, so the gesture reads as "what Android does"
 * rather than a house style.
 */
private const val PREDICTIVE_POP_TARGET_SCALE = 0.9f

/**
 * How far the page's corners round off as it recedes. A scaling rectangle with square
 * corners reads as a window being resized; rounding it is what makes the same motion
 * read as a card being lifted off the page underneath.
 */
private val PREDICTIVE_POP_CORNER_RADIUS = 28.dp

/**
 * How dark the page behind the receding card goes, at full gesture travel. The platform
 * applies this dim behind a dismissed activity; in-app there is nothing to apply it for
 * us, and without it the card and the page behind it are the same near-white and the
 * card does not read as lifted at all.
 */
private const val PREDICTIVE_POP_SCRIM_ALPHA = 0.16f

/**
 * The outgoing half of both pop specs, so the gesture and its release cannot drift apart.
 *
 * Note what is *not* here: a fade. The receding page does have to fade out — see
 * [PredictiveBackCard] for why — but it cannot be a `fadeOut` in this spec.
 *
 * A seeked transition is evaluated by *play time*: fraction f means play time
 * `f * totalDuration`, and a `fadeOut(tween(n))` is finished once play time passes `n`.
 * With this spec's 328 ms total, a 60 ms fade is over by fraction 0.18 — so the fade
 * would play out in the first fifth of the drag, long before the finger lifts, and the
 * card would spend the rest of the gesture transparent. Reaching past that means a delay
 * eating most of the duration, which leaves a fade too short to read.
 *
 * The fade therefore lives in [PredictiveBackCard], driven off the same progress value as
 * the scrim so the two cannot disagree. All this spec has to do is the spatial half.
 */
@Composable
private fun predictivePopExit(): ExitTransition {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    return scaleOut(animationSpec = spatial, targetScale = PREDICTIVE_POP_TARGET_SCALE)
}

/**
 * Where in the transition the receding page starts to fade out, as a fraction of the
 * gesture.
 *
 * This is a compromise forced by the gesture seeking the transition. The platform can
 * hold a dismissed activity fully opaque for the whole drag and fade it only once the
 * finger lifts; Nav3 hands the gesture the entire fraction range and reports nothing when
 * the finger lifts, so there is no such moment to hang a fade on. The fade has to be part
 * of the fraction, which means a long enough drag reaches into it.
 *
 * Late is the safer side of that trade. A real back gesture is a flick, released well
 * before full travel, and the transition then animates the rest of the way on its own
 * clock — so the fade plays out normally. Only a drag held all the way to the end spends
 * the fade while the finger is still down. Starting this early would put the fade in the
 * path of ordinary drags instead.
 *
 * The cubic in [cardOpacity] is what makes the late start affordable: the card is still
 * better than 0.9 opaque at 0.8 through the drag, so the visible fade stays in the last
 * stretch rather than thinning the card through the middle of the gesture.
 */
private const val PREDICTIVE_POP_FADE_START = 0.70f

/**
 * Wraps a destination so that, while a predictive back gesture is dismissing it, the
 * page becomes a rounded card and the page behind it dims.
 *
 * The gesture progress is read from the scene's own [EnterExitState], not from a value
 * this composable animates on its own clock. During predictive back the transition is
 * *seeked* by the drag rather than played, so anything driven by the transition follows
 * the finger for free — and rewinds correctly when the gesture is cancelled, which a
 * self-driven animation could not do. Navigation 3 exposes no public predictive-back
 * progress or scrim API, so this is the only handle there is.
 *
 * @param isTopDestination whether this destination is the last one on the back stack.
 * @param isInBackStack whether this destination is still on the back stack at all.
 *
 * Those two together decide whether the page is *receding* — being dismissed, and so
 * owed the card treatment — as opposed to merely being left behind by a forward push,
 * which must stay flat. Both cases put the page in [EnterExitState.PostExit], so state
 * alone cannot tell them apart, and no single signal covers the whole gesture:
 *
 * - While the finger is down, the pop has not committed yet, so the destination is
 *   still the top of the stack and `isTopDestination` carries it.
 * - At the instant of release the pop commits and the destination stops being top, so
 *   `isTopDestination` alone would drop the card a frame early — visibly snapping back to
 *   full size just before the animation that was supposed to carry it away.
 *   `isInBackStack` covers that window: a popped destination leaves the stack, a page
 *   behind a forward push does not.
 *
 * [graphicsLayer] is what keeps this cheap and safe: it transforms and clips at draw
 * time, so the radius can change every frame without re-laying out the page inside.
 */
@Composable
fun PredictiveBackCard(
    isTopDestination: Boolean,
    isInBackStack: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val transition = LocalNavAnimatedContentScope.current.transition
    val progress by transition.animateFloat(
        transitionSpec = { MaterialTheme.motionScheme.defaultSpatialSpec() },
        label = "predictiveBackProgress"
    ) { state ->
        // PostExit is the state a page is in once it is the one being left behind.
        if (state == EnterExitState.PostExit) 1f else 0f
    }

    // Two signals, because neither covers the whole gesture on its own — see the KDoc
    // above. `transition.isRunning` is deliberately not one of them: it stays true for the
    // entire predictive back gesture, seeking and playback alike, so it distinguishes
    // nothing. Worse, it is true of every page that is not the settled one, which would
    // mark the page behind a completed forward push as receding and fade it out from
    // under the screen that pushed it.
    val receding = isTopDestination || !isInBackStack

    // The card and the dim behind it have to leave together. Fading the card alone leaves
    // the scrim sitting at full strength on the frame the scene is dropped, which is a
    // hard cut of exactly the dim the user was watching — so one value drives both.
    val opacity = cardOpacity(progress)

    Box(modifier = modifier.fillMaxSize()) {
        if (receding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // The scene itself is scaled down by the transition, and this scrim
                    // is inside that scene, so it would shrink with it and leave an
                    // undimmed border around the page. Scaling it back up by the inverse
                    // keeps it covering the whole screen at any point in the gesture.
                    .graphicsLayer {
                        scaleX = 1f / PREDICTIVE_POP_TARGET_SCALE
                        scaleY = 1f / PREDICTIVE_POP_TARGET_SCALE
                    }
                    .background(Color.Black.copy(alpha = PREDICTIVE_POP_SCRIM_ALPHA * progress * opacity))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // All of these are written every frame, including at their zero values.
                    // A graphicsLayer block only applies what it sets, so skipping them
                    // would leave the previous frame's rounded shape, clip and alpha on the
                    // layer and the corners would never come back.
                    val radius = PREDICTIVE_POP_CORNER_RADIUS.value * progress
                    shape = RoundedCornerShape(radius.dp)
                    clip = radius > 0f
                    alpha = opacity
                }
        ) {
            content()
        }
    }
}

/**
 * How opaque the receding card is at a given point in the gesture.
 *
 * The card stays fully opaque while the user is still deciding and only fades as the
 * gesture reaches the end, where the finger is about to lift. See
 * [PREDICTIVE_POP_FADE_START] for why the fade has to be part of the gesture at all.
 *
 * The easing is a cubic rather than a straight ramp for a practical reason: with the fade
 * necessarily inside the gesture, a linear one would visibly thin the card through the
 * middle of an ordinary drag. Cubing keeps the curve pinned near 1 for most of the range
 * — at 0.85 through the drag the card is still at 0.88 — and spends the real fade in the
 * last stretch, which is where a releasing gesture is.
 *
 * The cubic reaches exactly 0 at fraction 1.0, and that matters: a full-length drag ends
 * there with nothing left to animate, so that frame is the last one the user sees. Any
 * residual opacity would be a card left hanging on screen with no animation to carry it
 * away, which is the hard cut this is all here to avoid.
 */
private fun cardOpacity(progress: Float): Float {
    val t = ((progress - PREDICTIVE_POP_FADE_START) / (1f - PREDICTIVE_POP_FADE_START))
        .coerceIn(0f, 1f)
    return 1f - t * t * t
}
