package ai.duagenai.design.screens

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import ai.duagenai.design.theme.DemoBadge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import ai.duagenai.design.theme.MaxContentWidth
import ai.duagenai.design.theme.TeamilyShapes
import ai.duagenai.design.theme.teamilyPalette
import ai.duagenai.design.theme.teamilyRestingShadow
import ai.duagenai.design.cube.CubeAvatar
import ai.duagenai.design.cube.CubeCatalog
import ai.duagenai.design.cube.CubeIds
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow

/* ============================================================================
 *  Agents — the "Agents" dock tab's destination.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  `DuaGenAINavHost` wired the Agents tab to `composable(AGENTS) { AgentsScreen() }`,
 *  but `AgentsScreen` was never shipped, so the nav graph could not compile and
 *  the tab had nowhere to land. This is that destination: the list of agents the
 *  user manages, drawn from the same product model Home's Agent mode lists.
 *
 *  It is deliberately read-only and data-free: the kit has no agent backend, so
 *  this screen renders the shipped sample agents and says so rather than
 *  pretending to fetch or to control anything.
 * ==========================================================================*/

/**
 * One agent the user manages. Mirrors Home's Agent-mode row. [chatId] is the
 * SAME stable id the drawer / Agent home use for this agent's thread, so the
 * card opens that thread and shows that thread's cube.
 */
private data class ManagedAgent(
    val chatId: String,
    val name: String,
    val role: String,
    val status: String,
)

/** The sample roster. Same three agents Agent's own thread reports on. */
private val managedAgents = listOf(
    ManagedAgent("chat_slides_assistant", "Slides Assistant", "Builds and exports decks", "Working"),
    ManagedAgent("chat_market_research", "Market Research", "Cites every claim", "Working"),
    ManagedAgent("chat_accounting_assistant", "Accounting Assistant", "Reconciles invoices", "Waiting on you"),
)

/**
 * The Agents / Discover list card: `TeamilyShapes.card` on `bgSurface`, a 1dp
 * `border` edge and the resting glass shadow, with a leading [avatar], a title
 * and one subtitle line. The whole card is one 48dp+ button for TalkBack.
 */
@Composable
internal fun TeamilyListCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    avatar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = teamilyPalette()
    val spacing = LocalDuaGenAISpacing.current
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .teamilyRestingShadow(palette, TeamilyShapes.card),
        shape = TeamilyShapes.card,
        color = palette.bgSurface,
        border = BorderStroke(1.dp, palette.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            avatar()
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = DuaGenAITextStyles.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = DuaGenAITextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(
    modifier: Modifier = Modifier,
    /** The app's bottom dock. The nav host passes the shared [HomeBottomNav]. */
    bottomBar: @Composable () -> Unit = {},
    /** Opens the agent's thread on the shared `chat/{chatId}?title=` route. */
    onOpenChat: (chatId: String, title: String) -> Unit = { _, _ -> },
) {
    val spacing = LocalDuaGenAISpacing.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Agents",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.TopCenter,
        ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxSize()
                .padding(horizontal = spacing.gutter, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MANAGED",
                    style = DuaGenAITextStyles.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Sample agents, not the user's live agents yet.
                Spacer(Modifier.width(spacing.xs))
                DemoBadge()
            }
            managedAgents.forEach { agent ->
                val cubeId = CubeCatalog.forEntity(agent.chatId)
                TeamilyListCard(
                    title = agent.name,
                    subtitle = "${agent.role} \u00b7 ${agent.status}",
                    onClick = { onOpenChat(agent.chatId, agent.name) },
                    avatar = {
                        CubeAvatar(
                            cubeId = cubeId,
                            size = 40.dp,
                            shape = DuaGenAITheme.shapes.avatarList,
                            contentDescription = "${agent.name} avatar, ${CubeIds.label(cubeId)}",
                        )
                    },
                )
            }
        }
        }
    }
}

@Preview(name = "Agents \u2014 light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AgentsLight() = DuaGenAITheme(darkTheme = false) { AgentsScreen() }

@Preview(name = "Agents \u2014 dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AgentsDark() = DuaGenAITheme(darkTheme = true) { AgentsScreen() }
