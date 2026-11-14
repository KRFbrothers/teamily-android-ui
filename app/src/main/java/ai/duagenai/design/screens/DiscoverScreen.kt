package ai.duagenai.design.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
 *  Discover — the "Discover" dock tab's destination.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  The nav graph registered `composable(DISCOVER) { DiscoverScreen() }`, but the
 *  composable was never shipped, so the graph could not compile and the tab led
 *  nowhere. This is that destination.
 *
 *  Like [AgentsScreen] it is honest about being a design surface: the cards are
 *  the product model's suggested starting points, and there is nothing to fetch.
 * ==========================================================================*/

/** One suggested starting point. */
private data class DiscoverCard(val title: String, val subtitle: String)

private val discoverCards = listOf(
    DiscoverCard("Draft a landing page", "Dark hero, one CTA, mobile-first"),
    DiscoverCard("Build a 12-slide deck", "Brand theme, exported to your workspace"),
    DiscoverCard("Reconcile this month", "Match invoices against the ledger"),
    DiscoverCard("Summarise a long PDF", "Cited, with the pages quoted"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(modifier: Modifier = Modifier) {
    val spacing = LocalDuaGenAISpacing.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Discover",
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
                text = "START WITH",
                style = DuaGenAITextStyles.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            discoverCards.forEach { card ->
                Column(modifier = Modifier.padding(vertical = spacing.xs)) {
                    Text(
                        text = card.title,
                        style = DuaGenAITextStyles.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = card.subtitle,
                        style = DuaGenAITextStyles.snippet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(name = "Discover \u2014 light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscoverLight() = DuaGenAITheme(darkTheme = false) { DiscoverScreen() }

@Preview(name = "Discover \u2014 dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscoverDark() = DuaGenAITheme(darkTheme = true) { DiscoverScreen() }
