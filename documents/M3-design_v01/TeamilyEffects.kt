package ai.duagenai.design.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/* ============================================================================
 *  Teamily 1:1 spec effects (sections 3 and 8): canvas atmosphere, resting
 *  glass shadow, primary green glow and press lift. Every colour comes from
 *  [TeamilyPalette]; screens apply these modifiers instead of raw values.
 * ==========================================================================*/

/**
 * Root screen canvas.
 *
 * Light: `--nv-atmosphere-deep` — `radial-gradient(ellipse at 8% 92%,
 * #e8f3ec 0%, transparent 55%)` over `bgCanvas` (#FFFFFF), radius
 * `maxDimension * 0.65`. Dark: the palette's `bgCanvasGradient`
 * (#1E2522 -> #1D2220 -> #171C19).
 */
fun Modifier.teamilyCanvas(palette: TeamilyPalette): Modifier {
    val deep = palette.atmosphereDeep ?: return this.background(palette.bgCanvasGradient)
    return this.drawBehind {
        drawRect(palette.bgCanvas)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(deep, deep.copy(alpha = 0f)),
                center = Offset(size.width * 0.08f, size.height * 0.92f),
                radius = size.maxDimension * 0.65f,
            ),
        )
    }
}

/** `--nv-shadow-resting` under a glass card. Light 4dp, dark 6dp. */
fun Modifier.teamilyRestingShadow(palette: TeamilyPalette, shape: Shape): Modifier =
    this.shadow(
        elevation = palette.restingShadowElevation,
        shape = shape,
        clip = false,
        ambientColor = palette.restingShadowAmbient,
        spotColor = palette.restingShadowSpot,
    )

/**
 * `--nv-primary-shadow-hover` green glow on the Send button and primary green
 * CTAs. Light mode only — the dark palette carries no glow, so this is a no-op.
 */
fun Modifier.teamilyPrimaryGlow(palette: TeamilyPalette): Modifier {
    val ambient = palette.primaryGlowAmbient ?: return this
    val spot = palette.primaryGlowSpot ?: return this
    return this.shadow(
        elevation = 10.dp,
        shape = CircleShape,
        clip = false,
        ambientColor = ambient,
        spotColor = spot,
    )
}

/**
 * Press/tap lift: scale 1f -> 0.98f while [interactionSource] is pressed
 * (Compose equivalent of the web hover lift). When [reduceMotion] is true the
 * element stays at 1f. This kit has no reduce-motion setting of its own, so
 * callers pass `false` until one exists.
 */
@Composable
fun Modifier.teamilyPressScale(
    interactionSource: InteractionSource,
    reduceMotion: Boolean = false,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val target = if (pressed && !reduceMotion) 0.98f else 1f
    val scale by animateFloatAsState(targetValue = target, label = "teamilyPressScale")
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
