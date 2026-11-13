package ai.duagenai.design.screens

import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================================================
 *  The 48dp touch target (audit fix #10) — one shared definition.
 *
 *  The kit draws several controls SMALLER than the M3 48dp minimum because that
 *  is how the mockup draws them (a 44dp header circle, a 42dp send disc). The
 *  visual size is a design decision and stays; the TOUCH AREA must not. M3's
 *  `minimumInteractiveComponentSize()` does exactly that: it keeps the child's
 *  drawn size and expands the interactive box around it to 48dp.
 *
 *  Why a shared helper rather than the raw modifier at each site: the number was
 *  being repeated (and one site forgot it), so it lives in one place now and a
 *  reviewer can grep [minTouchTarget] to see every control that relies on it.
 *
 *  Order matters. `minTouchTarget()` goes BEFORE `.size(…)` in the chain, so the
 *  48dp box is the outer one and the drawn control is centred inside it.
 * ==========================================================================*/

/** The M3 minimum touch target, as one constant instead of a literal per site. */
internal val MinTouchTarget = 48.dp

/**
 * Raises this element's interactive box to the M3 48dp minimum without changing
 * the size it is drawn at.
 *
 * Put it FIRST in the modifier chain, before the visual `.size(…)`:
 * ```
 * Modifier.minTouchTarget().size(36.dp)   // 36dp drawn, 48dp tappable
 * ```
 */
internal fun Modifier.minTouchTarget(): Modifier = minimumInteractiveComponentSize()

/**
 * A 48dp touch box that takes NO extra layout space (review fix, audit rows 7–24).
 *
 * [minTouchTarget] grows the element's layout box to 48dp, which is right for a
 * control in a flexible row but MOVES its neighbours when the drawn control is
 * pinned by offsets (the Dua Agent screens are laid out at the SVG's exact
 * coordinates). This variant keeps the drawn size and position pixel-identical:
 * the element still reports its drawn size to its parent, while the
 * [interaction] modifier (clickable / toggleable / semantics) sits on a box
 * centred on it that is at least 48 × 48. Taps and the accessibility node use
 * that larger box.
 *
 * Put it FIRST, before the visual `.size(…)` / `.clip(…)` / `.background(…)`.
 * Pass `indication = null` to the clickable and add
 * `.indication(interactionSource, LocalIndication.current)` after the visual
 * modifiers, so the pressed ripple stays clipped to the drawn shape:
 * ```
 * val src = remember { MutableInteractionSource() }
 * Modifier
 *     .touchTarget48(Modifier.clickable(src, indication = null, onClick = onClick))
 *     .size(78.dp, 32.dp).clip(shape).indication(src, LocalIndication.current)
 * ```
 */
internal fun Modifier.touchTarget48(interaction: Modifier, shiftUp: Dp = 0.dp): Modifier {
    // The drawn size and its offset inside the touch box, written by the inner
    // layout and read by the outer one in the same measure pass (the outer
    // measures the inner first).
    val drawn = IntArray(4)
    return this
        .layout { measurable, constraints ->
            val p = measurable.measure(constraints)
            val w = drawn[0]
            val h = drawn[1]
            // Put the touch box so the drawn control lands exactly at (0, 0).
            // (p.width - p.measuredWidth) / 2 undoes the auto-centring Compose
            // applies when the 48dp box is wider than the incoming constraints.
            layout(w, h) {
                p.place(
                    -drawn[2] - (p.width - p.measuredWidth) / 2,
                    -drawn[3] - (p.height - p.measuredHeight) / 2,
                )
            }
        }
        .then(interaction)
        .layout { measurable, constraints ->
            val p = measurable.measure(constraints)
            val min = MinTouchTarget.roundToPx()
            val w = maxOf(p.width, min)
            val h = maxOf(p.height, min)
            val ox = (w - p.width) / 2
            // [shiftUp] grows the box more above the control than below it, for
            // a control whose neighbour sits close underneath.
            val oy = ((h - p.height) / 2 + shiftUp.roundToPx()).coerceIn(0, h - p.height)
            drawn[0] = p.width
            drawn[1] = p.height
            drawn[2] = ox
            drawn[3] = oy
            layout(w, h) { p.place(ox, oy) }
        }
}
