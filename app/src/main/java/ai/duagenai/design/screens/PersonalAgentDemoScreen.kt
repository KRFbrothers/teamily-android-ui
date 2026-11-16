package ai.duagenai.design.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyShapes
import ai.duagenai.design.theme.teamilyCanvas
import ai.duagenai.design.theme.teamilyPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One demo message in the Personal Agent demo screen. */
private data class DemoChatMessage(
    val isUser: Boolean,
    val text: String,
    val time: String = "Now",
)

/**
 * Dedicated Demo screen for the Personal Agent (Permanent Agent).
 *
 * Demonstrates:
 * - Permanent agent identity & user personalization
 * - Autonomous background features & local privacy
 * - Interactive conversation & quick command prompts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalAgentDemoScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            DemoChatMessage(
                isUser = false,
                text = "Hey Fareed! 👋 I am your Personal Agent. I stay active 24/7 on your device to organize your schedules, drafts, and daily tasks.",
                time = "10:00 AM",
            ),
            DemoChatMessage(
                isUser = true,
                text = "What are my priorities and schedule for today?",
                time = "10:02 AM",
            ),
            DemoChatMessage(
                isUser = false,
                text = "Here is your plan for today:\n• 2:00 PM: Jetpack Compose Migration review\n• 4:30 PM: Teamily core team sync\n• Status: All email responses pre-drafted for your 1-tap confirmation.",
                time = "10:02 AM",
            ),
        )
    }

    val quickActions = listOf(
        "🗓️ Today's Schedule",
        "✍️ Draft Team Reply",
        "📝 Daily Digest",
        "⚡ Check Next Task",
    )

    fun sendPrompt(prompt: String) {
        if (prompt.isBlank()) return
        messages.add(DemoChatMessage(isUser = true, text = prompt, time = "Now"))
        inputText = ""
        scope.launch {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
            delay(600)
            val replyText = when {
                prompt.contains("Schedule", ignoreCase = true) || prompt.contains("Meeting", ignoreCase = true) ->
                    "Upcoming: 'Teamily core team sync' at 4:30 PM (in 45 mins). I'll ping you 5 minutes before."
                prompt.contains("Draft", ignoreCase = true) || prompt.contains("Reply", ignoreCase = true) ->
                    "Draft created: 'Hi team, the Native Jetpack Compose demo screens are ready for review.' Sent to Outbox."
                prompt.contains("Digest", ignoreCase = true) || prompt.contains("Priority", ignoreCase = true) ->
                    "Daily Digest: 3 tasks done, 1 meeting remaining, memory synced 42 insights about your coding preferences."
                else ->
                    "Got it! As your Personal Agent, I've noted '$prompt' and updated your local context ledger."
            }
            messages.add(DemoChatMessage(isUser = false, text = replyText, time = "Now"))
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .teamilyCanvas(palette),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.sm, vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(spacing.xs))
                BotBadge(40.dp)
                Spacer(Modifier.width(spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "fareed's Personal Agent",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(palette.signalGreen),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Permanent Agent \u00b7 Autonomous",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.signalGreen,
                        )
                    }
                }
            }

            // Scrollable Content & Chat List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = spacing.gutter, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    // Hero Banner Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = TeamilyShapes.card,
                        color = palette.cardHeroColor,
                        border = BorderStroke(1.dp, palette.heroCardBorder),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.signalGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Permanent Personal Agent",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Your dedicated personal assistant for scheduling, daily memory, tone-aware drafts, and local task automation.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            // Status Chips Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StatusPill(icon = Icons.Outlined.Lock, label = "Private", palette = palette)
                                StatusPill(icon = Icons.Outlined.Notifications, label = "Autonomous", palette = palette)
                                StatusPill(icon = Icons.Outlined.DateRange, label = "Memory Synced", palette = palette)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "CAPABILITIES (DEMO)",
                        style = DuaGenAITextStyles.eyebrow,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CapabilityCard(
                            title = "Smart Schedule",
                            description = "Previews meetings and prepares context before you join.",
                            icon = Icons.Outlined.DateRange,
                            modifier = Modifier.weight(1f),
                            palette = palette,
                        )
                        CapabilityCard(
                            title = "Tone Drafts",
                            description = "Generates replies in your natural voice.",
                            icon = Icons.Outlined.Edit,
                            modifier = Modifier.weight(1f),
                            palette = palette,
                        )
                    }
                }

                item {
                    Text(
                        text = "INTERACTIVE DEMO CONVERSATION",
                        style = DuaGenAITextStyles.eyebrow,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }

                // Chat Messages
                items(messages) { msg ->
                    ChatBubble(message = msg, palette = palette)
                }

                item {
                    // Quick Action Prompt Chips
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(
                            text = "Try asking:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            quickActions.take(2).forEach { action ->
                                ActionChip(label = action, onClick = { sendPrompt(action) }, palette = palette)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            quickActions.drop(2).forEach { action ->
                                ActionChip(label = action, onClick = { sendPrompt(action) }, palette = palette)
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar
            var selectedMode by remember { mutableStateOf("Auto") }
            TeamilyAgentChatBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = { sendPrompt(inputText) },
                placeholder = "Type a message...",
                selectedMode = selectedMode,
                onModeChange = { selectedMode = it },
                onMeetingMinutesClick = {
                    sendPrompt("Draft summary of today's meetings and tasks")
                },
                onDictateClick = {
                    sendPrompt("Take a quick voice note")
                },
                onVoiceMessageClick = {
                    sendPrompt("Voice message memo")
                },
                onVoiceCallClick = {
                    sendPrompt("Call Personal Agent")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = spacing.gutter, vertical = spacing.sm),
            )
        }
    }
}

@Composable
private fun StatusPill(
    icon: ImageVector,
    label: String,
    palette: ai.duagenai.design.theme.TeamilyPalette,
) {
    Surface(
        shape = TeamilyShapes.capsule,
        color = palette.surfaceRecessed,
        border = BorderStroke(0.5.dp, palette.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.signalGreen,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CapabilityCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    palette: ai.duagenai.design.theme.TeamilyPalette,
) {
    Card(
        modifier = modifier,
        shape = TeamilyShapes.card,
        colors = CardDefaults.cardColors(containerColor = palette.cardHeroColor),
        border = BorderStroke(1.dp, palette.heroCardBorder),
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.signalGreen,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ActionChip(
    label: String,
    onClick: () -> Unit,
    palette: ai.duagenai.design.theme.TeamilyPalette,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = TeamilyShapes.capsule,
        color = palette.surfaceRecessed,
        border = BorderStroke(1.dp, palette.border),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ChatBubble(
    message: DemoChatMessage,
    palette: ai.duagenai.design.theme.TeamilyPalette,
) {
    val isUser = message.isUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) palette.signalGreen else palette.cardHeroColor,
            border = if (isUser) null else BorderStroke(1.dp, palette.heroCardBorder),
            shadowElevation = 1.dp,
            modifier = Modifier.padding(
                start = if (isUser) 48.dp else 0.dp,
                end = if (isUser) 0.dp else 48.dp,
            ),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = if (isUser) Color.Black else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = if (isUser) Color.Black.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Preview(name = "Personal Agent Demo — Light", showBackground = true)
@Composable
private fun PersonalAgentDemoLightPreview() = DuaGenAITheme(darkTheme = false) {
    PersonalAgentDemoScreen()
}

@Preview(name = "Personal Agent Demo — Dark", showBackground = true)
@Composable
private fun PersonalAgentDemoDarkPreview() = DuaGenAITheme(darkTheme = true) {
    PersonalAgentDemoScreen()
}
