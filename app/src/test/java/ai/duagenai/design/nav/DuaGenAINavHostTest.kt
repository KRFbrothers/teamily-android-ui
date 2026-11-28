package ai.duagenai.design.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import kotlinx.coroutines.flow.first
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
 *  there is no chat list on Home to tap. And the app ships no sample
 *  conversations: the drawer's history section renders the honest empty
 *  state until real chats exist. The route plumbing those rows used to drive
 *  is pinned by navigating straight to `chat/{chatId}?title={title}` — the
 *  same thing a deep link or a future real row does:
 *
 *      navigate("chat/<id>?title=<title>")  ->  assert ChatScreen
 *
 *  One test pins the drawer's empty history; the rest pin the route, the
 *  title's survival through `Uri.encode`, and the cube the thread draws.
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
 *    3. ChatScreen draws the SAME cube in the header and beside every message
 *       once the conversation starts (a thread begins empty, so the tests send
 *       one message and wait for the canned reply before counting bubble
 *       cubes). Every cube assertion states the expected count instead of
 *       assuming one. That count is itself the assertion that no bubble fell
 *       back to the user's saved cube.
 *
 *  THE COMPOSER ON A THREAD
 *  ------------------------
 *  ChatScreen draws a composer under the conversation again, so Agent can be
 *  talked to. It is the SAME `ChatBar` pill Dua's home draws \u2014 one
 *  implementation, two call sites. An empty thread shows ChatBar's own
 *  `"Ask Dua..."`, and a thread with messages shows `"What's next?"`.
 *  [assertComposer] therefore checks the placeholder
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
private data class ChatCase(
    val chatId: String,
    val title: String,
    /** Agent threads get the agent header pill. The composer hint is ChatBar's. */
    val agent: Boolean,
)

private val CHATS = listOf(
    ChatCase("chat_webpage_developer", "Webpage Developer", agent = true),
    ChatCase("chat_design_team", "Design Team \u00b7 5", agent = false),
    ChatCase("chat_slides_assistant", "Slides Assistant", agent = true),
    ChatCase("chat_david_park", "David Park", agent = false),
    ChatCase("chat_market_research", "Market Research", agent = true),
    ChatCase("chat_accounting_assistant", "Accounting Assistant", agent = true),
    ChatCase("chat_family", "Family \u00b7 8", agent = false),
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
 * The old composer override a thread must NOT show.
 *
 * ChatScreen used to pass `"Type a message..."` into ChatBar and hide the
 * dynamic hint. If it reappears, that override is back.
 */
private const val COMPOSER_PLACEHOLDER = "Type a message..."

/**
 * The hint an empty thread shows. ChatBar owns it: `"Ask Dua..."` until the
 * thread has a message, then `"What's next?"`.
 */
private const val AGENT_COMPOSER_HINT = "Ask Dua..."

/**
 * The Agent-home hero card's title (the user's personal agent).
 *
 * Used to FIND the card: it is the one filled card on Agent home and tapping
 * it opens the Personal Agent thread. The title is unique to the card.
 */
private const val AGENT_ROLE_LINE = PERSONAL_AGENT_TITLE

/** Agent home's section headers and header brand. */
private const val PINNED_HEADER = "Pinned \u00b7 1"
private const val CHATS_HEADER = "Chats \u00b7 0"

/**
 * The screen-edge inset every Agent-home element shares: `DuaGenAISpacing.gutter`
 * (14dp). The top bar, the search row, the hero card, both section headers and
 * the empty state all start on this one vertical line.
 */
private const val GUTTER_DP = 14f

/** res/values/strings.xml demo_label. */
private const val DEMO_LABEL = "Demo"
private const val APP_NAME_TEXT = "Dua Gen AI"
private const val DUA_AGENT_ROW = "Dua Agent"

/* The composer's controls, by their TalkBack descriptions. */
private const val ATTACH_LABEL = "Attach"
private const val SKILLS_LABEL = "Skills"
private const val MCP_LABEL = "MCP Tools"
private const val SEND_LABEL = "Send message"
private const val STOP_LABEL = "Stop"
private const val VOICE_LABEL = "Voice input"

/* The composer is ChatBar: its own labels. */
private const val THREAD_VOICE_LABEL = "Voice message"
private const val DICTATE_LABEL = "Dictate"

/** Dua home's welcome: greeting and the four quick starters. */
private const val DUA_GREETING = "How can I help you today?"
private const val DUA_STARTER_EXPLAIN = "\uD83D\uDCA1 Explain a topic"
private const val DUA_STARTER_EMAIL = "\u270D\uFE0F Write an email"
private const val DUA_STARTER_IDEAS = "\uD83E\uDDE0 Brainstorm ideas"
private const val DUA_STARTER = "\uD83D\uDDD3\uFE0F Plan my day"
private val DUA_STARTERS = listOf(DUA_STARTER_EXPLAIN, DUA_STARTER_EMAIL, DUA_STARTER_IDEAS, DUA_STARTER)

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

/** res/values/strings.xml composer_coming_soon. */
private const val COMING_SOON = "Jaldi aa raha hai"

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

/** The drawer's footer avatar: "Profile, Not signed in" / "Profile, Signed in". */
private const val DRAWER_PROFILE = "Profile, "
/** M3's scrim label: the drawer has no X, the scrim (and Back) close it. */
private const val CLOSE_DRAWER = "Close navigation menu"

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
 * Names that came from the old mockup roster. The app ships NO sample
 * conversations or agents, so these must be nowhere on screen — in Dua mode,
 * in Agent mode, or in the drawer. The list is kept as the negative fixture.
 */
private val MANAGED_AGENTS = listOf("Code Reviewer", "Backend Architect", "DevOps Automator", "API Tester")

/**
 * How many cubes ChatScreen draws beside the bubbles of a started
 * conversation.
 *
 * A thread now starts EMPTY — no seeded dialogue. Each test that counts
 * bubble cubes sends one message first and waits for the canned offline
 * reply, so the thread holds one user bubble and one agent bubble. The
 * user bubble has no avatar. Only the agent reply draws the bot mark,
 * beside the one in the header.
 */
private const val THREAD_BUBBLE_CUBES = 1
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
        // Mute / Pin now persist in the process-wide DataStore: a test that
        // toggles a built-in agent must not leak it into the next one.
        runBlocking {
            val settings = ai.duagenai.design.screens.SettingsStore(compose.activity.applicationContext)
            BUILT_IN_FLAG_IDS.forEach { settings.forgetAgent(it) }
        }
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
     * The composer is the single-row floating `ChatBar`: "Attach" `+`, the
     * editable field (showing [placeholder]), "Dictate" mic, and "Voice message"
     * when empty. "Send message" appears only once text is typed.
     */
    private fun assertComposer(placeholder: String) {
        assertVisible(placeholder)
        if (placeholder != AGENT_COMPOSER_HINT) assertNotVisible(AGENT_COMPOSER_HINT)
        assertNotVisible(COMPOSER_PLACEHOLDER)
        assertVisibleDescCount(ATTACH_LABEL, 1)
        assertVisibleDescCount(DICTATE_LABEL, 1)
        assertVisibleDescCount(THREAD_VOICE_LABEL, 1)
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
     *   3. the composer's own controls ("Attach", "Dictate", "Voice input",
     *      "Skills", "MCP Tools") are absent — they would only exist inside a
     *      composer.
     */
    private fun assertNoComposer() {
        val editable = compose.onAllNodes(hasSetTextAction())
            .fetchSemanticsNodes().count { isOnScreen(it) }
        assertEquals("this surface must have no text input", 0, editable)

        assertNotVisible(COMPOSER_PLACEHOLDER)
        assertVisibleDescCount(ATTACH_LABEL, 0)
        assertVisibleDescCount(DICTATE_LABEL, 0)
        assertVisibleDescCount(VOICE_LABEL, 0)
        assertVisibleDescCount(THREAD_VOICE_LABEL, 0)
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
     * ChatScreen is on top. Agent threads have the header pill and no status
     * line, so the marker is the route, not [CHAT_MARKER].
     */
    private fun assertOnChat() {
        assertEquals("a chat thread must be open", DuaGenAIRoutes.CHAT, currentRoute())
    }

    private fun assertNotOnChat() {
        assertTrue("no chat thread may be open", currentRoute() != DuaGenAIRoutes.CHAT)
    }

    /**
     * Opens the sidebar drawer: Dua home's hamburger or Agent home's avatar —
     * both are labelled "Open menu".
     */
    private fun openDrawer() {
        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.waitForIdle()
    }

    /**
     * Opens Settings the way a user does from the sidebar: the drawer footer
     * avatar. Profile is reached only from Settings → Edit profile.
     */
    private fun openSettingsFromDrawer() {
        openDrawer()
        compose.onNodeWithContentDescription(DRAWER_PROFILE, substring = true).performClick()
        compose.waitForIdle()
    }

    /**
     * Opens Profile the way a user does: drawer avatar → Settings → Edit profile.
     */
    private fun openProfileFromDrawer() {
        openSettingsFromDrawer()
        compose.onNodeWithText(SETTINGS_EDIT_PROFILE_LABEL).performClick()
        compose.waitForIdle()
    }

    private fun openSettingsFromHome() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.waitForIdle()
    }

    private fun openProfilePictureSheet() {
        compose.onNodeWithContentDescription("Change profile picture").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
    }


    private fun switchTo(mode: HomeMode) {
        if (visibleDescCount("${mode.label} mode") > 0) {
            compose.onNodeWithContentDescription("${mode.label} mode").performClick()
        } else {
            // The drawer's Dua | Agent toggle always offers both modes.
            openDrawer()
            modeRow(mode.label).performClick()
        }
        compose.waitForIdle()
    }

    /**
     * Opens a conversation by navigating straight to its chat route — what a
     * real history row (or a deep link) does once conversations exist. The
     * drawer ships no sample rows, so the UI path has nothing to tap yet;
     * this keeps every route-plumbing test honest about what it drives.
     */
    private fun openChatFromDrawer(chat: ChatCase) {
        compose.runOnUiThread {
            navController.navigate(DuaGenAIRoutes.chat(chat.chatId, chat.title))
        }
        compose.waitForIdle()
    }

    /**
     * Sends one message in the open thread and waits for the canned offline
     * reply, leaving exactly one user bubble and one agent bubble. Only the
     * agent bubble carries the bot mark.
     */
    private fun startConversation() {
        compose.onNode(hasSetTextAction()).performTextInput("Test message")
        compose.waitForIdle()
        compose.onNodeWithContentDescription(SEND_LABEL).performClick()
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("I've picked that up", substring = true))
                .fetchSemanticsNodes().count { isOnScreen(it) } >= 1
        }
    }

    // ------------------------------------------------- Dua mode is plain

    @Test
    fun colorSchemes_mapSurfaceRolesFromPalette_primaryUnchanged() {
        val light = ai.duagenai.design.theme.DuaGenAITheme.lightScheme(
            ai.duagenai.design.theme.TeamilyLightPalette,
        )
        val dark = ai.duagenai.design.theme.DuaGenAITheme.darkScheme(
            ai.duagenai.design.theme.TeamilyDarkPalette,
        )
        val lp = ai.duagenai.design.theme.TeamilyLightPalette
        val dp = ai.duagenai.design.theme.TeamilyDarkPalette
        // Freeze primary stays #087C45 in both schemes.
        org.junit.Assert.assertEquals(androidx.compose.ui.graphics.Color(0xFF087C45), light.primary)
        org.junit.Assert.assertEquals(androidx.compose.ui.graphics.Color(0xFF087C45), dark.primary)
        // Dialog container (surfaceContainerHigh) comes from the palette, not M3 baseline.
        org.junit.Assert.assertEquals(lp.surfaceRecessed, light.surfaceContainerHigh)
        org.junit.Assert.assertEquals(dp.bgElevated, dark.surfaceContainerHigh)
        val lightRoles = listOf(
            light.surfaceContainerLowest, light.surfaceContainerLow, light.surfaceContainer,
            light.surfaceContainerHigh, light.surfaceContainerHighest, light.surfaceDim,
            light.surfaceBright, light.surfaceTint,
        )
        val darkRoles = listOf(
            dark.surfaceContainerLowest, dark.surfaceContainerLow, dark.surfaceContainer,
            dark.surfaceContainerHigh, dark.surfaceContainerHighest, dark.surfaceDim,
            dark.surfaceBright, dark.surfaceTint,
        )
        val lightTokens = setOf(lp.bgSurface, lp.bottomnavBg, lp.bgElevated, lp.surfaceRecessed, lp.bgCanvas, lp.accent)
        val darkTokens = setOf(dp.surfaceRecessed, dp.bgCanvas, dp.bgSurface, dp.bgElevated, dp.chromeFill, dp.accent)
        org.junit.Assert.assertTrue(lightTokens.containsAll(lightRoles))
        org.junit.Assert.assertTrue(darkTokens.containsAll(darkRoles))
    }

    @Test
    fun duaGenAITheme_isDarkFollowsExplicitThemeMode() {
        var resolvedLight = true
        var resolvedDark = false
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                resolvedLight = DuaGenAITheme.isDark
            }
            DuaGenAITheme(darkTheme = true) {
                resolvedDark = DuaGenAITheme.isDark
            }
        }
        compose.waitForIdle()
        assertFalse(resolvedLight)
        assertTrue(resolvedDark)
    }

    @Test
    fun duaModeShowsOnlyTheComposerAndNoAgentSurface() {
        launch()
        switchTo(HomeMode.Dua)

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
            assertVisibleDescCount(avatarLabel(chat.title), 0)
        }
    }

    @Test
    fun duaModeHasNoAgentEntryPointAtAll() {
        launch()
        switchTo(HomeMode.Dua)

        // The three agents Agent manages must not appear anywhere in Dua mode —
        // not as a row, not as a badge, not as a tab label.
        for (agent in MANAGED_AGENTS) {
            assertNotVisible(agent)
        }

        // The top bar's right button is the gear, not the plus.
        assertVisibleDescCount("Settings", 1)
        assertVisibleDescCount("New chat", 0)

        // Dua's pill carries no Skills / MCP icons (Dua Agent capabilities).
        // Attach (+) and Voice are announced ENABLED now: + opens the Quick
        // Actions sheet and Voice explains "coming soon" on tap.
        assertVisibleDescCount(SKILLS_LABEL, 0)
        assertVisibleDescCount(MCP_LABEL, 0)
        assertEquals(
            0,
            compose.onAllNodes(hasContentDescription(ATTACH_LABEL) and isNotEnabled())
                .fetchSemanticsNodes().count { isOnScreen(it) },
        )
        assertEquals(
            0,
            compose.onAllNodes(hasContentDescription(VOICE_LABEL) and isNotEnabled())
                .fetchSemanticsNodes().count { isOnScreen(it) },
        )
    }

    /**
     * Dua home's composer is typeable; Send opens a NEW Dua thread (fresh chat
     * id, no sample seed) carrying that first message, and the reply lands
     * there. A second Send from home opens another new thread.
     */
    @Test
    fun duaHomeSendOpensANewDuaThreadWithTheFirstMessage() {
        TokenStore.token = null
        launch()
        switchTo(HomeMode.Dua)

        compose.onNode(hasSetTextAction()).performTextInput("Plan my week")
        compose.waitForIdle()
        assertVisibleDescCount(VOICE_LABEL, 0)
        compose.onNodeWithContentDescription(SEND_LABEL).performClick()
        compose.waitForIdle()

        assertEquals(DuaGenAIRoutes.CHAT, currentRoute())
        val firstId = navController.currentBackStackEntry?.arguments?.getString("chatId").orEmpty()
        assertTrue("a fresh Dua chat id", firstId.startsWith("dua-"))
        assertVisible(DuaGenAIRoutes.DUA_THREAD_TITLE)
        assertVisibleSubstring("Plan my week")
        // A new thread, not the sample conversation.
        assertNotVisible("Can we integrate our team skills and MCP tools here?")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("I've picked that up", substring = true))
                .fetchSemanticsNodes().count { isOnScreen(it) } >= 1
        }

        // Back to home: the draft was cleared; a second Send is a new thread.
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        assertVisible(DUA_ASK_HINT)
        compose.onNode(hasSetTextAction()).performTextInput("Second")
        compose.waitForIdle()
        compose.onNodeWithContentDescription(SEND_LABEL).performClick()
        compose.waitForIdle()
        val secondId = navController.currentBackStackEntry?.arguments?.getString("chatId").orEmpty()
        assertTrue(secondId.startsWith("dua-") && secondId != firstId)
        assertNotVisible("Plan my week")
    }

    /**
     * Dua home's + opens the Quick Actions sheet; its Skills row (Dua is not
     * the agent) answers with "Jaldi aa raha hai", and Voice opens VoiceLive.
     */
    @Test
    fun duaHomePlusOpensQuickActionsAndVoiceShowsComingSoon() {
        launch()
        switchTo(HomeMode.Dua)
        compose.onNodeWithContentDescription(ATTACH_LABEL).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Quick Actions").assertExists()
        compose.onNode(hasText(SKILLS_LABEL) and hasContentDescription(SKILLS_LABEL)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(COMING_SOON).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        compose.onNodeWithContentDescription(VOICE_LABEL).performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.VOICE_LIVE, currentRoute())
    }

    /** Dua home's welcome: badge, greeting and starters; a starter opens a new Dua chat. */
    @Test
    fun duaHomeStarterChipOpensANewDuaThreadWithThatText() {
        TokenStore.token = null
        launch()
        switchTo(HomeMode.Dua)
        assertVisible(DUA_GREETING)
        compose.onNodeWithText(DUA_STARTER).performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.CHAT, currentRoute())
        val id = navController.currentBackStackEntry?.arguments?.getString("chatId").orEmpty()
        assertTrue("a fresh Dua chat id", id.startsWith("dua-"))
        assertEquals(DUA_STARTER, navController.currentBackStackEntry?.arguments?.getString("message"))
    }

    /** The app launches straight into Dua home. */
    @Test
    fun theAppLaunchesIntoDuaHome() {
        launch()
        assertVisibleDescCount("Open menu", 1)
        assertVisibleDescCount("Dua mode", 1)
        assertVisibleDescCount("Agent mode", 1)
        assertVisibleDescCount("Settings", 1)
        assertVisible(DUA_GREETING)
        for (starter in DUA_STARTERS) assertVisible(starter)
        assertVisible(DUA_ASK_HINT)
        // Dua home has no Agent chat list, brand title, or + button.
        assertNotVisible(PINNED_HEADER)
        assertNotVisible(CHATS_HEADER)
        assertNotVisible(APP_NAME_TEXT)
        assertVisibleDescCount("New chat", 0)
    }

    // ------------------------------------------- Agent mode is the agent surface

    @Test
    fun agentModeShowsTheChatListAndTheAgentsTab() {
        launch()
        switchTo(HomeMode.Agent)

        // The reference's chat-list home: search row, hero card, Pinned · 1
        // with Dua Agent, the (empty) Chats section, and the floating dock.
        assertVisible("Search")
        assertVisible(AGENT_ROLE_LINE)
        assertVisible(PINNED_HEADER)
        assertVisible(DUA_AGENT_ROW)
        assertVisibleDescCount("Verified", 1)
        assertVisible(CHATS_HEADER)
        compose.onNodeWithTag("home_pull_to_refresh").assertExists()
        // No fake conversations: the section says so, and none of the old
        // sample rows is anywhere on screen.
        assertVisible("No conversations yet")
        for (agent in MANAGED_AGENTS) {
            assertNotVisible(agent)
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

        // And its right button is the plus ("New chat"), not the gear.
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
        assertVisibleDescCount(CLOSE_DRAWER, 1)
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
        compose.onNodeWithTag("create_agent_avatar_edit_badge", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("create_agent_avatar_preview").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Shape: ${AgentSilhouette.Drop.label}").assertIsSelected()
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
        switchTo(HomeMode.Dua)

        assertVisibleDescCount("Open menu", 1)
        assertVisibleDescCount("Dua mode", 1)
        assertVisibleDescCount("Agent mode", 1)
        assertVisibleDescCount("Settings", 1)
        assertVisible(DUA_ASK_HINT)
    }

    @Test
    fun theModeSwitchesBothWays() {
        launch()

        // Launch is Dua home. Dua -> Agent goes through the pill.
        assertVisible(DUA_GREETING)
        assertVisible(DUA_ASK_HINT)
        assertVisibleDescCount("Dua mode", 1)
        compose.onNodeWithContentDescription("Agent mode").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        // Agent home: avatar, brand, +, search, Personal Agent, Pinned, Chats, dock.
        assertVisibleDescCount("Open menu", 1)
        assertVisible(APP_NAME_TEXT)
        assertVisibleDescCount("New chat", 1)
        assertVisible("Search")
        assertVisible(AGENT_ROLE_LINE)
        assertVisible(PINNED_HEADER)
        assertVisible(DUA_AGENT_ROW)
        assertVisible(CHATS_HEADER)
        for (tab in HomeTab.entries) assertVisible(tab.label)
        // Option D: no Dua | Agent pill on Agent home.
        assertVisibleDescCount("Dua mode", 0)
        assertVisibleDescCount("Agent mode", 0)
        assertNotVisible(DUA_ASK_HINT)
        assertNotVisible(DUA_GREETING)

        // Agent -> Dua goes through the drawer toggle's "Dua" segment.
        openDrawer()
        assertVisible("Dua")
        assertVisibleDescCount("Switch to Dua mode", 1)
        modeRow("Dua").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        assertVisibleDescCount(CLOSE_DRAWER, 0)
        assertVisible(DUA_GREETING)
        for (starter in DUA_STARTERS) assertVisible(starter)
        assertVisible(DUA_ASK_HINT)
        assertVisibleDescCount("Dua mode", 1)
        assertVisibleDescCount("Agent mode", 1)
        assertNotVisible(CHATS_HEADER)
        assertNotVisible(PINNED_HEADER)
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
        // Expanded again: still no fake rows, just the honest empty line.
        assertVisible("No conversations yet")
        for (agent in MANAGED_AGENTS) assertNotVisible(agent)
    }

    /**
     * Wherever the app still shows sample/demo content, it carries the small
     * "Demo" label — and wherever the sample content was removed, the surface
     * says so honestly instead of wearing a Demo badge over a fake list.
     *
     * Still demo-labelled: Discover's suggested starters, the Profile plan
     * (no billing exists yet) and the offline canned reply.
     * Honestly empty now: the drawer history, Agent home's Chats section and
     * the Agents roster — no "Demo" badge can dress up data that is not there.
     */
    @Test
    fun sampleDataCarriesTheDemoLabel() {
        TokenStore.token = null
        launch()
        fun demoCount() = compose.onAllNodes(hasText(DEMO_LABEL, substring = true))
            .fetchSemanticsNodes().count { isOnScreen(it) }

        // Drawer: HISTORY is now the honest empty state, not a sample list.
        openDrawer()
        assertVisible("No conversations yet")
        assertEquals("drawer history ships no demo sample", 0, demoCount())

        // Profile plan is still a placeholder and says so.
        compose.onNodeWithContentDescription(DRAWER_PROFILE, substring = true).performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.SETTINGS, currentRoute())
        compose.onNodeWithText(SETTINGS_EDIT_PROFILE_LABEL).performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.PROFILE, currentRoute())
        assertTrue("profile plan carries Demo", demoCount() >= 1)
        // Profile was opened from Settings, so one Back lands on Settings and
        // a second Back returns to Home — the stack the rest of this test needs.
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.SETTINGS, currentRoute())
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())

        // Agent home's Chats section: honest empty state, no demo rows.
        switchTo(HomeMode.Agent)
        assertVisible("No conversations yet")
        assertEquals("agent home ships no demo sample", 0, demoCount())

        // Agents is empty and honest; Discover still suggests and says Demo.
        compose.onNodeWithText(HomeTab.Managed.label).performClick()
        compose.waitForIdle()
        assertVisible("No agents yet")
        assertEquals("agents screen ships no demo roster", 0, demoCount())
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        compose.onNodeWithText(HomeTab.Discover.label).performClick()
        compose.waitForIdle()
        assertTrue("Discover cards carry Demo", demoCount() >= 1)
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()

        // Offline canned reply in a thread.
        compose.onNodeWithText(DUA_AGENT_ROW).performClick()
        compose.waitForIdle()
        val before = demoCount()
        compose.onNode(hasSetTextAction()).performTextInput("Hi")
        compose.onNodeWithContentDescription(SEND_LABEL).performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("I've picked that up", substring = true))
                .fetchSemanticsNodes().count { isOnScreen(it) } >= 1
        }
        compose.waitForIdle()
        assertTrue("offline canned reply carries Demo", demoCount() > before)
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
        // The Discover screen carries the same dock, with Discover lit.
        assertTabSelected(HomeTab.Discover.label)

        // Back returns Home and the highlight follows the route (it used to
        // stay stuck on Discover).
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        assertTabSelected(HomeTab.Chats.label)
    }

    /**
     * The four tab screens show the same five-tab dock; switching tabs never
     * stacks screens (Home + one tab at most), Chats pops back to Home, and
     * Back from any tab returns Home.
     */
    @Test
    fun tabScreensShareTheDockAndNeverStack() {
        launch()
        switchTo(HomeMode.Agent)

        for ((tab, route) in listOf(
            HomeTab.Managed to DuaGenAIRoutes.AGENTS,
            HomeTab.Discover to DuaGenAIRoutes.DISCOVER,
            HomeTab.Memory to DuaGenAIRoutes.MEMORY,
            HomeTab.Artifacts to DuaGenAIRoutes.ARTIFACTS,
        )) {
            compose.onAllNodes(hasText(tab.label) and hasClickAction()).onLast().performClick()
            compose.waitForIdle()
            assertEquals(route, currentRoute())
            assertTabSelected(tab.label)
            // Exactly five tabs in the dock on every tab screen. Counted on
            // screen only: the closed drawer stays composed off screen and its
            // Dua | Agent toggle is also a pair of tabs.
            assertEquals(
                5,
                compose.onAllNodes(
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
                ).fetchSemanticsNodes().count { isOnScreen(it) },
            )
        }

        // Four tab hops later, Back still lands on Home: nothing stacked.
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())

        // Chats from a tab screen pops back to Home too.
        compose.onAllNodes(hasText(HomeTab.Discover.label) and hasClickAction()).onLast().performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasText(HomeTab.Chats.label) and hasClickAction()).onLast().performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        assertTabSelected(HomeTab.Chats.label)
    }

    /**
     * No real unread-memory source yet: by default the Memory tab draws no
     * dot and announces just "Memory". With hasUnreadMemory = true the old
     * "Memory, New updates" label comes back.
     */
    @Test
    fun memoryTabHasNoUnreadDotUntilARealSourceSaysSo() {
        val unread = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                ai.duagenai.design.screens.HomeBottomNav(
                    selected = HomeTab.Chats,
                    onSelect = {},
                    hasUnreadMemory = unread.value,
                )
            }
        }
        compose.waitForIdle()
        assertEquals(1, compose.onAllNodes(hasContentDescription("Memory")).fetchSemanticsNodes().size)
        assertEquals(
            0,
            compose.onAllNodes(hasContentDescription("Memory, New updates")).fetchSemanticsNodes().size,
        )
        val bounds = compose.onNode(hasContentDescription("Memory")).fetchSemanticsNode().boundsInRoot

        compose.runOnIdle { unread.value = true }
        compose.waitForIdle()
        assertEquals(
            1,
            compose.onAllNodes(hasContentDescription("Memory, New updates")).fetchSemanticsNodes().size,
        )
        // Same tab size with or without the dot: the dock layout does not move.
        assertEquals(
            bounds,
            compose.onNode(hasContentDescription("Memory, New updates")).fetchSemanticsNode().boundsInRoot,
        )
    }

    /**
     * Both section headers ("Pinned · 1", "Chats · 0") must be a 48dp-tall,
     * full-width tap target.
     *
     * They used to be `sizeIn(minHeight = 34.dp)` with no `fillMaxWidth`, so the
     * clickable box was 34dp tall and only as wide as its content (~100dp). Two
     * consequences, both pinned here:
     *
     *   1. 34dp is below the Material 3 / WCAG 2.5.8 48dp minimum touch target.
     *   2. The empty space to the right of the label looked like part of the
     *      header row but did nothing when tapped, because the Row wrapped its
     *      content instead of filling the list width.
     */
    @Test
    fun sectionHeadersAre48dpTallAndFullWidth() {
        launch()
        switchTo(HomeMode.Agent)

        val density = compose.density.density
        val minHeight = 48f * density - 1f
        val rootWidth = compose.onAllNodes(isRoot()).fetchSemanticsNodes().first().boundsInRoot.width
        // The row is inset by the screen gutter (14dp) on each side, so a
        // full-width header is the root width minus 28dp. 1dp of tolerance for
        // rounding.
        val minWidth = rootWidth - 2f * GUTTER_DP * density - 1f

        for (label in listOf(PINNED_HEADER, CHATS_HEADER)) {
            val b = compose.onNode(hasText(label) and hasClickAction())
                .fetchSemanticsNode().boundsInRoot
            assertTrue(
                "section header \"$label\" must be >= 48dp tall, was ${b.height / density}dp",
                b.height >= minHeight,
            )
            assertTrue(
                "section header \"$label\" must span the full row width, was " +
                    "${b.width / density}dp of ${rootWidth / density}dp",
                b.width >= minWidth,
            )
        }
    }

    /**
     * Every Agent-home element starts on ONE left edge: the screen gutter.
     *
     * The hero card used to be inset 10dp, the section headers 6dp and the empty
     * state 14dp, so the screen had three different left edges and read ragged.
     * All three now use `DuaGenAISpacing.gutter` (14dp), which is what the top bar
     * and the search row already used.
     */
    @Test
    fun agentHomeElementsShareOneLeftGutter() {
        launch()
        switchTo(HomeMode.Agent)

        val density = compose.density.density
        val expected = GUTTER_DP * density
        // 1dp of tolerance: the capsule clip and the avatar ring round by a
        // fraction of a pixel, and the hero card carries a 0.5dp border.
        val tolerance = 1f * density

        val heroLeft = compose
            .onNode(hasContentDescription(PERSONAL_AGENT_TITLE, substring = true) and hasClickAction())
            .fetchSemanticsNode().boundsInRoot.left
        val pinnedLeft = compose.onNode(hasText(PINNED_HEADER) and hasClickAction())
            .fetchSemanticsNode().boundsInRoot.left
        val chatsLeft = compose.onNode(hasText(CHATS_HEADER) and hasClickAction())
            .fetchSemanticsNode().boundsInRoot.left
        val emptyLeft = compose.onNodeWithTag("home_empty_chats_state")
            .fetchSemanticsNode().boundsInRoot.left

        for ((name, left) in listOf(
            "hero card" to heroLeft,
            "Pinned header" to pinnedLeft,
            "Chats header" to chatsLeft,
            "empty state" to emptyLeft,
        )) {
            assertTrue(
                "$name must start at the ${GUTTER_DP}dp gutter, was ${left / density}dp",
                kotlin.math.abs(left - expected) <= tolerance,
            )
        }
    }

    /**
     * The empty Chats state is centred in the list, not pinned under the header.
     *
     * It used to be a top-aligned Column with `padding(top = 12.dp)`, which left
     * roughly 80% of the screen empty below it and made the screen look broken
     * rather than intentionally empty. It now sits in a `fillParentMaxHeight` Box
     * with `contentAlignment = Center`.
     */
    @Test
    fun emptyChatsStateIsCentredNotStuckUnderTheHeader() {
        launch()
        switchTo(HomeMode.Agent)

        val density = compose.density.density
        val viewport = compose.onNodeWithTag("home_pull_to_refresh")
            .fetchSemanticsNode().boundsInRoot
        val headerBottom = compose.onNode(hasText(CHATS_HEADER) and hasClickAction())
            .fetchSemanticsNode().boundsInRoot.bottom
        val empty = compose.onNodeWithTag("home_empty_chats_state")
            .fetchSemanticsNode().boundsInRoot

        // Not hugging the header: a real gap, not the old 12dp.
        assertTrue(
            "empty state must clear the Chats header, gap was " +
                "${(empty.top - headerBottom) / density}dp",
            empty.top - headerBottom >= 40f * density,
        )

        // And it sits in the middle third of the list viewport, not at its top.
        val centre = (empty.top + empty.bottom) / 2f
        val lower = viewport.top + viewport.height * 0.30f
        val upper = viewport.top + viewport.height * 0.70f
        assertTrue(
            "empty state must be vertically centred, its centre was at " +
                "${(centre - viewport.top) / density}dp of ${viewport.height / density}dp",
            centre in lower..upper,
        )
    }

    /**
     * The section divider is inset to the content, not full-bleed.
     *
     * It used to be a bare `HorizontalDivider` with only vertical padding, so it
     * ran the whole 390dp screen width while every other element on Agent home is
     * inset. It read as a rule from a different layout system.
     *
     * It is now inset symmetrically on both sides to the section header's TEXT
     * left edge:
     *
     *   inset = gutter (14) + header inner padding (6) + icon (13) + xs (4) = 37dp
     *
     * Both sides use the same value. An earlier revision used 37dp on the left
     * and 20dp on the right, which lined the rule up with two different edges and
     * made it look off-centre. The inset is derived from the same constants the
     * composables use, so this test fails if either side drifts.
     */
    @Test
    fun sectionDividerIsInsetToTheContentNotFullBleed() {
        launch()
        switchTo(HomeMode.Agent)

        val density = compose.density.density
        val rootWidth = compose.onAllNodes(isRoot()).fetchSemanticsNodes().first().boundsInRoot.width
        val divider = compose
            .onNodeWithTag("home_section_divider", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        // 37dp: the "Pinned"/"Chats" label's left edge, used on BOTH sides.
        val inset = (GUTTER_DP + 6f + 13f + 4f) * density
        val expectedStart = inset
        val expectedEnd = rootWidth - inset
        val tolerance = 1.5f * density

        assertTrue(
            "divider must start on the header text edge (${expectedStart / density}dp), " +
                "was ${divider.left / density}dp",
            kotlin.math.abs(divider.left - expectedStart) <= tolerance,
        )
        assertTrue(
            "divider must end symmetrically at ${expectedEnd / density}dp, " +
                "was ${divider.right / density}dp",
            kotlin.math.abs(divider.right - expectedEnd) <= tolerance,
        )
        // Symmetry is the point: both insets must be equal, not merely close to
        // their own expected values.
        val leftInset = divider.left
        val rightInset = rootWidth - divider.right
        assertTrue(
            "divider insets must be symmetric, left ${leftInset / density}dp vs " +
                "right ${rightInset / density}dp",
            kotlin.math.abs(leftInset - rightInset) <= tolerance,
        )
        // And it must NOT be full-bleed any more.
        assertTrue(
            "divider must not span the full screen width, was " +
                "${divider.width / density}dp of ${rootWidth / density}dp",
            divider.width < rootWidth - 2f * GUTTER_DP * density,
        )
    }

    /**
     * The hero card carries its title only — no trailing label.
     *
     * The card used to render a right-aligned trailing label. That slot held a
     * hardcoded fake timestamp ("Mon") with no real source behind it, and it
     * competed with the title for width. It has been removed, so the title now
     * owns the full content width and nothing sits on the card's right edge.
     */
    @Test
    fun heroCardHasNoTrailingLabel() {
        launch()
        switchTo(HomeMode.Agent)

        val density = compose.density.density
        val card = compose
            .onNode(hasContentDescription(PERSONAL_AGENT_TITLE, substring = true) and hasClickAction())
            .fetchSemanticsNode().boundsInRoot

        // The old trailing-label slot must be gone entirely.
        assertEquals(
            "hero card must not render a trailing label any more",
            0,
            compose.onAllNodesWithTag("home_hero_trailing_label", useUnmergedTree = true)
                .fetchSemanticsNodes().size,
        )

        // The title must be the only text on the card's title line, and it must
        // now be free to use the card's full content width: its right edge should
        // reach past where the old 20dp-ish label slot began.
        val title = compose
            .onNodeWithText(PERSONAL_AGENT_TITLE, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "hero title must sit inside the card, title ${title.left / density}.." +
                "${title.right / density}dp vs card ${card.left / density}.." +
                "${card.right / density}dp",
            title.left >= card.left - 1f * density && title.right <= card.right + 1f * density,
        )
        // With no label competing for width the title is no longer squeezed into
        // the left portion of the card.
        val cardContentWidth = card.width - 20f * density // 10dp inner padding each side
        assertTrue(
            "hero title must be allowed the card's full content width, was " +
                "${title.width / density}dp of ${cardContentWidth / density}dp",
            title.width >= cardContentWidth * 0.5f,
        )
    }

    @Test
    fun homeTabForMapsTheFiveTabRoutesOnly() {
        assertEquals(HomeTab.Chats, homeTabFor(DuaGenAIRoutes.HOME))
        assertEquals(HomeTab.Managed, homeTabFor(DuaGenAIRoutes.AGENTS))
        assertEquals(HomeTab.Discover, homeTabFor(DuaGenAIRoutes.DISCOVER))
        assertEquals(HomeTab.Memory, homeTabFor(DuaGenAIRoutes.MEMORY))
        assertEquals(HomeTab.Artifacts, homeTabFor(DuaGenAIRoutes.ARTIFACTS))
        assertEquals(null, homeTabFor(DuaGenAIRoutes.SETTINGS))
        assertEquals(5, HomeTab.entries.size)
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
    fun theProfileScreenShowsIdentityRowsAndNoSignIn() {
        launch()
        openProfileFromDrawer()

        assertEquals(DuaGenAIRoutes.PROFILE, currentRoute())
        assertVisible("Profile")
        assertVisible("Name")
        assertVisible("About")
        assertVisible("Username")
        assertVisible("Reserve username")
        assertVisible("Phone")
        assertVisible("Links")
        assertVisible("Not set")
        // Sign in / Sign out live on Settings, once.
        assertNotVisible(SIGN_IN_LABEL)
        assertNotVisible(SESSION_TOKEN_LABEL)
        assertNotVisible(SETTINGS_SIGN_OUT_LABEL)
        assertNotVisible(EDIT_PROFILE_HELPER)
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
    fun theDrawerShowsTheHonestEmptyHistory() {
        launch()
        openDrawer()

        // No sample conversations are shipped, so the history says so…
        assertVisible("No conversations yet")

        // …and none of the old fake rows — titles or their avatars — is drawn.
        for (chat in CHATS) {
            // ChatGPT style would show the title only, no avatar — but with
            // honest empty history even the title must not appear.
            assertNotVisible(chat.title)
            assertVisibleDescCount(avatarLabel(chat.title), 0)
        }
    }

    @Test
    fun tappingEachChatRouteOpensThatChatWithTheSameCube() {
        launch()

        for (chat in CHATS) {
            openChatFromDrawer(chat)

            // ChatScreen is open…
            assertOnChat()

            // …with the shared composer under the (honest empty) conversation.
            // Every thread uses ChatBar's empty hint, not a per-title override.
            assertComposer(threadComposerHint(chat))

            // An empty thread draws the header avatar only. Say something, wait
            // for the canned reply, and the bot shape appears beside the
            // agent bubble. The user bubble stays plain.
            assertVisibleDescCount(avatarLabel(chat.title), THREAD_HEADER_CUBES)
            startConversation()
            assertVisibleDescCount(bubbleLabel(chat.title), THREAD_BUBBLE_CUBES)

            // Back to Home for the next row — never a second setContent.
            compose.onNodeWithContentDescription("Back").performClick()
            compose.waitForIdle()
            assertEquals("back must return to Home between rows", DuaGenAIRoutes.HOME, currentRoute())
        }
    }

    @Test
    fun theThreadDrawsTheChatsCubeBesideTheAgentBubble() {
        launch()

        openChatFromDrawer(CHATS.first { it.chatId == "chat_market_research" })
        startConversation()

        assertVisibleDescCount(avatarLabel("Market Research"), THREAD_HEADER_CUBES)
        assertVisibleDescCount(bubbleLabel("Market Research"), THREAD_BUBBLE_CUBES)
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
        assertVisibleDescCount(avatarLabel("Design Team \u00b7 5"), THREAD_HEADER_CUBES)
    }

    // ------------------------------------------------------------- back

    @Test
    fun theBackArrowReturnsToHome() {
        launch()

        openChatFromDrawer(CHATS.first { it.chatId == "chat_slides_assistant" })
        assertOnChat()

        // The TopAppBar's navigationIcon.
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()

        // Home is back: the thread's status line is gone and Dua home (the
        // launch home) is visible again.
        assertNotOnChat()
        assertVisible(DUA_GREETING)
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
        assertOnChat()

        // NavHost installs a BackHandler; dispatch through the activity so the
        // real system-back path is exercised, not just the toolbar button.
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()

        assertNotOnChat()
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

        for (chat in CHATS) {
            compose.runOnUiThread {
                navController.navigate(DuaGenAIRoutes.chat(chat.chatId, chat.title))
            }
            compose.waitForIdle()

            assertOnChat()
            assertVisibleDescCount(avatarLabel(chat.title), THREAD_HEADER_CUBES)
            startConversation()
            assertVisibleDescCount(bubbleLabel(chat.title), THREAD_BUBBLE_CUBES)

            compose.runOnUiThread { navController.popBackStack() }
            compose.waitForIdle()
        }
    }

    // ------------------------------------------------------- drawer chrome

    @Test
    fun theDrawerClosesFromTheScrimAndHasNoCloseX() {
        launch()
        openDrawer()
        assertVisible("HISTORY")
        // ChatGPT layout: no X. TalkBack closes it through M3's scrim.
        assertVisibleDescCount("Close menu", 0)
        compose.onNodeWithContentDescription(CLOSE_DRAWER).performClick()
        compose.waitForIdle()

        // Closed: the history section is off screen, Dua home (launch) is back.
        assertNotVisible("HISTORY")
        assertVisible(DUA_GREETING)
        assertVisible(DUA_ASK_HINT)
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
    }

    /** A drawer toggle segment, found by its TalkBack action. */
    private fun modeRow(target: String) =
        compose.onNode(hasContentDescription("Switch to $target mode") and hasClickAction())

    /**
     * Drawer Dua | Agent toggle, directly under the title: BOTH segments are
     * always there as tabs, the active mode's segment is the selected one, and
     * a tap closes the drawer and opens that mode's home.
     */
    @Test
    fun drawerToggleAgentOpensAgentHomeAndDuaReturnsToDuaHome() {
        launch()
        openDrawer()
        // Launch is Dua home: both segments present, Dua is selected.
        assertVisibleDescCount("Switch to Agent mode", 1)
        assertVisibleDescCount("Switch to Dua mode", 1)
        modeRow("Dua").assert(isSelected())
        modeRow("Agent").assert(isNotSelected())
        modeRow("Agent").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        // The old cube + swap-arrow row is gone: no "Mode" nav row.
        assertNotVisible("Mode")

        modeRow("Agent").performClick()
        compose.waitForIdle()
        // Drawer closed, Agent home is showing.
        assertVisibleDescCount(CLOSE_DRAWER, 0)
        assertNotVisible("HISTORY")
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        assertVisible(CHATS_HEADER)
        assertVisible(PINNED_HEADER)
        assertVisible(APP_NAME_TEXT)
        assertVisibleDescCount("New chat", 1)
        assertVisibleDescCount("Dua mode", 0)
        assertVisibleDescCount("Agent mode", 0)
        assertNotVisible(DUA_ASK_HINT)

        // Back in the drawer the Agent segment is selected; Dua returns home.
        openDrawer()
        modeRow("Agent").assert(isSelected())
        modeRow("Dua").assert(isNotSelected())
        modeRow("Dua").performClick()
        compose.waitForIdle()
        assertVisibleDescCount(CLOSE_DRAWER, 0)
        assertNotVisible("HISTORY")
        assertVisible(DUA_GREETING)
        for (starter in DUA_STARTERS) assertVisible(starter)
        assertVisible(DUA_ASK_HINT)
        assertVisibleDescCount("Dua mode", 1)
        assertVisibleDescCount("Agent mode", 1)
        assertNotVisible(CHATS_HEADER)

        // Tapping the ACTIVE segment keeps the mode and still closes the drawer.
        openDrawer()
        modeRow("Dua").performClick()
        compose.waitForIdle()
        assertVisibleDescCount(CLOSE_DRAWER, 0)
        assertVisible(DUA_GREETING)
        assertNotVisible(CHATS_HEADER)
    }

    /** Library / Images / Computer / Videos: snackbar inside the open drawer. */
    @Test
    fun comingSoonDrawerRowsShowTheSnackbarAndKeepTheDrawerOpen() {
        launch()
        // "See all" is part of this family, but only when a history exists —
        // with the honest empty history it is not shown at all.
        listOf("Library", "Images", "Computer", "Videos").forEach { label ->
            openDrawerIfClosed()
            compose.onNode(hasText(label) and hasClickAction()).performClick()
            compose.waitForIdle()
            assertVisible("Jaldi aa raha hai")
            // The drawer stayed open: its close X is still on screen.
            assertTrue(visibleDescCount(CLOSE_DRAWER) > 0)
            assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        }
    }

    /** Footer: green "Chat" pill + round avatar only. No name, no plan, no gear. */
    @Test
    fun theDrawerFooterIsTheChatPillAndTheAvatarOnly() {
        TokenStore.token = null
        launch()
        openDrawer()
        assertVisible("Chat")
        assertVisibleDescCount("Profile, Not signed in", 1)
        assertNotVisible("Not signed in")
        assertNotVisible("FREE")
        assertNotVisible("PRO")
        assertVisibleDescCount("Profile", 0)
    }

    /**
     * Header: "Dua Gen AI" (app_name) on ONE line beside the round search
     * button, inside a drawer no wider than 280dp.
     */
    @Test
    fun theDrawerTitleIsTheAppNameOnOneLineWithin280dp() {
        launch()
        switchTo(HomeMode.Dua)
        // Dua home draws no app name, so the only one on screen is the drawer's.
        assertNotVisible(APP_NAME_TEXT)
        openDrawer()
        assertVisible(APP_NAME_TEXT)

        val title = compose.onAllNodesWithText(APP_NAME_TEXT).fetchSemanticsNodes().first { isOnScreen(it) }
        val search = compose.onAllNodes(hasContentDescription("Search") and hasClickAction() and isEnabled())
            .fetchSemanticsNodes().first { isOnScreen(it) }
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        title.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertEquals("title must not wrap", 1, layouts.first().lineCount)
        assertTrue("title must end before the search button", title.boundsInRoot.right <= search.boundsInRoot.left)
        val density = compose.activity.resources.displayMetrics.density
        assertTrue(
            "drawer (search button edge) must sit inside 280dp",
            search.boundsInRoot.right <= 280f * density + 0.5f,
        )
    }

    /** Both mode-row texts fit the 280dp drawer on one line, unclipped. */
    @Test
    fun theModeRowTextFitsOnOneLine() {
        launch()
        // Launch is Dua home, so the first row is "Agent", then "Dua".
        val density = compose.activity.resources.displayMetrics.density
        for (text in listOf("Agent", "Dua")) {
            openDrawer()
            // Unmerged Text node inside the 280dp drawer (the Dua-home pill
            // also draws "Agent"/"Dua" in the strip beside the sheet).
            val node = compose.onAllNodesWithText(text, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .first { isOnScreen(it) && it.boundsInRoot.left < 200f * density }
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            assertEquals("\"$text\" must not wrap", 1, layouts.first().lineCount)
            // (Pixel widths are not checked here: this class runs Robolectric's
            // legacy graphics, which measures ~1px per glyph. The real-font fit
            // is checked by the NATIVE-graphics render.)
            assertFalse("\"$text\" must not be ellipsized", layouts.first().isLineEllipsized(0))
            modeRow(text).performClick()
            compose.waitForIdle()
        }
    }

    /** The round search button replaces the field; search is not built yet. */
    @Test
    fun theDrawerSearchButtonShowsComingSoon() {
        launch()
        openDrawer()
        // Home's own Search pill is disabled; the drawer's round button is the
        // one enabled, tappable "Search".
        compose.onNode(hasContentDescription("Search") and hasClickAction() and isEnabled()).performClick()
        compose.waitForIdle()
        assertVisible("Jaldi aa raha hai")
    }

    /** The green "Chat" pill is the old New chat: back to the home list. */
    @Test
    fun theDrawerChatPillStartsANewChat() {
        launch()
        openDrawer()
        assertVisible("HISTORY")
        compose.onNodeWithContentDescription("Start a new chat").performClick()
        compose.waitForIdle()
        // The drawer closed and launch home (Dua) is showing. The Chat pill
        // does not change mode.
        assertNotVisible("HISTORY")
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
        assertVisible(DUA_GREETING)
        assertVisible(DUA_ASK_HINT)
    }

    private fun openDrawerIfClosed() {
        if (visibleDescCount(CLOSE_DRAWER) == 0) openDrawer()
    }

    @Test
    fun theDrawerFooterShowsTheSavedCube() {
        launch()
        openDrawer()

        compose.onNodeWithTag(ai.duagenai.design.screens.DRAWER_AVATAR_TAG).assertExists()
    }

    // ------------------------------------------------- drawer avatar -> Settings

    /**
     * The drawer footer's avatar opens SETTINGS, not Profile.
     *
     * The avatar is found by its content description ("Profile, Not signed in"
     * while signed out). That label is also what keeps this test honest: if the
     * avatar ever went back to a no-op, the click would leave the route on HOME
     * and the assertion would fail.
     */
    @Test
    fun theDrawerAvatarOpensTheSettingsScreen() {
        launch()
        openDrawer()

        // The avatar is in the drawer footer, on screen while the sheet is open.
        assertVisibleDescCount("Profile, Not signed in", 1)

        compose.onNodeWithContentDescription(DRAWER_PROFILE, substring = true).performClick()
        compose.waitForIdle()

        assertEquals(
            "the drawer avatar must open Settings, not Profile",
            DuaGenAIRoutes.SETTINGS,
            currentRoute(),
        )

        // SettingsScreen is really on screen, not just the route: its signed-out
        // header and the Edit profile row are the screen's markers.
        assertVisible("Not signed in")
        assertVisible(SETTINGS_EDIT_PROFILE_LABEL)
        assertVisible(SETTINGS_SIGN_IN_LABEL)
    }

    /**
     * Settings → Edit profile still opens Profile.
     *
     * Profile is reached only this way (or from other Settings entry points),
     * never from the drawer footer avatar.
     */
    @Test
    fun settingsEditProfileOpensTheProfileScreen() {
        launch()
        openSettingsFromDrawer()
        assertEquals(DuaGenAIRoutes.SETTINGS, currentRoute())

        compose.onNodeWithText(SETTINGS_EDIT_PROFILE_LABEL).performClick()
        compose.waitForIdle()

        assertEquals(
            "Edit profile must open the existing profile route",
            DuaGenAIRoutes.PROFILE,
            currentRoute(),
        )
        assertVisible("Profile")
        assertVisible("Name")
        assertVisible("Reserve username")
    }

    /**
     * Back from Settings (opened from the drawer avatar) returns to the screen
     * the drawer was opened from.
     *
     * The avatar closes the drawer and pushes SETTINGS, so the back stack is
     * Home -> Settings and a pop lands on Home — the same behaviour every other
     * drawer destination has.
     */
    @Test
    fun backFromSettingsOpenedFromDrawerReturnsToHome() {
        launch()
        openDrawer()
        compose.onNodeWithContentDescription(DRAWER_PROFILE, substring = true).performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.SETTINGS, currentRoute())

        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()

        assertEquals(
            "back from Settings must land on Home",
            DuaGenAIRoutes.HOME,
            currentRoute(),
        )
        // Home is really back: Dua home (the launch home) is visible again.
        assertVisible(DUA_GREETING)
        assertVisible(DUA_ASK_HINT)
    }

    /**
     * The avatar is reachable from Agent's home too, and still opens Settings.
     *
     * The drawer is a sibling of the NavHost, so it is the same sheet in both
     * modes. This pins that the wiring is not Dua-only.
     */
    @Test
    fun theDrawerAvatarOpensSettingsFromAgentModeToo() {
        launch()
        switchTo(HomeMode.Agent)
        openDrawer()

        compose.onNodeWithContentDescription(DRAWER_PROFILE, substring = true).performClick()
        compose.waitForIdle()

        assertEquals(DuaGenAIRoutes.SETTINGS, currentRoute())
        assertVisible("Not signed in")
        assertVisible(SETTINGS_EDIT_PROFILE_LABEL)
    }

    // ------------------------------------------- Profile picture sheet

    @Test
    fun theProfilePictureSheetHasOnlyCameraAndGallery() {
        launch()
        openProfileFromDrawer()
        openProfilePictureSheet()
        assertVisible("Camera")
        assertVisible("Gallery")
        assertNotVisible("Choose cube")
        assertNotVisible("Import from Instagram")
        assertNotVisible("Import from Facebook")
        assertNotVisible("AI images")
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
        openSettingsFromHome()
        assertEquals(DuaGenAIRoutes.SETTINGS, currentRoute())

        compose.onNodeWithText("Appearance").performScrollTo()
        compose.waitForIdle()
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
        openSettingsFromHome()
        compose.onNodeWithText("Appearance").performScrollTo()
        compose.waitForIdle()
        // The row starts on the default.
        assertVisible("System default")

        compose.onNodeWithText("Appearance").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.APPEARANCE, currentRoute())

        compose.onNodeWithText("Dark").performClick()
        compose.waitForIdle()

        // Back to Settings: the row's subtitle is now the saved mode.
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.SETTINGS, currentRoute())
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("Dark")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Dark").performScrollTo()
        compose.waitForIdle()
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
        openSettingsFromHome()
        compose.onNodeWithText("Storage").performScrollTo()
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
        openSettingsFromHome()
        compose.onNodeWithText("Notifications").performScrollTo()
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
    /**
     * Agent home lists no conversations until real ones exist, so there is no
     * listed row to tap. Opening a listed chat's thread is pinned at the route
     * level by [tappingEachChatRouteOpensThatChatWithTheSameCube]; here the
     * product behaviour is the negative: the old sample row is gone.
     */
    @Test
    fun agentHomeListsNoFakeChatsToTap() {
        launch()
        switchTo(HomeMode.Agent)

        assertNotVisible("Code Reviewer")
        assertVisible("No conversations yet")
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())
    }

    /**
     * Agent's thread shows the conversation AND the composer.
     *
     * A thread starts honestly empty — "No messages yet" — and the shared
     * `ChatBar` pill sits under it so the conversation can start. Both
     * halves are asserted, because "the composer is there" is only meaningful
     * if the conversation it belongs to is still there.
     */
    @Test
    fun agentsThreadShowsTheConversationAndTheComposer() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(DUA_AGENT_ROW).performClick()
        compose.waitForIdle()

        // The thread is open…
        assertOnChat()

        // …it is Dua Agent's, so its top bar carries that name…
        assertVisible(DUA_AGENT_TITLE)

        // …and the pain area is the honest empty thread, not a scripted
        // dialogue. Saying something fills it — that flow has its own test.
        assertVisible("No messages yet")

        // The composer is there, with ChatBar's empty-thread hint.
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

        // The field is cleared. The thread now has a message, so the hint
        // moves off the empty-thread line and onto the active one.
        assertVisible("What's next?")
        assertNotVisible(AGENT_COMPOSER_HINT)

        // \u2026and Agent replies after the typing beat. The reply is a canned
        // string, so this waits for it rather than asserting it instantly.
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("I've picked that up", substring = true))
                .fetchSemanticsNodes().count { isOnScreen(it) } >= 1
        }
        assertVisibleSubstring("I've picked that up")
    }

    /**
     * The thread chat bar reads Attach, the text field, Dictate, then Voice
     * (replaced by Send once text is typed) — and every interactive control's
     * touch box is at least the 48dp M3 minimum.
     */
    @Test
    fun theChatBarToolbarOrderAndAll48dp() {
        openAgentThread()
        fun bounds(desc: String) =
            compose.onNodeWithContentDescription(desc).fetchSemanticsNode().boundsInRoot
        val min = 48f * compose.density.density - 1f
        val attach = bounds(ATTACH_LABEL)
        val field = compose.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot
        val dictate = bounds(DICTATE_LABEL)
        val voice = bounds(THREAD_VOICE_LABEL)
        assertTrue("Attach sits before text field", attach.center.x < field.center.x)
        assertTrue("Text field sits before Dictate", field.center.x < dictate.center.x)
        assertTrue("Dictate sits before Voice", dictate.center.x < voice.center.x)
        assertTrue("Text field touch box >= 48dp tall", field.height >= min)

        compose.onNode(hasSetTextAction()).performTextInput("Hi")
        compose.waitForIdle()
        val send = bounds(SEND_LABEL)
        assertTrue("Send sits after Dictate", dictate.right <= send.left + 1f)
        for (b in listOf(attach, dictate, voice, send)) {
            assertTrue("chat bar control must be >= 48dp wide", b.width >= min)
            assertTrue("chat bar control must be >= 48dp tall", b.height >= min)
        }
    }

    /** Quick Actions' Skills row opens the Skills sheet: five Dua Agent skills, all on, toggleable. */
    @Test
    fun thePlusSheetOpensTheSkillsSheetAndTogglesASkill() {
        openAgentThread()
        compose.onNodeWithContentDescription(ATTACH_LABEL).performClick()
        compose.waitForIdle()
        compose.onNode(hasText(SKILLS_LABEL) and hasContentDescription(SKILLS_LABEL)).performClick()
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
     * Quick Actions' Connectors row opens the MCP sheet: the master switch (on by default),
     * no invented external servers, and the built-in tool ids as status chips.
     */
    @Test
    fun thePlusSheetOpensTheMcpSheetWithTheBuiltInTools() {
        openAgentThread()
        compose.onNodeWithContentDescription(ATTACH_LABEL).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Connectors") and hasContentDescription("Connectors")).performClick()
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
        // The sheet's Skills row.
        compose.onNode(hasText(SKILLS_LABEL) and hasContentDescription(SKILLS_LABEL)).performClick()
        compose.waitForIdle()
        compose.onNode(hasContentDescription(DUA_AGENT_SKILL_TITLES.first())).assertExists()
    }

    /** Quick Actions sheet displays all 5 sections and Context/Automate respond cleanly. */
    @Test
    fun quickActionsSheet_contextMemoryAttachesChipAndAutomatePrefillsDraft() {
        openAgentThread()
        compose.onNodeWithContentDescription(ATTACH_LABEL).performClick()
        compose.waitForIdle()

        listOf("Agent tools", "Frequently used", "Create", "Automate", "Context").forEach { section ->
            compose.onNodeWithText(section).assertExists()
        }

        compose.onNode(hasText("Memory") and hasContentDescription("Memory"))
            .performScrollTo()
            .performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Remove Memory").assertExists()

        compose.onNodeWithContentDescription(ATTACH_LABEL).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Daily work brief") and hasContentDescription("Daily work brief"))
            .performScrollTo()
            .performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Prepare my daily work brief").assertExists()
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
        assertVisibleDescCount(THREAD_VOICE_LABEL, 1)
    }

    /** Back from Agent's thread lands on Agent's home, not on Dua's. */
    @Test
    fun backFromAgentsThreadReturnsToAgentsHome() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(DUA_AGENT_ROW).performClick()
        compose.waitForIdle()
        assertOnChat()

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
    @Suppress("USELESS_IS_CHECK")
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
        val sent = JSONObject(transport.bodies[0]!!)
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

        val autoBody = JSONObject(transport.bodies[0]!!)
        assertEquals("Dua", autoBody.getString("model"))
        assertEquals("auto", autoBody.getString("mode"))
        assertEquals(false, autoBody.getBoolean("stream"))
        assertFalse(autoBody.has("skills"))
        val autoMessage = autoBody.getJSONArray("messages").getJSONObject(0)
        assertEquals("user", autoMessage.getString("role"))
        assertEquals("Hi there", autoMessage.getString("text"))

        val agentBody = JSONObject(transport.bodies[1]!!)
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
        assertVisible("Name, about, username and photo")
        val editEnabled = compose.onAllNodes(
            hasText(SETTINGS_EDIT_PROFILE_LABEL) and hasClickAction(),
        ).fetchSemanticsNodes().count { isOnScreen(it) }
        assertTrue("Edit profile must open Profile", editEnabled >= 1)
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
    fun profileScreenHasIdentityRowsAndNoSignIn() {
        TokenStore.token = null
        compose.setContent {
            DuaGenAITheme(darkTheme = false) { ProfileScreen() }
        }
        compose.waitForIdle()
        assertVisible("Name")
        assertVisible("Reserve username")
        assertEquals(
            0,
            compose.onAllNodes(hasText(SIGN_IN_LABEL) and hasClickAction())
                .fetchSemanticsNodes().size,
        )
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

    // ------------------------------------------- genuine Teamily UI wiring

    /** Personal Agent's thread: its quick-action chips are live and send like typing. */
    @Test
    fun personalAgentThreadShowsTheQuickActionChips() {
        TokenStore.token = null
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(AGENT_ROLE_LINE).performClick()
        compose.waitForIdle()
        assertEquals(
            ai.duagenai.design.screens.PERSONAL_AGENT_CHAT_ID,
            navController.currentBackStackEntry?.arguments?.getString("chatId"),
        )
        assertVisible("Try asking:")
        val chip = ai.duagenai.design.screens.PERSONAL_AGENT_QUICK_ACTIONS.first()
        compose.onNodeWithText(chip).performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("I've picked that up", substring = true))
                .fetchSemanticsNodes().count { isOnScreen(it) } >= 1
        }
    }

    /**
     * Dua Agent's thread header pill: verified check; avatar/name open Dua
     * Agent's profile, whose robot action opens the workspace.
     */
    @Test
    fun duaAgentThreadHeaderWorkspaceOpensTheWorkspace() {
        openAgentThread()
        assertVisibleDescCount("Verified", 1)
        assertVisible("Invite")
        clickVisibleText(DUA_AGENT_TITLE)
        assertEquals(DuaGenAIRoutes.AGENT_PROFILE, currentRoute())
        assertEquals(AGENT_CHAT_ID, navController.currentBackStackEntry?.arguments?.getString("agentId"))
        compose.onNodeWithContentDescription("Open agent workspace").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.AGENT_WORKSPACE, currentRoute())
        assertVisible("Agent workspace")
        assertVisible(DUA_AGENT_TITLE)
    }

    // ------------------------------------- every agent: profile & workspace

    /** Clicks the one ON-SCREEN node showing [text] (the closed drawer also composes rows). */
    private fun clickVisibleText(text: String) {
        val matches = compose.onAllNodesWithText(text)
        val i = matches.fetchSemanticsNodes().indexOfFirst { isOnScreen(it) }
        assertTrue("expected \"$text\" on screen", i >= 0)
        matches[i].performClick()
        compose.waitForIdle()
    }

    /** Opens Webpage Developer's thread and taps its header pill. */
    private fun openWebpageDeveloperProfile() {
        launch()
        openChatFromDrawer(CHATS.first { it.chatId == "chat_webpage_developer" })
        clickVisibleText("Webpage Developer")
    }

    /** A regular agent's header pill opens THAT agent's profile, not Dua Agent's. */
    @Test
    fun aRegularAgentsHeaderOpensItsOwnProfile() {
        launch()
        openChatFromDrawer(CHATS.first { it.chatId == "chat_webpage_developer" })
        assertVisible("Invite")
        assertVisible(AGENT_COMPOSER_HINT)
        clickVisibleText("Webpage Developer")

        assertEquals(DuaGenAIRoutes.AGENT_PROFILE, currentRoute())
        val args = navController.currentBackStackEntry?.arguments
        assertEquals("chat_webpage_developer", args?.getString("agentId"))
        assertEquals("Webpage Developer", args?.getString("name"))
        assertTrue(compose.onAllNodesWithText("Webpage Developer").fetchSemanticsNodes().count { isOnScreen(it) } >= 2)
        assertNotVisible(DUA_AGENT_TITLE)
        assertVisible("AI")
    }

    /** A person's or group's thread keeps the plain header: no pill, no Invite. */
    @Test
    fun aPersonsThreadHasNoAgentPill() {
        launch()
        openChatFromDrawer(CHATS.first { it.chatId == "chat_david_park" })
        assertVisible(CHAT_MARKER)
        assertNotVisible("Invite")
    }

    /** Invite in the pill opens New Group with that agent selected. */
    @Test
    fun theHeaderInviteOpensThatAgentsNewGroup() {
        launch()
        openChatFromDrawer(CHATS.first { it.chatId == "chat_webpage_developer" })
        compose.onNodeWithText("Invite").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.AGENT_NEW_GROUP, currentRoute())
        assertEquals("chat_webpage_developer", navController.currentBackStackEntry?.arguments?.getString("agentId"))
        assertVisible("New Group")
        assertVisibleDescCount(avatarLabel("Webpage Developer"), 1)
    }

    /** The more-sheet has exactly Share, Clear Chat History and Delete agent. */
    @Test
    fun theProfileMoreSheetShowsThreeItems() {
        openWebpageDeveloperProfile()
        compose.onNodeWithContentDescription("More").performClick()
        compose.waitForIdle()
        assertVisible("Share")
        assertVisible("Clear Chat History")
        assertVisible("Delete agent")

        // Built-in agents cannot be deleted: the row is shown disabled, and a
        // tap opens no dialog.
        compose.onNodeWithText("Delete agent").assertIsNotEnabled()
        compose.onNodeWithText("Share").assertIsEnabled()
        compose.onNodeWithText("Clear Chat History").assertIsEnabled()
        compose.onNodeWithText("Delete agent").performClick()
        compose.waitForIdle()
        assertNotVisible("Delete Webpage Developer?")
        assertEquals(DuaGenAIRoutes.AGENT_PROFILE, currentRoute())
    }

    /** Clear Chat History asks first, then empties that agent's thread. */
    @Test
    fun clearChatHistoryEmptiesThatAgentsThread() {
        openWebpageDeveloperProfile()
        compose.onNodeWithContentDescription("More").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Clear Chat History").performClick()
        compose.waitForIdle()
        assertVisible("Clear chat history?")
        compose.onNodeWithText("Clear").performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()
        assertOnChat()
        assertVisibleDescCount(bubbleLabel("Webpage Developer"), 0)
    }

    /** Mute and Pin are real toggles on every agent's profile. */
    @Test
    fun muteAndPinToggleOnAnAgentsProfile() {
        openWebpageDeveloperProfile()
        val mute = compose.onNodeWithContentDescription("Mute Notifications")
        mute.performScrollTo()
        mute.assertIsOff()
        mute.performClick()
        // The flag round-trips through DataStore (off the main thread).
        waitUntilToggledOn("Mute Notifications")
        compose.onNodeWithContentDescription("Mute Notifications").assertIsOn()
        val pin = compose.onNodeWithContentDescription("Pin to Top")
        pin.assertIsOff()
        pin.performClick()
        waitUntilToggledOn("Pin to Top")
        compose.onNodeWithContentDescription("Pin to Top").assertIsOn()
    }

    /** The agent's workspace and Overview carry its name, not "Dua Agent". */
    @Test
    fun workspaceAndOverviewShowTheAgentsName() {
        openWebpageDeveloperProfile()
        compose.onNodeWithContentDescription("Open agent workspace").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.AGENT_WORKSPACE, currentRoute())
        assertVisible("Agent workspace")
        assertVisible("Webpage Developer")
        assertNotVisible(DUA_AGENT_TITLE)

        compose.onNodeWithText("Overview").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.AGENT_OVERVIEW, currentRoute())
        assertVisible("Pick up where Webpage Developer left off")
        assertVisible("Webpage Developer")
        assertNotVisible("Pick up where Dua Agent left off")
    }

    /** Waits for the switch labelled [label] to read On (Mute / Pin persist asynchronously). */
    private fun waitUntilToggledOn(label: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().any {
                it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ToggleableState) ==
                    androidx.compose.ui.state.ToggleableState.On
            }
        }
    }

    /** Creates one agent through the real Create Agent form; lands on Profile -> Agents. */
    private fun createAgentNamed(name: String) {
        if (currentRoute() != DuaGenAIRoutes.CREATE_AGENT) {
            compose.runOnUiThread { navController.navigate(DuaGenAIRoutes.CREATE_AGENT) }
            compose.waitForIdle()
        }
        compose.onNode(hasSetTextAction()).performTextInput(name)
        compose.waitForIdle()
        createButton().performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.PROFILE_AGENTS, currentRoute())
    }

    /** The chat id the Agents list row [name] opens (then back to the list). */
    private fun madeAgentIdOf(name: String): String {
        assertEquals(DuaGenAIRoutes.PROFILE_AGENTS, currentRoute())
        clickVisibleText(name)
        assertEquals(DuaGenAIRoutes.CHAT, currentRoute())
        val id = navController.currentBackStackEntry?.arguments?.getString("chatId").orEmpty()
        compose.runOnUiThread { navController.popBackStack() }
        compose.waitForIdle()
        return id
    }

    /**
     * Deleting the MIDDLE of three made agents removes only it: the others keep
     * their names, their stable ids, their own transcript and their own
     * Mute / Pin (ids used to be list positions, so the third agent inherited
     * the second one's data).
     */
    @Test
    fun aMadeAgentCanBeDeleted() {
        openCreateAgent()
        createAgentNamed("Alpha")
        createAgentNamed("Beta")
        createAgentNamed("Gamma")
        val alpha = madeAgentIdOf("Alpha")
        val beta = madeAgentIdOf("Beta")
        val gamma = madeAgentIdOf("Gamma")
        assertEquals(3, setOf(alpha, beta, gamma).size)
        assertTrue(listOf(alpha, beta, gamma).all { ai.duagenai.design.screens.isUserMadeAgent(it) })

        // Each agent's own data: a transcript line and its flags.
        val transcripts = androidx.lifecycle.ViewModelProvider(compose.activity)[
            ai.duagenai.design.screens.ChatTranscriptStore::class.java,
        ]
        compose.runOnUiThread {
            transcripts.append(beta, ai.duagenai.design.screens.SavedMessage(ai.duagenai.design.screens.SavedMessage.USER, "beta-only", "now"))
            transcripts.append(gamma, ai.duagenai.design.screens.SavedMessage(ai.duagenai.design.screens.SavedMessage.USER, "gamma-only", "now"))
        }
        val settings = ai.duagenai.design.screens.SettingsStore(compose.activity.applicationContext)
        runBlocking {
            settings.setAgentMuted(beta, true)
            settings.setAgentPinned(beta, true)
            settings.setAgentMuted(gamma, true)
            settings.setAgentPinned(gamma, true)
        }

        // Delete Beta from its profile.
        compose.runOnUiThread {
            navController.navigate(DuaGenAIRoutes.agent(DuaGenAIRoutes.AGENT_PROFILE, beta, "Beta"))
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("More").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Delete agent").assertIsEnabled()
        compose.onNodeWithText("Delete agent").performClick()
        compose.waitForIdle()
        assertVisible("Delete Beta?")
        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.HOME, currentRoute())

        // Beta's flags are dropped; Gamma's are still its own.
        compose.waitUntil(timeoutMillis = 5_000) {
            runBlocking { beta !in settings.mutedAgents.first() && beta !in settings.pinnedAgents.first() }
        }
        runBlocking {
            assertTrue(gamma in settings.mutedAgents.first())
            assertTrue(gamma in settings.pinnedAgents.first())
            assertFalse(alpha in settings.mutedAgents.first())
        }
        // Transcripts: Beta's is gone, Gamma's still ends with its own line.
        assertTrue(transcripts.threads.value[beta].orEmpty().isEmpty())
        assertEquals("gamma-only", transcripts.threads.value[gamma]?.lastOrNull()?.text)
        assertTrue(transcripts.threads.value[alpha].orEmpty().none { it.text == "beta-only" || it.text == "gamma-only" })

        // The list keeps Alpha and Gamma, with the SAME ids as before.
        compose.runOnUiThread { navController.navigate(DuaGenAIRoutes.PROFILE_AGENTS) }
        compose.waitForIdle()
        assertVisible("Alpha")
        assertVisible("Gamma")
        assertNotVisible("Beta")
        assertEquals(alpha, madeAgentIdOf("Alpha"))
        assertEquals(gamma, madeAgentIdOf("Gamma"))

        // Gamma's profile shows its own Mute / Pin, Alpha's shows none.
        compose.runOnUiThread {
            navController.navigate(DuaGenAIRoutes.agent(DuaGenAIRoutes.AGENT_PROFILE, gamma, "Gamma"))
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Mute Notifications").performScrollTo()
        waitUntilToggledOn("Pin to Top")
        compose.onNodeWithContentDescription("Mute Notifications").assertIsOn()
        compose.onNodeWithContentDescription("Pin to Top").assertIsOn()
        compose.runOnUiThread {
            navController.navigate(DuaGenAIRoutes.agent(DuaGenAIRoutes.AGENT_PROFILE, alpha, "Alpha"))
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Mute Notifications").performScrollTo()
        compose.onNodeWithContentDescription("Mute Notifications").assertIsOff()
        compose.onNodeWithContentDescription("Pin to Top").assertIsOff()
    }

    /** Legacy madeAgents lines get their old index id written in once; delete never shifts ids. */
    @Test
    fun madeAgentIdsAreStableAndLegacyLinesMigrate() {
        val legacy = "Old One\nOld Two\tStar\tPink\nOld Three"
        val migrated = ai.duagenai.design.screens.withMadeAgentIds(legacy)
        val lines = linesOf(migrated)
        assertEquals(
            listOf("made_agent_0", "made_agent_1", "made_agent_2"),
            lines.mapIndexed { i, l -> ai.duagenai.design.screens.madeAgentId(l, i) },
        )
        assertEquals(listOf("Old One", "Old Two", "Old Three"), lines.map { madeAgentName(it) })
        assertEquals(AgentSilhouette.Star, AgentSilhouette.fromKey(madeAgentShapeKey(lines[1])))
        // Idempotent.
        assertEquals(migrated, ai.duagenai.design.screens.withMadeAgentIds(migrated))
        // Deleting the middle keeps the third agent's id.
        val after = linesOf(ai.duagenai.design.screens.withoutMadeAgent(legacy, "made_agent_1"))
        assertEquals(
            listOf("made_agent_0", "made_agent_2"),
            after.mapIndexed { i, l -> ai.duagenai.design.screens.madeAgentId(l, i) },
        )
        // New lines get a fresh id that can never look like a legacy index.
        val fresh = madeAgentLine("New", AgentSilhouette.Star.name, AgentSwatch.Pink.name)
        val id = ai.duagenai.design.screens.madeAgentId(fresh, 0)
        assertTrue(id.startsWith("made_agent_u"))
        assertTrue(id != ai.duagenai.design.screens.madeAgentId(madeAgentLine("New", "", ""), 0))
    }

    /** An agent name with & ? / + survives the route encoding into its profile title. */
    @Test
    fun anAgentNameWithSpecialCharactersRoundTripsToItsProfile() {
        val name = "R&D ? Ops / A+B"
        launch()
        compose.runOnUiThread { navController.navigate(DuaGenAIRoutes.chat("made_agent_uspecial", name)) }
        compose.waitForIdle()
        assertEquals(name, navController.currentBackStackEntry?.arguments?.getString("title"))
        clickVisibleText(name)
        assertEquals(DuaGenAIRoutes.AGENT_PROFILE, currentRoute())
        val args = navController.currentBackStackEntry?.arguments
        assertEquals("made_agent_uspecial", args?.getString("agentId"))
        assertEquals(name, args?.getString("name"))
        assertTrue(compose.onAllNodesWithText(name).fetchSemanticsNodes().count { isOnScreen(it) } >= 1)
    }

    /**
     * Mute / Pin are per agent and persisted: muting Webpage Developer leaves
     * Slides Assistant alone, and the choice survives the host being torn
     * down and restored.
     */
    @Test
    fun muteAndPinStayPerAgentAndSurviveHostRecreation() {
        val settings = ai.duagenai.design.screens.SettingsStore(compose.activity.applicationContext)
        runBlocking { BUILT_IN_FLAG_IDS.forEach { settings.forgetAgent(it) } }
        navController = TestNavHostController(compose.activity).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        val restorer = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restorer.setContent {
            DuaGenAITheme(darkTheme = false) {
                DuaGenAINavHost(
                    navController = navController,
                    signedOutArtifacts = demoArtifacts,
                    signedOutMemories = sampleMemories,
                )
            }
        }
        compose.waitForIdle()
        fun openProfile(id: String, name: String) {
            compose.runOnUiThread {
                navController.navigate(DuaGenAIRoutes.agent(DuaGenAIRoutes.AGENT_PROFILE, id, name))
            }
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Mute Notifications").performScrollTo()
        }
        openProfile("chat_webpage_developer", "Webpage Developer")
        compose.onNodeWithContentDescription("Mute Notifications").assertIsOff()
        compose.onNodeWithContentDescription("Mute Notifications").performClick()
        compose.onNodeWithContentDescription("Pin to Top").performClick()
        waitUntilToggledOn("Pin to Top")
        waitUntilToggledOn("Mute Notifications")
        compose.onNodeWithContentDescription("Mute Notifications").assertIsOn()

        // Another agent is untouched.
        openProfile("chat_slides_assistant", "Slides Assistant")
        compose.onNodeWithContentDescription("Mute Notifications").assertIsOff()
        compose.onNodeWithContentDescription("Pin to Top").assertIsOff()

        // Tear the host down and restore it: the flags come back from DataStore.
        restorer.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        openProfile("chat_webpage_developer", "Webpage Developer")
        waitUntilToggledOn("Mute Notifications")
        compose.onNodeWithContentDescription("Mute Notifications").assertIsOn()
        compose.onNodeWithContentDescription("Pin to Top").assertIsOn()
        openProfile("chat_slides_assistant", "Slides Assistant")
        compose.onNodeWithContentDescription("Mute Notifications").assertIsOff()
    }

    /** Saving, replacing and clearing the photo bump the version and move the path; old files go. */
    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun theProfilePhotoSaveReplaceAndClearUpdateVersionAndPath() {
        val context = compose.activity.applicationContext
        val settings = ai.duagenai.design.screens.SettingsStore(context)
        runBlocking {
            settings.clearProfilePhoto()
            val v0 = settings.profilePhotoVersion.first()
            assertEquals(null, settings.profilePhotoPath.first())

            // Save (camera path): a big bitmap is stored at most 512px.
            val big = android.graphics.Bitmap.createBitmap(2000, 1000, android.graphics.Bitmap.Config.ARGB_8888)
            val p1 = settings.saveProfilePhoto(big)
            val v1 = settings.profilePhotoVersion.first()
            assertTrue(v1 > v0)
            assertEquals(p1, settings.profilePhotoPath.first())
            val shown = ai.duagenai.design.screens.SettingsStore.decodeSampledFile(p1)
            assertTrue("decoded=${shown?.width}x${shown?.height} file=${java.io.File(p1).length()}", shown != null && maxOf(shown.width, shown.height) <= 512)

            // Replace (gallery path): a NEW file, a higher version, the old file gone.
            val src = java.io.File(context.cacheDir, "big-gallery.jpg")
            src.outputStream().use { big.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, it) }
            val p2 = settings.saveProfilePhotoFromUri(android.net.Uri.fromFile(src))
            assertTrue(p2 != null && p2 != p1)
            val v2 = settings.profilePhotoVersion.first()
            assertTrue(v2 > v1)
            assertEquals(p2, settings.profilePhotoPath.first())
            assertFalse(java.io.File(p1).exists())
            val replaced = ai.duagenai.design.screens.SettingsStore.decodeSampledFile(p2!!)
            assertTrue(replaced != null && maxOf(replaced.width, replaced.height) <= 1024)

            // An unreadable uri saves nothing.
            assertEquals(null, settings.saveProfilePhotoFromUri(android.net.Uri.fromFile(java.io.File(context.cacheDir, "missing.jpg"))))
            assertEquals(p2, settings.profilePhotoPath.first())

            // Clear: no path, file gone, version bumped again.
            settings.clearProfilePhoto()
            assertEquals(null, settings.profilePhotoPath.first())
            assertFalse(java.io.File(p2).exists())
            assertTrue(settings.profilePhotoVersion.first() > v2)
        }
        // The sample size keeps the longest side at or above the target.
        assertEquals(1, ai.duagenai.design.screens.SettingsStore.sampleSizeFor(500, 300, 512))
        assertEquals(2, ai.duagenai.design.screens.SettingsStore.sampleSizeFor(1500, 800, 512))
        assertEquals(8, ai.duagenai.design.screens.SettingsStore.sampleSizeFor(4096, 3072, 512))
    }

    /**
     * The Agents screen ships no sample roster, so there is no card to tap
     * until the user creates a real agent (the Create Agent flow has its own
     * tests). The honest state: an empty note, and none of the old fake cards.
     */
    @Test
    fun anAgentsCardOpensThatAgentsThread() {
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Managed.label).performClick()
        compose.waitForIdle()

        assertVisible("No agents yet")
        assertNotVisible("Slides Assistant")
        assertEquals(DuaGenAIRoutes.AGENTS, currentRoute())
    }

    /** A Discover card starts a NEW thread carrying the card as its first message. */
    @Test
    fun aDiscoverCardStartsANewThread() {
        TokenStore.token = null
        launch()
        switchTo(HomeMode.Agent)
        compose.onNodeWithText(HomeTab.Discover.label).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Draft a landing page").performClick()
        compose.waitForIdle()
        assertEquals(DuaGenAIRoutes.CHAT, currentRoute())
        val args = navController.currentBackStackEntry?.arguments
        assertTrue(args?.getString("chatId").orEmpty().startsWith("dua-"))
        assertEquals("Draft a landing page", args?.getString("message"))
    }

    /** Overview shows clean values instead of the raw [agent-id] / [agent-url] placeholders. */
    @Test
    fun theOverviewHasNoRawAgentPlaceholders() {
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                ai.duagenai.design.screens.DuaAgentOverviewScreen()
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Agent ID: Not set").assertExists()
        compose.onNodeWithText("Public URL: Not set").assertExists()
        assertEquals(0, compose.onAllNodesWithText("[agent-id]").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("[agent-url]").fetchSemanticsNodes().size)
    }

    /** The session file saves and clears the token, and lives outside Auto Backup. */
    @Test
    fun theSessionPersistenceSavesAndClearsTheToken() {
        val context = compose.activity.applicationContext
        val persistence = ai.duagenai.design.screens.SessionPersistence.get(context)
        runBlocking {
            persistence.write("tok-saved")
            assertEquals("tok-saved", persistence.read())
            persistence.write(null)
            assertEquals(null, persistence.read())
        }
        // noBackupFilesDir is `<data>/no_backup`, which Auto Backup never includes.
        assertEquals(context.noBackupFilesDir.name, persistence.file.parentFile?.name)
        assertEquals(ai.duagenai.design.screens.SessionPersistence.FILE_NAME, persistence.file.name)
        assertTrue("the session file must exist after a write", persistence.file.exists())
    }

    /** A saved token is restored at startup, and Sign out (null) removes it from disk. */
    @Test
    fun persistToRestoresTheSavedTokenAndSignOutClearsIt() {
        val context = compose.activity.applicationContext
        val persistence = ai.duagenai.design.screens.SessionPersistence.get(context)
        runBlocking { persistence.write("tok-restored") }
        TokenStore.token = null
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
        try {
            TokenStore.persistTo(context, scope)
            val deadline = System.currentTimeMillis() + 5_000
            while (TokenStore.token == null && System.currentTimeMillis() < deadline) Thread.sleep(20)
            assertEquals("tok-restored", TokenStore.token)

            TokenStore.clear()
            val clearBy = System.currentTimeMillis() + 5_000
            var saved: String? = "tok-restored"
            while (saved != null && System.currentTimeMillis() < clearBy) {
                saved = runBlocking { persistence.read() }
                if (saved != null) Thread.sleep(20)
            }
            assertEquals(null, saved)
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            TokenStore.token = null
        }
    }

    // ------------------------------------------------------------- labels

    /**
     * These mirror the exact strings the screens pass to CubeAvatar. They are
     * duplicated here on purpose: if a screen changes its label, the test must
     * fail rather than silently keep matching.
     */

    /** Chat header & agent avatar: `"<name> avatar"`. */
    private fun avatarLabel(title: String) = "$title avatar"

    /** Chat message bubbles: `"<name> bubble"`. */
    private fun bubbleLabel(title: String) = "$title bubble"

    /**
     * The composer hint an empty thread shows. ChatBar picks `"Ask Dua..."`
     * for every conversation; [chat] is unused and kept so call sites stay
     * obvious if a thread ever needs its own hint again.
     */
    private fun threadComposerHint(@Suppress("UNUSED_PARAMETER") chat: ChatCase) =
        AGENT_COMPOSER_HINT
}

/** Create Agent strings, as in res/values/strings.xml. */
private const val CREATE_AGENT_TITLE = "Create Agent"

/** Built-in agents whose persisted Mute / Pin the tests reset after each run. */
private val BUILT_IN_FLAG_IDS = listOf("chat_webpage_developer", "chat_slides_assistant")
private const val CREATE_AGENT_NAME_HINT = "Name your Agent"
private const val CREATE_AGENT_ACTION = "Create"
