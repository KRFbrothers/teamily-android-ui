package ai.duagenai.design.cube

import ai.duagenai.design.R
import ai.duagenai.design.screens.MinTouchTarget
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyLightPalette
import ai.duagenai.design.theme.TeamilyMarks
import ai.duagenai.design.theme.TeamilyMotion
import ai.duagenai.design.theme.TeamilyPalette
import ai.duagenai.design.theme.rememberReduceMotion
import ai.duagenai.design.theme.teamilyPalette
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
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
 *  Create Agent — opened ONLY from Agent home's plus menu ("Create Agent").
 *
 *  Big preview (silhouette + blinking eyes, morphs on every shape tap), a name
 *  field, the 15-shape grid and the colour swatches, then Create. Create is
 *  enabled once the trimmed name is non-empty; a shape and a colour are always
 *  preselected. The name and both selections are rememberSaveable.
 * ==========================================================================*/

/** The eye ink for the Create Agent silhouette on a light body. */
private val AvatarInk = TeamilyLightPalette.tileInk

/** Shapes per row in the Create Agent grid (15 shapes = 3 rows). */
private const val CreateAgentShapeColumns = 5

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateAgentScreen(
    onCreate: (name: String, silhouette: AgentSilhouette, swatch: AgentSwatch) -> Unit =
        { _, _, _ -> },
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()

    var name by rememberSaveable { mutableStateOf("") }
    var silhouette by rememberSaveable { mutableStateOf(AgentSilhouette.DEFAULT) }
    var swatch by rememberSaveable { mutableStateOf(AgentSwatch.DEFAULT) }
    val canCreate = name.trim().isNotEmpty()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.create_agent_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Button(
                onClick = { onCreate(name.trim(), silhouette, swatch) },
                enabled = canCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = spacing.gutter, vertical = spacing.md)
                    .sizeIn(minHeight = MinTouchTarget),
                shape = DuaGenAITheme.shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(stringResource(R.string.create_agent_action), style = DuaGenAITextStyles.button)
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter, vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The big preview: interactive with hover/press feedback and a subtle edit badge.
            val avatarInteraction = remember { MutableInteractionSource() }
            val isHovered by avatarInteraction.collectIsHoveredAsState()
            val isPressed by avatarInteraction.collectIsPressedAsState()
            val activeFeedback = isHovered || isPressed
            val avatarScale by animateFloatAsState(
                targetValue = when {
                    isPressed -> 0.96f
                    isHovered -> 1.04f
                    else -> 1f
                },
                animationSpec = TeamilyMotion.standardSpring(),
                label = "createAgentAvatarScale",
            )
            val badgeAlpha by animateFloatAsState(
                targetValue = if (activeFeedback) 1f else 0.88f,
                animationSpec = tween(TeamilyMotion.durationSmall),
                label = "createAgentEditBadgeAlpha",
            )
            val ringAlpha by animateFloatAsState(
                targetValue = if (activeFeedback) 0.35f else 0f,
                animationSpec = tween(TeamilyMotion.durationSmall),
                label = "createAgentRingAlpha",
            )
            val allSilhouettes = remember { AgentSilhouette.values().toList() }
            val cycleSilhouette: () -> Unit = {
                val nextIndex = (allSilhouettes.indexOf(silhouette) + 1) % allSilhouettes.size
                silhouette = allSilhouettes[nextIndex]
            }

            Box(
                modifier = Modifier
                    .size(144.dp)
                    .graphicsLayer {
                        scaleX = avatarScale
                        scaleY = avatarScale
                    }
                    .clip(CircleShape)
                    .hoverable(interactionSource = avatarInteraction)
                    .clickable(
                        interactionSource = avatarInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = cycleSilhouette,
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = ringAlpha),
                        shape = CircleShape,
                    )
                    .testTag("create_agent_avatar_preview"),
                contentAlignment = Alignment.Center,
            ) {
                SilhouetteAvatar(
                    silhouette = silhouette,
                    fill = swatch.color(palette),
                    size = 132.dp,
                    shape = RectangleShape,
                    contentDescription = "Agent preview: ${silhouette.label}, ${swatch.label}",
                    showEyes = true,
                    animateMorph = true,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .graphicsLayer { alpha = badgeAlpha }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = DuaGenAITheme.colors.hairline,
                            shape = CircleShape,
                        )
                        .testTag("create_agent_avatar_edit_badge"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(spacing.lg))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.replace('\n', ' ') },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = DuaGenAITheme.shapes.pill,
                placeholder = { Text(stringResource(R.string.create_agent_name_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
            )

            Spacer(Modifier.height(spacing.lg))

            // The 15 shapes, 5 per row. Thumbnails have no eyes.
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AgentSilhouette.values().toList().chunked(CreateAgentShapeColumns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        row.forEach { s ->
                            AgentShapeCell(
                                silhouette = s,
                                selected = s == silhouette,
                                onClick = { silhouette = s },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.lg))

            // The colour swatches, centred and wrapping on a narrow phone.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                AgentSwatch.values().forEach { entry ->
                    AgentSwatchDot(
                        swatch = entry,
                        color = entry.color(palette),
                        selected = entry == swatch,
                        onClick = { swatch = entry },
                    )
                }
            }
        }
    }
}

/** One shape in the Create Agent grid: a 56dp cell, ringed in primary when selected. */
@Composable
private fun AgentShapeCell(
    silhouette: AgentSilhouette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(DuaGenAITheme.shapes.avatarList)
            .background(
                if (selected) DuaGenAITheme.colors.brandSoft
                else MaterialTheme.colorScheme.surface,
            )
            .then(
                if (selected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = DuaGenAITheme.shapes.avatarList,
                ) else Modifier,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = "Shape: ${silhouette.label}"
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        SilhouetteAvatar(
            silhouette = silhouette,
            fill = MaterialTheme.colorScheme.onBackground,
            size = 40.dp,
            shape = RectangleShape,
            contentDescription = null,
            showEyes = false,
            animateMorph = false,
        )
    }
}

/** One colour swatch: a 32dp dot inside a 48dp touch target. */
@Composable
private fun AgentSwatchDot(
    swatch: AgentSwatch,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(MinTouchTarget)
            .clip(CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = "Color: ${swatch.label}"
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (selected) Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ) else Modifier,
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(color)
                // A hairline so the Ink dot still reads on the dark canvas.
                .border(1.dp, DuaGenAITheme.colors.hairline, CircleShape),
        )
    }
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
    modifier: Modifier = Modifier,
) {
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

    val reduceMotion = rememberReduceMotion()
    val blink = if (showEyes && !reduceMotion) {
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
    val eyeColor = if (color.luminance() > 0.45f) AvatarInk else TeamilyMarks.paper

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

@Preview(name = "Create Agent — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CreateAgentLight() = DuaGenAITheme(darkTheme = false) { CreateAgentScreen() }

@Preview(name = "Create Agent — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CreateAgentDark() = DuaGenAITheme(darkTheme = true) { CreateAgentScreen() }
