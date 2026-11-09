package ai.duagenai.design.nav

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ai.duagenai.design.cube.CubeIds
import ai.duagenai.design.cube.CubePickerScreen
import ai.duagenai.design.cube.CubeStore
import ai.duagenai.design.screens.AgentsScreen
import ai.duagenai.design.screens.AppearanceScreen
import ai.duagenai.design.screens.ArtifactsRepository
import ai.duagenai.design.screens.ArtifactsScreen
import ai.duagenai.design.screens.ChatScreen
import ai.duagenai.design.screens.ORHAN_CHAT_ID
import ai.duagenai.design.screens.ORHAN_CHAT_TITLE
import ai.duagenai.design.screens.DiscoverScreen
import ai.duagenai.design.screens.DrawerDestination
import ai.duagenai.design.screens.HomeMode
import ai.duagenai.design.screens.HomeScreen
import ai.duagenai.design.screens.HomeTab
import ai.duagenai.design.screens.MemoryRepository
import ai.duagenai.design.screens.MemoryScreen
import ai.duagenai.design.screens.NotificationsScreen
import ai.duagenai.design.screens.ProfileScreen
import ai.duagenai.design.screens.SettingsScreen
import ai.duagenai.design.screens.SettingsStore
import ai.duagenai.design.screens.SidebarDrawer
import ai.duagenai.design.screens.StorageScreen
import ai.duagenai.design.screens.ThemeMode
import ai.duagenai.design.screens.defaultNotificationState
import ai.duagenai.design.screens.notificationSummary
import ai.duagenai.design.screens.sampleChats
import ai.duagenai.design.theme.DuaGenAITheme
import kotlinx.coroutines.launch

/* ============================================================================
 *  Dua Gen AI — navigation
 *
 *  The kit shipped seven screens as standalone composables with no host, so
 *  "Change cube" on Profile had nowhere to go. This is the minimal wiring:
 *  Navigation Compose (the library the screens were already written against),
 *  one NavHost, and ONE CubeStore instance hoisted here.
 *
 *  Why the store lives here and not in each screen:
 *      `CubeStore.selected` is a single Flow. Collecting it once at the host and
 *      passing the id down means Profile, Home's top bar, the drawer footer and
 *      the picker all read the same value — there is no second copy that can
 *      drift. Saving writes the store, the Flow emits, and every screen that
 *      takes `cubeId` recomposes on its own. No manual refresh, no event bus.
 *
 *  THE NEW SHELL — sidebar drawer
 *  ------------------------------
 *  Home's hamburger opens [SidebarDrawer]. The drawer is hosted here with M3's
 *  [ModalNavigationDrawer], which supplies the scrim, the swipe-to-dismiss and
 *  the open/close animation. That is the existing M3 pattern, so no custom
 *  animation code and no new navigation framework is introduced.
 *
 *  The drawer state is hoisted here rather than inside HomeScreen because the
 *  drawer is a sibling of the NavHost, not a child of Home: it must be able to
 *  cover the whole app surface, and it must survive a Home recomposition.
 *
 *  The Dua | Orhan mode is also hoisted here, with `rememberSaveable`, so a
 *  configuration change (rotation, dark-mode toggle) does not silently reset the
 *  user's chosen mode.
 *
 *  What is deliberately NOT here:
 *      Chat, Agents and Discover do not take the saved cube. Chat derives its
 *      cube from the `chatId` it opens; Agents/Discover carry a per-entity cube.
 *      Changing the avatar therefore never rewrites a conversation or an agent.
 *
 *  Chat is reached from a DRAWER HISTORY ROW, never from the bottom bar: the row
 *  passes its `chatId` and `name` into `chat/{chatId}?title={title}`, and
 *  ChatScreen derives its cube from that id with the same
 *  `CubeCatalog.forEntity` the row used. Back pops to Home.
 *
 *  PROFILE is reached from the DRAWER FOOTER GEAR. The route and its
 *  `ProfileScreen` destination already existed; what was missing was a caller,
 *  so the screen was unreachable. The gear now navigates there through the same
 *  `navController.navigate(...)` every other destination uses — no new route,
 *  no new screen, and the gear's icon and styling are untouched. Back pops to
 *  whatever opened the drawer, which is Home in the normal flow.
 *
 *  Why not from Home: Dua's home is deliberately empty (Dua is a chatbot, not an
 *  agent host), so it draws no chat list. The drawer's history section is the
 *  one place conversations are listed, and it renders the same `sampleChats`
 *  list with the same per-chat cubes.
 * ==========================================================================*/

/** Every destination, in one place so a typo cannot silently create a new route. */
object DuaGenAIRoutes {
    const val HOME = "home"
    const val AGENTS = "agents"
    const val DISCOVER = "discover"
    const val MEMORY = "memory"
    const val ARTIFACTS = "artifacts"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val CUBE_PICKER = "cube_picker"
    const val APPEARANCE = "appearance"
    const val STORAGE = "storage"
    const val NOTIFICATIONS = "notifications"

    /** `chat/{chatId}?title={title}` — the title is optional and display-only. */
    const val CHAT = "chat/{chatId}?title={title}"

    /**
     * Builds a chat route.
     *
     * The title is URL-encoded because a chat name can contain `/`, `?` or a
     * space, any of which would otherwise split the path and break the match.
     * The cube is never derived from the title — only from [chatId] — so a
     * rename cannot change a conversation's avatar.
     */
    fun chat(chatId: String, title: String): String =
        "chat/${Uri.encode(chatId)}?title=${Uri.encode(title)}"
}

/**
 * The app's single navigation graph, wrapped in the sidebar drawer.
 *
 * @param navController hoisted so a caller (a test, a deep link) can drive it.
 */
@Composable
fun DuaGenAINavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // applicationContext: the store outlives any single Activity recreation, and
    // DataStore must not be created twice for the same file.
    val store = remember(context) { CubeStore(context.applicationContext) }
    // The Profile settings (theme / storage / notifications) live in the SAME
    // DataStore pattern CubeStore uses, created once here and passed down — so
    // the Profile rows' subtitles and the screens they open read one copy.
    val settings = remember(context) { SettingsStore(context.applicationContext) }
    // The Artifacts data source. Created once here and passed into the screen,
    // so the screen holds no data of its own and a real backend later means
    // changing only the repository.
    val artifactsRepository = remember { ArtifactsRepository() }
    // The Memory data source, the same shape as the Artifacts one: created once
    // here and passed down, so the screen holds no data of its own and
    // pull-to-refresh has a `load()` to re-run.
    val memoryRepository = remember { MemoryRepository() }
    val scope = rememberCoroutineScope()

    // The one and only read of the saved cube in the whole app.
    //
    // `CubeIds.DEFAULT` is passed explicitly because `CubeStore.selected` is a
    // plain `Flow<String>`, and the `Flow<T>` overload of
    // `collectAsStateWithLifecycle` has NO default for `initialValue` — only the
    // `StateFlow<T>` overload does. Omitting it is a compile error, not a
    // runtime fallback. The value is also the right one: it is exactly what the
    // store itself emits when nothing has been saved yet, so the first frame
    // shows the same cube the first emission will.
    val savedCubeId by store.selected.collectAsStateWithLifecycle(CubeIds.DEFAULT)

    // The Profile settings, collected once here. Each has an explicit initial
    // value because the `Flow<T>` overload of `collectAsStateWithLifecycle` has
    // no default — the same reason `savedCubeId` passes one above.
    val themeMode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
    val storageUsedGb by settings.storageUsedGb
        .collectAsStateWithLifecycle(SettingsStore.DEFAULT_STORAGE_USED_GB)
    val storageTotalGb by settings.storageTotalGb
        .collectAsStateWithLifecycle(SettingsStore.DEFAULT_STORAGE_TOTAL_GB)
    val notifications by settings.notifications
        .collectAsStateWithLifecycle(defaultNotificationState)

    // Drawer state — hoisted so the drawer can cover the whole app surface and
    // survive a Home recomposition. `rememberDrawerState` is M3's own holder; it
    // carries the animation and the swipe gesture.
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Dua | Orhan. rememberSaveable so rotation / theme change keeps the choice.
    var mode by rememberSaveable { mutableStateOf(HomeMode.Dua) }

    // Which bottom-nav item is highlighted. HOISTED out of HomeScreen and
    // rememberSaveable, so a tab tap survives a Home recomposition, a rotation
    // and a process death — the same treatment `mode` already gets. HomeScreen
    // takes it as a value and reports taps back through `onSelectTab`; it no
    // longer hardcodes `HomeTab.Chats`, which is why the indicator could never
    // move before.
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.Chats) }

    // Which drawer row is highlighted. Derived from the current route rather
    // than stored separately, so the drawer can never claim a destination the
    // NavHost is not actually showing.
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val selectedDestination = when (currentRoute) {
        DuaGenAIRoutes.HOME -> DrawerDestination.NewChat
        DuaGenAIRoutes.SETTINGS -> null
        else -> null
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // The scrim is M3's own; tapping it closes the drawer, which is the
        // "scrim tap par band hota hai" behaviour, for free.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            SidebarDrawer(
                cubeId = savedCubeId,
                userName = "Fareed Khan",
                planLabel = "PRO",
                // The SAME list Home renders — one source, so a history row and
                // its Home row can never disagree about a name or a cube.
                history = sampleChats,
                selected = selectedDestination,
                onSelect = { destination ->
                    scope.launch { drawerState.close() }
                    when (destination) {
                        // New chat returns to the list. A real app would create
                        // a conversation here; the kit has no such screen.
                        DrawerDestination.NewChat -> navController.navigate(DuaGenAIRoutes.HOME) {
                            popUpTo(DuaGenAIRoutes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                        // Orhan is a MODE, not a screen: it switches the
                        // composer and leaves the user on Home.
                        DrawerDestination.Orhan -> {
                            mode = HomeMode.Orhan
                            navController.navigate(DuaGenAIRoutes.HOME) {
                                popUpTo(DuaGenAIRoutes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        // Library / Images / Computer / Videos have no screen
                        // yet — the request was explicitly two screens only.
                        // They close the drawer and do nothing else, rather
                        // than navigating to a route that does not exist.
                        DrawerDestination.Library,
                        DrawerDestination.Images,
                        DrawerDestination.Computer,
                        DrawerDestination.Videos -> Unit
                    }
                },
                onOpenChat = { chatId, title ->
                    scope.launch { drawerState.close() }
                    navController.navigate(DuaGenAIRoutes.chat(chatId, title))
                },
                // The drawer footer's gear opens PROFILE. It is the SAME
                // `profile` destination the graph already registers — this is
                // the wiring that was missing, not a new screen. The drawer
                // closes first, exactly like a history row, so the destination
                // is revealed in the pain area rather than behind the sheet.
                onOpenProfile = {
                    scope.launch { drawerState.close() }
                    navController.navigate(DuaGenAIRoutes.PROFILE)
                },
                onClose = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = DuaGenAIRoutes.HOME,
            modifier = modifier,
        ) {
            composable(DuaGenAIRoutes.HOME) {
                HomeScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    mode = mode,
                    onModeChange = { mode = it },
                    // The top bar's right button is a gear in Dua mode and a plus
                    // in Orhan mode, so the two callbacks are separate.
                    onOpenSettings = { navController.navigate(DuaGenAIRoutes.SETTINGS) },
                    onNewChat = { navController.navigate(DuaGenAIRoutes.HOME) },
                    // Every tab now has a destination. `Managed` is the
                    // reference's second tab, relabelled for the product model:
                    // Orhan is itself an agent, so the tab lists the agents it
                    // controls and opens the same AgentsScreen. Memory and
                    // Artifacts open their own placeholder screens.
                    onSelectTab = { tab ->
                        // Record the selection FIRST, so the indicator moves
                        // immediately, then navigate.
                        selectedTab = tab
                        when (tab) {
                            HomeTab.Chats -> Unit
                            HomeTab.Managed -> navController.navigate(DuaGenAIRoutes.AGENTS)
                            HomeTab.Discover -> navController.navigate(DuaGenAIRoutes.DISCOVER)
                            HomeTab.Memory -> navController.navigate(DuaGenAIRoutes.MEMORY)
                            HomeTab.Artifacts -> navController.navigate(DuaGenAIRoutes.ARTIFACTS)
                        }
                    },
                    selectedTab = selectedTab,
                    // The pinned Orhan card opens ORHAN'S OWN thread. Orhan is an
                    // agent, so it has a conversation of its own — and it goes
                    // through the SAME `chat/{chatId}` route every other thread
                    // uses, with Orhan's own stable id. No new route was invented.
                    onOpenOrhanChat = {
                        navController.navigate(
                            DuaGenAIRoutes.chat(ORHAN_CHAT_ID, ORHAN_CHAT_TITLE),
                        )
                    },
                )
            }

            composable(DuaGenAIRoutes.PROFILE) {
                ProfileScreen(
                    cubeId = savedCubeId,
                    // The whole point of this file: Profile -> picker.
                    onChangeCube = { navController.navigate(DuaGenAIRoutes.CUBE_PICKER) },
                    // The other three rows, each wired to its own destination.
                    onOpenAppearance = { navController.navigate(DuaGenAIRoutes.APPEARANCE) },
                    onOpenStorage = { navController.navigate(DuaGenAIRoutes.STORAGE) },
                    onOpenNotifications = { navController.navigate(DuaGenAIRoutes.NOTIFICATIONS) },
                    // Each row's subtitle is the CURRENT value, read from the
                    // same store the screen it opens writes to.
                    themeModeLabel = themeMode.label,
                    storageLabel = storageLabel(storageUsedGb, storageTotalGb),
                    notificationsLabel = notificationSummary(notifications),
                )
            }

            composable(DuaGenAIRoutes.APPEARANCE) {
                AppearanceScreen(
                    selected = themeMode,
                    // Persist through the existing store; the Flow re-emits and
                    // the Profile row's subtitle updates on its own.
                    onSelect = { mode -> scope.launch { settings.setThemeMode(mode) } },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(DuaGenAIRoutes.STORAGE) {
                StorageScreen(
                    usedGb = storageUsedGb,
                    totalGb = storageTotalGb,
                    // "Clear cache" writes the post-clear value; the Flow
                    // re-emits and both this screen and the Profile row update.
                    onClearCache = {
                        scope.launch {
                            settings.setStorageUsedGb(SettingsStore.CLEARED_STORAGE_USED_GB)
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(DuaGenAIRoutes.NOTIFICATIONS) {
                NotificationsScreen(
                    enabled = notifications,
                    onToggle = { setting, enabled ->
                        scope.launch { settings.setNotification(setting, enabled) }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(DuaGenAIRoutes.CUBE_PICKER) {
                CubePickerScreen(
                    // The picker starts from the persisted value, so reopening it
                    // always shows the saved cube rather than a stale draft.
                    savedCubeId = savedCubeId,
                    onSave = { picked ->
                        // Persist FIRST, then leave. Popping before the write
                        // lands would let a fast back-press show the old avatar
                        // on Profile for a frame, and a process death in that
                        // window would lose the choice entirely.
                        scope.launch {
                            store.save(picked)
                            navController.popBackStack()
                        }
                    },
                    // Cancel writes nothing — the draft dies with the screen.
                    onCancel = { navController.popBackStack() },
                )
            }

            composable(
                route = DuaGenAIRoutes.CHAT,
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val chatId = entry.arguments?.getString("chatId").orEmpty()
                val title = entry.arguments?.getString("title").orEmpty()
                ChatScreen(
                    chatId = chatId,
                    chatTitle = title.ifBlank { "Chat" },
                    // Back returns to whatever opened the thread — Home, in the
                    // normal flow. popBackStack() is a no-op if the stack is
                    // empty, so a deep link into a chat cannot crash on back.
                    onBack = { navController.popBackStack() },
                )
            }

            composable(DuaGenAIRoutes.AGENTS) { AgentsScreen() }
            composable(DuaGenAIRoutes.DISCOVER) { DiscoverScreen() }
            composable(DuaGenAIRoutes.MEMORY) {
                // The screen reads from the repository, not a hardcoded list.
                MemoryScreen(repository = memoryRepository)
            }
            composable(DuaGenAIRoutes.ARTIFACTS) {
                // The screen reads from the repository, not a hardcoded list.
                ArtifactsScreen(repository = artifactsRepository)
            }
            composable(DuaGenAIRoutes.SETTINGS) { SettingsScreen() }
        }
    }
}

/**
 * Host activity.
 *
 * `enableEdgeToEdge()` is called before `setContent` so the theme's
 * `WindowCompat` status-bar work in `DuaGenAITheme` has an edge-to-edge window
 * to act on.
 */
/**
 * `3.2` / `15.0` -> `"3.2 GB of 15 GB"` — the Profile row's Storage subtitle.
 *
 * Kept here (not in the screen) because the ROW shows it, and the row is drawn
 * by ProfileScreen from a value the host passes in.
 */
private fun storageLabel(usedGb: Double, totalGb: Double): String {
    fun fmt(v: Double): String {
        val r = Math.round(v * 10) / 10.0
        return if (r % 1.0 == 0.0) "${r.toInt()} GB" else "$r GB"
    }
    return "${fmt(usedGb)} of ${fmt(totalGb)}"
}

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
