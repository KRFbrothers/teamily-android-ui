package ai.duagenai.design.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** One orchestration message in Dua Agent demo. */
private data class DuaAgentMessage(
    val isUser: Boolean,
    val text: String,
    val time: String = "Now",
    val subAgentsActive: List<String> = emptyList(),
)

/**
 * Dedicated Demo screen for Dua Agent (Official Permanent Agent).
 *
 * Demonstrates:
 * - Official verified status & multi-agent orchestration
 * - Delegating complex workflows across sub-agents
 * - Built-in skills, MCP connectivity, and live workspace controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaAgentDemoScreen(
    onBack: () -> Unit = {},
    onOpenWorkspace: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val palette = teamilyPalette()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            DuaAgentMessage(
                isUser = false,
                text = "I'm Dua Agent — the official agent of Dua Gen AI. I coordinate complex multi-agent workflows, supervise specialized agents, and interface with MCP tools.",
                time = "10:15 AM",
            ),
            DuaAgentMessage(
                isUser = true,
                text = "Deploy Q3 Product Launch workflow across our agents.",
                time = "10:16 AM",
            ),
            DuaAgentMessage(
                isUser = false,
                text = "Multi-agent orchestration executed:\n1. 📊 Slides Assistant -> Created 12-slide pitch deck [Complete]\n2. 🔍 Market Research -> Analyzed 4 key competitors [Complete]\n3. 💰 Accounting Assistant -> Reconciled project budget ledger [Active]\n\nAll results compiled and available in your Workspace.",
                time = "10:17 AM",
                subAgentsActive = listOf("Slides Assistant", "Market Research", "Accounting Assistant"),
            ),
        )
    }

    val demoActions = listOf(
        "🚀 Run Multi-Agent Workflow",
        "🛠️ Test Installed Skills",
        "🔌 Query MCP Connectors",
        "🌐 Deploy Live Site",
    )

    fun sendCommand(cmd: String) {
        if (cmd.isBlank()) return
        messages.add(DuaAgentMessage(isUser = true, text = cmd, time = "Now"))
        inputText = ""
        scope.launch {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
            delay(600)
            val replyText = when {
                cmd.contains("Multi-Agent", ignoreCase = true) || cmd.contains("Workflow", ignoreCase = true) ->
                    "Orchestrating workflow across 3 agents:\n• Slides Assistant: Building presentation draft\n• Market Research: Extracting citations\n• API Tester: Running automated test suite\nProgress: 100% synchronized."
                cmd.contains("Skills", ignoreCase = true) ->
                    "Active Dua Agent Skills (5/5):\n1. Web Search & Verification\n2. Python 3.11 Code Execution\n3. File System & Artifact Storage\n4. Data Extraction Engine\n5. RESTful API Interface"
                cmd.contains("MCP", ignoreCase = true) ->
                    "MCP Status: Connected to local host via stdio. Exposing 14 agent tools to external clients (Claude Code, Codex)."
                cmd.contains("Deploy", ignoreCase = true) || cmd.contains("Site", ignoreCase = true) ->
                    "Live site deployed to web workspace: https://dua.run/q3-preview. SSL active, CDN cached."
                else ->
                    "Dua Agent Engine received command: '$cmd'. Dispatched to multi-agent orchestrator queue."
            }
            messages.add(
                DuaAgentMessage(
                    isUser = false,
                    text = replyText,
                    time = "Now",
                    subAgentsActive = if (cmd.contains("Multi-Agent", ignoreCase = true)) {
                        listOf("Slides Assistant", "Market Research", "API Tester")
                    } else emptyList(),
                ),
            )
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
                SignalBotBadge(40.dp)
                Spacer(Modifier.width(spacing.sm))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Teamily Agent",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(6.dp))
                        VerifiedCheck()
                    }
                    Text(
                        text = "Official Permanent Agent \u00b7 Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.signalGreen,
                    )
                }
                OutlinedButton(
                    onClick = onOpenWorkspace,
                    shape = TeamilyShapes.capsule,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, palette.border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.signalGreen),
                ) {
                    Text(
                        text = "Workspace",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                    )
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
                    // Official Engine Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = TeamilyShapes.card,
                        color = palette.cardHeroColor,
                        border = BorderStroke(1.dp, palette.heroCardBorder),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SignalBotBadge(24.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Dua Agent \u2014 The Official Core",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.width(4.dp))
                                VerifiedCheck()
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "The primary orchestrator of Dua Gen AI. Controls child agents, executes skills, manages MCP tool servers, and powers team collaboration.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                EnginePill(icon = Icons.Outlined.Hub, label = "Orchestration", palette = palette)
                                EnginePill(icon = Icons.Outlined.Build, label = "5 Skills", palette = palette)
                                EnginePill(icon = Icons.Outlined.Extension, label = "MCP Enabled", palette = palette)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "MULTI-AGENT ORCHESTRATION MATRIX",
                        style = DuaGenAITextStyles.eyebrow,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TeamilyShapes.card)
                            .background(palette.cardHeroColor)
                            .border(1.dp, palette.heroCardBorder, TeamilyShapes.card)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OrchestrationRow("Slides Assistant", "Decks & Presentations", "Ready", palette)
                        OrchestrationRow("Market Research", "Competitive Intel & Citations", "Complete", palette)
                        OrchestrationRow("Accounting Assistant", "Ledgers & Invoices", "Supervised", palette)
                    }
                }

                item {
                    Text(
                        text = "INTERACTIVE ORCHESTRATION DEMO",
                        style = DuaGenAITextStyles.eyebrow,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }

                // Chat Messages
                items(messages) { msg ->
                    DuaAgentBubble(message = msg, palette = palette)
                }

                item {
                    // Quick Action Prompt Chips
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(
                            text = "Orchestration commands:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            demoActions.take(2).forEach { action ->
                                ActionChip(label = action, onClick = { sendCommand(action) }, palette = palette)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            demoActions.drop(2).forEach { action ->
                                ActionChip(label = action, onClick = { sendCommand(action) }, palette = palette)
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
                onSend = { sendCommand(inputText) },
                placeholder = "Type a message...",
                selectedMode = selectedMode,
                onModeChange = { selectedMode = it },
                onMeetingMinutesClick = {
                    sendCommand("Generate meeting minutes from recent discussions")
                },
                onDictateClick = {
                    sendCommand("Start dictation session")
                },
                onVoiceMessageClick = {
                    sendCommand("Voice message recording started")
                },
                onVoiceCallClick = {
                    sendCommand("Initiate voice call session")
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
private fun EnginePill(
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
private fun OrchestrationRow(
    name: String,
    role: String,
    status: String,
    palette: ai.duagenai.design.theme.TeamilyPalette,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(palette.signalGreen),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = TeamilyShapes.capsule,
            color = palette.surfaceRecessed,
            border = BorderStroke(0.5.dp, palette.border),
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                color = palette.signalGreen,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun DuaAgentBubble(
    message: DuaAgentMessage,
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
                if (message.subAgentsActive.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        message.subAgentsActive.forEach { agent ->
                            Surface(
                                shape = TeamilyShapes.capsule,
                                color = palette.surfaceRecessed,
                                border = BorderStroke(0.5.dp, palette.border),
                            ) {
                                Text(
                                    text = "🤖 $agent",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = palette.signalGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
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

@Preview(name = "Dua Agent Demo — Light", showBackground = true)
@Composable
private fun DuaAgentDemoLightPreview() = DuaGenAITheme(darkTheme = false) {
    DuaAgentDemoScreen()
}

@Preview(name = "Dua Agent Demo — Dark", showBackground = true)
@Composable
private fun DuaAgentDemoDarkPreview() = DuaGenAITheme(darkTheme = true) {
    DuaAgentDemoScreen()
}
