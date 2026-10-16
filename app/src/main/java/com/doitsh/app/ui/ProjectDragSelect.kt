package com.doitsh.app.ui

/**
 * The geometry behind the project picker's press-and-hold drag selection.
 *
 * The interaction it serves is the one Google Translate's language button uses: press and
 * hold, and a list of choices appears with the *current* one already under your finger;
 * slide without lifting to move the highlight, and let go to commit. Being able to drag
 * away from where you pressed is the whole point, which is why this is not just a tap and
 * why the list has to be placed thoughtfully rather than just dropped near the field.
 *
 * ## Why the arithmetic lives here
 *
 * Mapping a finger's y to a row index is the part that can be quietly wrong — off by one
 * at a boundary, or unable to reach the first row, or drifting when the list is clamped
 * against a screen edge — and it is also the part a test can actually exercise. The
 * gesture that feeds these functions cannot be driven from a unit test at all, so the
 * logic is separated from it deliberately: this file is verified, the gesture is reasoned
 * about and checked by hand. The same split as [dueDateForPreset] and the sheet that
 * calls it.
 */

/**
 * Where the drag list's top edge should sit, in window pixels, so that [selectedIndex]
 * lands under a finger resting at [anchorFingerY].
 *
 * Seating the *current* selection under the finger is what makes the gesture feel
 * continuous rather than like a menu that happened to open: at the instant the list
 * appears, nothing has moved, and sliding from there is a direct manipulation of the
 * highlight. Placing the list by its centre instead would open it with some unrelated row
 * under the finger, so the first slide would jump.
 *
 * Clamped into the window because a list at the top or bottom would otherwise be drawn
 * partly off-screen — and the row the user is aiming with is the last thing that should
 * be allowed to leave the screen. When the clamp engages, the row under the finger is no
 * longer the selected one, which is precisely what [dragTargetIndex] then reports: the
 * highlight follows the finger rather than the list.
 */
internal fun dragListTop(
    anchorFingerY: Float,
    selectedIndex: Int,
    itemCount: Int,
    itemHeight: Float,
    windowHeight: Float,
): Float {
    if (itemCount <= 0 || itemHeight <= 0f) return anchorFingerY
    val listHeight = itemCount * itemHeight
    val unclamped = anchorFingerY - (selectedIndex + 0.5f) * itemHeight
    val maxTop = (windowHeight - listHeight).coerceAtLeast(0f)
    return unclamped.coerceIn(0f, maxTop)
}

/**
 * Which row a finger at [fingerY] is over, given the list's top edge.
 *
 * Overshooting either end resolves to the end row rather than to nothing, so a finger
 * that slides past the list still commits to a real choice. That matters more than it
 * sounds: the highlight is what the user aims with, so it must never be able to leave the
 * list and leave them with no idea what releasing will do.
 */
internal fun dragTargetIndex(
    fingerY: Float,
    listTop: Float,
    itemHeight: Float,
    itemCount: Int,
): Int {
    if (itemCount <= 0 || itemHeight <= 0f) return 0
    val raw = ((fingerY - listTop) / itemHeight).toInt()
    return raw.coerceIn(0, itemCount - 1)
}
