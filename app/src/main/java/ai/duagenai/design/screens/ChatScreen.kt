package ai.duagenai.design.screens

import ai.duagenai.design.theme.PandaMark
import ai.duagenai.design.theme.DemoBadge
import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Parcel
import android.os.Parcelable
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import java.util.Locale
import ai.duagenai.design.R
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.TeamilyShapes
import ai.duagenai.design.theme.teamilyCanvas
import ai.duagenai.design.theme.teamilyPalette

/* ============================================================================
 *  Chat — M3 components used:
 *      Scaffold, TopAppBar (with navigationIcon), IconButton, Surface (bubbles),
 *      Icon
 *  Bubbles use the brand's 20dp / 6dp tail shape from DuaGenAIShapeTokens.
 *
 *  The cube in the header and beside every message is THIS CONVERSATION's own
 *  cube, derived from its stable `chatId` with the same `CubeCatalog.forEntity`
 *  mapping Home's chat rows use — so opening a row shows exactly the cube that
 *  row displayed, and picking a new avatar in Profile never touches a thread.
 *  The user's saved cube (CubeStore) appears on Home's top bar and Profile only.
 *
 *  `chatId` is a required parameter: the nav host passes the id of the row that
 *  was tapped, and `onBack` pops to Home.
 *
 *  COMPOSER — the adaptive floating ChatBar
 *  -----------------------------------------
 *  The thread's composer is [ChatBar] (ChatBar.kt):
 *  `+` opens ComposerPlusSheet (Quick Actions), which hands off to
 *  ComposerSkillsSheet / ComposerMcpSheet; Mic triggers speech dictation; the
 *  trailing action morphs between Voice Live, Send, and Stop while Agent's
 *  reply is pending (`agentTyping`).
 *
 *  HEADERS — the two pinned agents
 *  -------------------------------
 *  EVERY agent thread (built-in or made, see [isAgentChatId]) gets the agent
 *  header pill ([AgentChatHeaderPill]): back · avatar + badge · name · Invite.
 *  Avatar / name open that agent's profile, Invite its New Group; the
 *  workspace is one tap further, on the profile's robot action. Dua threads,
 *  people and groups keep the plain top bar with their own cube avatar.
 *
 *  The composer sits in `Scaffold`'s `bottomBar`, so the `inner` padding the
 *  LazyColumn consumes already includes its height — the last message can never
 *  hide underneath it, and the IME inset is handled by the Scaffold too.
 *  The top bar uses the canvas colour, not a transparent fill, so a long
 *  thread cannot scroll through the title.
 *
 *  PER-THREAD STATE
 *  ----------------
 *  `messages` and `draft` are `rememberSaveable` KEYED ON `chatId`. The key is
 *  what makes the state per-thread: a message sent in Agent's thread is stored
 *  under Agent's key, so it can never appear in another conversation, and a
 *  half-typed draft does not leak between threads. The key also means the state
 *  survives rotation and back-navigation.
 * ==========================================================================*/

/**
 * The conversation the @Preview frames render.
 *
 * It is one of Home's `ChatRow.chatId` values: both screens turn it into a cube
 * with `CubeCatalog.forEntity`, so the row's avatar and this thread's avatar are
 * the same value by construction rather than by agreement.
 *
 * It is used ONLY by the previews. [ChatScreen] has no default `chatId` on
 * purpose — a default would let a caller open "some chat" and silently show the
 * wrong conversation's cube, which is exactly the bug this wiring removes.
 */
const val SAMPLE_CHAT_ID = "chat_webpage_developer"

/**
 * Dua Work's local conversation id.
 *
 * The hero card and the pinned "Dua Work" row on Agent home open this thread. A
 * signed-in send posts `mode: "agent"` and `stream: true` to the engine, with
 * the enabled skill ids. It is a stable id, so `CubeCatalog.forEntity` gives
 * the thread a cube the same way it gives every other thread one.
 */
const val AGENT_CHAT_ID = "chat_agent"

/**
 * True for the single official Dua Work agent thread — whether opened from
 * Home's local route ([AGENT_CHAT_ID]) or from backend agent id `"dua-work"` /
 * legacy fallback `"dua-agent"`.
 */
fun isOfficialAgentThread(chatId: String): Boolean =
    chatId == AGENT_CHAT_ID || isOfficialAgentId(chatId)

/** The title Agent's thread shows in its preview top bar. */
const val AGENT_CHAT_TITLE = DUA_WORK_TITLE

sealed interface Message {
    val text: String
    val time: String
    data class Agent(override val text: String, override val time: String) : Message
    data class User(override val text: String, override val time: String) : Message
}

/**
 * One message as the thread STORES it.
 *
 * The seed threads below are `Message` (a sealed type, pleasant to author); the
 * live list is a flat [SavedMessage] because it has to survive
 * `rememberSaveable`, which needs a single Bundle-storable shape rather than a
 * sealed hierarchy. [Message.toSaved] is the one-way bridge, so the seed and the
 * live list can never disagree about what a message is.
 *
 * It is `Parcelable` by hand rather than with `@Parcelize` so the file does not
 * depend on the kotlin-parcelize plugin being applied in the consuming module.
 */
data class SavedMessage(
    val who: String,
    val text: String,
    val time: String,
    val agentId: String? = null,
    val agentName: String? = null,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(who)
        dest.writeString(text)
        dest.writeString(time)
        dest.writeString(agentId)
        dest.writeString(agentName)
    }

    companion object {
        const val AGENT = "agent"
        const val USER = "user"

        @JvmField
        val CREATOR: Parcelable.Creator<SavedMessage> =
            object : Parcelable.Creator<SavedMessage> {
                override fun createFromParcel(source: Parcel): SavedMessage = SavedMessage(
                    who = source.readString().orEmpty(),
                    text = source.readString().orEmpty(),
                    time = source.readString().orEmpty(),
                    agentId = source.readString(),
                    agentName = source.readString(),
                )

                override fun newArray(size: Int): Array<SavedMessage?> = arrayOfNulls(size)
            }
    }
}

/** The seed -> stored bridge. `Agent` becomes [SavedMessage.AGENT], else user. */
private fun Message.toSaved(): SavedMessage = SavedMessage(
    who = if (this is Message.Agent) SavedMessage.AGENT else SavedMessage.USER,
    text = text,
    time = time,
)

/**
 * Agent's canned reply.
 *
 * Used only while [TokenStore.token] is null and no [ChatDataSource] is
 * injected, so previews and the existing UI tests stay offline. A signed-in
 * session sends the turn to the live backend instead.
 */
private const val AGENT_REPLY =
    "Got it — I've picked that up and I'm on it. I'll report back here as soon as it's done."

/** How long the typing indicator shows before Agent's reply lands. */
private const val AGENT_REPLY_DELAY_MS = 900L

/** The clock label a freshly sent message carries, e.g. "9:41 AM". */
internal fun nowLabel(): String =
    java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date())

/**
 * Lightweight chat markdown so an agent reply does not show raw `* **Heading:**`.
 *
 * `**bold**` becomes bold, `` `code` `` becomes monospace, and a line that
 * starts with `* ` or `- ` becomes a `• ` bullet. Anything else is left as text.
 */
internal fun formatChatMarkdown(raw: String): AnnotatedString {
    val normalized = raw.lineSequence().joinToString("\n") { line ->
        val lead = line.takeWhile { it == ' ' || it == '\t' }
        val body = line.substring(lead.length)
        when {
            body.startsWith("* ") -> lead + "• " + body.removePrefix("* ")
            body.startsWith("- ") -> lead + "• " + body.removePrefix("- ")
            else -> line
        }
    }
    return buildAnnotatedString {
        var index = 0
        while (index < normalized.length) {
            if (normalized.startsWith("**", index)) {
                val end = normalized.indexOf("**", startIndex = index + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(normalized.substring(index + 2, end))
                    }
                    index = end + 2
                    continue
                }
            }
            if (normalized[index] == '`') {
                val end = normalized.indexOf('`', startIndex = index + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(normalized.substring(index + 1, end))
                    }
                    index = end + 1
                    continue
                }
            }
            append(normalized[index])
            index++
        }
    }
}

/**
 * PREVIEW-ONLY fixture for Agent's thread — the conversation the pinned card
 * opens, written to Agent's role from the product model: the user asks for
 * work, Agent reports what it did across the agents it manages.
 *
 * It seeds nothing in the real app: [ChatScreen] starts every thread empty
 * and only the @Previews pass this list, so a design preview shows a
 * conversation instead of a blank canvas.
 */
private val agentThread = listOf(
    Message.User(
        "I need the Q3 launch handled across our workspace agents.",
        "9:31 AM",
    ),
    Message.Agent(
        "I've split that across three agents. Welcome to your workspace! Task tracking, research, and documentation pipelines are active.",
        "9:31 AM",
    ),
    Message.User("Can you monitor the project goals and keep the team updated?", "9:33 AM"),
    Message.Agent(
        "Workspace monitoring is active. I will track progress and deliver updates directly to this channel.",
        "9:34 AM",
    ),
)

/** PREVIEW-ONLY fixture for an ordinary thread. See [agentThread]. */
private val sampleThread = listOf(
    Message.User(
        "Hello! What tools and capabilities are available in this workspace?",
        "9:38 AM",
    ),
    Message.Agent(
        "Welcome to your workspace agent! You can create documents, websites, code, and automate workflows via the + menu.",
        "9:38 AM",
    ),
    Message.User("Can we integrate our team skills and MCP tools here?", "9:40 AM"),
    Message.Agent(
        "Yes, Skills and MCP Tools are fully integrated and accessible directly from the composer below.",
        "9:41 AM",
    ),
)

@Composable
fun ChatScreen(
    // Required, not defaulted: the caller must say WHICH conversation this is,
    // because the cube is derived from it. A default here would silently show
    // one chat's cube inside another chat's thread.
    chatId: String,
    chatTitle: String = "Webpage Developer",
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    // The composer's agent tools, hoisted by the nav host so the choice is one
    // app-wide setting, not per thread. Defaults keep every existing caller (and
    // the @Previews) compiling: with no handler the screen keeps its own copy.
    enabledSkillIds: Set<String> = DEFAULT_DUA_SKILLS,
    onToggleSkill: ((String) -> Unit)? = null,
    mcpEnabled: Boolean = true,
    onToggleMcp: ((Boolean) -> Unit)? = null,
    // Agent threads only (every agent, built-in or made): non-null swaps the
    // top bar for the agent header pill, whose avatar / name open THIS agent's
    // profile. Null (Dua threads, people, groups) keeps the plain top bar.
    onOpenAgentProfile: (() -> Unit)? = null,
    /** Create tiles in the composer + sheet. */
    onOpenCreate: ((String) -> Unit)? = null,
    /** Opens full-screen Gemini Live voice session. */
    onOpenVoiceLive: () -> Unit = {},
    /** The agent header pill's "Invite": this agent's New Group. */
    onInvite: () -> Unit = {},
    // Where the transcript LIVES. A ViewModel, not `rememberSaveable`: see
    // ChatTranscriptStore for why a whole conversation must never go into the
    // saved-instance Bundle. Hoisted by the nav host so every thread shares one
    // retained store; the default keeps the @Previews working.
    transcriptStore: ChatTranscriptStore = viewModel(),
    // Where a live reply comes from. Null uses [RemoteChatDataSource] once a
    // session token exists, and the canned offline reply when it does not. A
    // test injects a fake; the default keeps every existing caller compiling.
    chatDataSource: ChatDataSource? = null,
    /**
     * A NEW thread's first message (Dua home's composer Send). Non-null: the
     * thread starts empty (no seed) and this is sent once, as if typed, so the
     * reply lands here. Ignored once the thread has any message.
     */
    initialMessage: String? = null,
    /**
     * The messages a thread starts with. EMPTY by default: a real conversation
     * has no history until someone says something, so opening any thread shows
     * an honest empty canvas with the composer — not a scripted fake dialogue.
     * Only the @Previews pass [sampleThread] / [agentThread], purely so a
     * design preview shows bubbles instead of a blank page.
     */
    seed: List<Message> = emptyList(),
    dynamicSkills: List<SkillItem>? = null,
) {
    // An incoming first message starts an EMPTY thread and is sent once — it
    // must never sit behind a scripted dialogue. Otherwise the thread shows
    // exactly the caller's [seed], which defaults to empty.
    val initialSeed = if (initialMessage != null) emptyList() else seed
    // The thread's initial content comes ONLY from the caller. Nothing here
    // invents a conversation.
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    // PER-THREAD message state, held in a RETAINED ViewModel — NOT in
    // `rememberSaveable`.
    //
    // WHY: `rememberSaveable` writes its value into the Activity's
    // saved-instance Bundle, which is a Binder transaction with a ~1 MB budget.
    // A whole conversation (or a few threads' worth) overflows it and the
    // process dies with `TransactionTooLargeException` while backgrounding —
    // the user sees "the app crashes when I switch away". A transcript is
    // application state, so it lives in the store, which survives rotation and
    // is never serialised into the Bundle.
    //
    // The seed is rendered on the first frame (a pure read) and committed by the
    // effect below, so the thread is never briefly empty.
    val threads by transcriptStore.threads.collectAsState()
    val messages: List<SavedMessage> = threads[chatId] ?: initialSeed.map { it.toSaved() }
    LaunchedEffect(chatId) { transcriptStore.ensure(chatId, initialSeed.map { it.toSaved() }) }
    // The composer's draft, saved the same way so a half-typed line survives a
    // rotation. Also keyed on `chatId`, so drafts do not leak between threads.
    // A draft is ONE short string, so the Bundle budget is a non-issue — this is
    // exactly the state that belongs in `rememberSaveable`.
    val draftState = rememberSaveable(chatId, saver = TextFieldState.Saver) { TextFieldState("") }
    // Agent's reply is shown after a short beat, with the typing indicator up in
    // the meantime. Plain `remember`, NOT `rememberSaveable`: the indicator is
    // transient, and the coroutine that would clear it died with the old
    // composition — restoring it would leave a spinner that never resolves.
    var agentTyping by remember(chatId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // The pending reply, so Stop can cancel it.
    var replyJob by remember { mutableStateOf<Job?>(null) }

    // Local fallback for the agent tools when no caller hoists them (previews).
    var localSkills by rememberSaveable { mutableStateOf(ArrayList(enabledSkillIds)) }
    var localMcp by rememberSaveable { mutableStateOf(mcpEnabled) }
    val skills: Set<String> = if (onToggleSkill != null) enabledSkillIds else localSkills.toSet()
    val toggleSkill: (String) -> Unit = onToggleSkill ?: { id ->
        localSkills = ArrayList(if (id in localSkills) localSkills - id else localSkills + id)
    }
    val mcp = if (onToggleMcp != null) mcpEnabled else localMcp
    val toggleMcp: (Boolean) -> Unit = onToggleMcp ?: { localMcp = it }
    val listState = rememberLazyListState()
    var remoteSkills by remember { mutableStateOf<List<SkillItem>?>(null) }
    LaunchedEffect(chatId) {
        if (TokenStore.token != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    remoteSkills = RemoteSkillsDataSource().fetch(if (isOfficialAgentThread(chatId)) OFFICIAL_AGENT_ID else null)
                } catch (_: Exception) {}
            }
        }
    }

    // Composer sheets (+ / Skills / MCP).
    var sheet by rememberSaveable(chatId) { mutableStateOf(ComposerSheet.None) }
    var contextChips by rememberSaveable(chatId) { mutableStateOf(listOf<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val comingSoon = stringResource(R.string.composer_coming_soon)
    val showComingSoon: () -> Unit = {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(comingSoon)
        }
    }
    val dictatePrompt = stringResource(R.string.voice_dictate_prompt)
    val dictateUnavailable = stringResource(R.string.voice_dictate_unavailable)
    val dictateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val existing = draftState.text.toString()
                val merged = if (existing.isBlank()) spoken else "$existing $spoken"
                draftState.edit { replace(0, length, merged) }
            }
        }
    }
    val handleDictate: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, dictatePrompt)
        }
        try {
            dictateLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(dictateUnavailable)
            }
        } catch (_: Exception) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(dictateUnavailable)
            }
        }
    }
    val isOfficialAgent = isOfficialAgentThread(chatId)
    val timeoutMessage = stringResource(R.string.chat_request_timeout)
    val offlineMessage = stringResource(R.string.chat_request_offline)
    // A failed live turn stays out of the transcript. These two short strings
    // are enough to redraw the error bubble after rotation.
    var errorText by rememberSaveable(chatId) { mutableStateOf("") }
    var retryText by rememberSaveable(chatId) { mutableStateOf("") }

    // The list is the conversation plus, while Agent is "typing", one extra row.
    val showError = errorText.isNotEmpty() && !agentTyping
    val rows = messages.size +
        (if (agentTyping) 1 else 0) +
        (if (showError) 1 else 0)

    // Keep the newest message in view. Keyed on the row count so it fires when a
    // message is added (and when the typing row or the error bubble appears).
    LaunchedEffect(rows, errorText) {
        if (rows > 0) listState.animateScrollToItem(rows - 1)
    }

    fun launchReply(text: String, recordUser: Boolean) {
        // Guarded twice over: blank text, or a reply still in flight.
        if (text.isEmpty() || agentTyping) return
        if (recordUser) {
            // The store is the single writer: the screen never holds a mutable copy
            // of the transcript, so a message cannot be lost by a stale local list.
            transcriptStore.append(chatId, SavedMessage(SavedMessage.USER, text, nowLabel()))
            draftState.edit { replace(0, length, "") }
            contextChips = emptyList()
        }
        errorText = ""
        retryText = ""
        agentTyping = true
        // Offline until there is a session or an injected source. This branch is
        // what the existing UI tests drive: a 900ms beat, then [AGENT_REPLY].
        val source = chatDataSource
        val useLive = source != null || TokenStore.token != null
        val skillSnapshot = skills
        replyJob = scope.launch {
            if (!useLive) {
                delay(AGENT_REPLY_DELAY_MS)
                agentTyping = false
                transcriptStore.append(chatId, SavedMessage(SavedMessage.AGENT, AGENT_REPLY, nowLabel()))
            } else {
                val actualSource = source ?: RemoteChatDataSource()
                var streamedAny = false
                try {
                    withContext(Dispatchers.IO) {
                        actualSource.streamMessage(
                            chatId = chatId,
                            text = text,
                            skills = skillSnapshot,
                            onChunk = { agentId, agentName, delta ->
                                streamedAny = true
                                agentTyping = false
                                transcriptStore.appendOrUpdateStreaming(
                                    chatId = chatId,
                                    agentId = agentId,
                                    agentName = agentName,
                                    delta = delta,
                                    time = nowLabel(),
                                )
                            },
                            onAgentDone = { _, _ ->
                                agentTyping = false
                            },
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiError) {
                    if (!streamedAny) {
                        errorText = if (e.timedOut) timeoutMessage else e.userMessage
                        if (e.retryable) retryText = text
                    }
                } catch (e: Exception) {
                    if (!streamedAny) {
                        errorText = e.message?.takeIf { it.isNotBlank() } ?: offlineMessage
                        retryText = text
                    }
                } finally {
                    agentTyping = false
                }
            }
        }
    }

    fun send() {
        launchReply(draftState.text.toString().trim(), recordUser = true)
    }

    // One tap resends the failed user prompt. That bubble is already in the
    // thread, so Retry does not add a second copy of it.
    fun retry() {
        val text = retryText
        if (text.isEmpty() || agentTyping) return
        launchReply(text, recordUser = false)
    }

    // A new thread opened with a first message sends it exactly once: only
    // while the retained store still holds no message for this chat id.
    LaunchedEffect(chatId) {
        val first = initialMessage?.trim().orEmpty()
        transcriptStore.ensure(chatId, initialSeed.map { it.toSaved() })
        if (first.isNotEmpty() && transcriptStore.threads.value[chatId].isNullOrEmpty()) {
            draftState.edit { replace(0, length, first) }
            send()
        } else if (TokenStore.token != null && !isOfficialAgent && !chatId.startsWith("chat_")) {
            // Load persistent conversation history from GET /api/conversations/:id/messages
            scope.launch(Dispatchers.IO) {
                try {
                    val saved = RemoteConversationsDataSource().fetchMessages(chatId)
                    if (saved.isNotEmpty()) {
                        transcriptStore.ensure(chatId, saved)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Stop: cancel the pending reply and drop the typing indicator. The user's
    // message stays in the thread; Agent simply does not answer it.
    fun stop() {
        replyJob?.cancel()
        replyJob = null
        agentTyping = false
    }

    // Teamily canvas behind the thread. The top bar uses the same canvas colour
    // so messages scroll behind an opaque header instead of through the title.
    val palette = teamilyPalette()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .teamilyCanvas(palette),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (onOpenAgentProfile != null) {
                // Every agent's thread: back · avatar + badge · name · Invite.
                // The band is opaque so the list cannot show through the pill.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.topbarBg),
                ) {
                    AgentChatHeaderPill(
                        agentId = chatId,
                        name = chatTitle,
                        onBack = onBack,
                        onOpenProfile = onOpenAgentProfile,
                        onInvite = onInvite,
                    )
                }
            } else TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // The official agent carries its SignalBotBadge in front of the title.
                        if (isOfficialAgent) {
                            SignalBotBadge(32.dp)
                            Spacer(Modifier.width(spacing.sm))
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = chatTitle,
                                    style = DuaGenAITextStyles.rowTitle,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                if (isOfficialAgent) {
                                    Spacer(Modifier.width(4.dp))
                                    VerifiedCheck()
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.logoMint),
                                )
                                Spacer(Modifier.width(spacing.xs))
                                Text(
                                    text = "Agent · always on",
                                    style = DuaGenAITextStyles.meta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    when {
                        // Official agent: its badge is already in the title.
                        isOfficialAgent -> Unit
                        else -> {
                            PandaMark(
                                size = 32.dp,
                                contentDescription = "$chatTitle avatar",
                                modifier = Modifier.padding(end = spacing.gutter),
                            )
                        }
                    }
                },
                expandedHeight = 80.dp,
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.topbarBg,
                    scrolledContainerColor = palette.topbarBg,
                ),
            )
        },
        // The floating adaptive Gemini-style chat bar with bottom ambient glow
        bottomBar = {
            val isDark = DuaGenAITheme.isDark
            val glowAlpha = if (isDark) 0.18f else 0.42f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = glowAlpha),
                            ),
                        ),
                    )
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.union(WindowInsets.ime),
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                ChatBar(
                    state = draftState,
                    hasMessages = messages.isNotEmpty(),
                    onSend = { send() },
                    onAttachClick = { sheet = ComposerSheet.Plus },
                    onDictateClick = handleDictate,
                    onVoiceMessageClick = onOpenVoiceLive,
                    sending = agentTyping,
                    onStop = { stop() },
                    contextChips = contextChips,
                    onDismissChip = { chip -> contextChips = contextChips - chip },
                    modifier = Modifier.padding(
                        top = 4.dp,
                        bottom = 16.dp,
                    ),
                )
            }
        },
    ) { inner ->
        // Padding is outside the list, then the list is clipped to that box.
        // Avatars and bubbles therefore stop at the header and cannot draw
        // behind the floating more button.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(inner)
                .clipToBounds()
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = spacing.gutter,
                vertical = spacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (messages.isEmpty() && !agentTyping) {
                // An honest empty thread: no scripted opener, just a hint that
                // points at the composer. Centered in the main area.
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(horizontal = spacing.lg, vertical = spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.chat_empty_title),
                            style = DuaGenAITextStyles.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(spacing.xs))
                        Text(
                            text = stringResource(R.string.chat_empty_body),
                            style = DuaGenAITextStyles.snippet,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(24.dp))
                        ChatStarterChips(
                            onPick = { prompt ->
                                if (!agentTyping) {
                                    draftState.edit { replace(0, length, prompt) }
                                    send()
                                }
                            },
                        )
                    }
                }
            }
            itemsIndexed(messages) { index, msg ->
                val isLatest = index == messages.lastIndex
                when (msg.who) {
                    SavedMessage.AGENT -> AgentBubble(msg, chatTitle, isLatest = isLatest)
                    else -> UserBubble(msg, isLatest = isLatest)
                }
            }
            if (showError) {
                item {
                    ChatErrorBubble(
                        message = errorText,
                        chatTitle = chatTitle,
                        onRetry = if (retryText.isNotEmpty()) {
                            { retry() }
                        } else {
                            null
                        },
                    )
                }
            }
            if (agentTyping) {
                item { TypingBubble(chatTitle) }
            }
        }
    }

    // The composer's sheets. `+` opens Quick Actions, which hands off to the
    // Skills / MCP sheets rather than duplicating them.
    val automatePrompts = mapOf(
        "meeting_reminder" to stringResource(R.string.quick_actions_prompt_meeting_reminder),
        "daily_brief" to stringResource(R.string.quick_actions_prompt_daily_brief),
        "expert_research" to stringResource(R.string.quick_actions_prompt_expert_research),
        "drive_goal" to stringResource(R.string.quick_actions_prompt_drive_goal),
    )
    val contextLabels = mapOf(
        "memory" to stringResource(R.string.quick_actions_memory),
        "recall" to stringResource(R.string.quick_actions_recall),
    )
    when (sheet) {
        ComposerSheet.Plus -> ComposerPlusSheet(
            onOpenSkills = { sheet = ComposerSheet.Skills },
            onOpenMcp = { sheet = ComposerSheet.Mcp },
            onDismiss = { sheet = ComposerSheet.None },
            onOpenCreate = { key ->
                sheet = ComposerSheet.None
                onOpenCreate?.invoke(key)
            },
            onUpload = { sheet = ComposerSheet.None; showComingSoon() },
            onPhotos = { sheet = ComposerSheet.None; showComingSoon() },
            onCamera = { sheet = ComposerSheet.None; showComingSoon() },
            onVideos = { sheet = ComposerSheet.None; showComingSoon() },
            onVoiceCall = { sheet = ComposerSheet.None; showComingSoon() },
            onExploreMore = { sheet = ComposerSheet.None; showComingSoon() },
            onAllAutomations = { sheet = ComposerSheet.None; showComingSoon() },
            onAutomate = { key ->
                sheet = ComposerSheet.None
                val prompt = automatePrompts[key]
                if (prompt != null) {
                    draftState.edit { replace(0, length, prompt) }
                } else {
                    showComingSoon()
                }
            },
            onContext = { key ->
                sheet = ComposerSheet.None
                val chip = contextLabels[key]
                if (chip != null) {
                    if (chip !in contextChips) contextChips = contextChips + chip
                } else {
                    showComingSoon()
                }
            },
        )
        ComposerSheet.Skills -> ComposerSkillsSheet(
            enabledSkillIds = skills,
            onToggleSkill = toggleSkill,
            onDismiss = { sheet = ComposerSheet.None },
            skills = dynamicSkills ?: remoteSkills,
        )
        ComposerSheet.Mcp -> ComposerMcpSheet(
            mcpEnabled = mcp,
            onToggleMcp = toggleMcp,
            onDismiss = { sheet = ComposerSheet.None },
        )
        ComposerSheet.None -> Unit
    }
}

/**
 * The typing indicator — expressive floating pill with M3 1.5 wavy progress indicator
 * and a subtle thinking status label.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TypingBubble(chatTitle: String) {
    val spacing = LocalDuaGenAISpacing.current
    Row(verticalAlignment = Alignment.Bottom) {
        PandaMark(
            size = 28.dp,
            contentDescription = "$chatTitle bubble",
        )
        Spacer(Modifier.width(spacing.sm))
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.semantics {
                contentDescription = "Agent is typing"
                liveRegion = LiveRegionMode.Polite
            },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .width(44.dp)
                        .height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Text(
                    text = stringResource(R.string.chat_thinking),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A failed live turn: a soft error bubble and, when the prompt can be sent
 * again, one Retry control. The hit area stays at least 48dp.
 */
@Composable
private fun ChatErrorBubble(
    message: String,
    chatTitle: String,
    onRetry: (() -> Unit)?,
) {
    val spacing = LocalDuaGenAISpacing.current
    Row(verticalAlignment = Alignment.Bottom) {
        PandaMark(
            size = 28.dp,
            contentDescription = "$chatTitle bubble",
        )
        Spacer(Modifier.width(spacing.sm))
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 8.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                if (onRetry != null) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .minTouchTarget()
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.chat_retry),
                            style = DuaGenAITextStyles.button,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentBubble(msg: SavedMessage, chatTitle: String, isLatest: Boolean = false) {
    val spacing = LocalDuaGenAISpacing.current
    val effectiveTitle = msg.agentName?.takeIf { it.isNotBlank() } ?: chatTitle
    Row(verticalAlignment = Alignment.Bottom) {
        PandaMark(
            size = 28.dp,
            contentDescription = "$effectiveTitle bubble",
        )
        Spacer(Modifier.width(spacing.sm))
        Column {
            Surface(
                shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .then(
                        if (isLatest) Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        else Modifier,
                    ),
            ) {
                Text(
                    text = formatChatMarkdown(msg.text),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None,
                        ),
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                val metaText = if (!msg.agentName.isNullOrBlank()) {
                    "${msg.agentName} \u00b7 ${msg.time}"
                } else {
                    msg.time
                }
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // The offline canned reply is not a real answer: say so.
                if (msg.text == AGENT_REPLY) {
                    Spacer(Modifier.width(spacing.xs))
                    DemoBadge()
                }
            }
        }
    }
}

@Composable
private fun UserBubble(msg: SavedMessage, isLatest: Boolean = false) {
    val palette = teamilyPalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 24.dp),
                color = palette.bubbleUser,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .then(
                        if (isLatest) Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        else Modifier,
                    ),
            ) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None,
                        ),
                    ),
                    color = palette.bubbleUserText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = msg.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
    }
}

/**
 * Three airy floating starter suggestion chips for empty chat threads.
 */
@Composable
private fun ChatStarterChips(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val starters = listOf(
        Triple(
            stringResource(R.string.chat_starter_plan_day),
            Icons.Outlined.CalendarToday,
            stringResource(R.string.chat_starter_plan_day),
        ),
        Triple(
            stringResource(R.string.chat_starter_draft_reply),
            Icons.Outlined.EditNote,
            stringResource(R.string.chat_starter_draft_reply),
        ),
        Triple(
            stringResource(R.string.chat_starter_brainstorm),
            Icons.Outlined.Lightbulb,
            stringResource(R.string.chat_starter_brainstorm),
        ),
    )
    val isDark = DuaGenAITheme.isDark
    val chipColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val chipBorder = if (isDark) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        starters.forEach { (text, icon, _) ->
            Surface(
                onClick = { onPick(text) },
                shape = RoundedCornerShape(20.dp),
                color = chipColor,
                border = chipBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(name = "Chat — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatLight() = DuaGenAITheme(darkTheme = false) {
    ChatScreen(chatId = SAMPLE_CHAT_ID, chatTitle = "Webpage Developer", seed = sampleThread)
}

@Preview(name = "Chat — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatDark() = DuaGenAITheme(darkTheme = true) {
    ChatScreen(chatId = SAMPLE_CHAT_ID, chatTitle = "Webpage Developer", seed = sampleThread)
}

@Preview(name = "Agent chat — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AgentChatLight() = DuaGenAITheme(darkTheme = false) {
    ChatScreen(chatId = AGENT_CHAT_ID, chatTitle = AGENT_CHAT_TITLE, seed = agentThread)
}

@Preview(name = "Agent chat — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AgentChatDark() = DuaGenAITheme(darkTheme = true) {
    ChatScreen(chatId = AGENT_CHAT_ID, chatTitle = AGENT_CHAT_TITLE, seed = agentThread)
}

@Preview(name = "Chat Empty State — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatEmptyStateLightPreview() = DuaGenAITheme(darkTheme = false) {
    ChatScreen(chatId = SAMPLE_CHAT_ID, chatTitle = "Webpage Developer", seed = emptyList())
}

@Preview(name = "Chat Empty State — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatEmptyStateDarkPreview() = DuaGenAITheme(darkTheme = true) {
    ChatScreen(chatId = SAMPLE_CHAT_ID, chatTitle = "Webpage Developer", seed = emptyList())
}
