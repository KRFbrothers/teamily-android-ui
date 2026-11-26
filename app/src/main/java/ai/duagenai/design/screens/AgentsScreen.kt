package ai.duagenai.design.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ai.duagenai.design.theme.PandaMark
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
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
 *  user manages.
 *
 *  It is deliberately data-free: the kit has no agent backend, so rather than
 *  rendering a fake roster of agents that are "Working" or "Waiting on you",
 *  the screen says honestly that there are no agents yet. Real agents appear
 *  here when a backend provides them.
 * ==========================================================================*/

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

@Composable
fun AgentsScreen(
    modifier: Modifier = Modifier,
    agents: List<AgentItem> = emptyList(),
    onCreateAgent: () -> Unit = {},
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
                actions = {
                    IconButton(onClick = onCreateAgent) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "New Agent",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
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
                Text(
                    text = "MANAGED",
                    style = DuaGenAITextStyles.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (agents.isEmpty()) {
                    Text(
                        text = "No agents yet",
                        style = DuaGenAITextStyles.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Agents you add will show up here.",
                        style = DuaGenAITextStyles.snippet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                    ) {
                        items(agents.size, key = { agents[it].id }) { idx ->
                            val agent = agents[idx]
                            TeamilyListCard(
                                title = agent.name,
                                subtitle = agent.description.ifBlank { "Mode: ${agent.mode}" },
                                onClick = { onOpenChat(agent.id, agent.name) },
                                avatar = {
                                    PandaMark(
                                        size = 40.dp,
                                        contentDescription = agent.name,
                                    )
                                },
                            )
                        }
                    }
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
