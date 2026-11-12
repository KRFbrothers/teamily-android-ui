package ai.duagenai.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================================================
 *  Teamily-grade Forest-Slate glass tokens (Dua Gen AI v01)
 *
 *  The ONLY place the forest-slate polish colours live. Screens read them via
 *  [teamilyPalette] and never declare their own `Color(0x...)`.
 *
 *  Values follow the Teamily 1:1 match spec, whose source of truth is the
 *  `--nv-*` tokens of teamily.zip (styles-T3LtfMsY.css):
 *    brand   logoMint #42D982 · signalGreen #00D95A · accent #087C45
 *            accentHover #118A51 · accentPressed #066A3B · accentSoft #EEF8F2
 *            brandGradient #118A51 -> #087C45
 *    light   bgCanvas #FFFFFF + atmosphere #E8F3EC (ellipse at 8% 92%)
 *            bgElevated #F6F8F5 · chromeFill/composerBg #EEF2EF
 *            border #DDE6E0 · borderStrong #C9D5CD · textPrimary #182B23
 *    dark    bgCanvasGradient #1E2522 -> #1D2220 -> #171C19
 *            bgSurface/bottomnavBg #1F2521 · composerBg #29322C
 *            border / rim #D1DAD3 @ 0x38 · borderGradient -> #4C564F
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
    /** Light-mode `--nv-atmosphere-deep` radial glow; null in dark (gradient canvas). */
    val atmosphereDeep: Color?,
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
    bgCanvasGradient = Brush.verticalGradient(
        listOf(Color(0xFF1E2522), Color(0xFF1D2220), Color(0xFF171C19)),
    ),
    // Live measured surface & dock: #1F2521 (R=31, G=37, B=33)
    bgSurface = Color(0xFF1F2521),
    // --nv-glass-2-surface dark
    surfaceGradient = Brush.verticalGradient(
        listOf(
            Color(0xBD2F3932),
            Color(0xA3232C26),
            Color(0x941B231E),
        ),
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
    // --nv-glass-rim dark: #d1dad338 -> #4C564F
    borderGradient = Brush.linearGradient(
        listOf(
            Color(0x38D1DAD3),
            Color(0xFF4C564F),
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
    accentHover = Color(0xFF118A51),
    accentPressed = Color(0xFF066A3B),
    accentSoft = Color(0xFFEEF8F2),
    accentContrast = Color(0xFFECFFEF),
    // --nv-brand-product / --nv-signal
    logoMint = Color(0xFF42D982),
    signalGreen = Color(0xFF00D95A),
    gradientStart = Color(0xFF118A51),
    gradientEnd = Color(0xFF087C45),
    // --nv-green-600 -> --nv-green-700
    brandGradient = Brush.verticalGradient(
        listOf(Color(0xFF118A51), Color(0xFF087C45)),
    ),
    // --nv-user-bubble / --nv-agent-bubble
    bubbleUser = Color(0xFF505752),
    bubbleUserText = Color(0xFFFFFFFF),
    bubbleAgent = Color(0xFF303433),
    bubbleAgentText = Color(0xFFEAF0EC),
    sendButton = Color(0xFF235039),
    sendButtonIcon = Color(0xFFECFFEF),
    // Blank chat page. Right side stays #171C19. Lower left lifts to #222926.
    chatCanvas = Color(0xFF171C19),
    chatGlow = Color(0xFF222926),
    danger = Color(0xFFF2B8B5),
    dangerSoft = Color(0xFF4A2320),
    dangerInk = Color(0xFFF2B8B5),
    primarySoft = Color(0xFF235039),
    primaryInk = Color(0xFFA6E6B9),
    fieldEdge = Color(0xFF7C9083),
    tileCyan = Color(0xFF3EC0EA),
    tileYellow = Color(0xFFF6C945),
    tileViolet = Color(0xFF9A7BFF),
    tileMint = Color(0xFF9BE3B8),
    tileInk = Color(0xFF17201B),
    atmosphereDeep = null,
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
        listOf(
            Color(0xB8FFFFFF),
            Color(0x8AF7FBF9),
            Color(0x7AECF3EF),
        ),
    ),
    bgElevated = Color(0xFFF6F8F5),
    cardGradient = Brush.verticalGradient(
        listOf(
            Color(0xBDFAFCFB),
            Color(0x94F0F5F3),
        ),
    ),
    borderGradient = Brush.linearGradient(
        listOf(
            Color(0xCCFFFFFF),
            Color(0xFFE8EDE9),
            Color(0x14284735),
        ),
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
    gradientStart = Color(0xFF118A51),
    gradientEnd = Color(0xFF087C45),
    brandGradient = Brush.verticalGradient(
        listOf(Color(0xFF118A51), Color(0xFF087C45)),
    ),
    bubbleUser = Color(0xFFDDF3E5),
    bubbleUserText = Color(0xFF182B23),
    bubbleAgent = Color(0xFFF0F2F0),
    bubbleAgentText = Color(0xFF182B23),
    sendButton = Color(0xFF087C45),
    sendButtonIcon = Color(0xFFFFFFFF),
    chatCanvas = Color(0xFFFFFFFF),
    chatGlow = Color(0xFFFFFFFF),
    danger = Color(0xFFA4271E),
    dangerSoft = Color(0xFFF7DCD9),
    dangerInk = Color(0xFFA4271E),
    primarySoft = Color(0xFFDDF3E5),
    primaryInk = Color(0xFF066A3B),
    fieldEdge = Color(0xFF788C7E),
    tileCyan = Color(0xFF3EC0EA),
    tileYellow = Color(0xFFF6C945),
    tileViolet = Color(0xFF9A7BFF),
    tileMint = Color(0xFF9BE3B8),
    tileInk = Color(0xFF17201B),
    // --nv-atmosphere-deep
    atmosphereDeep = Color(0xFFE8F3EC),
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
    /** `--nv-radius-xl` 20px — user chat bubble. */
    val userBubble = RoundedCornerShape(20.dp)
    /** `--nv-radius-2xl` 24px — bottom sheets. */
    val sheet = RoundedCornerShape(24.dp)
    /** Bottom composer pill, 28dp. */
    val composer = RoundedCornerShape(28.dp)
    /** `--nv-radius-3xl` 32px. */
    val hero = RoundedCornerShape(32.dp)
    /** `--nv-radius-pill` — floating bottom navigation dock and its selected pill. */
    val capsule = RoundedCornerShape(999.dp)
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
