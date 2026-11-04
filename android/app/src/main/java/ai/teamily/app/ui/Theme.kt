package ai.teamily.app.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Immutable
class TeamilyColors(
    val canvas: Color, val surface: Color, val surface2: Color, val surface3: Color,
    val line: Color, val lineSoft: Color, val field: Color, val fieldEdge: Color,
    val navBg: Color, val scrim: Color,
    val ink: Color, val ink2: Color, val ink3: Color,
    val primary: Color, val onPrimary: Color, val primarySoft: Color, val primaryInk: Color,
    val mint: Color, val brandSoft: Color, val danger: Color,
    val userBubble: Color, val userInk: Color, val agentBubble: Color, val agentInk: Color,
    val isDark: Boolean,
)

val DayColors = TeamilyColors(
    canvas = Color(0xFFEEF2EF), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFF6F8F5),
    surface3 = Color(0xFFEEF2EF), line = Color(0xFFDDE6E0), lineSoft = Color(0xFFE9EFEA),
    field = Color(0xFFEEF2EF), fieldEdge = Color(0xFFD7E2DB),
    navBg = Color(0xF0FFFFFF), scrim = Color(0x1A182B23),
    ink = Color(0xFF182B23), ink2 = Color(0xFF4F6157), ink3 = Color(0xFF5C6D63),
    primary = Color(0xFF087C45), onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFDDF3E5), primaryInk = Color(0xFF066A3B),
    mint = Color(0xFF42D982), brandSoft = Color(0xFFCBE7D2), danger = Color(0xFFA4271E),
    userBubble = Color(0xFF087C45), userInk = Color(0xFFFFFFFF),
    agentBubble = Color(0xFFFFFFFF), agentInk = Color(0xFF182B23),
    isDark = false,
)

val NightColors = TeamilyColors(
    canvas = Color(0xFF141816), surface = Color(0xFF1D2220), surface2 = Color(0xFF272E2B),
    surface3 = Color(0xFF323B36), line = Color(0xFF3D4842), lineSoft = Color(0xFF2B3330),
    field = Color(0xFF272E2B), fieldEdge = Color(0xFF3D4842),
    navBg = Color(0xF21D2220), scrim = Color(0x73000000),
    ink = Color(0xFFF7F9F7), ink2 = Color(0xFFC3CDC7), ink3 = Color(0xFF93A199),
    primary = Color(0xFFCBE7D2), onPrimary = Color(0xFF17201B),
    primarySoft = Color(0xFF235039), primaryInk = Color(0xFFA6E6B9),
    mint = Color(0xFF42D982), brandSoft = Color(0xFF183826), danger = Color(0xFFF2B8B5),
    userBubble = Color(0xFF505752), userInk = Color(0xFFF7F9F7),
    agentBubble = Color(0xFF303433), agentInk = Color(0xFFF7F9F7),
    isDark = true,
)

val LocalTeamily = staticCompositionLocalOf { DayColors }

enum class ThemeMode(val label: String, val tag: String, val description: String) {
    System("System", "Auto", "Follows your Android theme setting and updates live."),
    Night("Night", "Dark", "Deep charcoal canvas #141816 with a desaturated user bubble."),
    Day("Day", "Light", "Bright canvas #eef2ef with the Teamily green user bubble."),
}

/** Persists the theme mode under the key from the design spec. */
class ThemeController(context: Context) {
    private val prefs = context.getSharedPreferences("teamily", Context.MODE_PRIVATE)
    var mode by mutableStateOf(
        prefs.getString(KEY, null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System
    )
        private set

    fun set(newMode: ThemeMode) {
        mode = newMode
        prefs.edit().putString(KEY, newMode.name).apply()
    }

    private companion object {
        const val KEY = "teamily-theme-mode"
    }
}

val LocalThemeController = staticCompositionLocalOf<ThemeController> { error("ThemeController not provided") }

object T {
    private val font = FontFamily.Default // swap for Lato once the TTF subsets are in res/font

    private fun style(weight: FontWeight, size: Float, line: Float, spacing: Float = 0f) = TextStyle(
        fontFamily = font, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.em,
    )

    val display = style(FontWeight.Black, 21f, 25f, -0.02f)
    val title = style(FontWeight.Bold, 17f, 23f, -0.01f)
    val section = style(FontWeight.Bold, 13f, 18f, 0.06f)
    val rowTitle = style(FontWeight.Bold, 15f, 22f, -0.01f)
    val body = style(FontWeight.Normal, 14.5f, 22.5f)
    val rowSub = style(FontWeight.Normal, 12.5f, 18f)
    val chip = style(FontWeight.Bold, 12f, 14f, 0.02f)
    val caption = style(FontWeight.Normal, 11.5f, 16f, 0.01f)
    val overline = style(FontWeight.Bold, 10f, 12f, 0.1f)
}

@Composable
fun TeamilyTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Night -> true
        ThemeMode.Day -> false
    }
    val c = if (dark) NightColors else DayColors
    val scheme: ColorScheme = if (dark) {
        darkColorScheme(primary = c.primary, onPrimary = c.onPrimary, background = c.canvas,
            surface = c.surface, onSurface = c.ink, onBackground = c.ink, error = c.danger)
    } else {
        lightColorScheme(primary = c.primary, onPrimary = c.onPrimary, background = c.canvas,
            surface = c.surface, onSurface = c.ink, onBackground = c.ink, error = c.danger)
    }
    CompositionLocalProvider(LocalTeamily provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

val tc: TeamilyColors
    @Composable get() = LocalTeamily.current
