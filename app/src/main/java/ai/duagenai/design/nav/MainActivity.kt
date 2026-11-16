package ai.duagenai.design.nav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
            DuaGenAITheme {
                DuaGenAINavHost()
            }
        }
    }
}
