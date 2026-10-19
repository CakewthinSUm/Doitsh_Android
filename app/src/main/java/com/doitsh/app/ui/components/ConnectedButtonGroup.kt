package com.doitsh.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A one-of-N choice drawn as the Expressive **connected** button group: one continuous
 * rounded container, the checked item a solid filled pill, neighbours sharing edges.
 *
 * ## Why this is not `ButtonGroup { toggleableItem(...) }`
 *
 * `ButtonGroup`'s scope API cannot express the connected variant. `toggleableItem`
 * hardcodes its own `ToggleButton` with no `shapes` argument, so every item comes out as a
 * standalone pill, and the group's default `horizontalArrangement` spaces items by
 * `ButtonGroupSmallTokens.BetweenSpace` — 12dp of visible gap, the *disconnected* token.
 * The two together render as separate lozenges rather than one segmented control.
 *
 * The connected geometry lives in `ButtonGroupDefaults` (`ConnectedSpaceBetween` — 2dp —
 * plus `connectedLeadingButtonShapes`, `connectedMiddleButtonShapes` and
 * `connectedTrailingButtonShapes`, which share edges between neighbours), and the only
 * supported way to reach it is what the official `SingleSelectConnectedButtonGroupSample`
 * does: a `Row` spaced by `ConnectedSpaceBetween`, holding `ToggleButton`s that pick their
 * shape by position. This is that sample, with two callers instead of one.
 *
 * ## Contract
 *
 * - [selected] is the option currently checked, or `null` for none.
 * - [onToggle] receives the tapped option and whether it is now checked. A tap on the
 *   checked item reports `checked = false` — whether that clears the choice or does
 *   nothing is the caller's decision, because a priority can be un-set while a theme
 *   cannot.
 * - Labels are passed in already resolved: the items are laid out inside a `Row`, so the
 *   caller resolves its own strings during composition rather than threading resource
 *   ids through here.
 */
@Composable
fun <T> ConnectedButtonGroup(
    options: List<T>,
    selected: T?,
    labelFor: (T) -> String,
    onToggle: (option: T, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { checked -> onToggle(option, checked) },
                modifier = Modifier.weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(labelFor(option))
            }
        }
    }
}
