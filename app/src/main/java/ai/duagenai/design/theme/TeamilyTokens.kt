package ai.duagenai.design.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================================================
 *  Teamily-grade Forest-Slate glass tokens (Dua Gen AI v01)
 *
 *  The ONLY place the forest-slate polish colours live. Screens read them via
 *  [teamilyPalette] and never declare their own `Color(0x...)`.
 *
 *  Source of truth: the `--nv-*` tokens of teamily.zip (styles-T3LtfMsY.css,
 *  `:root[data-ui="2.0"]` and `.dark`). Where the earlier pasted 1:1 spec
 *  disagreed with the zip, the zip value is used.
 *    brand   logoMint #42D982 · signalGreen #00D95A · accent #087C45
 *            accentHover #118A51 · accentPressed #066A3B · accentSoft #EEF8F2
 *            brandGradient (--nv-primary-fill) #0B8249 -> #087C45
 *    light   atmosphere: #E8F3EC ellipse at 8% 92%, transparent at 52%, over #FAFBFA
 *            bgElevated #F6F8F5 · chromeFill/composerBg #EEF2EF
 *            border #DDE6E0 · borderStrong #C9D5CD · textPrimary #182B23
 *            borderGradient (--nv-edge) 145deg #FFF 0% / #E8EDE9 35% / #CFD9D2 100%
 *    dark    atmosphere: #242B27 ellipse at 8% 92%, transparent at 48%, over #171C19
 *            bgSurface/bottomnavBg #1F2521 · composerBg #29322C
 *            border / rim #D1DAD3 @ 0x38 · borderGradient (--nv-edge) #AEBBB1 @ 0x5C -> #4B5750
 *            primary fill #CBE7D2 with #17201B ink, no shadow
 *            bubbleUser #505752 · bubbleAgent #303433
 *
 *  Compose has no backdrop blur, so the "frosted" dock is the opaque-ish
 *  `bottomnavBg` fill plus a 1dp `borderGradient` edge — no faked blur.
 * ==========================================================================*/

data class TeamilyPalette(
    val bgCanvas: Color,
    val bgCanvasTop: Color,
    val bgCanvasBottom: Color,
    val bgCanvasGradient: Brush,
    val bgSurface: Color,
    val surfaceGradient: Brush,
    val bgElevated: Color,
    val cardGradient: Brush,
    val borderGradient: Brush,
    val cardHeroColor: Color,
    val surfaceRecessed: Color,
    val statusbarBg: Color,
    val topbarBg: Color,
    val composerBg: Color,
    val bottomnavBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    /** Light `--nv-neutral-300`. The spec gives no dark value, so dark is null. */
    val borderStrong: Color?,
    val chromeFill: Color,
    val plusFill: Color,
    val plusIcon: Color,
    val navSelected: Color,
    val navSelectedContent: Color,
    val iconIdle: Color,
    val accent: Color,
    val accentHover: Color,
    val accentPressed: Color,
    val accentSoft: Color,
    val accentContrast: Color,
    val logoMint: Color,
    val signalGreen: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val brandGradient: Brush,
    val bubbleUser: Color,
    val bubbleUserText: Color,
    val bubbleAgent: Color,
    val bubbleAgentText: Color,
    val sendButton: Color,
    val sendButtonIcon: Color,
    val chatCanvas: Color,
    val chatGlow: Color,
    val danger: Color,
    val dangerSoft: Color,
    val dangerInk: Color,
    val primarySoft: Color,
    val primaryInk: Color,
    val fieldEdge: Color,
    val tileCyan: Color,
    val tileYellow: Color,
    val tileViolet: Color,
    val tileMint: Color,
    val tileInk: Color,
    /** `--nv-atmosphere-deep`: the radial glow colour at 8% / 92%. */
    val atmosphereDeep: Color,
    /** The solid colour under the glow in `--nv-atmosphere`. */
    val atmosphereBase: Color,
    /** Where the glow reaches transparent, as a fraction of the CSS farthest-corner ellipse. */
    val atmosphereStop: Float,
    /** Active Skills / MCP Tools glyph in the composer. Light `--nv-green-700`; dark keeps logoMint. */
    val toolActive: Color,
    /** Unread dot on the dock's Memory tab (measured on agent-home-reference.jpg: #A2E4B6 = dark `--nv-green-700`). */
    val notificationDot: Color,
    /** `--nv-notification-outline`: the ring around [notificationDot]. */
    val notificationOutline: Color,
    /** `--nv-shadow-resting` for glass cards. */
    val restingShadowElevation: Dp,
    val restingShadowAmbient: Color,
    val restingShadowSpot: Color,
    /** `--nv-primary-shadow-hover` green glow; Light mode only, null in dark. */
    val primaryGlowAmbient: Color?,
    val primaryGlowSpot: Color?,
)

val TeamilyDarkPalette = TeamilyPalette(
    // Live measured on realme device: #1D2220 (R=29, G=34, B=32) rich slate-green charcoal
    bgCanvas = Color(0xFF1D2220),
    bgCanvasTop = Color(0xFF1E2522),
    bgCanvasBottom = Color(0xFF171C19),
    // Device-measured vertical canvas. Not in the zip and no longer drawn by
    // `teamilyCanvas`, which now paints the zip `--nv-atmosphere` (see below).
    bgCanvasGradient = Brush.verticalGradient(
        listOf(Color(0xFF1E2522), Color(0xFF1D2220), Color(0xFF171C19)),
    ),
    // Live measured surface & dock: #1F2521 (R=31, G=37, B=33)
    bgSurface = Color(0xFF1F2521),
    // --nv-glass-2-surface dark
    surfaceGradient = Brush.verticalGradient(
        0f to Color(0xBD2F3932),
        0.48f to Color(0xA3232C26),
        1f to Color(0x941B231E),
    ),
    // Elevated surface (dark).
    bgElevated = Color(0xFF272E2B),
    // --nv-glass-surface dark
    cardGradient = Brush.verticalGradient(
        listOf(
            Color(0xCC29322C),
            Color(0xAD1D2520),
        ),
    ),
    // --nv-edge dark: linear-gradient(145deg, #aebbb15c, #4b5750)
    borderGradient = Brush.linearGradient(
        listOf(
            Color(0x5CAEBBB1),
            Color(0xFF4B5750),
        ),
    ),
    // Hero pinned card fill (dark).
    cardHeroColor = Color(0xFF253D31),
    // Live measured recessed search field: #181D1B (R=24, G=29, B=27)
    surfaceRecessed = Color(0xFF181D1B),
    statusbarBg = Color(0xFF1D2220),
    topbarBg = Color(0xFF1D2220),
    // Top of `--nv-glass-surface` dark (spec allows solid #323B36 only as an elevated fallback).
    composerBg = Color(0xFF29322C),
    bottomnavBg = Color(0xFF1F2521),
    // Live measured primary text: #F7F9F7 (R=247, G=249, B=247)
    textPrimary = Color(0xFFF7F9F7),
    // Section labels on the home list, sampled 8 Nov 2026: #B4BFB8
    textSecondary = Color(0xFFB4BFB8),
    // --nv-glass-rim dark: #d1dad338
    border = Color(0x38D1DAD3),
    borderStrong = null,
    // Filter circle / round icon button fill.
    chromeFill = Color(0xFF323B36),
    // Header add button. Fill #CBE7D2, glyph #17201B.
    plusFill = Color(0xFFCBE7D2),
    plusIcon = Color(0xFF17201B),
    // Selected tab bubble #545B56, label #F7F9F7. Idle icons #EDF1ED.
    navSelected = Color(0xFF545B56),
    navSelectedContent = Color(0xFFF7F9F7),
    iconIdle = Color(0xFFEDF1ED),
    accent = Color(0xFF087C45),
    // Dark --nv-green-600 / -800 / -50 (same ramp steps as the light values).
    accentHover = Color(0xFF91D9A8),
    accentPressed = Color(0xFFC0F0CD),
    accentSoft = Color(0xFF183826),
    // Dark --action-primary-text.
    accentContrast = Color(0xFF17201B),
    // --nv-brand-product / --nv-signal
    logoMint = Color(0xFF42D982),
    signalGreen = Color(0xFF00D95A),
    // Dark --nv-primary-fill is a flat #cbe7d2 (no gradient, --nv-primary-shadow none).
    gradientStart = Color(0xFFCBE7D2),
    gradientEnd = Color(0xFFCBE7D2),
    brandGradient = SolidColor(Color(0xFFCBE7D2)),
    // --nv-user-bubble / --nv-agent-bubble
    bubbleUser = Color(0xFF505752),
    bubbleUserText = Color(0xFFFFFFFF),
    bubbleAgent = Color(0xFF303433),
    bubbleAgentText = Color(0xFFEAF0EC),
    sendButton = Color(0xFFCBE7D2),
    sendButtonIcon = Color(0xFF17201B),
    // Blank chat page. Right side stays #171C19. Lower left lifts to #222926.
    chatCanvas = Color(0xFF171C19),
    chatGlow = Color(0xFF222926),
    // Dark --nv-error-600 / -50 / -700.
    danger = Color(0xFFF4A59C),
    dangerSoft = Color(0xFF472925),
    dangerInk = Color(0xFFE78E83),
    primarySoft = Color(0xFF235039),
    // Dark --nv-green-700.
    primaryInk = Color(0xFFA2E4B6),
    // Dark --nv-field-edge.
    fieldEdge = Color(0xFFAEBBB1),
    tileCyan = Color(0xFF3EC0EA),
    tileYellow = Color(0xFFF6C945),
    tileViolet = Color(0xFF9A7BFF),
    tileMint = Color(0xFF9BE3B8),
    tileInk = Color(0xFF17201B),
    // --nv-atmosphere dark: radial-gradient(ellipse at 8% 92%, #242b27 0, transparent 48%), #171c19
    atmosphereDeep = Color(0xFF242B27),
    atmosphereBase = Color(0xFF171C19),
    atmosphereStop = 0.48f,
    toolActive = Color(0xFF42D982),
    notificationDot = Color(0xFFA2E4B6),
    // Dark --nv-notification-outline = --nv-canvas.
    notificationOutline = Color(0xFF141816),
    restingShadowElevation = 6.dp,
    restingShadowAmbient = Color(0x55000000),
    restingShadowSpot = Color(0x88000000),
    primaryGlowAmbient = null,
    primaryGlowSpot = null,
)

val TeamilyLightPalette = TeamilyPalette(
    bgCanvas = Color(0xFFFFFFFF),
    bgCanvasTop = Color(0xFFFFFFFF),
    bgCanvasBottom = Color(0xFFFFFFFF),
    bgCanvasGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF)),
    ),
    bgSurface = Color(0xFFFFFFFF),
    surfaceGradient = Brush.verticalGradient(
        0f to Color(0xB8FFFFFF),
        0.48f to Color(0x8AF7FBF9),
        1f to Color(0x7AECF3EF),
    ),
    bgElevated = Color(0xFFF6F8F5),
    cardGradient = Brush.verticalGradient(
        listOf(
            Color(0xBDFAFCFB),
            Color(0x94F0F5F3),
        ),
    ),
    // --nv-edge: linear-gradient(145deg, #fff 0%, #e8ede9 35%, #cfd9d2 100%)
    borderGradient = Brush.linearGradient(
        0f to Color(0xFFFFFFFF),
        0.35f to Color(0xFFE8EDE9),
        1f to Color(0xFFCFD9D2),
    ),
    cardHeroColor = Color(0xFFDDF3E5),
    surfaceRecessed = Color(0xFFF2F4F3),
    statusbarBg = Color(0xFFFFFFFF),
    topbarBg = Color(0xFFFFFFFF),
    // Same fill as the round icon buttons (chromeFill).
    composerBg = Color(0xFFEEF2EF),
    bottomnavBg = Color(0xFFF8FAF8),
    textPrimary = Color(0xFF182B23),
    textSecondary = Color(0xFF5C6D63),
    border = Color(0xFFDDE6E0),
    borderStrong = Color(0xFFC9D5CD),
    chromeFill = Color(0xFFEEF2EF),
    // Light add button is solid green #0A8048 with a white glyph.
    plusFill = Color(0xFF0A8048),
    plusIcon = Color(0xFFFFFFFF),
    // Selected tab is the same pale green as the pinned card. Label is #066A3B.
    navSelected = Color(0xFFDDF3E5),
    navSelectedContent = Color(0xFF066A3B),
    iconIdle = Color(0xFF26372E),
    accent = Color(0xFF087C45),
    accentHover = Color(0xFF118A51),
    accentPressed = Color(0xFF066A3B),
    accentSoft = Color(0xFFEEF8F2),
    accentContrast = Color(0xFFFFFFFF),
    logoMint = Color(0xFF42D982),
    signalGreen = Color(0xFF00D95A),
    // --nv-primary-fill: linear-gradient(180deg, #0b8249 0%, #087c45 100%)
    gradientStart = Color(0xFF0B8249),
    gradientEnd = Color(0xFF087C45),
    brandGradient = Brush.verticalGradient(
        listOf(Color(0xFF0B8249), Color(0xFF087C45)),
    ),
    bubbleUser = Color(0xFFDDF3E5),
    bubbleUserText = Color(0xFF182B23),
    bubbleAgent = Color(0xFFF0F2F0),
    bubbleAgentText = Color(0xFF182B23),
    sendButton = Color(0xFF087C45),
    sendButtonIcon = Color(0xFFFFFFFF),
    chatCanvas = Color(0xFFFFFFFF),
    chatGlow = Color(0xFFFFFFFF),
    // --nv-error-600 / -50 / -700.
    danger = Color(0xFFC93434),
    dangerSoft = Color(0xFFFDEEEE),
    dangerInk = Color(0xFFB12C2C),
    primarySoft = Color(0xFFDDF3E5),
    primaryInk = Color(0xFF066A3B),
    // --nv-field-edge = --nv-neutral-500.
    fieldEdge = Color(0xFF5C6D63),
    tileCyan = Color(0xFF3EC0EA),
    tileYellow = Color(0xFFF6C945),
    tileViolet = Color(0xFF9A7BFF),
    tileMint = Color(0xFF9BE3B8),
    tileInk = Color(0xFF17201B),
    // --nv-atmosphere: radial-gradient(ellipse at 8% 92%, #e8f3ec 0, transparent 52%), #fafbfa
    atmosphereDeep = Color(0xFFE8F3EC),
    atmosphereBase = Color(0xFFFAFBFA),
    atmosphereStop = 0.52f,
    // --nv-green-700 (light --text-brand / active ink); #42D982 on #EEF2EF is 1.62:1.
    toolActive = Color(0xFF087C45),
    // Light --nv-green-700, the same ramp step as the measured dark dot.
    notificationDot = Color(0xFF087C45),
    // --nv-notification-outline = --nv-neutral-0.
    notificationOutline = Color(0xFFFFFFFF),
    restingShadowElevation = 4.dp,
    restingShadowAmbient = Color(0x0A284735),
    restingShadowSpot = Color(0x1F284735),
    // --nv-primary-shadow-hover: #06482a33 ambient, #087c4566 spot
    primaryGlowAmbient = Color(0x3306482A),
    primaryGlowSpot = Color(0x66087C45),
)

/** Corner radii — `--nv-radius-*`. */
object TeamilyShapes {
    /** `--nv-radius-xs` 8px. */
    val xs = RoundedCornerShape(8.dp)
    /** `--nv-radius-md` 12px. */
    val md = RoundedCornerShape(12.dp)
    /** `--nv-radius-lg` 16px. */
    val lg = RoundedCornerShape(16.dp)
    /** `--nv-radius-xl` 20px — pinned agent, Memory and Artifact cards. */
    val card = RoundedCornerShape(20.dp)
    /** `--nv-radius-xl` 20px — user chat bubble, with the `--kit-radius-tail`
     *  6px corner at the bottom end (beside the user's avatar). */
    val userBubble = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 6.dp, bottomStart = 20.dp)
    /** `--nv-radius-2xl` 24px — bottom sheets. */
    val sheet = RoundedCornerShape(24.dp)
    /** `--kit-radius-composer` = `--nv-radius-3xl` 32px. */
    val composer = RoundedCornerShape(32.dp)
    /** `--nv-radius-3xl` 32px. */
    val hero = RoundedCornerShape(32.dp)
    /** `--nv-radius-pill` — floating bottom navigation dock and its selected pill. */
    val capsule = RoundedCornerShape(999.dp)
}

/** Icon strokes — `--nv-icon-stroke` 1.75 and `--nv-icon-stroke-strong` 2.25 (active / current). */
object TeamilyStroke {
    val icon: Dp = 1.75.dp
    val iconStrong: Dp = 2.25.dp
}

/**
 * Motion — `--nv-duration-*`, `--nv-ease-*` and `--nv-spring-standard-ui-*`.
 * Spring: mass 1, stiffness 300, damping 32, i.e. damping ratio
 * 32 / (2 * sqrt(300 * 1)) = 0.9238.
 */
object TeamilyMotion {
    const val durationExtraSmall = 70
    const val durationSmall = 140
    const val durationBase = 240
    const val durationLarge = 320
    const val durationExtraLarge = 480
    /** `--nv-ease-enter` cubic-bezier(.2,.2,.2,1). */
    val easeEnter = CubicBezierEasing(0.2f, 0.2f, 0.2f, 1f)
    /** `--nv-ease-change` cubic-bezier(.4,.1,.2,1). */
    val easeChange = CubicBezierEasing(0.4f, 0.1f, 0.2f, 1f)
    /** `--nv-ease-exit` cubic-bezier(.6,.1,.7,.7). */
    val easeExit = CubicBezierEasing(0.6f, 0.1f, 0.7f, 0.7f)
    const val springStiffness = 300f
    const val springDampingRatio = 0.9238f
    fun <T> standardSpring(): SpringSpec<T> =
        spring(dampingRatio = springDampingRatio, stiffness = springStiffness)
}

/**
 * The palette for the theme currently in effect.
 *
 * Follows `DuaGenAITheme(darkTheme = ...)` by reading the active M3 background:
 * a dark background selects [TeamilyDarkPalette], a light one
 * [TeamilyLightPalette]. Previews and the app therefore stay in sync with the
 * existing theme switch instead of introducing a second one.
 */
@Composable
@ReadOnlyComposable
fun teamilyPalette(): TeamilyPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) TeamilyDarkPalette
    else TeamilyLightPalette
