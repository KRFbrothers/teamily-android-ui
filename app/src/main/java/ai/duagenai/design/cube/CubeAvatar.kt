package ai.duagenai.design.cube

import ai.duagenai.design.theme.TeamilyMotion
import ai.duagenai.design.theme.TeamilyPalette
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import kotlin.math.PI
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

/** The seam / eye ink, shared by the cube and the Create Agent silhouette. */
private val AvatarInk = Color(0xFF17201B)

/**
 * One cube avatar.
 *
 * @param cubeId the catalogue id (`CubeIds`). An unknown id falls back to
 *   [CubeIds.DEFAULT] rather than drawing nothing.
 * @param size the drawn square.
 * @param shape the clip: a small radius inside a bubble, a larger one on
 *   Profile. Comes from `DuaGenAITheme.shapes`.
 * @param silhouette Create Agent only: draw this agent shape instead of the
 *   cube. Null (the default) keeps the cube, so existing callers are unchanged.
 * @param fill the silhouette's colour (a swatch from [AgentSwatch]).
 * @param showEyes draws two eyes with a periodic blink. The big preview only.
 * @param animateMorph when the silhouette changes, morph from the old shape to
 *   the new one (graphics-shapes [Morph]) and fade the colour, instead of
 *   snapping.
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
    silhouette: AgentSilhouette? = null,
    fill: Color? = null,
    showEyes: Boolean = false,
    animateMorph: Boolean = false,
) {
    // Create Agent mode. Every existing caller passes no silhouette, so it
    // falls through to the cube below exactly as before.
    if (silhouette != null) {
        SilhouetteAvatar(
            silhouette = silhouette,
            fill = fill ?: CubeFills.first(),
            size = size,
            shape = shape,
            contentDescription = contentDescription,
            showEyes = showEyes,
            animateMorph = animateMorph,
            modifier = modifier,
        )
        return
    }
    val id = CubeIds.normalize(cubeId)
    val index = CubeIds.indexOf(id) ?: 1
    val base = CubeFills[(index - 1) % CubeFills.size]
    val ink = AvatarInk

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

/* ============================================================================
 *  Create Agent silhouettes
 *
 *  Fifteen rounded shapes, built once with graphics-shapes so any two can be
 *  morphed into each other. Every shape is laid out on a unit radius around
 *  (0, 0); drawing re-centres it from its bounds, so a wide pill and a round
 *  circle both sit in the middle of the avatar.
 * ==========================================================================*/

/** The 15 shapes the Create Agent grid offers, in grid order. */
enum class AgentSilhouette(val label: String) {
    Circle("Circle"),
    Drop("Drop"),
    Tile("Tile"),
    Sparkle("Sparkle"),
    Cloud("Cloud"),
    Pebble("Pebble"),
    Scallop("Scallop"),
    Clover("Clover"),
    Bean("Bean"),
    Triangle("Triangle"),
    Hexagon("Hexagon"),
    Star("Star"),
    Flower("Flower"),
    Diamond("Diamond"),
    Sunburst("Sunburst");

    /** The graphics-shapes polygon. Built lazily, once per process. */
    internal val polygon: RoundedPolygon by lazy { buildSilhouette(this) }

    companion object {
        /** The shape Create Agent preselects. */
        val DEFAULT: AgentSilhouette = Circle

        /** Parses a stored key, falling back to [DEFAULT]. */
        fun fromKey(key: String?): AgentSilhouette =
            values().firstOrNull { it.name == key } ?: DEFAULT
    }
}

/**
 * The Create Agent colour swatches: the five frozen tile tokens plus the four
 * named Create Agent tokens. Colours are resolved from [TeamilyPalette], so no
 * screen ever holds a raw hex.
 */
enum class AgentSwatch(val label: String) {
    Mint("Mint"),
    Cyan("Cyan"),
    Yellow("Yellow"),
    Violet("Violet"),
    Ink("Ink"),
    Brown("Brown"),
    Red("Red"),
    Blue("Blue"),
    Pink("Pink");

    /** This swatch's colour in [palette]. */
    fun color(palette: TeamilyPalette): Color = when (this) {
        Mint -> palette.tileMint
        Cyan -> palette.tileCyan
        Yellow -> palette.tileYellow
        Violet -> palette.tileViolet
        Ink -> palette.tileInk
        Brown -> palette.tileBrown
        Red -> palette.tileRed
        Blue -> palette.tileBlue
        Pink -> palette.tilePink
    }

    companion object {
        /** The colour Create Agent preselects. Mint reads on both canvases. */
        val DEFAULT: AgentSwatch = Mint

        /** Parses a stored key, falling back to [DEFAULT]. */
        fun fromKey(key: String?): AgentSwatch =
            values().firstOrNull { it.name == key } ?: DEFAULT
    }
}

/** Vertices on a unit circle, starting at [startDeg], radius per vertex. */
private fun ring(count: Int, startDeg: Float, radius: (Int) -> Float): FloatArray {
    val out = FloatArray(count * 2)
    for (i in 0 until count) {
        val a = (startDeg + 360f * i / count) * (PI / 180.0)
        out[i * 2] = (radius(i) * cos(a)).toFloat()
        out[i * 2 + 1] = (radius(i) * sin(a)).toFloat()
    }
    return out
}

/** A regular polygon with soft corners, the first vertex at [startDeg]. */
private fun softPolygon(sides: Int, rounding: Float, startDeg: Float = -90f): RoundedPolygon =
    RoundedPolygon(
        vertices = ring(sides, startDeg) { 1f },
        rounding = CornerRounding(rounding, 0.6f),
    )

/** A star: [points] outer tips alternating with inner valleys at [inner]. */
private fun softStar(
    points: Int,
    inner: Float,
    tipRounding: Float,
    valleyRounding: Float,
    startDeg: Float = -90f,
): RoundedPolygon {
    val tip = CornerRounding(tipRounding, 0.6f)
    val valley = CornerRounding(valleyRounding, 0.6f)
    return RoundedPolygon(
        vertices = ring(points * 2, startDeg) { if (it % 2 == 0) 1f else inner },
        perVertexRounding = List(points * 2) { if (it % 2 == 0) tip else valley },
    )
}

/** A rectangle [w] × [h] centred on the origin, with corner [rounding]. */
private fun softRect(w: Float, h: Float, rounding: Float): RoundedPolygon =
    RoundedPolygon(
        vertices = floatArrayOf(w / 2, -h / 2, w / 2, h / 2, -w / 2, h / 2, -w / 2, -h / 2),
        rounding = CornerRounding(rounding, 0.6f),
    )

private fun buildSilhouette(s: AgentSilhouette): RoundedPolygon = when (s) {
    AgentSilhouette.Circle -> RoundedPolygon.circle(numVertices = 8)
    // A teardrop: a gently rounded tip on top, two very round bottom corners.
    AgentSilhouette.Drop -> RoundedPolygon(
        vertices = ring(3, -90f) { 1f },
        perVertexRounding = listOf(
            CornerRounding(0.2f, 0.6f),
            CornerRounding(0.85f, 0.6f),
            CornerRounding(0.85f, 0.6f),
        ),
    )
    AgentSilhouette.Tile -> softPolygon(sides = 4, rounding = 0.3f, startDeg = -35f)
    AgentSilhouette.Sparkle -> softStar(points = 4, inner = 0.5f, tipRounding = 0.25f, valleyRounding = 0.2f)
    AgentSilhouette.Cloud -> softStar(points = 3, inner = 0.68f, tipRounding = 0.6f, valleyRounding = 0.15f)
    AgentSilhouette.Pebble -> softPolygon(sides = 5, rounding = 0.5f)
    AgentSilhouette.Scallop -> softStar(points = 10, inner = 0.84f, tipRounding = 0.16f, valleyRounding = 0.06f)
    AgentSilhouette.Clover -> softStar(points = 4, inner = 0.52f, tipRounding = 0.5f, valleyRounding = 0.12f, startDeg = -45f)
    AgentSilhouette.Bean -> softRect(w = 2f, h = 1.4f, rounding = 0.65f)
    AgentSilhouette.Triangle -> softPolygon(sides = 3, rounding = 0.3f, startDeg = 0f)
    AgentSilhouette.Hexagon -> softPolygon(sides = 6, rounding = 0.25f)
    AgentSilhouette.Star -> softStar(points = 5, inner = 0.55f, tipRounding = 0.15f, valleyRounding = 0.08f)
    AgentSilhouette.Flower -> softStar(points = 6, inner = 0.72f, tipRounding = 0.35f, valleyRounding = 0.1f)
    AgentSilhouette.Diamond -> softPolygon(sides = 4, rounding = 0.2f)
    AgentSilhouette.Sunburst -> softStar(points = 12, inner = 0.8f, tipRounding = 0.08f, valleyRounding = 0.04f)
}

/** Builds a Compose [Path] from graphics-shapes cubics, scaled and offset. */
private fun List<Cubic>.toPath(scale: Float, dx: Float, dy: Float): Path {
    val path = Path()
    if (isEmpty()) return path
    val first = first()
    path.moveTo(first.anchor0X * scale + dx, first.anchor0Y * scale + dy)
    for (c in this) {
        path.cubicTo(
            c.control0X * scale + dx, c.control0Y * scale + dy,
            c.control1X * scale + dx, c.control1Y * scale + dy,
            c.anchor1X * scale + dx, c.anchor1Y * scale + dy,
        )
    }
    path.close()
    return path
}

/** Blink: eyes open most of the cycle, closed for a brief beat. */
private const val BlinkCycleMs = 3600

/**
 * The Create Agent avatar: a silhouette in [fill], optionally with blinking
 * eyes, morphing between shapes when [animateMorph] is on.
 */
@Composable
private fun SilhouetteAvatar(
    silhouette: AgentSilhouette,
    fill: Color,
    size: Dp,
    shape: Shape,
    contentDescription: String?,
    showEyes: Boolean,
    animateMorph: Boolean,
    modifier: Modifier,
) {
    // The morph runs from the last shape to the new one. Without
    // animateMorph both ends are the current shape, so it simply draws it.
    var from by remember { mutableStateOf(silhouette) }
    var to by remember { mutableStateOf(silhouette) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(silhouette, animateMorph) {
        if (silhouette == to) return@LaunchedEffect
        if (animateMorph) {
            from = to
            to = silhouette
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                tween(TeamilyMotion.durationLarge, easing = TeamilyMotion.easeChange),
            )
        } else {
            from = silhouette
            to = silhouette
            progress.snapTo(1f)
        }
    }
    val drawnFrom = if (animateMorph) from else silhouette
    val drawnTo = if (animateMorph) to else silhouette
    val morph = remember(drawnFrom, drawnTo) { Morph(drawnFrom.polygon, drawnTo.polygon) }

    val color = if (animateMorph) {
        animateColorAsState(
            fill,
            tween(TeamilyMotion.durationLarge, easing = TeamilyMotion.easeChange),
            label = "agentFill",
        ).value
    } else {
        fill
    }

    // Eyes open = 1, shut = ~0.1. An infinite transition, so a compose test
    // never waits on it.
    val blink = if (showEyes) {
        rememberInfiniteTransition(label = "agentBlink").animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                keyframes {
                    durationMillis = BlinkCycleMs
                    1f at BlinkCycleMs - 300
                    0.1f at BlinkCycleMs - 180
                    1f at BlinkCycleMs - 60
                },
            ),
            label = "agentBlinkScale",
        ).value
    } else {
        1f
    }
    // Ink eyes on a light body, white eyes on a dark one.
    val eyeColor = if (color.luminance() > 0.45f) AvatarInk else Color.White

    Canvas(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier.clearAndSetSemantics { }
                },
            ),
    ) {
        val cubics = morph.asCubics(progress.value)
        val bounds = morph.calculateBounds()
        val w = bounds[2] - bounds[0]
        val h = bounds[3] - bounds[1]
        if (w <= 0f || h <= 0f) return@Canvas
        val box = this.size.minDimension * 0.84f
        val scale = box / maxOf(w, h)
        val dx = this.size.width / 2f - (bounds[0] + w / 2f) * scale
        val dy = this.size.height / 2f - (bounds[1] + h / 2f) * scale
        drawPath(cubics.toPath(scale, dx, dy), color = color)

        if (showEyes) drawEyes(eyeColor, blink)
    }
}

/** Two upright capsule eyes around the centre, squashed by [blink]. */
private fun DrawScope.drawEyes(color: Color, blink: Float) {
    val d = this.size.minDimension
    val eyeW = d * 0.085f
    val eyeH = d * 0.2f * blink
    val gap = d * 0.1f
    val cy = this.size.height / 2f
    val cx = this.size.width / 2f
    for (x in listOf(cx - gap - eyeW / 2f, cx + gap - eyeW / 2f)) {
        drawRoundRect(
            color = color,
            topLeft = Offset(x, cy - eyeH / 2f),
            size = Size(eyeW, eyeH),
            cornerRadius = CornerRadius(eyeW / 2f),
        )
    }
}
