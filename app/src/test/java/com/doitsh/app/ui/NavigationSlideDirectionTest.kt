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
