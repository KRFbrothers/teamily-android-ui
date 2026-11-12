package ai.duagenai.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/* ============================================================================
 *  Teamily-grade Forest-Slate glass tokens (Dua Gen AI v01)
 *
 *  The ONLY place the forest-slate polish colours live. Screens read them via
 *  [teamilyPalette] and never declare their own `Color(0x...)`.
 *
 *  Values are the same `TeamilyLightPalette` / `TeamilyDarkPalette` the Dua-ai
 *  app ships (apps/android/.../ui/chat/TeamilyTokens.kt), so the design package
 *  and the live app cannot drift. Dark values match the polish spec exactly:
 *    bgCanvasGradient #1E2522 -> #1D2220 -> #171C19
 *    borderGradient   #AEBBB15C -> #4C564F
 *    cardHeroColor #253D31 · logoMint #43D982 · bottomnavBg #1F2521
 *    navSelected #545B56 · plusFill #CBE7D2 · plusIcon #17201B
 *    bubbleUser #253D31 · bubbleAgent #272E2B · border #323B36
 *    composerBg #323B36 · brandGradient #0B8249 -> #087C45
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
    val chromeFill: Color,
    val plusFill: Color,
    val plusIcon: Color,
    val navSelected: Color,
    val navSelectedContent: Color,
    val iconIdle: Color,
    val accent: Color,
    val accentHover: Color,
    val accentSoft: Color,
    val accentContrast: Color,
    val logoMint: Color,
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
    // Decompiled 176deg: linear-gradient(176deg, rgba(47,57,50,0.74) 0%, rgba(35,44,38,0.64) 48%, rgba(27,35,30,0.58) 100%)
    surfaceGradient = Brush.verticalGradient(
        listOf(
            Color(0xBC2F3932),
            Color(0xA3232C26),
            Color(0x941B231E),
        ),
    ),
    // Live measured agent bubble / elevated surface: #272E2B (R=39, G=46, B=43)
    bgElevated = Color(0xFF272E2B),
    // Decompiled 180deg: linear-gradient(180deg, rgba(41,50,44,0.8), rgba(29,37,32,0.68))
    cardGradient = Brush.verticalGradient(
        listOf(
            Color(0xCC29322C),
            Color(0xAD1D2520),
        ),
    ),
    // Decompiled 145deg: linear-gradient(145deg, #aebbb15c, #4b5750)
    borderGradient = Brush.linearGradient(
        listOf(
            Color(0x5CAEBBB1),
            Color(0xFF4C564F),
        ),
    ),
    // Live measured hero pinned card: #253D31 (R=37, G=61, B=49)
    cardHeroColor = Color(0xFF253D31),
    // Live measured recessed search field: #181D1B (R=24, G=29, B=27)
    surfaceRecessed = Color(0xFF181D1B),
    statusbarBg = Color(0xFF1D2220),
    topbarBg = Color(0xFF1D2220),
    // Same fill as the round icon buttons (chromeFill).
    composerBg = Color(0xFF323B36),
    bottomnavBg = Color(0xFF1F2521),
    // Live measured primary text: #F7F9F7 (R=247, G=249, B=247)
    textPrimary = Color(0xFFF7F9F7),
    // Section labels on the home list, sampled 8 Nov 2026: #B4BFB8
    textSecondary = Color(0xFFB4BFB8),
    // Live measured borders and the filter circle fill: #323B36
    border = Color(0xFF323B36),
    chromeFill = Color(0xFF323B36),
    // Header add button. Fill #CBE7D2, glyph #17201B.
    plusFill = Color(0xFFCBE7D2),
    plusIcon = Color(0xFF17201B),
    // Selected tab bubble #545B56, label #F7F9F7. Idle icons #EDF1ED.
    navSelected = Color(0xFF545B56),
    navSelectedContent = Color(0xFFF7F9F7),
    iconIdle = Color(0xFFEDF1ED),
    accent = Color(0xFF087C45),
    accentHover = Color(0xFF0B8249),
    accentSoft = Color(0xFFCBE7D2),
    accentContrast = Color(0xFFECFFEF),
    // Logo mark on the home header, sampled 8 Nov 2026: #43D982
    logoMint = Color(0xFF43D982),
    gradientStart = Color(0xFF0B8249),
    gradientEnd = Color(0xFF087C45),
    // Decompiled 180deg: linear-gradient(180deg, #0b8249 0%, #087c45 100%)
    brandGradient = Brush.verticalGradient(
        listOf(Color(0xFF0B8249), Color(0xFF087C45)),
    ),
    // Live measured chat bubbles
    bubbleUser = Color(0xFF253D31),
    bubbleUserText = Color(0xFFF7F9F7),
    bubbleAgent = Color(0xFF272E2B),
    bubbleAgentText = Color(0xFFF7F9F7),
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
            Color(0x7AECEFEF),
        ),
    ),
    bgElevated = Color(0xFFFFFFFF),
    cardGradient = Brush.verticalGradient(
        listOf(
            Color(0xBDFAFCFB),
            Color(0x94F0F5F3),
        ),
    ),
    borderGradient = Brush.linearGradient(
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFFE8EDE9),
            Color(0xFFCFD9D2),
        ),
    ),
    cardHeroColor = Color(0xFFDDF3E5),
    surfaceRecessed = Color(0xFFF2F4F3),
    statusbarBg = Color(0xFFFFFFFF),
    topbarBg = Color(0xFFFFFFFF),
    // Same fill as the round icon buttons (chromeFill).
    composerBg = Color(0xFFEEF2EF),
    bottomnavBg = Color(0xFFF8FAF8),
    textPrimary = Color(0xFF14201A),
    textSecondary = Color(0xFF5C6D63),
    border = Color(0xFFE1E7E2),
    chromeFill = Color(0xFFEEF2EF),
    // Light add button is solid green #0A8048 with a white glyph.
    plusFill = Color(0xFF0A8048),
    plusIcon = Color(0xFFFFFFFF),
    // Selected tab is the same pale green as the pinned card. Label is #066A3B.
    navSelected = Color(0xFFDDF3E5),
    navSelectedContent = Color(0xFF066A3B),
    iconIdle = Color(0xFF26372E),
    accent = Color(0xFF087C45),
    accentHover = Color(0xFF0B8249),
    accentSoft = Color(0xFFE4F3E9),
    accentContrast = Color(0xFFFFFFFF),
    logoMint = Color(0xFF43D982),
    gradientStart = Color(0xFF0B8249),
    gradientEnd = Color(0xFF087C45),
    brandGradient = Brush.verticalGradient(
        listOf(Color(0xFF0B8249), Color(0xFF087C45)),
    ),
    bubbleUser = Color(0xFFDDF3E5),
    bubbleUserText = Color(0xFF14201A),
    bubbleAgent = Color(0xFFF0F2F0),
    bubbleAgentText = Color(0xFF14201A),
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
)

/** Corner radii used by the forest-slate polish. */
object TeamilyShapes {
    /** Pinned agent, Memory and Artifact cards. */
    val card = RoundedCornerShape(20.dp)
    /** User chat bubble. */
    val userBubble = RoundedCornerShape(20.dp)
    /** Bottom composer pill. */
    val composer = RoundedCornerShape(28.dp)
    /** Floating bottom navigation dock and its selected pill. */
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
