package com.doitsh.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which motion a transition segment gets, decided from the two container positions alone.
 *
 * The rule matters for two reasons.
 *
 * First, the bar's switch direction follows the button that was pressed rather than a fixed
 * convention: moving to a later button is leftward travel (the page enters from the trailing
 * edge), moving back is the mirror. Deriving it from the published positions is what lets
 * the bar be reordered or customised without touching the transitions.
 *
 * Second, the motion must be a function of the *segment*, never of which spec Nav3 selected.
 * A cancelled predictive back gesture does not pop, so `isPop` is false and Nav3 runs the
 * rewind under its forward spec, over the same segment the drag was seeking — if the motion
 * depended on the spec, the page would jump the instant the finger lifted.
 */
class NavigationMotionTest {

    @Test
    fun movingToALaterBarDestinationSlidesLeft() {
        assertEquals(NavigationMotion.Switch(SlideDirection.Left), navigationMotion(from = 0, to = 1))
        assertEquals(NavigationMotion.Switch(SlideDirection.Left), navigationMotion(from = 1, to = 2))
    }

    @Test
    fun movingToAnEarlierBarDestinationSlidesRight() {
        assertEquals(NavigationMotion.Switch(SlideDirection.Right), navigationMotion(from = 1, to = 0))
        assertEquals(NavigationMotion.Switch(SlideDirection.Right), navigationMotion(from = 2, to = 0))
    }

    @Test
    fun theSwitchDirectionIsOrderingNotIdentity() {
        // Only the relative order of the two positions matters, which is what generalises
        // from the current bar to any set of bar buttons.
        assertEquals(SlideDirection.Left, navigationSlideDirection(from = 2, to = 3))
        assertEquals(SlideDirection.Right, navigationSlideDirection(from = 3, to = 2))
    }

    @Test
    fun enteringAPushedScreenIsAPush() {
        // Settings publishes no container position. Opening it is a stack operation, so it
        // slides in from the trailing edge.
        assertEquals(NavigationMotion.Push, navigationMotion(from = 0, to = null))
        assertEquals(NavigationMotion.Push, navigationMotion(from = 1, to = null))
        // Pushed screen on top of a pushed screen: still forward.
        assertEquals(NavigationMotion.Push, navigationMotion(from = null, to = null))
    }

    @Test
    fun leavingAPushedScreenIsAReveal() {
        // This is the segment a Settings back gesture seeks, and it must read the same
        // whether the gesture is still dragging, committed, cancelled, or a plain back
        // press — because all four see exactly these two scenes.
        assertEquals(NavigationMotion.Reveal, navigationMotion(from = null, to = 0))
        assertEquals(NavigationMotion.Reveal, navigationMotion(from = null, to = 1))
        assertEquals(NavigationMotion.Reveal, navigationMotion(from = null, to = 2))
    }

    @Test
    fun aRevealIsNotTheMirrorOfAPush() {
        // Regression guard for the cancelled-gesture jump: push and reveal are different
        // motions, not one motion with the direction flipped. If these two ever collapse
        // into the same value the parallax swaps sides and the reveal no longer finishes
        // the motion the drag started.
        val pushed = navigationMotion(from = 0, to = null)
        val revealed = navigationMotion(from = null, to = 0)
        assert(pushed != revealed) {
            "push and reveal must stay distinct; both were $pushed"
        }
    }
}
