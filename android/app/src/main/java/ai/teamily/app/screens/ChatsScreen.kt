package ai.teamily.app.screens

import ai.teamily.app.Chat
import ai.teamily.app.R
import ai.teamily.app.Sample
import ai.teamily.app.ui.Avatar
import ai.teamily.app.ui.Gutter
import ai.teamily.app.ui.Pill
import ai.teamily.app.ui.SearchField
import ai.teamily.app.ui.SectionLabel
import ai.teamily.app.ui.T
import ai.teamily.app.ui.tc
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val Filters = listOf("All", "Unread", "Agents", "Groups")

@Composable
fun ChatsScreen(onOpenChat: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    val chats = Sample.chats.filter {
        when (filter) {
            "Unread" -> it.unread > 0
            "Agents" -> it.isAgent
            "Groups" -> it.members != null
            else -> true
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = Gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.teamily_mark), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            Text("Teamily AI", style = T.title, color = tc.ink, modifier = Modifier.weight(1f))
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = tc.ink)
            }
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = tc.ink)
            }
        }
        SearchField("Search chats, agents, people")
        Row(Modifier.padding(horizontal = Gutter, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Filters.forEach { Pill(it, selected = it == filter, onClick = { filter = it }) }
        }
        if (chats.isEmpty()) EmptyChats() else LazyColumn(Modifier.weight(1f)) {
            item { SectionLabel("${chats.size} chats") }
            items(chats, key = { it.id }) { ChatRow(it) { onOpenChat(it.id) } }
        }
    }
}


@Composable
private fun ChatRow(chat: Chat, onClick: () -> Unit) {
    val unread = chat.unread > 0
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).height(IntrinsicSize.Min).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (unread) tc.primary else Color.Transparent))
        Spacer(Modifier.width(Gutter - 3.dp))
        Avatar(chat.avatar, agentBadge = chat.isAgent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val name = if (chat.members != null) "${chat.title} · ${chat.members}" else chat.title
                Text(name, style = T.rowTitle, color = tc.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(chat.time, style = T.caption, color = tc.ink3)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.isAgent) {
                    Text(
                        "AGENT", style = T.overline, color = tc.primaryInk,
                        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(tc.primarySoft).padding(horizontal = 5.dp, vertical = 3.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(chat.preview, style = T.body, color = tc.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (unread) {
                    Box(Modifier.padding(start = 8.dp).size(20.dp).background(tc.primary, CircleShape), Alignment.Center) {
                        Text("${chat.unread}", style = T.overline, color = tc.onPrimary)
                    }
                }
            }
        }
        Spacer(Modifier.width(Gutter))
    }
}

@Composable
private fun EmptyChats() {
    Column(Modifier.fillMaxSize().padding(28.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).background(tc.primarySoft, CircleShape), Alignment.Center) {
            Box(Modifier.size(32.dp).background(tc.mint, CircleShape))
        }
        Spacer(Modifier.height(18.dp))
        Text("No chats yet", style = T.title, color = tc.ink)
        Text("Start a chat with an agent or a person.", style = T.body, color = tc.ink2)
    }
}
