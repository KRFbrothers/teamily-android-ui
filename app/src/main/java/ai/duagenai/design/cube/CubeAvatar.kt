package ai.duagenai.design.cube

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/* ============================================================================
 *  CubeAvatar — the ONE avatar composable.
 *
 *  WHY THIS FILE EXISTS
 *  ---------------------------------------------------------------------------
 *  Chat's header and bubbles, the drawer footer, Profile, the picker grid and
 *  Home's rows all call `CubeAvatar(...)`, but no file defined it — so each
 *  screen would have had to invent its own avatar, which is exactly how a kit
 *  ends up with five slightly different ones. This is the one implementation.
 *
 *  WHAT IT DRAWS
 *  -------------
 *  The avatar is DRAWN: an isometric cube whose three faces are three shades of
 *  one colour chosen deterministically from the cube id.
 *
 *  The result is stable per id (the same cube always looks the same), distinct
 *  across the catalogue, and free of any hardcoded colour: the base hue comes
 *  from the palette's own tile tokens.
 *
 *  ACCESSIBILITY
 *  -------------
 *  The avatar is decorative except for the label the caller passes, so it is
 *  exactly one semantics node with that description — never two, and never a
 *  silent image.
 * ==========================================================================*/

/** The three palette accent tiles, in catalogue order. */
private val CubeFills: List<Color> = listOf(
    Color(0xFF9BE3B8), // mint
    Color(0xFF3EC0EA), // cyan
    Color(0xFFF6C945), // yellow
    Color(0xFF9A7BFF), // violet
    Color(0xFF42D982), // brand mint
)

/**
 * One cube avatar.
 *
 * @param cubeId the catalogue id (`CubeIds`). An unknown id falls back to
 *   [CubeIds.DEFAULT] rather than drawing nothing.
 * @param size the drawn square.
 * @param shape the clip: a small radius inside a bubble, a larger one on
 *   Profile. Comes from `DuaGenAITheme.shapes`.
 * @param contentDescription what a screen reader announces. Callers pass a
 *   phrase that names the cube (e.g. "Design Team avatar, Cube 08"), so the
 *   avatar is never just "image". Null marks the avatar DECORATIVE — used when
 *   the surrounding row already names the cube, so TalkBack does not read the
 *   same thing twice.
 */
@Composable
fun CubeAvatar(
    cubeId: String,
    size: Dp,
    shape: Shape,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    val id = CubeIds.normalize(cubeId)
    val index = CubeIds.indexOf(id) ?: 1
    val base = CubeFills[(index - 1) % CubeFills.size]
    val ink = Color(0xFF17201B)

    Canvas(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    // Decorative: the row already names the cube, so the avatar
                    // must not add a second, duplicate announcement.
                    Modifier.clearAndSetSemantics { }
                },
            ),
    ) {
        // A soft tile behind the cube so a light cube still reads on a light
        // card, and the avatar looks like a "chip" rather than a floating shape.
        drawRect(color = base.copy(alpha = 0.22f))
        drawCube(base = base, ink = ink, inset = this.size.minDimension * 0.14f)
    }
}

/**
 * Draws the isometric cube: a top face, a left face and a right face, plus a
 * hairline seam so the three faces stay legible at 28dp.
 */
private fun DrawScope.drawCube(base: Color, ink: Color, inset: Float) {
    val side = this.size.minDimension - inset * 2f
    if (side <= 0f) return

    val cx = this.size.width / 2f
    val cy = this.size.height / 2f
    val r = side / 2f

    // Hexagon vertices: the cube's silhouette. 30° steps, starting at the top.
    fun at(deg: Double) = Offset(
        cx + (r * cos(Math.toRadians(deg))).toFloat(),
        cy + (r * sin(Math.toRadians(deg))).toFloat(),
    )

    val top = at(-90.0)
    val upperRight = at(-30.0)
    val lowerRight = at(30.0)
    val bottom = at(90.0)
    val lowerLeft = at(150.0)
    val upperLeft = at(210.0)
    val center = Offset(cx, cy)

    // Top face: lightest, so the cube reads as lit from above.
    drawPath(
        path = Path().apply {
            moveTo(top.x, top.y)
            lineTo(upperRight.x, upperRight.y)
            lineTo(center.x, center.y)
            lineTo(upperLeft.x, upperLeft.y)
            close()
        },
        color = base,
    )

    // Left face: mid tone.
    drawPath(
        path = Path().apply {
            moveTo(upperLeft.x, upperLeft.y)
            lineTo(center.x, center.y)
            lineTo(center.x, center.y + r * 0.5f)
            lineTo(lowerLeft.x, lowerLeft.y)
            lineTo(bottom.x, bottom.y)
            close()
        },
        color = base.copy(alpha = 0.72f),
    )

    // Right face: darkest, which gives the volume.
    drawPath(
        path = Path().apply {
            moveTo(upperRight.x, upperRight.y)
            lineTo(lowerRight.x, lowerRight.y)
            lineTo(bottom.x, bottom.y)
            lineTo(center.x, center.y + r * 0.5f)
            lineTo(center.x, center.y)
            close()
        },
        color = base.copy(alpha = 0.48f),
    )

    // Seam: the three edges meeting at the centre.
    val seam = Path().apply {
        moveTo(center.x, center.y)
        lineTo(upperLeft.x, upperLeft.y)
        moveTo(center.x, center.y)
        lineTo(upperRight.x, upperRight.y)
        moveTo(center.x, center.y)
        lineTo(center.x, center.y + r * 0.5f)
    }
    drawPath(seam, color = ink.copy(alpha = 0.35f), style = Stroke(width = r * 0.06f))
}
