package ai.duagenai.design.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ===========================================================================
//  DuagenAI — Color.kt
//  ---------------------------------------------------------------------------
//  YAHI EK FILE me hex literals hain. Kisi bhi composable me Color(0xFF...) mat
//  likho — hamesha MaterialTheme.colorScheme.<role> use karo, taaki light/dark
//  apne aap switch ho.
//
//  Source: DuagenAI Android design reference (brand token palette) + M3 slot
//  mapping. Contrast ratios sRGB relative luminance se verify kiye gaye hain
//  (WCAG AA: text 4.5:1, non-text/UI 3:1).
//
//  NOTE: `tertiary` original design tokens me nahi tha — teal accent derive
//  kiya gaya hai (secondary mint se alag rakhne ke liye). Baaki sab brand
//  tokens se seedha mapped hai.
//
//  Live app theme is still `DuaGenAITheme` in this same package. It reads these
//  tokens. Dark primary stays `DuagenAIGreen` (#087C45), not `DuagenAIGreenDark`,
//  because `DuaGenAINavHostTest` freezes that green in both schemes.
// ===========================================================================


// ---------------------------------------------------------------------------
//  Brand palette — raw tokens (design se)
// ---------------------------------------------------------------------------

// Primary — brand green
val DuagenAIGreen          = Color(0xFF087C45)
val DuagenAIGreenOn        = Color(0xFFFFFFFF)
val DuagenAIGreenSoft      = Color(0xFFDDF3E5)
val DuagenAIGreenInk       = Color(0xFF066A3B)

// Mint accent — sirf dots / indicators ke liye, TEXT ke liye nahi
val DuagenAIMint           = Color(0xFF42D982)
val DuagenAIMintInk        = Color(0xFF06301C)
val DuagenAIBrandSoft      = Color(0xFFCBE7D2)
val DuagenAIBrandSoftInk   = Color(0xFF0A3A22)

// Primary gradient — brand green → mint. CTA / hero surfaces ke liye.
// Dono stop already-defined brand tokens hain, koi naya hex nahi.
val DuagenAIPrimaryGradient = Brush.linearGradient(
    colors = listOf(DuagenAIGreen, DuagenAIMint),
)

// Teal — derived tertiary accent
val DuagenAITeal           = Color(0xFF0A6B6B)
val DuagenAITealOn         = Color(0xFFFFFFFF)
val DuagenAITealSoft       = Color(0xFFD3EDEC)
val DuagenAITealInk        = Color(0xFF00494C)

// Canvas / surfaces
val DuagenAICanvas         = Color(0xFFEEF2EF)
val DuagenAISurface        = Color(0xFFFFFFFF)
val DuagenAISurfaceLow     = Color(0xFFF6F8F5)
val DuagenAISurfaceHigh    = Color(0xFFE9EFEA)
val DuagenAISurfaceHighest = Color(0xFFE4EBE6)

// Ink (text)
val DuagenAIInk            = Color(0xFF182B23)
val DuagenAIInk2           = Color(0xFF56665C)

// Lines
val DuagenAILine           = Color(0xFFDDE6E0)
val DuagenAILineStrong     = Color(0xFF7D8D84)

// Error
val DuagenAIError          = Color(0xFFB3261E)
val DuagenAIErrorOn        = Color(0xFFFFFFFF)
val DuagenAIErrorSoft      = Color(0xFFF9DEDC)
val DuagenAIErrorInk       = Color(0xFF410E0B)


// ---------------------------------------------------------------------------
//  Dark palette — raw tokens
// ---------------------------------------------------------------------------

val DuagenAIGreenDark      = Color(0xFFCBE7D2)
val DuagenAIGreenDarkOn    = Color(0xFF17201B)
val DuagenAIGreenDarkSoft  = Color(0xFF235039)
val DuagenAIGreenDarkInk   = Color(0xFFA2E4B6)

val DuagenAIMintDarkInk    = Color(0xFF06291A)
val DuagenAIBrandSoftDark  = Color(0xFF183826)
val DuagenAIBrandSoftDarkInk = Color(0xFFA6E6B9)

val DuagenAITealDark       = Color(0xFF7FD3D8)
val DuagenAITealDarkOn     = Color(0xFF00363A)
val DuagenAITealDarkSoft   = Color(0xFF004F53)
val DuagenAITealDarkInk    = Color(0xFF9BF0F4)

val DuagenAICanvasDark     = Color(0xFF141816)
val DuagenAISurfaceDark    = Color(0xFF1D2220)
val DuagenAISurfaceDarkLow = Color(0xFF191E1C)
val DuagenAISurfaceDarkHigh = Color(0xFF272E2B)
val DuagenAISurfaceDarkHighest = Color(0xFF323B36)
val DuagenAISurfaceDarkLowest  = Color(0xFF0D1110)

val DuagenAIInkDark        = Color(0xFFF7F9F7)
val DuagenAIInk2Dark       = Color(0xFFCBD4CE)

val DuagenAILineDark       = Color(0xFF2B3330)
val DuagenAILineStrongDark = Color(0xFF6B7972)

val DuagenAIErrorDark      = Color(0xFFF2B8B5)
val DuagenAIErrorDarkOn    = Color(0xFF601410)
val DuagenAIErrorDarkSoft  = Color(0xFF8C1D18)
val DuagenAIErrorDarkInk   = Color(0xFFF9DEDC)
