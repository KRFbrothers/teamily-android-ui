package ai.duagenai.design.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.TeamilyMarks
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyShapes
import ai.duagenai.design.theme.teamilyCanvas
import ai.duagenai.design.theme.teamilyPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Personal Agent's quick-action prompts. Shared by this demo and the live
 * Personal Agent thread (ChatScreen), so the two can never drift.
 */
internal val PERSONAL_AGENT_QUICK_ACTIONS = listOf(
    "🗓️ Today's Schedule",
    "✍️ Draft Team Reply",
    "📝 Daily Digest",
    "⚡ Check Next Task",
)

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

    val quickActions = PERSONAL_AGENT_QUICK_ACTIONS

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
                    .padding(horizontal = spacing.sm, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(spacing.xs))
                BotBadge(34.dp)
                Spacer(Modifier.width(spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "fareed's Personal Agent",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(palette.signalGreen),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Permanent Agent \u00b7 Autonomous",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
                contentPadding = PaddingValues(horizontal = spacing.gutter, vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Chat Messages
                items(messages) { msg ->
                    ChatBubble(message = msg, palette = palette)
                }

                item {
                    QuickActionChips(
                        label = "Try asking:",
                        actions = quickActions,
                        onPick = { sendPrompt(it) },
                    )
                }
            }

            // Bottom Input Bar
            var selectedMode by remember { mutableStateOf("Auto") }
            ChatBar(
                value = inputText,
                onValueChange = { inputText = it },
                hasMessages = messages.isNotEmpty(),
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

/**
 * A caption plus prompt chips in rows of two. Shared by both demo screens, the
 * live Personal Agent thread and Dua home's quick starters.
 */
@Composable
internal fun QuickActionChips(
    label: String?,
    actions: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val palette = teamilyPalette()
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalAlignment = horizontalAlignment,
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
            )
            Spacer(Modifier.height(2.dp))
        }
        actions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = horizontalArrangement,
            ) {
                row.forEach { action ->
                    ActionChip(label = action, onClick = { onPick(action) }, palette = palette)
                }
            }
        }
    }
}

/**
 * One prompt chip. Drawn as a compact capsule; the touch box is raised to the
 * 48dp minimum ([minTouchTarget]) and it is announced as a button.
 */
@Composable
internal fun ActionChip(
    label: String,
    onClick: () -> Unit,
    palette: ai.duagenai.design.theme.TeamilyPalette,
) {
    val isDark = isSystemInDarkTheme()
    val chipColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else palette.surfaceRecessed
    val chipBorder = if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                     else BorderStroke(1.dp, palette.border)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .minTouchTarget()
            .semantics { role = Role.Button },
        shape = TeamilyShapes.capsule,
        color = chipColor,
        border = chipBorder,
    ) {
        Text(
            text = label,
            // Lato ships 400 / 700 only; Medium (500) already resolved to 400.
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 14.dp,
            ),
            color = if (isUser) palette.signalGreen else palette.cardHeroColor,
            border = if (isUser) null else BorderStroke(1.dp, palette.heroCardBorder),
            shadowElevation = 0.5.dp,
            modifier = Modifier.padding(
                start = if (isUser) 40.dp else 0.dp,
                end = if (isUser) 0.dp else 40.dp,
            ),
        ) {
            Column(Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                    ),
                    color = if (isUser) TeamilyMarks.ink else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                    color = if (isUser) TeamilyMarks.ink.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
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
