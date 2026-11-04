package ai.teamily.app

import ai.teamily.app.screens.AgentsScreen
import ai.teamily.app.screens.AppearanceScreen
import ai.teamily.app.screens.ChatScreen
import ai.teamily.app.screens.ChatsScreen
import ai.teamily.app.screens.DiscoverScreen
import ai.teamily.app.screens.EditProfileScreen
import ai.teamily.app.screens.InfoScreen
import ai.teamily.app.screens.LoginScreen
import ai.teamily.app.screens.LanguageScreen
import ai.teamily.app.screens.ProfileScreen
import ai.teamily.app.screens.SettingsScreen
import ai.teamily.app.screens.TogglesScreen
import ai.teamily.app.ui.BottomNav
import ai.teamily.app.ui.LocalThemeController
import ai.teamily.app.ui.TeamilyTheme
import ai.teamily.app.ui.ThemeController
import ai.teamily.app.ui.tc
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val controller = ThemeController(applicationContext)
        val api = Api(applicationContext)
        setContent {
            CompositionLocalProvider(LocalThemeController provides controller) {
                TeamilyTheme(controller.mode) {
                    Box(Modifier.fillMaxSize().background(tc.canvas)) {
                        if (api.token == null) LoginScreen(api) else TeamilyApp(api)
                    }
                }
            }
        }
    }
}

private val MainRoutes = setOf("chats", "agents", "discover", "profile")

@androidx.compose.runtime.Composable
fun TeamilyApp(api: Api) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    Column(Modifier.fillMaxSize().background(tc.canvas)) {
        Box(Modifier.weight(1f)) {
            NavHost(nav, startDestination = "chats") { graph(nav, api) }
        }
        if (route in MainRoutes) {
            BottomNav(route) { target ->
                nav.navigate(target) {
                    popUpTo("chats") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
}

private fun NavGraphBuilder.graph(nav: NavHostController, api: Api) {
    val back: () -> Unit = { nav.popBackStack() }
    val go: (String) -> Unit = { nav.navigate(it) }

    composable("chats") { ChatsScreen { id -> nav.navigate("chat/$id") } }
    composable("chat/{id}") { ChatScreen(it.arguments?.getString("id").orEmpty(), api, back) }
    composable("agents") { AgentsScreen { id -> nav.navigate("chat/$id") } }
    composable("discover") { DiscoverScreen { id -> nav.navigate("chat/$id") } }
    composable("profile") { ProfileScreen(go) { api.signOut() } }
    composable("settings") { SettingsScreen(back, go) }
    composable("appearance") { AppearanceScreen(back) }
    composable("edit") { EditProfileScreen(back) }
    composable("notifications") {
        TogglesScreen("Notifications", back, listOf(
            "Messages" to true, "Agent updates" to true, "Mentions" to true,
            "Group activity" to true, "Artifact ready" to true, "Product news" to false, "Tips" to false,
        ))
    }
    composable("privacy") {
        TogglesScreen("Privacy & security", back, listOf("App lock" to false, "Hide previews" to true, "Read receipts" to true))
    }
    composable("chatprefs") {
        TogglesScreen("Chat preferences", back, listOf("Send with Enter" to true, "Typing indicators" to true, "Stream replies" to true))
    }
    composable("storage") {
        InfoScreen("Storage", back, listOf("Used" to "3.2 GB", "Total" to "15 GB", "Artifacts" to "1.9 GB", "Media" to "1.3 GB"))
    }
    composable("language") { LanguageScreen(back) }
    composable("about") {
        InfoScreen("About Teamily AI", back, listOf("Version" to "1.8.2", "Build" to "1820", "Terms" to "teamily.ai/terms", "Licenses" to "Open source"))
    }
}
