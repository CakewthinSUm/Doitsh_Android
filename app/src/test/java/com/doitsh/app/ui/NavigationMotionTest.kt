package com.doitsh.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which motion a transition segment gets, decided from the two container positions alone.
 *
 * Since the three top-level pages moved into one `Home` destination, the only segments
 * NavDisplay composes are stack operations: pushing Settings, and leaving it. The rule
 * still has to be decided from the scenes rather than from which spec Nav3 selected,
 * because a cancelled predictive back gesture does not pop — `isPop` is false and the
 * rewind runs under the forward spec, over the same segment the drag was seeking. If the
 * motion depended on the spec, the page would jump the instant the finger lifted.
 */
class NavigationMotionTest {

    @Test
    fun enteringAPushedScreenIsAPush() {
        // Settings publishes no container position. Opening it is a stack operation, so it
        // slides in from the trailing edge.
        assertEquals(NavigationMotion.Push, navigationMotion(from = 0, to = null))
        // Pushed screen on top of a pushed screen: still forward.
        assertEquals(NavigationMotion.Push, navigationMotion(from = null, to = null))
    }

    @Test
    fun leavingAPushedScreenIsAReveal() {
        // This is the segment a Settings back gesture seeks, and it must read the same
        // whether the gesture is still dragging, committed, cancelled, or a plain back
        // press — because all four see exactly these two scenes.
        assertEquals(NavigationMotion.Reveal, navigationMotion(from = null, to = 0))
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
