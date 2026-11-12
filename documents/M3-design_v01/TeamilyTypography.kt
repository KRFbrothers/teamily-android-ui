package ai.duagenai.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ============================================================================
 *  Teamily type scale — `--font-sans: var(--font-lato)` and the `--text-*`
 *  scale of the Teamily 1:1 match spec (section 9).
 *
 *  This design package is not a Gradle app, so it has no `res/font/` and no
 *  `R.font.lato_*`. The scale is therefore built from a [FontFamily] argument:
 *  the app passes its Lato family, e.g.
 *
 *      val LatoFontFamily = FontFamily(
 *          Font(R.font.lato_regular, FontWeight.Normal),
 *          Font(R.font.lato_bold, FontWeight.Bold),
 *          Font(R.font.lato_black, FontWeight.Black),
 *      )
 *      MaterialTheme(typography = teamilyTypography(LatoFontFamily)) { ... }
 *
 *  The reference Lato files are the woff2 in webpages/M3-design_v01/fonts/
 *  (400 / 700 / 900). Until the app wires R.font, [TeamilyTypography] uses
 *  [FontFamily.Default] with the exact sizes and weights below.
 * ==========================================================================*/

/** Code blocks use the system monospace family. */
val TeamilyMonoFontFamily: FontFamily = FontFamily.Monospace

/** The spec's M3 type scale for the given sans family (Lato in the app). */
fun teamilyTypography(sans: FontFamily = FontFamily.Default): Typography = Typography(
    // --text-2xl
    headlineMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    // --text-xl
    titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    // --text-lg
    titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 28.sp),
    // --text-base
    bodyLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    // --text-sm
    bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    // --text-xs
    labelMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

/** Kit typography with the platform default family; see the header for Lato. */
val TeamilyTypography: Typography = teamilyTypography()
