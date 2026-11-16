package ai.duagenai.design.nav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.duagenai.design.screens.SettingsStore
import ai.duagenai.design.screens.ThemeMode
import ai.duagenai.design.theme.DuaGenAITheme

/**
 * The app's single launcher activity.
 *
 * It lives in its own file, next to [DuaGenAINavHost], because it is the entry
 * point `AndroidManifest.xml` names (`.nav.MainActivity`) — a launcher class
 * buried at the bottom of a 550-line nav file is not findable by anyone
 * searching for it.
 *
 * `enableEdgeToEdge()` is called before `setContent` so [DuaGenAITheme] can
 * paint the status-bar band (`#1D2220` dark, `#FAFBFA` light) and set
 * `isAppearanceLightStatusBars = !darkTheme`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settings = remember(context) { SettingsStore(context.applicationContext) }
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
            val isDark = when (themeMode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }
            DuaGenAITheme(darkTheme = isDark) {
                DuaGenAINavHost()
            }
        }
    }
}
