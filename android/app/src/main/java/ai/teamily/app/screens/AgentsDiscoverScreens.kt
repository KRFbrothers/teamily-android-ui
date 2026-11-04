package ai.teamily.app.screens

import ai.teamily.app.Sample
import ai.teamily.app.ui.Avatar
import ai.teamily.app.ui.Card
import ai.teamily.app.ui.Gutter
import ai.teamily.app.ui.Pill
import ai.teamily.app.ui.PrimaryButton
import ai.teamily.app.ui.ScreenTitleBar
import ai.teamily.app.ui.SectionLabel
import ai.teamily.app.ui.T
import ai.teamily.app.ui.tc
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun AgentsScreen(onOpenChat: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenTitleBar("Your agents") {
            Text("${Sample.agents.count { it.working }} active", style = T.caption, color = tc.ink3)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Sample.agents, key = { it.id }) { a ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(a.avatar)
                            Spacer(Modifier.width(12.dp))
                            Text(a.name, style = T.rowTitle, color = tc.ink)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(a.description, style = T.body, color = tc.ink2, maxLines = 2)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (a.working) Pill("Working", container = tc.primarySoft, content = tc.primaryInk)
                            else Pill("Idle", container = tc.surface2, content = tc.ink3)
                            Pill(a.category, container = tc.surface2, content = tc.ink2)
                            Spacer(Modifier.weight(1f))
                            Text("Open", style = T.rowTitle, color = tc.primaryInk,
                                modifier = Modifier.clickable { onOpenChat(a.id) }.padding(8.dp))
                        }
                    }
                }
            }
            item {
                val shape = RoundedCornerShape(16.dp)
                Box(
                    Modifier.fillMaxWidth().height(72.dp).clip(shape).background(tc.surface2).border(1.5.dp, tc.line, shape),
                    Alignment.Center,
                ) { Text("+ Create a new agent", style = T.rowTitle, color = tc.primaryInk) }
            }
        }
    }
}

@Composable
fun DiscoverScreen(onOpenChat: (String) -> Unit) {
    val trending = listOf(
        Triple("slides", "Productivity", "Deck in 5 prompts · 2.1k users"),
        Triple("market", "Research", "Cited briefs · 1.4k users"),
        Triple("accounting", "Finance", "Invoice triage · 980 users"),
    )
    Column(Modifier.fillMaxSize()) {
        ScreenTitleBar("Discover")
        LazyColumn(Modifier.weight(1f)) {
            item {
                val shape = RoundedCornerShape(32.dp)
                Column(
                    Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(shape).background(tc.primarySoft).padding(20.dp),
                ) {
                    Text("FEATURED AGENT", style = T.overline, color = tc.primaryInk)
                    Spacer(Modifier.height(6.dp))
                    Text("Webpage Developer", style = T.title, color = tc.ink)
                    Text("Prompt a full landing page and get a published link in under two minutes.", style = T.body, color = tc.ink2)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Start a chat", onClick = { onOpenChat("webpage") })
                }
            }
            item { SectionLabel("Trending in community") }
            items(trending) { (id, category, meta) ->
                val agent = Sample.agents.first { it.id == id }
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenChat(id) }.padding(horizontal = Gutter, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(agent.avatar, size = 44.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(agent.name, style = T.rowTitle, color = tc.ink)
                        Text(meta, style = T.body, color = tc.ink3)
                    }
                    Pill(category, container = tc.surface2, content = tc.ink2)
                }
            }
            item { SectionLabel("From people you follow") }
            item {
                Row(Modifier.padding(horizontal = Gutter, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                    Avatar(ai.teamily.app.R.drawable.user_alice, size = 44.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Row {
                            Text("Alice Chen", style = T.rowTitle, color = tc.ink, modifier = Modifier.weight(1f))
                            Text("12 minutes ago", style = T.caption, color = tc.ink3)
                        }
                        Text("shared Webpage Developer with the Design Team", style = T.body, color = tc.ink2)
                        Text("· 3 replies", style = T.caption, color = tc.ink3)
                    }
                }
            }
        }
    }
}
