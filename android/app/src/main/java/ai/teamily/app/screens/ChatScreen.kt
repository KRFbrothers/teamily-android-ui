package ai.teamily.app.screens

import ai.teamily.app.Api
import ai.teamily.app.ApiException
import ai.teamily.app.Message
import ai.teamily.app.Sample
import ai.teamily.app.ui.Avatar
import ai.teamily.app.ui.Gutter
import ai.teamily.app.ui.OutlinedPill
import ai.teamily.app.ui.PrimaryButton
import ai.teamily.app.ui.T
import ai.teamily.app.ui.tc
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(chatId: String, api: Api, onBack: () -> Unit) {
    val chat = Sample.chats.first { it.id == chatId }
    val messages = remember { mutableStateListOf<Message>().apply { addAll(Sample.thread) } }
    var draft by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size, thinking) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().background(tc.surface).statusBarsPadding().height(60.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = tc.ink)
            }
            Avatar(chat.avatar, size = 40.dp, agentBadge = chat.isAgent)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(chat.title, style = T.rowTitle, color = tc.ink)
                Text(if (chat.isAgent) "Agent · always on" else "Online", style = T.caption, color = tc.ink3)
            }
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = tc.ink)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(tc.lineSoft))

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Gutter),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Text("Today", style = T.caption, color = tc.ink3, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            items(messages) { m -> Bubble(m, showArtifact = !m.fromUser && m === messages.firstOrNull { !it.fromUser }) }
            if (thinking) item { Bubble(Message(false, "Thinking…", ""), showArtifact = false) }
        }

        Composer(
            value = draft, onChange = { draft = it }, enabled = !thinking,
            onSend = {
                val text = draft.trim()
                if (text.isNotEmpty()) {
                    messages += Message(true, text, "Now")
                    draft = ""
                    thinking = true
                    scope.launch {
                        val reply = try {
                            api.assist(text)
                        } catch (e: ApiException) {
                            "⚠ ${e.message}"
                        }
                        messages += Message(false, reply, "Now")
                        thinking = false
                    }
                }
            },
        )
    }
}

@Composable
private fun Bubble(m: Message, showArtifact: Boolean) {
    val maxFraction = if (m.fromUser) 0.78f else 0.85f
    val maxWidth = (LocalConfiguration.current.screenWidthDp * maxFraction).dp
    val shape = if (m.fromUser) RoundedCornerShape(14.dp, 14.dp, 5.dp, 14.dp) else RoundedCornerShape(14.dp, 14.dp, 14.dp, 5.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.fromUser) Alignment.End else Alignment.Start) {
        Column(
            Modifier.widthIn(max = maxWidth).clip(shape)
                .background(if (m.fromUser) tc.userBubble else tc.agentBubble)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(m.text, style = T.body, color = if (m.fromUser) tc.userInk else tc.agentInk)
            if (showArtifact) {
                Spacer(Modifier.height(10.dp))
                ArtifactCard()
            }
        }
        Text(m.time, style = T.caption, color = tc.ink3, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ArtifactCard() {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tc.surface2).border(1.dp, tc.line, shape).padding(12.dp),
    ) {
        Text("PREVIEW", style = T.overline, color = tc.primaryInk)
        Text("meetup-landing · v1", style = T.rowTitle, color = tc.ink)
        Text("Responsive · 12 sections · Lato", style = T.rowSub, color = tc.ink3)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedPill("Open preview", onClick = {})
            PrimaryButton("Publish", onClick = {})
        }
    }
}

@Composable
private fun Composer(value: String, onChange: (String) -> Unit, enabled: Boolean, onSend: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(shape).background(tc.surface).border(1.dp, tc.line, shape).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Attach", tint = tc.ink2)
        }
        Box(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            if (value.isEmpty()) Text("Message Teamily…", style = T.body, color = tc.ink3)
            BasicTextField(
                value = value, onValueChange = onChange, maxLines = 5,
                textStyle = T.body.copy(color = tc.ink), cursorBrush = SolidColor(tc.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val canSend = enabled && value.isNotBlank()
        IconButton(
            onClick = onSend, enabled = canSend,
            modifier = Modifier.size(44.dp).background(if (canSend) tc.primary else tc.surface3, CircleShape),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (canSend) tc.onPrimary else tc.ink3)
        }
    }
}
