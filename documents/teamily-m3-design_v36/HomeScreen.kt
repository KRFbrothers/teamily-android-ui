package ai.duagenai.design.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
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
import ai.duagenai.design.cube.CubeCatalog
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing

/* ============================================================================
 *  Home — the two modes, per the confirmed product model
 *
 *  Dua Gen AI is one package with two halves:
 *
 *    DUA   = a chatbot. Simple chat, Q&A, no agent work.
 *    ORHAN = an Agent. Multitasks, controls other agents, can create new agents.
 *
 *  That model decides what each home surface may contain, and the two
 *  references agree with it:
 *
 *    DUA MODE (reference 1) — deliberately PLAIN
 *      • top bar: circular white hamburger · Dua|Orhan toggle · circular white gear
 *      • body: EMPTY. Dua is a chatbot, not an agent host, so there is no chat
 *        list, no search row, no "CHATS" header and no bottom navigation here.
 *        The reference's body is empty and so is this one — nothing is invented
 *        to fill it.
 *      • bottom: one collapsed "Ask Dua ..." pill (plus · placeholder · mic ·
 *        green voice button).
 *      • NO agent-related entry point of any kind. Not a row, not a badge, not
 *        a tab. Dua does not manage agents.
 *
 *    ORHAN MODE (reference 2) — the agent surface
 *      • top bar: circular white hamburger · toggle (Orhan selected) · circular
 *        white plus
 *      • search row: circular filter button + "Search" pill
 *      • PINNED ORHAN CARD: Orhan itself, directly under the search row and
 *        OUTSIDE the scrolling list, so it stays put while the agents below
 *        scroll. Orhan is an agent, so it has to be visible on its own surface
 *        — the reference draws it as a brand-tinted card with a white disc
 *        holding the wireframe cube glyph, its name, a one-line role and a
 *        trailing timestamp.
 *      • agent list: the agents Orhan CONTROLS — each with its own cube, its
 *        status and a "Start a chat" entry point, under an "AGENTS · N" header.
 *        Orhan is an agent that controls other agents, so its home is the agents
 *        it manages.
 *      • bottom: floating 5-item navigation pill. Its second item is "Managed",
 *        not "Agents": Orhan is itself an agent, so a tab called "Agents" would
 *        be ambiguous — the tab lists the agents Orhan controls, and its glyph
 *        is a hierarchy (one node over two) rather than a single robot face.
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
 *      only in the drawer footer / Profile. Home's top bar carries a gear or a
 *      plus, not an avatar, in this reference set.
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
    Orhan("Orhan"),
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
 * [Managed] is the reference's second item, relabelled for the product model:
 * Orhan is itself an agent, so the tab that lists the agents it controls is
 * called "Managed" and carries a hierarchy glyph. It opens the same
 * `AgentsScreen` the kit already ships.
 *
 * `Memory` and `Artifacts` each have their own placeholder screen
 * (`MemoryScreen` / `ArtifactsScreen`), so every tab in this enum now has a
 * destination the host can navigate to.
 */
enum class HomeTab(val label: String) {
    Chats("Chats"),
    Managed("Managed"),
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
    /** The top bar's right button in Orhan mode (the plus) — starts a new chat. */
    onNewChat: () -> Unit = {},
    /** Bottom navigation. Only Chats / Managed / Discover have screens. */
    onSelectTab: (HomeTab) -> Unit = {},
    /**
     * Which bottom-nav item is highlighted. HOISTED, not local: the host owns
     * the selection so it survives a Home recomposition and a configuration
     * change, exactly like [mode]. Defaults to [HomeTab.Chats] — the tab Orhan's
     * home has always shown as active.
     */
    selectedTab: HomeTab = HomeTab.Chats,
    /**
     * Opens ORHAN'S OWN thread. The pinned Orhan card is the entry point: Orhan
     * is an agent, so it has a conversation of its own, and tapping its card
     * opens it through the same `chat/{chatId}` route every other thread uses.
     */
    onOpenOrhanChat: () -> Unit = {},
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
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.fillMaxSize()) {

            /* ---------- top bar: hamburger · toggle · gear/plus ---------- */
            HomeTopBar(
                mode = mode,
                onOpenDrawer = onOpenDrawer,
                onModeChange = onModeChange,
                onOpenSettings = onOpenSettings,
                onNewChat = onNewChat,
            )

            if (mode == HomeMode.Dua) {
                /* ================= DUA MODE =================
                 * Dua is a chatbot. Its home is the conversation itself, so the
                 * body is empty — exactly as the reference draws it. No chat
                 * list, no search row, no section header, no bottom navigation,
                 * and nothing agent-related: Dua does not manage agents. The
                 * only control is the composer at the bottom. */
                Spacer(Modifier.weight(1f).fillMaxWidth())
                DuaComposer()
            } else {
                /* ================= ORHAN MODE =================
                 * Orhan is an agent that controls other agents, so its home is
                 * the agents it manages — each with its own cube, its status and
                 * a "Start a chat" entry point. Search row on top, then ORHAN
                 * ITSELF pinned (outside the scroller), then the agent list under
                 * an "AGENTS · N" header, then the floating navigation pill. No
                 * composer. */
                SearchRow(
                    filterActive = filterActive,
                    onToggleFilter = { filterActive = !filterActive },
                    modifier = Modifier.padding(
                        start = LocalDuaGenAISpacing.current.gutter,
                        end = LocalDuaGenAISpacing.current.gutter,
                        top = LocalDuaGenAISpacing.current.sm,
                        bottom = LocalDuaGenAISpacing.current.sm,
                    ),
                )

                // Orhan itself, pinned. It sits in the Column — NOT inside the
                // LazyColumn below — so scrolling the managed agents never moves
                // it. This is the whole point of the reference's layout.
                PinnedOrhanCard(
                    onClick = onOpenOrhanChat,
                    modifier = Modifier.padding(
                        start = LocalDuaGenAISpacing.current.gutter,
                        end = LocalDuaGenAISpacing.current.gutter,
                        bottom = LocalDuaGenAISpacing.current.sm,
                    ),
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = LocalDuaGenAISpacing.current.gutter,
                        end = LocalDuaGenAISpacing.current.gutter,
                        bottom = LocalDuaGenAISpacing.current.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(LocalDuaGenAISpacing.current.sm),
                ) {
                    item {
                        AgentsHeader(count = sampleAgents.size)
                    }
                    items(sampleAgents) { agent ->
                        // The SAME row composable AgentsScreen renders — one
                        // implementation, so the two surfaces cannot drift.
                        AgentRow(agent = agent)
                    }
                }

                HomeBottomNav(
                    selected = selectedTab,
                    onSelect = onSelectTab,
                )
            }
        }
    }
}

/**
 * The top bar.
 *
 * Three elements, vertically centred: a circular white button, the Dua|Orhan
 * toggle, and a second circular white button. The two side buttons are 44dp
 * circles with a soft shadow — the reference's "floating chip" look — and the
 * toggle takes the remaining width between them.
 *
 * The right button changes with the mode, as the references show: a gear in Dua
 * mode, a plus in Orhan mode. The hamburger's line count differs between the two
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

        CircleIconButton(
            onClick = if (mode == HomeMode.Dua) onOpenSettings else onNewChat,
            contentDescription = if (mode == HomeMode.Dua) "Settings" else "New chat",
        ) {
            Icon(
                if (mode == HomeMode.Dua) Icons.Filled.Settings else Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
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
        color = if (active) colors.brandSoft else MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        content = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                content()
            }
        },
    )
}

/**
 * The Dua | Orhan segmented control.
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
                    // "Dua" / "Orhan", which also appears in the drawer's nav
                    // list; this makes the control unambiguous to a screen
                    // reader ("Orhan mode, selected") and to a test.
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
 * Dua mode's composer: one collapsed pill.
 *
 * plus · "Ask Dua ..." · mic · green voice button. The reference's voice button
 * is a light mint circle with three green bars; here it is the kit's brand
 * gradient with the same three-bar waveform, because the gradient is this app's
 * established "primary action" fill and the mockup's purple is not ours.
 *
 * `internal`, not `private`, and with a `placeholder` parameter, because
 * ChatScreen draws the SAME composer under Orhan's thread. There is exactly one
 * composer implementation in this package: Dua's home and Orhan's thread both
 * call this function, so the two can never drift apart visually. Dua's call site
 * passes no argument, so its rendering is unchanged.
 */
@Composable
internal fun DuaComposer(
    placeholder: String = "Ask Dua ...",
    // The three optional parameters below are what make the SAME pill usable as
    // a real composer. Dua's call site passes none of them, so its rendering is
    // byte-identical to before: a static hint, a mic and the voice button.
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    onSend: (() -> Unit)? = null,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    // Editable only when a caller supplies a change handler. This is the single
    // switch between "Dua's presentational pill" and "a thread's live composer".
    val editable = onValueChange != null
    val canSend = editable && !value.isNullOrBlank()

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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = DuaGenAITheme.shapes.composer,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, colors.fieldEdge),
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 52.dp)
                    .padding(start = spacing.md, end = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add attachment",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Audit fix #9: attachments are not wired in this kit, so the
                    // glyph must not announce itself as an available action. It
                    // carries no click modifier (no ripple) and publishes
                    // `disabled()` so a screen reader says it is unavailable.
                    modifier = Modifier
                        .size(20.dp)
                        .semantics { disabled() },
                )
                Spacer(Modifier.width(spacing.sm))
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
                            onSend = { if (canSend) onSend?.invoke() },
                        ),
                        decorationBox = { innerTextField ->
                            if (value.isNullOrEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = DuaGenAITextStyles.bubble,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        modifier = Modifier.weight(1f),
                    )
                }
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = "Voice input",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Audit fix #9: same guard as the attachment glyph above —
                    // voice input is not wired, so it is announced as disabled
                    // rather than as a control that does nothing.
                    modifier = Modifier
                        .size(20.dp)
                        .semantics { disabled() },
                )
                Spacer(Modifier.width(spacing.sm))
                VoiceActionButton()
                if (editable) {
                    Spacer(Modifier.width(spacing.xs))
                    SendButton(enabled = canSend, onClick = { onSend?.invoke() })
                }
            }
        }
    }
}

/**
 * The send control, drawn only in an editable composer.
 *
 * It reuses the voice button's own geometry (a 36dp circle) and the kit's brand
 * gradient for its enabled state, so it is the same visual language rather than
 * a new one. Disabled it drops to the surface variant with the muted ink \u2014 no
 * new colour is introduced.
 */
@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = DuaGenAITheme.colors
    Box(
        modifier = Modifier
            // Audit fix #10: the disc is drawn at 36dp, but the touch area is
            // raised to the M3 48dp minimum. `minTouchTarget()` comes FIRST so
            // the 48dp box is the outer one and the 36dp disc stays centred
            // inside it — the composer's look is unchanged.
            .minTouchTarget()
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (enabled) colors.primaryGradient
                else SolidColor(MaterialTheme.colorScheme.surfaceVariant),
            )
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .semantics {
                contentDescription = "Send message"
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = if (enabled) colors.onGradient
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** The circular voice action — brand gradient, never the mockup's purple. */
@Composable
private fun VoiceActionButton() {
    val colors = DuaGenAITheme.colors
    Box(
        modifier = Modifier
            // Audit fix #10: 40dp drawn, 48dp tappable — same rule as the send
            // disc above, so the two composer controls match.
            .minTouchTarget()
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.primaryGradient)
            // Audit fix #9: voice capture is not wired in this kit, so the disc
            // is announced as disabled instead of looking like a live action.
            .semantics {
                contentDescription = "Voice input"
                disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        // A three-bar waveform, drawn as shapes so no icon asset is needed and
        // the button stays legible at 40dp.
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(8.dp, 14.dp, 8.dp).forEach { h ->
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(h)
                        .clip(CircleShape)
                        .background(colors.onGradient),
                )
            }
        }
    }
}

/**
 * Orhan mode's search row: a circular filter button and a "Search" pill.
 *
 * FILTER (audit fix #7) — the button used to be `onClick = {}`, i.e. it looked
 * pressable and did nothing. It is now a real toggle: [filterActive] is the
 * state, [onToggleFilter] flips it, and the active state is drawn with the kit's
 * own `brandSoft` / `onBrandSoft` pair — the same tokens the drawer's active row
 * and the pinned card use. No new colour, no new component.
 *
 * SEARCH (audit fix #9) — the pill is a presentational placeholder: this kit has
 * no search index. It carries no click modifier (so it never ripples), and it
 * publishes `disabled()` semantics so a screen reader announces it as
 * unavailable instead of leaving the user tapping a dead control.
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
        CircleIconButton(
            onClick = onToggleFilter,
            contentDescription = "Filter",
            active = filterActive,
        ) {
            // Three staggered bars — the reference's filter/sliders glyph. The
            // bars take the active ink when the filter is on, so the toggle's
            // state is visible and not only announced.
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                listOf(14.dp, 10.dp, 12.dp).forEach { w ->
                    Box(
                        modifier = Modifier
                            .width(w)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(
                                if (filterActive) colors.onBrandSoft
                                else MaterialTheme.colorScheme.onSurface,
                            ),
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
            shape = DuaGenAITheme.shapes.pill,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, colors.fieldEdge),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 44.dp)
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
                    style = DuaGenAITextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Orhan itself, pinned under the search row.
 *
 * The reference draws Orhan as a brand-tinted card: a white disc holding the
 * wireframe cube glyph, the name, a one-line role and a trailing timestamp.
 *
 * Two deliberate choices:
 *  • The glyph is [OrhanCubeGlyph] — the SAME 1.5dp wireframe the drawer's
 *    "Orhan" nav item draws, made `internal` for this reuse. It is NOT a
 *    [CubeAvatar]: the 42 cube assets are solid charcoal/orange fills and this
 *    is a stroke-only outline, so drawing it as a cube avatar would be wrong.
 *  • The card is tappable: it opens ORHAN'S OWN thread through the same
 *    `chat/{chatId}` route every other conversation uses. Orhan is an agent, so
 *    it has a conversation of its own — the card is its entry point.
 */
@Composable
private fun PinnedOrhanCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = DuaGenAITheme.shapes.card,
        color = colors.brandSoft,
        border = BorderStroke(1.dp, colors.onBrandSoft.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // White disc + wireframe glyph — the reference's pinned avatar.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                OrhanCubeGlyph(
                    tint = colors.onBrandSoft,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Orhan",
                    style = DuaGenAITextStyles.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    // Orhan's role, straight from the confirmed product model.
                    // Two lines so the whole role stays readable inside the card
                    // instead of being clipped to a single truncated line.
                    text = "Multitasks, controls agents, creates new ones",
                    style = DuaGenAITextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(spacing.sm))
            Text(
                text = "Fri",
                style = DuaGenAITextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "AGENTS · N" with the reference's trailing chevron — Orhan's managed agents. */
@Composable
private fun AgentsHeader(count: Int) {
    val spacing = LocalDuaGenAISpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.sm, bottom = spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "AGENTS · $count",
            style = DuaGenAITextStyles.eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(spacing.xs))
        // A small downward chevron, drawn as two strokes.
        Box(modifier = Modifier.size(10.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val p = Path().apply {
                    moveTo(w * 0.2f, h * 0.38f)
                    lineTo(w * 0.5f, h * 0.66f)
                    lineTo(w * 0.8f, h * 0.38f)
                }
                drawPath(
                    p,
                    // The SAME token the "AGENTS · N" label above uses, so the
                    // chevron tracks the theme in both light and dark mode. A
                    // hardcoded colour here failed contrast on the dark canvas.
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

/**
 * The floating bottom navigation pill.
 *
 * Five items in the reference's order. Every item now navigates: Chats stays on
 * Home, Managed opens Agents, Discover opens Discover, and Memory / Artifacts
 * open their own placeholder screens.
 *
 * Each item carries a real glyph rather than a placeholder dot, because the
 * second item's meaning had to change: it is "Managed" — the agents Orhan
 * controls — and a hierarchy glyph (one node over two) says that, where a single
 * robot face would have said "agents in general".
 */
@Composable
private fun HomeBottomNav(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.gutter,
                end = spacing.gutter,
                top = spacing.sm,
                bottom = spacing.md,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = DuaGenAITheme.shapes.pill,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeTab.entries.forEach { tab ->
                    val active = tab == selected
                    val tint = if (active) colors.onBrandSoft
                    else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier
                            .clip(DuaGenAITheme.shapes.pill)
                            .clickable { onSelect(tab) }
                            .sizeIn(minWidth = 56.dp, minHeight = 48.dp)
                            .padding(horizontal = spacing.xs, vertical = spacing.xs)
                            // The active tab is announced as selected, so a
                            // screen reader and a test both read the SAME
                            // hoisted state the tint is drawn from.
                            .semantics {
                                role = Role.Tab
                                selected = active
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        TabGlyph(tab = tab, tint = tint)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            style = DuaGenAITextStyles.navLabel,
                            color = tint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One 18dp glyph per tab, drawn with [Canvas].
 *
 * Drawn rather than imported because the kit's icon set is `material-icons-core`
 * only — it has no chat bubble, compass, memory or artifacts glyph — and adding
 * `material-icons-extended` for five shapes would pull a multi-megabyte
 * dependency into the app. These are the same five shapes the preview page
 * draws, so the two cannot disagree.
 */
@Composable
private fun TabGlyph(tab: HomeTab, tint: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)

        when (tab) {
            // A speech bubble with a tail.
            HomeTab.Chats -> {
                val p = Path().apply {
                    moveTo(w * 0.16f, h * 0.20f)
                    lineTo(w * 0.84f, h * 0.20f)
                    lineTo(w * 0.84f, h * 0.66f)
                    lineTo(w * 0.46f, h * 0.66f)
                    lineTo(w * 0.28f, h * 0.84f)
                    lineTo(w * 0.28f, h * 0.66f)
                    lineTo(w * 0.16f, h * 0.66f)
                    close()
                }
                drawPath(p, color = tint, style = stroke)
            }

            // Hierarchy: one node above, two below, joined — Orhan over the
            // agents it controls.
            HomeTab.Managed -> {
                drawCircle(tint, radius = w * 0.11f, center = Offset(w * 0.5f, h * 0.22f), style = stroke)
                drawCircle(tint, radius = w * 0.11f, center = Offset(w * 0.24f, h * 0.78f), style = stroke)
                drawCircle(tint, radius = w * 0.11f, center = Offset(w * 0.76f, h * 0.78f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.33f), Offset(w * 0.5f, h * 0.55f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.24f, h * 0.67f), Offset(w * 0.24f, h * 0.55f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.76f, h * 0.67f), Offset(w * 0.76f, h * 0.55f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.24f, h * 0.55f), Offset(w * 0.76f, h * 0.55f), stroke.width, StrokeCap.Round)
            }

            // A compass: ring plus needle.
            HomeTab.Discover -> {
                drawCircle(tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                val p = Path().apply {
                    moveTo(w * 0.62f, h * 0.38f)
                    lineTo(w * 0.54f, h * 0.58f)
                    lineTo(w * 0.38f, h * 0.62f)
                    lineTo(w * 0.46f, h * 0.42f)
                    close()
                }
                drawPath(p, color = tint, style = stroke)
            }

            // A head over a shoulder arc.
            HomeTab.Memory -> {
                drawCircle(tint, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.34f), style = stroke)
                val p = Path().apply {
                    moveTo(w * 0.20f, h * 0.84f)
                    cubicTo(w * 0.20f, h * 0.62f, w * 0.80f, h * 0.62f, w * 0.80f, h * 0.84f)
                }
                drawPath(p, color = tint, style = stroke)
            }

            // Two overlapping frames.
            HomeTab.Artifacts -> {
                val back = Path().apply {
                    moveTo(w * 0.16f, h * 0.16f)
                    lineTo(w * 0.60f, h * 0.16f)
                    lineTo(w * 0.60f, h * 0.60f)
                    lineTo(w * 0.16f, h * 0.60f)
                    close()
                }
                drawPath(back, color = tint, style = stroke)
                val front = Path().apply {
                    moveTo(w * 0.40f, h * 0.40f)
                    lineTo(w * 0.84f, h * 0.40f)
                    lineTo(w * 0.84f, h * 0.84f)
                    lineTo(w * 0.40f, h * 0.84f)
                    close()
                }
                drawPath(front, color = tint, style = stroke)
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

@Preview(name = "Home Orhan — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeOrhanLight() = DuaGenAITheme(darkTheme = false) {
    HomeScreen(mode = HomeMode.Orhan)
}

@Preview(name = "Home Orhan — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeOrhanDark() = DuaGenAITheme(darkTheme = true) {
    HomeScreen(mode = HomeMode.Orhan)
}
