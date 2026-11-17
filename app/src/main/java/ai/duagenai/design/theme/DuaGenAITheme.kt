package ai.duagenai.design.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ============================================================================
 *  DuaGenAITheme — the ONE theme the whole kit reads.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  Every screen in `documents/M3-design_v01/` was written against
 *  `DuaGenAITheme.colors`, `DuaGenAITheme.shapes`, `DuaGenAITextStyles`,
 *  `LocalDuaGenAISpacing` and a `DuaGenAITheme(darkTheme = …) { … }` wrapper —
 *  but the file that defines them was never shipped, so the package could not
 *  compile. This is that missing file: the type it references now exists.
 *
 *  THE SHAPE OF THE API (why an `object` with `operator fun invoke`)
 *  ----------------------------------------------------------------
 *  The screens use the name in TWO ways:
 *
 *      DuaGenAITheme(darkTheme = true) { ProfileScreen() }   // a wrapper
 *      DuaGenAITheme.colors.brandSoft                         // a token lookup
 *
 *  A plain composable function cannot also be an object, and a plain object
 *  cannot be "called" with a trailing lambda. Declaring it as an `object` with
 *  an `operator fun invoke` supports both call shapes, which is exactly what the
 *  call sites need — and it keeps the token lookups static, so a screen cannot
 *  accidentally read another theme's tokens.
 *
 *  WHERE THE COLOURS COME FROM
 *  ---------------------------
 *  Nothing is invented here: every value is read from [TeamilyLightPalette] /
 *  [TeamilyDarkPalette] in `TeamilyTokens.kt`, which carry the design's own
 *  measured values. This file only maps them onto M3's `ColorScheme` plus the
 *  handful of tokens the design uses that M3 has no slot for.
 *
 *  Follows the OS when `darkTheme` is omitted (`isSystemInDarkTheme()`), so a
 *  device-level dark-mode switch works without the app passing anything.
 * ==========================================================================*/

/**
 * The design tokens M3's [androidx.compose.material3.ColorScheme] has no slot
 * for.
 *
 * Read through `DuaGenAITheme.colors`; the values come from the Teamily
 * palettes so light and dark stay in one place.
 */
data class DuaGenAIColors(
    /** Pale brand fill behind a selected chip / brand badge. */
    val brandSoft: Color,
    /** Ink on [brandSoft]. */
    val onBrandSoft: Color,
    /** `--nv-brand-product`: the mint dot / brand accent. */
    val logoMint: Color,
    /** Ink on [logoMint] (the PRO badge). */
    val onLogoMint: Color,
    /** Field / pill fill for the drawer's search row. */
    val field: Color,
    /** The 1dp divider the settings lists draw between rows. */
    val hairline: Color,
)

/**
 * Corner shapes. All of them are rounded rectangles because the design is a
 * rounded M3 language — the numbers are the `--nv-radius-*` steps from
 * `TeamilyTokens.kt`.
 */
data class DuaGenAIShapes(
    /** Cards, menus, list containers — `--nv-radius-xl` 20dp. */
    val card: Shape,
    /** Buttons and chips — `--nv-radius-pill`. */
    val pill: Shape,
    /** Bottom sheets — `--nv-radius-2xl` 24dp. */
    val sheet: Shape,
    /** Agent chat bubble: 20dp with the 6dp tail at the bottom start. */
    val agentBubble: Shape,
    /** Small avatar inside a chat bubble (28dp art in a 10dp round). */
    val avatarMessage: Shape,
    /** Avatar in a list row (40dp art in a 12dp round). */
    val avatarList: Shape,
    /** The big Profile avatar (72dp art in an 18dp round). */
    val avatarProfile: Shape,
    /** The rounded stat tile on Dua Agent's workspace. */
    val statTile: Shape,
)

/**
 * The 4dp-based spacing scale the screens use for every gap.
 *
 * `gutter` is the screen's own horizontal inset (16dp); everything else is a
 * step on the 4dp ladder. Kept as a data class rather than a file of constants
 * so a screen reads `spacing.md` — one name, one value, no magic numbers.
 */
data class DuaGenAISpacing(
    /** Screen edge inset. */
    val gutter: Dp = 14.dp,
    /** 2dp — inside a chip. */
    val xxs: Dp = 2.dp,
    /** 4dp. */
    val xs: Dp = 4.dp,
    /** 6dp. */
    val sm: Dp = 6.dp,
    /** 10dp. */
    val md: Dp = 10.dp,
    /** 16dp — between sections. */
    val lg: Dp = 16.dp,
    /** 24dp — above the last element on a sheet. */
    val xxl: Dp = 24.dp,
)

/** The spacing scale for the current composition. */
val LocalDuaGenAISpacing = staticCompositionLocalOf { DuaGenAISpacing() }

/**
 * The kit's type scale.
 *
 * Sizes and weights are the `--nv-type-*` steps from [teamilyTypography], read
 * back through `MaterialTheme.typography` so a screen picks a ROLE (title, body,
 * caption) rather than a font size. The named styles below are the product's
 * own roles: a screen label, a list row, a bubble, and so on.
 *
 * Every one is `@Composable @ReadOnlyComposable`, so it stays correct when the
 * theme switches — no cached `TextStyle` can outlive the theme it came from.
 */
object DuaGenAITextStyles {

    /** Small all-caps label above a group ("ALERTS", "PRO"). */
    val eyebrow: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
        )

    /** The overline above a hero card ("DUA GEN AI"). */
    val kicker: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
        )

    /** A tappable row's title. */
    val rowTitle: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
        )

    /** A row's second line / a card's supporting copy. */
    val snippet: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
        )

    /** A timestamp, a count, a helper line. */
    val meta: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelMedium.copy(
            fontSize = 11.sp,
        )

    /** Chat bubble body. */
    val bubble: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )

    /** Button / chip label. */
    val button: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )

    /** The dock's tab label. */
    val navLabel: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )

    /** A big number on a stat tile. */
    val statNumber: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 20.sp,
        )
}

/**
 * The app theme: M3 [MaterialTheme] + the kit's tokens.
 *
 * ```kotlin
 * DuaGenAITheme(darkTheme = true) { DuaGenAINavHost() }
 * ```
 *
 * @param darkTheme which palette to use. Defaults to the OS setting, so a
 *   device dark-mode switch is honoured without the app passing anything.
 */
object DuaGenAITheme {

    /** Corner shapes — the same in light and dark. */
    val shapes: DuaGenAIShapes = DuaGenAIShapes(
        card = TeamilyShapes.card,
        pill = TeamilyShapes.capsule,
        sheet = TeamilyShapes.sheet,
        agentBubble = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomEnd = 20.dp,
            bottomStart = 6.dp,
        ),
        avatarMessage = RoundedCornerShape(10.dp),
        avatarList = RoundedCornerShape(12.dp),
        avatarProfile = RoundedCornerShape(18.dp),
        statTile = TeamilyShapes.lg,
    )

    /** The kit's own colour tokens for the theme currently in effect. */
    val colors: DuaGenAIColors
        @Composable @ReadOnlyComposable
        get() = if (isDark) DarkColors else LightColors

    private val LightColors = DuaGenAIColors(
        brandSoft = TeamilyLightPalette.accentSoft,
        onBrandSoft = TeamilyLightPalette.primaryInk,
        logoMint = TeamilyLightPalette.logoMint,
        onLogoMint = TeamilyLightPalette.tileInk,
        field = TeamilyLightPalette.chromeFill,
        hairline = TeamilyLightPalette.border,
    )

    private val DarkColors = DuaGenAIColors(
        brandSoft = TeamilyDarkPalette.accentSoft,
        onBrandSoft = TeamilyDarkPalette.primaryInk,
        logoMint = TeamilyDarkPalette.logoMint,
        onLogoMint = TeamilyDarkPalette.tileInk,
        field = TeamilyDarkPalette.chromeFill,
        hairline = TeamilyDarkPalette.border,
    )

    /**
     * True when the composition is running in the dark palette.
     *
     * Read from the ACTIVE background rather than from the parameter, so a
     * nested `DuaGenAITheme` and its tokens can never disagree.
     */
    private val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    /** Applies the theme to [content]. */
    @Composable
    operator fun invoke(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit,
    ) {
        val palette = if (darkTheme) TeamilyDarkPalette else TeamilyLightPalette
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme(palette) else lightScheme(palette),
            typography = TeamilyTypography,
            shapes = MaterialTheme.shapes.copy(
                small = TeamilyShapes.md,
                medium = TeamilyShapes.lg,
                large = TeamilyShapes.card,
            ),
        ) {
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window ?: return@SideEffect
                    // Dark theme: light icons. Light theme: dark icons.
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !darkTheme
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                }
            }
            CompositionLocalProvider(LocalDuaGenAISpacing provides DuaGenAISpacing()) {
                // True edge-to-edge: no opaque status-bar band on top. Each
                // screen's own `teamilyCanvas` / top bar fill draws behind the
                // transparent status bar; the icon colour is set just above.
                content()
            }
        }
    }

    /** The light [TeamilyPalette] mapped onto M3's roles. */
    internal fun lightScheme(p: TeamilyPalette) = lightColorScheme(
        primary = p.accent,
        onPrimary = p.accentContrast,
        primaryContainer = p.accentSoft,
        onPrimaryContainer = p.primaryInk,
        secondary = p.accentHover,
        onSecondary = p.accentContrast,
        secondaryContainer = p.brandSoftPlaceholder(),
        onSecondaryContainer = p.primaryInk,
        tertiary = p.tileViolet,
        onTertiary = p.tileInk,
        background = p.bgCanvas,
        onBackground = p.textPrimary,
        surface = p.bgSurface,
        onSurface = p.textPrimary,
        surfaceVariant = p.bgElevated,
        onSurfaceVariant = p.textSecondary,
        outline = p.fieldEdge,
        outlineVariant = p.border,
        error = p.danger,
        onError = Color.White,
        errorContainer = p.dangerSoft,
        onErrorContainer = p.dangerInk,
        inverseSurface = p.textPrimary,
        inverseOnSurface = p.bgCanvas,
        // M3 surface roles (dialogs, menus, sheets, nav bars read these).
        // Unset, they fall back to M3's baseline purple-grey tones. Mapped
        // lightest -> darkest from existing palette tokens; no new colors.
        surfaceContainerLowest = p.bgSurface,
        surfaceContainerLow = p.bottomnavBg,
        surfaceContainer = p.bgElevated,
        surfaceContainerHigh = p.surfaceRecessed,
        surfaceContainerHighest = p.bgCanvas,
        surfaceDim = p.bgCanvas,
        surfaceBright = p.bgSurface,
        surfaceTint = p.accent,
        scrim = Color.Black,
    )

    /** The dark [TeamilyPalette] mapped onto M3's roles. */
    internal fun darkScheme(p: TeamilyPalette) = darkColorScheme(
        // Freeze primary #087C45. onPrimary is textPrimary (#F7F9F7), not
        // accentContrast (#17201B), which fails contrast on this fill.
        primary = p.accent,
        onPrimary = p.textPrimary,
        primaryContainer = p.accentSoft,
        onPrimaryContainer = p.primaryInk,
        secondary = p.logoMint,
        onSecondary = p.accentContrast,
        secondaryContainer = p.primarySoft,
        onSecondaryContainer = p.primaryInk,
        tertiary = p.tileViolet,
        onTertiary = p.tileInk,
        background = p.bgCanvas,
        onBackground = p.textPrimary,
        surface = p.bgSurface,
        onSurface = p.textPrimary,
        surfaceVariant = p.bgElevated,
        onSurfaceVariant = p.textSecondary,
        outline = p.fieldEdge,
        outlineVariant = p.border,
        error = p.danger,
        onError = Color.White,
        errorContainer = p.dangerSoft,
        onErrorContainer = p.dangerInk,
        inverseSurface = p.textPrimary,
        inverseOnSurface = p.bgCanvas,
        // M3 surface roles, darkest -> lightest from existing palette tokens.
        // Primary / text-button colors are intentionally unchanged.
        surfaceContainerLowest = p.surfaceRecessed,
        surfaceContainerLow = p.bgCanvas,
        surfaceContainer = p.bgSurface,
        surfaceContainerHigh = p.bgElevated,
        surfaceContainerHighest = p.chromeFill,
        surfaceDim = p.surfaceRecessed,
        surfaceBright = p.chromeFill,
        surfaceTint = p.accent,
        scrim = Color.Black,
    )

    /** `accentSoft` is the light palette's brand-soft fill. */
    private fun TeamilyPalette.brandSoftPlaceholder(): Color = accentSoft
}
