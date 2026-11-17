package ai.duagenai.design.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyShapes
import ai.duagenai.design.theme.TeamilyStroke
import ai.duagenai.design.theme.teamilyPalette
import ai.duagenai.design.theme.teamilyPrimaryGlow
import ai.duagenai.design.theme.teamilyRestingShadow

/**
 * Authentic Teamily Agent Chat Bar matching 100% parity with the official
 * Teamily AI mobile application (ai.teamily.mobile).
 *
 * Layout Structure:
 * - Floating card container with rounded corners and soft resting shadow.
 * - Row 1: Text input field ("Type a message...").
 * - Row 2: Action toolbar:
 *   [+] [Auto ▾] [|] [Meeting Minutes] [Dictate] [|] [Emoji] ... [Voice msg] [Voice call] / [Send] / [Stop]
 *
 * Every toolbar control keeps its small drawn size but gets a 48dp touch box
 * via [touchTarget48], so the row still fits a 390dp phone.
 *
 * Live threads (ChatScreen) also pass [sending] + [onStop] (the trailing
 * control becomes Stop while a reply is in flight) and [onOpenSkills] /
 * [onOpenMcp], which add "Skills" and "MCP Tools" to the mode pill's menu so
 * the existing Skills / MCP sheets open from it.
 */
@Composable
fun TeamilyAgentChatBar(
    value: String = "",
    onValueChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
    placeholder: String = "Type a message...",
    enabled: Boolean = true,
    selectedMode: String = "Auto",
    onModeChange: (String) -> Unit = {},
    onAttachClick: () -> Unit = {},
    onMeetingMinutesClick: () -> Unit = {},
    onDictateClick: () -> Unit = {},
    onEmojiClick: () -> Unit = {},
    onVoiceMessageClick: () -> Unit = {},
    onVoiceCallClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    /** A reply is in flight. With [onStop] the trailing control becomes Stop. */
    sending: Boolean = false,
    onStop: (() -> Unit)? = null,
    /** Mode-pill menu entries that open the Skills / MCP sheets. Null hides them. */
    onOpenSkills: (() -> Unit)? = null,
    onOpenMcp: (() -> Unit)? = null,
) {
    val palette = teamilyPalette()
    var modeMenuExpanded by remember { mutableStateOf(false) }
    val hasText = value.isNotBlank()
    // Guarded: blank text or an in-flight reply never sends.
    val canSend = hasText && !sending
    val showStop = sending && onStop != null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .teamilyRestingShadow(palette, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = palette.composerBg,
        border = BorderStroke(1.dp, palette.borderGradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            // Row 1: Input text field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 30.dp, max = 100.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    textStyle = DuaGenAITextStyles.bubble.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                    ),
                    cursorBrush = SolidColor(palette.signalGreen),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = DuaGenAITextStyles.bubble.copy(fontSize = 13.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    },
                )
            }

            Spacer(Modifier.height(6.dp))

            // Row 2: Tool & Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // LEFT GROUP: Attach (+)
                ToolbarIconButton(
                    contentDescription = "Attach",
                    onClick = onAttachClick,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Mode Selector Pill: "Auto ▾"
                Box {
                    val pillSource = remember { MutableInteractionSource() }
                    Surface(
                        shape = TeamilyShapes.capsule,
                        color = palette.surfaceRecessed,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .touchTarget48(
                                Modifier
                                    .clickable(
                                        interactionSource = pillSource,
                                        indication = null,
                                        role = Role.Button,
                                    ) { modeMenuExpanded = true }
                                    .semantics { contentDescription = "Mode: $selectedMode" },
                            )
                            .clip(TeamilyShapes.capsule)
                            .indication(pillSource, LocalIndication.current),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = selectedMode,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(3.dp))
                            ChevronDownGlyph(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    DropdownMenu(
                        expanded = modeMenuExpanded,
                        onDismissRequest = { modeMenuExpanded = false },
                    ) {
                        listOf("Auto", "Agent Engine", "Deep Research", "Fast").forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = mode,
                                        fontWeight = if (mode == selectedMode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (mode == selectedMode) palette.signalGreen else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    onModeChange(mode)
                                    modeMenuExpanded = false
                                },
                            )
                        }
                        if (onOpenSkills != null || onOpenMcp != null) {
                            HorizontalDivider(color = palette.border)
                        }
                        if (onOpenSkills != null) {
                            DropdownMenuItem(
                                text = { Text("Skills") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Build, contentDescription = null)
                                },
                                onClick = {
                                    modeMenuExpanded = false
                                    onOpenSkills()
                                },
                            )
                        }
                        if (onOpenMcp != null) {
                            DropdownMenuItem(
                                text = { Text("MCP Tools") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Extension, contentDescription = null)
                                },
                                onClick = {
                                    modeMenuExpanded = false
                                    onOpenMcp()
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))
                VerticalDivider(color = palette.border)
                Spacer(Modifier.width(4.dp))

                // Tool: Meeting Minutes
                ToolbarIconButton(
                    contentDescription = "Meeting Minutes",
                    onClick = onMeetingMinutesClick,
                ) {
                    MeetingMinutesGlyph(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.width(2.dp))

                // Tool: Dictate
                ToolbarIconButton(
                    contentDescription = "Dictate",
                    onClick = onDictateClick,
                ) {
                    DictateGlyph(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.width(4.dp))
                VerticalDivider(color = palette.border)
                Spacer(Modifier.width(4.dp))

                // Tool: Emoji
                ToolbarIconButton(
                    contentDescription = "Emoji",
                    onClick = onEmojiClick,
                ) {
                    EmojiGlyph(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.weight(1f))

                // RIGHT GROUP: [Stop] while a reply is in flight, else [Send]
                // when there is text, else [Voice msg + Call].
                val trailing = when {
                    showStop -> TrailingAction.Stop
                    hasText -> TrailingAction.Send
                    else -> TrailingAction.Voice
                }
                AnimatedContent(
                    targetState = trailing,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "SendActionTransition",
                ) { target ->
                    if (target != TrailingAction.Voice) {
                        // Emerald primary disc (brandGradient + sendButtonIcon),
                        // drawn 32dp inside a 48dp touch box.
                        val isStop = target == TrailingAction.Stop
                        val sendSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .touchTarget48(
                                    Modifier
                                        .clickable(
                                            interactionSource = sendSource,
                                            indication = null,
                                            role = Role.Button,
                                            onClick = {
                                                if (isStop) onStop?.invoke()
                                                else if (canSend) onSend()
                                            },
                                        )
                                        .semantics {
                                            contentDescription = if (isStop) "Stop" else "Send message"
                                        },
                                )
                                .size(32.dp)
                                .teamilyPrimaryGlow(palette)
                                .clip(CircleShape)
                                .background(palette.brandGradient)
                                .indication(sendSource, LocalIndication.current),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isStop) Icons.Filled.Stop
                                else Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                                tint = palette.sendButtonIcon,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        // Voice message and Voice call buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ToolbarIconButton(
                                contentDescription = "Voice message",
                                onClick = onVoiceMessageClick,
                            ) {
                                VoiceMessageGlyph(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ToolbarIconButton(
                                contentDescription = "Voice call",
                                onClick = onVoiceCallClick,
                            ) {
                                VoiceCallGlyph(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The chat bar's single trailing control. */
private enum class TrailingAction { Voice, Send, Stop }

/** A 30dp toolbar glyph with a 48dp touch box ([touchTarget48]). */
@Composable
private fun ToolbarIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .touchTarget48(
                Modifier
                    .clickable(
                        role = Role.Button,
                        onClick = onClick,
                        interactionSource = source,
                        indication = null,
                    )
                    .semantics { this.contentDescription = contentDescription },
            )
            .size(30.dp)
            .clip(CircleShape)
            .indication(source, LocalIndication.current),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun VerticalDivider(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(1.dp)
            .height(14.dp)
            .background(color),
    )
}

/** Draws the chevron down glyph for the mode pill. */
@Composable
private fun ChevronDownGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(9.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.8f, h * 0.35f)
        }
        drawPath(path, color = tint, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Draws Meeting Minutes icon (document/note with bullet points). */
@Composable
private fun MeetingMinutesGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(17.dp)) {
        val w = size.width
        val h = size.height
        // Outer page border
        val page = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.85f)
            lineTo(w * 0.25f, h * 0.85f)
            close()
        }
        drawPath(page, color = tint, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Lines
        drawLine(tint, Offset(w * 0.40f, h * 0.35f), Offset(w * 0.65f, h * 0.35f), strokeWidth = 1.1.dp.toPx(), cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.40f, h * 0.50f), Offset(w * 0.65f, h * 0.50f), strokeWidth = 1.1.dp.toPx(), cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.40f, h * 0.65f), Offset(w * 0.60f, h * 0.65f), strokeWidth = 1.1.dp.toPx(), cap = StrokeCap.Round)
        // Bullet dots
        drawCircle(tint, radius = 0.9.dp.toPx(), center = Offset(w * 0.32f, h * 0.35f))
        drawCircle(tint, radius = 0.9.dp.toPx(), center = Offset(w * 0.32f, h * 0.50f))
        drawCircle(tint, radius = 0.9.dp.toPx(), center = Offset(w * 0.32f, h * 0.65f))
    }
}

/** Draws Dictate waveform bars glyph. */
@Composable
private fun DictateGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(17.dp)) {
        val w = size.width
        val h = size.height
        val barWidth = 1.3.dp.toPx()
        drawLine(tint, Offset(w * 0.25f, h * 0.40f), Offset(w * 0.25f, h * 0.60f), strokeWidth = barWidth, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.42f, h * 0.25f), Offset(w * 0.42f, h * 0.75f), strokeWidth = barWidth, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.58f, h * 0.18f), Offset(w * 0.58f, h * 0.82f), strokeWidth = barWidth, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.75f, h * 0.35f), Offset(w * 0.75f, h * 0.65f), strokeWidth = barWidth, cap = StrokeCap.Round)
    }
}

/** Draws smiling face Emoji glyph. */
@Composable
private fun EmojiGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(17.dp)) {
        val w = size.width
        val h = size.height
        // Face outline
        drawCircle(tint, radius = w * 0.40f, style = Stroke(width = 1.3.dp.toPx()))
        // Eyes
        drawCircle(tint, radius = 0.9.dp.toPx(), center = Offset(w * 0.37f, h * 0.40f))
        drawCircle(tint, radius = 0.9.dp.toPx(), center = Offset(w * 0.63f, h * 0.40f))
        // Smile
        val smile = Path().apply {
            moveTo(w * 0.35f, h * 0.58f)
            quadraticTo(w * 0.50f, h * 0.72f, w * 0.65f, h * 0.58f)
        }
        drawPath(smile, color = tint, style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Draws Voice Message microphone glyph. */
@Composable
private fun VoiceMessageGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(17.dp)) {
        val w = size.width
        val h = size.height
        // Mic capsule
        val capsule = Path().apply {
            moveTo(w * 0.38f, h * 0.25f)
            lineTo(w * 0.62f, h * 0.25f)
            lineTo(w * 0.62f, h * 0.55f)
            lineTo(w * 0.38f, h * 0.55f)
            close()
        }
        drawPath(capsule, color = tint, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Cradle arc
        val arc = Path().apply {
            moveTo(w * 0.28f, h * 0.45f)
            quadraticTo(w * 0.50f, h * 0.78f, w * 0.72f, h * 0.45f)
        }
        drawPath(arc, color = tint, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))
        // Stand line
        drawLine(tint, Offset(w * 0.50f, h * 0.70f), Offset(w * 0.50f, h * 0.85f), strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round)
    }
}

/** Draws Voice Call telephone handset glyph. */
@Composable
private fun VoiceCallGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(17.dp)) {
        val w = size.width
        val h = size.height
        val phone = Path().apply {
            moveTo(w * 0.30f, h * 0.28f)
            cubicTo(w * 0.30f, h * 0.22f, w * 0.38f, h * 0.20f, w * 0.44f, h * 0.26f)
            lineTo(w * 0.50f, h * 0.36f)
            cubicTo(w * 0.54f, h * 0.42f, w * 0.50f, h * 0.48f, w * 0.44f, h * 0.52f)
            cubicTo(w * 0.48f, h * 0.60f, w * 0.56f, h * 0.68f, w * 0.64f, h * 0.72f)
            cubicTo(w * 0.68f, h * 0.66f, w * 0.74f, h * 0.62f, w * 0.80f, h * 0.66f)
            lineTo(w * 0.90f, h * 0.72f)
            cubicTo(w * 0.96f, h * 0.78f, w * 0.94f, h * 0.86f, w * 0.88f, h * 0.86f)
            cubicTo(w * 0.60f, h * 0.86f, w * 0.28f, h * 0.56f, w * 0.30f, h * 0.28f)
            close()
        }
        drawPath(phone, color = tint, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Preview(name = "Teamily Chat Bar — Empty", showBackground = true)
@Composable
private fun TeamilyChatBarEmptyPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(16.dp)) {
        TeamilyAgentChatBar(value = "")
    }
}

@Preview(name = "Teamily Chat Bar — With Text", showBackground = true)
@Composable
private fun TeamilyChatBarTextPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(16.dp)) {
        TeamilyAgentChatBar(value = "P1+P2+P3 ek hi PR me fix karo")
    }
}
