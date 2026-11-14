package ai.duagenai.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
 *  `.type-display` and `.type-title`. The zip's Lato has 300 / 400 / 700 only,
 *  so no role uses 500, 600 or 900. The zip's label (700 13/18) and
 *  body-strong (700 16/24) have no M3 slot here and are not mapped.
 *
 *  Official SIL OFL Lato Regular (400) and Bold (700) ship in
 *  `documents/M3-design_v01/res/font/` (`lato_regular.ttf`, `lato_bold.ttf`),
 *  with the license at `documents/M3-design_v01/licenses/lato-OFL.txt`.
 *  The HTML preview uses the same files as woff2 in
 *  `webpages/M3-design_v01/fonts/`. This package is not a Gradle app, so it
 *  has no generated `R.font`. The scale is built from a [FontFamily]
 *  argument: copy the TTFs into the app's `res/font/` and pass Lato, e.g.
 *
 *      val LatoFontFamily = FontFamily(
 *          Font(R.font.lato_regular, FontWeight.Normal),
 *          Font(R.font.lato_bold, FontWeight.Bold),
 *      )
 *      MaterialTheme(typography = teamilyTypography(LatoFontFamily)) { ... }
 *
 *  Until that is wired, [TeamilyTypography] uses [FontFamily.Default]
 *  with the exact sizes and weights below.
 * ==========================================================================*/

/** `--nv-tracking-tight` -0.01em. */
private val TrackingTight = (-0.01).em

/** Code blocks use the system monospace family. */
val TeamilyMonoFontFamily: FontFamily = FontFamily.Monospace

/** The spec's M3 type scale for the given sans family (Lato in the app). */
fun teamilyTypography(sans: FontFamily = FontFamily.Default): Typography = Typography(
    // --nv-type-display + --nv-tracking-tight
    headlineMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = TrackingTight),
    // --nv-type-title + --nv-tracking-tight
    titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = TrackingTight),
    // --nv-type-heading
    titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp),
    // --nv-type-body
    bodyLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    // --nv-type-body-sm
    bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    // --nv-type-caption
    labelMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
)

/** Kit typography with the platform default family; see the header for Lato. */
val TeamilyTypography: Typography = teamilyTypography()
