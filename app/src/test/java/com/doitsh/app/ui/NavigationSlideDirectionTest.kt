package com.doitsh.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bottom bar's transition direction is derived from the positions of the two
 * destinations rather than hard-coded, so that a bar whose buttons are added, removed,
 * reordered or user-chosen still animates in the direction of the button that was
 * pressed. These tests pin that rule down.
 */
class NavigationSlideDirectionTest {

    @Test
    fun movingToALaterDestinationEntersFromTheTrailingEdge() {
        // Pressing the button to the right of the current one: content travels left, so
        // the new screen comes in from the right.
        assertEquals(SlideDirection.Left, navigationSlideDirection(from = 0, to = 1))
        assertEquals(SlideDirection.Left, navigationSlideDirection(from = 1, to = 4))
    }

    @Test
    fun movingToAnEarlierDestinationEntersFromTheLeadingEdge() {
        // Pressing the button to the left: the mirror of the case above.
        assertEquals(SlideDirection.Right, navigationSlideDirection(from = 1, to = 0))
        assertEquals(SlideDirection.Right, navigationSlideDirection(from = 4, to = 1))
    }

    @Test
    fun aPushWithoutAContainerPositionKeepsTheForwardDirection() {
        // Settings is pushed onto the stack rather than selected from the bar, so it
        // publishes no position. A push should still read as moving forward.
        assertEquals(SlideDirection.Left, navigationSlideDirection(from = 0, to = null))
        assertEquals(SlideDirection.Left, navigationSlideDirection(from = null, to = null))
        assertEquals(SlideDirection.Left, navigationSlideDirection(from = null, to = 3))
    }

    @Test
    fun theRuleIsOrderingNotIdentity() {
        // The direction depends only on the relative order of the two positions, which is
        // what makes it generalise to any set of bar buttons rather than the current two.
        assertEquals(SlideDirection.Left, navigationSlideDirection(from = 2, to = 3))
        assertEquals(SlideDirection.Right, navigationSlideDirection(from = 3, to = 2))
    }
}

/**
 * Which motion a transition segment gets, decided from the two container positions alone.
 *
 * This is the rule that makes a cancelled predictive back gesture continue the pop it
 * started. Nav3 runs the rewind under its *forward* spec — a cancelled gesture does not
 * pop, so `isPop` is false — so if the motion depended on which spec was selected, the
 * page would jump the instant the finger lifted. It must depend only on the segment.
 */
class NavigationMotionTest {

    @Test
    fun leavingAPushedScreenIsAReveal() {
        // Settings publishes no position; the bar destination underneath it does. This is
        // the segment a Settings back gesture seeks, and it reads the same whether the
        // gesture is dragging, committed or cancelled.
        assertEquals(NavigationMotion.Reveal, navigationMotion(from = null, to = 0))
        assertEquals(NavigationMotion.Reveal, navigationMotion(from = null, to = 1))
    }

    @Test
    fun pushingOntoAPushedScreenIsNotAReveal() {
        // Leaving a bar destination for a pushed screen is a push, not a reveal — the
        // reveal direction is fixed and would send the incoming screen in from the wrong
        // edge here.
        assertEquals(NavigationMotion.Push(SlideDirection.Left), navigationMotion(from = 0, to = null))
        assertEquals(NavigationMotion.Push(SlideDirection.Left), navigationMotion(from = 1, to = null))
    }

    @Test
    fun movingBetweenContainerPositionsIsAPushInThatDirection() {
        assertEquals(NavigationMotion.Push(SlideDirection.Left), navigationMotion(from = 0, to = 1))
        assertEquals(NavigationMotion.Push(SlideDirection.Right), navigationMotion(from = 1, to = 0))
    }

    @Test
    fun twoPushedScreensStillGetAForwardDirection() {
        // Neither publishes a position, so there is no container order to read; the
        // forward convention is what a push should look like.
        assertEquals(NavigationMotion.Push(SlideDirection.Left), navigationMotion(from = null, to = null))
    }
}
