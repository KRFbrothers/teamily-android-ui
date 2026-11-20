/*
 * `Enum.entries` is still an experimental stdlib API in Kotlin 1.9.24, and this
 * file reads `HomeMode.entries` / `HomeTab.entries` to build the mode switch and
 * the dock. Opting in here is the narrow, honest fix: the API is stable in
 * practice and the alternative (`values()`) allocates a fresh array on every
 * recomposition.
 *
 * A `@file:` annotation must precede the `package` declaration.
 */
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package ai.duagenai.design.screens

import ai.duagenai.design.bot.BabyGrokMark
import ai.duagenai.design.bot.LocalBotAppearance
import ai.duagenai.design.cube.CubeAvatar
import androidx.compose.foundation.shape.CircleShape
import ai.duagenai.design.theme.DemoBadge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ai.duagenai.design.R
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import ai.duagenai.design.cube.CubeCatalog
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyShapes
import ai.duagenai.design.theme.TeamilyStroke
import ai.duagenai.design.theme.teamilyCanvas
import ai.duagenai.design.theme.teamilyPalette
import ai.duagenai.design.theme.teamilyPressScale
import ai.duagenai.design.theme.teamilyPrimaryGlow
import ai.duagenai.design.theme.teamilyRestingShadow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxHeight
import ai.duagenai.design.theme.MaxContentWidth

/* ============================================================================
 *  Home — the two modes, per the confirmed product model
 *
 *  Dua Gen AI is one package: Dua AI, Dua Agent, and Dua Workspace.
 *  The Home UI has two modes:
 *
 *    DUA   = a chatbot. Simple chat, Q&A, no agent work.
 *    AGENT = multitasks, controls other agents, can create new agents.
 *
 *  That model decides what each home surface may contain, and the two
 *  references agree with it:
 *
 *    DUA MODE (reference 1) — deliberately PLAIN
 *      • top bar: circular white hamburger · Dua|Agent toggle · circular white gear
 *      • body: EMPTY. Dua is a chatbot, not an agent host, so there is no chat
 *        list, no search row, no "CHATS" header and no bottom navigation here.
 *        The reference's body is empty and so is this one — nothing is invented
 *        to fill it.
 *      • bottom: one collapsed "Ask Dua ..." pill (plus · placeholder · green
 *        voice button). The agent tools (Skills, MCP Tools, the `+` sheet) are
 *        drawn only under a thread, never here.
 *      • NO agent-related entry point of any kind. Not a row, not a badge, not
 *        a tab. Dua does not manage agents.
 *
 *    AGENT MODE (agent-home-reference.jpg — the Teamily chat-list home)
 *      • top bar: 48dp circular user avatar (opens the drawer) · green app
 *        badge + bold "Dua Gen AI" · 48dp circular plus (New chat). NO
 *        Dua | Agent toggle here: the drawer's "Dua" row switches back.
 *      • search row: 48dp circular filter button + full-width "Search" pill
 *      • ONE filled hero card: the user's personal agent (cardHeroColor);
 *        tapping it opens the Agent thread
 *      • canvas rows with no card boxes: "Pinned · 1" → Dua Agent ✓, a thin
 *        divider, then "Chats · 6"; both section headers collapse
 *      • bottom: floating 5-tab capsule dock (Chats · Agents · Discover ·
 *        Memory · Artifacts) drawn over the list, as in the reference.
 *
 *  Everything visual comes from the kit's own tokens — the green palette, Lato,
 *  the 20dp card radius, the 4dp spacing grid. The mockups' purple (#7C3AED) and
 *  blue (#2563EB) are deliberately absent: this is Dua Gen AI, not the other
 *  product.
 *
 *  CUBES — one rule, no exceptions:
 *      A conversation's cube is a pure function of its stable `chatId`
 *      (`CubeCatalog.forEntity`), so a row keeps its cube across restarts and
 *      reorderings, and it never changes when the user picks a new avatar. The
 *      USER's saved cube (CubeStore) is a different thing entirely and appears
 *      only in the drawer footer / Profile. Agent home's header avatar is the
 *      user's photo (a Painter supplied by the app), not a cube.
 *
 *  WHERE CHATS ARE OPENED FROM
 *      Dua's home is empty. Until a real conversation exists there is nothing
 *      to list anywhere: the drawer's history and Agent home's Chats section
 *      both render the honest empty state. When conversations do exist, the
 *      drawer hands a row's `chatId` and `name` to `onOpenChat`, the nav host
 *      turns them into `chat/{chatId}`, and ChatScreen derives its cube from
 *      the SAME id with the SAME pure function.
 * ==========================================================================*/

/**
 * The two assistant modes the toggle offers.
 *
 * Persisted by the host (rememberSaveable), not by this screen, so a
 * configuration change does not silently reset the user's choice.
 */
enum class HomeMode(val label: String) {
    Dua("Dua"),
    Agent("Agent"),
}

data class ChatRow(
    /** Stable identity of the conversation. Its cube is derived from this, so it
     *  must never be built from the display name or the list position. */
    val chatId: String,
    val name: String,
) {
    /** This chat's own cube — fixed for the lifetime of [chatId]. */
    val cubeId: String get() = CubeCatalog.forEntity(chatId)
}

/**
 * One conversation in a list (the drawer's history). The type stays while the
 * app ships no fake conversations: both the drawer and Agent home render the
 * honest empty state until a real chat exists to put in a row.
 */

/**
 * The bottom navigation destinations, in the reference's order.
 *
 * [Managed] keeps its identifier and its destination (the agents route) but
 * its visible label is "Agents", as in the reference dock.
 */
enum class HomeTab(val label: String) {
    Chats("Chats"),
    Managed("Agents"),
    Discover("Discover"),
    Memory("Memory"),
    Artifacts("Artifacts"),
}

@Composable
fun HomeScreen(
    /** Opens the sidebar drawer. The host owns the drawer state. */
    onOpenDrawer: () -> Unit = {},
    /**
     * Current assistant mode. Hoisted so it survives configuration changes.
     * The app launches into Dua home. Only Dua home draws the Dua | Agent
     * switch; from Agent home the drawer's "Dua" row switches back to Dua.
     */
    mode: HomeMode = HomeMode.Dua,
    onModeChange: (HomeMode) -> Unit = {},
    /** The top bar's right button in Dua mode (the gear). */
    onOpenSettings: () -> Unit = {},
    /** The top bar's right button in Agent mode (the plus) — starts a new chat. */
    onNewChat: () -> Unit = {},
    /** Bottom navigation. Only Chats / Managed / Discover have screens. */
    onSelectTab: (HomeTab) -> Unit = {},
    /**
     * Which bottom-nav item is highlighted. HOISTED, not local: the host owns
     * the selection so it survives a Home recomposition and a configuration
     * change, exactly like [mode]. Defaults to [HomeTab.Chats] — the tab Agent's
     * home has always shown as active.
     */
    selectedTab: HomeTab = HomeTab.Chats,
    /**
     * Opens the Personal Agent thread. The hero card is the entry point, and
     * the host routes [PERSONAL_AGENT_CHAT_ID] through the same `chat/{chatId}`
     * path every other thread uses.
     */
    onOpenPersonalAgentChat: () -> Unit = {},
    /**
     * Opens Dua Agent's thread. The pinned "Dua Agent" row is the entry point.
     * The host routes [AGENT_CHAT_ID] through the same chat path.
     */
    onOpenDuaAgentChat: () -> Unit = {},
    /**
     * Opens one of the chat rows. Each row already has a stable id; the
     * host uses the same chat route as the drawer history. No rows exist
     * yet — kept as the seam a real chat list will use.
     */
    @Suppress("UNUSED_PARAMETER")
    onOpenListedChat: (chatId: String, title: String) -> Unit = { _, _ -> },
    /**
     * Dua home's composer Send. Receives the trimmed first message; the host
     * opens a NEW Dua thread (fresh chat id) that sends it.
     */
    onSendDuaMessage: (String) -> Unit = {},
    /** Dua home's Attach `+` sheet: a Create tile was tapped (its key). */
    onOpenCreate: (String) -> Unit = {},
    /** Header "+" menu. Every entry must DO something real today. */
    onCreateAgent: () -> Unit = {},
    onNewGroup: () -> Unit = {},
    /**
     * Search-row filter. When [onFilterActiveChange] is set, the host owns the
     * flag (it also opens the public profile). Null keeps the local toggle.
     */
    filterActive: Boolean = false,
    onFilterActiveChange: ((Boolean) -> Unit)? = null,
    /** Public profile's top-right menu. */
    onPublicProfileSettings: () -> Unit = {},
    /**
     * Agent home's header photo. Null draws `dua_user_avatar` in the 48.dp
     * circle. A 10.dp `#42D982` online dot with a 2.dp `#1D2220` ring sits on
     * that circle's top-right rim. The green app badge is [AppBadge].
     */
    userAvatar: Painter? = null,
    hasUnreadMemory: Boolean = true,
    conversations: List<ConversationItem> = emptyList(),
    modifier: Modifier = Modifier,
) {
    // Local only when the host does not own the flag. On means the public
    // profile covers this home.
    var localFilter by rememberSaveable { mutableStateOf(false) }
    val filterOn = if (onFilterActiveChange != null) filterActive else localFilter
    val toggleFilter = {
        if (onFilterActiveChange != null) onFilterActiveChange(!filterActive)
        else localFilter = !localFilter
    }
    val closeProfile = {
        if (onFilterActiveChange != null) onFilterActiveChange(false)
        else localFilter = false
    }

    val palette = teamilyPalette()
    // Dua home's draft: one short string, so rememberSaveable (rotation-safe).
    var duaDraft by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.composer_coming_soon)

    Box(
        modifier = modifier
            .fillMaxSize()
            // Teamily canvas: the zip `--nv-atmosphere` glow at 8%/92%. Drawn
            // BEFORE the inset padding, so it also fills the transparent
            // status bar (true edge-to-edge, no separate band).
            .teamilyCanvas(palette)
            // Keeps the header below the status bar and the camera cutout. The
            // dock still applies its own navigation-bar inset, so the bottom
            // is not padded twice.
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = MaxContentWidth)
                .fillMaxHeight()
                .fillMaxWidth(),
        ) {

            if (mode == HomeMode.Dua) {
                /* ================= DUA MODE =================
                 * Top bar: hamburger · Dua|Agent toggle · gear. Dua is a
                 * chatbot: no chat list, no search row, no bottom navigation.
                 * The body is a centred welcome (bot badge, greeting and four
                 * quick starters); a starter sends exactly like the composer —
                 * a NEW Dua thread carrying that text. */
                HomeTopBar(
                    mode = mode,
                    onOpenDrawer = onOpenDrawer,
                    onModeChange = onModeChange,
                    onOpenSettings = onOpenSettings,
                    onNewChat = onNewChat,
                )
                DuaHomeWelcome(
                    onPick = { text -> onSendDuaMessage(text) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                Box(
                    Modifier.windowInsetsPadding(
                        WindowInsets.navigationBars.union(WindowInsets.ime),
                    ),
                ) {
                    Column {
                        // Attach / Voice taps explain themselves here.
                        SnackbarHost(snackbarHostState)
                        DuaComposer(
                            value = duaDraft,
                            onValueChange = { duaDraft = it },
                            onSend = {
                                val text = duaDraft.trim()
                                if (text.isNotEmpty()) {
                                    duaDraft = ""
                                    onSendDuaMessage(text)
                                }
                            },
                            onUnavailable = {
                                snackbarScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(comingSoon)
                                }
                            },
                            // + opens the Quick Actions sheet; Voice looks and
                            // reads enabled but still explains "coming soon".
                            attachEnabled = true,
                            voiceEnabled = true,
                            onOpenCreate = onOpenCreate,
                        )
                    }
                }
            } else {
                /* ================= AGENT MODE =================
                 * The reference's chat-list home: avatar · badge + "Dua Gen AI"
                 * · plus, then filter + Search, then the scrolling list (hero
                 * card, Pinned, Chats) with the floating dock drawn OVER it.
                 * No composer. */
                AgentHomeTopBar(
                    onOpenDrawer = onOpenDrawer,
                    onCreateAgent = onCreateAgent,
                    onNewGroup = onNewGroup,
                    userAvatar = userAvatar,
                )
                SearchRow(
                    filterActive = filterOn,
                    onToggleFilter = toggleFilter,
                    modifier = Modifier.padding(
                        start = LocalDuaGenAISpacing.current.gutter,
                        end = LocalDuaGenAISpacing.current.gutter,
                        top = 4.dp,
                        bottom = 8.dp,
                    ),
                )
                // Personal Agent is FIXED: remains pinned at top during scroll
                PersonalAgentHeroCard(
                    onClick = onOpenPersonalAgentChat,
                    modifier = Modifier.padding(
                        start = 9.dp,
                        end = 9.dp,
                        bottom = 6.dp,
                    ),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    AgentHomeList(
                        onOpenDuaAgentChat = onOpenDuaAgentChat,
                        conversations = conversations,
                        onOpenListedChat = onOpenListedChat,
                        modifier = Modifier.fillMaxSize(),
                    )
                    HomeBottomNav(
                        selected = selectedTab,
                        onSelect = onSelectTab,
                        hasUnreadMemory = hasUnreadMemory,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
        if (mode == HomeMode.Agent && filterOn) {
            androidx.activity.compose.BackHandler(onBack = closeProfile)
            PublicProfileScreen(
                onBack = closeProfile,
                onOpenSettings = onPublicProfileSettings,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The top bar.
 *
 * Three elements, vertically centred: a circular white button, the Dua|Agent
 * toggle, and a second circular white button. The two side buttons are 44dp
 * circles with a soft shadow — the reference's "floating chip" look — and the
 * toggle takes the remaining width between them.
 *
 * The right button changes with the mode, as the references show: a gear in Dua
 * mode, a plus in Agent mode. The hamburger's line count differs between the two
 * references (2 lines vs 3); that is a drawing detail of the same control, so one
 * icon serves both rather than two near-identical glyphs.
 */
@Composable
private fun HomeTopBar(
    mode: HomeMode,
    onOpenDrawer: () -> Unit,
    onModeChange: (HomeMode) -> Unit,
    onOpenSettings: () -> Unit,
    onNewChat: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.gutter,
                end = spacing.gutter,
                top = spacing.md,
                bottom = spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        CircleIconButton(
            onClick = onOpenDrawer,
            contentDescription = "Open menu",
        ) {
            Icon(
                Icons.Filled.Menu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }

        ModeToggle(
            mode = mode,
            onModeChange = onModeChange,
            modifier = Modifier.weight(1f),
        )

        // Agent mode's "+" (New chat) is the polish's header pill: plusFill
        // with a plusIcon glyph. Dua mode's gear keeps the neutral circle.
        val isPlus = mode == HomeMode.Agent
        CircleIconButton(
            onClick = if (mode == HomeMode.Dua) onOpenSettings else onNewChat,
            contentDescription = if (mode == HomeMode.Dua) "Settings" else "New chat",
            fill = if (isPlus) teamilyPalette().plusFill else null,
        ) {
            Icon(
                if (mode == HomeMode.Dua) Icons.Filled.Settings else Icons.Filled.Add,
                contentDescription = null,
                tint = if (isPlus) teamilyPalette().plusIcon
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A 44dp circular white button with a soft shadow — the reference's header
 * control. Built from [Surface] so the shadow and the shape come from M3 rather
 * than from a hand-rolled `Modifier.shadow`.
 *
 * [active] is the optional toggle state (the filter button uses it): when true
 * the circle takes the kit's `brandSoft` fill and `onBrandSoft` ink, and the
 * node publishes `selected` semantics so the state is announced, not only drawn.
 *
 * TOUCH TARGET (audit fix #10) — the circle stays 44dp visually, but
 * `minimumInteractiveComponentSize()` raises the touch area to the M3 48dp
 * minimum, so the control is reachable without changing the design.
 */
@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    active: Boolean = false,
    /** Optional fill override (the Agent-mode "+" uses `plusFill`). */
    fill: Color? = null,
    content: @Composable () -> Unit,
) {
    val colors = DuaGenAITheme.colors
    val palette = teamilyPalette()
    Surface(
        onClick = onClick,
        // 44dp is the VISUAL size; `minimumInteractiveComponentSize` raises the
        // touch target to the M3 48dp minimum without changing the circle.
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .semantics {
                this.contentDescription = contentDescription
                if (active) selected = true
            },
        shape = CircleShape,
        color = fill ?: if (active) colors.brandSoft else MaterialTheme.colorScheme.surface,
        shadowElevation = palette.restingShadowElevation,
        content = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                content()
            }
        },
    )
}

/**
 * The Dua | Agent segmented control.
 *
 * A two-cell pill: `surfaceVariant` track, `surface` selected cell, brand ink for
 * the active label. The reference's track is a light grey and the selected cell
 * is white — which is exactly what these two M3 slots resolve to in both themes.
 */
@Composable
private fun ModeToggle(
    mode: HomeMode,
    onModeChange: (HomeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    Row(
        modifier = modifier
            .clip(DuaGenAITheme.shapes.pill)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HomeMode.entries.forEach { entry ->
            val active = entry == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(DuaGenAITheme.shapes.pill)
                    .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onModeChange(entry) }
                    // A stable, unique label per cell. The visible text is just
                    // "Dua" / "Agent", which also appears in the drawer's nav
                    // list; this makes the control unambiguous to a screen
                    // reader ("Agent mode, selected") and to a test.
                    .semantics {
                        contentDescription = "${entry.label} mode"
                        role = Role.Tab
                        selected = active
                    }
                    .sizeIn(minHeight = 38.dp)
                    // Audit fix #10: the cell is drawn 38dp tall inside the
                    // pill's 3dp padding, which is 44dp — under the M3 minimum.
                    // `minTouchTarget()` raises the tappable box to 48dp; the
                    // pill's own fill and radius are untouched.
                    .minTouchTarget()
                    .padding(horizontal = spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = entry.label,
                    style = DuaGenAITextStyles.meta,
                    color = if (active) colors.onBrandSoft
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The ONE composer pill — Dua's home and every thread draw it.
 *
 * Layout, left to right:
 *
 *   Attach `+` · field (weight 1f) · [Skills · MCP Tools] · Send/Stop/Voice
 *
 * The field carries the row's `weight(1f)`, so it IS the flexible gap between
 * the left group (Attach) and the right group — there is no separate weighted
 * Spacer, which would split the free width with the field and squash it.
 * Every control is a 48dp touch target (`minTouchTarget()` / `MinTouchTarget`).
 *
 * The old dead 20dp mic glyph is gone; voice lives in the single trailing
 * Send/Stop/Voice control ([SendStopVoiceButton]).
 *
 * TWO CALL SITES, ONE COMPONENT
 * -----------------------------
 * `internal`, with optional parameters, because ChatScreen draws the SAME pill
 * under every thread. Dua's home passes value / change / send (a live field
 * whose Send opens a new Dua thread) plus [onUnavailable], so the disabled
 * Attach and the (unwired) voice waveform answer a tap with a snackbar. A
 * thread passes the
 * value / change / send handlers, which make the field live, and the skills /
 * MCP state, which turns on the agent tools: the Skills and MCP Tools icons and
 * a working Attach `+` that opens [ComposerPlusSheet].
 *
 * The agent tools are deliberately NOT drawn on Dua's home: Dua is the plain
 * chatbot and its home has no agent entry point of any kind (see the header of
 * this file). Skills and MCP are Dua Agent capabilities.
 */
@Composable
internal fun DuaComposer(
    placeholder: String = "Ask Dua ...",
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    onSend: (() -> Unit)? = null,
    // A reply is in flight. With [onStop] supplied the trailing control becomes
    // Stop, and IME send is guarded so a second message cannot race the first.
    sending: Boolean = false,
    onStop: (() -> Unit)? = null,
    // Agent tools. Both handlers must be supplied for the Skills / MCP icons and
    // the `+` sheet to appear — Dua's home supplies neither.
    enabledSkillIds: Set<String> = DEFAULT_DUA_SKILLS,
    onToggleSkill: ((String) -> Unit)? = null,
    mcpEnabled: Boolean = true,
    onToggleMcp: ((Boolean) -> Unit)? = null,
    /** Create tiles in the + sheet. Null keeps the tiles on screen; they dismiss only. */
    onOpenCreate: ((String) -> Unit)? = null,
    /**
     * Tap on a control that is shown disabled (Attach without agent tools,
     * Voice). The controls stay visually disabled and keep their labels; Dua
     * home uses this to show a "Jaldi aa raha hai" snackbar. Null: no click.
     */
    onUnavailable: (() -> Unit)? = null,
    /**
     * Dua home: Attach `+` is live and opens [ComposerPlusSheet] even without
     * the agent tools. Its Skills / MCP rows then answer with [onUnavailable]
     * (Dua is not the agent); its Create tiles call [onOpenCreate].
     */
    attachEnabled: Boolean = false,
    /**
     * Voice is announced enabled (not `disabled()`). There is still no voice
     * capture in this kit, so a tap goes to [onUnavailable] ("Jaldi aa raha hai").
     */
    voiceEnabled: Boolean = false,
) {
    val spacing = LocalDuaGenAISpacing.current
    // Editable only when a caller supplies a change handler. This is the single
    // switch between "Dua's presentational pill" and "a thread's live composer".
    val editable = onValueChange != null
    val canSend = editable && !value.isNullOrBlank() && !sending
    val live = editable && onStop != null
    val agentTools = onToggleSkill != null && onToggleMcp != null
    val plusLive = agentTools || attachEnabled
    var sheet by rememberSaveable { mutableStateOf(ComposerSheet.None) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.gutter,
                end = spacing.gutter,
                top = spacing.sm,
                bottom = spacing.md,
            ),
    ) {
        // Forest-slate composer capsule: 32dp (`--kit-radius-composer`) on `composerBg`, 1dp
        // `borderGradient` edge, soft floating shadow.
        val palette = teamilyPalette()
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = TeamilyShapes.composer,
            color = palette.composerBg,
            border = BorderStroke(1.dp, palette.borderGradient),
            shadowElevation = palette.restingShadowElevation,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 42.dp)
                    .padding(horizontal = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // LEFT — Attach
                ComposerIconButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = "Attach",
                    enabled = plusLive,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { sheet = ComposerSheet.Plus },
                    onDisabledClick = onUnavailable,
                )
                if (editable) {
                    BasicTextField(
                        value = value.orEmpty(),
                        onValueChange = onValueChange!!,
                        modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                        textStyle = DuaGenAITextStyles.bubble.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.5.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            // Guarded: blank text or an in-flight reply never sends.
                            onSend = { if (canSend) onSend?.invoke() },
                        ),
                        decorationBox = { innerTextField ->
                            // Centres the hint + text inside the >=48dp field box.
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (value.isNullOrEmpty()) {
                                    Text(
                                        text = placeholder,
                                        style = DuaGenAITextStyles.bubble,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                Box(Modifier.fillMaxWidth(), propagateMinConstraints = true) { innerTextField() }
                            }
                        },
                    )
                } else {
                    Text(
                        text = placeholder,
                        style = DuaGenAITextStyles.bubble,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                // RIGHT — Skills, MCP Tools, then the one Send/Stop/Voice control.
                if (agentTools) {
                    ComposerIconButton(
                        icon = Icons.Outlined.Build,
                        contentDescription = "Skills",
                        enabled = true,
                        tint = if (enabledSkillIds.isNotEmpty()) palette.toolActive
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        activeDot = enabledSkillIds.isNotEmpty(),
                        onClick = { sheet = ComposerSheet.Skills },
                    )
                    ComposerIconButton(
                        icon = Icons.Outlined.Extension,
                        contentDescription = "MCP Tools",
                        enabled = true,
                        tint = if (mcpEnabled) palette.toolActive
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        activeDot = mcpEnabled,
                        onClick = { sheet = ComposerSheet.Mcp },
                    )
                }
                SendStopVoiceButton(
                    mode = when {
                        sending && live -> SendMode.Stop
                        editable && !value.isNullOrBlank() -> SendMode.Send
                        else -> SendMode.Voice
                    },
                    onSend = { if (canSend) onSend?.invoke() },
                    onStop = { onStop?.invoke() },
                    onVoiceUnavailable = onUnavailable,
                    voiceEnabled = voiceEnabled,
                )
            }
        }
    }

    // The sheets. Opened from the icons above; the `+` sheet hands off to the
    // Skills / MCP sheets rather than duplicating them.
    if (plusLive) {
        when (sheet) {
            ComposerSheet.Plus -> ComposerPlusSheet(
                // Without agent tools (Dua home) the Skills / MCP rows close the
                // sheet and explain themselves instead of opening a dead sheet.
                onOpenSkills = {
                    if (agentTools) sheet = ComposerSheet.Skills
                    else { sheet = ComposerSheet.None; onUnavailable?.invoke() }
                },
                onOpenMcp = {
                    if (agentTools) sheet = ComposerSheet.Mcp
                    else { sheet = ComposerSheet.None; onUnavailable?.invoke() }
                },
                onDismiss = { sheet = ComposerSheet.None },
                onOpenCreate = { key ->
                    sheet = ComposerSheet.None
                    onOpenCreate?.invoke(key)
                },
            )
            ComposerSheet.Skills -> if (onToggleSkill != null) ComposerSkillsSheet(
                enabledSkillIds = enabledSkillIds,
                onToggleSkill = onToggleSkill,
                onDismiss = { sheet = ComposerSheet.None },
            )
            ComposerSheet.Mcp -> if (onToggleMcp != null) ComposerMcpSheet(
                mcpEnabled = mcpEnabled,
                onToggleMcp = onToggleMcp,
                onDismiss = { sheet = ComposerSheet.None },
            )
            ComposerSheet.None -> Unit
        }
    }
}

/**
 * A 48dp composer icon button: a 24dp glyph centred in the M3 minimum target,
 * with an optional small active dot (Skills / MCP). A disabled button takes no
 * click and publishes `disabled()` (audit fix #9).
 */
@Composable
private fun ComposerIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
    activeDot: Boolean = false,
    /** Disabled only: a tap still reaches this (e.g. a "coming soon" snackbar). */
    onDisabledClick: (() -> Unit)? = null,
) {
    val palette = teamilyPalette()
    Box(
        modifier = Modifier
            .size(MinTouchTarget)
            .clip(CircleShape)
            .then(
                when {
                    enabled -> Modifier.clickable(role = Role.Button, onClick = onClick)
                    onDisabledClick != null -> Modifier.clickable(role = Role.Button, onClick = onDisabledClick)
                    else -> Modifier
                },
            )
            .semantics {
                this.contentDescription = contentDescription
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp),
        )
        if (activeDot) {
            // 8dp logoMint dot at the glyph's top-end, ringed in the pill's own
            // fill so it reads as a badge rather than part of the glyph.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = 18.dp, bottom = 18.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(palette.composerBg)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(palette.logoMint),
            )
        }
    }
}

private enum class SendMode { Send, Stop, Voice }

/**
 * The single trailing control: a 48dp touch target wrapping a 42dp
 * `brandGradient` disc (emerald #0B8249 -> #087C45 in light and dark, with the
 * `sendButtonIcon` glyph — white — and the subtle primary glow), replacing the old separate 40dp voice disc +
 * 36dp send disc.
 *
 *  • Stop  — a reply is in flight on a live thread; calls `onStop`.
 *  • Send  — the field has text; calls `onSend`.
 *  • Voice — otherwise: a four-bar waveform. Voice capture is NOT wired in this
 *            kit, so this state is announced disabled and takes no click.
 */
@Composable
private fun SendStopVoiceButton(
    mode: SendMode,
    onSend: () -> Unit,
    onStop: () -> Unit,
    // Voice is not wired: still drawn and announced disabled, but a tap can
    // explain that (Dua home's "Jaldi aa raha hai" snackbar). Null: no click.
    onVoiceUnavailable: (() -> Unit)? = null,
    // Voice announced enabled (Dua home). The tap still goes to
    // [onVoiceUnavailable] because no voice capture exists yet.
    voiceEnabled: Boolean = false,
) {
    val palette = teamilyPalette()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(MinTouchTarget)
            .teamilyPressScale(interactionSource)
            .then(
                when (mode) {
                    SendMode.Send -> Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onSend,
                    )
                    SendMode.Stop -> Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onStop,
                    )
                    SendMode.Voice -> if (onVoiceUnavailable != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            role = Role.Button,
                            onClick = onVoiceUnavailable,
                        )
                    } else {
                        Modifier
                    }
                },
            )
            .semantics {
                contentDescription = when (mode) {
                    SendMode.Send -> "Send message"
                    SendMode.Stop -> "Stop"
                    SendMode.Voice -> "Voice input"
                }
                if (mode == SendMode.Voice && !voiceEnabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                // Subtle primary green glow (light and dark).
                .teamilyPrimaryGlow(palette)
                .clip(CircleShape)
                .background(palette.brandGradient),
            contentAlignment = Alignment.Center,
        ) {
            when (mode) {
                SendMode.Send -> Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    tint = palette.sendButtonIcon,
                    modifier = Modifier.size(20.dp),
                )
                SendMode.Stop -> Icon(
                    Icons.Filled.Stop,
                    contentDescription = null,
                    tint = palette.sendButtonIcon,
                    modifier = Modifier.size(20.dp),
                )
                SendMode.Voice -> Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Four bars, drawn as shapes so no icon asset is needed.
                    listOf(8.dp, 14.dp, 10.dp, 6.dp).forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(h)
                                .clip(CircleShape)
                                .background(palette.sendButtonIcon),
                        )
                    }
                }
            }
        }
    }
}

/* ============================================================================
 *  AGENT HOME — the Teamily chat-list layout (agent-home-reference.jpg)
 *
 *  Header (avatar · badge + "Dua Gen AI" · plus) → filter + Search pill →
 *  ONE filled hero card (the personal agent) → canvas rows with no card boxes
 *  (Pinned · 1 → Dua Agent ✓, divider, Chats · 6 → the listed chats) → floating
 *  5-tab dock drawn OVER the list, so the last row sits under it.
 * ==========================================================================*/

/** Hero card title. The thread opened from the card uses the same string. */
internal const val PERSONAL_AGENT_TITLE = "fareed's Personal Agent"
/** Honest hero subtitle — a description of what the card opens, not a fake chat line. */
internal const val PERSONAL_AGENT_PREVIEW = "Your own assistant — tap to chat"
internal const val PERSONAL_AGENT_DATE = "Mon"

/** The pinned official agent row. Matches Teamily AI mobile app parity. */
internal const val DUA_AGENT_TITLE = "Dua Agent"
internal const val DUA_AGENT_SUBTITLE = "I'm Dua Agent \u2014 the official agent of Dua Gen AI"
/** Timestamp on the pinned row: "Mon" right-aligned matching Teamily parity. */
internal const val DUA_AGENT_TIME = "Mon"

/** Header brand text. Matches Teamily AI mobile app parity. */
internal const val APP_NAME = "Dua Gen AI"

/**
 * Agent home's header: 48dp circular user avatar (opens the drawer) · 32dp
 * rounded-square app badge + 21sp bold "Dua Gen AI" · 48dp circular plus (New group).
 * No Dua | Agent toggle here — the way back to Dua is the drawer's "Dua" row.
 */
@Composable
private fun AgentHomeTopBar(
    onOpenDrawer: () -> Unit,
    onCreateAgent: () -> Unit,
    onNewGroup: () -> Unit,
    userAvatar: Painter?,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.gutter, end = spacing.gutter, top = 12.dp, bottom = 6.dp),
    ) {
        // LEFT — the user's photo: 35.dp visual circle inside 48.dp touch target.
        val photo = userAvatar ?: painterResource(R.drawable.dua_user_avatar)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(palette.chromeFill)
                    .clickable(role = Role.Button, onClick = onOpenDrawer)
                    .semantics { contentDescription = "Open menu" },
            ) {
                Image(
                    painter = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // 8.dp #42D982 fill, 2.dp #1D2120 ring.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 24.dp, y = 3.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(palette.avatarOnlineRing)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(palette.logoMint),
            )
        }

        // CENTER — badge + app name.
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppBadge(size = 30.dp, shape = TeamilyShapes.xs)
            Spacer(Modifier.width(spacing.sm))
            Text(
                text = APP_NAME,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.01).em,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }

        // RIGHT — plus (48.dp visual circle). Opens the menu of REAL actions.
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            var menuOpen by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.plusFill)
                    .clickable(role = Role.Button) { menuOpen = true }
                    .semantics { contentDescription = "New chat" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = palette.plusIcon,
                    modifier = Modifier.size(24.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Create Agent") },
                    onClick = {
                        menuOpen = false
                        onCreateAgent()
                    },
                )
                DropdownMenuItem(
                    text = { Text("New Group") },
                    onClick = {
                        menuOpen = false
                        onNewGroup()
                    },
                )
            }
        }
    }
}

/**
 * Dua home's body: a centred bot badge, a greeting and four quick starters.
 * Copy lives in `strings.xml` (`dua_home_greeting`, `dua_home_starter_*`).
 */
@Composable
private fun DuaHomeWelcome(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val starters = listOf(
        stringResource(R.string.dua_home_starter_explain),
        stringResource(R.string.dua_home_starter_email),
        stringResource(R.string.dua_home_starter_ideas),
        stringResource(R.string.dua_home_starter_plan),
    )
    // Box + centred Column: centred when it fits, scrollable when it does not
    // (small phones, large font scale, keyboard up).
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BotBadge(56.dp)
            Spacer(Modifier.height(spacing.md))
            Text(
                text = stringResource(R.string.dua_home_greeting),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(spacing.sm))
            QuickActionChips(
                label = null,
                actions = starters,
                onPick = onPick,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            )
        }
    }
}

/**
 * The green app badge: the cube logo (approved agent-home SVG `cube-logo`).
 * A `logoMint` tile or disc with the cube glyph in `tileInk`. The glyph is 65%
 * of the 30dp / 48dp badge and 75% of the 16dp mini badge, stroked at 1.75dp
 * (1.25dp on the mini) — the same values the preview page draws.
 */
@Composable
private fun AppBadge(size: Dp, shape: Shape) {
    val palette = teamilyPalette()
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(palette.logoMint),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.dua_bot_head),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size * 0.82f),
        )
    }
}

/**
 * The cube logo glyph on its 24-unit grid — `M12 2.6l8.1 4.7v9.4L12 21.4
 * 3.9 16.7V7.3z` plus the Y seam `M12 12l0-9.4M12 12l-8.1 4.7M12 12l8.1 4.7`,
 * scaled to the composable's size. Round caps and joins.
 */
@Composable
private fun CubeLogoGlyph(tint: Color, strokeWidth: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCubeLogo(tint, Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Draws the cube logo paths (24-unit grid) scaled to this canvas. */
private fun DrawScope.drawCubeLogo(tint: Color, stroke: Stroke) {
    val u = size.minDimension / 24f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    val hexagon = Path().apply {
        p(12f, 2.6f).let { moveTo(it.x, it.y) }
        p(20.1f, 7.3f).let { lineTo(it.x, it.y) }
        p(20.1f, 16.7f).let { lineTo(it.x, it.y) }
        p(12f, 21.4f).let { lineTo(it.x, it.y) }
        p(3.9f, 16.7f).let { lineTo(it.x, it.y) }
        p(3.9f, 7.3f).let { lineTo(it.x, it.y) }
        close()
    }
    drawPath(hexagon, color = tint, style = stroke)
    val c = p(12f, 12f)
    val seam = Path().apply {
        for (end in listOf(p(12f, 2.6f), p(3.9f, 16.7f), p(20.1f, 16.7f))) {
            moveTo(c.x, c.y)
            lineTo(end.x, end.y)
        }
    }
    drawPath(seam, color = tint, style = stroke)
}

/**
 * Agent mode's search row: a 48dp circular filter button (chromeFill, 1dp
 * border) and a full-width pill search bar (999dp, 1dp border).
 *
 * FILTER (audit fix #7) — a real toggle; the active state uses `brandSoft` /
 * `onBrandSoft` and publishes `selected`.
 * SEARCH (audit fix #9) — a presentational placeholder: no click, `disabled()`.
 */
@Composable
private fun SearchRow(
    filterActive: Boolean = false,
    onToggleFilter: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (filterActive) colors.brandSoft else Color(0xFF2A322E))
                .clickable(role = Role.Button, onClick = onToggleFilter)
                .semantics {
                    contentDescription = "Filter"
                    if (filterActive) selected = true
                },
            contentAlignment = Alignment.Center,
        ) {
            // Three centred bars, longest on top — the reference's filter glyph.
            val ink = if (filterActive) colors.onBrandSoft else MaterialTheme.colorScheme.onSurface
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                listOf(15.dp, 10.dp, 5.dp).forEach { w ->
                    Box(
                        modifier = Modifier
                            .width(w)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(ink),
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .semantics {
                    contentDescription = "Search"
                    disabled()
                },
            shape = TeamilyShapes.capsule,
            color = Color(0xFF181C1B),
            border = BorderStroke(1.dp, Color(0xFF323A36)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .padding(horizontal = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(spacing.sm))
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The ONE filled card on Agent home: the user's personal agent.
 *
 * `cardHeroColor` fill, 20dp corners, 1dp soft rim (`heroCardBorder`), resting
 * glass shadow and the 0.98 press scale. The avatar and its mini-badge are
 * the 3D bot image. Tapping it opens the Personal Agent thread
 * ([onClick] = `onOpenPersonalAgentChat`).
 */
@Composable
internal fun PersonalAgentHeroCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val heroShape = RoundedCornerShape(10.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .teamilyPressScale(interactionSource, targetScale = 0.97f)
            .teamilyRestingShadow(palette, heroShape)
            .clip(heroShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .semantics {
                contentDescription = "$PERSONAL_AGENT_TITLE, $PERSONAL_AGENT_PREVIEW, $PERSONAL_AGENT_DATE"
            },
        shape = heroShape,
        color = Color(0xFF263C30),
        border = null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatAvatar(
                ring = Color(0xFF263C30),
                miniBadge = true,
                size = 42.dp,
                mini = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(palette.logoMint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.dua_bot_head),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3E8EE)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.dua_bot_head),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = PERSONAL_AGENT_TITLE,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.5.sp,
                            lineHeight = 22.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (PERSONAL_AGENT_DATE.isNotEmpty()) {
                        Spacer(Modifier.width(spacing.sm))
                        Text(
                            text = PERSONAL_AGENT_DATE,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = PERSONAL_AGENT_PREVIEW,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A circular avatar with an optional green mini-badge at its bottom-end.
 * The badge is ringed in [ring] (the surface behind it) so it reads as a badge.
 */
@Composable
private fun ChatAvatar(
    ring: Color,
    miniBadge: Boolean,
    size: Dp = 40.dp,
    mini: @Composable () -> Unit = { AppBadge(size = 14.dp, shape = CircleShape) },
    content: @Composable () -> Unit,
) {
    val badgeSize = if (size > 44.dp) 18.dp else 17.dp
    Box(Modifier.size(size + 4.dp)) {
        Box(Modifier.align(Alignment.TopStart).size(size).clip(CircleShape)) { content() }
        if (miniBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(ring)
                    .padding(1.dp),
                contentAlignment = Alignment.Center,
            ) {
                mini()
            }
        }
    }
}

/**
 * Personal Agent avatar. Draws the Appearance pick (Grok Bot shape + color).
 * Decorative: the row already names the agent.
 */
@Composable
internal fun BotBadge(size: Dp) {
    val look = LocalBotAppearance.current
    BabyGrokMark(
        shape = look.shape,
        color = look.color,
        size = size,
    )
}

/**
 * Dua Agent's row avatar: [R.drawable.dua_bot_badge] on a `#00D95A` circle.
 * The PNG's mint field is made transparent so the circle shows. [BotBadge]
 * stays the untouched mint image for the hero.
 */
@Composable
internal fun SignalBotBadge(size: Dp = 42.dp) {
    val green = teamilyPalette().signalGreen
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(green),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.dua_bot_head),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size * 0.81f),
        )
    }
}

/** [R.drawable.dua_bot_badge] with its mint `#42D982` field punched out. */
private fun botBadgeWithoutMint(context: android.content.Context): ImageBitmap {
    val decoded = BitmapFactory.decodeResource(context.resources, R.drawable.dua_bot_badge)
    val src = decoded.copy(Bitmap.Config.ARGB_8888, true)
    val width = src.width
    val height = src.height
    val pixels = IntArray(width * height)
    src.getPixels(pixels, 0, width, 0, 0, width, height)
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        // Mint field and its anti-aliased edge. The face is near-white and the
        // body is near-black, so a green-leading pixel is background.
        if (g > 90 && g > r + 28 && g > b + 18) pixels[i] = 0
    }
    src.setPixels(pixels, 0, width, 0, 0, width, height)
    return src.asImageBitmap()
}

/** One chat row's 3D character, cropped to the avatar circle. */
/**
 * One canvas row — no card box: avatar · title (+ verified ✓) / 1-line
 * subtitle · right-aligned time. 18dp side padding to align avatar with hero card.
 */
@Composable
private fun CanvasChatRow(
    title: String,
    subtitle: String,
    time: String,
    verified: Boolean = false,
    onClick: () -> Unit,
    avatar: @Composable () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val rowModifier = Modifier
        .fillMaxWidth()
        .teamilyPressScale(interactionSource, targetScale = 0.97f)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        )
        .padding(horizontal = 18.dp, vertical = 9.dp)
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        avatar()
        Spacer(Modifier.width(spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.5.sp,
                            lineHeight = 20.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (verified) {
                        Spacer(Modifier.width(4.dp))
                        VerifiedCheck()
                    }
                }
                if (time.isNotEmpty()) {
                    Spacer(Modifier.width(spacing.sm))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The green verified check next to "Dua Agent". */
@Composable
internal fun VerifiedCheck() {
    val palette = teamilyPalette()
    Canvas(
        Modifier
            .size(16.dp)
            .semantics { contentDescription = "Verified" },
    ) {
        val w = size.width
        val h = size.height
        drawCircle(palette.logoMint)
        val p = Path().apply {
            moveTo(w * 0.30f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.66f)
            lineTo(w * 0.71f, h * 0.38f)
        }
        drawPath(p, color = palette.tileInk, style = Stroke(width = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round))
    }
}

private enum class SectionIcon { Pin, Chat }

/**
 * A collapsible section header: icon · "Label · N" · chevron. Tapping it
 * collapses / expands the rows under it; the state is announced.
 */
@Composable
private fun SectionHeader(
    icon: SectionIcon,
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    /** Shows the small "Demo" label after [label] (sample rows). */
    demo: Boolean = false,
) {
    val spacing = LocalDuaGenAISpacing.current
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .padding(horizontal = spacing.sm)
            .clip(TeamilyShapes.capsule)
            .clickable(role = Role.Button, onClick = onToggle)
            .sizeIn(minHeight = 34.dp)
            .padding(horizontal = spacing.sm)
            .semantics(mergeDescendants = true) {
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(13.dp)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (icon) {
                // A tilted push-pin.
                SectionIcon.Pin -> {
                    val p = Path().apply {
                        moveTo(w * 0.58f, h * 0.10f)
                        lineTo(w * 0.90f, h * 0.42f)
                        lineTo(w * 0.78f, h * 0.48f)
                        lineTo(w * 0.62f, h * 0.66f)
                        lineTo(w * 0.62f, h * 0.80f)
                        lineTo(w * 0.20f, h * 0.38f)
                        lineTo(w * 0.34f, h * 0.38f)
                        lineTo(w * 0.52f, h * 0.22f)
                        close()
                    }
                    drawPath(p, color = ink, style = stroke)
                    drawLine(ink, Offset(w * 0.40f, h * 0.60f), Offset(w * 0.12f, h * 0.88f), stroke.width, StrokeCap.Round)
                }
                // A round speech bubble with a tail.
                SectionIcon.Chat -> {
                    drawCircle(ink, radius = w * 0.40f, center = Offset(w * 0.52f, h * 0.48f), style = stroke)
                    drawLine(ink, Offset(w * 0.22f, h * 0.72f), Offset(w * 0.12f, h * 0.90f), stroke.width, StrokeCap.Round)
                }
            }
        }
        Spacer(Modifier.width(spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
            color = ink,
        )
        if (demo) {
            Spacer(Modifier.width(spacing.xs))
            DemoBadge()
        }
        Spacer(Modifier.width(spacing.xs))
        Canvas(Modifier.size(11.dp).rotate(if (expanded) 0f else -90f)) {
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(w * 0.2f, h * 0.38f)
                lineTo(w * 0.5f, h * 0.66f)
                lineTo(w * 0.8f, h * 0.38f)
            }
            drawPath(p, color = ink, style = Stroke(width = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round))
        }
    }
}

/**
 * Agent home's scrolling body: Pinned section (Teamily Agent) and Chats section
 * (Frontend Developer), matching the exact Teamily AI mobile app hierarchy.
 * Personal Agent hero card remains fixed at the top during scroll.
 */
@Composable
private fun AgentHomeList(
    onOpenDuaAgentChat: () -> Unit,
    conversations: List<ConversationItem> = emptyList(),
    onOpenListedChat: (chatId: String, title: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()
    var pinnedExpanded by rememberSaveable { mutableStateOf(true) }
    var chatsExpanded by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 2.dp, bottom = 112.dp),
    ) {
        item {
            Column {
                SectionHeader(
                    icon = SectionIcon.Pin,
                    label = "Pinned \u00b7 1",
                    expanded = pinnedExpanded,
                    onToggle = { pinnedExpanded = !pinnedExpanded },
                )
            }
        }
        if (pinnedExpanded) {
            item {
                CanvasChatRow(
                    title = DUA_AGENT_TITLE,
                    subtitle = DUA_AGENT_SUBTITLE,
                    time = DUA_AGENT_TIME,
                    verified = true,
                    onClick = onOpenDuaAgentChat,
                ) {
                    ChatAvatar(ring = palette.bgCanvas, miniBadge = false) {
                        SignalBotBadge(40.dp)
                    }
                }
            }
        }
        item {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    thickness = 0.5.dp,
                    color = palette.border,
                )
                SectionHeader(
                    icon = SectionIcon.Chat,
                    label = "Chats \u00b7 ${conversations.size}",
                    expanded = chatsExpanded,
                    onToggle = { chatsExpanded = !chatsExpanded },
                )
            }
        }
        if (chatsExpanded) {
            if (conversations.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.gutter)
                            .padding(top = 12.dp, bottom = 16.dp),
                    ) {
                        Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.5.sp,
                                lineHeight = 20.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Chats you start will appear here.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(conversations.size, key = { conversations[it].id }) { index ->
                    val item = conversations[index]
                    CanvasChatRow(
                        title = item.title,
                        subtitle = item.lastMessagePreview.ifBlank { "Tap to open chat" },
                        time = item.lastMessageAt,
                        onClick = { onOpenListedChat(item.id, item.title) },
                    ) {
                        ChatAvatar(ring = palette.bgCanvas, miniBadge = false) {
                            CubeAvatar(
                                cubeId = item.toChatRow().cubeId,
                                size = 40.dp,
                                shape = CircleShape,
                                contentDescription = item.title,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The floating bottom dock: a detached 999dp capsule (16dp side margin, 12dp
 * bottom margin) on `bottomnavBg`, 1dp `borderGradient` rim and a drop
 * shadow. Five tabs; the active one sits in a `navSelected` pill.
 *
 * Internal so the nav host can hand the SAME dock to the four tab screens
 * (Agents, Discover, Memory, Artifacts) through their `bottomBar` slot.
 */
@Composable
internal fun HomeBottomNav(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Whether Memory has unread updates. There is no real unread-memory source
     * yet, so it defaults to false: no dot, and TalkBack says just "Memory".
     * The dot sits on the glyph as an overlay, so hiding it changes no size.
     */
    hasUnreadMemory: Boolean = true,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Lift the dock clear of the system gesture / navigation bar. The
            // window is edge-to-edge, so without this the 12dp bottom margin is
            // measured from the very bottom of the screen and the dock ends up
            // sitting under the gesture pill.
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        shape = TeamilyShapes.capsule,
        color = palette.bottomnavBg,
        border = BorderStroke(1.dp, palette.borderGradient),
        shadowElevation = palette.restingShadowElevation,
    ) {
        Row(
            // `selectableGroup()` makes the five tabs one tab list for TalkBack
            // (the HTML preview's role="tablist").
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeTab.entries.forEach { tab ->
                val active = tab == selected
                val tint = if (active) palette.navSelectedContent
                else palette.iconIdle
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    // The capsule clip lives on a background layer only, so the
                    // Memory dot (which sits just outside the glyph) is not cut
                    // by the selected pill's rounded corner. The ripple is drawn
                    // in that clipped layer, so it keeps the capsule shape.
                    val interaction = remember { MutableInteractionSource() }
                    val haptic = LocalHapticFeedback.current
                    Box(
                        modifier = Modifier
                            .teamilyPressScale(interaction, targetScale = 0.97f)
                            .clickable(interactionSource = interaction, indication = null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tab)
                            }
                            .height(76.dp)
                            .fillMaxWidth()
                            .semantics {
                                role = Role.Tab
                                // `this.` — the parameter `selected` would shadow it.
                                this.selected = active
                                // Matches Teamily mobile app accessibility label.
                                // "New updates" only with a real unread source.
                                if (tab == HomeTab.Memory) {
                                    contentDescription =
                                        if (hasUnreadMemory) "${tab.label}, New updates" else tab.label
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .clip(TeamilyShapes.capsule)
                                .background(if (active) palette.navSelected else Color.Transparent)
                                .indication(interaction, LocalIndication.current),
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = spacing.xs, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box {
                                TabGlyph(tab = tab, tint = tint, strong = active)
                                if (tab == HomeTab.Memory && hasUnreadMemory) {
                                    // The reference's unread dot: 13.dp pale-green dot (#A2E4B6 fill with 2.dp #141816 outline ring)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset(x = 16.dp, y = (-5).dp)
                                            .size(13.dp)
                                            .clip(CircleShape)
                                            .background(palette.notificationOutline)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(palette.notificationDot),
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = tab.label,
                                style = DuaGenAITextStyles.navLabel.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = tint,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One 24dp glyph per tab, drawn with [Canvas] (the kit ships
 * `material-icons-core` only). The preview page draws the same five shapes.
 */
@Composable
private fun TabGlyph(tab: HomeTab, tint: Color, strong: Boolean = false) {
    // The chat list keeps Chats selected. That tab's glyph is the 3D bot,
    // sitting directly on the navSelected pill (#545B56 in dark) with transparent background.
    if (tab == HomeTab.Chats) {
        Image(
            painter = painterResource(R.drawable.dua_bot_head),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(28.dp),
        )
        return
    }
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        // `--nv-icon-stroke` 1.75; the active tab uses `--nv-icon-stroke-strong` 2.25.
        val width = if (strong) TeamilyStroke.iconStrong else TeamilyStroke.icon
        val stroke = Stroke(width = width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (tab) {
            // Drawn above as the 3D bot. This branch is only for exhaustiveness.
            HomeTab.Chats -> Unit

            // A robot: antenna, rounded head, two eyes, side ears.
            HomeTab.Managed -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.30f), stroke.width, StrokeCap.Round)
                val head = Path().apply {
                    moveTo(w * 0.32f, h * 0.30f)
                    lineTo(w * 0.68f, h * 0.30f)
                    quadraticTo(w * 0.80f, h * 0.30f, w * 0.80f, h * 0.42f)
                    lineTo(w * 0.80f, h * 0.70f)
                    quadraticTo(w * 0.80f, h * 0.82f, w * 0.68f, h * 0.82f)
                    lineTo(w * 0.32f, h * 0.82f)
                    quadraticTo(w * 0.20f, h * 0.82f, w * 0.20f, h * 0.70f)
                    lineTo(w * 0.20f, h * 0.42f)
                    quadraticTo(w * 0.20f, h * 0.30f, w * 0.32f, h * 0.30f)
                    close()
                }
                drawPath(head, color = tint, style = stroke)
                drawLine(tint, Offset(w * 0.40f, h * 0.50f), Offset(w * 0.40f, h * 0.60f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.60f, h * 0.50f), Offset(w * 0.60f, h * 0.60f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.08f, h * 0.52f), Offset(w * 0.08f, h * 0.62f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.92f, h * 0.52f), Offset(w * 0.92f, h * 0.62f), stroke.width, StrokeCap.Round)
            }

            // A compass: ring plus needle.
            HomeTab.Discover -> {
                drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                val p = Path().apply {
                    moveTo(w * 0.64f, h * 0.36f)
                    lineTo(w * 0.55f, h * 0.58f)
                    lineTo(w * 0.36f, h * 0.64f)
                    lineTo(w * 0.45f, h * 0.42f)
                    close()
                }
                drawPath(p, color = tint, style = stroke)
            }

            // A brain: two lobes split down the middle.
            HomeTab.Memory -> {
                val brain = Path().apply {
                    moveTo(w * 0.5f, h * 0.20f)
                    cubicTo(w * 0.40f, h * 0.08f, w * 0.20f, h * 0.14f, w * 0.22f, h * 0.30f)
                    cubicTo(w * 0.06f, h * 0.36f, w * 0.06f, h * 0.58f, w * 0.18f, h * 0.64f)
                    cubicTo(w * 0.16f, h * 0.82f, w * 0.38f, h * 0.92f, w * 0.5f, h * 0.80f)
                    cubicTo(w * 0.62f, h * 0.92f, w * 0.84f, h * 0.82f, w * 0.82f, h * 0.64f)
                    cubicTo(w * 0.94f, h * 0.58f, w * 0.94f, h * 0.36f, w * 0.78f, h * 0.30f)
                    cubicTo(w * 0.80f, h * 0.14f, w * 0.60f, h * 0.08f, w * 0.5f, h * 0.20f)
                    close()
                }
                drawPath(brain, color = tint, style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.20f), Offset(w * 0.5f, h * 0.80f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.30f, h * 0.46f), Offset(w * 0.40f, h * 0.52f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.70f, h * 0.46f), Offset(w * 0.60f, h * 0.52f), stroke.width, StrokeCap.Round)
            }

            // Stacked layers: a diamond over two chevrons.
            HomeTab.Artifacts -> {
                val top = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.90f, h * 0.32f)
                    lineTo(w * 0.5f, h * 0.52f)
                    lineTo(w * 0.10f, h * 0.32f)
                    close()
                }
                drawPath(top, color = tint, style = stroke)
                val mid = Path().apply {
                    moveTo(w * 0.10f, h * 0.50f)
                    lineTo(w * 0.5f, h * 0.70f)
                    lineTo(w * 0.90f, h * 0.50f)
                }
                drawPath(mid, color = tint, style = stroke)
                val low = Path().apply {
                    moveTo(w * 0.10f, h * 0.68f)
                    lineTo(w * 0.5f, h * 0.88f)
                    lineTo(w * 0.90f, h * 0.68f)
                }
                drawPath(low, color = tint, style = stroke)
            }
        }
    }
}

@Preview(name = "Home Dua — light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Home Dua — light — tablet", showBackground = true, widthDp = 800, heightDp = 1280)
@Preview(name = "Home Dua — light — foldable", showBackground = true, widthDp = 673, heightDp = 841)
@Composable
private fun HomeDuaLight() = DuaGenAITheme(darkTheme = false) { HomeScreen(mode = HomeMode.Dua) }

@Preview(name = "Home Dua — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Home Dua — dark — tablet", showBackground = true, widthDp = 800, heightDp = 1280)
@Preview(name = "Home Dua — dark — foldable", showBackground = true, widthDp = 673, heightDp = 841)
@Composable
private fun HomeDuaDark() = DuaGenAITheme(darkTheme = true) { HomeScreen(mode = HomeMode.Dua) }

@Preview(name = "Home Agent — light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Home Agent — light — tablet", showBackground = true, widthDp = 800, heightDp = 1280)
@Preview(name = "Home Agent — light — foldable", showBackground = true, widthDp = 673, heightDp = 841)
@Composable
private fun HomeAgentLight() = DuaGenAITheme(darkTheme = false) {
    HomeScreen(mode = HomeMode.Agent)
}

@Preview(name = "Home Agent — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Home Agent — dark — tablet", showBackground = true, widthDp = 800, heightDp = 1280)
@Preview(name = "Home Agent — dark — foldable", showBackground = true, widthDp = 673, heightDp = 841)
@Composable
private fun HomeAgentDark() = DuaGenAITheme(darkTheme = true) {
    HomeScreen(mode = HomeMode.Agent)
}
