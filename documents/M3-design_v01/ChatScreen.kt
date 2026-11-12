package ai.duagenai.design.screens

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ai.duagenai.design.cube.CubeAvatar
import ai.duagenai.design.cube.CubeCatalog
import ai.duagenai.design.cube.CubeIds
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme

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
 *  COMPOSER — now LIVE
 *  -------------------
 *  The thread's composer is NOT a new component: it is `DuaComposer` from
 *  HomeScreen.kt — the exact same pill Dua's home draws (plus · placeholder ·
 *  mic · green voice button), same tokens, same radius, same spacing. Dua's call
 *  site passes no value/change/send, so its rendering is unchanged; this screen
 *  passes all three, which is the single switch that makes the pill editable.
 *
 *  The composer sits in `Scaffold`'s `bottomBar`, so the `inner` padding the
 *  LazyColumn consumes already includes its height — the last message can never
 *  hide underneath it, and the IME inset is handled by the Scaffold too.
 *
 *  PER-THREAD STATE
 *  ----------------
 *  `messages` and `draft` are `rememberSaveable` KEYED ON `chatId`. The key is
 *  what makes the state per-thread: a message sent in Orhan's thread is stored
 *  under Orhan's key, so it can never appear in another conversation, and a
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
 * Orhan's OWN conversation id.
 *
 * Orhan is an agent, so it has a thread of its own — the pinned card on Home
 * opens it through the SAME `chat/{chatId}` route every other conversation uses.
 * No new route was invented for it.
 *
 * It is a stable id, so `CubeCatalog.forEntity` gives Orhan's thread a cube the
 * same way it gives every other thread one. It is deliberately NOT one of
 * `sampleChats`' ids: those are the conversations Orhan MANAGES, and Orhan's own
 * thread is a different thing.
 */
const val ORHAN_CHAT_ID = "chat_orhan"

/** The title Orhan's thread shows in its top bar. */
const val ORHAN_CHAT_TITLE = "Orhan"

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
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(who)
        dest.writeString(text)
        dest.writeString(time)
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
 * Orhan's canned reply.
 *
 * Deliberately static: this kit has no model behind it, and the brief asked for
 * a simple reply rather than a real call. It is written to Orhan's role — it
 * reports on the agents it controls — so the thread stays in character.
 */
private const val ORHAN_REPLY =
    "Got it — I've picked that up and I'm on it. I'll report back here as soon as it's done."

/** How long the typing indicator shows before Orhan's reply lands. */
private const val ORHAN_REPLY_DELAY_MS = 900L

/** The clock label a freshly sent message carries, e.g. "9:41 AM". */
private fun nowLabel(): String =
    java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date())

/**
 * Orhan's own thread — the conversation the pinned card opens.
 *
 * It is written to Orhan's role from the confirmed product model: Orhan
 * multitask, controls other agents and can create new ones. The user asks for
 * work, Orhan reports what it did across the agents it manages.
 */
private val orhanThread = listOf(
    Message.User(
        "I need the Q3 launch handled — deck, competitor research and the invoice reconciliation.",
        "9:31 AM",
    ),
    Message.Agent(
        "On it. I've split that across three agents: Slides Assistant is building the deck, Market Research is pulling cited competitor data, and Accounting Assistant is reconciling the invoices.",
        "9:31 AM",
    ),
    Message.User("Can you also spin up an agent for the launch email sequence?", "9:33 AM"),
    Message.Agent(
        "Done — I created a new agent for it and handed it the brief. All four are running now; I'll report back as each one finishes.",
        "9:34 AM",
    ),
)

private val sampleThread = listOf(
    Message.User(
        "Draft a landing page for our AI meetup in Palo Alto — dark hero, teal accent, one CTA.",
        "9:38 AM",
    ),
    Message.Agent(
        "On it. I'll use the Dua Gen AI green (#087C45) so it stays on-brand, keep one primary CTA, and make it mobile-first.",
        "9:38 AM",
    ),
    Message.User("Looks right. Make the hero headline bigger and add the speaker line-up.", "9:40 AM"),
    Message.Agent(
        "Done — headline is now 56/60 with the mint accent, and the line-up is a 3-up card row that stacks on small screens. Draft is ready to publish.",
        "9:41 AM",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    // Required, not defaulted: the caller must say WHICH conversation this is,
    // because the cube is derived from it. A default here would silently show
    // one chat's cube inside another chat's thread.
    chatId: String,
    chatTitle: String = "Webpage Developer",
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Orhan's own thread is the one conversation whose content is not the
    // generic sample: it is Orhan reporting on the agents it controls. Every
    // other id keeps the sample thread, so nothing else changes.
    val seed = if (chatId == ORHAN_CHAT_ID) orhanThread else sampleThread
    // Derived, never accepted as a parameter: the conversation's cube is a pure
    // function of its stable id, so this screen cannot drift from the Home row
    // that opened it — and it is not the user's saved cube.
    val cubeId = CubeCatalog.forEntity(chatId)
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    // PER-THREAD message state. `rememberSaveable` keyed on `chatId` means the
    // list survives rotation and back-navigation, and that a message sent in one
    // thread can never appear in another: the key changes with the thread, so
    // the saved list is a different one. The seed is the thread's own history.
    var messages by rememberSaveable(chatId) {
        mutableStateOf(ArrayList(seed.map { it.toSaved() }))
    }
    // The composer's draft, saved the same way so a half-typed line survives a
    // rotation. Also keyed on `chatId`, so drafts do not leak between threads.
    var draft by rememberSaveable(chatId) { mutableStateOf("") }
    // Orhan's reply is shown after a short beat, with the typing indicator up in
    // the meantime. It is a plain coroutine delay — no real model call.
    var orhanTyping by rememberSaveable(chatId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // The list is the conversation plus, while Orhan is "typing", one extra row.
    val rows = messages.size + if (orhanTyping) 1 else 0

    // Keep the newest message in view. Keyed on the row count so it fires when a
    // message is added (and when the typing row appears), not on every frame.
    LaunchedEffect(rows) {
        if (rows > 0) listState.animateScrollToItem(rows - 1)
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty()) return
        messages = ArrayList(messages + SavedMessage(SavedMessage.USER, text, nowLabel()))
        draft = ""
        orhanTyping = true
        scope.launch {
            delay(ORHAN_REPLY_DELAY_MS)
            orhanTyping = false
            messages = ArrayList(
                messages + SavedMessage(SavedMessage.AGENT, ORHAN_REPLY, nowLabel()),
            )
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = chatTitle,
                            style = DuaGenAITextStyles.rowTitle,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
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
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 32dp header avatar — this conversation's cube, untinted.
                    CubeAvatar(
                        cubeId = cubeId,
                        size = 32.dp,
                        shape = DuaGenAITheme.shapes.avatarMessage,
                        contentDescription = "$chatTitle avatar, ${CubeIds.label(cubeId)}",
                        modifier = Modifier.padding(end = spacing.gutter),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        // The composer is the SAME pill Dua's home draws — one implementation,
        // two call sites. Only the placeholder is thread-specific; the value /
        // change / send parameters are what make it live.
        bottomBar = {
            DuaComposer(
                placeholder = "Message $chatTitle…",
                value = draft,
                onValueChange = { draft = it },
                onSend = { send() },
            )
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
            items(messages) { msg ->
                when (msg.who) {
                    SavedMessage.AGENT -> AgentBubble(msg, cubeId, chatTitle)
                    else -> UserBubble(msg, cubeId, chatTitle)
                }
            }
            if (orhanTyping) {
                item { TypingBubble(cubeId, chatTitle) }
            }
        }
    }
}

/**
 * The typing indicator — the same agent bubble, with three dots instead of text.
 *
 * It reuses [AgentBubble]'s own geometry (28dp cube, same gap, same bubble fill
 * and radius) so the "Orhan is replying" state is the agent bubble mid-sentence
 * rather than a new component.
 */
@Composable
private fun TypingBubble(cubeId: String, chatTitle: String) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        CubeAvatar(
            cubeId = cubeId,
            size = 28.dp,
            shape = DuaGenAITheme.shapes.avatarMessage,
            contentDescription = "$chatTitle, ${CubeIds.label(cubeId)}",
        )
        Spacer(Modifier.width(spacing.sm))
        Box(
            modifier = Modifier
                .clip(DuaGenAITheme.shapes.agentBubble)
                .background(colors.agentBubble)
                .padding(horizontal = 14.dp, vertical = 11.dp)
                .semantics { contentDescription = "Orhan is typing" },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.agentBubbleInk.copy(alpha = 0.45f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentBubble(msg: SavedMessage, cubeId: String, chatTitle: String) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        // Same conversation cube as the header and the user's bubbles — the old
        // "W" letter box was a second avatar implementation living on this screen.
        CubeAvatar(
            cubeId = cubeId,
            size = 28.dp,
            shape = DuaGenAITheme.shapes.avatarMessage,
            contentDescription = "$chatTitle, ${CubeIds.label(cubeId)}",
        )
        Spacer(Modifier.width(spacing.sm))
        Column {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(DuaGenAITheme.shapes.agentBubble)
                    .background(colors.agentBubble)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Text(
                    text = msg.text,
                    style = DuaGenAITextStyles.bubble,
                    color = colors.agentBubbleInk,
                )
            }
            Spacer(Modifier.height(spacing.xxs))
            Text(
                text = msg.time,
                style = DuaGenAITextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UserBubble(msg: SavedMessage, cubeId: String, chatTitle: String) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(DuaGenAITheme.shapes.userBubble)
                    .background(colors.userBubble)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Text(
                    text = msg.text,
                    style = DuaGenAITextStyles.bubble,
                    color = colors.userBubbleInk,
                )
            }
            Spacer(Modifier.height(spacing.xxs))
            Text(
                text = msg.time,
                style = DuaGenAITextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(spacing.sm))
        // 32dp avatar beside the user's own message — this conversation's cube,
        // not the user's saved one (which stays on Home's top bar and Profile).
        CubeAvatar(
            cubeId = cubeId,
            size = 32.dp,
            shape = DuaGenAITheme.shapes.avatarMessage,
            contentDescription = "$chatTitle, ${CubeIds.label(cubeId)}",
        )
    }
}

@Preview(name = "Chat — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatLight() = DuaGenAITheme(darkTheme = false) {
    ChatScreen(chatId = SAMPLE_CHAT_ID, chatTitle = "Webpage Developer")
}

@Preview(name = "Chat — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatDark() = DuaGenAITheme(darkTheme = true) {
    ChatScreen(chatId = SAMPLE_CHAT_ID, chatTitle = "Webpage Developer")
}

@Preview(name = "Orhan chat — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrhanChatLight() = DuaGenAITheme(darkTheme = false) {
    ChatScreen(chatId = ORHAN_CHAT_ID, chatTitle = ORHAN_CHAT_TITLE)
}

@Preview(name = "Orhan chat — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrhanChatDark() = DuaGenAITheme(darkTheme = true) {
    ChatScreen(chatId = ORHAN_CHAT_ID, chatTitle = ORHAN_CHAT_TITLE)
}
