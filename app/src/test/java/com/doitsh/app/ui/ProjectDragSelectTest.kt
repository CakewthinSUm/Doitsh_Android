package com.doitsh.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The press-and-hold drag picker's two pieces of arithmetic.
 *
 * The gesture itself cannot be driven from a unit test — synthetic touch events cannot
 * hold a finger down (see the memory on predictive back) — so the split is deliberate:
 * this is the part that can be pinned, and the gesture that calls it is checked by hand
 * on the device. These tests are therefore the only automated guard on the interaction's
 * correctness, which is why they cover the boundaries rather than just the happy path.
 */
class ProjectDragSelectTest {

    private val itemHeight = 100f
    private val windowHeight = 2000f

    // --- dragListTop ------------------------------------------------------------------

    @Test
    fun theSelectedRowIsSeatedUnderTheFinger() {
        // Four rows, 100px each, the third one selected (index 2). To put its centre
        // under a finger at y=500 the list's top must be 500 - 2.5*100 = 250.
        val top = dragListTop(
            anchorFingerY = 500f,
            selectedIndex = 2,
            itemCount = 4,
            itemHeight = itemHeight,
            windowHeight = windowHeight,
        )
        assertEquals(250f, top, 0.001f)
        // And the round trip: the row under that finger is the one that was selected.
        assertEquals(2, dragTargetIndex(500f, top, itemHeight, 4))
    }

    @Test
    fun theFirstRowIsSeatedUnderTheFingerToo() {
        // index 0 needs top = 500 - 0.5*100 = 450, not 500: seating the row's *top* at
        // the finger would make every touch start half a row too low.
        val top = dragListTop(500f, 0, 4, itemHeight, windowHeight)
        assertEquals(450f, top, 0.001f)
        assertEquals(0, dragTargetIndex(500f, top, itemHeight, 4))
    }

    @Test
    fun aListNearTheTopIsClampedNotDrawnOffScreen() {
        // Pressing near the top of the screen with a late row selected would want a
        // negative top. It must clamp to 0 instead of drawing rows off-screen.
        val top = dragListTop(
            anchorFingerY = 40f,
            selectedIndex = 3,
            itemCount = 4,
            itemHeight = itemHeight,
            windowHeight = windowHeight,
        )
        assertEquals(0f, top, 0.001f)
        // The clamp means the finger is no longer over the selected row, and the target
        // must follow the finger rather than the list — y=40 is in row 0.
        assertEquals(0, dragTargetIndex(40f, top, itemHeight, 4))
    }

    @Test
    fun aListNearTheBottomIsClampedToStayOnScreen() {
        // Four rows need 400px; in a 2000px window the lowest legal top is 1600.
        val top = dragListTop(
            anchorFingerY = 1990f,
            selectedIndex = 0,
            itemCount = 4,
            itemHeight = itemHeight,
            windowHeight = windowHeight,
        )
        assertEquals(1600f, top, 0.001f)
        // The list ends exactly at the window's bottom edge, so its last row is fully
        // reachable rather than half cut off.
        assertEquals(windowHeight, top + 4 * itemHeight, 0.001f)
    }

    @Test
    fun aListTallerThanTheWindowPinsToTheTop() {
        // A window shorter than the list: maxTop would go negative, so it must pin to 0
        // rather than produce a negative top that draws the first rows off-screen.
        val top = dragListTop(
            anchorFingerY = 100f,
            selectedIndex = 5,
            itemCount = 20,
            itemHeight = itemHeight,
            windowHeight = 500f,
        )
        assertEquals(0f, top, 0.001f)
    }

    @Test
    fun aSingleItemListCentresThatItem() {
        val top = dragListTop(300f, 0, 1, itemHeight, windowHeight)
        assertEquals(250f, top, 0.001f)
        assertEquals(0, dragTargetIndex(300f, top, itemHeight, 1))
    }

    @Test
    fun degenerateInputsDoNotDivideByZero() {
        // Not reachable through the UI — the picker always has at least the Inbox — but a
        // division by a zero row height would be an arithmetic exception rather than a
        // wrong pixel, so the guards are worth their two lines.
        assertEquals(120f, dragListTop(120f, 0, 0, itemHeight, windowHeight), 0.001f)
        assertEquals(120f, dragListTop(120f, 0, 4, 0f, windowHeight), 0.001f)
        assertEquals(0, dragTargetIndex(120f, 0f, 0f, 4))
        assertEquals(0, dragTargetIndex(120f, 0f, itemHeight, 0))
    }

    // --- dragTargetIndex --------------------------------------------------------------

    @Test
    fun eachRowIsHitAcrossItsWholeHeight() {
        val top = 0f
        // Row boundaries land exactly on multiples of the row height, and the row below
        // the boundary belongs to the next index — the classic off-by-one.
        assertEquals(0, dragTargetIndex(0f, top, itemHeight, 4))
        assertEquals(0, dragTargetIndex(99f, top, itemHeight, 4))
        assertEquals(1, dragTargetIndex(100f, top, itemHeight, 4))
        assertEquals(1, dragTargetIndex(199f, top, itemHeight, 4))
        assertEquals(2, dragTargetIndex(200f, top, itemHeight, 4))
    }

    @Test
    fun overshootingEitherEndSticksToTheEndRow() {
        // Sliding past the list must still leave a real row highlighted, because the
        // highlight is what the user aims with and releasing commits it.
        assertEquals(0, dragTargetIndex(-500f, 0f, itemHeight, 4))
        assertEquals(3, dragTargetIndex(9999f, 0f, itemHeight, 4))
    }

    @Test
    fun theTargetIsNeverOutsideTheList() {
        // Swept across a range far wider than the list, in both directions, to assert the
        // property rather than spot-check it: whatever the finger does, the result is a
        // row that exists.
        val top = 250f
        var y = -400f
        while (y <= 1200f) {
            val index = dragTargetIndex(y, top, itemHeight, 4)
            assertTrue("index $index out of range at y=$y", index in 0..3)
            y += 7f
        }
    }

    @Test
    fun aClampedListStillMapsEveryVisibleRow() {
        // The combination that actually occurs on screen: a bottom-clamped list, dragged
        // across. Every row of the list must be reachable from somewhere in the list.
        val top = dragListTop(1990f, 0, 4, itemHeight, windowHeight)
        val reached = (0..3).map { row ->
            // Sample the middle of the row and assert it resolves back to that row.
            dragTargetIndex(top + (row + 0.5f) * itemHeight, top, itemHeight, 4)
        }
        assertEquals(listOf(0, 1, 2, 3), reached)
    }
}
