package ai.duagenai.design.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import ai.duagenai.design.theme.TeamilyTypography
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
 *        divider, then "Chats · 5"; both section headers collapse
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
 *      Dua's home is empty, so conversations are reached from the sidebar
 *      drawer's history rows — the same `sampleChats` list, the same per-chat
 *      cube. The drawer hands its row's `chatId` and `name` to `onOpenChat`, the
 *      nav host turns them into `chat/{chatId}`, and ChatScreen derives its cube
 *      from the SAME id with the SAME pure function.
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
 * The sample conversations.
 *
 * `internal` rather than private because [SidebarDrawer] renders the same list
 * as its history section — one list, so the drawer and any other surface can
 * never disagree about a chat's name or its cube.
 */
internal val sampleChats = listOf(
    ChatRow("chat_webpage_developer", "Webpage Developer"),
    ChatRow("chat_design_team", "Design Team · 5"),
    ChatRow("chat_slides_assistant", "Slides Assistant"),
    ChatRow("chat_david_park", "David Park"),
    ChatRow("chat_market_research", "Market Research"),
    ChatRow("chat_accounting_assistant", "Accounting Assistant"),
    ChatRow("chat_family", "Family · 8"),
)

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
    /** Current assistant mode. Hoisted so it survives configuration changes. */
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
     * Opens AGENT'S OWN thread. The pinned Agent card is the entry point: Agent
     * is an agent, so it has a conversation of its own, and tapping its card
     * opens it through the same `chat/{chatId}` route every other thread uses.
     */
    onOpenAgentChat: () -> Unit = {},
    /**
     * Agent home's header photo (user-avatar.jpg) and green app badge
     * (bot-badge.png). This package has no `res/`, so the app passes
     * `painterResource(...)` for each; the reference images ship in
     * webpages/M3-design_v01/assets/. Null draws a neutral fallback.
     */
    userAvatar: Painter? = null,
    appBadge: Painter? = null,
    modifier: Modifier = Modifier,
) {
    // The search row's filter toggle. Screen-local UI state, saved so it
    // survives rotation — the same `rememberSaveable` pattern the host uses for
    // `mode` and `selectedTab`. It is NOT hoisted to the host because nothing
    // outside this screen reads it.
    var filterActive by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Teamily canvas: the zip `--nv-atmosphere` (light #E8F3EC glow
            // over #FAFBFA, dark #242B27 glow over #171C19, both at 8%/92%).
            .teamilyCanvas(teamilyPalette()),
    ) {
        Column(Modifier.fillMaxSize()) {

            if (mode == HomeMode.Dua) {
                /* ================= DUA MODE =================
                 * Unchanged. Top bar: hamburger · Dua|Agent toggle · gear. Dua
                 * is a chatbot, so the body is empty — no chat list, no search
                 * row, no bottom navigation, nothing agent-related. The only
                 * control is the composer at the bottom. */
                HomeTopBar(
                    mode = mode,
                    onOpenDrawer = onOpenDrawer,
                    onModeChange = onModeChange,
                    onOpenSettings = onOpenSettings,
                    onNewChat = onNewChat,
                )
                Spacer(Modifier.weight(1f).fillMaxWidth())
                DuaComposer()
            } else {
                /* ================= AGENT MODE =================
                 * The reference's chat-list home: avatar · badge + "Dua Gen AI"
                 * · plus, then filter + Search, then the scrolling list (hero
                 * card, Pinned, Chats) with the floating dock drawn OVER it.
                 * No composer. */
                AgentHomeTopBar(
                    onOpenDrawer = onOpenDrawer,
                    onNewChat = onNewChat,
                    userAvatar = userAvatar,
                    appBadge = appBadge,
                )
                SearchRow(
                    filterActive = filterActive,
                    onToggleFilter = { filterActive = !filterActive },
                    modifier = Modifier.padding(
                        start = LocalDuaGenAISpacing.current.gutter,
                        end = LocalDuaGenAISpacing.current.gutter,
                        top = 8.dp,
                        bottom = 16.dp,
                    ),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    AgentHomeList(
                        onOpenAgentChat = onOpenAgentChat,
                        appBadge = appBadge,
                        modifier = Modifier.fillMaxSize(),
                    )
                    HomeBottomNav(
                        selected = selectedTab,
                        onSelect = onSelectTab,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
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
        shadowElevation = 2.dp,
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
 * under every thread. Dua's home passes nothing: a static "Ask Dua ..." hint,
 * an unavailable Attach and the (unwired) voice waveform. A thread passes the
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
) {
    val spacing = LocalDuaGenAISpacing.current
    // Editable only when a caller supplies a change handler. This is the single
    // switch between "Dua's presentational pill" and "a thread's live composer".
    val editable = onValueChange != null
    val canSend = editable && !value.isNullOrBlank() && !sending
    val live = editable && onStop != null
    val agentTools = onToggleSkill != null && onToggleMcp != null
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
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 52.dp)
                    .padding(horizontal = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // LEFT — Attach. A real 48dp button wherever there is a sheet
                // to open (a thread); on Dua's home there is no destination, so
                // it keeps the audit-fix #9 guard: no click, announced disabled.
                ComposerIconButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = "Attach",
                    enabled = agentTools,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { sheet = ComposerSheet.Plus },
                )
                if (editable) {
                    // The real input. Same text style and colour as the static
                    // hint it replaces, so the pill does not change shape when a
                    // thread becomes editable.
                    BasicTextField(
                        value = value.orEmpty(),
                        onValueChange = onValueChange!!,
                        modifier = Modifier.weight(1f),
                        textStyle = DuaGenAITextStyles.bubble.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            // Guarded: blank text or an in-flight reply never sends.
                            onSend = { if (canSend) onSend?.invoke() },
                        ),
                        decorationBox = { innerTextField ->
                            if (value.isNullOrEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = DuaGenAITextStyles.bubble,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
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
                )
            }
        }
    }

    // The sheets. Opened from the icons above; the `+` sheet hands off to the
    // Skills / MCP sheets rather than duplicating them.
    if (agentTools) {
        when (sheet) {
            ComposerSheet.Plus -> ComposerPlusSheet(
                onOpenSkills = { sheet = ComposerSheet.Skills },
                onOpenMcp = { sheet = ComposerSheet.Mcp },
                onDismiss = { sheet = ComposerSheet.None },
            )
            ComposerSheet.Skills -> ComposerSkillsSheet(
                enabledSkillIds = enabledSkillIds,
                onToggleSkill = onToggleSkill!!,
                onDismiss = { sheet = ComposerSheet.None },
            )
            ComposerSheet.Mcp -> ComposerMcpSheet(
                mcpEnabled = mcpEnabled,
                onToggleMcp = onToggleMcp!!,
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
) {
    val palette = teamilyPalette()
    Box(
        modifier = Modifier
            .size(MinTouchTarget)
            .clip(CircleShape)
            .then(
                if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier,
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
            modifier = Modifier.size(24.dp),
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
 * `brandGradient` disc (zip `--nv-primary-fill`: light #0B8249 -> #087C45 with a
 * white glyph, dark flat #CBE7D2 with a #17201B glyph and no shadow — the same
 * pair as the dark header plus), replacing the old separate 40dp voice disc +
 * 36dp send disc.
 *
 *  • Stop  — a reply is in flight on a live thread; calls `onStop`.
 *  • Send  — the field has text; calls `onSend`.
 *  • Voice — otherwise: a four-bar waveform. Voice capture is NOT wired in this
 *            kit, so this state is announced disabled and takes no click.
 */
@Composable
private fun SendStopVoiceButton(mode: SendMode, onSend: () -> Unit, onStop: () -> Unit) {
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
                    SendMode.Voice -> Modifier
                },
            )
            .semantics {
                contentDescription = when (mode) {
                    SendMode.Send -> "Send message"
                    SendMode.Stop -> "Stop"
                    SendMode.Voice -> "Voice input"
                }
                if (mode == SendMode.Voice) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                // Light-mode primary green glow (no-op in dark).
                .teamilyPrimaryGlow(palette)
                .clip(CircleShape)
                .background(palette.brandGradient),
            contentAlignment = Alignment.Center,
        ) {
            when (mode) {
                SendMode.Send -> Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    tint = palette.accentContrast,
                    modifier = Modifier.size(20.dp),
                )
                SendMode.Stop -> Icon(
                    Icons.Filled.Stop,
                    contentDescription = null,
                    tint = palette.accentContrast,
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
                                .background(palette.accentContrast),
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
 *  (Pinned · 1 → Dua Agent ✓, divider, Chats · 5 → five chats) → floating
 *  5-tab dock drawn OVER the list, so the last row sits under it.
 * ==========================================================================*/

/** One canvas row on Agent home. Static sample copy from the reference. */
internal data class AgentHomeChat(
    val chatId: String,
    val title: String,
    val subtitle: String,
    val time: String,
)

/** Agent home's "Chats · 5" rows, in the reference's order. */
internal val agentHomeChats = listOf(
    AgentHomeChat("chat_code_reviewer", "Code Reviewer", "You are now friends", "13:07"),
    AgentHomeChat("chat_backend_architect", "Backend Architect", "You are now friends", "13:06"),
    AgentHomeChat("chat_devops_automator", "DevOps Automator", "You are now friends", "13:05"),
    AgentHomeChat("chat_api_tester", "API Tester", "You are now friends", "12:52"),
    AgentHomeChat("chat_frontend_developer", "Frontend Developer", "You are now friends", "12:40"),
)

/** The personal agent hero card copy, as in the reference. */
internal const val PERSONAL_AGENT_TITLE = "fareed's Personal Agent"
internal const val PERSONAL_AGENT_PREVIEW = "Delete ka option agent ki chat ke andar hai, \u2026"
internal const val PERSONAL_AGENT_DATE = "Wed"

/** The pinned official agent row. */
internal const val DUA_AGENT_TITLE = "Dua Agent"
internal const val DUA_AGENT_SUBTITLE = "I'm Dua Agent \u2014 the official agent of Dua Gen AI"
internal const val DUA_AGENT_TIME = "15:19"

/** Header brand text. */
internal const val APP_NAME = "Dua Gen AI"

/**
 * Agent home's header: 48dp circular user avatar (opens the drawer) · green
 * rounded-square app badge + bold "Dua Gen AI" · 48dp circular plus (New chat).
 * No Dua | Agent toggle here — the way back to Dua is the drawer's "Dua" row.
 */
@Composable
private fun AgentHomeTopBar(
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    userAvatar: Painter?,
    appBadge: Painter?,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.gutter, end = spacing.gutter, top = 20.dp, bottom = 10.dp),
    ) {
        // LEFT — the user's photo. Same drawer the Dua hamburger opens, so the
        // accessibility label stays "Open menu".
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(MinTouchTarget)
                .clip(CircleShape)
                .background(palette.chromeFill)
                .clickable(role = Role.Button, onClick = onOpenDrawer)
                .semantics { contentDescription = "Open menu" },
            contentAlignment = Alignment.Center,
        ) {
            if (userAvatar != null) {
                Image(
                    painter = userAvatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // CENTER — badge + app name.
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppBadge(painter = appBadge, size = 30.dp, shape = TeamilyShapes.xs)
            Spacer(Modifier.width(spacing.sm))
            Text(
                text = APP_NAME,
                style = TeamilyTypography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }

        // RIGHT — New chat: plusFill circle with a plusIcon glyph.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(MinTouchTarget)
                .clip(CircleShape)
                .background(palette.plusFill)
                .clickable(role = Role.Button, onClick = onNewChat)
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
    }
}

/**
 * The green app badge (bot-badge.png). The image is supplied by the app as a
 * [Painter] (this package has no `res/`); without one, a `logoMint` tile with
 * the kit's agent glyph stands in.
 */
@Composable
private fun AppBadge(painter: Painter?, size: Dp, shape: Shape) {
    val palette = teamilyPalette()
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(palette.logoMint),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AgentCubeGlyph(tint = palette.tileInk, modifier = Modifier.size(size * 0.6f))
        }
    }
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
    val palette = teamilyPalette()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(MinTouchTarget)
                .clip(CircleShape)
                .background(if (filterActive) colors.brandSoft else palette.chromeFill)
                .border(1.dp, palette.border, CircleShape)
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                listOf(18.dp, 12.dp, 6.dp).forEach { w ->
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
                .semantics {
                    contentDescription = "Search"
                    disabled()
                },
            shape = TeamilyShapes.capsule,
            color = palette.surfaceRecessed,
            border = BorderStroke(1.dp, palette.border),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = MinTouchTarget)
                    .padding(horizontal = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(spacing.sm))
                Text(
                    text = "Search",
                    style = TeamilyTypography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The ONE filled card on Agent home: the user's personal agent.
 *
 * `cardHeroColor` fill, 20dp corners, 1dp `borderGradient` edge, resting glass
 * shadow and the 0.98 press scale. Tapping it opens the existing Agent thread
 * ([onClick] = `onOpenAgentChat`).
 */
@Composable
private fun PersonalAgentHeroCard(
    onClick: () -> Unit,
    appBadge: Painter?,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .teamilyPressScale(interactionSource)
            .teamilyRestingShadow(palette, TeamilyShapes.card)
            .clip(TeamilyShapes.card)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = TeamilyShapes.card,
        color = palette.cardHeroColor,
        border = BorderStroke(1.dp, palette.borderGradient),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatAvatar(ring = palette.cardHeroColor, miniBadge = true, appBadge = appBadge) {
                AppBadge(painter = appBadge, size = 48.dp, shape = CircleShape)
            }
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = PERSONAL_AGENT_TITLE,
                        style = TeamilyTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(spacing.sm))
                    Text(
                        text = PERSONAL_AGENT_DATE,
                        style = TeamilyTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = PERSONAL_AGENT_PREVIEW,
                    style = TeamilyTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A 48dp circular avatar with an optional green mini-badge at its bottom-end.
 * The badge is ringed in [ring] (the surface behind it) so it reads as a badge.
 */
@Composable
private fun ChatAvatar(
    ring: Color,
    miniBadge: Boolean,
    appBadge: Painter?,
    content: @Composable () -> Unit,
) {
    Box(Modifier.size(52.dp)) {
        Box(Modifier.align(Alignment.TopStart).size(48.dp).clip(CircleShape)) { content() }
        if (miniBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ring)
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppBadge(painter = appBadge, size = 16.dp, shape = CircleShape)
            }
        }
    }
}

/** The pastel tile behind a chat avatar: the kit's tile colour over white. */
@Composable
private fun TileAvatar(tile: Color) {
    val palette = teamilyPalette()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .background(tile.copy(alpha = 0.38f)),
        contentAlignment = Alignment.Center,
    ) {
        AgentCubeGlyph(tint = palette.tileInk, modifier = Modifier.size(26.dp))
    }
}

/**
 * One canvas row — no card box: avatar · title (+ verified ✓) / 1-line
 * subtitle · right-aligned time. 16dp side padding, sits on the canvas.
 */
@Composable
private fun CanvasChatRow(
    title: String,
    subtitle: String,
    time: String,
    verified: Boolean = false,
    avatar: @Composable () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.gutter, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        avatar()
        Spacer(Modifier.width(spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = TeamilyTypography.bodyLarge.copy(fontSize = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (verified) {
                        Spacer(Modifier.width(6.dp))
                        VerifiedCheck()
                    }
                }
                Spacer(Modifier.width(spacing.sm))
                Text(
                    text = time,
                    style = TeamilyTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = subtitle,
                style = TeamilyTypography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The green verified check next to "Dua Agent". */
@Composable
private fun VerifiedCheck() {
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
) {
    val spacing = LocalDuaGenAISpacing.current
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .padding(horizontal = spacing.sm)
            .clip(TeamilyShapes.capsule)
            .clickable(role = Role.Button, onClick = onToggle)
            .sizeIn(minHeight = MinTouchTarget)
            .padding(horizontal = spacing.sm)
            .semantics(mergeDescendants = true) {
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(16.dp)) {
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
            style = TeamilyTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = ink,
        )
        Spacer(Modifier.width(spacing.xs))
        Canvas(Modifier.size(14.dp).rotate(if (expanded) 0f else -90f)) {
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
 * Agent home's scrolling body: hero card, Pinned section, divider, Chats
 * section. Bottom padding leaves room for the floating dock drawn over it.
 */
@Composable
private fun AgentHomeList(
    onOpenAgentChat: () -> Unit,
    appBadge: Painter?,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()
    var pinnedExpanded by rememberSaveable { mutableStateOf(true) }
    var chatsExpanded by rememberSaveable { mutableStateOf(true) }
    val tiles = listOf(palette.tileViolet, palette.tileCyan, palette.tileYellow, palette.tileMint, palette.tileCyan)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp),
    ) {
        item {
            PersonalAgentHeroCard(
                onClick = onOpenAgentChat,
                appBadge = appBadge,
                modifier = Modifier.padding(horizontal = spacing.sm),
            )
        }
        item {
            Column {
                Spacer(Modifier.height(2.dp))
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
                ) {
                    ChatAvatar(ring = palette.bgCanvas, miniBadge = false, appBadge = appBadge) {
                        AppBadge(painter = appBadge, size = 48.dp, shape = CircleShape)
                    }
                }
            }
        }
        item {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    thickness = 0.5.dp,
                    color = palette.border,
                )
                SectionHeader(
                    icon = SectionIcon.Chat,
                    label = "Chats \u00b7 ${agentHomeChats.size}",
                    expanded = chatsExpanded,
                    onToggle = { chatsExpanded = !chatsExpanded },
                )
            }
        }
        if (chatsExpanded) {
            itemsIndexed(agentHomeChats, key = { _, c -> c.chatId }) { index, chat ->
                CanvasChatRow(title = chat.title, subtitle = chat.subtitle, time = chat.time) {
                    ChatAvatar(ring = palette.bgCanvas, miniBadge = true, appBadge = appBadge) {
                        TileAvatar(tile = tiles[index % tiles.size])
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
 */
@Composable
private fun HomeBottomNav(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        shape = TeamilyShapes.capsule,
        color = palette.bottomnavBg,
        border = BorderStroke(1.dp, palette.borderGradient),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeTab.entries.forEach { tab ->
                val active = tab == selected
                val tint = if (active) palette.navSelectedContent
                else MaterialTheme.colorScheme.onSurface
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    // The capsule clip lives on a background layer only, so the
                    // Memory dot (which sits just outside the glyph) is not cut
                    // by the selected pill's rounded corner. The ripple is drawn
                    // in that clipped layer, so it keeps the capsule shape.
                    val interaction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .clickable(interactionSource = interaction, indication = null) { onSelect(tab) }
                            .sizeIn(minWidth = 64.dp, minHeight = 60.dp)
                            .semantics {
                                role = Role.Tab
                                // `this.` — the parameter `selected` would shadow it.
                                this.selected = active
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
                                .padding(horizontal = spacing.xs, vertical = spacing.xs),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box {
                                TabGlyph(tab = tab, tint = tint, strong = active)
                                if (tab == HomeTab.Memory) {
                                    // The reference's unread dot: #A2E4B6 fill (measured;
                                    // = dark --nv-green-700) in a --nv-notification-outline
                                    // ring. Measured at 3.2px/dp: 8.75dp fill, 2.2dp ring,
                                    // centred 1dp past the glyph's top-end corner.
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset(x = 18.5.dp, y = (-7.5).dp)
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
                                    fontSize = 13.sp,
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
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // `--nv-icon-stroke` 1.75; the active tab uses `--nv-icon-stroke-strong` 2.25.
        val width = if (strong) TeamilyStroke.iconStrong else TeamilyStroke.icon
        val stroke = Stroke(width = width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (tab) {
            // The Dua bot: square head with two eyes over a stem.
            HomeTab.Chats -> {
                val head = Path().apply {
                    moveTo(w * 0.22f, h * 0.14f)
                    lineTo(w * 0.78f, h * 0.14f)
                    quadraticBezierTo(w * 0.86f, h * 0.14f, w * 0.86f, h * 0.22f)
                    lineTo(w * 0.86f, h * 0.52f)
                    quadraticBezierTo(w * 0.86f, h * 0.60f, w * 0.78f, h * 0.60f)
                    lineTo(w * 0.62f, h * 0.60f)
                    lineTo(w * 0.62f, h * 0.88f)
                    lineTo(w * 0.42f, h * 0.88f)
                    lineTo(w * 0.42f, h * 0.60f)
                    lineTo(w * 0.22f, h * 0.60f)
                    quadraticBezierTo(w * 0.14f, h * 0.60f, w * 0.14f, h * 0.52f)
                    lineTo(w * 0.14f, h * 0.22f)
                    quadraticBezierTo(w * 0.14f, h * 0.14f, w * 0.22f, h * 0.14f)
                    close()
                }
                drawPath(head, color = tint, style = stroke)
                drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.40f, h * 0.37f))
                drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.62f, h * 0.37f))
            }

            // A robot: antenna, rounded head, two eyes, side ears.
            HomeTab.Managed -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.30f), stroke.width, StrokeCap.Round)
                val head = Path().apply {
                    moveTo(w * 0.32f, h * 0.30f)
                    lineTo(w * 0.68f, h * 0.30f)
                    quadraticBezierTo(w * 0.80f, h * 0.30f, w * 0.80f, h * 0.42f)
                    lineTo(w * 0.80f, h * 0.70f)
                    quadraticBezierTo(w * 0.80f, h * 0.82f, w * 0.68f, h * 0.82f)
                    lineTo(w * 0.32f, h * 0.82f)
                    quadraticBezierTo(w * 0.20f, h * 0.82f, w * 0.20f, h * 0.70f)
                    lineTo(w * 0.20f, h * 0.42f)
                    quadraticBezierTo(w * 0.20f, h * 0.30f, w * 0.32f, h * 0.30f)
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
@Composable
private fun HomeDuaLight() = DuaGenAITheme(darkTheme = false) { HomeScreen() }

@Preview(name = "Home Dua — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeDuaDark() = DuaGenAITheme(darkTheme = true) { HomeScreen() }

@Preview(name = "Home Agent — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeAgentLight() = DuaGenAITheme(darkTheme = false) {
    HomeScreen(mode = HomeMode.Agent)
}

@Preview(name = "Home Agent — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeAgentDark() = DuaGenAITheme(darkTheme = true) {
    HomeScreen(mode = HomeMode.Agent)
}
