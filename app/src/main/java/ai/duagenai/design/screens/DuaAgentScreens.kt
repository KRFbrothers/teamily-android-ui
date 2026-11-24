package ai.duagenai.design.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import ai.duagenai.design.bot.BabyGrokMark
import ai.duagenai.design.bot.LocalBotAppearance
import kotlinx.coroutines.launch
import ai.duagenai.design.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.TeamilyLightPalette
import ai.duagenai.design.theme.TeamilyMarks
import ai.duagenai.design.theme.TeamilyPalette
import ai.duagenai.design.theme.teamilyPalette

/* ============================================================================
 *  Dua Agent — profile & workspace (approved SVG set dua-agent-01 … 13)
 *
 *  Thirteen frames, nine destinations:
 *      01 / 02 / 04 / 05  DuaAgentProfileScreen    one scrolling list; Skills and
 *                                                   Connectors expand in place
 *      03                 DuaAgentProfileScreen    ⋯ opens the more-sheet
 *                                                   (Share · Clear Chat History)
 *      06                 DuaAgentConnectAppScreen
 *      07                 DuaAgentWorkspaceScreen  Manage: Overview · Live Sites ·
 *                                                   Mail · Artifacts · Agent API
 *      08 … 12            Overview / Artifacts / Agent API / Live Sites / Mail
 *      13                 DuaAgentNewGroupScreen
 *
 *  Every size, offset and colour is the approved SVG's value (390-wide frame,
 *  content y measured from below the status bar). Shared colours come from
 *  TeamilyPalette; the ones the palette has no slot for (icon-well accents,
 *  switch track / thumb, scrim, sheet handle) are listed once in [AgentColors].
 *  Placeholders stay literal: [agent-skill-id], [agent-skill-description].
 *  Overview's agent id / public URL have no source yet, so they read
 *  "Agent ID: Not set" / "Public URL: Not set" instead of raw placeholders.
 * ==========================================================================*/

// ---------------------------------------------------------------------------
// Colours
// ---------------------------------------------------------------------------

/** The SVG set's colour roles. Built from [TeamilyPalette] wherever it has the value. */
data class AgentColors(
    val ink: Color, val ink2: Color, val dim: Color, val border: Color, val chrome: Color,
    val recessed: Color, val topbar: Color, val body: Color, val hairline: Color,
    val atmosphereDeep: Color, val atmosphereStop: Float,
    val plusFill: Color, val plusIcon: Color, val primaryInk: Color, val dangerInk: Color,
    val aiFill: Color, val aiInk: Color,
    val well: Color, val wellInk: Color, val wellConnectors: Color, val wellConnectorsInk: Color,
    val wellMute: Color, val wellMuteInk: Color, val wellPin: Color, val wellPinInk: Color,
    val track: Color, val thumb: Color, val trackOn: Color, val thumbOn: Color,
    val unread: Color, val unreadInk: Color, val needsYou: Color, val scrim: Color, val sheet: Color,
    val button: Color, val buttonInk: Color, val tabIdle: Color, val tabActive: Color,
    val tabUnderline: Color, val focusRing: Color, val dashed: Color, val emptyIconBg: Color,
    val handle: Color, val navSelected: Color, val navSelectedInk: Color, val surface: Color,
    val notionMark: Color, val isDark: Boolean,
    /** The agent badge on an avatar (theme accent; mint in dark) and its glyph ink. */
    val badge: Color, val badgeInk: Color,
)

fun agentColors(p: TeamilyPalette, dark: Boolean): AgentColors = AgentColors(
    ink = p.textPrimary, ink2 = p.textSecondary,
    dim = if (dark) TeamilyMarks.dimDark else p.textSecondary,
    border = p.border, chrome = p.chromeFill, recessed = p.surfaceRecessed,
    topbar = p.topbarBg, body = p.bgElevated,
    hairline = if (dark) TeamilyMarks.hairlineDark else p.border,
    atmosphereDeep = p.atmosphereDeep, atmosphereStop = if (dark) 0.48f else 0.52f,
    plusFill = p.plusFill, plusIcon = p.plusIcon, primaryInk = p.primaryInk, dangerInk = p.dangerInk,
    aiFill = if (dark) TeamilyMarks.aiFillDark else p.primarySoft, aiInk = p.primaryInk,
    well = if (dark) p.navSelected else p.chromeFill,
    wellInk = if (dark) p.primaryInk else p.accent,
    wellConnectors = if (dark) TeamilyMarks.wellConnectorsDark else TeamilyMarks.wellConnectorsLight,
    wellConnectorsInk = if (dark) TeamilyMarks.wellConnectorsInkDark else TeamilyMarks.wellConnectorsInkLight,
    wellMute = if (dark) TeamilyMarks.wellMuteDark else TeamilyMarks.wellMuteLight,
    wellMuteInk = if (dark) TeamilyMarks.wellMuteInkDark else TeamilyMarks.wellMuteInkLight,
    wellPin = if (dark) TeamilyMarks.wellPinDark else TeamilyMarks.wellPinLight,
    wellPinInk = if (dark) TeamilyMarks.wellPinInkDark else TeamilyMarks.wellPinInkLight,
    track = if (dark) TeamilyMarks.trackDark else p.border,
    thumb = if (dark) TeamilyMarks.thumbDark else p.accentContrast,
    trackOn = if (dark) p.primaryInk else p.accent,
    thumbOn = if (dark) p.tileInk else p.accentContrast,
    unread = if (dark) p.primaryInk else p.primarySoft,
    unreadInk = if (dark) p.tileInk else p.primaryInk,
    needsYou = if (dark) TeamilyMarks.needsYouDark else TeamilyMarks.needsYouLight,
    scrim = if (dark) TeamilyMarks.scrimDark else TeamilyMarks.scrimLight,
    sheet = p.topbarBg, button = p.accent, buttonInk = TeamilyMarks.paper,
    tabIdle = p.textSecondary, tabActive = p.textPrimary, tabUnderline = p.logoMint,
    focusRing = if (dark) p.accentHover else p.accent,
    dashed = if (dark) TeamilyMarks.dashedDark else (p.borderStrong ?: TeamilyMarks.dashedLight),
    emptyIconBg = p.chromeFill,
    handle = if (dark) p.navSelected else (p.borderStrong ?: TeamilyMarks.dashedLight),
    navSelected = p.navSelected, navSelectedInk = p.navSelectedContent, surface = p.bgSurface,
    notionMark = if (dark) TeamilyMarks.paper else TeamilyMarks.ink, isDark = dark,
    badge = if (dark) p.logoMint else p.accent, badgeInk = p.accentContrast,
)

/** Follows the active theme the same way [teamilyPalette] does (dark palette = light ink). */
@Composable
private fun rememberAgentColors(): AgentColors {
    val p = teamilyPalette()
    return remember(p) { agentColors(p, dark = p.textPrimary.luminance() > 0.5f) }
}



// ---------------------------------------------------------------------------
// Type — Lato family from the theme, sizes / line heights from the SVGs
// ---------------------------------------------------------------------------

@Composable
private fun t(size: Int, line: Int, bold: Boolean = false): TextStyle = TextStyle(
    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = line.sp,
)

// ---------------------------------------------------------------------------
// Icons — the SVG set's 24-unit stroke glyphs (1.75 stroke, round caps / joins)
// ---------------------------------------------------------------------------

private class Part(val d: String, val filled: Boolean = false)

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

private val ICONS: Map<String, List<Part>> = mapOf(
    "back" to listOf(Part("M15 6l-6 6 6 6")),
    "close" to listOf(Part("M6 6l12 12M18 6L6 18")),
    "more" to listOf(Part(circle(6f, 12f, 1.4f), true), Part(circle(12f, 12f, 1.4f), true), Part(circle(18f, 12f, 1.4f), true)),
    "robot" to listOf(Part("M12 3.2v3.4"), Part(rrect(5f, 6.6f, 14f, 12.2f, 2.6f)), Part("M9.4 11.4v2.4M14.6 11.4v2.4M3.2 12.2v2.4M20.8 12.2v2.4")),
    "workspace" to listOf(
        Part(rrect(4f, 6.5f, 16f, 12.5f, 2.2f)),
        Part("M 9 6.5 V 4.5 a 1 1 0 0 1 1 -1 h 4 a 1 1 0 0 1 1 1 v 2"),
        Part("M 4 10.5 h 16"),
        Part("M 10 13.5 v 2.5 M 14 13.5 v 2.5"),
    ),
    "idcard" to listOf(Part(rrect(4f, 5.5f, 16f, 13f, 2.2f)), Part(circle(9.2f, 11.2f, 2.1f)), Part("M13.2 10.2h5.2M13.2 13.4h4.2")),
    "person_plus" to listOf(Part(circle(10f, 8.2f, 3f)), Part("M5.2 18c.4-3.2 2.4-4.8 4.8-4.8S14.4 14.8 14.8 18"), Part("M17.2 10.5v5M14.7 13h5")),
    "search" to listOf(Part(circle(11f, 11f, 6.2f)), Part("M20 20l-3.4-3.4")),
    "phone" to listOf(Part("M 8.2 4.8 h 2.2 l 1.2 3.2 -1.6 1.2 a 11 11 0 0 0 5.6 5.6 l 1.2 -1.6 3.2 1.2 v 2.2 c 0 .9 -.7 1.7 -1.6 1.8 -7.2 .6 -13.2 -5.4 -12.6 -12.6 .1 -.9 .9 -1.6 1.8 -1.6 z")),
    "chev_right" to listOf(Part("M10 6l6 6-6 6")),
    "chev_down" to listOf(Part("M6 9l6 6 6-6")),
    "chev_up" to listOf(Part("M6 15l6-6 6 6")),
    "translate" to listOf(Part("M4.5 6.5h10.5M9.75 6.5V5.2M7.2 6.5c.3 3.4 2.4 6.4 5.4 8"), Part("M12.8 9.2c-.8 2.2-2.4 4.2-4.6 5.6"), Part("M13.6 13.2h6.2M16.7 13.2l-3 6.3M16.7 13.2l3 6.3M14.6 17.4h4.2")),
    "channels" to listOf(Part(circle(12f, 16.6f, 1.5f)), Part("M 8.4 13.4 a 5.2 5.2 0 0 1 7.2 0 M 6.2 10.8 a 8.2 8.2 0 0 1 11.6 0 M 4.2 8.2 a 11 11 0 0 1 15.6 0")),
    "puzzle" to listOf(Part("M 8.2 4.8 h 2.6 c 0 1.5 1.2 2.4 2.4 2.4 S 15.6 6.3 15.6 4.8 h 2.2 A 1.6 1.6 0 0 1 19.4 6.4 v 2.4 c -1.5 0 -2.4 1.1 -2.4 2.3 s .9 2.3 2.4 2.3 v 2.4 a 1.6 1.6 0 0 1 -1.6 1.6 h -2.4 c 0 -1.5 -1.1 -2.4 -2.3 -2.4 s -2.3 .9 -2.3 2.4 H 8.2 A 1.6 1.6 0 0 1 6.6 17.4 v -2.2 c 1.5 0 2.4 -1.2 2.4 -2.4 S 8.1 10.4 6.6 10.4 V 8 A 1.6 1.6 0 0 1 8.2 6.4 z")),
    "link" to listOf(Part("M9.6 13.6l4.8-4.8"), Part("M 11.2 7.2 l 1.4 -1.4 a 3.2 3.2 0 0 1 4.5 4.5 L 15.7 11.7"), Part("M 12.8 16.8 l -1.4 1.4 a 3.2 3.2 0 0 1 -4.5 -4.5 L 8.3 12.3")),
    "bell" to listOf(Part("M 6.5 16.2 V 11 a 5.5 5.5 0 0 1 11 0 v 5.2 l 1.2 1.6 H 5.3 z"), Part("M 10 18.8 a 2 2 0 0 0 4 0")),
    "pin" to listOf(Part("M13.92 2.8l7.2 7.2-2.7 1.35-3.6 4.05v3.15L5.22 9.15h3.15l4.05-3.6z"), Part("M9.6 14.2l-6.3 6.3")),
    "share" to listOf(Part(circle(6.5f, 12f, 2.2f)), Part(circle(17f, 6.8f, 2.2f)), Part(circle(17f, 17.2f, 2.2f)), Part("M8.5 11.1l4.6-3.1M8.5 12.9l4.6 3.1")),
    "pencil" to listOf(Part("M13.2 5.2l5.6 5.6-10.2 10.2H3v-5.6z"), Part("M11.4 7l5.6 5.6")),
    "arrow_up_right" to listOf(Part("M8 16L16 8M9.5 8H16v6.5")),
    "copy" to listOf(Part(rrect(8.2f, 8.2f, 11f, 11f, 1.8f)), Part(rrect(4.8f, 4.8f, 11f, 11f, 1.8f))),
    "mail" to listOf(Part(rrect(3.6f, 6f, 16.8f, 12f, 2f)), Part("M4.4 7.4L12 12.4l7.6-5")),
    "layers" to listOf(Part("M12 3.4l8.6 4.3-8.6 4.3-8.6-4.3z"), Part("M3.4 12.2l8.6 4.3 8.6-4.3M3.4 16.2l8.6 4.3 8.6-4.3")),
    "window" to listOf(Part(rrect(4.2f, 5.2f, 15.6f, 13.6f, 2.2f)), Part("M4.2 9.2h15.6"), Part(circle(7.2f, 7.2f, 0.7f), true)),
    "api" to listOf(Part(circle(12f, 6.2f, 2.1f)), Part(circle(6.2f, 17f, 2.1f)), Part(circle(17.8f, 17f, 2.1f)), Part("M12 8.4v3.2M12 11.6L7.6 15.4M12 11.6l4.4 3.8")),
    "book" to listOf(Part("M5 5.2c1.8-.8 3.8-.8 5.8 0v13.2c-2-.8-4-.8-5.8 0zM13.2 5.2c1.8-.8 3.8-.8 5.8 0v13.2c-2-.8-4-.8-5.8 0z"), Part("M12 5.2v13.2")),
    "bolt" to listOf(Part("M13.2 3.5L6.5 13.2h5.2L10.8 20.5l6.8-9.8h-5.2z")),
    "trash" to listOf(Part("M4.5 7h15M9.5 7V4.8h5V7"), Part("M6.5 7l.9 12.2a1.6 1.6 0 0 0 1.6 1.5h6a1.6 1.6 0 0 0 1.6-1.5L17.5 7"), Part("M10.2 10.8v6M13.8 10.8v6")),
    "eraser" to listOf(Part("M5.2 14.2l6.4-6.4 7.2 7.2-4.2 4.2H8.2z"), Part("M9.4 10l4.8 4.8")),
    "bubble" to listOf(Part("M 5.4 6.2 h 13.2 A 2 2 0 0 1 20.6 8.2 v 6.2 A 2 2 0 0 1 18.6 16.4 h -3.6 v 3.8 l -4.6 -3.8 H 5.4 A 2 2 0 0 1 3.4 14.4 V 8.2 A 2 2 0 0 1 5.4 6.2 z")),
    "braces" to listOf(Part("M 9.2 5.5 H 7.4 A 2 2 0 0 0 5.4 7.5 v 3.2 L 4 12 l 1.4 1.3 v 3.2 a 2 2 0 0 0 2 2 h 1.8"), Part("M 14.8 5.5 h 1.8 a 2 2 0 0 1 2 2 v 3.2 L 20 12 l -1.4 1.3 v 3.2 a 2 2 0 0 1 -2 2 h -1.8")),
    "bars" to listOf(Part("M6 16.8V11.2M12 16.8V6.5M18 16.8V9.2")),
    "check" to listOf(Part("M5.5 12.2l4.2 4.2 8.8-9")),
    "star" to listOf(Part("M12 4.2l2.1 4.4 4.8.6-3.5 3.4.9 4.8L12 15.2 7.7 17.4l.9-4.8-3.5-3.4 4.8-.6z")),
    "glyph" to listOf(Part("M12 2.6l8.1 4.7v9.4L12 21.4 3.9 16.7V7.3z"), Part("M12 12l0-9.4M12 12l-8.1 4.7M12 12l8.1 4.7")),
)

private val iconCache = HashMap<String, ImageVector>()

private fun agentVector(name: String, strokeWidth: Float = 1.75f): ImageVector =
    iconCache.getOrPut("$name@$strokeWidth") {
        val b = ImageVector.Builder(
            name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
        )
        for (part in ICONS.getValue(name)) {
            if (part.filled) {
                b.addPath(pathData = addPathNodes(part.d), fill = SolidColor(TeamilyMarks.ink))
            } else {
                b.addPath(
                    pathData = addPathNodes(part.d),
                    stroke = SolidColor(TeamilyMarks.ink),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        b.build()
    }

@Composable
private fun AgentIcon(name: String, size: Dp, tint: Color, modifier: Modifier = Modifier, strokeWidth: Float = 1.75f) {
    Icon(imageVector = agentVector(name, strokeWidth), contentDescription = null, modifier = modifier.size(size), tint = tint)
}

// ---------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------

/** Base colour plus the Teamily radial glow at 8% / 92% with the SVG's radii. */
private fun Modifier.agentAtmosphere(base: Color, c: AgentColors): Modifier = this.drawBehind {
    drawRect(base)
    val cx = size.width * 0.08f
    val cy = size.height * 0.92f
    val rx = maxOf(cx, size.width - cx)
    val ry = maxOf(cy, size.height - cy)
    if (rx <= 0f || ry <= 0f) return@drawBehind
    val k = rx / ry
    val brush = Brush.radialGradient(
        0f to c.atmosphereDeep,
        c.atmosphereStop to c.atmosphereDeep.copy(alpha = 0f),
        center = Offset(cx, cy),
        radius = ry,
    )
    scale(scaleX = k, scaleY = 1f, pivot = Offset(cx, cy)) {
        drawRect(brush = brush, topLeft = Offset(cx - cx / k, 0f), size = Size(size.width / k, size.height))
    }
}

/** Mint circle with the cube glyph in its inner 62%, optional mini cube badge. */
@Composable
fun AgentCubeAvatar(size: Dp, modifier: Modifier = Modifier, badge: Dp = 0.dp, ring: Color = Color.Transparent) {
    val mint = teamilyPalette().logoMint
    val tileInk = teamilyPalette().tileInk
    Box(modifier.size(size)) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(mint), contentAlignment = Alignment.Center) {
            AgentIcon("glyph", size * 0.62f, tileInk)
        }
        if (badge > 0.dp) {
            val c = size / 2 * 1.62f
            Box(Modifier.offset(c - badge / 2 - 2.dp, c - badge / 2 - 2.dp).size(badge + 4.dp).clip(CircleShape).background(ring))
            Box(
                Modifier.offset(c - badge / 2, c - badge / 2).size(badge).clip(CircleShape).background(mint),
                contentAlignment = Alignment.Center,
            ) { AgentIcon("glyph", badge * 0.62f, tileInk, strokeWidth = 1.5f) }
        }
    }
}

// ---------------------------------------------------------------------------
// Per-agent identity — every agent reuses these screens with its id + name
// ---------------------------------------------------------------------------

/** Chat id prefix of an agent the user made with Agent home's Create Agent. */
const val MADE_AGENT_PREFIX = "made_agent_"

/**
 * Built-in agent threads. Groups, people and Dua threads are NOT agents, so
 * they keep the plain chat header. The legacy curated chat ids stay mapped
 * even though nothing lists them any more: a thread opened by one (an old
 * link, a test) keeps the agent header it always had.
 */
internal val BUILT_IN_AGENT_IDS: Set<String> = setOf(
    AGENT_CHAT_ID,
    PERSONAL_AGENT_CHAT_ID,
    "chat_webpage_developer",
    "chat_slides_assistant",
    "chat_market_research",
    "chat_accounting_assistant",
)

/** True for an agent's thread (built-in or user-made). */
fun isAgentChatId(chatId: String): Boolean =
    chatId.startsWith(MADE_AGENT_PREFIX) || chatId in BUILT_IN_AGENT_IDS

/** Only agents the user made can be deleted; built-in agents cannot. */
fun isUserMadeAgent(agentId: String): Boolean = agentId.startsWith(MADE_AGENT_PREFIX)

/**
 * One agent's avatar, circular: the bot art for Dua Agent / Personal Agent,
 * otherwise that thread's own cube (the same `CubeCatalog.forEntity` the chat
 * bubbles use). The old per-agent 3D character art belonged to the sample
 * roster and is gone with it.
 */
@Composable
internal fun AgentAvatar(agentId: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val res = when (agentId) {
        AGENT_CHAT_ID, PERSONAL_AGENT_CHAT_ID -> R.drawable.dua_bot_badge
        else -> null
    }
    if (res != null) {
        Image(
            painter = painterResource(res),
            contentDescription = "$name avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
    } else {
        val look = LocalBotAppearance.current
        BabyGrokMark(
            shape = look.shape,
            color = look.color,
            size = size,
            contentDescription = "$name avatar, ${look.shape.label}",
            modifier = modifier,
        )
    }
}

/** The Activity behind [this] context, unwrapping ContextWrappers; null if none. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Small green agent badge on an avatar's bottom-right, ringed in [ring]. */
@Composable
private fun AgentBadgeDot(size: Dp, c: AgentColors, ring: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(size + 3.dp).clip(CircleShape).background(ring), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).clip(CircleShape).background(c.badge), contentAlignment = Alignment.Center) {
            AgentIcon("robot", size * 0.7f, c.badgeInk, strokeWidth = 2f)
        }
    }
}

/**
 * An agent thread's header: 3-part floating pill header
 * (< circle · auto-sizing [Avatar + Name] capsule · ⋯ circle).
 * Tapping the avatar or name opens that agent's profile; ⋯ opens its New Group / actions.
 */
@Composable
fun AgentChatHeaderPill(
    agentId: String,
    name: String,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier,
    verified: Boolean = agentId == AGENT_CHAT_ID,
) {
    val c = rememberAgentColors()
    val backLabel = stringResource(R.string.back)
    val inviteLabel = stringResource(R.string.agent_invite)
    val profileLabel = stringResource(R.string.agent_open_profile, name)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Left — Floating < Back Circle (44.dp drawn circle centered in 48.dp touch target)
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .semantics {
                        contentDescription = backLabel
                        role = Role.Button
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AgentIcon("back", 24.dp, MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // 2. Middle — Auto-Sizing [Avatar + Name] Floating Capsule
        Box(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.Start)
                .animateContentSize(),
        ) {
            Surface(
                onClick = onOpenProfile,
                modifier = Modifier
                    .height(44.dp)
                    .semantics {
                        onClick(label = profileLabel) {
                            onOpenProfile()
                            true
                        }
                        role = Role.Button
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 6.dp, end = 16.dp),
                ) {
                    Box(Modifier.size(32.dp)) {
                        AgentAvatar(agentId, name, 32.dp)
                        AgentBadgeDot(
                            size = 10.dp,
                            c = c,
                            ring = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.align(Alignment.BottomEnd),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (verified) {
                        Spacer(Modifier.width(4.dp))
                        VerifiedCheck()
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // 3. Right — Floating ⋯ Circle (44.dp drawn circle centered in 48.dp touch target)
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                onClick = onInvite,
                modifier = Modifier
                    .size(44.dp)
                    .semantics {
                        text = AnnotatedString(inviteLabel)
                        contentDescription = inviteLabel
                        role = Role.Button
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AgentIcon("more", 24.dp, MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

/**
 * Layout-neutral 48dp tap target (review fix): [visual] is drawn exactly as
 * before, the click + its semantics sit on a ≥48dp box centred on it
 * ([touchTarget48]), and the pressed ripple stays clipped to [visual].
 */
@Composable
private fun Modifier.tap48(
    onClick: () -> Unit,
    visual: Modifier,
    label: String? = null,
    shiftUp: Dp = 0.dp,
): Modifier {
    val src = remember { MutableInteractionSource() }
    return touchTarget48(
        Modifier
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .then(if (label != null) Modifier.semantics { contentDescription = label; role = Role.Button } else Modifier),
        shiftUp,
    ).then(visual).indication(src, LocalIndication.current)
}

/**
 * The SVG switch: 40 × 24 track, 18dp thumb — drawn at that size, tappable on a
 * 48dp box, and announced by [label] (the row's title).
 */
@Composable
private fun AgentToggle(checked: Boolean, onChange: (Boolean) -> Unit, c: AgentColors, label: String, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .touchTarget48(
                Modifier
                    .toggleable(value = checked, interactionSource = src, indication = null, role = Role.Switch, onValueChange = onChange)
                    .semantics { contentDescription = label },
            )
            .size(40.dp, 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) c.trackOn else c.track)
            .indication(src, LocalIndication.current),
    ) {
        Box(
            Modifier.offset(x = if (checked) 18.dp else 4.dp, y = 3.dp).size(18.dp).clip(CircleShape)
                .background(if (checked) c.thumbOn else c.thumb),
        )
    }
}

@Composable
private fun Hairline(c: AgentColors) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
}

/** 56dp app bar. [center] centres icon + title; [close] swaps back for ×. */
@Composable
private fun AgentAppBar(
    title: String,
    c: AgentColors,
    onBack: () -> Unit,
    center: Boolean = false,
    icon: String? = null,
    close: Boolean = false,
    titleColor: Color = c.ink,
    trailing: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(48.dp).clip(CircleShape).clickable(onClick = onBack)
                .semantics { contentDescription = if (close) "Close" else "Back"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            if (close) {
                AgentIcon("close", 24.dp, titleColor)
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        if (center) {
            Row(
                Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(c.emptyIconBg), contentAlignment = Alignment.Center) {
                        AgentIcon(icon, 18.dp, c.wellInk)
                    }
                }
                Text(title, style = t(18, 24, true), color = titleColor)
            }
        } else {
            Text(title, style = t(18, 24, true), color = titleColor, modifier = Modifier.align(Alignment.CenterStart).padding(start = 56.dp))
        }
        trailing?.invoke(this)
    }
}

/**
 * Top region on the top-bar colour: status-bar inset, app bar, optional
 * subtitle and hairline.
 */
@Composable
private fun AgentTop(
    c: AgentColors,
    subtitle: String? = null,
    hairline: Boolean = true,
    below: (@Composable () -> Unit)? = null,
    bar: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(c.topbar).statusBarsPadding()) {
        if (subtitle != null) {
            Box {
                bar()
                Text(subtitle, style = t(13, 18), color = c.ink2, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 56.dp))
            }
            Spacer(Modifier.height(10.dp))
        } else {
            bar()
        }
        if (hairline) Hairline(c)
        below?.invoke()
    }
}

@Composable
private fun Well(icon: String, fill: Color, ink: Color, modifier: Modifier = Modifier, size: Dp = 36.dp, iconSize: Dp = 20.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(10.dp)).background(fill), contentAlignment = Alignment.Center) {
        AgentIcon(icon, iconSize, ink)
    }
}

@Composable
private fun PillButton(label: String, c: AgentColors, onClick: () -> Unit = {}) {
    Box(
        Modifier.padding(horizontal = 20.dp)
            .tap48(onClick, Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(c.chrome)),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = t(15, 22, true), color = c.ink) }
}

/** Tab label with the mint underline 22dp below its top (SVG 10, 13). */
@Composable
private fun UnderlinedTab(content: @Composable () -> Unit, c: AgentColors, on: Boolean) {
    Column(Modifier.width(IntrinsicSize.Max)) {
        content()
        if (on) {
            Spacer(Modifier.height(2.dp))
            Box(Modifier.fillMaxWidth().height(2.5.dp).clip(RoundedCornerShape(1.5.dp)).background(c.tabUnderline))
        }
    }
}

// ---------------------------------------------------------------------------
// 01 – 05  Profile
// ---------------------------------------------------------------------------

private enum class Trail { None, ChevRight, ChevDown, ChevUp, Toggle }

/** A settings row: 32dp well at x 35, title at x 81, trailing control 20dp from the edge. */
@Composable
private fun ProfileRow(
    c: AgentColors,
    height: Dp,
    icon: String,
    title: String,
    wellFill: Color = c.well,
    wellInk: Color = c.wellInk,
    subtitle: String? = null,
    link: String? = null,
    trail: Trail = Trail.None,
    checked: Boolean = false,
    onToggle: (Boolean) -> Unit = {},
    onClick: (() -> Unit)? = null,
    hairlineAt: Dp? = null,
) {
    // A tappable row never contains the switch (no call site passes both), so
    // the switch stays its own control; the row's tap box is ≥48dp (47dp rows).
    Box(
        if (onClick != null) Modifier.tap48(onClick, Modifier.fillMaxWidth().height(height))
        else Modifier.fillMaxWidth().height(height),
    ) {
        Well(icon, wellFill, wellInk, Modifier.offset(35.dp, 0.dp))
        val hasSub = subtitle != null || link != null
        Text(title, style = t(16, 24, true), color = c.ink, modifier = Modifier.offset(81.dp, if (hasSub) 1.dp else 4.dp))
        if (subtitle != null) Text(subtitle, style = t(12, 16), color = c.ink2, modifier = Modifier.offset(81.dp, 25.dp))
        if (link != null) Text(link, style = t(13, 18, true), color = c.primaryInk, modifier = Modifier.offset(81.dp, 41.dp))
        when (trail) {
            Trail.ChevRight, Trail.ChevDown, Trail.ChevUp -> AgentIcon(
                when (trail) {
                    Trail.ChevRight -> "chev_right"
                    Trail.ChevUp -> "chev_up"
                    else -> "chev_down"
                },
                20.dp, c.ink2, Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 6.dp),
            )
            Trail.Toggle -> AgentToggle(checked, onToggle, c, title, Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 4.dp))
            Trail.None -> Unit
        }
        if (hairlineAt != null) {
            Box(Modifier.padding(start = 83.dp, top = hairlineAt, end = 20.dp).fillMaxWidth().height(1.dp).background(c.hairline))
        }
    }
}

/**
 * Agent profile — SVG 01 (top), 02 (scrolled), 03 (more-sheet), 04 (Skills
 * open) and 05 (Connectors open). ONE screen for every agent: [agentId] picks
 * the avatar and [agentName] the title. The `initial*` parameters exist so
 * previews and tests can open each frame directly; the app passes none of them.
 *
 * Action pill (reference order): robot → Agent workspace · id card → Overview
 * (the full profile) · person+ → New Group · ⋯ → more-sheet (Share, Clear Chat
 * History, Delete agent).
 *
 * Mute / Pin are hoisted when the caller passes their handlers (the nav host
 * keeps them per agent); otherwise the screen keeps its own copy (previews).
 */
@Composable
fun DuaAgentProfileScreen(
    onBack: () -> Unit = {},
    onOpenWorkspace: () -> Unit = {},
    onOpenNewGroup: () -> Unit = {},
    onOpenConnectApp: () -> Unit = {},
    modifier: Modifier = Modifier,
    agentName: String = DUA_AGENT_TITLE,
    agentId: String = AGENT_CHAT_ID,
    onOpenOverview: () -> Unit = {},
    skillsList: List<SkillItem> = emptyList(),
    instructionsList: List<AgentInstructionItem> = emptyList(),
    triggersList: List<AgentTriggerItem> = emptyList(),
    onAddSkill: () -> Unit = {},
    onRunTrigger: (String) -> Unit = {},
    muted: Boolean = false,
    onMutedChange: ((Boolean) -> Unit)? = null,
    pinned: Boolean = false,
    onPinnedChange: ((Boolean) -> Unit)? = null,
    /** Clear Chat History, after the confirm dialog. */
    onClearHistory: () -> Unit = {},
    /** Delete agent, after the confirm dialog. Only called when [canDelete]. */
    canDelete: Boolean = isUserMadeAgent(agentId),
    onDelete: () -> Unit = {},
    initialSkillsExpanded: Boolean = false,
    initialConnectorsExpanded: Boolean = false,
    initialSheetOpen: Boolean = false,
    initialScrollToEnd: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
) {
    val c = rememberAgentColors()
    var skills by rememberSaveable { mutableStateOf(initialSkillsExpanded) }
    var connectors by rememberSaveable { mutableStateOf(initialConnectorsExpanded) }
    var sheet by rememberSaveable { mutableStateOf(initialSheetOpen) }
    // "clear" / "delete" while its confirm dialog is up.
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    val toggles = remember { mutableStateMapOf<String, Boolean>() }
    fun on(key: String) = toggles[key] == true
    val isMuted = if (onMutedChange != null) muted else on("mute")
    val setMuted: (Boolean) -> Unit = onMutedChange ?: { toggles["mute"] = it }
    val isPinned = if (onPinnedChange != null) pinned else on("pin")
    val setPinned: (Boolean) -> Unit = onPinnedChange ?: { toggles["pin"] = it }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val shareFailedMsg = stringResource(R.string.agent_share_failed)
    val clearedMsg = stringResource(R.string.agent_history_cleared)
    val shareText = stringResource(R.string.agent_share_text, agentName)
    val shareTitle = stringResource(R.string.agent_share)
    fun toast(msg: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(msg)
        }
    }
    if (initialScrollToEnd) LaunchedEffect(Unit) { scrollState.scrollTo(scrollState.maxValue) }
    val titleInk = if (sheet) c.dim else c.ink

    Box(modifier.fillMaxSize().background(c.topbar)) {
        Column(Modifier.fillMaxSize()) {
            AgentTop(c) { AgentAppBar(agentName, c, onBack, titleColor = titleInk) }
            Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                // Hero — top-bar colour down to content.
                Box(Modifier.fillMaxWidth().height(230.dp).background(c.topbar)) {
                    Box(Modifier.align(Alignment.TopCenter).offset(y = 12.dp).size(96.dp)) {
                        AgentAvatar(agentId, agentName, 92.dp, Modifier.align(Alignment.TopStart))
                        // Edit pencil on the avatar (reference). Display only:
                        // agent avatars have no editor yet.
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(30.dp).clip(CircleShape).background(c.topbar),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.size(26.dp).clip(CircleShape).background(c.aiFill),
                                contentAlignment = Alignment.Center,
                            ) { AgentIcon("pencil", 14.dp, c.aiInk) }
                        }
                    }
                    Row(
                        Modifier.align(Alignment.TopCenter).offset(y = 116.dp).height(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(agentName, style = t(17, 24, true), color = titleInk)
                        Box(
                            Modifier.size(30.dp, 16.dp).clip(RoundedCornerShape(8.dp)).background(c.aiFill),
                            contentAlignment = Alignment.Center,
                        ) { Text("AI", style = t(10, 12, true), color = c.aiInk) }
                    }
                    Row(
                        Modifier.align(Alignment.TopCenter).offset(y = 152.dp).size(260.dp, 52.dp).clip(RoundedCornerShape(26.dp)).background(c.body),
                    ) {
                        val actions = listOf<Triple<String, String, () -> Unit>>(
                            Triple("robot", stringResource(R.string.agent_action_workspace), onOpenWorkspace),
                            Triple("idcard", stringResource(R.string.agent_action_full_profile), onOpenOverview),
                            Triple("person_plus", stringResource(R.string.agent_action_new_group), onOpenNewGroup),
                            Triple("more", stringResource(R.string.agent_action_more), { sheet = true }),
                        )
                        for ((icon, label, action) in actions) {
                            Box(
                                Modifier.weight(1f).fillMaxSize()
                                    .clickable(onClick = action)
                                    .semantics { contentDescription = label; role = Role.Button },
                                contentAlignment = Alignment.Center,
                            ) { AgentIcon(icon, 20.dp, c.ink) }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.body)
                        .padding(top = 10.dp, bottom = 40.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(56.dp)) {
                        AgentIcon("search", 16.dp, c.ink2, Modifier.offset(28.dp, 14.dp))
                        Text("Search chat history", style = t(14, 20), color = c.ink2, modifier = Modifier.offset(54.dp, 12.dp))
                    }
                    ProfileRow(c, 73.dp, "phone", "Voice call", trail = Trail.ChevRight)
                    ProfileRow(
                        c, 126.dp, "translate", "Auto-translation",
                        subtitle = "Messages will be translated into English.", link = "Change language",
                        trail = Trail.Toggle, checked = on("translate"), onToggle = { toggles["translate"] = it },
                    )
                    ProfileRow(c, 87.dp, "channels", "Channels", subtitle = "0 connected", trail = Trail.ChevDown)
                    val skillCount = if (skillsList.isNotEmpty()) skillsList.size else 1
                    ProfileRow(
                        c, if (skills) 47.dp else 95.dp, "puzzle", "Skills", subtitle = "$skillCount active skill${if (skillCount != 1) "s" else ""}",
                        trail = if (skills) Trail.ChevUp else Trail.ChevDown, onClick = { skills = !skills },
                    )
                    if (skills) {
                        if (skillsList.isNotEmpty()) {
                            for (item in skillsList) {
                                Box(Modifier.fillMaxWidth().height(48.dp)) {
                                    Well("bolt", c.well, c.wellInk, Modifier.offset(37.dp, 0.dp), size = 28.dp, iconSize = 16.dp)
                                    Text(item.name, style = t(14, 20, true), color = c.ink, modifier = Modifier.offset(81.dp, (-1).dp))
                                    Text(
                                        item.description, style = t(12, 16), color = c.ink2, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, modifier = Modifier.offset(81.dp, 19.dp).padding(end = 40.dp),
                                    )
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxWidth().height(60.dp)) {
                                Well("bolt", c.well, c.wellInk, Modifier.offset(37.dp, 0.dp), size = 28.dp, iconSize = 16.dp)
                                Text("[agent-skill-id]", style = t(14, 20, true), color = c.ink, modifier = Modifier.offset(81.dp, (-1).dp))
                                Text(
                                    "[agent-skill-description]", style = t(12, 16), color = c.ink2, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, modifier = Modifier.offset(81.dp, 19.dp).padding(end = 101.dp),
                                )
                            }
                        }
                        Box(Modifier.padding(top = 8.dp, bottom = 12.dp)) { PillButton("+  Add Skill", c, onAddSkill) }
                        Spacer(Modifier.height(16.dp))
                    }
                    ProfileRow(
                        c, if (connectors) 48.dp else 77.dp, "link", "Connectors", c.wellConnectors, c.wellConnectorsInk,
                        subtitle = "2 apps connected", trail = if (connectors) Trail.ChevUp else Trail.ChevDown,
                        onClick = { connectors = !connectors },
                    )
                    if (connectors) {
                        for (name in listOf("GitHub", "Agent")) {
                            Box(Modifier.fillMaxWidth().height(48.dp)) {
                                Text(name, style = t(16, 24), color = c.ink, modifier = Modifier.offset(36.dp, 12.dp))
                                AgentToggle(on(name), { toggles[name] = it }, c, name, Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 12.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        PillButton("+  Connect App", c, onOpenConnectApp)
                        Spacer(Modifier.height(10.dp))
                        PillButton("+  Add MCP server", c)
                        Spacer(Modifier.height(40.dp))
                    }
                    ProfileRow(
                        c, 70.dp, "bell", stringResource(R.string.agent_mute), c.wellMute, c.wellMuteInk,
                        trail = Trail.Toggle, checked = isMuted, onToggle = setMuted, hairlineAt = 52.dp,
                    )
                    ProfileRow(
                        c, 103.dp, "pin", stringResource(R.string.agent_pin), c.wellPin, c.wellPinInk,
                        trail = Trail.Toggle, checked = isPinned, onToggle = setPinned,
                    )
                }
            }
        }
        if (sheet) {
            Box(Modifier.fillMaxSize().background(c.scrim).clickable { sheet = false })
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(c.sheet)
                    .padding(bottom = 24.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(32.dp)) {
                    Box(
                        Modifier.align(Alignment.TopCenter).offset(y = 10.dp).size(36.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp)).background(c.handle),
                    )
                }
                val items = listOf<Triple<String, String, Color>>(
                    Triple("share", stringResource(R.string.agent_share), c.ink),
                    Triple("eraser", stringResource(R.string.agent_clear_history), c.dangerInk),
                    Triple("trash", stringResource(R.string.agent_delete), c.dangerInk),
                )
                items.forEachIndexed { i, (icon, label, ink) ->
                    // Built-in agents cannot be deleted: their Delete row is
                    // shown dimmed and disabled instead of answering a tap.
                    val enabled = i != 2 || canDelete
                    val rowInk = if (enabled) ink else ink.copy(alpha = 0.38f)
                    Box(
                        Modifier.fillMaxWidth().height(56.dp).clickable(enabled = enabled, role = Role.Button) {
                            if (!enabled) return@clickable
                            sheet = false
                            when (i) {
                                0 -> {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    val chooser = Intent.createChooser(send, shareTitle)
                                    // Only a non-Activity context needs a new task.
                                    if (context.findActivity() == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    val ok = runCatching { context.startActivity(chooser) }.isSuccess
                                    if (!ok) toast(shareFailedMsg)
                                }
                                1 -> confirm = "clear"
                                else -> confirm = "delete"
                            }
                        },
                    ) {
                        AgentIcon(icon, 22.dp, rowInk, Modifier.offset(24.dp, 16.dp))
                        Text(label, style = t(16, 24), color = rowInk, modifier = Modifier.offset(60.dp, 15.dp))
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }

    when (confirm) {
        "clear" -> AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.agent_clear_confirm_title)) },
            text = { Text(stringResource(R.string.agent_clear_confirm_body, agentName)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    onClearHistory()
                    toast(clearedMsg)
                }) { Text(stringResource(R.string.agent_clear_confirm), color = c.dangerInk) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.profile_cancel)) }
            },
        )
        "delete" -> AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.agent_delete_confirm_title, agentName)) },
            text = { Text(stringResource(R.string.agent_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    onDelete()
                }) { Text(stringResource(R.string.agent_delete_confirm), color = c.dangerInk) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.profile_cancel)) }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// 06  Connect App
// ---------------------------------------------------------------------------

private class Brand(val d: String, val fill: Color? = null, val stroke: Color? = null, val alpha: Float = 1f)

private fun brandMarks(key: String, notion: Color): List<Brand> = when (key) {
    "gmail" -> listOf(
        Brand("M 2.2 6.4 c 0 -1 .8 -1.8 1.8 -1.8 h 16 c 1 0 1.8 .8 1.8 1.8 v 11.2 c 0 1 -.8 1.8 -1.8 1.8 h -16 A 1.8 1.8 0 0 1 2.2 17.6 z", TeamilyMarks.gmail),
        Brand("M3 6.2l9 7 9-7v2.2L12 15.6 3 8.4z", TeamilyMarks.paper),
        Brand("M 3 8.2 L 12 15.2 V 19 H 4.2 A 1.2 1.2 0 0 1 3 17.8 z", TeamilyMarks.gmailFold, alpha = 0.35f),
    )
    "calendar" -> listOf(
        Brand(rrect(3f, 3.4f, 18f, 17.2f, 2.4f), TeamilyMarks.paper),
        Brand("M3 3.4h18v5.2H3z", TeamilyMarks.googleBlue),
        Brand(circle(7.2f, 6f, 0.9f), TeamilyMarks.paper),
        Brand(circle(16.8f, 6f, 0.9f), TeamilyMarks.paper),
    )
    "notion" -> listOf(Brand("M6 4.2h3.4L16.2 15V4.2H19V19.8h-3.4L7.8 9v10.8H5V4.2z", notion))
    "sheets" -> listOf(
        Brand(rrect(3.6f, 2.8f, 16.8f, 18.4f, 2f), TeamilyMarks.googleGreen),
        Brand("M7 8.4h10M7 12h10M7 15.6h10M10.4 8.4v10.4", stroke = TeamilyMarks.paper),
    )
    "slack" -> listOf(
        Brand(rrect(9.4f, 2.4f, 5.2f, 8.2f, 2.6f), TeamilyMarks.slackMagenta),
        Brand(rrect(13.4f, 9.4f, 8.2f, 5.2f, 2.6f), TeamilyMarks.slackCyan),
        Brand(rrect(9.4f, 13.4f, 5.2f, 8.2f, 2.6f), TeamilyMarks.slackGreen),
        Brand(rrect(2.4f, 9.4f, 8.2f, 5.2f, 2.6f), TeamilyMarks.slackYellow),
    )
    "supabase" -> listOf(Brand("M14.4 2.6L4.6 13.8h6.2L8.4 21.4 19.4 10H13.2L16.6 2.6z", TeamilyMarks.supabase))
    "outlook" -> listOf(
        Brand(rrect(2.4f, 6f, 19.2f, 12.6f, 1.6f), TeamilyMarks.microsoft),
        Brand("M3.2 7.2L12 13.2l8.8-6", stroke = TeamilyMarks.paper),
    )
    "twitter" -> listOf(
        Brand(
            "M 22 5.8 a 8.2 8.2 0 0 1 -2.36 .65 4.1 4.1 0 0 0 1.8 -2.27 8.2 8.2 0 0 1 -2.6 1 4.1 4.1 0 0 0 -7 3.74 11.65 11.65 0 0 1 -8.45 -4.29 4.1 4.1 0 0 0 1.27 5.48 4.07 4.07 0 0 1 -1.86 -.51 v .05 a 4.1 4.1 0 0 0 3.29 4.02 4.1 4.1 0 0 1 -1.85 .07 4.1 4.1 0 0 0 3.83 2.85 A 8.23 8.23 0 0 1 2 18.4 a 11.62 11.62 0 0 0 6.29 1.84 c 7.55 0 11.68 -6.25 11.68 -11.67 0 -.18 0 -.36 -.01 -.53 A 8.35 8.35 0 0 0 22 5.8 z",
            TeamilyMarks.twitter,
        ),
    )
    "drive" -> listOf(
        Brand("M12 3.2L4.2 16.8h5.4L14.6 9z", TeamilyMarks.googleGreen),
        Brand("M12 3.2L19.8 16.8h-5.4L9.4 9z", TeamilyMarks.googleBlue),
        Brand("M4.2 16.8L7.4 21.2h9.2l3.2-4.4z", TeamilyMarks.googleYellow),
    )
    "docs" -> listOf(
        Brand("M 6 2.8 h 8.2 L 19.2 8 v 13.2 A 1.4 1.4 0 0 1 17.8 22.6 H 6 A 1.4 1.4 0 0 1 4.6 21.2 V 4.2 A 1.4 1.4 0 0 1 6 2.8 z", TeamilyMarks.googleBlue),
        Brand("M14.2 2.8V7.4c0 .5.4.9.9.9h4.1z", TeamilyMarks.googleBlueDeep),
        Brand("M7.6 12.2h8.8M7.6 15.2h8.8M7.6 18.2h6.2", stroke = TeamilyMarks.paper),
    )
    else -> emptyList()
}

/** 32dp surface tile with the app's 24dp brand mark (SVG 06). */
@Composable
private fun AppTile(key: String, c: AgentColors) {
    val vector = remember(key, c.notionMark) {
        val b = ImageVector.Builder(name = key, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        for (m in brandMarks(key, c.notionMark)) {
            if (m.fill != null) b.addPath(pathData = addPathNodes(m.d), fill = SolidColor(m.fill), fillAlpha = m.alpha)
            if (m.stroke != null) {
                b.addPath(
                    pathData = addPathNodes(m.d), stroke = SolidColor(m.stroke),
                    strokeLineWidth = if (key == "outlook") 1.6f else 1.2f,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        b.build()
    }
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(c.surface), contentAlignment = Alignment.Center) {
        Image(painter = rememberVectorPainter(vector), contentDescription = null, modifier = Modifier.size(24.dp))
        if (key == "calendar") {
            Text("31", style = t(9, 9, true), color = TeamilyMarks.googleBlue, textAlign = TextAlign.Center, modifier = Modifier.offset(y = 3.dp))
        }
    }
}

private val CONNECT_APPS = listOf(
    "gmail" to "Gmail", "calendar" to "Google Calendar", "notion" to "Notion", "sheets" to "Google Sheets",
    "slack" to "Slack", "supabase" to "Supabase", "outlook" to "Outlook", "twitter" to "Twitter",
    "drive" to "Google Drive", "docs" to "Google Docs",
)

/** SVG 06 — Connect App: focused search, ten apps with icon tiles, Manage in Settings. */
@Composable
fun DuaAgentConnectAppScreen(onClose: () -> Unit = {}, modifier: Modifier = Modifier) {
    val c = rememberAgentColors()
    Box(modifier.fillMaxSize().agentAtmosphere(c.topbar, c)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(84.dp)) {
                Text("Connect App", style = t(20, 28, true), color = c.ink, modifier = Modifier.align(Alignment.Center))
                Box(
                    Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).size(48.dp).clip(CircleShape).clickable(onClick = onClose)
                        .semantics { contentDescription = "Close"; role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) { AgentIcon("close", 24.dp, c.ink) }
            }
            Box(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp))
                    .background(c.recessed).border(1.75.dp, c.focusRing, RoundedCornerShape(22.dp)),
            ) {
                AgentIcon("search", 20.dp, c.ink2, Modifier.offset(14.dp, 12.dp))
                Text("Search apps…", style = t(15, 22), color = c.ink2, modifier = Modifier.offset(40.dp, 11.dp))
            }
            Spacer(Modifier.height(16.dp))
            for ((key, name) in CONNECT_APPS) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppTile(key, c)
                    Spacer(Modifier.width(10.dp))
                    Text(name, style = t(16, 22), color = c.ink, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.tap48(
                            {}, Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).border(1.25.dp, c.border, RoundedCornerShape(16.dp)),
                        ).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Connect", style = t(13, 18, true), color = c.ink) }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AgentIcon("check", 14.dp, c.ink2, Modifier.offset(y = 2.dp))
            Text("Manage in Settings", style = t(13, 16), color = c.ink2)
        }
    }
}

// ---------------------------------------------------------------------------
// Shared list row (07, 08)
// ---------------------------------------------------------------------------

/** Three overlapping avatar circles — the 3-avatar group cluster. */
@Composable
internal fun GroupCluster() {
    Box(Modifier.size(40.dp)) {
        val s = 23.2.dp
        val avatars = listOf(
            Triple(Offset(0f, 16.8f), R.drawable.avatar_code_reviewer, "Code Reviewer"),
            Triple(Offset(16.8f, 20.28f), R.drawable.avatar_backend_architect, "Backend Architect"),
            Triple(Offset(8.12f, 0f), R.drawable.avatar_frontend_developer, "Frontend Developer"),
        )
        for ((pos, res, desc) in avatars) {
            Image(
                painter = painterResource(res),
                contentDescription = desc,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(pos.x.dp, pos.y.dp)
                    .size(s)
                    .clip(CircleShape)
                    .border(1.5.dp, TeamilyMarks.clusterRing, CircleShape),
            )
        }
    }
}

@Composable
private fun ConversationRow(
    c: AgentColors,
    title: String,
    subtitle: String,
    time: String,
    unread: String? = null,
    plus: Boolean = false,
    avatar: @Composable () -> Unit = { GroupCluster() },
) {
    Box(Modifier.fillMaxWidth().height(54.dp)) {
        Box(Modifier.offset(16.dp, 6.dp)) { avatar() }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 64.dp, end = 16.dp, top = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = t(14, 18, true), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (plus) {
                    Box(Modifier.padding(top = 1.dp).height(15.dp).clip(RoundedCornerShape(8.dp)).background(TeamilyLightPalette.tileViolet)) {
                        AgentIcon("star", 9.dp, TeamilyLightPalette.tileInk, Modifier.offset(4.dp, 3.dp), strokeWidth = 1.2f)
                        Text("PLUS", style = t(11, 14, true), color = TeamilyLightPalette.tileInk, modifier = Modifier.padding(start = 14.dp, end = 8.dp))
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(time, style = t(11, 14), color = c.ink2)
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 64.dp, end = 16.dp, bottom = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = subtitle,
                style = t(12, 16),
                color = c.ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (unread != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(c.unread),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(unread, style = t(10, 13, true), color = c.unreadInk)
                }
            }
        }
    }
}

@Composable
private fun SearchField(c: AgentColors, label: String) {
    Box(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(36.dp).clip(RoundedCornerShape(18.dp))
            .background(c.recessed).border(1.dp, c.border, RoundedCornerShape(18.dp)),
    ) {
        AgentIcon("search", 18.dp, c.ink2, Modifier.offset(12.dp, 9.dp))
        Text(label, style = t(13, 18), color = c.ink2, modifier = Modifier.offset(36.dp, 8.dp))
    }
}

// ---------------------------------------------------------------------------
// 07  Agent workspace
// ---------------------------------------------------------------------------

enum class AgentWorkspaceDestination { Overview, Channels, LiveSites, Mail, Artifacts, AgentApi }

/**
 * SVG 07 — Agent workspace matching official Teamily AI app parity.
 * The Manage grid features all 6 tiles; CHAT section displays active team conversations.
 */
@Composable
fun DuaAgentWorkspaceScreen(
    onBack: () -> Unit = {},
    onOpen: (AgentWorkspaceDestination) -> Unit = {},
    modifier: Modifier = Modifier,
    agentName: String = DUA_AGENT_TITLE,
    agentId: String = AGENT_CHAT_ID,
    onAgentHome: () -> Unit = onBack,
    userAvatar: (@Composable () -> Unit)? = null,
) {
    val c = rememberAgentColors()
    Column(modifier.fillMaxSize().background(c.body)) {
        AgentTop(c) { AgentAppBar("Agent workspace", c, onBack, center = true) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp)).background(c.body)) {
                AgentAvatar(agentId, agentName, 36.dp, Modifier.offset(14.dp, 9.dp))
                Text(agentName, style = t(15, 20, true), color = c.ink, modifier = Modifier.offset(58.dp, 8.dp))
                Row(
                    Modifier.offset(58.dp, 28.dp).clickable(onClick = onAgentHome),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Agent home", style = t(12, 16), color = c.primaryInk)
                    AgentIcon("arrow_up_right", 12.dp, c.primaryInk, Modifier.offset(y = 1.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("MANAGE", style = t(10, 14, true), color = c.ink2, modifier = Modifier.padding(start = 16.dp))
            Spacer(Modifier.height(5.dp))
            val tiles = listOf(
                Triple("bars", "Overview", AgentWorkspaceDestination.Overview),
                Triple("channels", "Channels", AgentWorkspaceDestination.Channels),
                Triple("window", "Live Sites", AgentWorkspaceDestination.LiveSites),
                Triple("mail", "Mail", AgentWorkspaceDestination.Mail),
                Triple("layers", "Artifacts", AgentWorkspaceDestination.Artifacts),
                Triple("api", "Agent API", AgentWorkspaceDestination.AgentApi),
            )
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (rowTiles in tiles.chunked(3)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((icon, label, dest) in rowTiles) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(c.body)
                                    .clickable { onOpen(dest) }
                            ) {
                                AgentIcon(icon, 18.dp, c.wellInk, Modifier.align(Alignment.TopCenter).offset(y = 10.dp))
                                Text(
                                    label, style = t(11, 15, true), color = c.ink, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().offset(y = 35.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("CHAT", style = t(10, 14, true), color = c.ink2)
                Text("5", style = t(11, 14), color = c.ink2, modifier = Modifier.align(Alignment.TopEnd))
            }
            Spacer(Modifier.height(5.dp))
            SearchField(c, "Search")
            Spacer(Modifier.height(6.dp))
            ConversationRow(
                c, "fareed",
                "Teamily Android ai app Open karo rea Open frontend developer than click upper baar it agent profile,",
                "19:50", plus = true, unread = "2", avatar = {
                    if (userAvatar != null) {
                        Box(Modifier.size(40.dp).clip(CircleShape)) { userAvatar() }
                    } else {
                        Image(
                            painter = painterResource(R.drawable.dua_user_avatar),
                            contentDescription = "fareed avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                        )
                    }
                },
            )
            ConversationRow(c, "Full-Stack Delivery Guild", "[Someone @you] Code Reviewer: [Custom Message]", "Sun", unread = "4")
            ConversationRow(c, "Full-Stack Delivery Guild", "[Someone @you] Frontend Developer: PR ready hai: https://github.com/KRFbrothers/teamily-android-ui/pull/9", "Sun")
            ConversationRow(c, "Full-Stack Delivery Guild", "[Someone @you] $agentName: [Custom Message]", "11/03/26", unread = "4")
            ConversationRow(c, "AI Feature Build Room", "Prompt Engineer, Data Engineer, Frontend Developer were invited to", "10/17/26", unread = "1")
        }
    }
}

// ---------------------------------------------------------------------------
// 08  Overview
// ---------------------------------------------------------------------------

/**
 * SVG 08 — Overview, for any agent: [agentId] picks the avatar and
 * [agentName] fills the card and "Pick up where … left off".
 */
@Composable
fun DuaAgentOverviewScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    agentName: String = DUA_AGENT_TITLE,
    agentId: String = AGENT_CHAT_ID,
) {
    val description = if (agentId == AGENT_CHAT_ID) {
        DUA_AGENT_SUBTITLE
    } else {
        stringResource(R.string.agent_overview_description, agentName)
    }
    val c = rememberAgentColors()
    Column(modifier.fillMaxSize().background(c.body)) {
        AgentTop(c, subtitle = "Attention, recent work, and original conversations") {
            AgentAppBar("Overview", c, onBack, center = true, icon = "bars")
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(15.dp))
            Box(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(208.dp).clip(RoundedCornerShape(20.dp))
                    .background(c.chrome).border(1.dp, c.border, RoundedCornerShape(20.dp)),
            ) {
                AgentAvatar(agentId, agentName, 52.dp, Modifier.offset(16.dp, 16.dp))
                Text("Agent ID: Not set", style = t(12, 16), color = c.ink2, modifier = Modifier.offset(80.dp, 14.dp))
                Text(
                    agentName, style = t(18, 26, true), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(80.dp, 36.dp).padding(end = 96.dp),
                )
                Text(
                    description, style = t(14, 20), color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(16.dp, 80.dp).width(326.dp),
                )
                Row(Modifier.offset(16.dp, 116.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Public URL: Not set", style = t(12, 16), color = c.ink2)
                    AgentIcon("copy", 14.dp, c.ink2, Modifier.offset(y = 1.dp))
                }
                Box(
                    // Share sits 8dp above Edit / View public profile, so its 48dp
                    // box grows 2dp more upward and meets theirs without overlap.
                    Modifier.align(Alignment.TopEnd).padding(top = 108.dp, end = 16.dp).tap48(
                        {},
                        Modifier.size(96.dp, 36.dp).clip(RoundedCornerShape(18.dp)).background(c.body)
                            .border(1.dp, c.border, RoundedCornerShape(18.dp)),
                        shiftUp = 2.dp,
                    ),
                ) {
                    AgentIcon("share", 20.dp, c.ink, Modifier.offset(14.dp, 8.dp))
                    Text("Share", style = t(14, 20, true), color = c.ink, modifier = Modifier.offset(38.dp, 8.dp))
                }
                Row(Modifier.offset(16.dp, 152.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.tap48({}, Modifier.size(145.36.dp, 40.dp).clip(RoundedCornerShape(20.dp)).background(c.button)),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AgentIcon("pencil", 18.dp, c.buttonInk)
                        Text("Edit profile", style = t(13, 18, true), color = c.buttonInk)
                    }
                    Row(
                        Modifier.tap48(
                            {}, Modifier.size(170.64.dp, 40.dp).clip(RoundedCornerShape(20.dp)).border(1.25.dp, c.ink, RoundedCornerShape(20.dp)),
                        ),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AgentIcon("arrow_up_right", 16.dp, c.ink)
                        Text("View public profile", style = t(13, 18, true), color = c.ink)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            // The heading takes the space LEFT of the chip and wraps there,
            // so a long agent name can never run under "Channels".
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    "Pick up where $agentName left off", style = t(20, 26, true), color = c.ink,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.padding(top = 4.dp).tap48(
                        {},
                        Modifier.size(112.dp, 36.dp).clip(RoundedCornerShape(18.dp)).background(c.chrome)
                            .border(1.dp, c.border, RoundedCornerShape(18.dp)),
                    ),
                ) {
                    AgentIcon("channels", 20.dp, c.wellInk, Modifier.offset(12.dp, 8.dp))
                    Text("Channels", style = t(13, 20, true), color = c.ink, modifier = Modifier.offset(36.dp, 8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "2 items need your attention across 3 conversations.", style = t(14, 20), color = c.ink2,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Needs you", style = t(16, 22, true), color = c.ink)
                Text("2", style = t(14, 20, true), color = c.needsYou, modifier = Modifier.offset(y = 1.dp))
            }
            Spacer(Modifier.height(2.dp))
            Text("Unread or waiting for a decision", style = t(12, 16), color = c.ink2, modifier = Modifier.padding(start = 16.dp))
            Spacer(Modifier.height(12.dp))
            ConversationRow(c, "Full-Stack Delivery Guild", "Frontend Developer, Backend Archite…", "Wed", unread = "1")
            ConversationRow(c, "Full-Stack Delivery Guild", "[Someone @you] $agentName: [Cust…", "11/03/26", unread = "2")
        }
    }
}

// ---------------------------------------------------------------------------
// 09  Artifacts (empty)
// ---------------------------------------------------------------------------

/** SVG 09 — Artifacts empty state. */
@Composable
fun DuaAgentArtifactsScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val c = rememberAgentColors()
    Column(modifier.fillMaxSize().background(c.body)) {
        AgentTop(c, subtitle = "Work created and shared by this Agent") {
            AgentAppBar("Artifacts", c, onBack, center = true, icon = "layers")
        }
        Spacer(Modifier.height(15.dp))
        Text("0 published · 0 draft", style = t(14, 20), color = c.ink2, modifier = Modifier.padding(start = 16.dp))
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(88.dp).drawBehind {
                val w = 1.5.dp.toPx()
                drawRoundRect(
                    color = c.dashed,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))),
                )
            },
            contentAlignment = Alignment.Center,
        ) { Text("No artifacts yet.", style = t(15, 22), color = c.ink2) }
    }
}

// ---------------------------------------------------------------------------
// 10  Agent API
// ---------------------------------------------------------------------------

@Composable
private fun ApiCard(c: AgentColors, icon: String, title: String, body: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.chrome)) {
        Well(icon, c.well, c.ink, Modifier.offset(16.dp, 12.dp))
        Row(Modifier.offset(58.dp, 14.dp)) {
            Text(title, style = t(15, 22, true), color = c.ink)
            Spacer(Modifier.width(8.dp))
            Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AgentIcon("copy", 14.dp, c.ink2)
                Text("Off", style = t(12, 16), color = c.ink2)
            }
        }
        AgentToggle(checked, onToggle, c, title, Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp))
        Text(body, style = t(13, 18), color = c.ink2, modifier = Modifier.padding(start = 58.dp, top = 40.dp, end = 72.dp, bottom = 22.dp))
    }
}

/** SVG 10 — Agent API, Setup tab. */
@Composable
fun DuaAgentApiScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier, agentName: String = DUA_AGENT_TITLE) {
    val c = rememberAgentColors()
    val toggles = remember { mutableStateMapOf<String, Boolean>() }
    Column(modifier.fillMaxSize().background(c.body)) {
        AgentTop(
            c,
            subtitle = "How websites, programs and other agents reach $agentName, and what happened there",
            below = {
                Row(Modifier.padding(start = 16.dp, top = 11.dp).height(28.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("Setup", "Conversations", "Call log", "Keys", "Site access").forEachIndexed { i, tab ->
                        UnderlinedTab({ Text(tab, style = t(14, 20, i == 0), color = if (i == 0) c.tabActive else c.tabIdle) }, c, on = i == 0)
                    }
                }
            },
        ) {
            AgentAppBar("Agent API", c, onBack, center = true, icon = "api") {
                AgentIcon("book", 24.dp, c.ink, Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            Text("On a website", style = t(18, 26, true), color = c.ink, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                "A chat bubble on your own site, answered by $agentName.", style = t(13, 18), color = c.ink2,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            ApiCard(c, "bubble", "Website Widget", "Embed a chat bubble on any website", toggles["widget"] == true) { toggles["widget"] = it }
            Spacer(Modifier.height(20.dp))
            Text("From programs and other agents", style = t(18, 26, true), color = c.ink, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                "Your backend over REST or the OpenAI SDK, and AI clients such as Claude Code over MCP.",
                style = t(13, 18), color = c.ink2, modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            ApiCard(
                c, "braces", "RESTful API for servers", "Call $agentName over HTTPS from your own backend.",
                toggles["rest"] == true,
            ) { toggles["rest"] = it }
            Spacer(Modifier.height(10.dp))
            ApiCard(
                c, "robot", "API for other Agents",
                "Let Codex, Claude Code and other Agents discover and call $agentName over MCP.", toggles["mcp"] == true,
            ) { toggles["mcp"] = it }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// 11 / 12  Live Sites (empty), Mail (coming soon)
// ---------------------------------------------------------------------------

/** SVG 11 — Live Sites empty state. */
@Composable
fun DuaAgentLiveSitesScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier, agentName: String = DUA_AGENT_TITLE) {
    val c = rememberAgentColors()
    Column(modifier.fillMaxSize().background(c.body)) {
        AgentTop(c, subtitle = "Built, connected, and operated by $agentName's Agent Team") {
            AgentAppBar("Live Sites", c, onBack, center = true, icon = "window")
        }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = 216.dp).size(64.dp).clip(RoundedCornerShape(16.dp)).background(c.emptyIconBg),
                contentAlignment = Alignment.Center,
            ) { AgentIcon("window", 28.dp, c.wellInk) }
            Text("No live sites yet", style = t(18, 26, true), color = c.ink, modifier = Modifier.align(Alignment.TopCenter).offset(y = 296.dp))
            Text(
                "Sites $agentName builds and publishes appear here, each with its Web admin one click away.",
                style = t(14, 20), color = c.ink2, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = 328.dp).width(300.dp),
            )
        }
    }
}

/** SVG 12 — Mail, coming soon on mobile. */
@Composable
fun DuaAgentMailScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val c = rememberAgentColors()
    Column(modifier.fillMaxSize().background(c.body)) {
        AgentTop(c, hairline = false) { AgentAppBar("Mail", c, onBack, center = true, icon = "mail") }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = 235.dp).size(72.dp).clip(CircleShape).background(c.emptyIconBg),
                contentAlignment = Alignment.Center,
            ) { AgentIcon("mail", 28.dp, c.wellInk) }
            Text("Coming soon", style = t(20, 28, true), color = c.ink, modifier = Modifier.align(Alignment.TopCenter).offset(y = 327.dp))
            Text(
                "Agent email, drafts, and replies", style = t(14, 20), color = c.ink2,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = 361.dp),
            )
            Text(
                "This section is coming soon on mobile.", style = t(13, 18), color = c.ink2,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = 387.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 13  New Group
// ---------------------------------------------------------------------------

@Composable
private fun ColoredAgentAvatar(fill: Color, ring: Color) {
    Box(Modifier.size(32.dp)) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
            AgentIcon("glyph", 17.6.dp, TeamilyLightPalette.tileInk, strokeWidth = 1.5f)
        }
        Box(Modifier.offset(17.32.dp, 17.32.dp).size(17.2.dp).clip(CircleShape).background(ring))
        Box(Modifier.offset(18.92.dp, 18.92.dp).size(14.dp).clip(CircleShape).background(TeamilyLightPalette.logoMint), contentAlignment = Alignment.Center) {
            AgentIcon("glyph", 9.dp, TeamilyLightPalette.tileInk, strokeWidth = 1.2f)
        }
    }
}

private val GROUP_AGENTS = listOf(
    TeamilyLightPalette.tileYellow to "Finance Assistant",
    TeamilyMarks.assistantSlides to "Slides Assistant",
    TeamilyLightPalette.tileCyan to "Webpage Developer",
    TeamilyMarks.assistantTravel to "Travel Planner",
    TeamilyLightPalette.tileMint to "Health Assistant",
)

/** SVG 13 — New Group (Custom Group selected, Agents tab, Members Pay). */
@Composable
fun DuaAgentNewGroupScreen(
    onClose: () -> Unit = {},
    onCreate: () -> Unit = {},
    modifier: Modifier = Modifier,
    /** The agent the group starts with ("Selected 1"). */
    agentId: String = AGENT_CHAT_ID,
    agentName: String = DUA_AGENT_TITLE,
) {
    val c = rememberAgentColors()
    val selectedInk = if (c.isDark) c.ink else c.navSelectedInk
    Box(modifier.fillMaxSize().agentAtmosphere(c.body, c)) {
        Column(Modifier.fillMaxSize()) {
            AgentTop(c, hairline = false) {
                AgentAppBar("New Group", c, onClose, close = true) {
                    Box(
                        Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                            .tap48(onCreate, Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(c.plusFill))
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Create", style = t(16, 22, true), color = c.plusIcon) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp)).background(c.chrome)) {
                Box(Modifier.offset(2.dp, 2.dp).size(177.dp, 36.dp).clip(RoundedCornerShape(18.dp)).background(c.navSelected))
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Custom Group", style = t(13, 22, true), color = selectedInk)
                    }
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("AI Agent Team", style = t(13, 22), color = c.ink2)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.padding(horizontal = 16.dp).height(36.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Group Name", style = t(13, 18, true), color = c.ink, modifier = Modifier.padding(top = 8.dp))
                Box(Modifier.weight(1f).fillMaxSize().border(1.25.dp, c.border, RoundedCornerShape(10.dp))) {
                    Text("Enter group name (Optional)", style = t(13, 18), color = c.ink2, modifier = Modifier.offset(12.dp, 7.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Hairline(c)
            // "Selected 1" text top sits 11dp under the hairline; the 28dp avatar is
            // centred on it (6dp above the text top), so the row starts 6dp earlier.
            Spacer(Modifier.height(5.dp))
            Row(Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Selected 1", style = t(13, 18), color = c.ink2, modifier = Modifier.padding(top = 6.dp))
                Box(Modifier.size(30.dp)) {
                    AgentAvatar(agentId, agentName, 28.dp)
                    AgentBadgeDot(9.dp, c, c.body, Modifier.align(Alignment.BottomEnd))
                }
            }
            Spacer(Modifier.height(18.dp))
            SearchField(c, "Search")
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(26.dp)) {
                Box(Modifier.offset(48.dp, 0.dp)) {
                    UnderlinedTab({
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Agents", style = t(14, 20, true), color = c.ink)
                            Text("21", style = t(13, 20), color = c.ink2)
                        }
                    }, c, on = true)
                }
                Row(Modifier.offset(220.dp, 0.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Friends", style = t(14, 20), color = c.ink2)
                    Text("0", style = t(13, 20), color = c.ink2)
                }
            }
            Spacer(Modifier.height(8.dp))
            for ((fill, name) in GROUP_AGENTS.filter { it.second != agentName }) {
                Box(Modifier.fillMaxWidth().height(52.dp)) {
                    Box(Modifier.offset(23.dp, 10.dp).size(20.dp).border(1.5.dp, c.ink2, CircleShape))
                    Box(Modifier.offset(48.dp, 4.dp)) { ColoredAgentAvatar(fill, c.body) }
                    Text(name, style = t(15, 22, true), color = c.ink, modifier = Modifier.offset(86.dp, 8.dp))
                }
            }
        }
        // Payment mode, pinned to the bottom (SVG frame y 748 → 844).
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(96.dp).background(c.body)) {
            Row(Modifier.offset(16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Payment Mode", style = t(13, 18, true), color = c.ink)
                Row(Modifier.offset(y = (-4).dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(c.navSelected).padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Members Pay", style = t(12, 16, true), color = selectedInk) }
                    Box(
                        Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, c.border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Owner Pays", style = t(12, 16), color = c.ink2) }
                }
            }
            Text("Each member uses their own credits", style = t(12, 16), color = c.ink2, modifier = Modifier.offset(16.dp, 44.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark for each approved SVG (390 × 844)
// ---------------------------------------------------------------------------

@Preview(name = "Agent 01 · Profile — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent01ProfileLight() = DuaGenAITheme(darkTheme = false) { DuaAgentProfileScreen() }

@Preview(name = "Agent 01 · Profile — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent01ProfileDark() = DuaGenAITheme(darkTheme = true) { DuaAgentProfileScreen() }

@Preview(name = "Agent 02 · Profile scrolled — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent02ProfileScrolledLight() = DuaGenAITheme(darkTheme = false) { DuaAgentProfileScreen(initialScrollToEnd = true) }

@Preview(name = "Agent 02 · Profile scrolled — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent02ProfileScrolledDark() = DuaGenAITheme(darkTheme = true) { DuaAgentProfileScreen(initialScrollToEnd = true) }

@Preview(name = "Agent 03 · More sheet — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent03MoreSheetLight() = DuaGenAITheme(darkTheme = false) { DuaAgentProfileScreen(initialSheetOpen = true) }

@Preview(name = "Agent 03 · More sheet — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent03MoreSheetDark() = DuaGenAITheme(darkTheme = true) { DuaAgentProfileScreen(initialSheetOpen = true) }

@Preview(name = "Agent 04 · Skills expanded — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent04SkillsExpandedLight() = DuaGenAITheme(darkTheme = false) { DuaAgentProfileScreen(initialSkillsExpanded = true, initialScrollToEnd = true) }

@Preview(name = "Agent 04 · Skills expanded — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent04SkillsExpandedDark() = DuaGenAITheme(darkTheme = true) { DuaAgentProfileScreen(initialSkillsExpanded = true, initialScrollToEnd = true) }

@Preview(name = "Agent 05 · Connectors expanded — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent05ConnectorsExpandedLight() = DuaGenAITheme(darkTheme = false) { DuaAgentProfileScreen(initialConnectorsExpanded = true, initialScrollToEnd = true) }

@Preview(name = "Agent 05 · Connectors expanded — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent05ConnectorsExpandedDark() = DuaGenAITheme(darkTheme = true) { DuaAgentProfileScreen(initialConnectorsExpanded = true, initialScrollToEnd = true) }

@Preview(name = "Agent 06 · Connect App — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent06ConnectAppLight() = DuaGenAITheme(darkTheme = false) { DuaAgentConnectAppScreen() }

@Preview(name = "Agent 06 · Connect App — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent06ConnectAppDark() = DuaGenAITheme(darkTheme = true) { DuaAgentConnectAppScreen() }

@Preview(name = "Agent 07 · Agent workspace — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent07AgentWorkspaceLight() = DuaGenAITheme(darkTheme = false) { DuaAgentWorkspaceScreen() }

@Preview(name = "Agent 07 · Agent workspace — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent07AgentWorkspaceDark() = DuaGenAITheme(darkTheme = true) { DuaAgentWorkspaceScreen() }

@Preview(name = "Agent 08 · Overview — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent08OverviewLight() = DuaGenAITheme(darkTheme = false) { DuaAgentOverviewScreen() }

@Preview(name = "Agent 08 · Overview — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent08OverviewDark() = DuaGenAITheme(darkTheme = true) { DuaAgentOverviewScreen() }

@Preview(name = "Agent 09 · Artifacts — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent09ArtifactsLight() = DuaGenAITheme(darkTheme = false) { DuaAgentArtifactsScreen() }

@Preview(name = "Agent 09 · Artifacts — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent09ArtifactsDark() = DuaGenAITheme(darkTheme = true) { DuaAgentArtifactsScreen() }

@Preview(name = "Agent 10 · Agent API — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent10AgentApiLight() = DuaGenAITheme(darkTheme = false) { DuaAgentApiScreen() }

@Preview(name = "Agent 10 · Agent API — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent10AgentApiDark() = DuaGenAITheme(darkTheme = true) { DuaAgentApiScreen() }

@Preview(name = "Agent 11 · Live Sites — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent11LiveSitesLight() = DuaGenAITheme(darkTheme = false) { DuaAgentLiveSitesScreen() }

@Preview(name = "Agent 11 · Live Sites — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent11LiveSitesDark() = DuaGenAITheme(darkTheme = true) { DuaAgentLiveSitesScreen() }

@Preview(name = "Agent 12 · Mail — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent12MailLight() = DuaGenAITheme(darkTheme = false) { DuaAgentMailScreen() }

@Preview(name = "Agent 12 · Mail — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent12MailDark() = DuaGenAITheme(darkTheme = true) { DuaAgentMailScreen() }

@Preview(name = "Agent 13 · New Group — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent13NewGroupLight() = DuaGenAITheme(darkTheme = false) { DuaAgentNewGroupScreen() }

@Preview(name = "Agent 13 · New Group — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Agent13NewGroupDark() = DuaGenAITheme(darkTheme = true) { DuaAgentNewGroupScreen() }

@Preview(name = "AgentChatHeaderPill · short — light", showBackground = true, widthDp = 390)
@Composable
private fun AgentChatHeaderPillShortLightPreview() = DuaGenAITheme(darkTheme = false) {
    AgentChatHeaderPill(
        agentId = "agent_dua",
        name = "Dua",
        onBack = {},
        onOpenProfile = {},
        onInvite = {},
    )
}

@Preview(name = "AgentChatHeaderPill · long — dark", showBackground = true, widthDp = 390)
@Composable
private fun AgentChatHeaderPillLongDarkPreview() = DuaGenAITheme(darkTheme = true) {
    AgentChatHeaderPill(
        agentId = "agent_dua",
        name = "Dua Gen AI Enterprise Assistant",
        onBack = {},
        onOpenProfile = {},
        onInvite = {},
    )
}
