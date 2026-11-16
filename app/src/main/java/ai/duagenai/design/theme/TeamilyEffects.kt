package ai.duagenai.design.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/* ============================================================================
 *  Teamily 1:1 spec effects (sections 3 and 8): canvas atmosphere, resting
 *  glass shadow, primary green glow and press lift. Every colour comes from
 *  [TeamilyPalette]; screens apply these modifiers instead of raw values.
 * ==========================================================================*/

/**
 * Root screen canvas — `--nv-atmosphere` from the zip:
 *
 *   light  radial-gradient(ellipse at 8% 92%, #e8f3ec 0, transparent 52%), #eef2ef
 *   dark   radial-gradient(ellipse at 8% 92%, #242b27 0, transparent 48%), #141816
 *
 * CSS sizes the ellipse to the farthest corner (here the top-right one), so its
 * radii are 0.92 * width * sqrt(2) and 0.92 * height * sqrt(2). Compose only has
 * circular radial gradients, so the circle is drawn with radius ry and scaled
 * horizontally by rx / ry around the centre to get the same ellipse.
 */
fun Modifier.teamilyCanvas(palette: TeamilyPalette): Modifier = this.drawBehind {
    drawRect(palette.atmosphereBase)
    val deep = palette.atmosphereDeep
    val cx = size.width * 0.08f
    val cy = size.height * 0.92f
    val rx = 0.92f * size.width * SQRT_2
    val ry = 0.92f * size.height * SQRT_2
    if (rx <= 0f || ry <= 0f) return@drawBehind
    val k = rx / ry
    val brush = Brush.radialGradient(
        0f to deep,
        palette.atmosphereStop to deep.copy(alpha = 0f),
        center = Offset(cx, cy),
        radius = ry,
    )
    scale(scaleX = k, scaleY = 1f, pivot = Offset(cx, cy)) {
        // Pre-scaled rect, so after the x-scale it covers exactly the canvas.
        drawRect(
            brush = brush,
            topLeft = Offset(cx - cx / k, 0f),
            size = Size(size.width / k, size.height),
        )
    }
}

private const val SQRT_2 = 1.4142135f

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
 * Press/tap lift: scale 1f -> 0.98f (zip `active:scale-[0.98]`) while [interactionSource] is pressed,
 * on the zip's standard UI spring,
 * (Compose equivalent of the web hover lift). When [reduceMotion] is true the
 * element stays at 1f. This kit has no reduce-motion setting of its own, so
 * callers pass `false` until one exists.
 */
@Composable
fun Modifier.teamilyPressScale(
    interactionSource: InteractionSource,
    reduceMotion: Boolean = false,
    targetScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val target = if (pressed && !reduceMotion) targetScale else 1f
    // `--nv-spring-standard-ui`: stiffness 300, damping 32 (ratio 0.9238).
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = TeamilyMotion.standardSpring(),
        label = "teamilyPressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
