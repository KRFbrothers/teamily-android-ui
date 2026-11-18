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
 *  user manages.
 *
 *  It is deliberately data-free: the kit has no agent backend, so rather than
 *  rendering a fake roster of agents that are "Working" or "Waiting on you",
 *  the screen says honestly that there are no agents yet. Real agents appear
 *  here when a backend provides them.
 * ==========================================================================*/

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
        }
    }
}

@Preview(name = "Agents \u2014 light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AgentsLight() = DuaGenAITheme(darkTheme = false) { AgentsScreen() }

@Preview(name = "Agents \u2014 dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AgentsDark() = DuaGenAITheme(darkTheme = true) { AgentsScreen() }
