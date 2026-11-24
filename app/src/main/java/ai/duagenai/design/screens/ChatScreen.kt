package ai.duagenai.design.screens

import ai.duagenai.design.theme.DemoBadge
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lightbulb
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ai.duagenai.design.R
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ai.duagenai.design.bot.BabyGrokMark
import ai.duagenai.design.bot.LocalBotAppearance
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
 *  COMPOSER — the 2-row Teamily chat bar
 *  -------------------------------------
 *  The thread's composer is [ChatBar] (ChatBar.kt):
 *  row 1 the field, row 2 the toolbar. `+` opens ComposerPlusSheet, which hands
 *  off to ComposerSkillsSheet / ComposerMcpSheet; the mode pill's menu also
 *  lists Skills and MCP Tools. While Agent's reply is pending (`agentTyping`)
 *  the trailing control is Stop, which cancels the reply. Meeting Minutes,
 *  Dictate, Emoji, Voice message and Voice call have no backend yet, so a tap
 *  shows the "Jaldi aa raha hai" snackbar (the same answer Dua home gives).
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
 * The Personal Agent's conversation id.
 *
 * Agent home's hero card opens this thread through the same `chat/{chatId}`
 * route as every other conversation. It is a normal thread: a signed-in send
 * posts `mode: "auto"` and `stream: false` to the Node API. It is deliberately
 * not [AGENT_CHAT_ID].
 */
const val PERSONAL_AGENT_CHAT_ID = "chat_personal"

/**
 * Dua Agent's conversation id.
 *
 * The pinned "Dua Agent" row on Agent home opens this thread. A signed-in send
 * posts `mode: "agent"` and `stream: true` to the engine, with the enabled
 * skill ids. It is a stable id, so `CubeCatalog.forEntity` gives the thread a
 * cube the same way it gives every other thread one.
 */
const val AGENT_CHAT_ID = "chat_agent"

/** The title Agent's thread shows in its top bar. */
const val AGENT_CHAT_TITLE = "Agent"

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
    val agentHeader = onOpenAgentProfile != null
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
                    remoteSkills = RemoteSkillsDataSource().fetch(if (chatId == AGENT_CHAT_ID) "dua-agent" else null)
                } catch (_: Exception) {}
            }
        }
    }

    // Composer sheets (+ / Skills / MCP) and the display-only mode pill.
    var sheet by rememberSaveable(chatId) { mutableStateOf(ComposerSheet.None) }
    var chatMode by rememberSaveable(chatId) {
        mutableStateOf(if (chatId == AGENT_CHAT_ID) "Agent Engine" else "Auto")
    }
    var contextChips by rememberSaveable(chatId) { mutableStateOf(listOf<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val comingSoon = stringResource(R.string.composer_coming_soon)
    val showComingSoon: () -> Unit = {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(comingSoon)
        }
    }
    val isPersonal = chatId == PERSONAL_AGENT_CHAT_ID
    val isDuaAgent = chatId == AGENT_CHAT_ID

    // The list is the conversation plus, while Agent is "typing", one extra row
    // (and, on the Personal Agent thread, the quick-action chips row).
    val rows = messages.size + (if (agentTyping) 1 else 0) + (if (isPersonal) 1 else 0)

    // Keep the newest message in view. Keyed on the row count so it fires when a
    // message is added (and when the typing row appears), not on every frame.
    LaunchedEffect(rows) {
        if (rows > 0) listState.animateScrollToItem(rows - 1)
    }

    fun send() {
        val text = draftState.text.toString().trim()
        // Guarded twice over: blank text, or a reply still in flight.
        if (text.isEmpty() || agentTyping) return
        // The store is the single writer: the screen never holds a mutable copy
        // of the transcript, so a message cannot be lost by a stale local list.
        transcriptStore.append(chatId, SavedMessage(SavedMessage.USER, text, nowLabel()))
        draftState.edit { replace(0, length, "") }
        contextChips = emptyList()
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
                        transcriptStore.append(chatId, SavedMessage(SavedMessage.AGENT, e.userMessage, nowLabel()))
                    }
                } catch (e: Exception) {
                    if (!streamedAny) {
                        val msg = e.message?.takeIf { it.isNotBlank() } ?: "Chat request: no connection."
                        transcriptStore.append(chatId, SavedMessage(SavedMessage.AGENT, msg, nowLabel()))
                    }
                } finally {
                    agentTyping = false
                }
            }
        }
    }

    // A new thread opened with a first message sends it exactly once: only
    // while the retained store still holds no message for this chat id.
    LaunchedEffect(chatId) {
        val first = initialMessage?.trim().orEmpty()
        transcriptStore.ensure(chatId, initialSeed.map { it.toSaved() })
        if (first.isNotEmpty() && transcriptStore.threads.value[chatId].isNullOrEmpty()) {
            draftState.edit { replace(0, length, first) }
            send()
        } else if (TokenStore.token != null && !isPersonal && !isDuaAgent && !chatId.startsWith("chat_")) {
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

    // Teamily canvas behind the whole thread (the zip `--nv-atmosphere`, light
    // and dark); the Scaffold and top bar are transparent so it shows through.
    Scaffold(
        modifier = modifier.teamilyCanvas(teamilyPalette()),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (onOpenAgentProfile != null) {
                // Every agent's thread: back · avatar + badge · name · Invite.
                AgentChatHeaderPill(
                    agentId = chatId,
                    name = chatTitle,
                    onBack = onBack,
                    onOpenProfile = onOpenAgentProfile,
                    onInvite = onInvite,
                )
            } else TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // The two pinned agents carry their own badge in front of
                        // the title (Personal: BotBadge, Dua Agent: SignalBotBadge).
                        if (isPersonal) {
                            BotBadge(32.dp)
                            Spacer(Modifier.width(spacing.sm))
                        } else if (isDuaAgent) {
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
                                if (isDuaAgent) {
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
                        // Personal Agent: its badge is already in the title.
                        isPersonal || isDuaAgent -> Unit
                        else -> {
                            val look = LocalBotAppearance.current
                            BabyGrokMark(
                                shape = look.shape,
                                color = look.color,
                                size = 32.dp,
                                contentDescription = "$chatTitle avatar, ${look.shape.label}",
                                modifier = Modifier.padding(end = spacing.gutter),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        // The floating adaptive Gemini-style chat bar with bottom ambient glow
        bottomBar = {
            val isDark = isSystemInDarkTheme()
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
                    // Agent threads read "Type a message..." (reference);
                    // other threads name the conversation.
                    placeholder = if (agentHeader) {
                        stringResource(R.string.chat_agent_placeholder)
                    } else {
                        "Message $chatTitle…"
                    },
                    selectedMode = chatMode,
                    onModeChange = { chatMode = it },
                    onAttachClick = { sheet = ComposerSheet.Plus },
                    onMeetingMinutesClick = showComingSoon,
                    onDictateClick = showComingSoon,
                    onEmojiClick = showComingSoon,
                    onVoiceMessageClick = showComingSoon,
                    onVoiceCallClick = showComingSoon,
                    sending = agentTyping,
                    onStop = { stop() },
                    onOpenSkills = { sheet = ComposerSheet.Skills },
                    onOpenMcp = { sheet = ComposerSheet.Mcp },
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(
                horizontal = spacing.gutter,
                vertical = spacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (messages.isEmpty() && !agentTyping) {
                // An honest empty thread: no scripted opener, just a hint that
                // points at the composer. Centered in the pain area. The
                // Personal Agent's quick-action chips live here too, so they
                // stay on screen instead of being pushed out by this filler.
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(horizontal = spacing.lg, vertical = spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "No messages yet",
                            style = DuaGenAITextStyles.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(spacing.xs))
                        Text(
                            text = "Say something below to start the conversation.",
                            style = DuaGenAITextStyles.snippet,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isPersonal) {
                            Spacer(Modifier.height(spacing.lg))
                            QuickActionChips(
                                label = "Try asking:",
                                actions = PERSONAL_AGENT_QUICK_ACTIONS,
                                onPick = { prompt ->
                                    if (!agentTyping) {
                                        draftState.edit { replace(0, length, prompt) }
                                        send()
                                    }
                                },
                            )
                        } else {
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
            }
            items(messages) { msg ->
                when (msg.who) {
                    SavedMessage.AGENT -> AgentBubble(msg, chatTitle)
                    else -> UserBubble(msg, chatTitle)
                }
            }
            if (agentTyping) {
                item { TypingBubble(chatTitle) }
            }
            // Personal Agent, conversation underway: the same quick-action
            // chips as its demo screen, under the messages. A chip sends its
            // text like a typed message. (In an EMPTY thread the chips render
            // inside the empty state above — this item would sit off screen.)
            if (isPersonal && messages.isNotEmpty()) {
                item {
                    QuickActionChips(
                        label = "Try asking:",
                        actions = PERSONAL_AGENT_QUICK_ACTIONS,
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
    }

    // The composer's sheets. `+` opens Quick Actions, which hands off to the
    // Skills / MCP sheets rather than duplicating them.
    when (sheet) {
        ComposerSheet.Plus -> ComposerPlusSheet(
            onOpenSkills = { sheet = ComposerSheet.Skills },
            onOpenMcp = { sheet = ComposerSheet.Mcp },
            onDismiss = { sheet = ComposerSheet.None },
            onOpenCreate = { key ->
                sheet = ComposerSheet.None
                onOpenCreate?.invoke(key)
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
    val look = LocalBotAppearance.current
    Row(verticalAlignment = Alignment.Bottom) {
        BabyGrokMark(
            shape = look.shape,
            color = look.color,
            size = 28.dp,
            contentDescription = "$chatTitle, ${look.shape.label}",
        )
        Spacer(Modifier.width(spacing.sm))
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.semantics { contentDescription = "Agent is typing" },
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

@Composable
private fun AgentBubble(msg: SavedMessage, chatTitle: String) {
    val spacing = LocalDuaGenAISpacing.current
    val effectiveTitle = msg.agentName?.takeIf { it.isNotBlank() } ?: chatTitle
    val look = LocalBotAppearance.current

    Row(verticalAlignment = Alignment.Bottom) {
        BabyGrokMark(
            shape = look.shape,
            color = look.color,
            size = 28.dp,
            contentDescription = "$effectiveTitle, ${look.shape.label}",
        )
        Spacer(Modifier.width(spacing.sm))
        Column {
            Surface(
                shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.widthIn(max = 300.dp),
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
private fun UserBubble(msg: SavedMessage, chatTitle: String) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()
    val look = LocalBotAppearance.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 24.dp),
                color = palette.bubbleUser,
                modifier = Modifier.widthIn(max = 300.dp),
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
        Spacer(Modifier.width(spacing.sm))
        BabyGrokMark(
            shape = look.shape,
            color = look.color,
            size = 32.dp,
            contentDescription = "$chatTitle, ${look.shape.label}",
        )
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
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
