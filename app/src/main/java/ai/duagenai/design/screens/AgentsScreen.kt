package ai.duagenai.design.screens

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

/** One agent the user manages. Mirrors Home's Agent-mode row. */
private data class ManagedAgent(val name: String, val role: String, val status: String)

/** The sample roster. Same three agents Agent's own thread reports on. */
private val managedAgents = listOf(
    ManagedAgent("Slides Assistant", "Builds and exports decks", "Working"),
    ManagedAgent("Market Research", "Cites every claim", "Working"),
    ManagedAgent("Accounting Assistant", "Reconciles invoices", "Waiting on you"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(modifier: Modifier = Modifier) {
    val spacing = LocalDuaGenAISpacing.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = spacing.gutter, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = "MANAGED",
                style = DuaGenAITextStyles.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            managedAgents.forEach { agent ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.xs),
                ) {
                    Text(
                        text = agent.name,
                        style = DuaGenAITextStyles.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${agent.role} \u00b7 ${agent.status}",
                        style = DuaGenAITextStyles.snippet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
