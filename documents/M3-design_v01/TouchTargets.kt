package ai.duagenai.design.screens

import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.Modifier
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
