package ai.duagenai.design.theme

import ai.duagenai.design.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/* ============================================================================
 *  Teamily type scale — `--font-sans: var(--font-lato)` and the mobile
 *  (`width <= 767px`) `--nv-type-*` scale of teamily.zip (styles-T3LtfMsY.css):
 *
 *    display   700 24/32  -0.01em  -> headlineMedium
 *    title     700 20/28  -0.01em  -> titleLarge
 *    heading   700 18/26           -> titleMedium
 *    body      400 16/26           -> bodyLarge
 *    body-sm   400 14/22           -> bodyMedium
 *    caption   400 12/16           -> labelMedium
 *
 *  `--nv-tracking-tight` (-0.01em) is applied only where the zip applies it:
 *  `.type-display` and `.type-title`. Sizes stay this screen scale.
 *  v4 also ships Lato Medium (500) and Black (900) in `res/font`. The screen
 *  scale below stays 400/700 so existing rows keep the same width and height.
 *
 *  Official SIL OFL Lato faces live in `app/src/main/res/font/`:
 *  `lato_regular.ttf`, `lato_medium.ttf`, `lato_bold.ttf`, `lato_black.ttf`.
 *  [TeamilySansFontFamily] loads them. [teamilyTypography] and
 *  [TeamilyTypography] use that family; every M3 role is defined so
 *  unset slots cannot fall back to the platform default (or to weight 500).
 *  Sizes and weights of the six mapped roles stay the scale above.
 * ==========================================================================*/

/** `--nv-tracking-tight` -0.01em. */
private val TrackingTight = (-0.01).em

/** Lato 400 / 500 / 700 / 900 from `res/font`. */
val TeamilySansFontFamily: FontFamily = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_medium, FontWeight.Medium),
    Font(R.font.lato_bold, FontWeight.Bold),
    Font(R.font.lato_black, FontWeight.Black),
)

/** Code blocks use the system monospace family. */
val TeamilyMonoFontFamily: FontFamily = FontFamily.Monospace

/**
 * Lato's ascender/descender metrics put extra font padding above the glyphs on
 * Android, so text sits visually low inside chips, pills and buttons. Every
 * slot below starts from this base: no font padding, glyphs centred in the
 * full line height (nothing trimmed).
 */
private val TeamilyBaseTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

/** The spec's M3 type scale for the given sans family (Lato in the app). */
fun teamilyTypography(sans: FontFamily = TeamilySansFontFamily): Typography = Typography(
    // Sizes stay the screen scale above. Medium (500) is the real Lato face.
    displayLarge = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    // --nv-type-display + --nv-tracking-tight
    headlineMedium = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = TrackingTight),
    headlineSmall = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    // --nv-type-title + --nv-tracking-tight
    titleLarge = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = TrackingTight),
    // --nv-type-heading
    titleMedium = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp),
    titleSmall = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    // --nv-type-body
    bodyLarge = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    // --nv-type-body-sm
    bodyMedium = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    // --nv-type-caption
    labelMedium = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TeamilyBaseTextStyle.copy(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

/** Kit typography with Lato 400 / 500 / 700 / 900. */
val TeamilyTypography: Typography = teamilyTypography()
