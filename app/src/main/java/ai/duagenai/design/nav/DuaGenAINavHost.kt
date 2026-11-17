package ai.duagenai.design.nav

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ai.duagenai.design.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ai.duagenai.design.cube.CubeIds
import ai.duagenai.design.cube.CreateAgentScreen
import ai.duagenai.design.cube.CubePickerScreen
import ai.duagenai.design.cube.CubeStore
import ai.duagenai.design.screens.PersonalAgentDemoScreen
import ai.duagenai.design.screens.DuaAgentDemoScreen
import ai.duagenai.design.screens.AgentsScreen
import ai.duagenai.design.screens.AppearanceScreen
import ai.duagenai.design.screens.ArtifactsRepository
import ai.duagenai.design.screens.ArtifactItem
import ai.duagenai.design.screens.MemoryItem
import ai.duagenai.design.screens.ArtifactsScreen
import ai.duagenai.design.screens.ChatScreen
import ai.duagenai.design.screens.AGENT_CHAT_ID
import ai.duagenai.design.screens.DUA_AGENT_TITLE
import ai.duagenai.design.screens.PERSONAL_AGENT_CHAT_ID
import ai.duagenai.design.screens.PERSONAL_AGENT_TITLE
import ai.duagenai.design.screens.DiscoverScreen
import ai.duagenai.design.screens.DrawerDestination
import ai.duagenai.design.screens.AgentWorkspaceDestination
import ai.duagenai.design.screens.DuaAgentApiScreen
import ai.duagenai.design.screens.DuaAgentArtifactsScreen
import ai.duagenai.design.screens.DuaAgentConnectAppScreen
import ai.duagenai.design.screens.DuaAgentLiveSitesScreen
import ai.duagenai.design.screens.DuaAgentMailScreen
import ai.duagenai.design.screens.DuaAgentNewGroupScreen
import ai.duagenai.design.screens.DuaAgentOverviewScreen
import ai.duagenai.design.screens.DuaAgentProfileScreen
import ai.duagenai.design.screens.DuaAgentWorkspaceScreen
import ai.duagenai.design.screens.DuaAgentSkills
import ai.duagenai.design.screens.ChatTranscriptStore
import ai.duagenai.design.screens.isAgentChatId
import ai.duagenai.design.screens.isUserMadeAgent
import ai.duagenai.design.screens.HomeMode
import ai.duagenai.design.screens.HomeScreen
import ai.duagenai.design.screens.HomeTab
import ai.duagenai.design.screens.HomeBottomNav
import ai.duagenai.design.screens.MemoryRepository
import ai.duagenai.design.screens.MemoryScreen
import ai.duagenai.design.screens.RemoteArtifactsDataSource
import ai.duagenai.design.screens.RemoteMemoryDataSource
import ai.duagenai.design.screens.TokenStore
import ai.duagenai.design.screens.NotificationsScreen
import ai.duagenai.design.screens.ProfileScreen
import ai.duagenai.design.screens.AddConnectorScreen
import ai.duagenai.design.screens.ChannelsScreen
import ai.duagenai.design.screens.ConnectorsScreen
import ai.duagenai.design.screens.CreateKindScreen
import ai.duagenai.design.screens.DEFAULT_CHANNEL_STATE
import ai.duagenai.design.screens.DEFAULT_CONNECTOR_STATE
import ai.duagenai.design.screens.LanguageScreen
import ai.duagenai.design.screens.ProfileListScreen
import ai.duagenai.design.screens.RuntimesScreen
import ai.duagenai.design.screens.SettingsScreen
import ai.duagenai.design.screens.SettingsStore
import ai.duagenai.design.screens.SidebarDrawer
import ai.duagenai.design.screens.SkillConfigScreen
import ai.duagenai.design.screens.StorageScreen
import ai.duagenai.design.screens.decodeChannels
import ai.duagenai.design.screens.decodeConnectors
import ai.duagenai.design.screens.encodeChannels
import ai.duagenai.design.screens.encodeConnectors
import ai.duagenai.design.screens.linesOf
import ai.duagenai.design.screens.madeAgentLine
import ai.duagenai.design.screens.withMadeAgentIds
import ai.duagenai.design.screens.withoutMadeAgent
import ai.duagenai.design.screens.plusLine
import ai.duagenai.design.screens.profileAgentRows
import ai.duagenai.design.screens.profileArtifactRows
import ai.duagenai.design.screens.profileChatRows
import ai.duagenai.design.screens.APP_LANGUAGES
import ai.duagenai.design.screens.CatalogApp
import ai.duagenai.design.screens.ConnectorRow
import ai.duagenai.design.screens.ThemeMode
import ai.duagenai.design.screens.defaultNotificationState
import ai.duagenai.design.screens.notificationSummary
import ai.duagenai.design.screens.demoArtifacts
import ai.duagenai.design.screens.sampleChats
import ai.duagenai.design.screens.sampleMemories
import ai.duagenai.design.theme.DuaGenAITheme
import kotlin.math.round
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
 *  The Dua | Agent mode is also hoisted here, with `rememberSaveable`, so a
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
 *  SETTINGS is reached from the DRAWER FOOTER AVATAR. The route and its
 *  `SettingsScreen` destination already existed; the avatar now navigates
 *  there through the same `navController.navigate(...)` every other
 *  destination uses — no new route, no new screen. PROFILE is reached only
 *  via Settings → Edit profile. Back from Settings pops to whatever opened
 *  the drawer, which is Home in the normal flow.
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
    const val PROFILE_CHATS = "profile/chats"
    const val PROFILE_AGENTS = "profile/agents"
    const val PROFILE_ARTIFACTS = "profile/artifacts"
    const val SETTINGS_CONNECTORS = "settings/connectors"
    const val SETTINGS_ADD_CONNECTOR = "settings/connectors/add"
    const val SETTINGS_RUNTIMES = "settings/runtimes"
    const val SETTINGS_LANGUAGE = "settings/language"
    const val SETTINGS_CHANNELS = "settings/channels"
    const val SETTINGS_SKILLS = "settings/skills"
    const val CREATE = "create/{kind}"

    fun create(kind: String): String = "create/${Uri.encode(kind)}"

    // Agent profile & workspace (approved SVG set dua-agent-01..13), reused
    // for EVERY agent: each route carries the agent's chat id and display
    // name. Both default to Dua Agent, so Dua Agent behaves exactly as before.
    private const val AGENT_ARGS = "?agentId={agentId}&name={name}"
    const val AGENT_PROFILE = "agent/profile$AGENT_ARGS"
    const val AGENT_CONNECT_APP = "agent/connect_app$AGENT_ARGS"
    const val AGENT_WORKSPACE = "agent/workspace$AGENT_ARGS"
    const val AGENT_OVERVIEW = "agent/overview$AGENT_ARGS"
    const val AGENT_ARTIFACTS = "agent/artifacts$AGENT_ARGS"
    const val AGENT_API = "agent/api$AGENT_ARGS"
    const val AGENT_LIVE_SITES = "agent/live_sites$AGENT_ARGS"
    const val AGENT_MAIL = "agent/mail$AGENT_ARGS"
    const val AGENT_NEW_GROUP = "agent/new_group$AGENT_ARGS"

    /**
     * Builds one of the AGENT_* routes for [agentId] / [name]. Both are
     * URL-encoded, like [chat], because a name can hold `/`, `?` or `&`.
     */
    fun agent(route: String, agentId: String = AGENT_CHAT_ID, name: String = DUA_AGENT_TITLE): String =
        route.substringBefore('?') + "?agentId=${Uri.encode(agentId)}&name=${Uri.encode(name)}"
    /** Create Agent: opened only from Agent home's plus menu. */
    const val CREATE_AGENT = "agent/create"
    const val PERSONAL_AGENT_DEMO = "agent/personal_demo"
    const val DUA_AGENT_DEMO = "agent/dua_demo"

    /**
     * `chat/{chatId}?title={title}&message={message}` — the title is optional
     * and display-only; `message` is a NEW thread's first message (Dua home's
     * composer), sent once by ChatScreen.
     */
    const val CHAT = "chat/{chatId}?title={title}&message={message}"

    /**
     * Builds a chat route.
     *
     * The title is URL-encoded because a chat name can contain `/`, `?` or a
     * space, any of which would otherwise split the path and break the match.
     * The cube is never derived from the title — only from [chatId] — so a
     * rename cannot change a conversation's avatar.
     */
    fun chat(chatId: String, title: String, message: String? = null): String =
        "chat/${Uri.encode(chatId)}?title=${Uri.encode(title)}" +
            if (message.isNullOrBlank()) "" else "&message=${Uri.encode(message)}"

    /** Title of a new Dua thread started from Dua home's composer. */
    const val DUA_THREAD_TITLE = "Dua"

    /** A fresh, never-reused chat id for a new Dua thread. */
    fun newDuaChatId(): String = "dua-${java.util.UUID.randomUUID()}"
}

/** The `agentId` / `name` arguments every AGENT_* route declares (Dua Agent by default). */
private val agentArguments = listOf(
    navArgument("agentId") {
        type = NavType.StringType
        defaultValue = AGENT_CHAT_ID
    },
    navArgument("name") {
        type = NavType.StringType
        defaultValue = DUA_AGENT_TITLE
    },
)

private fun NavBackStackEntry.agentId(): String =
    arguments?.getString("agentId").orEmpty().ifBlank { AGENT_CHAT_ID }

private fun NavBackStackEntry.agentName(): String =
    arguments?.getString("name").orEmpty().ifBlank { DUA_AGENT_TITLE }

/**
 * The dock tab a route belongs to, or null when the route is not one of the
 * five tab destinations. Home is the Chats tab.
 */
internal fun homeTabFor(route: String?): HomeTab? = when (route) {
    DuaGenAIRoutes.HOME -> HomeTab.Chats
    DuaGenAIRoutes.AGENTS -> HomeTab.Managed
    DuaGenAIRoutes.DISCOVER -> HomeTab.Discover
    DuaGenAIRoutes.MEMORY -> HomeTab.Memory
    DuaGenAIRoutes.ARTIFACTS -> HomeTab.Artifacts
    else -> null
}

/**
 * Bottom-dock navigation, the M3 pattern: Chats pops back to Home; any other
 * tab replaces whatever tab sits above Home (popUpTo Home, saving its state),
 * is single-top, and restores its saved state. The stack is therefore never
 * deeper than Home + one tab, and Back from a tab returns Home.
 */
internal fun NavController.navigateToTab(tab: HomeTab) {
    val route = when (tab) {
        HomeTab.Chats -> {
            popBackStack(DuaGenAIRoutes.HOME, inclusive = false)
            return
        }
        HomeTab.Managed -> DuaGenAIRoutes.AGENTS
        HomeTab.Discover -> DuaGenAIRoutes.DISCOVER
        HomeTab.Memory -> DuaGenAIRoutes.MEMORY
        HomeTab.Artifacts -> DuaGenAIRoutes.ARTIFACTS
    }
    navigate(route) {
        popUpTo(DuaGenAIRoutes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The app's single navigation graph, wrapped in the sidebar drawer.
 *
 * @param navController hoisted so a caller (a test, a deep link) can drive it.
 */
@Composable
@Suppress("ComposableCallWithoutComposePlugin")
fun DuaGenAINavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    /**
     * Seed for the signed-out in-memory source.
     *
     * EMPTY by default, and that is the production behaviour: a build must never
     * ship fake rows. It exists as a parameter so a test (or a `@Preview`) can
     * supply fixtures and exercise the loaded grid without a backend. Nothing in
     * `app/src/main` passes a non-empty value.
     */
    signedOutArtifacts: List<ArtifactItem> = emptyList(),
    /** Seed for the signed-out in-memory Memory source. Empty by default. */
    signedOutMemories: List<MemoryItem> = emptyList(),
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
    // so the screen holds no data of its own.
    //
    // WHICH SOURCE: the live backend when a session token exists, the in-memory
    // source otherwise. Without a token the live service answers 401 anyway, so
    // the signed-out state is an honest EMPTY list rather than fake rows.
    //
    // THE KEY IS OBSERVABLE STATE, NOT A PLAIN VAR.
    // This used to be `val sessionToken = TokenStore.token` with
    // `remember(sessionToken)`. `TokenStore.token` was a plain `@Volatile var`,
    // which Compose cannot observe — so signing in would NOT have recomposed
    // this function and the app would have stayed on the demo source until the
    // next process start. `TokenStore.session` is a StateFlow, collected here,
    // so a sign-in rebuilds both repositories immediately.
    val sessionToken by TokenStore.session.collectAsStateWithLifecycle()
    val artifactsRepository = remember(sessionToken, signedOutArtifacts) {
        ArtifactsRepository(
            dataSource = if (sessionToken != null) RemoteArtifactsDataSource() else null,
            // Signed out: no fake rows. The empty state is the truth.
            seed = signedOutArtifacts,
        )
    }
    // The Memory data source, the same shape as the Artifacts one: created once
    // here and passed down, so the screen holds no data of its own and
    // pull-to-refresh has a `load()` to re-run.
    val memoryRepository = remember(sessionToken, signedOutMemories) {
        MemoryRepository(
            dataSource = if (sessionToken != null) RemoteMemoryDataSource() else null,
            seed = signedOutMemories,
        )
    }
    val scope = rememberCoroutineScope()
    // ONE transcript store for every thread, scoped to the host (not to a
    // chat's back-stack entry), so an agent profile's "Clear Chat History"
    // clears the same transcript the thread shows.
    val transcriptStore: ChatTranscriptStore = viewModel()

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
    val profilePhotoPath by settings.profilePhotoPath.collectAsStateWithLifecycle(null)
    val profilePhotoVersion by settings.profilePhotoVersion.collectAsStateWithLifecycle(0L)
    // Agent profile Mute / Pin, persisted per STABLE agent id.
    val mutedAgents by settings.mutedAgents.collectAsStateWithLifecycle(emptySet())
    val pinnedAgents by settings.pinnedAgents.collectAsStateWithLifecycle(emptySet())
    val profileName by settings.profileName.collectAsStateWithLifecycle("")
    val profileAbout by settings.profileAbout.collectAsStateWithLifecycle("")
    val profileUsername by settings.profileUsername.collectAsStateWithLifecycle("")
    val profilePhone by settings.profilePhone.collectAsStateWithLifecycle("")
    val profileLinks by settings.profileLinks.collectAsStateWithLifecycle("")

    // Drawer state — hoisted so the drawer can cover the whole app surface and
    // survive a Home recomposition. `rememberDrawerState` is M3's own holder; it
    // carries the animation and the swipe gesture.
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // The app launches into Dua home (greeting, starters, composer, pill).
    // Dua home's top bar has the Dua | Agent switch; from Agent home the
    // drawer's "Dua" row (TalkBack: "Switch to Dua mode") switches back.
    var mode by rememberSaveable { mutableStateOf(HomeMode.Dua) }
    // The thread composer's agent tools — which Dua Agent skills are on and
    // whether MCP tools are enabled. Hoisted here (not per thread) so the choice
    // is one app-wide setting every thread's composer reads. Default: all five
    // skills on, MCP on. Saved as a List so it survives rotation.
    var enabledSkills by rememberSaveable { mutableStateOf(DuaAgentSkills.ids) }
    var mcpEnabled by rememberSaveable { mutableStateOf(true) }

    // Which bottom-nav item is highlighted. DERIVED from the current back
    // stack entry, not stored: the dock can never claim a tab the NavHost is
    // not showing (the old stored value stayed on Discover/Memory/Artifacts
    // after Back to Home). Off the five tab routes it falls back to Chats.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = homeTabFor(backStackEntry?.destination?.route) ?: HomeTab.Chats
    // The one dock, shared by Home (Agent mode) and the four tab screens.
    val tabDock: @Composable () -> Unit = {
        HomeBottomNav(
            selected = selectedTab,
            onSelect = { tab -> navController.navigateToTab(tab) },
        )
    }

    // Agent-home filter. True covers Home with the public profile.
    var publicProfileOpen by rememberSaveable { mutableStateOf(false) }
    // Header menu creates. Newline-separated so rememberSaveable can keep them.
    var madeAgents by rememberSaveable { mutableStateOf("") }
    // A madeAgents string restored from an older build has lines without a
    // stable id: write their derived ids in once, so they never shift again.
    LaunchedEffect(madeAgents) {
        val migrated = withMadeAgentIds(madeAgents)
        if (migrated != madeAgents) madeAgents = migrated
    }
    var madeArtifacts by rememberSaveable { mutableStateOf("") }
    var connectorState by rememberSaveable { mutableStateOf(DEFAULT_CONNECTOR_STATE) }
    var recentConnectors by rememberSaveable { mutableStateOf("") }
    var channelState by rememberSaveable { mutableStateOf(DEFAULT_CHANNEL_STATE) }
    var languageId by rememberSaveable { mutableStateOf("en") }
    val connectors = decodeConnectors(connectorState)
    val channels = decodeChannels(channelState)
    val languageLabel = APP_LANGUAGES.firstOrNull { it.id == languageId }?.label ?: "English"

    // The drawer highlights no row (ChatGPT style), so no selection is derived.

    val isDark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    DuaGenAITheme(darkTheme = isDark) {
    val layoutDirection = if (languageId == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        // The scrim is M3's own; tapping it closes the drawer, which is the
        // "scrim tap par band hota hai" behaviour, for free.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            SidebarDrawer(
                cubeId = savedCubeId,
                // No hardcoded identity: the avatar only says "Signed in" /
                // "Not signed in" until a real session supplies a name.
                signedIn = sessionToken != null,
                mode = mode,
                // The SAME list Home renders — one source, so a history row and
                // its Home row can never disagree about a name or a cube.
                history = sampleChats,
                onSelect = { destination ->
                    scope.launch { drawerState.close() }
                    when (destination) {
                        // New chat (the drawer's green "Chat" pill) returns to the list. A real app would create
                        // a conversation here; the kit has no such screen.
                        DrawerDestination.NewChat -> navController.navigate(DuaGenAIRoutes.HOME) {
                            popUpTo(DuaGenAIRoutes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                        // ONE mode row: it shows the target mode's short
                        // label. A tap closes the drawer and flips to that
                        // mode's home. Agent home has no toggle, so this
                        // is its way back to Dua.
                        DrawerDestination.ModeSwitch -> {
                            mode = if (mode == HomeMode.Agent) HomeMode.Dua else HomeMode.Agent
                            navController.navigate(DuaGenAIRoutes.HOME) {
                                popUpTo(DuaGenAIRoutes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        // Library / Images / Computer / Videos have no screen
                        // yet. The drawer answers them itself with the "Jaldi
                        // aa raha hai" snackbar and never calls onSelect.
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
                // The drawer footer's avatar opens SETTINGS. Profile is
                // reached only via Settings → Edit profile. The drawer
                // closes first, exactly like a history row, so the destination
                // is revealed in the pain area rather than behind the sheet.
                onOpenProfile = {
                    scope.launch { drawerState.close() }
                    navController.navigate(DuaGenAIRoutes.SETTINGS)
                },
            )
        },
    ) {
        val navGraph: NavGraphBuilder.() -> Unit = {
            composable(DuaGenAIRoutes.HOME) {
                HomeScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    mode = mode,
                    onModeChange = { mode = it },
                    // Dua home's composer: every Send starts a NEW Dua thread
                    // (fresh chat id) carrying that first message.
                    onSendDuaMessage = { text ->
                        navController.navigate(
                            DuaGenAIRoutes.chat(
                                DuaGenAIRoutes.newDuaChatId(),
                                DuaGenAIRoutes.DUA_THREAD_TITLE,
                                message = text,
                            ),
                        )
                    },
                    // The top bar's right button is a gear in Dua mode and a plus
                    // in Agent mode, so the two callbacks are separate.
                    // Dua home's + sheet: Create tiles open the same create
                    // route a thread's + sheet uses.
                    onOpenCreate = { kind -> navController.navigate(DuaGenAIRoutes.create(kind)) },
                    onOpenSettings = { navController.navigate(DuaGenAIRoutes.SETTINGS) },
                    onNewChat = { navController.navigate(DuaGenAIRoutes.HOME) },
                    // Every tab has a destination (see [navigateToTab]).
                    // `HomeTab.Managed` is shown as "Agents" and opens the
                    // agents route. Home is the Chats tab.
                    onSelectTab = { tab -> navController.navigateToTab(tab) },
                    selectedTab = selectedTab,
                    // Personal Agent (the hero) is a normal thread: auto mode on
                    // the API. Dua Agent (the pinned row) is the engine thread.
                    // The listed chats below use the same chat route as the drawer.
                    onOpenPersonalAgentChat = {
                        navController.navigate(
                            DuaGenAIRoutes.chat(PERSONAL_AGENT_CHAT_ID, PERSONAL_AGENT_TITLE),
                        )
                    },
                    onOpenDuaAgentChat = {
                        navController.navigate(
                            DuaGenAIRoutes.chat(AGENT_CHAT_ID, DUA_AGENT_TITLE),
                        )
                    },
                    onOpenListedChat = { chatId, title ->
                        navController.navigate(DuaGenAIRoutes.chat(chatId, title))
                    },
                    onCreateAgent = { navController.navigate(DuaGenAIRoutes.CREATE_AGENT) },
                    onNewGroup = { navController.navigate(DuaGenAIRoutes.agent(DuaGenAIRoutes.AGENT_NEW_GROUP)) },
                    onCreateArtifact = {
                        val name = "New artifact ${linesOf(madeArtifacts).size + 1}"
                        madeArtifacts = plusLine(madeArtifacts, name)
                        navController.navigate(DuaGenAIRoutes.PROFILE_ARTIFACTS)
                    },
                    filterActive = publicProfileOpen,
                    onFilterActiveChange = { publicProfileOpen = it },
                    onPublicProfileSettings = { navController.navigate(DuaGenAIRoutes.SETTINGS) },
                )
            }

            composable(DuaGenAIRoutes.PROFILE) {
                ProfileScreen(
                    cubeId = savedCubeId,
                    photoPath = profilePhotoPath,
                    photoVersion = profilePhotoVersion,
                    displayName = profileName,
                    about = profileAbout,
                    username = profileUsername,
                    phone = profilePhone,
                    links = profileLinks,
                    onChangeCube = { navController.navigate(DuaGenAIRoutes.CUBE_PICKER) },
                    onBack = { navController.popBackStack() },
                    onPhotoChosen = { bitmap -> scope.launch { settings.saveProfilePhoto(bitmap) } },
                    // Decoded + downsampled on IO inside the store.
                    onPhotoUriChosen = { uri -> settings.saveProfilePhotoFromUri(uri) != null },
                    onClearPhoto = { scope.launch { settings.clearProfilePhoto() } },
                    onSaveName = { value -> scope.launch { settings.setProfileName(value) } },
                    onSaveAbout = { value -> scope.launch { settings.setProfileAbout(value) } },
                    onSaveUsername = { value -> scope.launch { settings.setProfileUsername(value) } },
                    onSavePhone = { value -> scope.launch { settings.setProfilePhone(value) } },
                    onSaveLinks = { value -> scope.launch { settings.setProfileLinks(value) } },
                    signedIn = sessionToken != null,
                    chatCount = profileChatRows().size,
                    agentCount = profileAgentRows(linesOf(madeAgents)).size,
                    artifactCount = profileArtifactRows(linesOf(madeArtifacts)).size,
                    onOpenChats = { navController.navigate(DuaGenAIRoutes.PROFILE_CHATS) },
                    onOpenAgents = { navController.navigate(DuaGenAIRoutes.PROFILE_AGENTS) },
                    onOpenArtifacts = { navController.navigate(DuaGenAIRoutes.PROFILE_ARTIFACTS) },
                )
            }

            composable(DuaGenAIRoutes.PROFILE_CHATS) {
                val rows = profileChatRows()
                ProfileListScreen(
                    title = "Chats",
                    kicker = "Conversations",
                    rows = rows,
                    onBack = { navController.popBackStack() },
                    onOpenRow = { row ->
                        navController.navigate(DuaGenAIRoutes.chat(row.id, row.title))
                    },
                )
            }
            composable(DuaGenAIRoutes.PROFILE_AGENTS) {
                val rows = profileAgentRows(linesOf(madeAgents))
                ProfileListScreen(
                    title = "Agents",
                    kicker = "Your agents",
                    rows = rows,
                    onBack = { navController.popBackStack() },
                    onOpenRow = { row ->
                        navController.navigate(DuaGenAIRoutes.chat(row.id, row.title))
                    },
                )
            }
            composable(DuaGenAIRoutes.PROFILE_ARTIFACTS) {
                ProfileListScreen(
                    title = "Artifacts",
                    kicker = "Saved outputs",
                    rows = profileArtifactRows(linesOf(madeArtifacts)),
                    onBack = { navController.popBackStack() },
                    onOpenRow = { _ -> },
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

            composable(DuaGenAIRoutes.CREATE_AGENT) {
                CreateAgentScreen(
                    onCreate = { name, silhouette, swatch ->
                        madeAgents = plusLine(
                            withMadeAgentIds(madeAgents),
                            madeAgentLine(name, silhouette.name, swatch.name),
                        )
                        // Land on the Agents list; Back from there returns to
                        // Agent home, not to a filled-in form.
                        navController.navigate(DuaGenAIRoutes.PROFILE_AGENTS) {
                            popUpTo(DuaGenAIRoutes.CREATE_AGENT) { inclusive = true }
                        }
                    },
                    onCancel = { navController.popBackStack() },
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
                        // Persist on a worker, then pop on this (main) thread.
                        // DataStore resumes a plain coroutine off the main
                        // thread, and NavController rejects that pop.
                        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                            store.save(picked)
                        }
                        navController.popBackStack()
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
                    navArgument("message") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val chatId = entry.arguments?.getString("chatId").orEmpty()
                val title = entry.arguments?.getString("title").orEmpty()
                val firstMessage = entry.arguments?.getString("message").orEmpty()
                val shownTitle = title.ifBlank { stringResource(R.string.chat_default_title) }
                ChatScreen(
                    chatId = chatId,
                    initialMessage = firstMessage.ifBlank { null },
                    chatTitle = shownTitle,
                    // Back returns to whatever opened the thread — Home, in the
                    // normal flow. popBackStack() is a no-op if the stack is
                    // empty, so a deep link into a chat cannot crash on back.
                    onBack = { navController.popBackStack() },
                    enabledSkillIds = enabledSkills.toSet(),
                    onToggleSkill = { id ->
                        enabledSkills =
                            if (id in enabledSkills) enabledSkills - id else enabledSkills + id
                    },
                    mcpEnabled = mcpEnabled,
                    onToggleMcp = { mcpEnabled = it },
                    transcriptStore = transcriptStore,
                    // EVERY agent thread has a profile: the header pill's avatar
                    // and name open THIS agent's profile; Invite its New Group.
                    onOpenAgentProfile = if (isAgentChatId(chatId)) {
                        { navController.navigate(DuaGenAIRoutes.agent(DuaGenAIRoutes.AGENT_PROFILE, chatId, shownTitle)) }
                    } else null,
                    onInvite = {
                        navController.navigate(DuaGenAIRoutes.agent(DuaGenAIRoutes.AGENT_NEW_GROUP, chatId, shownTitle))
                    },
                    onOpenCreate = { kind ->
                        navController.navigate(DuaGenAIRoutes.create(kind))
                    },
                )
            }

            composable(DuaGenAIRoutes.PERSONAL_AGENT_DEMO) {
                PersonalAgentDemoScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(DuaGenAIRoutes.DUA_AGENT_DEMO) {
                DuaAgentDemoScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWorkspace = { navController.navigate(DuaGenAIRoutes.agent(DuaGenAIRoutes.AGENT_WORKSPACE)) },
                    onOpenProfile = { navController.navigate(DuaGenAIRoutes.agent(DuaGenAIRoutes.AGENT_PROFILE)) },
                )
            }

            // --- Agent profile & workspace (any agent: id + name in the route) ---
            composable(DuaGenAIRoutes.AGENT_PROFILE, arguments = agentArguments) { entry ->
                val id = entry.agentId()
                val name = entry.agentName()
                fun go(route: String) = navController.navigate(DuaGenAIRoutes.agent(route, id, name))
                // 02 (scrolled), 03 (more sheet), 04/05 (expanded) are states of
                // this one screen, not separate routes.
                DuaAgentProfileScreen(
                    agentId = id,
                    agentName = name,
                    onBack = { navController.popBackStack() },
                    onOpenWorkspace = { go(DuaGenAIRoutes.AGENT_WORKSPACE) },
                    onOpenOverview = { go(DuaGenAIRoutes.AGENT_OVERVIEW) },
                    onOpenNewGroup = { go(DuaGenAIRoutes.AGENT_NEW_GROUP) },
                    onOpenConnectApp = { go(DuaGenAIRoutes.AGENT_CONNECT_APP) },
                    muted = id in mutedAgents,
                    onMutedChange = { on -> scope.launch { settings.setAgentMuted(id, on) } },
                    pinned = id in pinnedAgents,
                    onPinnedChange = { on -> scope.launch { settings.setAgentPinned(id, on) } },
                    onClearHistory = { transcriptStore.clear(id) },
                    canDelete = isUserMadeAgent(id),
                    onDelete = {
                        // Only made agents reach here: drop ITS line (by stable
                        // id, so no other agent shifts), its transcript and its
                        // flags, then return to Home.
                        madeAgents = withoutMadeAgent(madeAgents, id)
                        transcriptStore.clear(id)
                        scope.launch { settings.forgetAgent(id) }
                        navController.popBackStack(DuaGenAIRoutes.HOME, inclusive = false)
                    },
                )
            }
            composable(DuaGenAIRoutes.AGENT_CONNECT_APP, arguments = agentArguments) {
                DuaAgentConnectAppScreen(onClose = { navController.popBackStack() })
            }
            composable(DuaGenAIRoutes.AGENT_WORKSPACE, arguments = agentArguments) { entry ->
                val id = entry.agentId()
                val name = entry.agentName()
                DuaAgentWorkspaceScreen(
                    agentId = id,
                    agentName = name,
                    onBack = { navController.popBackStack() },
                    onOpen = { dest ->
                        navController.navigate(
                            DuaGenAIRoutes.agent(
                                when (dest) {
                                    AgentWorkspaceDestination.Overview -> DuaGenAIRoutes.AGENT_OVERVIEW
                                    AgentWorkspaceDestination.Channels -> DuaGenAIRoutes.AGENT_CONNECT_APP
                                    AgentWorkspaceDestination.LiveSites -> DuaGenAIRoutes.AGENT_LIVE_SITES
                                    AgentWorkspaceDestination.Mail -> DuaGenAIRoutes.AGENT_MAIL
                                    AgentWorkspaceDestination.Artifacts -> DuaGenAIRoutes.AGENT_ARTIFACTS
                                    AgentWorkspaceDestination.AgentApi -> DuaGenAIRoutes.AGENT_API
                                },
                                id,
                                name,
                            ),
                        )
                    },
                )
            }
            composable(DuaGenAIRoutes.AGENT_OVERVIEW, arguments = agentArguments) { entry ->
                DuaAgentOverviewScreen(
                    agentId = entry.agentId(),
                    agentName = entry.agentName(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(DuaGenAIRoutes.AGENT_ARTIFACTS, arguments = agentArguments) {
                DuaAgentArtifactsScreen(onBack = { navController.popBackStack() })
            }
            composable(DuaGenAIRoutes.AGENT_API, arguments = agentArguments) { entry ->
                DuaAgentApiScreen(agentName = entry.agentName(), onBack = { navController.popBackStack() })
            }
            composable(DuaGenAIRoutes.AGENT_LIVE_SITES, arguments = agentArguments) { entry ->
                DuaAgentLiveSitesScreen(agentName = entry.agentName(), onBack = { navController.popBackStack() })
            }
            composable(DuaGenAIRoutes.AGENT_MAIL, arguments = agentArguments) {
                DuaAgentMailScreen(onBack = { navController.popBackStack() })
            }
            composable(DuaGenAIRoutes.AGENT_NEW_GROUP, arguments = agentArguments) { entry ->
                DuaAgentNewGroupScreen(
                    agentId = entry.agentId(),
                    agentName = entry.agentName(),
                    onClose = { navController.popBackStack() },
                    onCreate = { navController.popBackStack() },
                )
            }

            // The four tab screens carry the same dock as Home, so the user
            // can always switch tabs; Back from any of them returns Home.
            // Agents: each card opens that agent's thread on the shared chat route.
            composable(DuaGenAIRoutes.AGENTS) {
                AgentsScreen(
                    bottomBar = tabDock,
                    onOpenChat = { chatId, title ->
                        navController.navigate(DuaGenAIRoutes.chat(chatId, title))
                    },
                )
            }
            composable(DuaGenAIRoutes.DISCOVER) {
                DiscoverScreen(
                    bottomBar = tabDock,
                    // A card is a starting point: a NEW thread (fresh id) titled
                    // with the card and carrying it as the first message.
                    onStartChat = { prompt ->
                        navController.navigate(
                            DuaGenAIRoutes.chat(DuaGenAIRoutes.newDuaChatId(), prompt, message = prompt),
                        )
                    },
                )
            }
            composable(DuaGenAIRoutes.MEMORY) {
                // The screen reads from the repository, not a hardcoded list.
                MemoryScreen(
                    repository = memoryRepository,
                    // A 401 is not retryable: the honest action is to sign in,
                    // not to tap Retry forever. Sign in lives only in Settings.
                    onSignIn = { navController.navigate(DuaGenAIRoutes.SETTINGS) },
                    bottomBar = tabDock,
                )
            }
            composable(DuaGenAIRoutes.ARTIFACTS) {
                // The screen reads from the repository, not a hardcoded list.
                ArtifactsScreen(
                    repository = artifactsRepository,
                    onSignIn = { navController.navigate(DuaGenAIRoutes.SETTINGS) },
                    bottomBar = tabDock,
                )
            }
            composable(DuaGenAIRoutes.SETTINGS) {
                SettingsScreen(
                    cubeId = savedCubeId,
                    photoPath = profilePhotoPath,
                    photoVersion = profilePhotoVersion,
                    displayName = profileName,
                    connectorSummary = "${connectors.count { it.connected }}/${connectors.size}",
                    languageLabel = languageLabel,
                    themeModeLabel = themeMode.label,
                    storageLabel = storageLabel(storageUsedGb, storageTotalGb),
                    notificationsLabel = notificationSummary(notifications),
                    onOpenProfile = { navController.navigate(DuaGenAIRoutes.PROFILE) },
                    onBack = { navController.popBackStack() },
                    onOpenConnectors = { navController.navigate(DuaGenAIRoutes.SETTINGS_CONNECTORS) },
                    onOpenRuntimes = { navController.navigate(DuaGenAIRoutes.SETTINGS_RUNTIMES) },
                    onOpenSkills = { navController.navigate(DuaGenAIRoutes.SETTINGS_SKILLS) },
                    onOpenLanguage = { navController.navigate(DuaGenAIRoutes.SETTINGS_LANGUAGE) },
                    onOpenChannels = { navController.navigate(DuaGenAIRoutes.SETTINGS_CHANNELS) },
                    onOpenAgentApi = { navController.navigate(DuaGenAIRoutes.agent(DuaGenAIRoutes.AGENT_API)) },
                    onOpenAppearance = { navController.navigate(DuaGenAIRoutes.APPEARANCE) },
                    onOpenStorage = { navController.navigate(DuaGenAIRoutes.STORAGE) },
                    onOpenNotifications = { navController.navigate(DuaGenAIRoutes.NOTIFICATIONS) },
                    // The one Sign out: confirm -> server logout -> Home.
                    onSignedOut = {
                        navController.navigate(DuaGenAIRoutes.HOME) {
                            popUpTo(DuaGenAIRoutes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(DuaGenAIRoutes.SETTINGS_CONNECTORS) {
                ConnectorsScreen(
                    rows = connectors,
                    onChange = { connectorState = encodeConnectors(it) },
                    onAdd = { navController.navigate(DuaGenAIRoutes.SETTINGS_ADD_CONNECTOR) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(DuaGenAIRoutes.SETTINGS_ADD_CONNECTOR) {
                AddConnectorScreen(
                    already = connectors.map { it.key }.toSet(),
                    recentKeys = linesOf(recentConnectors),
                    onAdd = { app: CatalogApp ->
                        if (connectors.none { it.key == app.key }) {
                            connectorState = encodeConnectors(
                                connectors + ConnectorRow(
                                    app.key, app.label, app.subtitle, true,
                                ),
                            )
                        }
                        val next = listOf(app.key) + linesOf(recentConnectors).filter { it != app.key }
                        recentConnectors = next.take(10).joinToString("\n")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(DuaGenAIRoutes.SETTINGS_RUNTIMES) {
                RuntimesScreen(onBack = { navController.popBackStack() })
            }
            composable(DuaGenAIRoutes.SETTINGS_LANGUAGE) {
                LanguageScreen(
                    selectedId = languageId,
                    onSelect = { languageId = it },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(DuaGenAIRoutes.SETTINGS_CHANNELS) {
                ChannelsScreen(
                    rows = channels,
                    onChange = { channelState = encodeChannels(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(DuaGenAIRoutes.SETTINGS_SKILLS) {
                SkillConfigScreen(
                    enabledIds = enabledSkills.toSet(),
                    onToggle = { id ->
                        enabledSkills = if (id in enabledSkills) enabledSkills - id else enabledSkills + id
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = DuaGenAIRoutes.CREATE,
                arguments = listOf(navArgument("kind") { type = NavType.StringType }),
            ) { entry ->
                CreateKindScreen(
                    kind = entry.arguments?.getString("kind").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }

        val isRobolectric = runCatching {
            Class.forName("org.robolectric.Robolectric")
        }.isSuccess

        if (isRobolectric) {
            NavHost(
                navController = navController,
                startDestination = DuaGenAIRoutes.HOME,
                modifier = Modifier.fillMaxSize(),
                builder = navGraph,
            )
        } else {
            NavHost(
                navController = navController,
                startDestination = DuaGenAIRoutes.HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(250),
                    ) + fadeIn(animationSpec = tween(250))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(250),
                    ) + fadeOut(animationSpec = tween(250))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(250),
                    ) + fadeIn(animationSpec = tween(250))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(250),
                    ) + fadeOut(animationSpec = tween(250))
                },
                builder = navGraph,
            )
        }
    }
    }
    }
}

/**
 * `3.2` / `15.0` -> `"3.2 GB of 15 GB"` — the Profile row's Storage subtitle.
 *
 * Kept here (not in the screen) because the ROW shows it, and the row is drawn
 * by ProfileScreen from a value the host passes in.
 */
private fun storageLabel(usedGb: Double, totalGb: Double): String {
    fun fmt(v: Double): String {
        val r = round(v * 10) / 10.0
        return if (r % 1.0 == 0.0) "${r.toInt()} GB" else "$r GB"
    }
    return "${fmt(usedGb)} of ${fmt(totalGb)}"
}

@Preview(name = "DuaGenAI nav host — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
@Suppress("ComposableCallWithoutComposePlugin")
private fun DuaGenAINavHostLight() {
    DuaGenAITheme(darkTheme = false) {
        DuaGenAINavHost(
            signedOutArtifacts = demoArtifacts,
            signedOutMemories = sampleMemories,
        )
    }
}

@Preview(name = "DuaGenAI nav host — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
@Suppress("ComposableCallWithoutComposePlugin")
private fun DuaGenAINavHostDark() {
    DuaGenAITheme(darkTheme = true) {
        DuaGenAINavHost(
            signedOutArtifacts = demoArtifacts,
            signedOutMemories = sampleMemories,
        )
    }
}


