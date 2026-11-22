package ai.duagenai.design.screens

import ai.duagenai.design.theme.teamilyPalette
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import ai.duagenai.design.theme.DemoBadge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

/** One suggested starting point, with its leading glyph. */
private data class DiscoverCard(val title: String, val subtitle: String, val icon: ImageVector)

private val discoverCards = listOf(
    DiscoverCard("Draft a landing page", "Dark hero, one CTA, mobile-first", Icons.Outlined.Web),
    DiscoverCard("Build a 12-slide deck", "Brand theme, exported to your workspace", Icons.Outlined.Slideshow),
    DiscoverCard("Reconcile this month", "Match invoices against the ledger", Icons.AutoMirrored.Outlined.ReceiptLong),
    DiscoverCard("Summarise a long PDF", "Cited, with the pages quoted", Icons.Outlined.Description),
)

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    /** The app's bottom dock. The nav host passes the shared [HomeBottomNav]. */
    bottomBar: @Composable () -> Unit = {},
    /**
     * Starts a NEW thread from a card on the shared chat route. The nav host
     * gives it a fresh id; the card title is the title and first message.
     */
    onStartChat: (prompt: String) -> Unit = {},
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
                    text = "START WITH",
                    style = DuaGenAITextStyles.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Sample cards, not a live catalogue yet.
                Spacer(Modifier.width(spacing.xs))
                DemoBadge()
            }
            discoverCards.forEach { card ->
                TeamilyListCard(
                    title = card.title,
                    subtitle = card.subtitle,
                    onClick = { onStartChat(card.title) },
                    avatar = {
                        val palette = teamilyPalette()
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(DuaGenAITheme.shapes.avatarList)
                                .background(palette.primarySoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                card.icon,
                                contentDescription = null,
                                tint = palette.primaryInk,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    },
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
