package ai.duagenai.design.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.duagenai.design.cube.CubeCatalog
import ai.duagenai.design.cube.CubeIds
import ai.duagenai.design.cube.CubeStore
import ai.duagenai.design.BuildConfig
import ai.duagenai.design.screens.ApiConfig
import ai.duagenai.design.screens.ApiError
import ai.duagenai.design.screens.ArtifactItem
import ai.duagenai.design.screens.ArtifactsDataSource
import ai.duagenai.design.screens.ArtifactsRepository
import ai.duagenai.design.screens.ArtifactsScreen
import ai.duagenai.design.screens.ArtifactsUiState
import ai.duagenai.design.screens.BaseRemoteRepository
import ai.duagenai.design.screens.HomeMode
import ai.duagenai.design.screens.HomeTab
import ai.duagenai.design.screens.HttpClientProvider
import ai.duagenai.design.screens.HttpStatusException
import ai.duagenai.design.screens.HttpTransport
import ai.duagenai.design.screens.Identifiable
import ai.duagenai.design.screens.JsonHttpClient
import ai.duagenai.design.screens.MemoryDataSource
import ai.duagenai.design.screens.MemoryItem
import ai.duagenai.design.screens.MemoryRepository
import ai.duagenai.design.screens.MemoryScreen
import ai.duagenai.design.screens.MemorySharer
import ai.duagenai.design.screens.MemoryUiState
import ai.duagenai.design.screens.TokenProvider
import ai.duagenai.design.screens.TokenStore
import ai.duagenai.design.screens.sampleMemories
import ai.duagenai.design.screens.UrlLauncher
import ai.duagenai.design.screens.linesOf
import ai.duagenai.design.screens.madeAgentColorKey
import ai.duagenai.design.screens.madeAgentLine
import ai.duagenai.design.screens.madeAgentName
import ai.duagenai.design.screens.madeAgentShapeKey
import ai.duagenai.design.screens.plusLine
import ai.duagenai.design.screens.profileAgentRows
import ai.duagenai.design.cube.AgentSilhouette
import ai.duagenai.design.cube.AgentSwatch
import ai.duagenai.design.screens.UrlOpener
import ai.duagenai.design.screens.WriteResult
import ai.duagenai.design.screens.DuaAgentSkills
import ai.duagenai.design.screens.ProfileScreen
import ai.duagenai.design.screens.RemoteAccountDataSource
import ai.duagenai.design.screens.RemoteArtifactsDataSource
import ai.duagenai.design.screens.RemoteChatDataSource
import ai.duagenai.design.screens.SETTINGS_ACCOUNT_HELPER
import ai.duagenai.design.screens.SETTINGS_EDIT_PROFILE_LABEL
import ai.duagenai.design.screens.SETTINGS_SIGN_IN_LABEL
import ai.duagenai.design.screens.SETTINGS_SIGN_OUT_LABEL
import ai.duagenai.design.screens.SettingsScreen
import ai.duagenai.design.screens.RemoteDataSource
import ai.duagenai.design.screens.RemoteMemoryDataSource
import ai.duagenai.design.screens.demoArtifacts
import ai.duagenai.design.screens.AGENT_CHAT_ID
import ai.duagenai.design.screens.DUA_AGENT_TITLE
import ai.duagenai.design.screens.PERSONAL_AGENT_CHAT_ID
import ai.duagenai.design.screens.PERSONAL_AGENT_TITLE
import ai.duagenai.design.theme.DuaGenAITheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch as coroutineLaunch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/* ============================================================================
 *  The chat route, driven through the REAL navigation graph.
 *
 *  What this proves that ChatCubeMappingTest cannot: that the id a drawer
 *  history row hands to `onOpenChat` actually arrives at ChatScreen through
 *  `chat/{chatId}?title={title}`, that the title survives `Uri.encode`, and
 *  that the cube drawn in the thread is the cube the row drew. The mapping test
 *  pins the values; this one pins the plumbing between them.
 *
 *  WHERE CHATS ARE OPENED FROM — this changed with the product model
 *  ---------------------------------------------------------------
 *  Dua's home is deliberately EMPTY (Dua is a chatbot, not an agent host), so
 *  there is no chat list on Home to tap. Conversations are reached from the
 *  sidebar drawer's history section, which renders the same `sampleChats` list
 *  with the same per-chat cubes. Every "open a chat" test therefore goes:
 *
 *      tap "Open menu"  ->  tap the history row  ->  assert ChatScreen
 *
 *  WHY EVERY ASSERTION IS DISPLAY-AWARE
 *  ------------------------------------
 *  `ModalNavigationDrawer` composes its `drawerContent` even while the drawer is
 *  CLOSED — it is merely translated off-screen. So a naive
 *  `onAllNodesWithText("HISTORY").assertCountEquals(0)` would fail on a
 *  perfectly correct screen, and a naive `onNodeWithText("Search")` would throw
 *  "found 2 nodes" because the drawer has a Search pill too.
 *
 *  Every helper below therefore counts only nodes that are actually ON SCREEN
 *  (bounds inside the root, non-zero size). That is what the user sees, which is
 *  what these tests are about.
 *
 *  Three more rules this file follows, each one a bug caught while writing it:
 *
 *    1. `setContent` may be called ONCE per rule. Every test launches once and
 *       moves around with clicks / popBackStack; a loop never relaunches.
 *    2. The NavHostController is built BEFORE `setContent`, not inside it — a
 *       controller created in the composition lambda is rebuilt on every
 *       recomposition and the back stack silently resets.
 *    3. ChatScreen draws the SAME cube in five places (header + 2 agent bubbles
 *       + 2 user bubbles), so `onNodeWithContentDescription` would throw
 *       "expected exactly 1 node but found 5". Every cube assertion states the
 *       expected count instead of assuming one. That count is itself the
 *       assertion that no bubble fell back to the user's saved cube.
 *
 *  THE COMPOSER ON A THREAD
 *  ------------------------
 *  ChatScreen draws a composer under the conversation again, so Agent can be
 *  talked to. It is the SAME `DuaComposer` pill Dua's home draws \u2014 one
 *  implementation, two call sites \u2014 and only its placeholder differs
 *  (`"Message <title>..."`). [assertComposer] therefore checks the placeholder
 *  text plus the composer's own controls, and [assertNoComposer] is kept for the
 *  one surface a composer must still be absent from: Agent's HOME, which is the
 *  agent surface, not a conversation.
 *
 *  AGENT'S OWN THREAD
 *  ------------------
 *  Dua Agent has a conversation of its own. The pinned "Dua Agent" row opens
 *  it through the SAME `chat/{chatId}` route every other thread uses, with
 *  [AGENT_CHAT_ID]. The hero card opens the Personal Agent on
 *  [PERSONAL_AGENT_CHAT_ID]. No new route was invented. Dua Agent's pain area
 *  holds Agent's and the user's messages, and it carries the composer.
 *
 *  THE MEMORY TAB, THE FILTER AND THE INERT GUARD
 *  ----------------------------------------------
 *  Three later fixes are pinned here too:
 *
 *    • Memory renders a real LIST (`sampleMemories`), not the empty state —
 *      [theMemoryScreenRendersTheMemoryList] asserts the first and last entries
 *      are on screen and the empty-state line is gone.
 *    • The search row's filter is a real TOGGLE, not `onClick = {}` —
 *      [theFilterButtonTogglesItsSelectedState] reads its `selected` semantics
 *      before, after one tap, and after a second.
 *    • The search pill is inert and SAYS SO — [theSearchPillIsAnnouncedAsDisabled]
 *      asserts it publishes `disabled()` while the filter beside it stays enabled.
 *
 *  Run:
 *    ./gradlew :app:testDebugUnitTest --tests "ai.duagenai.design.nav.DuaGenAINavHostTest"
 *
 *  Requires (app/build.gradle.kts):
 *    testImplementation("org.robolectric:robolectric:4.13")
 *    testImplementation("androidx.compose.ui:ui-test-junit4")
 *    testImplementation("androidx.navigation:navigation-testing:2.8.5")
 *    debugImplementation("androidx.compose.ui:ui-test-manifest")
 *    testOptions { unitTests.isIncludeAndroidResources = true }
 * ==========================================================================*/

/**
 * The seven conversations, with the cube each must show.
 *
 * The expected cubes are hardcoded on purpose (see ChatCubeMappingTest): a test
 * that recomputes them with the production expression cannot detect a change to
 * that expression.
 */
private data class ChatCase(val chatId: String, val title: String, val cubeId: String)

private val CHATS = listOf(
    ChatCase("chat_webpage_developer", "Webpage Developer", "cube_34"),
    ChatCase("chat_design_team", "Design Team \u00b7 5", "cube_08"),
    ChatCase("chat_slides_assistant", "Slides Assistant", "cube_11"),
    ChatCase("chat_david_park", "David Park", "cube_35"),
    ChatCase("chat_market_research", "Market Research", "cube_06"),
    ChatCase("chat_accounting_assistant", "Accounting Assistant", "cube_32"),
    ChatCase("chat_family", "Family \u00b7 8", "cube_12"),
)

/**
 * The cheapest unambiguous "this is ChatScreen" marker.
 *
 * The top bar's status line: every thread carries it, for every conversation, so
 * it identifies ChatScreen without depending on which chat was opened. The
 * composer is asserted separately, by [assertComposer].
 */
private const val CHAT_MARKER = "Agent \u00b7 always on"

/**
 * The generic composer placeholder a thread must NOT show.
 *
 * `DuaComposer` is shared, so a thread's placeholder is `"Message <title>..."`.
 * This is the OLD generic string: if it ever reappeared on a thread it would mean
 * the placeholder stopped being thread-specific. Kept as a named constant so the
 * assertion reads as intent rather than as a magic string.
 */
private const val COMPOSER_PLACEHOLDER = "Type a message"

/**
 * The placeholder Agent's own thread must show.
 *
 * `ChatScreen` passes `"Message $chatTitle..."`, and the pinned row opens
 * [DUA_AGENT_TITLE], so this is the exact string the pill renders.
 */
private const val AGENT_COMPOSER_HINT = "Message $DUA_AGENT_TITLE\u2026"

/**
 * The Agent-home hero card's title (the user's personal agent).
 *
 * Used to FIND the card: it is the one filled card on Agent home and tapping
 * it opens the Personal Agent thread. The title is unique to the card.
 */
private const val AGENT_ROLE_LINE = "fareed's Personal Agent"

/** Agent home's section headers and header brand. */
private const val PINNED_HEADER = "Pinned \u00b7 1"
private const val CHATS_HEADER = "Chats \u00b7 6"
private const val APP_NAME_TEXT = "Dua Gen AI"
private const val DUA_AGENT_ROW = "Dua Agent"

/* The composer's controls, by their TalkBack descriptions. */
private const val ATTACH_LABEL = "Attach"
private const val SKILLS_LABEL = "Skills"
private const val MCP_LABEL = "MCP Tools"
private const val SEND_LABEL = "Send message"
private const val STOP_LABEL = "Stop"
private const val VOICE_LABEL = "Voice input"

/** The Skills sheet's rows — Dua-ai's `DuaAgentSkills` titles, in order. */
private val DUA_AGENT_SKILL_TITLES = listOf(
    "Frontend Developer",
    "Backend Architect",
    "API Tester",
    "DevOps Automator",
    "Code Reviewer",
)

/** The MCP sheet's built-in tool chips. */
private val BUILT_IN_TOOLS = listOf(
    "google_search", "fetch_url", "run_python", "generate_image", "save_note",
    "search_notes", "remember", "recall", "search_documents",
)

/** Dua mode's collapsed composer placeholder — the marker for "this is Dua home". */
private const val DUA_ASK_HINT = "Ask Dua ..."

/**
 * The Memory placeholder screen's markers (`MemoryScreen.kt`).
 *
 * The heading is the TopAppBar title; the empty-state line is the body copy.
 * Both are asserted so the test proves the SCREEN opened, not merely that the
 * tab's indicator moved.
 */
private const val MEMORY_HEADING = "Memory"
private const val MEMORY_EMPTY = "Abhi koi memory nahi hai"

/** The Artifacts placeholder screen's markers (`ArtifactsScreen.kt`). */
private const val ARTIFACTS_HEADING = "Artifacts"
private const val ARTIFACTS_EMPTY = "Abhi koi artifact nahi hai"

/**
 * The Artifacts grid's markers (`ArtifactsScreen.kt`).
 *
 * The screen renders the repository's `demoArtifacts` by default, so these are
 * the first and last entries' titles — asserting both proves the GRID rendered,
 * not merely that the screen opened. The empty-state string is kept as the
 * fallback marker.
 */
private const val ARTIFACTS_FIRST_ITEM = "Q3 Launch Deck"
private const val ARTIFACTS_LAST_ITEM = "Meeting Notes"

/** The picker's markers (`CubePickerScreen.kt`). */
private const val PICKER_HEADING = "Choose cube"
private const val PICKER_SAVE = "Save"
private const val PICKER_CANCEL = "Cancel"

/**
 * The Profile settings screens' markers.
 *
 * Each is the screen's TopAppBar title, so asserting it proves the SCREEN
 * opened rather than merely that the route changed.
 */
private const val APPEARANCE_HEADING = "Appearance"
private const val STORAGE_HEADING = "Storage"
private const val NOTIFICATIONS_HEADING = "Notifications"

/** The Appearance screen's three choices, in order. */
private val THEME_CHOICES = listOf("System default", "Light", "Dark")

/** The Notifications screen's three switches, in order. */
private val NOTIFICATION_SWITCHES =
    listOf("Push notifications", "Task updates", "Weekly summary")

/** The Storage screen's Clear-cache action and its confirmation. */
private const val CLEAR_CACHE = "Clear cache"
private const val CLEAR_CACHE_CONFIRM = "Clear"

/**
 * The cube picker's search + filter markers (`CubePickerScreen.kt`).
 *
 * The search field's placeholder and the four category chips. The no-results
 * line is the state a query that matches nothing must show.
 */
private const val PICKER_SEARCH_HINT = "Search cubes"
private val PICKER_CATEGORIES = listOf("All", "Orange", "Charcoal", "Multi")
private const val PICKER_NO_RESULTS = "Koi cube nahi mila"
private const val PICKER_CLEAR_FILTERS = "Clear filters"

/**
 * The Memory list's markers (`MemoryScreen.kt`).
 *
 * The screen renders `sampleMemories` by default, so these are the first and
 * last entries' titles — asserting both proves the LIST rendered, not merely
 * that the screen opened. The empty-state string is kept as the fallback marker.
 */
private const val MEMORY_FIRST_ITEM = "Q3 launch is the current priority"
private const val MEMORY_LAST_ITEM = "Invoice #2211 flagged for review"

/** The filter toggle's content description (`SearchRow` in HomeScreen.kt). */
private const val FILTER_LABEL = "Filter"

/**
 * The Artifacts filter sheet's markers (audit fix #7).
 *
 * The sheet's title, its Clear action and the "Image" chip. The chip's label is
 * also the artifact type it filters on, so tapping it must leave only the image
 * artifacts on screen.
 */
private const val ARTIFACTS_FILTER_SHEET_TITLE = "Filter by type"
private const val ARTIFACTS_FILTER_CLEAR = "Clear"
private const val ARTIFACTS_FILTER_IMAGE = "Image"

/** The no-match panel's action (audit fix #9). */
private const val ARTIFACTS_NO_MATCH_CLEAR = "Clear filter"

/** An image artifact's title — the card the "Image" filter must keep. */
private const val ARTIFACTS_IMAGE_ITEM = "Brand Hero Banner"

/**
 * The preview caption's two states (audit fix #8).
 *
 * The caption is `"<title> · <type> · <size>"`, built from the artifact itself.
 * These are the distinctive halves of the first card's and the image card's
 * captions, so asserting one and then the other proves the caption FOLLOWS the
 * selection instead of being a fixed string.
 */
private const val ARTIFACTS_CAPTION_FIRST = "Q3 Launch Deck · Webpage"
private const val ARTIFACTS_CAPTION_IMAGE = "Brand Hero Banner · Image"

/**
 * The Memory filter's markers (the Artifacts pattern, applied to Memory).
 *
 * The chip row offers the same shape the Artifacts sheet does, so the two
 * filter surfaces are asserted the same way: a chip narrows the list, the
 * search field narrows it, and a chip with no entries shows the no-match panel
 * with a way out.
 */
private const val MEMORY_CHIP_PREFERENCE = "Filter: Preference"
private const val MEMORY_CHIP_TASK = "Filter: Task"
private const val MEMORY_NO_MATCH = "Koi memory nahi mili"
private const val MEMORY_NO_MATCH_CLEAR = "Clear filters"
private const val MEMORY_SEARCH_HINT = "Search memories"
private const val MEMORY_PREFERENCE_ITEM = "Brand green is #087C45"
private const val MEMORY_PROJECT_ITEM = "Q3 launch is the current priority"

/**
 * The swipe-to-delete and undo markers.
 *
 * The swipe opens the SAME confirmation the long-press opens, so the dialog's
 * own strings are the assertion target; the snackbar's Undo is what proves the
 * delete is reversible.
 */
private const val ARTIFACT_DELETE_DIALOG_TITLE = "Delete artifact?"
private const val ARTIFACT_DELETE_CONFIRM = "Delete"
private const val ARTIFACT_DELETE_CANCEL = "Cancel"
private const val ARTIFACT_UNDO = "Undo"
private const val ARTIFACT_DELETED_SNACK = "Artifact deleted"

/** The pull-to-refresh indicator's description. */
private const val ARTIFACTS_REFRESHING = "Refreshing artifacts"

/**
 * The long-press context menu's three actions (this fix).
 *
 * The menu items are addressed by their own content descriptions, because the
 * bare words "Open" / "Delete" also appear elsewhere (the delete dialog's
 * confirm button is "Delete"), so a text match would be ambiguous.
 */
private const val MENU_OPEN_IMAGE = "Open Brand Hero Banner"
private const val MENU_OPEN_NEW_TAB_IMAGE = "Open Brand Hero Banner in new tab"
private const val MENU_OPEN_NEW_TAB_NOTES = "Open Meeting Notes in new tab"
private const val MENU_OPEN_NEW_TAB_HELPER = "No link for this artifact"
private const val MENU_SHARE_IMAGE = "Share Brand Hero Banner"
private const val MENU_DELETE_IMAGE = "Delete Brand Hero Banner"
private const val MENU_SHARE_HELPER = "Not available in this build"

// Memory swipe-to-delete.
private const val MEMORY_DELETE_DIALOG_TITLE = "Delete memory?"
private const val MEMORY_DELETED_SNACK = "Memory deleted"
private const val MEMORY_UNDO = "Undo"

/**
 * The Memory long-press context menu's markers (this fix).
 *
 * The menu items are addressed by their own content descriptions, because the
 * bare words "Open" / "Delete" also appear elsewhere (the delete dialog's confirm
 * button is "Delete"), so a text match would be ambiguous. The detail dialog's
 * Close button is the marker for the URL-less "Open" fallback.
 */
private const val MEMORY_MENU_OPEN = "Open Q3 launch is the current priority"
private const val MEMORY_MENU_DELETE = "Delete Q3 launch is the current priority"
private const val MEMORY_MENU_OPEN_NO_URL = "Open Brand green is #087C45"
private const val MEMORY_DETAIL_CLOSE = "Close"

/**
 * The detail dialog's "Open source" button (this fix).
 *
 * Shown ONLY when the memory has a URL, so its presence/absence is the
 * assertion: the URL-bearing memory shows it, the URL-less one does not.
 */
private const val MEMORY_OPEN_SOURCE = "Open source in browser"

/**
 * The detail dialog's "Copy text" button (this fix).
 *
 * ALWAYS present \u2014 every memory has text \u2014 so its presence is not the
 * assertion; what it COPIES and the "Copied" feedback are. The button is
 * addressed by its own description, because the bare words "Copy text" would
 * also match the label.
 */
private const val MEMORY_COPY_TEXT = "Copy memory text"
private const val MEMORY_COPIED_SNACK = "Copied"

/**
 * The detail dialog's "Share" button (this fix).
 *
 * ALWAYS present \u2014 it hands the memory to the platform's own share sheet (an
 * ACTION_SEND intent), with the text plus the URL when there is one. Addressed
 * by its own description, because the bare word "Share" would also match the
 * label.
 */
private const val MEMORY_SHARE = "Share memory"

/**
 * The Cube picker chip's long-press tooltip (this fix).
 *
 * The tooltip carries the chip's FULL label, so its description is the label
 * itself. The chip's own description is the filter marker, so the two are
 * distinct nodes.
 */
private fun pickerTooltip(cat: String) = "Tooltip: $cat"

/**
 * The shared URL opener's markers (this fix).
 *
 * The helper is exercised directly (a fake launcher records the URL) and through
 * both screens, so the test proves the two screens route through ONE helper
 * rather than each carrying its own copy.
 */
private const val SHARED_OPENER_URL = "https://static.teamily.ai/sites/demo/q3-launch-brief/index.html"

/** The Profile screen's two inert controls and the reason shown for one. */
private const val EDIT_PROFILE_LABEL = "Edit profile"
private const val SIGN_IN_LABEL = "Sign in"
/** The raw token field Profile used to show. It must not exist any more. */
private const val SESSION_TOKEN_LABEL = "Session token"
private const val EDIT_PROFILE_HELPER = "Profile editing is not available in this build."

/**
 * Agent home's chat rows that are on screen above the floating dock.
 * Later rows sit under the dock, as in the reference.
 */
private val MANAGED_AGENTS = listOf("Code Reviewer", "Backend Architect", "DevOps Automator", "API Tester")

/**
 * How many cubes ChatScreen draws for one conversation.
 *
 * `sampleThread` is User, Agent, User, Agent — two of each — plus the header.
 * If a bubble ever stopped using the thread's cube, this count would drop and
 * the test would say so.
 */
private const val THREAD_BUBBLE_CUBES = 4
private const val THREAD_HEADER_CUBES = 1

/**
 * A data source that fails its first [failures] fetches, then succeeds.
 *
 * It is the seam the repository tests drive: [fetchCount] proves the repository
 * actually went through the source, and the failure count lets one test see both
 * the Error state and the Retry recovery.
 */
private class FlakyArtifactsDataSource(private val failures: Int) : ArtifactsDataSource {
    var fetchCount: Int = 0
        private set

    override suspend fun fetch(): List<ArtifactItem> {
        fetchCount++
        if (fetchCount <= failures) throw IllegalStateException("boom")
        return demoArtifacts
    }

    override suspend fun add(item: ArtifactItem): ArtifactItem = item
    override suspend fun delete(id: String) = Unit
}

/**
 * A memory data source that fails its first [failures] fetches, then succeeds.
 *
 * The Memory twin of [FlakyArtifactsDataSource]: [fetchCount] proves the
 * repository went through the source, and the failure count lets one test see
 * both the Error state and the Retry recovery. [addCount] / [deleteCount] prove
 * the undo and the delete reach the SOURCE, not just the screen's copy.
 */
private class FlakyMemoryDataSource(private val failures: Int) : MemoryDataSource {
    var fetchCount: Int = 0
        private set
    var addCount: Int = 0
        private set
    var deleteCount: Int = 0
        private set

    override suspend fun fetch(): List<MemoryItem> {
        fetchCount++
        if (fetchCount <= failures) throw IllegalStateException("boom")
        return sampleMemories
    }

    override suspend fun add(item: MemoryItem): MemoryItem {
        addCount++
        return item
    }

    override suspend fun delete(id: String) {
        deleteCount++
    }
}

/**
 * An artifacts source whose WRITES fail while its reads succeed.
 *
 * This is the seam the "failed write must be visible" tests drive: the list
 * loads normally, so the screen is in its Loaded state, and then `delete` /
 * `add` throw. Before the fix the screen showed "Artifact deleted" anyway and
 * offered an Undo that re-added a row which had never been removed.
 *
 * The thrown message is deliberately a raw, ugly one (`boom`) so a test can
 * prove the UI does NOT show it: the repository must substitute its own safe
 * sentence.
 */
private class FailingWriteArtifactsDataSource(
    private val failDelete: Boolean = true,
    private val failAdd: Boolean = false,
) : ArtifactsDataSource {
    var deleteCount: Int = 0
        private set
    var addCount: Int = 0
        private set

    override suspend fun fetch(): List<ArtifactItem> = demoArtifacts

    override suspend fun add(item: ArtifactItem): ArtifactItem {
        addCount++
        if (failAdd) throw IllegalStateException("boom")
        return item
    }

    override suspend fun delete(id: String) {
        deleteCount++
        if (failDelete) throw IllegalStateException("boom")
    }
}

/** The Memory twin of [FailingWriteArtifactsDataSource]. */
private class FailingWriteMemoryDataSource(
    private val failDelete: Boolean = true,
    private val failAdd: Boolean = false,
) : MemoryDataSource {
    var deleteCount: Int = 0
        private set
    var addCount: Int = 0
        private set

    override suspend fun fetch(): List<MemoryItem> = sampleMemories

    override suspend fun add(item: MemoryItem): MemoryItem {
        addCount++
        if (failAdd) throw IllegalStateException("boom")
        return item
    }

    override suspend fun delete(id: String) {
        deleteCount++
        if (failDelete) throw IllegalStateException("boom")
    }
}

/**
 * A fake [HttpTransport] that records every call and returns a canned body.
 *
 * It is the seam the shared-client tests drive: [calls] proves a remote source
 * actually went through [JsonHttpClient] (and with which method and URL),
 * [tokens] proves which bearer token each call carried, and [failWith] lets a
 * test see the labelled status failure — all without a socket.
 */
private class RecordingTransport(
    private val response: String = "[]",
    private val failWith: Int? = null,
    private val respond: ((url: String) -> String)? = null,
) : HttpTransport {
    val calls: MutableList<Pair<String, String>> = mutableListOf()

    /** The token handed to the transport for each call, in call order. */
    val tokens: MutableList<String?> = mutableListOf()

    /** The JSON body of each call, in call order. */
    val bodies: MutableList<String?> = mutableListOf()

    override fun execute(method: String, url: String, body: String?, token: String?): String {
        calls += method to url
        tokens += token
        bodies += body
        failWith?.let { throw HttpStatusException(it) }
        return respond?.invoke(url) ?: response
    }
}

/**
 * A fake [MemorySharer] that records every payload it is handed.
 *
 * It is the seam the Share test drives: [shared] proves the detail dialog's
 * "Share" button actually reached the platform share path (and with which text
 * and URL), all without leaving the process \u2014 the same approach
 * [RecordingTransport] takes for the HTTP client.
 */
private class RecordingSharer : MemorySharer {
    val shared: MutableList<Pair<String, String?>> = mutableListOf()

    override fun share(text: String, url: String?) {
        shared += text to url
    }
}

@RunWith(AndroidJUnit4::class)
// Robolectric's default window is too short for this kit: the drawer footer,
// the last artifact card and the last chat row sit below the fold, and
// isOnScreen then reports them missing. A Pixel-class window keeps every
// asserted row inside the root. It does not change what the app draws.
@Config(sdk = [34], qualifiers = "w411dp-h1400dp")
class DuaGenAINavHostTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Session and host overrides are process-wide. A test that signs in or
     * repoints [ApiConfig] must not leak into the next one: the UI tests depend
     * on a null token, which is what keeps chat on the canned reply.
     */
    @After
    fun resetSessionAndHostOverrides() {
        TokenStore.token = null
        ApiConfig.overrideBaseUrl = null
        ApiConfig.overrideEngineBaseUrl = null
    }

    /**
     * A real [NavHostController] bound to the test activity, so the test can
     * both drive the graph directly and read the resulting back stack.
     */
    private lateinit var navController: NavHostController

    /** Launches the graph. Call at most once per test — see rule 1 above. */
    private fun launch() {
        navController = TestNavHostController(compose.activity).apply {
            // Navigation Compose needs its navigator registered before the graph
            // is attached; TestNavHostController does not do it for you.
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                // The nav host's signed-out source is EMPTY in production (a build
                // must never ship fake rows), so the UI tests that assert on a
                // populated grid/list pass the fixtures explicitly. This is the
                // same seam a `@Preview` uses.
                DuaGenAINavHost(
                    navController = navController,
                    signedOutArtifacts = demoArtifacts,
                    signedOutMemories = sampleMemories,
                )
            }
        }
        compose.waitForIdle()
    }

    // ------------------------------------------------- on-screen counting
    //
    // The drawer is composed while closed, so "exists in the tree" is not the
    // same as "the user can see it". These helpers count only what is on screen.

    /**
     * Clicks [this] node.
     *
     * The library `performClick` injects a touch. A modal bottom sheet is a
     * second window, and Robolectric delivers that touch to the activity, so
     * the control inside the sheet never runs. The semantics click is the
     * action the control publishes and it runs in the sheet's composition.
     * A node with no click action still receives a touch.
     */
    private fun SemanticsNodeInteraction.performClick(): SemanticsNodeInteraction {
        val click = fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)
        return if (click != null) performSemanticsAction(SemanticsActions.OnClick)
        else performTouchInput { click() }
    }

    /**
     * True when [node] is on screen and has a real size.
     *
     * A [androidx.compose.material3.ModalBottomSheet] or
     * [androidx.compose.material3.TooltipBox] popup is a second semantics root.
     * `onRoot()` requires exactly one and throws while one is open. The activity
     * root is the first root. Content that lives in a dialog or popup root is
     * on screen when it has a real size — its coordinates are not the activity's.
     */
    private fun isOnScreen(node: androidx.compose.ui.semantics.SemanticsNode): Boolean {
        val b = node.boundsInRoot
        if (b.width <= 0f || b.height <= 0f) return false
        val mainRoot = compose.onAllNodes(isRoot()).fetchSemanticsNodes().first()
        val main = mainRoot.boundsInRoot
        var root = node
        var parent = root.parent
        while (parent != null) {
            root = parent
            parent = root.parent
        }
        // Dialog, sheet, and tooltip content lives in its own root.
        if (root.id != mainRoot.id) return true
        return b.left >= main.left - 1f && b.right <= main.right + 1f &&
            b.top >= main.top - 1f && b.bottom <= main.bottom + 1f
    }

    /** How many nodes carrying [text] are actually visible. */
    private fun visibleTextCount(text: String): Int =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().count { isOnScreen(it) }

    /** How many nodes carrying [description] are actually visible. */
    private fun visibleDescCount(description: String): Int =
        compose.onAllNodesWithContentDescription(description)
            .fetchSemanticsNodes().count { isOnScreen(it) }

    /** Asserts [text] is on screen at least once. */
    private fun assertVisible(text: String) {
        assertTrue("expected \"$text\" to be on screen", visibleTextCount(text) >= 1)
    }

    /** Asserts [text] is nowhere on screen. */
    private fun assertNotVisible(text: String) {
        assertEquals("expected \"$text\" to be off screen", 0, visibleTextCount(text))
    }

    /**
     * Asserts the bottom-nav item labelled [label] is the SELECTED one.
     *
     * The selection is hoisted state now, so it is read from the semantics the
     * nav item publishes (`selected = active`) rather than from a hardcoded
     * constant. `hasText` finds the item's merged node; `isSelected` reads the
     * flag the tint is drawn from, so the two can never disagree.
     */
    private fun assertTabSelected(label: String) {
        val n = compose.onAllNodes(hasText(label) and isSelected())
            .fetchSemanticsNodes().count { isOnScreen(it) }
        assertTrue("expected the \"$label\" tab to be the selected one", n >= 1)
    }

    /**
     * Asserts some visible node's text CONTAINS [fragment].
     *
     * `onAllNodesWithText` matches the WHOLE string by default, which is wrong
     * for a message bubble: the bubble's text is a full sentence, so asserting a
     * distinctive fragment of it needs substring matching.
     */
    private fun assertVisibleSubstring(fragment: String) {
        val n = compose.onAllNodes(hasText(fragment, substring = true))
            .fetchSemanticsNodes().count { isOnScreen(it) }
        assertTrue("expected a visible node containing \"$fragment\"", n >= 1)
    }

    /** Asserts exactly [count] visible nodes carry [description]. */
    private fun assertVisibleDescCount(description: String, count: Int) {
        assertEquals(
            "expected $count visible node(s) with description \"$description\"",
            count,
            visibleDescCount(description),
        )
    }

    /**
     * Asserts the thread HAS a composer, with [placeholder] as its hint.
     *
     * The composer is the shared `DuaComposer` pill. A thread's pill is
     * identified by what it draws, left to right: the "Attach" `+`, the field
     * (exactly one editable node, showing the placeholder), then the agent tools
     * "Skills" and "MCP Tools", then the single Send/Stop/Voice control. With an
     * empty field that control is the voice waveform ("Voice input"), so no
     * "Send message" node exists yet — Send appears only once text is typed.
     * There is no separate mic glyph any more.
     *
     * The old generic placeholder must be gone: a thread's hint is
     * `"Message <title>..."`, so seeing `"Type a message"` would mean the
     * placeholder stopped being thread-specific.
     */
    private fun assertComposer(placeholder: String) {
        assertVisible(placeholder)
        assertNotVisible(COMPOSER_PLACEHOLDER)
        assertVisibleDescCount(ATTACH_LABEL, 1)
        assertVisibleDescCount(SKILLS_LABEL, 1)
        assertVisibleDescCount(MCP_LABEL, 1)
        assertVisibleDescCount(VOICE_LABEL, 1)
        assertVisibleDescCount(SEND_LABEL, 0)
        assertEquals(
            "the composer must have exactly one editable field",
            1,
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().count { isOnScreen(it) },
        )
    }

    /**
     * Asserts a surface has NO composer at all.
     *
     * Used for Agent's HOME, which must not draw the Dua pill: the composer
     * belongs to a conversation, and Agent's home is the agent surface. Checks
     * the things a composer would leave behind rather than trusting one string:
     *
     *   1. no node accepts text input (`hasSetTextAction`);
     *   2. no composer placeholder is on screen;
     *   3. the composer's own controls ("Attach", "Voice input", "Skills",
     *      "MCP Tools") are absent — they would only exist inside a composer.
     */
    private fun assertNoComposer() {
        val editable = compose.onAllNodes(hasSetTextAction())
            .fetchSemanticsNodes().count { isOnScreen(it) }
        assertEquals("this surface must have no text input", 0, editable)

        assertNotVisible(COMPOSER_PLACEHOLDER)
        assertVisibleDescCount(ATTACH_LABEL, 0)
        assertVisibleDescCount(VOICE_LABEL, 0)
        assertVisibleDescCount(SKILLS_LABEL, 0)
        assertVisibleDescCount(MCP_LABEL, 0)
    }

    /** Opens Dua Agent's thread the way a user does: Agent mode -> pinned row. */
    private fun openAgentThread() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(DUA_AGENT_ROW).performClick()
        compose.waitForIdle()
    }

    private fun currentRoute(): String? =
        navController.currentBackStackEntry?.destination?.route

    /**
     * Opens the sidebar drawer: Dua home's hamburger or Agent home's avatar —
     * both are labelled "Open menu".
     */
    private fun openDrawer() {
        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.waitForIdle()
    }

    /**
     * Switches mode the way a user does. Dua home has the toggle; Agent home
     * has none, so going back to Dua uses the drawer's "Dua" row.
     */
    private fun switchTo(mode: HomeMode) {
        if (visibleDescCount("${mode.label} mode") > 0) {
            compose.onNodeWithContentDescription("${mode.label} mode").performClick()
        } else {
            openDrawer()
            compose.onNode(hasText(mode.label) and hasClickAction()).performClick()
        }
        compose.waitForIdle()
    }

    /** Opens a conversation the way a user does: drawer -> history row. */
    private fun openChatFromDrawer(chat: ChatCase) {
        openDrawer()
        compose.onNodeWithText(chat.title).performClick()
        compose.waitForIdle()
    }

    // ------------------------------------------------- Dua mode is plain

    @Test
    fun duaModeShowsOnlyTheComposerAndNoAgentSurface() {
        launch()

        // The composer is there…
        assertVisible(DUA_ASK_HINT)

        // …and nothing agent-related is. No section header, no search row, no
        // bottom navigation, no agent row, no chat row.
        assertNotVisible(CHATS_HEADER)
        assertNotVisible(PINNED_HEADER)
        assertNotVisible(APP_NAME_TEXT)
        assertNotVisible("CHATS \u00b7 7")
        assertNotVisible("Search")
        assertNotVisible(HomeTab.Managed.label)
        assertNotVisible(HomeTab.Discover.label)
        assertNotVisible(HomeTab.Memory.label)
        assertNotVisible(HomeTab.Artifacts.label)

        // No chat row either: Dua's home is empty, so no conversation title is
        // on screen and no per-chat cube is drawn.
        for (chat in CHATS) {
            assertNotVisible(chat.title)
            assertVisibleDescCount(avatarLabel(chat.title, chat.cubeId), 0)
        }
    }

    @Test
    fun duaModeHasNoAgentEntryPointAtAll() {
        launch()

        // The three agents Agent manages must not appear anywhere in Dua mode —
        // not as a row, not as a badge, not as a tab label.
        for (agent in MANAGED_AGENTS) {
            assertNotVisible(agent)
        }

        // The top bar's right button is the gear, not the plus.
        assertVisibleDescCount("Settings", 1)
        assertVisibleDescCount("New chat", 0)

        // Dua's pill carries no agent tools: Skills and MCP are Dua Agent
        // capabilities, so they are drawn only under a thread. Its Attach has no
        // sheet to open here, so it is announced unavailable, and its voice
        // waveform is not wired either. There is no separate mic glyph.
        assertVisibleDescCount(SKILLS_LABEL, 0)
        assertVisibleDescCount(MCP_LABEL, 0)
        assertEquals(
            1,
            compose.onAllNodes(hasContentDescription(ATTACH_LABEL) and isNotEnabled())
                .fetchSemanticsNodes().count { isOnScreen(it) },
        )
        assertEquals(
            1,
            compose.onAllNodes(hasContentDescription(VOICE_LABEL) and isNotEnabled())
                .fetchSemanticsNodes().count { isOnScreen(it) },
        )
    }

    // ------------------------------------------- Agent mode is the agent surface

    @Test
    fun agentModeShowsTheChatListAndTheAgentsTab() {
        launch()
        switchTo(HomeMode.Agent)

        // The reference's chat-list home: search row, hero card, Pinned · 1
        // with Dua Agent, the chat section with its rows, and the floating dock.
        assertVisible("Search")
        assertVisible(AGENT_ROLE_LINE)
        assertVisible(PINNED_HEADER)
        assertVisible(DUA_AGENT_ROW)
        assertVisibleDescCount("Verified", 1)
        assertVisible(CHATS_HEADER)
        for (agent in MANAGED_AGENTS) {
            assertVisible(agent)
        }
        // The old heavy agent list is gone.
        assertNotVisible("AGENTS \u00b7 3")

        // The second tab reads "Agents", as in the reference dock.
        assertVisible(HomeTab.Managed.label)
        assertVisible("Agents")
        assertNotVisible("Managed")

        // Agent's HOME has no composer: the Dua pill belongs to a conversation,
        // and this is the agent surface.
        assertNotVisible(DUA_ASK_HINT)
        assertNoComposer()

        // And its right button is the plus, not the gear.
        assertVisibleDescCount("New chat", 1)
        assertVisibleDescCount("Settings", 0)
    }

    /** Agent home's header: avatar (opens the drawer) · "Dua Gen AI" · plus. No toggle. */
    @Test
    fun agentHomeHeaderIsAvatarBrandAndPlusWithNoToggle() {
        launch()
        switchTo(HomeMode.Agent)

        assertVisible(APP_NAME_TEXT)
        assertNotVisible("Teamily AI")
        assertVisibleDescCount("Open menu", 1)
        assertVisibleDescCount("New chat", 1)
        assertVisibleDescCount("Dua mode", 0)
        assertVisibleDescCount("Agent mode", 0)

        // The avatar opens the existing drawer.
        openDrawer()
        assertVisibleDescCount("Close menu", 1)
    }

    // ------------------------------------------------- Create Agent

    /** The Create button: the bottom bar's one clickable "Create" node. */
    private fun createButton() = compose.onNode(hasText(CREATE_AGENT_ACTION) and hasClickAction())

    /** Agent home -> plus -> "Create Agent", the way a user does. */
    private fun openCreateAgent() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithContentDescription("New chat").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(CREATE_AGENT_TITLE).performClick()
        compose.waitForIdle()
    }

    /** The plus menu's Create Agent opens the form; it no longer auto-creates "New Agent N". */
    @Test
    fun thePlusMenuCreateAgentOpensTheCreateAgentScreen() {
        openCreateAgent()

        assertEquals(DuaGenAIRoutes.CREATE_AGENT, currentRoute())
        assertVisible(CREATE_AGENT_TITLE)
        assertVisible(CREATE_AGENT_NAME_HINT)
        assertNotVisible("New Agent 1")
        // 15 shapes and 9 swatches, with the defaults preselected.
        assertEquals(15, AgentSilhouette.values().size)
        AgentSilhouette.values().forEach { assertVisibleDescCount("Shape: ${it.label}", 1) }
        AgentSwatch.values().forEach { assertVisibleDescCount("Color: ${it.label}", 1) }
        compose.onNodeWithContentDescription("Shape: ${AgentSilhouette.DEFAULT.label}").assertIsSelected()
        compose.onNodeWithContentDescription("Color: ${AgentSwatch.DEFAULT.label}").assertIsSelected()
    }

    /** Create stays disabled for an empty and for a whitespace-only name. */
    @Test
    fun createIsDisabledForAnEmptyOrBlankName() {
        openCreateAgent()

        createButton().assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("   ")
        compose.waitForIdle()
        createButton().assertIsNotEnabled()

        compose.onNode(hasSetTextAction()).performTextInput("Ava")
        compose.waitForIdle()
        createButton().assertIsEnabled()
    }

    /** Create saves name, shape and colour, then lands on Profile → Agents. */
    @Test
    fun createSavesTheAgentAndLandsOnProfileAgents() {
        openCreateAgent()

        compose.onNode(hasSetTextAction()).performTextInput("  Research Buddy ")
        compose.onNodeWithContentDescription("Shape: ${AgentSilhouette.Star.label}").performClick()
        compose.onNodeWithContentDescription("Color: ${AgentSwatch.Pink.label}").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Shape: ${AgentSilhouette.Star.label}").assertIsSelected()
        compose.onNodeWithContentDescription("Color: ${AgentSwatch.Pink.label}").assertIsSelected()
        assertVisibleDescCount("Agent preview: Star, Pink", 1)

        createButton().performClick()
        compose.waitForIdle()

        assertEquals(DuaGenAIRoutes.PROFILE_AGENTS, currentRoute())
        assertVisible("Research Buddy")
        // The form is popped, so Back from the list does not reopen it.
        assertTrue(
            navController.currentBackStack.value.none {
                it.destination.route == DuaGenAIRoutes.CREATE_AGENT
            },
        )
    }

    /** Close leaves Create Agent without saving anything. */
    @Test
    fun closeFromCreateAgentSavesNothing() {
        openCreateAgent()
        compose.onNode(hasSetTextAction()).performTextInput("Draft")
        compose.onNodeWithContentDescription("Close").performClick()
        compose.waitForIdle()

        assertTrue(currentRoute() != DuaGenAIRoutes.CREATE_AGENT)
        assertNotVisible("Draft")
    }

    /** A madeAgents line keeps name, shape and colour; a bare legacy name still reads. */
    @Test
    fun madeAgentLinesRoundTripNameShapeAndColor() {
        val line = madeAgentLine(" Research\tBuddy ", AgentSilhouette.Star.name, AgentSwatch.Pink.name)
        assertEquals("Research Buddy", madeAgentName(line))
        assertEquals(AgentSilhouette.Star, AgentSilhouette.fromKey(madeAgentShapeKey(line)))
        assertEquals(AgentSwatch.Pink, AgentSwatch.fromKey(madeAgentColorKey(line)))

        val stored = plusLine(plusLine("", "Old Agent"), line)
        val titles = profileAgentRows(linesOf(stored)).map { it.title }
        assertTrue(titles.containsAll(listOf("Old Agent", "Research Buddy")))
        assertEquals(null, madeAgentShapeKey("Old Agent"))
        assertEquals(AgentSilhouette.DEFAULT, AgentSilhouette.fromKey(null))
    }

    /** Dua home is unchanged: hamburger, the toggle, the gear and the composer. */
    @Test
    fun duaHomeKeepsTheToggleAndTheComposer() {
        launch()

        assertVisibleDescCount("Open menu", 1)
        assertVisibleDescCount("Dua mode", 1)
        assertVisibleDescCount("Agent mode", 1)
        assertVisibleDescCount("Settings", 1)
        assertVisible(DUA_ASK_HINT)
    }

    @Test
    fun theModeSwitchesBothWays() {
        launch()

        // Dua -> Agent, through the toggle.
        switchTo(HomeMode.Agent)
        assertVisible(CHATS_HEADER)
        assertNotVisible(DUA_ASK_HINT)

        // Agent -> Dua, through the drawer's "Dua" row (Agent home has no toggle).
        switchTo(HomeMode.Dua)
        assertVisible(DUA_ASK_HINT)
        assertNotVisible(CHATS_HEADER)
        assertVisibleDescCount("Dua mode", 1)
    }

    /** The Pinned and Chats section headers collapse and expand their rows. */
    @Test
    fun theSectionHeadersCollapseAndExpand() {
        launch()
        switchTo(HomeMode.Agent)

        compose.onNodeWithText(PINNED_HEADER).performClick()
        compose.waitForIdle()
        assertNotVisible(DUA_AGENT_ROW)
        compose.onNodeWithText(PINNED_HEADER).performClick()
        compose.waitForIdle()
        assertVisible(DUA_AGENT_ROW)

        compose.onNodeWithText(CHATS_HEADER).performClick()
        compose.waitForIdle()
        for (agent in MANAGED_AGENTS) assertNotVisible(agent)
        compose.onNodeWithText(CHATS_HEADER).performClick()
        compose.waitForIdle()
        assertVisible(MANAGED_AGENTS.first())
    }

    @Test
    fun theManagedTabOpensTheAgentsScreen() {
        launch()
        switchTo(HomeMode.Agent)

        compose.onNodeWithText(HomeTab.Managed.label).performClick()
        compose.waitForIdle()

        assertEquals(
            "the Agents tab must open the agents screen",
            DuaGenAIRoutes.AGENTS,
            currentRoute(),
        )
    }

    @Test
    fun theBottomNavSelectionIsHoistedAndMoves() {
        launch()
        switchTo(HomeMode.Agent)

        // Chats is the default selection — the tab Agent's home has always shown
        // as active, now supplied by the host instead of hardcoded in HomeScreen.
        assertTabSelected(HomeTab.Chats.label)

        // Tapping Discover moves the selection AND navigates. The bottom nav is
        // drawn by HomeScreen only, so the indicator is asserted after coming
        // back: if the selection were still a constant inside HomeScreen, Home
        // would re-render with Chats active and this would fail.
        compose.onNodeWithText(HomeTab.Discover.label).performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.DISCOVER, currentRoute())

        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        assertTabSelected(HomeTab.Discover.label)

        // And back to Chats, so the move is not one-way.
        compose.onNodeWithText(HomeTab.Chats.label).performClick()
        compose.waitForIdle()
        assertTabSelected(HomeTab.Chats.label)
    }

    @Test
    fun theMemoryTabOpensTheMemoryScreen() {
        launch()
        switchTo(HomeMode.Agent)

        compose.onNodeWithText(HomeTab.Memory.label).performClick()
        compose.waitForIdle()

        assertEquals(
            "the Memory tab must open the memory screen",
            DuaGenAIRoutes.MEMORY,
            currentRoute(),
        )
        // The screen itself, not just the route: its title and its real list.
        assertVisible(MEMORY_HEADING)
        assertVisible(MEMORY_FIRST_ITEM)
        assertVisible(MEMORY_LAST_ITEM)
        // The list is showing, so the empty state must NOT be.
        assertNotVisible(MEMORY_EMPTY)
        // …and it is NOT the Artifacts screen.
        assertNotVisible(ARTIFACTS_EMPTY)
    }

    /**
     * The Memory screen renders a LIST, not the empty state.
     *
     * `sampleMemories` is the default, so every entry's title must be on screen
     * and the empty-state line must be gone. This is the assertion that the list
     * wiring actually happened rather than the placeholder staying put.
     */
    @Test
    fun theMemoryScreenRendersTheMemoryList() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Memory.label).performClick()
        compose.waitForIdle()

        assertVisible(MEMORY_FIRST_ITEM)
        assertVisible(MEMORY_LAST_ITEM)
        // A category chip and a timestamp from the first card.
        assertVisible("Project")
        assertVisible("2 din pehle")
        assertNotVisible(MEMORY_EMPTY)
    }

    /**
     * The filter button is a real toggle, not a dead control (audit fix #7).
     *
     * It used to be `onClick = {}`. Now it flips `filterActive`, which publishes
     * `selected` semantics — so the state is readable from the node rather than
     * from a screenshot. Tapping twice must return it to unselected.
     */
    @Test
    fun theFilterButtonTogglesItsSelectedState() {
        launch()
        switchTo(HomeMode.Agent)

        // Off by default.
        assertEquals(
            "the filter must start unselected",
            0,
            compose.onAllNodes(hasContentDescription(FILTER_LABEL) and isSelected())
                .fetchSemanticsNodes().count { isOnScreen(it) },
        )

        compose.onNodeWithContentDescription(FILTER_LABEL).performClick()
        compose.waitForIdle()
        assertTrue(
            "tapping the filter must select it",
            compose.onAllNodes(hasContentDescription(FILTER_LABEL) and isSelected())
                .fetchSemanticsNodes().count { isOnScreen(it) } >= 1,
        )

        // And back off, so the toggle is not one-way.
        compose.onNodeWithContentDescription(FILTER_LABEL).performClick()
        compose.waitForIdle()
        assertEquals(
            "tapping the filter again must deselect it",
            0,
            compose.onAllNodes(hasContentDescription(FILTER_LABEL) and isSelected())
                .fetchSemanticsNodes().count { isOnScreen(it) },
        )
    }

    /**
     * The search pill is inert and says so (audit fix #9).
     *
     * It is a presentational placeholder — this kit has no search index — so it
     * must publish `disabled()` semantics instead of looking tappable and doing
     * nothing. The filter beside it stays enabled, which is what makes this a
     * guard rather than a blanket disable.
     */
    @Test
    fun theSearchPillIsAnnouncedAsDisabled() {
        launch()
        switchTo(HomeMode.Agent)

        val disabled = compose.onAllNodes(hasContentDescription("Search") and isNotEnabled())
            .fetchSemanticsNodes().count { isOnScreen(it) }
        assertTrue("the search pill must be announced as disabled", disabled >= 1)

        // The filter is NOT disabled — it is a working control.
        val filterDisabled = compose.onAllNodes(
            hasContentDescription(FILTER_LABEL) and isNotEnabled(),
        ).fetchSemanticsNodes().count { isOnScreen(it) }
        assertEquals("the filter must stay enabled", 0, filterDisabled)
    }

    @Test
    fun theArtifactsTabOpensTheArtifactsScreen() {
        launch()
        switchTo(HomeMode.Agent)

        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        assertEquals(
            "the Artifacts tab must open the artifacts screen",
            DuaGenAIRoutes.ARTIFACTS,
            currentRoute(),
        )
        assertVisible(ARTIFACTS_HEADING)
        // launch() seeds demoArtifacts, so the grid is showing. The empty line is
        // what production draws when that seed is absent.
        assertVisible(ARTIFACTS_FIRST_ITEM)
        assertNotVisible(ARTIFACTS_EMPTY)
        assertNotVisible(MEMORY_EMPTY)
    }

    /**
     * The Artifacts screen renders a GRID, not the empty state.
     *
     * The repository's `demoArtifacts` is the default, so every entry's title must
     * be on screen and the empty-state line must be gone. This is the assertion
     * that the grid wiring actually happened rather than the placeholder staying
     * put.
     */
    @Test
    fun theArtifactsScreenRendersTheArtifactGrid() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        assertVisible(ARTIFACTS_FIRST_ITEM)
        assertVisible(ARTIFACTS_LAST_ITEM)
        // A type chip and a timestamp from the first card.
        assertVisible("Webpage")
        assertVisible("Aaj")
        assertNotVisible(ARTIFACTS_EMPTY)
    }

    /**
     * Audit fix #7 — the Artifacts filter button OPENS a sheet and APPLIES a
     * filter.
     *
     * The button used to be a bare toggle that filtered nothing. Now it opens a
     * ModalBottomSheet of type chips; tapping "Image" narrows the grid to the
     * image artifacts only. Both halves are asserted: the sheet appears, and the
     * grid actually changes — a sheet that opened but filtered nothing would
     * still fail here.
     */
    @Test
    fun theArtifactsFilterButtonOpensTheSheetAndFiltersTheGrid() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        // The unfiltered grid: a document and an image are both on screen.
        assertVisible(ARTIFACTS_FIRST_ITEM)          // Q3 Launch Deck (Webpage)
        assertVisible(ARTIFACTS_IMAGE_ITEM)          // Brand Hero Banner (Image)

        // The filter button opens the sheet.
        compose.onNodeWithContentDescription(FILTER_LABEL).performClick()
        compose.waitForIdle()
        assertVisible(ARTIFACTS_FILTER_SHEET_TITLE)
        assertVisible(ARTIFACTS_FILTER_CLEAR)

        // Applying "Image" narrows the grid: the image stays, the webpage goes.
        // The chip is addressed by its own description, because the cards also
        // carry their type as text.
        compose.onNodeWithContentDescription("Filter: $ARTIFACTS_FILTER_IMAGE").performClick()
        compose.waitForIdle()
        assertVisible(ARTIFACTS_IMAGE_ITEM)
        assertNotVisible(ARTIFACTS_FIRST_ITEM)

        // Clear restores the full grid.
        compose.onNodeWithText(ARTIFACTS_FILTER_CLEAR).performClick()
        compose.waitForIdle()
        assertVisible(ARTIFACTS_FIRST_ITEM)
    }

    /**
     * Audit fix #9 — a filter that matches nothing shows the no-match panel, not
     * a blank grid.
     *
     * "Link" is a real artifact type with no demo entries, so it is the case the
     * panel exists for. The panel must name the filter and offer the way out.
     */
    @Test
    fun aFilterWithNoMatchesShowsTheNoMatchPanel() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription(FILTER_LABEL).performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Filter: Link").performClick()
        compose.waitForIdle()

        // The panel names the filter and offers the way out — never a blank area.
        assertVisibleSubstring("Koi Link artifact nahi mila")
        assertVisible(ARTIFACTS_NO_MATCH_CLEAR)
        assertNotVisible(ARTIFACTS_FIRST_ITEM)

        // The way out works.
        compose.onNodeWithText(ARTIFACTS_NO_MATCH_CLEAR).performClick()
        compose.waitForIdle()
        assertVisible(ARTIFACTS_FIRST_ITEM)
    }

    /**
     * Audit fix #8 — the preview caption describes the SELECTED artifact.
     *
     * The caption is built from the artifact's own name, type and size, so it
     * must change when a different card is selected. Asserting the first card's
     * caption, then a second card's, proves it follows the selection rather than
     * being a fixed string.
     */
    @Test
    fun thePreviewCaptionFollowsTheSelectedArtifact() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        // The caption starts on the first card.
        assertVisibleSubstring(ARTIFACTS_CAPTION_FIRST)

        // Selecting another card moves the caption to that card.
        compose.onNodeWithText(ARTIFACTS_IMAGE_ITEM).performClick()
        compose.waitForIdle()
        assertVisibleSubstring(ARTIFACTS_CAPTION_IMAGE)
        assertNotVisible(ARTIFACTS_CAPTION_FIRST)
    }

    /**
     * Audit fix #9 — a disabled control is announced as disabled and cannot act.
     *
     * "Edit profile" used to be `onClick = {}`: it looked live and swallowed the
     * tap. It is now `enabled = false`, so it publishes `disabled()` semantics
     * and a click changes nothing. The helper line that explains why is asserted
     * too, because a disabled control with no reason is its own usability bug.
     */
    @Test
    fun theDisabledProfileControlsAreNotClickable() {
        launch()
        openDrawer()
        // The drawer gear is "Profile". The home gear is "Settings" and opens
        // the settings screen; this test is about the profile controls.
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()

        // Both inert controls are announced as disabled.
        val disabled = compose.onAllNodes(
            hasText(EDIT_PROFILE_LABEL) and isNotEnabled(),
        ).fetchSemanticsNodes().count { isOnScreen(it) }
        assertTrue("\"$EDIT_PROFILE_LABEL\" must be announced as disabled", disabled >= 1)

        // Signed out (the default here) the SESSION card is a "Sign in" row
        // into the one email/password sign-in. There is no raw token field and
        // no Sign out here: the one Sign out lives in Settings.
        //
        // The card sits below the fold, so it is scrolled into view first: a
        // LazyColumn does not compose an off-screen item, and asserting on one
        // would fail for a reason that has nothing to do with the fix.
        compose.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasText(SIGN_IN_LABEL))
        compose.waitForIdle()
        assertVisible(SIGN_IN_LABEL)
        assertNotVisible(SESSION_TOKEN_LABEL)
        assertNotVisible(SETTINGS_SIGN_OUT_LABEL)


        // The reason is on screen, not left to the user to guess.
        assertVisible(EDIT_PROFILE_HELPER)

        // Clicking it changes nothing: the Profile screen is still the route.
        compose.onNodeWithText(EDIT_PROFILE_LABEL).performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.PROFILE, currentRoute())
    }

    // ------------------------------------------- Memory filter + search

    /**
     * The Memory category chips filter the list.
     *
     * The chip row is the Artifacts pattern applied to Memory: tapping
     * "Preference" must leave only the preference memories on screen and drop
     * the project ones. Asserting both halves proves the chip actually filters
     * rather than only changing its own selected state.
     */
    @Test
    fun theMemoryFilterChipNarrowsTheList() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Memory.label).performClick()
        compose.waitForIdle()

        // Unfiltered: a project memory and a preference memory are both there.
        assertVisible(MEMORY_PROJECT_ITEM)
        assertVisible(MEMORY_PREFERENCE_ITEM)

        // The chip is addressed by its own description, because the cards also
        // carry their category as text.
        compose.onNodeWithContentDescription(MEMORY_CHIP_PREFERENCE).performClick()
        compose.waitForIdle()

        assertVisible(MEMORY_PREFERENCE_ITEM)
        assertNotVisible(MEMORY_PROJECT_ITEM)
    }

    /**
     * The Memory search field filters the list, and combines with the chip.
     *
     * Typing narrows the list to the matching memory. The query is chosen to hit
     * exactly ONE entry ("087C45" appears only in the brand-green memory), so
     * the assertion proves the search actually filtered rather than merely
     * leaving the list intact.
     */
    @Test
    fun theMemorySearchFiltersTheList() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Memory.label).performClick()
        compose.waitForIdle()

        // The search field is there (its placeholder is showing)…
        assertVisible(MEMORY_SEARCH_HINT)

        // …and typing in it filters the list. The Memory screen has exactly one
        // text field, so this selector is unambiguous.
        compose.onNode(hasSetTextAction()).performTextInput("087C45")
        compose.waitForIdle()

        assertVisible(MEMORY_PREFERENCE_ITEM)
        assertNotVisible(MEMORY_PROJECT_ITEM)
    }

    /**
     * A Memory chip with no entries shows the no-match panel, not a blank list.
     *
     * "Task" is a real memory category with no demo entries, so it is the case
     * the panel exists for. The panel must say so and offer the way out, and the
     * way out must restore the full list.
     */
    @Test
    fun aMemoryFilterWithNoMatchesShowsTheNoMatchPanel() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Memory.label).performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription(MEMORY_CHIP_TASK).performClick()
        compose.waitForIdle()

        assertVisible(MEMORY_NO_MATCH)
        assertVisible(MEMORY_NO_MATCH_CLEAR)
        assertNotVisible(MEMORY_PROJECT_ITEM)

        // The way out works.
        compose.onNodeWithText(MEMORY_NO_MATCH_CLEAR).performClick()
        compose.waitForIdle()
        assertVisible(MEMORY_PROJECT_ITEM)
    }

    // ------------------------------------------- Artifacts swipe + refresh

    /**
     * Swipe-to-delete opens the SAME confirmation the long-press opens, and
     * confirming removes the artifact.
     *
     * The swipe is a real gesture on the card, so this proves the gesture is
     * wired rather than only the dialog. The card must be gone afterwards.
     */
    @Test
    fun swipingAnArtifactCardOpensTheDeleteConfirmation() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        assertVisible(ARTIFACTS_IMAGE_ITEM)

        // Swipe the image card from end to start.
        compose.onNodeWithText(ARTIFACTS_IMAGE_ITEM).performTouchInput { swipeLeft() }
        compose.waitForIdle()

        // The confirmation appears \u2014 the same one the long-press opens.
        assertVisible(ARTIFACT_DELETE_DIALOG_TITLE)
        assertVisible(ARTIFACT_DELETE_CONFIRM)

        // Confirming removes the artifact and offers Undo.
        compose.onNodeWithText(ARTIFACT_DELETE_CONFIRM).performClick()
        compose.waitForIdle()
        assertNotVisible(ARTIFACTS_IMAGE_ITEM)
        assertVisible(ARTIFACT_DELETED_SNACK)
        assertVisible(ARTIFACT_UNDO)
    }

    /**
     * Undo restores a deleted artifact.
     *
     * The snackbar's Undo re-adds through the repository, so the card must come
     * back. This is what makes the delete reversible rather than final.
     */
    @Test
    fun undoingADeleteRestoresTheArtifact() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(ARTIFACTS_IMAGE_ITEM).performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText(ARTIFACT_DELETE_CONFIRM).performClick()
        compose.waitForIdle()
        assertNotVisible(ARTIFACTS_IMAGE_ITEM)

        // Undo brings it back.
        compose.onNodeWithText(ARTIFACT_UNDO).performClick()
        compose.waitForIdle()
        assertVisible(ARTIFACTS_IMAGE_ITEM)
    }

    /**
     * Cancelling the delete confirmation leaves the artifact alone.
     *
     * The swipe must not destroy anything on its own \u2014 the dialog owns the
     * decision, so Cancel is a no-op on the data.
     */
    @Test
    fun cancellingTheDeleteConfirmationKeepsTheArtifact() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(ARTIFACTS_IMAGE_ITEM).performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertVisible(ARTIFACT_DELETE_DIALOG_TITLE)

        compose.onNodeWithText(ARTIFACT_DELETE_CANCEL).performClick()
        compose.waitForIdle()

        assertVisible(ARTIFACTS_IMAGE_ITEM)
        assertNotVisible(ARTIFACT_DELETED_SNACK)
    }

    /**
     * Pull-to-refresh re-runs the repository's load path.
     *
     * The gesture must reach the repository, not merely spin an indicator, so
     * this asserts the repository's own `loadCount` went up AND the grid is
     * still on screen afterwards \u2014 a refresh must not blank the content.
     */
    @Test
    fun pullToRefreshReloadsTheArtifacts() {
        val repository = ArtifactsRepository(seed = demoArtifacts, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                ArtifactsScreen(repository = repository)
            }
        }
        compose.waitForIdle()

        val before = repository.loadCount
        assertTrue("the first load must have run", before >= 1)

        // The refresh path the gesture calls.
        kotlinx.coroutines.runBlocking { repository.refresh() }
        compose.waitForIdle()

        assertTrue(
            "pull-to-refresh must re-run the repository's load()",
            repository.loadCount > before,
        )
        // The grid is still there \u2014 a refresh never blanks the content.
        assertVisible(ARTIFACTS_FIRST_ITEM)
    }

    // ------------------------------------------- long-press context menu

    /**
     * Long-pressing a card opens the CONTEXT MENU, and its Open action moves the
     * preview caption to that card.
     *
     * The menu is the new surface: it used to be that a long-press opened the
     * delete dialog directly. Now it opens a menu of three actions, and Open is
     * the one that selects the card — which is what the caption follows. Both
     * halves are asserted, so a menu that opened but did nothing would fail.
     */
    @Test
    fun longPressOpensTheContextMenuAndOpenUpdatesTheCaption() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        // The caption starts on the first card.
        assertVisibleSubstring(ARTIFACTS_CAPTION_FIRST)

        // Long-press the image card: the menu opens with all three actions.
        compose.onNodeWithContentDescription("$ARTIFACTS_IMAGE_ITEM, Image")
            .performTouchInput { longClick() }
        compose.waitForIdle()
        assertVisibleDescCount(MENU_OPEN_IMAGE, 1)
        assertVisibleDescCount(MENU_SHARE_IMAGE, 1)
        assertVisibleDescCount(MENU_DELETE_IMAGE, 1)

        // Open selects the card, so the caption follows it.
        compose.onNodeWithContentDescription(MENU_OPEN_IMAGE).performClick()
        compose.waitForIdle()
        assertVisibleSubstring(ARTIFACTS_CAPTION_IMAGE)
        assertNotVisible(ARTIFACTS_CAPTION_FIRST)
    }

    /**
     * The context menu's Share item is DISABLED and says why.
     *
     * A real share sheet is not available in this build, so the item must not
     * look live and swallow the tap — it publishes `disabled()` semantics and
     * carries a helper line. This is audit fix #9's rule applied to a menu item.
     */
    @Test
    fun theContextMenuShareItemIsDisabled() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("$ARTIFACTS_IMAGE_ITEM, Image")
            .performTouchInput { longClick() }
        compose.waitForIdle()

        val disabled = compose.onAllNodes(
            hasContentDescription(MENU_SHARE_IMAGE) and isNotEnabled(),
        ).fetchSemanticsNodes().count { isOnScreen(it) }
        assertTrue("the Share item must be announced as disabled", disabled >= 1)

        // The reason is on screen, not left to the user to guess.
        assertVisible(MENU_SHARE_HELPER)
    }

    /**
     * The context menu's Delete action opens the SAME confirmation the swipe
     * opens.
     *
     * One delete path, two gestures: the menu's Delete and the swipe both set the
     * same pending-delete state, so the dialog is identical. Confirming removes
     * the artifact.
     */
    @Test
    fun theContextMenuDeleteOpensTheConfirmation() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Artifacts.label).performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("$ARTIFACTS_IMAGE_ITEM, Image")
            .performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(MENU_DELETE_IMAGE).performClick()
        compose.waitForIdle()

        // The same confirmation the swipe opens.
        assertVisible(ARTIFACT_DELETE_DIALOG_TITLE)
        assertVisible(ARTIFACT_DELETE_CONFIRM)

        compose.onNodeWithText(ARTIFACT_DELETE_CONFIRM).performClick()
        compose.waitForIdle()
        assertNotVisible(ARTIFACTS_IMAGE_ITEM)
        assertVisible(ARTIFACT_DELETED_SNACK)
    }

    /**
     * The context menu has FOUR items, in Material 3's order, and "Open in new
     * tab" fires the platform's URL handler.
     *
     * The handler is injected, so this proves the intent path is wired without
     * leaving the process: the lambda records the URL it was handed.
     */
    @Test
    fun theContextMenuHasFourItemsAndOpenInNewTabFiresTheIntent() {
        val opened = mutableListOf<String>()
        val repository = ArtifactsRepository(seed = demoArtifacts, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                ArtifactsScreen(
                    repository = repository,
                    onOpenInNewTab = { opened += it },
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("$ARTIFACTS_IMAGE_ITEM, Image")
            .performTouchInput { longClick() }
        compose.waitForIdle()

        // All four items are present.
        assertVisibleDescCount(MENU_OPEN_IMAGE, 1)
        assertVisibleDescCount(MENU_OPEN_NEW_TAB_IMAGE, 1)
        assertVisibleDescCount(MENU_SHARE_IMAGE, 1)
        assertVisibleDescCount(MENU_DELETE_IMAGE, 1)

        // Tapping it hands the artifact's URL to the platform handler.
        compose.onNodeWithContentDescription(MENU_OPEN_NEW_TAB_IMAGE).performClick()
        compose.waitForIdle()

        assertEquals(1, opened.size)
        assertTrue(
            "the opened URL must be the artifact's own",
            opened.single().contains("brand-hero"),
        )
    }

    /**
     * "Open in new tab" is DISABLED for an artifact with no URL.
     *
     * Meeting Notes is seeded without a URL on purpose, so this is the state that
     * proves the item cannot be a silent no-op: it is announced as disabled and
     * carries a helper line saying why.
     */
    @Test
    fun openInNewTabIsDisabledWhenTheArtifactHasNoUrl() {
        val opened = mutableListOf<String>()
        val repository = ArtifactsRepository(seed = demoArtifacts, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                ArtifactsScreen(
                    repository = repository,
                    onOpenInNewTab = { opened += it },
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("$ARTIFACTS_LAST_ITEM, Document")
            .performTouchInput { longClick() }
        compose.waitForIdle()

        val disabled = compose.onAllNodes(
            hasContentDescription(MENU_OPEN_NEW_TAB_NOTES) and isNotEnabled(),
        ).fetchSemanticsNodes().count { isOnScreen(it) }
        assertTrue("the item must be announced as disabled", disabled >= 1)
        assertVisible(MENU_OPEN_NEW_TAB_HELPER)

        // And a tap cannot fire the handler.
        compose.onNodeWithContentDescription(MENU_OPEN_NEW_TAB_NOTES).performClick()
        compose.waitForIdle()
        assertTrue("a disabled item must not open anything", opened.isEmpty())
    }

    // ------------------------------------------- Memory swipe-to-delete

    /**
     * Swiping a memory row opens the delete confirmation, confirming removes it,
     * and Undo restores it through the repository.
     *
     * The whole loop is asserted, because the point of the fix is that the row is
     * reversible: delete must go through the data source and Undo must come back
     * through it too.
     */
    @Test
    fun swipingAMemoryRowDeletesItAndUndoRestoresIt() {
        val repository = MemoryRepository(seed = sampleMemories, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository)
            }
        }
        compose.waitForIdle()

        assertVisible(MEMORY_PREFERENCE_ITEM)

        compose.onNodeWithContentDescription("Memory: $MEMORY_PREFERENCE_ITEM")
            .performTouchInput { swipeLeft() }
        compose.waitForIdle()

        assertVisible(MEMORY_DELETE_DIALOG_TITLE)

        compose.onNodeWithText(ARTIFACT_DELETE_CONFIRM).performClick()
        compose.waitForIdle()
        assertNotVisible(MEMORY_PREFERENCE_ITEM)
        assertVisible(MEMORY_DELETED_SNACK)

        compose.onNodeWithText(MEMORY_UNDO).performClick()
        compose.waitForIdle()
        assertVisible(MEMORY_PREFERENCE_ITEM)
    }

    /**
     * Cancelling the memory delete confirmation keeps the row.
     *
     * The swipe must not destroy anything on its own — the dialog owns the
     * decision.
     */
    @Test
    fun cancellingTheMemoryDeleteKeepsTheRow() {
        val repository = MemoryRepository(seed = sampleMemories, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Memory: $MEMORY_PREFERENCE_ITEM")
            .performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertVisible(MEMORY_DELETE_DIALOG_TITLE)

        compose.onNodeWithText(ARTIFACT_DELETE_CANCEL).performClick()
        compose.waitForIdle()

        assertVisible(MEMORY_PREFERENCE_ITEM)
        assertNotVisible(MEMORY_DELETED_SNACK)
    }

    // ------------------------------------------- Memory pull-to-refresh

    /**
     * Pull-to-refresh on the Memory screen re-runs the repository's load path.
     *
     * The gesture must reach the repository, not merely spin an indicator, so
     * this asserts the repository's own `loadCount` went up AND the list is still
     * on screen afterwards — a refresh must not blank the content.
     */
    @Test
    fun memoryPullToRefreshReloadsTheList() {
        val repository = MemoryRepository(seed = sampleMemories, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository)
            }
        }
        compose.waitForIdle()

        val before = repository.loadCount
        assertTrue("the first load must have run", before >= 1)

        // The refresh path the gesture calls.
        kotlinx.coroutines.runBlocking { repository.refresh() }
        compose.waitForIdle()

        assertTrue(
            "pull-to-refresh must re-run the repository's load()",
            repository.loadCount > before,
        )
        // The list is still there — a refresh never blanks the content.
        assertVisible(MEMORY_FIRST_ITEM)
    }

    // ------------------------------------------- repository data source

    /**
     * The repository loads THROUGH its data source, not from a local list.
     *
     * A fake source counts its fetches, so this proves the data actually came
     * from the source — the seam a real backend plugs into.
     */
    @Test
    fun theRepositoryLoadsFromItsDataSource() {
        val source = FlakyArtifactsDataSource(failures = 0)
        val repository = ArtifactsRepository(dataSource = source)

        kotlinx.coroutines.runBlocking { repository.load() }

        val state = repository.state.value
        assertTrue("the load must end Loaded", state is ArtifactsUiState.Loaded)
        assertEquals(
            "the loaded list must be the source's list",
            demoArtifacts.size,
            (state as ArtifactsUiState.Loaded).artifacts.size,
        )
        assertTrue("load() must go through the data source", source.fetchCount >= 1)
    }

    /**
     * A failing data source surfaces an Error, and Retry recovers.
     *
     * The source fails its first fetch and succeeds afterwards, so this proves
     * both halves of the error contract: the failure becomes
     * [ArtifactsUiState.Error], and re-running `load()` (what Retry does) ends
     * Loaded.
     */
    @Test
    fun theRepositorySurfacesAnErrorAndRetryRecovers() {
        val source = FlakyArtifactsDataSource(failures = 1)
        val repository = ArtifactsRepository(dataSource = source)

        kotlinx.coroutines.runBlocking { repository.load() }
        assertTrue(
            "a failing source must surface an Error",
            repository.state.value is ArtifactsUiState.Error,
        )

        // Retry: the same load() call, now succeeding.
        kotlinx.coroutines.runBlocking { repository.load() }
        assertTrue(
            "Retry must recover to Loaded",
            repository.state.value is ArtifactsUiState.Loaded,
        )
    }

    // ------------------------------------------- Memory long-press menu

    /**
     * Long-pressing a memory row opens the context menu with Open and Delete.
     *
     * The same pattern the Artifacts grid uses: a Material 3 DropdownMenu anchored
     * to the row, the destructive action last. Both items are addressed by their
     * own content descriptions, because the bare words also appear elsewhere.
     */
    @Test
    fun longPressOnAMemoryRowOpensTheContextMenu() {
        val repository = MemoryRepository(seed = sampleMemories, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Memory: $MEMORY_PROJECT_ITEM")
            .performTouchInput { longClick() }
        compose.waitForIdle()

        assertVisibleDescCount(MEMORY_MENU_OPEN, 1)
        assertVisibleDescCount(MEMORY_MENU_DELETE, 1)
    }

    /**
     * The menu's "Open" falls back to the detail dialog when the memory has no
     * URL.
     *
     * Brand green is seeded without a URL on purpose, so this is the state that
     * proves "Open" is never a silent no-op: the shared [UrlOpener] returns false
     * and the screen shows the memory's detail instead.
     */
    @Test
    fun theMemoryMenuOpenFallsBackToTheDetailDialogWhenThereIsNoUrl() {
        val opened = mutableListOf<String>()
        val repository = MemoryRepository(seed = sampleMemories, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository, onOpenInNewTab = { opened += it })
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Memory: $MEMORY_PREFERENCE_ITEM")
            .performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(MEMORY_MENU_OPEN_NO_URL).performClick()
        compose.waitForIdle()

        // The detail dialog opened (its Close button is unique to it)…
        assertVisible(MEMORY_DETAIL_CLOSE)
        // …and nothing was handed to the platform, because there was no URL.
        assertTrue("a URL-less Open must not open anything", opened.isEmpty())
    }

    /**
     * The menu's "Open" hands the memory's URL to the SHARED opener.
     *
     * The launcher is injected, so this proves the Memory screen routes through
     * the same helper the Artifacts screen uses — without leaving the process.
     */
    @Test
    fun theMemoryMenuOpenHandsTheUrlToTheSharedOpener() {
        val opened = mutableListOf<String>()
        val repository = MemoryRepository(seed = sampleMemories, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository, onOpenInNewTab = { opened += it })
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Memory: $MEMORY_PROJECT_ITEM")
            .performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(MEMORY_MENU_OPEN).performClick()
        compose.waitForIdle()

        assertEquals(1, opened.size)
        assertEquals(SHARED_OPENER_URL, opened.single())
        // The detail dialog did NOT open: there was a URL to open instead.
        assertNotVisible(MEMORY_DETAIL_CLOSE)
    }

    /**
     * The menu's Delete opens the SAME confirmation the swipe opens, and Undo
     * restores the row through the repository.
     *
     * One delete path, two gestures: the menu's Delete and the swipe both set the
     * same pending-delete state, so the dialog is identical.
     */
    @Test
    fun theMemoryMenuDeleteOpensTheSameConfirmationAndUndoRestores() {
        val repository = MemoryRepository(seed = sampleMemories, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Memory: $MEMORY_PROJECT_ITEM")
            .performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(MEMORY_MENU_DELETE).performClick()
        compose.waitForIdle()

        // The same confirmation the swipe opens.
        assertVisible(MEMORY_DELETE_DIALOG_TITLE)

        compose.onNodeWithText(ARTIFACT_DELETE_CONFIRM).performClick()
        compose.waitForIdle()
        assertNotVisible(MEMORY_PROJECT_ITEM)
        assertVisible(MEMORY_DELETED_SNACK)

        compose.onNodeWithText(MEMORY_UNDO).performClick()
        compose.waitForIdle()
        assertVisible(MEMORY_PROJECT_ITEM)
    }

    // ------------------------------------------- shared URL opener

    /**
     * The detail dialog's "Open source" button appears ONLY when the memory has a
     * URL, and tapping it hands that URL to the SHARED opener.
     *
     * Two halves, one rule: a URL-bearing memory shows the button and it opens
     * the URL through the same [UrlOpener] the context menu uses; a URL-less
     * memory shows NO button at all (not a disabled one), because there is
     * nothing to open.
     */
    @Test
    fun theMemoryDetailOpenSourceButtonAppearsOnlyWithAUrl() {
        val opened = mutableListOf<String>()
        val repository = MemoryRepository(seed = sampleMemories, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository, onOpenInNewTab = { opened += it })
            }
        }
        compose.waitForIdle()

        // A memory WITH a URL: tap the card, the detail dialog opens, and the
        // "Open source" button is there.
        compose.onNodeWithContentDescription("Memory: $MEMORY_PROJECT_ITEM").performClick()
        compose.waitForIdle()
        assertVisibleDescCount(MEMORY_OPEN_SOURCE, 1)

        // Tapping it hands the URL to the shared opener.
        compose.onNodeWithContentDescription(MEMORY_OPEN_SOURCE).performClick()
        compose.waitForIdle()
        assertEquals(1, opened.size)
        assertEquals(SHARED_OPENER_URL, opened.single())

        // A memory WITHOUT a URL: the detail dialog opens but the button is
        // ABSENT \u2014 not disabled, not empty.
        compose.onNodeWithContentDescription("Memory: $MEMORY_PREFERENCE_ITEM").performClick()
        compose.waitForIdle()
        assertVisible(MEMORY_DETAIL_CLOSE)
        assertVisibleDescCount(MEMORY_OPEN_SOURCE, 0)
    }

    /**
     * The detail dialog's "Copy text" button copies the whole memory and shows
     * the "Copied" feedback.
     *
     * The button is ALWAYS present (every memory has text), so this asserts the
     * two things that matter: the clipboard receives the memory's title + summary,
     * and the confirmation appears on the same snackbar surface the undo flow
     * uses.
     */
    @Test
    fun theMemoryDetailCopyTextButtonCopiesTheMemoryAndConfirms() {
        val repository = MemoryRepository(seed = sampleMemories, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository)
            }
        }
        compose.waitForIdle()

        // Open the detail dialog for a memory WITH a URL: Copy text sits beside
        // Open source and Close.
        compose.onNodeWithContentDescription("Memory: $MEMORY_PROJECT_ITEM").performClick()
        compose.waitForIdle()
        assertVisibleDescCount(MEMORY_COPY_TEXT, 1)

        // Tap it: the clipboard gets the whole memory, and the confirmation shows.
        compose.onNodeWithContentDescription(MEMORY_COPY_TEXT).performClick()
        compose.waitForIdle()
        val clipboard = compose.activity.getSystemService(android.content.ClipboardManager::class.java)
        assertTrue(
            "the memory's text must reach the clipboard",
            clipboard.hasPrimaryClip() &&
                clipboard.primaryClip!!.getItemAt(0).text.toString().contains(MEMORY_PROJECT_ITEM),
        )
        assertVisible(MEMORY_COPIED_SNACK)

        // A memory WITHOUT a URL still shows Copy text \u2014 it is not tied to the URL.
        compose.onNodeWithContentDescription("Memory: $MEMORY_PREFERENCE_ITEM").performClick()
        compose.waitForIdle()
        assertVisibleDescCount(MEMORY_COPY_TEXT, 1)
    }

    /**
     * The detail dialog's "Share" button is shown, and tapping it hands the
     * memory's text (plus its URL when it has one) to the platform share seam.
     *
     * The sharer is injected, so this proves the screen routes the payload
     * through the ACTION_SEND seam without leaving the process \u2014 the same
     * approach the URL-opener test takes. The button is ALWAYS present (every
     * memory can be shared), so the URL-less memory shares too, with a null URL.
     */
    @Test
    fun theMemoryDetailShareButtonSharesTheMemoryTextAndUrl() {
        val sharer = RecordingSharer()
        val repository = MemoryRepository(seed = sampleMemories, loadDelayMs = 0)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository, onShare = { text, url -> sharer.share(text, url) })
            }
        }
        compose.waitForIdle()

        // A memory WITH a URL: the detail dialog shows Share beside Copy text.
        compose.onNodeWithContentDescription("Memory: $MEMORY_PROJECT_ITEM").performClick()
        compose.waitForIdle()
        assertVisibleDescCount(MEMORY_SHARE, 1)

        // Tapping it hands the memory's text AND its URL to the sharer.
        compose.onNodeWithContentDescription(MEMORY_SHARE).performClick()
        compose.waitForIdle()
        assertEquals(1, sharer.shared.size)
        val (text, url) = sharer.shared.single()
        assertTrue(
            "the shared text must carry the memory's title",
            text.contains(MEMORY_PROJECT_ITEM),
        )
        assertEquals(SHARED_OPENER_URL, url)

        // A memory WITHOUT a URL still shows Share \u2014 it is not tied to the URL \u2014
        // and the shared url is null.
        compose.onNodeWithContentDescription("Memory: $MEMORY_PREFERENCE_ITEM").performClick()
        compose.waitForIdle()
        assertVisibleDescCount(MEMORY_SHARE, 1)
        compose.onNodeWithContentDescription(MEMORY_SHARE).performClick()
        compose.waitForIdle()
        assertEquals(2, sharer.shared.size)
        assertTrue("a URL-less memory must share a null url", sharer.shared.last().second == null)
    }

    /**
     * The Cube picker's category chips are 48dp touch targets.
     *
     * The audit's rule applied to the picker: the chip's drawn size is unchanged,
     * but its tappable box must meet the M3 48dp minimum. The chip is addressed
     * by its own description, and its height is read from the semantics bounds.
     */
    @Test
    fun theCubePickerChipsAreAtLeast48dpTall() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Change cube").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.CUBE_PICKER, currentRoute())

        for (cat in PICKER_CATEGORIES) {
            val node = compose.onNodeWithContentDescription("Filter: $cat").fetchSemanticsNode()
            val heightDp = node.boundsInRoot.height / compose.density.density
            assertTrue(
                "the \"$cat\" chip must be at least 48dp tall, was ${heightDp}dp",
                heightDp >= 48f,
            )
        }
    }

    // ------------------------------------------- cube picker chip tooltip

    /**
     * Long-pressing a Cube picker chip shows a tooltip with the chip's FULL
     * label, and the tap still applies the filter.
     *
     * The tooltip is the M3 plain tooltip, so its content is the label itself;
     * the chip's own description is the filter marker. Asserting both proves the
     * two gestures stay distinct: the long-press reveals the name, the tap
     * filters.
     */
    @Test
    fun theCubePickerChipLongPressShowsATooltipWithTheFullLabel() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Change cube").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.CUBE_PICKER, currentRoute())

        // The tooltip is not up before the long-press.
        assertVisibleDescCount(pickerTooltip("Charcoal"), 0)

        // Long-press the chip: the tooltip appears with the full label.
        compose.onNodeWithContentDescription("Filter: Charcoal").performTouchInput { longClick() }
        compose.waitForIdle()
        assertVisibleDescCount(pickerTooltip("Charcoal"), 1)

        // The tap still applies the filter (the long-press did not).
        compose.onNodeWithContentDescription("Filter: Orange").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Filter: Orange").assertIsSelected()
    }

    // ------------------------------------------- shared URL opener

    /**
     * The shared [UrlOpener] opens a real URL and reports it, and no-ops on a
     * null/blank one.
     *
     * This is the helper's own contract, tested directly: the boolean is what lets
     * a caller show a fallback (Memory) or keep a control disabled (Artifacts)
     * instead of swallowing the tap.
     */
    @Test
    fun theSharedUrlOpenerOpensAUrlAndReportsIt() {
        val opened = mutableListOf<String>()
        val opener = UrlOpener(UrlLauncher { opened += it })

        assertTrue("a real URL must open", opener.open(SHARED_OPENER_URL))
        assertEquals(1, opened.size)
        assertEquals(SHARED_OPENER_URL, opened.single())

        // Null and blank are the same answer: nothing opened.
        assertTrue("a null URL must not open", !opener.open(null))
        assertTrue("a blank URL must not open", !opener.open("   "))
        assertEquals("nothing more must have been opened", 1, opened.size)
    }

    // ------------------------------------------- Memory repository source

    /**
     * The Memory repository loads THROUGH its data source, not from a local list.
     *
     * The Memory twin of [theRepositoryLoadsFromItsDataSource]: a fake source
     * counts its fetches, so this proves the data came from the source — the seam
     * a real backend plugs into.
     */
    @Test
    fun theMemoryRepositoryLoadsFromItsDataSource() {
        val source = FlakyMemoryDataSource(failures = 0)
        val repository = MemoryRepository(dataSource = source)

        kotlinx.coroutines.runBlocking { repository.load() }

        val state = repository.state.value
        assertTrue("the load must end Loaded", state is MemoryUiState.Loaded)
        assertEquals(
            "the loaded list must be the source's list",
            sampleMemories.size,
            (state as MemoryUiState.Loaded).memories.size,
        )
        assertTrue("load() must go through the data source", source.fetchCount >= 1)
    }

    /**
     * A failing Memory source surfaces an Error, and Retry recovers.
     *
     * Both halves of the error contract: the failure becomes
     * [MemoryUiState.Error], and re-running `load()` (what Retry does) ends
     * Loaded.
     */
    @Test
    fun theMemoryRepositorySurfacesAnErrorAndRetryRecovers() {
        val source = FlakyMemoryDataSource(failures = 1)
        val repository = MemoryRepository(dataSource = source)

        kotlinx.coroutines.runBlocking { repository.load() }
        assertTrue(
            "a failing source must surface an Error",
            repository.state.value is MemoryUiState.Error,
        )

        kotlinx.coroutines.runBlocking { repository.load() }
        assertTrue(
            "Retry must recover to Loaded",
            repository.state.value is MemoryUiState.Loaded,
        )
    }

    /**
     * The Memory delete and the undo's add both go THROUGH the data source.
     *
     * This is what makes the undo honest: the row is restored by the same source
     * that removed it, not only in the screen's copy.
     */
    @Test
    fun theMemoryRepositoryDeleteAndUndoGoThroughTheSource() {
        val source = FlakyMemoryDataSource(failures = 0)
        val repository = MemoryRepository(dataSource = source)
        kotlinx.coroutines.runBlocking { repository.load() }

        val item = sampleMemories.first()
        kotlinx.coroutines.runBlocking { repository.delete(item.id) }
        assertEquals("delete must reach the source", 1, source.deleteCount)

        kotlinx.coroutines.runBlocking { repository.add(item) }
        assertEquals("undo's add must reach the source", 1, source.addCount)
    }

    // ------------------------------------------- drawer -> thread

    @Test
    fun theDrawerListsEveryChatWithItsOwnCube() {
        launch()
        openDrawer()

        for (chat in CHATS) {
            // One avatar per history row, so exactly one visible node per label.
            assertVisibleDescCount(avatarLabel(chat.title, chat.cubeId), 1)
        }
    }

    @Test
    fun theDrawerRowsDoNotShareACube() {
        launch()
        openDrawer()

        val distinct = CHATS.map { it.cubeId }.toSet()
        assertEquals("the seven rows must carry seven different cubes", 7, distinct.size)

        for (chat in CHATS) {
            assertVisibleDescCount(avatarLabel(chat.title, chat.cubeId), 1)
        }
    }

    @Test
    fun tappingEachDrawerRowOpensThatChatWithTheSameCube() {
        launch()

        for (chat in CHATS) {
            openChatFromDrawer(chat)

            // ChatScreen is open…
            assertVisible(CHAT_MARKER)

            // …with the shared composer under the conversation, its hint naming
            // THIS thread.
            assertComposer(threadComposerHint(chat.title))

            // …and it is drawing the SAME cube the row drew: once in the header,
            // four times beside the messages.
            assertVisibleDescCount(avatarLabel(chat.title, chat.cubeId), THREAD_HEADER_CUBES)
            assertVisibleDescCount(bubbleLabel(chat.title, chat.cubeId), THREAD_BUBBLE_CUBES)

            // Back to Home for the next row — never a second setContent.
            compose.onNodeWithContentDescription("Back").performClick()
            compose.waitForIdle()
            assertEquals("back must return to Home between rows", DuaGenAIRoutes.HOME, currentRoute())
        }
    }

    @Test
    fun theThreadDrawsTheChatsCubeInEveryBubble() {
        launch()

        openChatFromDrawer(CHATS.first { it.chatId == "chat_market_research" })

        // Header + all four bubbles carry cube_06. If any bubble had kept the
        // user's saved cube, the bubble count would be short.
        assertVisibleDescCount(avatarLabel("Market Research", "cube_06"), THREAD_HEADER_CUBES)
        assertVisibleDescCount(bubbleLabel("Market Research", "cube_06"), THREAD_BUBBLE_CUBES)
    }

    @Test
    fun theRouteCarriesTheChatIdAndTheTitle() {
        launch()

        openChatFromDrawer(CHATS.first { it.chatId == "chat_david_park" })

        assertEquals(
            "the row must navigate to the chat route, not to a generic screen",
            DuaGenAIRoutes.CHAT,
            currentRoute(),
        )
        val args = navController.currentBackStackEntry?.arguments
        assertEquals("chat_david_park", args?.getString("chatId"))
        assertEquals("David Park", args?.getString("title"))
    }

    @Test
    fun aTitleWithASeparatorSurvivesTheRoute() {
        launch()

        // "Design Team · 5" contains a middle dot and a space. Uri.encode must
        // keep them intact or the title would arrive truncated.
        openChatFromDrawer(CHATS.first { it.chatId == "chat_design_team" })

        val args = navController.currentBackStackEntry?.arguments
        assertEquals("Design Team \u00b7 5", args?.getString("title"))
        assertEquals("chat_design_team", args?.getString("chatId"))
        assertVisibleDescCount(avatarLabel("Design Team \u00b7 5", "cube_08"), THREAD_HEADER_CUBES)
    }

    // ------------------------------------------------------------- back

    @Test
    fun theBackArrowReturnsToHome() {
        launch()

        openChatFromDrawer(CHATS.first { it.chatId == "chat_slides_assistant" })
        assertVisible(CHAT_MARKER)

        // The TopAppBar's navigationIcon.
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()

        // Home is back: the thread's status line is gone and Dua's own composer
        // is visible again.
        assertNotVisible(CHAT_MARKER)
        assertVisible(DUA_ASK_HINT)
        assertEquals(
            "back must pop the chat entry, leaving Home on top",
            DuaGenAIRoutes.HOME,
            currentRoute(),
        )
    }

    @Test
    fun systemBackAlsoReturnsToHome() {
        launch()

        openChatFromDrawer(CHATS.first { it.chatId == "chat_family" })
        assertVisible(CHAT_MARKER)

        // NavHost installs a BackHandler; dispatch through the activity so the
        // real system-back path is exercised, not just the toolbar button.
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()

        assertNotVisible(CHAT_MARKER)
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
    }

    @Test
    fun backFromEveryChatLandsOnHome() {
        launch()

        for (chat in CHATS) {
            openChatFromDrawer(chat)
            compose.onNodeWithContentDescription("Back").performClick()
            compose.waitForIdle()

            assertEquals(
                "back from ${chat.chatId} must land on Home",
                DuaGenAIRoutes.HOME,
                currentRoute(),
            )
        }
    }

    // ------------------------------------------------- direct navigation

    @Test
    fun navigatingStraightToAChatRouteShowsThatChatsCube() {
        launch()

        // The same thing a deep link would do: no drawer row involved, so the
        // cube can only come from the route argument.
        for (chat in CHATS) {
            compose.runOnUiThread {
                navController.navigate(DuaGenAIRoutes.chat(chat.chatId, chat.title))
            }
            compose.waitForIdle()

            assertVisible(CHAT_MARKER)
            assertVisibleDescCount(avatarLabel(chat.title, chat.cubeId), THREAD_HEADER_CUBES)
            assertVisibleDescCount(bubbleLabel(chat.title, chat.cubeId), THREAD_BUBBLE_CUBES)

            compose.runOnUiThread { navController.popBackStack() }
            compose.waitForIdle()
        }
    }

    @Test
    fun theChatCubeIsNotTheSavedCube() {
        launch()

        // CubeIds.DEFAULT is what CubeStore emits before anything is saved, and
        // it is what the drawer footer and Profile draw. No chat may resolve to
        // it, or "the thread shows the user's avatar" would be indistinguishable
        // from correct behaviour.
        for (chat in CHATS) {
            assertTrue(
                "${chat.chatId} must not resolve to the saved-cube default",
                chat.cubeId != CubeIds.DEFAULT,
            )
            assertEquals(
                "the hardcoded expectation must match the production mapping",
                chat.cubeId,
                CubeCatalog.forEntity(chat.chatId),
            )
        }
    }

    // ------------------------------------------------------- drawer chrome

    @Test
    fun theDrawerClosesOnItsCloseButton() {
        launch()
        openDrawer()
        assertVisible("HISTORY")

        // The drawer's own close X.
        compose.onNodeWithContentDescription("Close menu").performClick()
        compose.waitForIdle()

        // Closed: the history section is off screen, Home's composer is back.
        assertNotVisible("HISTORY")
        assertVisible(DUA_ASK_HINT)
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
    }

    @Test
    fun theDrawerFooterShowsTheSavedCube() {
        // A previous test may have saved another cube into the process-wide store.
        runBlocking {
            CubeStore(compose.activity.applicationContext).save(CubeIds.DEFAULT)
        }
        launch()
        openDrawer()

        // The footer avatar is the USER's saved cube — a different thing from a
        // chat's cube, and the only place Home's shell draws it.
        assertVisibleDescCount("Your avatar, ${CubeIds.label(CubeIds.DEFAULT)}", 1)
    }

    // ------------------------------------------------- drawer gear -> Profile

    /**
     * The drawer footer's gear opens PROFILE.
     *
     * The `profile` route and its `ProfileScreen` destination already existed;
     * what was missing was a caller, so the screen was unreachable. The gear now
     * navigates there through the same `navController.navigate(...)` every other
     * destination uses — no new route, no new screen.
     *
     * The gear is found by its content description, which is "Profile" (it was
     * "Settings" while it did nothing). That label is also what keeps this test
     * honest: if the gear ever went back to a no-op, the click would leave the
     * route on HOME and the assertion would fail.
     */
    @Test
    fun theDrawerGearOpensTheProfileScreen() {
        launch()
        openDrawer()

        // The gear is in the drawer footer, on screen while the sheet is open.
        assertVisibleDescCount("Profile", 1)

        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()

        assertEquals(
            "the drawer gear must open the existing profile route",
            DuaGenAIRoutes.PROFILE,
            currentRoute(),
        )

        // ProfileScreen is really on screen, not just the route: its own title
        // and the "Change cube" row are the screen's markers.
        assertVisible("Profile")
        assertVisible("Change cube")
    }

    /**
     * Back from Profile returns to the screen the drawer was opened from.
     *
     * The gear closes the drawer and pushes PROFILE, so the back stack is
     * Home -> Profile and a pop lands on Home — the same behaviour every other
     * drawer destination has.
     */
    @Test
    fun backFromProfileReturnsToHome() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.PROFILE, currentRoute())

        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()

        assertEquals(
            "back from Profile must land on Home",
            DuaGenAIRoutes.HOME,
            currentRoute(),
        )
        // Home is really back: Dua's own composer is visible again.
        assertVisible(DUA_ASK_HINT)
    }

    /**
     * The gear is reachable from Agent's home too, and still opens Profile.
     *
     * The drawer is a sibling of the NavHost, so it is the same sheet in both
     * modes. This pins that the wiring is not Dua-only.
     */
    @Test
    fun theDrawerGearOpensProfileFromAgentModeToo() {
        launch()
        switchTo(HomeMode.Agent)
        openDrawer()

        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()

        assertEquals(DuaGenAIRoutes.PROFILE, currentRoute())
        assertVisible("Change cube")
    }

    // ------------------------------------------- Profile -> cube picker

    /**
     * "Change cube" on Profile opens the cube picker.
     *
     * The row is wired to [DuaGenAIRoutes.CUBE_PICKER] through the same
     * `navController.navigate(...)` every other destination uses. The picker's
     * own title and its Save/Cancel controls are the screen's markers.
     */
    @Test
    fun theChangeCubeRowOpensTheCubePicker() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.PROFILE, currentRoute())

        compose.onNodeWithText("Change cube").performClick()
        compose.waitForIdle()

        assertEquals(
            "the Change cube row must open the cube picker route",
            DuaGenAIRoutes.CUBE_PICKER,
            currentRoute(),
        )
        assertVisible(PICKER_HEADING)
        assertVisible(PICKER_SAVE)
        assertVisible(PICKER_CANCEL)
    }

    /**
     * Cancel from the picker discards the draft and returns to Profile.
     *
     * The picker's Cancel writes nothing — it just pops back, so Profile keeps
     * the previously saved cube. This is the "unsaved change discard" contract.
     */
    @Test
    fun cancelFromThePickerReturnsToProfileWithoutSaving() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Change cube").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.CUBE_PICKER, currentRoute())

        compose.onNodeWithText(PICKER_CANCEL).performClick()
        compose.waitForIdle()

        assertEquals(
            "Cancel must pop back to Profile without saving",
            DuaGenAIRoutes.PROFILE,
            currentRoute(),
        )
        assertVisible("Change cube")
    }

    /**
     * Save from the picker persists the choice and returns to Profile.
     *
     * The picker's Save writes through CubeStore (the existing storage pattern)
     * and then pops back, so Profile's header and the "Change cube" row both
     * re-read the saved cube from the Flow and update on their own.
     */
    @Test
    fun saveFromThePickerPersistsAndReturnsToProfile() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Change cube").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.CUBE_PICKER, currentRoute())

        // Pick a different cube, then Save.
        compose.onNodeWithContentDescription("Cube 07").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(PICKER_SAVE).performClick()
        compose.waitForIdle()

        assertEquals(
            "Save must pop back to Profile after persisting",
            DuaGenAIRoutes.PROFILE,
            currentRoute(),
        )
        // The saved cube is now Cube 07, so the header and the row agree.
        assertVisibleDescCount("Your avatar, Cube 07", 1)
        assertVisible("Avatar · Cube 07")
    }

    // ------------------------------------------- Profile -> settings rows

    /**
     * The Appearance row opens the Appearance screen.
     *
     * The row used to be inert (`SettingsRow("Appearance", "Night mode", ...)`
     * with no `onClick`). It is now wired to [DuaGenAIRoutes.APPEARANCE] through
     * the same `navController.navigate(...)` every other destination uses, and
     * its subtitle is the CURRENT theme mode rather than a fixed string.
     */
    @Test
    fun theAppearanceRowOpensTheAppearanceScreen() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.PROFILE, currentRoute())

        compose.onNodeWithText("Appearance").performClick()
        compose.waitForIdle()

        assertEquals(
            "the Appearance row must open the appearance route",
            DuaGenAIRoutes.APPEARANCE,
            currentRoute(),
        )
        // The screen itself: its title and all three choices.
        assertVisible(APPEARANCE_HEADING)
        for (choice in THEME_CHOICES) assertVisible(choice)
    }

    /**
     * Choosing a theme persists it and the Profile row's subtitle follows.
     *
     * The choice is written through SettingsStore (the existing DataStore
     * pattern), the Flow re-emits, and the Profile row re-reads it — so the
     * row's subtitle changes from "System default" to "Dark" on its own.
     */
    @Test
    fun choosingAThemePersistsAndUpdatesTheProfileRow() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()
        // The row starts on the default.
        assertVisible("System default")

        compose.onNodeWithText("Appearance").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.APPEARANCE, currentRoute())

        compose.onNodeWithText("Dark").performClick()
        compose.waitForIdle()

        // Back to Profile: the row's subtitle is now the saved mode.
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.PROFILE, currentRoute())
        assertVisible("Dark")
    }

    /**
     * The Storage row opens the Storage screen, and Clear cache asks first.
     *
     * The row is wired to [DuaGenAIRoutes.STORAGE]; the screen shows the usage
     * and a Clear-cache action that opens a confirmation dialog before it
     * writes anything — the destructive-action guard.
     */
    @Test
    fun theStorageRowOpensTheStorageScreenAndClearCacheConfirms() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Storage").performClick()
        compose.waitForIdle()

        assertEquals(
            "the Storage row must open the storage route",
            DuaGenAIRoutes.STORAGE,
            currentRoute(),
        )
        assertVisible(STORAGE_HEADING)
        assertVisible(CLEAR_CACHE)

        // Tapping Clear cache opens the confirmation, not a silent wipe.
        compose.onNodeWithText(CLEAR_CACHE).performClick()
        compose.waitForIdle()
        assertVisible("Clear cache?")
        assertVisible(CLEAR_CACHE_CONFIRM)
    }

    /**
     * The Notifications row opens the Notifications screen with its switches.
     *
     * The row is wired to [DuaGenAIRoutes.NOTIFICATIONS]; the screen lists the
     * three switches and its summary line is derived from the same map.
     */
    @Test
    fun theNotificationsRowOpensTheNotificationsScreen() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Notifications").performClick()
        compose.waitForIdle()

        assertEquals(
            "the Notifications row must open the notifications route",
            DuaGenAIRoutes.NOTIFICATIONS,
            currentRoute(),
        )
        assertVisible(NOTIFICATIONS_HEADING)
        for (switch in NOTIFICATION_SWITCHES) assertVisible(switch)
    }

    // ------------------------------------------- cube picker search + filter

    /**
     * The picker's search field filters the grid live.
     *
     * Typing a cube's number narrows the grid to it, so a cube that was on
     * screen before the query is gone after it — the assertion that the
     * filter actually ran rather than the field being decorative.
     */
    @Test
    fun thePickerSearchFiltersTheGrid() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Change cube").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.CUBE_PICKER, currentRoute())

        // The search field and the chips are there.
        assertVisible(PICKER_SEARCH_HINT)
        for (cat in PICKER_CATEGORIES) assertVisible(cat)

        // Type a query that matches exactly one cube.
        compose.onNode(hasSetTextAction()).performTextInput("07")
        compose.waitForIdle()

        // Cube 07 is on screen; a cube the query excludes is not.
        assertVisible("Cube 07")
        assertNotVisible("Cube 08")
    }

    /**
     * A query that matches nothing shows the no-results state with a clear.
     *
     * The state is the picker's own fallback, and its Clear filters action is
     * the one-tap way back to the full grid.
     */
    @Test
    fun thePickerShowsNoResultsForAnUnmatchedQuery() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Change cube").performClick()
        compose.waitForIdle()

        compose.onNode(hasSetTextAction()).performTextInput("zzz")
        compose.waitForIdle()

        assertVisible(PICKER_NO_RESULTS)
        assertVisible(PICKER_CLEAR_FILTERS)
        // No cube is shown while nothing matches.
        assertNotVisible("Cube 01")
    }

    // ------------------------------------------------- Agent's own thread

    /**
     * The hero card opens the Personal Agent thread.
     *
     * That thread is a normal chat ([PERSONAL_AGENT_CHAT_ID]), so a signed-in
     * send posts `mode: "auto"` to the API. It uses the same `chat/{chatId}`
     * route as every other conversation.
     */
    @Test
    fun tappingThePinnedAgentCardOpensAgentsOwnThread() {
        launch()
        switchTo(HomeMode.Agent)

        assertVisible(AGENT_ROLE_LINE)

        compose.onNodeWithText(AGENT_ROLE_LINE).performClick()
        compose.waitForIdle()

        assertEquals(
            "the hero card must open the chat route, not a new screen",
            DuaGenAIRoutes.CHAT,
            currentRoute(),
        )
        val args = navController.currentBackStackEntry?.arguments
        assertEquals(PERSONAL_AGENT_CHAT_ID, args?.getString("chatId"))
        assertEquals(PERSONAL_AGENT_TITLE, args?.getString("title"))
    }

    /**
     * The pinned Dua Agent row opens Dua Agent's thread.
     *
     * The id is [AGENT_CHAT_ID], which is what sends `mode: "agent"` and the
     * skill ids to the engine. The row stays the same visual; it is now a button.
     */
    @Test
    fun tappingTheDuaAgentRowOpensTheAgentThread() {
        launch()
        switchTo(HomeMode.Agent)

        assertVisible(DUA_AGENT_ROW)
        compose.onNodeWithText(DUA_AGENT_ROW).performClick()
        compose.waitForIdle()

        assertEquals(DuaGenAIRoutes.CHAT, currentRoute())
        val args = navController.currentBackStackEntry?.arguments
        assertEquals(AGENT_CHAT_ID, args?.getString("chatId"))
        assertEquals(DUA_AGENT_TITLE, args?.getString("title"))
    }

    /** One of the listed chats opens its own existing thread id. */
    @Test
    fun tappingAListedChatOpensThatThread() {
        launch()
        switchTo(HomeMode.Agent)

        compose.onNodeWithText("Code Reviewer").performClick()
        compose.waitForIdle()

        assertEquals(DuaGenAIRoutes.CHAT, currentRoute())
        val args = navController.currentBackStackEntry?.arguments
        assertEquals("chat_code_reviewer", args?.getString("chatId"))
        assertEquals("Code Reviewer", args?.getString("title"))
    }

    /**
     * Agent's thread shows the conversation AND the composer.
     *
     * The pain area holds Agent's and the user's messages, and the shared
     * `DuaComposer` pill sits under them so the thread is usable. Both halves are
     * asserted, because "the composer is there" is only meaningful if the
     * conversation it belongs to is still there.
     */
    @Test
    fun agentsThreadShowsTheConversationAndTheComposer() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(DUA_AGENT_ROW).performClick()
        compose.waitForIdle()

        // The thread is open\u2026
        assertVisible(CHAT_MARKER)

        // \u2026it is Dua Agent's, so its top bar carries that name\u2026
        assertVisible(DUA_AGENT_TITLE)

        // \u2026and the pain area holds both sides of the conversation. These are
        // distinctive fragments of the first user line and the first agent line
        // of `agentThread`.
        assertVisibleSubstring("I need the Q3 launch handled")
        assertVisibleSubstring("I've split that across three agents")

        // The composer is there, and its hint names Agent.
        assertComposer(AGENT_COMPOSER_HINT)
    }

    /**
     * The composer is LIVE: typing and sending adds the user's message to the
     * thread, clears the field, and Agent replies.
     *
     * This is the behaviour a static pill could not have. The message is typed
     * into the composer's own field and sent through its own send control, then
     * asserted in the pain area \u2014 the same path a user takes.
     */
    @Test
    fun sendingAMessageAddsItToTheThreadAndAgentReplies() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(DUA_AGENT_ROW).performClick()
        compose.waitForIdle()

        // Type into the composer's field: the trailing control turns from the
        // voice waveform into Send. Then send.
        compose.onNode(hasSetTextAction()).performTextInput("Book the venue for the launch")
        compose.waitForIdle()
        assertVisibleDescCount(VOICE_LABEL, 0)
        assertVisibleDescCount(SEND_LABEL, 1)
        compose.onNodeWithContentDescription(SEND_LABEL).performClick()
        compose.waitForIdle()

        // The user's message is in the thread\u2026
        assertVisibleSubstring("Book the venue for the launch")

        // \u2026the field is cleared, so the hint is showing again\u2026
        assertVisible(AGENT_COMPOSER_HINT)

        // \u2026and Agent replies after the typing beat. The reply is a canned
        // string, so this waits for it rather than asserting it instantly.
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("I've picked that up", substring = true))
                .fetchSemanticsNodes().count { isOnScreen(it) } >= 1
        }
        assertVisibleSubstring("I've picked that up")
    }

    /**
     * The right side of the thread composer is Skills, MCP Tools, then the
     * Send/Voice control — in that order, left to right — and every one of
     * them, plus Attach, is at least the 48dp M3 touch target.
     */
    @Test
    fun theComposerRightSideIsSkillsThenMcpThenSendAndAllAre48dp() {
        openAgentThread()
        compose.onNode(hasSetTextAction()).performTextInput("Hi")
        compose.waitForIdle()

        fun bounds(desc: String) =
            compose.onNodeWithContentDescription(desc).fetchSemanticsNode().boundsInRoot
        val attach = bounds(ATTACH_LABEL)
        val skills = bounds(SKILLS_LABEL)
        val mcp = bounds(MCP_LABEL)
        val send = bounds(SEND_LABEL)
        assertTrue("Attach sits left of Skills", attach.right <= skills.left)
        assertTrue("Skills sits left of MCP Tools", skills.right <= mcp.left + 1f)
        assertTrue("MCP Tools sits left of Send", mcp.right <= send.left + 1f)
        val min = 48f * compose.density.density - 1f
        for (b in listOf(attach, skills, mcp, send)) {
            assertTrue("composer control must be >= 48dp wide", b.width >= min)
            assertTrue("composer control must be >= 48dp tall", b.height >= min)
        }
    }

    /** Skills opens the Skills sheet: five Dua Agent skills, all on, toggleable. */
    @Test
    fun theSkillsIconOpensTheSkillsSheetAndTogglesASkill() {
        openAgentThread()
        compose.onNodeWithContentDescription(SKILLS_LABEL).performClick()
        compose.waitForIdle()

        for (title in DUA_AGENT_SKILL_TITLES) {
            compose.onNode(hasContentDescription(title) and isOn()).assertExists()
        }
        compose.onNode(hasContentDescription(DUA_AGENT_SKILL_TITLES.first())).performClick()
        compose.waitForIdle()
        compose.onNode(hasContentDescription(DUA_AGENT_SKILL_TITLES.first()) and isOff())
            .assertExists()
    }

    /**
     * MCP Tools opens the MCP sheet: the master switch (on by default), no
     * invented external servers, and the built-in tool ids as status chips.
     */
    @Test
    fun theMcpIconOpensTheMcpSheetWithTheBuiltInTools() {
        openAgentThread()
        compose.onNodeWithContentDescription(MCP_LABEL).performClick()
        compose.waitForIdle()

        compose.onNode(hasContentDescription(MCP_LABEL) and isOn()).assertExists()
        compose.onNodeWithText("None configured").assertExists()
        for (tool in BUILT_IN_TOOLS) compose.onNodeWithText(tool).assertExists()

        compose.onNode(hasContentDescription(MCP_LABEL) and isOn()).performClick()
        compose.waitForIdle()
        compose.onNode(hasContentDescription(MCP_LABEL) and isOff()).assertExists()
    }

    /** Attach `+` opens the plus sheet, which hands off to the Skills sheet. */
    @Test
    fun theAttachButtonOpensThePlusSheet() {
        openAgentThread()
        compose.onNodeWithContentDescription(ATTACH_LABEL).performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Quick Actions").assertExists()
        compose.onNodeWithText("Agent tools").assertExists()
        // The sheet's Skills row (the composer's Skills icon carries no text).
        compose.onNode(hasText(SKILLS_LABEL) and hasContentDescription(SKILLS_LABEL)).performClick()
        compose.waitForIdle()
        compose.onNode(hasContentDescription(DUA_AGENT_SKILL_TITLES.first())).assertExists()
    }

    /**
     * While Agent's reply is pending the trailing control is Stop; tapping it
     * cancels the reply, so Agent never answers and the control returns to the
     * voice waveform.
     */
    @Test
    fun stopCancelsThePendingReply() {
        openAgentThread()
        compose.mainClock.autoAdvance = false
        compose.onNode(hasSetTextAction()).performTextInput("Cancel this one")
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithContentDescription(SEND_LABEL).performClick()
        compose.mainClock.advanceTimeByFrame()

        compose.onNodeWithContentDescription(STOP_LABEL).performClick()
        compose.mainClock.advanceTimeBy(5_000)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()

        assertVisibleSubstring("Cancel this one")
        assertEquals(
            0,
            compose.onAllNodes(hasText("I've picked that up", substring = true))
                .fetchSemanticsNodes().size,
        )
        assertVisibleDescCount(STOP_LABEL, 0)
        assertVisibleDescCount(VOICE_LABEL, 1)
    }

    /** Back from Agent's thread lands on Agent's home, not on Dua's. */
    @Test
    fun backFromAgentsThreadReturnsToAgentsHome() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(DUA_AGENT_ROW).performClick()
        compose.waitForIdle()
        assertVisible(CHAT_MARKER)

        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()

        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        // Agent's home, not Dua's: the agent surface is back and the Dua pill is
        // not. The mode is hoisted in the host, so it survives the round trip.
        assertVisible(CHATS_HEADER)
        assertNotVisible(DUA_ASK_HINT)
    }

    // ------------------------------------------- shared HTTP client

    /**
     * The shared client returns the body on success and reports the call.
     *
     * This is the transport both remote sources sit on, so its contract is
     * pinned here once rather than twice.
     */
    @Test
    fun theSharedHttpClientReturnsTheBodyOnSuccess() {
        val transport = RecordingTransport(response = """[{"id":"x"}]""")
        val client = JsonHttpClient(transport = transport, label = "Artifacts request")

        val body = client.request("GET", "https://api.example/v1/artifacts")

        assertEquals("the body must pass through", """[{"id":"x"}]""", body)
        assertEquals("one call must reach the transport", 1, transport.calls.size)
        assertEquals("GET", transport.calls[0].first)
        assertEquals("https://api.example/v1/artifacts", transport.calls[0].second)
    }

    /**
     * A non-2xx status becomes a labelled failure.
     *
     * The label is what keeps the shared client honest: even though the
     * mechanics are one implementation, an error still says which feature
     * failed.
     */
    @Test
    fun theSharedHttpClientLabelsAStatusFailure() {
        val transport = RecordingTransport(failWith = 500)
        val client = JsonHttpClient(transport = transport, label = "Memory request")

        val error = runCatching { client.request("GET", "https://api.example/v1/memories") }
            .exceptionOrNull()

        assertTrue("a non-2xx must throw", error is IllegalStateException)
        assertEquals(
            "the failure must carry the feature label",
            "Memory request failed: HTTP 500",
            error?.message,
        )
    }

    /**
     * `requestOrThrow` turns a non-2xx into a typed [ApiError], not a string.
     *
     * This is the whole point of the switch: the repository forwards
     * `e.message` into its Error state, so the TYPE decides what the user reads.
     * An [ApiError] carries a curated sentence and a `retryable` flag; the old
     * `IllegalStateException` carried only `"Memory request failed: HTTP 500"`.
     */
    @Test
    fun requestOrThrowReportsAStatusFailureAsATypedApiError() {
        val transport = RecordingTransport(failWith = 500)
        val client = JsonHttpClient(transport = transport, label = "Memory request")

        val error = runCatching { client.requestOrThrow("GET", "https://api.example/v1/memories") }
            .exceptionOrNull()

        assertTrue("a non-2xx must throw an ApiError", error is ApiError)
        val apiError = error as ApiError
        assertEquals("the status must be carried", 500, apiError.status)
        assertTrue("a 5xx is worth retrying", apiError.retryable)
        assertEquals(
            "the message must be the curated sentence, not the raw status",
            "Memory request: the server is unavailable.",
            apiError.userMessage,
        )
        assertTrue(
            "the raw status string must not leak into the user message",
            !apiError.userMessage.contains("HTTP 500"),
        )
    }

    /**
     * A 4xx is NOT retryable — retrying a 401 or a 422 only burns battery.
     *
     * The `retryable` flag is what lets the UI decide whether offering a Retry
     * action is honest, so the two halves are pinned separately.
     */
    @Test
    fun requestOrThrowMarksAClientErrorAsNotRetryable() {
        val transport = RecordingTransport(failWith = 401)
        val client = JsonHttpClient(transport = transport, label = "Artifacts request")

        val error = runCatching { client.requestOrThrow("GET", "https://api.example/v1/artifacts") }
            .exceptionOrNull() as ApiError

        assertEquals(401, error.status)
        assertTrue("a 401 must not be retryable", !error.retryable)
        assertEquals(
            "a 401 must say sign-in is required",
            "Artifacts request: sign-in required.",
            error.userMessage,
        )
    }

    /**
     * A request that never reached the server is a network failure, not a status.
     *
     * `status` is null and the failure IS retryable — the distinction the old
     * string-based error could not express at all.
     */
    @Test
    fun requestOrThrowReportsANetworkFailureAsRetryable() {
        val transport = HttpTransport { _, _, _, _ -> throw java.io.IOException("no route to host") }
        val client = JsonHttpClient(transport = transport, label = "Artifacts request")

        val error = runCatching { client.requestOrThrow("GET", "https://api.example/v1/artifacts") }
            .exceptionOrNull() as ApiError

        assertEquals("a network failure has no status", null, error.status)
        assertTrue("a network failure is retryable", error.retryable)
        assertEquals(
            "the message must be the curated sentence",
            "Artifacts request: no connection.",
            error.userMessage,
        )
        assertTrue(
            "the raw IOException text must not leak",
            !error.userMessage.contains("no route to host"),
        )
    }

    // ------------------------------------------------------- auth token wiring

    /**
     * The token actually reaches the transport.
     *
     * This is the regression the seam had: [TokenProvider] was read by
     * `authToken()` and then dropped, because [HttpTransport.execute] had no
     * token parameter. A provider that returns a token must now be visible to
     * the transport on the very same call.
     */
    @Test
    fun theTokenProviderReachesTheTransport() {
        val transport = RecordingTransport(response = "[]")
        val client = JsonHttpClient(
            transport = transport,
            label = "Artifacts request",
            tokenProvider = TokenProvider { "tok-123" },
        )

        client.requestOrThrow("GET", "https://api.example/v1/artifacts")

        assertEquals("one call must reach the transport", 1, transport.tokens.size)
        assertEquals(
            "the provider's token must be handed to the transport",
            "tok-123",
            transport.tokens[0],
        )
    }

    /**
     * No provider means an anonymous request, not an empty header.
     *
     * `TokenProvider.None` is the default, and null is the transport's signal to
     * omit `Authorization` entirely — an empty `Bearer ` value would be a
     * malformed header rather than an anonymous request.
     */
    @Test
    fun theDefaultProviderSendsAnAnonymousRequest() {
        val transport = RecordingTransport(response = "[]")
        val client = JsonHttpClient(transport = transport, label = "Request")

        client.requestOrThrow("GET", "https://api.example/v1/artifacts")

        assertEquals("the default must be anonymous", null, transport.tokens[0])
    }

    /**
     * The token is asked for PER REQUEST, not captured once at construction.
     *
     * A session store that refreshes an expiring token relies on this: the
     * client must call `token()` again on every request, so a token set after
     * the client was built is still sent.
     */
    @Test
    fun theTokenIsReadPerRequestSoARefreshIsPickedUp() {
        val transport = RecordingTransport(response = "[]")
        var current: String? = "first"
        val client = JsonHttpClient(
            transport = transport,
            label = "Request",
            tokenProvider = TokenProvider { current },
        )

        client.requestOrThrow("GET", "https://api.example/v1/artifacts")
        current = "second"
        client.requestOrThrow("GET", "https://api.example/v1/artifacts")

        assertEquals(
            "each request must read the provider again",
            listOf("first", "second"),
            transport.tokens,
        )
    }

    /**
     * `request()` sends the token too — the wiring is not requestOrThrow-only.
     *
     * Both entry points share the transport, so a caller that still uses the
     * simpler `request()` must not silently go anonymous.
     */
    @Test
    fun thePlainRequestAlsoSendsTheToken() {
        val transport = RecordingTransport(response = "[]")
        val client = JsonHttpClient(
            transport = transport,
            label = "Request",
            tokenProvider = TokenProvider { "tok-plain" },
        )

        client.request("GET", "https://api.example/v1/artifacts")

        assertEquals("tok-plain", transport.tokens[0])
    }

    /**
     * `TokenStore` normalises blank values to null.
     *
     * A half-filled sign-in field must not produce `Authorization: Bearer `,
     * which is a malformed header rather than an anonymous request.
     */
    @Test
    fun tokenStoreTreatsBlankAsSignedOut() {
        val previous = TokenStore.token
        try {
            TokenStore.token = "   "
            assertEquals("whitespace must normalise to null", null, TokenStore.token)

            TokenStore.token = "  tok-trimmed  "
            assertEquals("a real token must be trimmed", "tok-trimmed", TokenStore.token)

            TokenStore.token = null
            assertEquals("null must stay null", null, TokenStore.token)
        } finally {
            TokenStore.token = previous
        }
    }

    /**
     * The shared client reads [TokenStore], so signing in needs no rebuild.
     *
     * This is the end-to-end contract a sign-in flow depends on: set the token
     * once, and the app-wide client starts sending it.
     */
    @Test
    fun theSharedClientSendsTheTokenStoreValue() {
        val previous = TokenStore.token
        try {
            TokenStore.token = "session-token"
            assertEquals(
                "the shared client must read TokenStore",
                "session-token",
                HttpClientProvider.client.authToken(),
            )
        } finally {
            TokenStore.token = previous
        }
    }

    /**
     * The remote sources now surface a typed [ApiError], not a raw status string.
     *
     * This is the end-to-end half of the switch: a 500 from the transport must
     * reach the caller as an [ApiError] whose `userMessage` is safe to render,
     * because that is exactly what the repository puts in its Error state.
     */
    @Test
    fun theRemoteSourcesSurfaceATypedApiError() {
        val transport = RecordingTransport(failWith = 500)
        val client = JsonHttpClient(transport = transport, label = "Request")
        val artifacts = RemoteArtifactsDataSource(
            baseUrl = "https://api.example/v1/artifacts",
            client = client,
        )
        val memories = RemoteMemoryDataSource(
            baseUrl = "https://api.example/v1/memories",
            client = client,
        )

        val artifactsError = runCatching {
            kotlinx.coroutines.runBlocking { artifacts.fetch() }
        }.exceptionOrNull()
        val memoriesError = runCatching {
            kotlinx.coroutines.runBlocking { memories.fetch() }
        }.exceptionOrNull()

        assertTrue("Artifacts must surface an ApiError", artifactsError is ApiError)
        assertTrue("Memory must surface an ApiError", memoriesError is ApiError)
        assertEquals(
            "the Artifacts label must survive",
            "Artifacts request: the server is unavailable.",
            (artifactsError as ApiError).userMessage,
        )
        assertEquals(
            "the Memory label must survive",
            "Memory request: the server is unavailable.",
            (memoriesError as ApiError).userMessage,
        )
    }

    /**
     * The remote Artifacts source parses through the shared client.
     *
     * The transport is faked, so this proves the source's own half — the
     * endpoint and the JSON shape — while the request mechanics come from
     * [JsonHttpClient].
     */
    @Test
    fun theRemoteArtifactsSourceParsesThroughTheSharedClient() {
        val json = """
            [{"id":"art_1","title":"Q3 Launch Deck","type":"Webpage",
              "timestamp":"Aaj","sizeBytes":2411724,
              "url":"https://static.teamily.ai/sites/demo/q3/index.html"},
             {"id":"art_2","title":"Meeting Notes","type":"Document",
              "timestamp":"Aaj","sizeBytes":1024}]
        """.trimIndent()
        val transport = RecordingTransport(response = json)
        val source = RemoteArtifactsDataSource(
            baseUrl = "https://api.example/v1/artifacts",
            client = JsonHttpClient(transport = transport, label = "Artifacts request"),
        )

        val items = kotlinx.coroutines.runBlocking { source.fetch() }

        assertEquals("both entries must parse", 2, items.size)
        assertEquals("Q3 Launch Deck", items[0].title)
        assertEquals(2_411_724L, items[0].sizeBytes)
        assertEquals(
            "a present url must survive",
            "https://static.teamily.ai/sites/demo/q3/index.html",
            items[0].url,
        )
        assertTrue("an absent url must be null, not blank", items[1].url == null)
        assertEquals("GET", transport.calls[0].first)
        assertEquals("https://api.example/v1/artifacts", transport.calls[0].second)
    }

    /**
     * The remote Memory source parses through the same shared client.
     *
     * Same shape as the Artifacts test above, which is the point: the two
     * sources differ only in their endpoint and their JSON, never in how the
     * request is made.
     */
    @Test
    fun theRemoteMemorySourceParsesThroughTheSharedClient() {
        val json = """
            [{"id":"mem_1","title":"Brand green is #087C45","summary":"Prefers green.",
              "timestamp":"4 din pehle","category":"Preference"},
             {"id":"mem_2","title":"Q3 launch","summary":"Current priority.",
              "timestamp":"2 din pehle","category":"Project",
              "url":"https://static.teamily.ai/sites/demo/q3/index.html"}]
        """.trimIndent()
        val transport = RecordingTransport(response = json)
        val source = RemoteMemoryDataSource(
            baseUrl = "https://api.example/v1/memories",
            client = JsonHttpClient(transport = transport, label = "Memory request"),
        )

        val items = kotlinx.coroutines.runBlocking { source.fetch() }

        assertEquals("both entries must parse", 2, items.size)
        assertEquals("Preference", items[0].category)
        assertTrue("a blank url must be null", items[0].url == null)
        assertEquals(
            "a present url must survive",
            "https://static.teamily.ai/sites/demo/q3/index.html",
            items[1].url,
        )
        assertEquals("GET", transport.calls[0].first)
        assertEquals("https://api.example/v1/memories", transport.calls[0].second)
    }

    /**
     * Both remote sources route through ONE shared client implementation.
     *
     * A single transport instance is handed to both sources; if either still
     * carried its own request code, its call would not appear here.
     */
    @Test
    fun bothRemoteSourcesShareOneClientImplementation() {
        val transport = RecordingTransport(response = "[]")
        val client = JsonHttpClient(transport = transport, label = "Request")

        val artifacts = RemoteArtifactsDataSource(
            baseUrl = "https://api.example/v1/artifacts",
            client = client,
        )
        val memories = RemoteMemoryDataSource(
            baseUrl = "https://api.example/v1/memories",
            client = client,
        )

        kotlinx.coroutines.runBlocking {
            artifacts.fetch()
            memories.fetch()
        }

        assertEquals("both sources must go through the one client", 2, transport.calls.size)
        assertEquals("https://api.example/v1/artifacts", transport.calls[0].second)
        assertEquals("https://api.example/v1/memories", transport.calls[1].second)
    }

    // ------------------------------------------- shared repository base class

    /**
     * Both repositories ARE the shared base class.
     *
     * The refactor's whole point: Artifacts and Memory no longer each carry
     * their own load / refresh / add / delete — they inherit ONE engine. If
     * either stopped extending [BaseRemoteRepository], this fails.
     */
    @Test
    fun bothRepositoriesShareTheBaseClass() {
        assertTrue(
            "ArtifactsRepository must extend the shared base",
            ArtifactsRepository(seed = demoArtifacts, loadDelayMs = 0) is BaseRemoteRepository<*, *>,
        )
        assertTrue(
            "MemoryRepository must extend the shared base",
            MemoryRepository(seed = sampleMemories, loadDelayMs = 0) is BaseRemoteRepository<*, *>,
        )
    }

    /**
     * The base class drives load / refresh / add / delete for a repository.
     *
     * Exercised through the Artifacts repository (the Memory twin is covered by
     * the existing source tests): load reaches the source, refresh re-runs it
     * WITHOUT blanking the list, add prepends through the source, and delete
     * removes through the source — all inherited, none re-implemented.
     */
    @Test
    fun theBaseClassDrivesLoadRefreshAddAndDelete() {
        val source = FlakyArtifactsDataSource(failures = 0)
        val repository = ArtifactsRepository(dataSource = source)

        kotlinx.coroutines.runBlocking { repository.load() }
        val loaded = repository.state.value as ArtifactsUiState.Loaded
        assertEquals("load must come from the source", demoArtifacts.size, loaded.artifacts.size)

        // refresh re-runs load() and keeps the list on screen.
        val before = repository.loadCount
        kotlinx.coroutines.runBlocking { repository.refresh() }
        assertTrue("refresh must re-run load()", repository.loadCount > before)
        assertTrue(
            "refresh must not blank the list",
            repository.state.value is ArtifactsUiState.Loaded,
        )

        // add prepends through the source.
        val fresh = ArtifactItem(
            id = "art_new",
            title = "Fresh",
            type = "Image",
            timestamp = "Aaj",
            sizeBytes = 1L,
        )
        kotlinx.coroutines.runBlocking { repository.add(fresh) }
        val afterAdd = repository.state.value as ArtifactsUiState.Loaded
        assertEquals("add must prepend", "art_new", afterAdd.artifacts.first().id)

        // delete removes through the source.
        kotlinx.coroutines.runBlocking { repository.delete("art_new") }
        val afterDelete = repository.state.value as ArtifactsUiState.Loaded
        assertTrue(
            "delete must remove the item",
            afterDelete.artifacts.none { it.id == "art_new" },
        )
    }

    // ------------------------------------------- failed writes are visible

    /**
     * A refused delete returns [WriteResult.Failure], keeps the row, and
     * publishes a SAFE message.
     *
     * This is the repository half of the fix. The source throws `boom`; the
     * repository must not pass that through to the UI, and it must not remove
     * the item from the list either.
     */
    @Test
    fun aRefusedDeleteKeepsTheRowAndReportsASafeMessage() {
        val source = FailingWriteArtifactsDataSource(failDelete = true)
        val repository = ArtifactsRepository(dataSource = source)
        kotlinx.coroutines.runBlocking { repository.load() }

        val item = demoArtifacts.first()
        val result = kotlinx.coroutines.runBlocking { repository.delete(item.id) }

        assertTrue("a refused delete must report Failure", result is WriteResult.Failure)
        assertEquals("the delete must have reached the source", 1, source.deleteCount)

        val loaded = repository.state.value as ArtifactsUiState.Loaded
        assertTrue(
            "a refused delete must NOT remove the row",
            loaded.artifacts.any { it.id == item.id },
        )

        val message = (result as WriteResult.Failure).message
        assertEquals(
            "the failure must be the feature's own sentence",
            "Artifact delete nahi ho paya.",
            message,
        )
        assertTrue("the raw exception text must not leak", !message.contains("boom"))
        assertEquals(
            "the failure must be published for the screen",
            message,
            repository.lastWriteError.value,
        )
    }

    /** A refused add reports Failure and leaves the list untouched. */
    @Test
    fun aRefusedAddKeepsTheListAndReportsASafeMessage() {
        val source = FailingWriteArtifactsDataSource(failDelete = false, failAdd = true)
        val repository = ArtifactsRepository(dataSource = source)
        kotlinx.coroutines.runBlocking { repository.load() }

        val fresh = ArtifactItem(
            id = "art_refused",
            title = "Refused",
            type = "Image",
            timestamp = "Aaj",
            sizeBytes = 1L,
        )
        val result = kotlinx.coroutines.runBlocking { repository.add(fresh) }

        assertTrue("a refused add must report Failure", result is WriteResult.Failure)
        val loaded = repository.state.value as ArtifactsUiState.Loaded
        assertTrue(
            "a refused add must NOT appear in the list",
            loaded.artifacts.none { it.id == "art_refused" },
        )
        assertEquals(
            "Artifact save nahi ho paya.",
            (result as WriteResult.Failure).message,
        )
    }

    /**
     * A refused delete on the Artifacts SCREEN says so and offers no Undo.
     *
     * The end-to-end half of the fix: the row stays, the success snackbar never
     * appears, and there is no Undo to tap — because nothing was deleted.
     */
    @Test
    fun aRefusedArtifactDeleteShowsAFailureAndNoUndo() {
        val repository = ArtifactsRepository(
            dataSource = FailingWriteArtifactsDataSource(failDelete = true),
        )
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                ArtifactsScreen(repository = repository)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(ARTIFACTS_IMAGE_ITEM).performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText(ARTIFACT_DELETE_CONFIRM).performClick()
        compose.waitForIdle()

        // The row is still there, and the screen says the delete failed.
        assertVisible(ARTIFACTS_IMAGE_ITEM)
        assertVisible("Artifact delete nahi ho paya.")
        assertNotVisible(ARTIFACT_DELETED_SNACK)
        assertNotVisible(ARTIFACT_UNDO)
    }

    /**
     * A refused delete on the Memory SCREEN says so and offers no Undo.
     *
     * The Memory twin of [aRefusedArtifactDeleteShowsAFailureAndNoUndo], so the
     * two screens cannot drift apart again.
     */
    @Test
    fun aRefusedMemoryDeleteShowsAFailureAndNoUndo() {
        val repository = MemoryRepository(
            dataSource = FailingWriteMemoryDataSource(failDelete = true),
        )
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                MemoryScreen(repository = repository)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Memory: $MEMORY_PREFERENCE_ITEM")
            .performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText(ARTIFACT_DELETE_CONFIRM).performClick()
        compose.waitForIdle()

        assertVisible(MEMORY_PREFERENCE_ITEM)
        assertVisible("Memory delete nahi ho payi.")
        assertNotVisible(MEMORY_DELETED_SNACK)
        assertNotVisible(MEMORY_UNDO)
    }

    /**
     * A successful delete still offers Undo, and a failed Undo is reported.
     *
     * The delete succeeds, so the success snackbar and Undo DO appear; the
     * re-add then fails, and that failure must be shown rather than swallowed.
     */
    @Test
    fun aFailedUndoIsReportedInsteadOfSilentlyLosingTheRow() {
        val repository = ArtifactsRepository(
            dataSource = FailingWriteArtifactsDataSource(failDelete = false, failAdd = true),
        )
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                ArtifactsScreen(repository = repository)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(ARTIFACTS_IMAGE_ITEM).performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText(ARTIFACT_DELETE_CONFIRM).performClick()
        compose.waitForIdle()

        // The delete worked, so Undo is a real offer.
        assertNotVisible(ARTIFACTS_IMAGE_ITEM)
        assertVisible(ARTIFACT_DELETED_SNACK)
        assertVisible(ARTIFACT_UNDO)

        compose.onNodeWithText(ARTIFACT_UNDO).performClick()
        compose.waitForIdle()

        // The restore failed, and the screen says so instead of pretending.
        assertVisible("Artifact restore nahi ho paya.")
        assertNotVisible(ARTIFACTS_IMAGE_ITEM)
    }

    /**
     * Both remote sources default to the ONE shared client instance.
     *
     * The client is a property of the app, not of a feature: constructing the
     * two sources with no explicit client must hand both the SAME
     * [HttpClientProvider.client] — one connection pool, one config.
     */
    @Test
    fun bothRemoteSourcesDefaultToTheOneSharedClient() {
        val artifacts = RemoteArtifactsDataSource()
        val memories = RemoteMemoryDataSource()

        assertTrue(
            "the Artifacts source must default to the shared client",
            artifacts.client === HttpClientProvider.client,
        )
        assertTrue(
            "the Memory source must default to the shared client",
            memories.client === HttpClientProvider.client,
        )
        assertTrue(
            "both sources must share ONE client instance",
            artifacts.client === memories.client,
        )
    }

    /**
     * The shared client is still injectable.
     *
     * A test (or a caller with a bespoke transport) can pass its own client; the
     * default is the shared one, but the parameter is not sealed off.
     */
    @Test
    fun theSharedClientIsStillInjectable() {
        val transport = RecordingTransport(response = "[]")
        val custom = JsonHttpClient(transport = transport, label = "Custom")
        val source = RemoteArtifactsDataSource(
            baseUrl = "https://api.example/v1/artifacts",
            client = custom,
        )

        assertTrue("an injected client must be used", source.client === custom)
        assertTrue(
            "an injected client must not be the shared one",
            source.client !== HttpClientProvider.client,
        )
    }

    // ------------------------------------------------- live backend wiring

    /**
     * The engine host follows the build value, then a runtime override, always
     * without a trailing slash.
     */
    @Test
    fun engineBaseUrlResolvesAndHonoursTheOverride() {
        ApiConfig.overrideEngineBaseUrl = null
        val fromBuild = BuildConfig.ENGINE_BASE_URL.ifBlank { ApiConfig.FALLBACK_ENGINE_BASE_URL }
            .trim()
            .trimEnd('/')
        assertEquals(fromBuild, ApiConfig.ENGINE_BASE_URL)
        assertEquals("$fromBuild/api/chat", ApiConfig.engineUrl("/api/chat"))
        assertEquals("$fromBuild/api/chat", ApiConfig.engineUrl("api/chat"))

        ApiConfig.overrideEngineBaseUrl = "https://engine.example/ "
        assertEquals("https://engine.example", ApiConfig.ENGINE_BASE_URL)
        assertEquals("https://engine.example/api/chat", ApiConfig.engineUrl("/api/chat"))

        ApiConfig.overrideEngineBaseUrl = null
        assertEquals("clearing the override restores the build value", fromBuild, ApiConfig.ENGINE_BASE_URL)
    }

    /**
     * [TokenStore.state] emits the normalised token, so a collector sees sign-in
     * and sign-out without reading the property again.
     */
    @Test
    fun tokenStoreStateEmitsNormalisedUpdates() {
        TokenStore.token = null
        val seen = mutableListOf<String?>()
        runBlocking {
            val job = coroutineLaunch(Dispatchers.Unconfined) {
                TokenStore.state.collect { seen += it }
            }
            try {
                TokenStore.token = "  live-token  "
                TokenStore.token = "   "
            } finally {
                job.cancel()
            }
        }
        assertEquals(listOf(null, "live-token", null), seen)
        assertEquals(null, TokenStore.token)
        assertEquals(null, TokenStore.state.value)
        assertTrue(TokenStore.state === TokenStore.session)
    }

    /** Email sign-in reads `session.accessToken`, trims it, and stores it. */
    @Test
    fun emailLoginStoresTheSessionAccessToken() {
        ApiConfig.overrideBaseUrl = "https://api.example"
        val transport = RecordingTransport(
            response = """
                {"configured":true,"session":{"accessToken":"  access-123  ","refreshToken":"r"}}
            """.trimIndent(),
        )
        val source = RemoteAccountDataSource(
            client = JsonHttpClient(transport = transport, label = "Sign in"),
        )

        val token = kotlinx.coroutines.runBlocking {
            source.emailLogin("  owner@dua.ai  ", "secret")
        }

        assertEquals("access-123", token)
        assertEquals("access-123", TokenStore.token)
        assertEquals("access-123", TokenStore.state.value)
        assertEquals("POST", transport.calls[0].first)
        assertEquals("https://api.example/api/account/email-login", transport.calls[0].second)
        val sent = JSONObject(transport.bodies[0])
        assertEquals("owner@dua.ai", sent.getString("email"))
        assertEquals("secret", sent.getString("password"))
    }

    /** A body with no access token is not a sign-in, and the stored token stays. */
    @Test
    fun emailLoginRefusesABodyWithoutAnAccessToken() {
        TokenStore.token = "keep-me"
        val transport = RecordingTransport(response = """{"session":{}}""")
        val source = RemoteAccountDataSource(
            client = JsonHttpClient(transport = transport, label = "Sign in"),
        )

        val error = runCatching {
            kotlinx.coroutines.runBlocking { source.emailLogin("a@b.c", "pw") }
        }.exceptionOrNull()

        assertTrue(error is ApiError)
        assertEquals("keep-me", TokenStore.token)
    }

    /** Sign-out posts `{}` and clears the token even when the server refuses. */
    @Test
    fun logoutClearsTheTokenWhenTheCallFails() {
        ApiConfig.overrideBaseUrl = "https://api.example"
        TokenStore.token = "session"
        val transport = RecordingTransport(failWith = 500)
        val source = RemoteAccountDataSource(
            client = JsonHttpClient(transport = transport, label = "Sign out"),
        )

        kotlinx.coroutines.runBlocking { source.logout() }

        assertEquals(null, TokenStore.token)
        assertEquals("POST", transport.calls[0].first)
        assertEquals("https://api.example/api/account/logout", transport.calls[0].second)
        assertEquals("{}", transport.bodies[0])
    }

    /**
     * A normal thread posts `mode: "auto"` to the API. Dua Agent posts
     * `mode: "agent"` plus the skill ids to the engine, and SSE text chunks
     * are joined. A non-SSE agent body falls back to `{"text":"..."}`.
     */
    @Test
    fun remoteChatRoutesAutoAndAgentAndParsesSse() {
        ApiConfig.overrideBaseUrl = "https://api.example"
        ApiConfig.overrideEngineBaseUrl = "https://engine.example"
        val sse = listOf(
            "event: text",
            "data: {\"type\":\"text\",\"text\":\"Hello \"}",
            "data: {\"type\":\"thinking\",\"text\":\"hmm\"}",
            "data: {\"type\":\"text\",\"text\":\"world\"}",
            "",
        ).joinToString("\n")
        val transport = RecordingTransport { url ->
            if (url.startsWith("https://engine.example")) sse else """{"text":"from api"}"""
        }
        val source = RemoteChatDataSource(
            client = JsonHttpClient(transport = transport, label = "Chat request"),
        )
        val skills = DuaAgentSkills.ids.toSet()

        val auto = kotlinx.coroutines.runBlocking {
            source.sendMessage(PERSONAL_AGENT_CHAT_ID, "Hi there", skills)
        }
        val agent = kotlinx.coroutines.runBlocking {
            source.sendMessage(AGENT_CHAT_ID, "Build it", skills)
        }

        assertEquals("from api", auto)
        assertEquals("Hello world", agent)
        assertEquals("https://api.example/api/chat", transport.calls[0].second)
        assertEquals("https://engine.example/api/chat", transport.calls[1].second)

        val autoBody = JSONObject(transport.bodies[0])
        assertEquals("Dua", autoBody.getString("model"))
        assertEquals("auto", autoBody.getString("mode"))
        assertEquals(false, autoBody.getBoolean("stream"))
        assertFalse(autoBody.has("skills"))
        val autoMessage = autoBody.getJSONArray("messages").getJSONObject(0)
        assertEquals("user", autoMessage.getString("role"))
        assertEquals("Hi there", autoMessage.getString("text"))

        val agentBody = JSONObject(transport.bodies[1])
        assertEquals("agent", agentBody.getString("mode"))
        assertEquals(true, agentBody.getBoolean("stream"))
        val sentSkills = agentBody.getJSONArray("skills")
        assertEquals(DuaAgentSkills.ids.size, sentSkills.length())
        assertEquals("frontend-developer", sentSkills.getString(0))
        assertEquals("backend-architect", sentSkills.getString(1))
        assertEquals("api-tester", sentSkills.getString(2))
        assertEquals("devops-automator", sentSkills.getString(3))
        assertEquals("code-reviewer", sentSkills.getString(4))

        val plain = RemoteChatDataSource(
            client = JsonHttpClient(
                transport = RecordingTransport(response = """{"text":"plain agent"}"""),
                label = "Chat request",
            ),
        )
        val fallback = kotlinx.coroutines.runBlocking {
            plain.sendMessage(AGENT_CHAT_ID, "Hi", setOf("code-reviewer"))
        }
        assertEquals("plain agent", fallback)
    }

    /**
     * Signed out, Settings offers Sign in and no Sign out. A token drops the
     * Sign in row and shows exactly ONE Sign out (Profile & Device).
     */
    @Test
    fun settingsAccountRowsFollowTheSessionToken() {
        TokenStore.token = null
        compose.setContent {
            DuaGenAITheme(darkTheme = false) { SettingsScreen() }
        }
        compose.waitForIdle()

        assertVisible(SETTINGS_SIGN_IN_LABEL)
        assertVisible(SETTINGS_ACCOUNT_HELPER)
        val editDisabled = compose.onAllNodes(
            hasText(SETTINGS_EDIT_PROFILE_LABEL) and isNotEnabled(),
        ).fetchSemanticsNodes().count { isOnScreen(it) }
        assertTrue("Edit profile must stay disabled", editDisabled >= 1)
        assertEquals(
            "No Sign out while signed out",
            0,
            compose.onAllNodes(hasText(SETTINGS_SIGN_OUT_LABEL)).fetchSemanticsNodes().size,
        )

        compose.runOnIdle { TokenStore.token = "session-token" }
        compose.waitForIdle()

        assertNotVisible("Use your Dua account email and password.")
        compose.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasText(SETTINGS_SIGN_OUT_LABEL))
        compose.waitForIdle()
        assertEquals(
            "Exactly one Sign out once a session exists",
            1,
            compose.onAllNodes(hasText(SETTINGS_SIGN_OUT_LABEL) and hasClickAction())
                .fetchSemanticsNodes().size,
        )
        TokenStore.token = null
    }

    /**
     * Profile's SESSION card: signed out it is a clickable "Sign in" row (it
     * opens [AccountSignInDialog], the same dialog as Settings) and there is no
     * raw token field. Opening the dialog itself is not asserted: its text
     * fields keep Robolectric from ever going idle.
     */
    @Test
    fun profileSessionCardIsASignInRowWithNoTokenField() {
        TokenStore.token = null
        compose.setContent {
            DuaGenAITheme(darkTheme = false) { ProfileScreen() }
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasText(SIGN_IN_LABEL))
        compose.waitForIdle()
        assertTrue(
            compose.onAllNodes(hasText(SIGN_IN_LABEL) and hasClickAction())
                .fetchSemanticsNodes().isNotEmpty(),
        )
        assertEquals(0, compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText(SESSION_TOKEN_LABEL).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText(SETTINGS_SIGN_OUT_LABEL).fetchSemanticsNodes().size)
    }

    /**
     * The one Sign out: confirm dialog -> server logout (POST
     * /api/account/logout, token cleared) -> onSignedOut (the host goes Home).
     */
    @Test
    fun settingsSignOutConfirmsThenLogsOutOnTheServer() {
        ApiConfig.overrideBaseUrl = "https://api.example"
        TokenStore.token = "session-token"
        val transport = RecordingTransport(response = "{}")
        val source = RemoteAccountDataSource(
            client = JsonHttpClient(transport = transport, label = "Sign out"),
        )
        var signedOut = false
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                SettingsScreen(accountDataSource = source, onSignedOut = { signedOut = true })
            }
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasText(SETTINGS_SIGN_OUT_LABEL))
        compose.onNodeWithText(SETTINGS_SIGN_OUT_LABEL).performClick()
        compose.waitForIdle()

        // Nothing happens before the confirm.
        assertVisible("Sign out?")
        assertTrue(transport.calls.isEmpty())
        assertEquals("session-token", TokenStore.token)

        // The dialog's confirm button is the last "Sign out" node.
        compose.onAllNodesWithText(SETTINGS_SIGN_OUT_LABEL).onLast().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { signedOut }

        assertEquals(null, TokenStore.token)
        assertEquals("https://api.example/api/account/logout", transport.calls[0].second)
    }

    // ------------------------------------------------------------- labels

    /**
     * These mirror the exact strings the screens pass to CubeAvatar. They are
     * duplicated here on purpose: if a screen changes its label, the test must
     * fail rather than silently keep matching.
     */

    /** Drawer history row and Chat header: `"<name> avatar, Cube NN"`. */
    private fun avatarLabel(title: String, cubeId: String) =
        "$title avatar, ${CubeIds.label(cubeId)}"

    /** Chat message bubbles: `"<name>, Cube NN"`. */
    private fun bubbleLabel(title: String, cubeId: String) =
        "$title, ${CubeIds.label(cubeId)}"

    /**
     * The composer hint a thread shows: `ChatScreen` passes
     * `"Message $chatTitle\u2026"`, so this mirrors that expression.
     *
     * Duplicated on purpose, like the two label helpers above: if the screen
     * changes its hint, the test must fail rather than silently keep matching.
     */
    private fun threadComposerHint(title: String) = "Message $title\u2026"
}

/** Create Agent strings, as in res/values/strings.xml. */
private const val CREATE_AGENT_TITLE = "Create Agent"
private const val CREATE_AGENT_NAME_HINT = "Name your Agent"
private const val CREATE_AGENT_ACTION = "Create"
