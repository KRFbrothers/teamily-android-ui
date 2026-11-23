package ai.duagenai.design.screens

import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyPalette
import ai.duagenai.design.theme.TeamilyShapes
import ai.duagenai.design.theme.teamilyPalette
import ai.duagenai.design.theme.teamilyPrimaryGlow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.MorphPolygonShape
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.FilledTonalToggleButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.shape.CornerRounding

/**
 * Authentic Teamily Agent Chat Bar — Style A (Unified Floating Card Composer).
 *
 * Implements M3 1.5.0 expressive features:
 * - Floating Card with 28.dp rounded corners, focus-animated border, soft shadow.
 * - Streaming Wavy Progress Bar along top edge during agent replies.
 * - Context Chips Row with CapturedAnimatedVisibility.
 * - Row 1: State-based BasicTextField with PerLine LineHeightStyle.
 * - Row 2: Expressive Action Toolbar with M3 1.5 SplitButton, shape-morphing ToggleButton,
 *   dictate/minutes/emoji glyphs, and MorphPolygonShape right action button.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TeamilyAgentChatBar(
    state: TextFieldState,
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
    contextChips: List<String> = emptyList(),
    onDismissChip: (String) -> Unit = {},
) {
    val palette = teamilyPalette()
    val isDark = isSystemInDarkTheme()

    // Focus state for smooth card border illumination
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "ComposerBorderAnimation",
    )

    // Reactive state derivation using Compose 1.13 computedStateOf
    val hasText by remember(state) {
        computedStateOf { state.text.isNotBlank() }
    }
    val canSend = hasText && !sending
    val showStop = sending && onStop != null

    // MorphPolygonShape for Streaming Stop button: circle <-> 8-point rounded star
    val circlePolygon = remember { PolygonShape.circle() }
    val starPolygon = remember {
        PolygonShape.star(
            numPoints = 8,
            innerRadiusRatio = 0.82f,
            outerRounding = CornerRounding.fraction(0.5f),
        )
    }
    val infiniteTransition = rememberInfiniteTransition(label = "StopMorphTransition")
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "StopMorphProgress",
    )
    val morphShape = remember(circlePolygon, starPolygon) {
        MorphPolygonShape(circlePolygon, starPolygon) { morphProgress }
    }

    // Floating card container: 28dp radius, adaptive shadow, focused border
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDark) 2.dp else 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Streaming Wavy Progress Indicator along the top edge
            AnimatedVisibility(
                visible = sending,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }

            // Attached Context Chips with Compose 1.13 CapturedAnimatedVisibility
            CapturedAnimatedVisibility(
                visible = contextChips.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    contextChips.forEach { chipText ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = chipText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove $chipText",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable { onDismissChip(chipText) },
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                // Row 1: Multiline State-Based Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 34.dp, max = 110.dp)
                        .onFocusChanged { isFocused = it.isFocused },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        state = state,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        textStyle = DuaGenAITextStyles.bubble.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.None,
                                mode = LineHeightStyle.Mode.PerLine,
                            ),
                        ),
                        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = 4),
                        cursorBrush = SolidColor(palette.signalGreen),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        onKeyboardAction = { if (canSend) onSend() },
                        decorator = { innerTextField ->
                            if (state.text.isEmpty()) {
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

                // Row 2: Expressive Action Toolbar (Fits cleanly on 363dp - 390dp screens)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // LEFT GROUP
                    // 1. Attach (+) button
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

                    Spacer(Modifier.width(6.dp))

                    // 2. M3 1.5.0 Split Button (Auto | ▾) with anchored DropdownMenu
                    ModeSplitButton(
                        selectedMode = selectedMode,
                        onModeChange = onModeChange,
                        onOpenSkills = onOpenSkills,
                        onOpenMcp = onOpenMcp,
                        palette = palette,
                    )

                    Spacer(Modifier.width(6.dp))

                    // 3. Shape-Morphing Memory Toggle (Pill -> 12dp squircle)
                    var memoryActive by rememberSaveable { mutableStateOf(false) }
                    val memorySource = remember { MutableInteractionSource() }
                    val memoryShape = if (memoryActive) RoundedCornerShape(12.dp) else CircleShape
                    Box(
                        modifier = Modifier
                            .touchTarget48(
                                Modifier
                                    .clickable(
                                        interactionSource = memorySource,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = { memoryActive = !memoryActive },
                                    )
                                    .semantics {
                                        contentDescription = if (memoryActive) "Memory: On" else "Memory: Off"
                                    },
                            )
                            .size(36.dp)
                            .clip(memoryShape)
                            .background(
                                if (memoryActive) MaterialTheme.colorScheme.primaryContainer
                                else palette.surfaceRecessed,
                            )
                            .indication(memorySource, LocalIndication.current),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = null,
                            tint = if (memoryActive) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // 4. Dictate wave icon button
                    ToolbarIconButton(
                        contentDescription = "Dictate",
                        onClick = onDictateClick,
                    ) {
                        DictateGlyph(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.weight(1f))

                    // RIGHT GROUP: Morphing Action Button (Mic / Send / Stop)
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
                        when (target) {
                            TrailingAction.Stop -> {
                                val stopSource = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .touchTarget48(
                                            Modifier
                                                .clickable(
                                                    interactionSource = stopSource,
                                                    indication = null,
                                                    role = Role.Button,
                                                    onClick = { onStop?.invoke() },
                                                )
                                                .semantics { contentDescription = "Stop" },
                                        )
                                        .size(42.dp)
                                        .clip(morphShape)
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .indication(stopSource, LocalIndication.current),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Stop,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            TrailingAction.Send -> {
                                val sendSource = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .touchTarget48(
                                            Modifier
                                                .clickable(
                                                    interactionSource = sendSource,
                                                    indication = null,
                                                    role = Role.Button,
                                                    onClick = { if (canSend) onSend() },
                                                )
                                                .semantics { contentDescription = "Send message" },
                                        )
                                        .size(42.dp)
                                        .teamilyPrimaryGlow(palette)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .indication(sendSource, LocalIndication.current),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Send,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            TrailingAction.Voice -> {
                                val voiceSource = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .touchTarget48(
                                            Modifier
                                                .clickable(
                                                    interactionSource = voiceSource,
                                                    indication = null,
                                                    role = Role.Button,
                                                    onClick = onVoiceMessageClick,
                                                )
                                                .semantics { contentDescription = "Voice message" },
                                        )
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(palette.surfaceRecessed)
                                        .indication(voiceSource, LocalIndication.current),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    VoiceMessageGlyph(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible overload for string-controlled callers and previews.
 */
@Composable
fun TeamilyAgentChatBar(
    value: String,
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
    sending: Boolean = false,
    onStop: (() -> Unit)? = null,
    onOpenSkills: (() -> Unit)? = null,
    onOpenMcp: (() -> Unit)? = null,
    contextChips: List<String> = emptyList(),
    onDismissChip: (String) -> Unit = {},
) {
    val state = rememberTextFieldState(initialText = value)
    LaunchedEffect(value) {
        if (state.text.toString() != value) {
            state.edit { replace(0, length, value) }
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .collect { newText ->
                if (newText != value) {
                    onValueChange(newText)
                }
            }
    }
    TeamilyAgentChatBar(
        state = state,
        onSend = onSend,
        placeholder = placeholder,
        enabled = enabled,
        selectedMode = selectedMode,
        onModeChange = onModeChange,
        onAttachClick = onAttachClick,
        onMeetingMinutesClick = onMeetingMinutesClick,
        onDictateClick = onDictateClick,
        onEmojiClick = onEmojiClick,
        onVoiceMessageClick = onVoiceMessageClick,
        onVoiceCallClick = onVoiceCallClick,
        modifier = modifier,
        sending = sending,
        onStop = onStop,
        onOpenSkills = onOpenSkills,
        onOpenMcp = onOpenMcp,
        contextChips = contextChips,
        onDismissChip = onDismissChip,
    )
}

/** Mode Split Button with anchored dropdown */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModeSplitButton(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    onOpenSkills: (() -> Unit)?,
    onOpenMcp: (() -> Unit)?,
    palette: TeamilyPalette,
    modifier: Modifier = Modifier,
) {
    var modeMenuExpanded by remember { mutableStateOf(false) }
    val modes = listOf("Auto", "Instant", "Agentic", "Deep Research", "Fast")
    val displayMode = when (selectedMode) {
        "Agent Engine" -> "Agentic"
        "Deep Research" -> "Research"
        else -> selectedMode
    }

    Box(modifier = modifier) {
        val leadingSource = remember { MutableInteractionSource() }
        val trailingSource = remember { MutableInteractionSource() }

        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = { modeMenuExpanded = true },
                    modifier = Modifier.touchTarget48(
                        Modifier
                            .clickable(
                                interactionSource = leadingSource,
                                indication = null,
                                role = Role.Button,
                                onClick = { modeMenuExpanded = true },
                            )
                            .semantics { contentDescription = "Mode: $selectedMode" },
                    ),
                    shapes = SplitButtonDefaults.leadingButtonShapesFor(SplitButtonDefaults.ExtraSmallContainerHeight),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = displayMode,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = modeMenuExpanded,
                    onCheckedChange = { modeMenuExpanded = it },
                    modifier = Modifier.touchTarget48(
                        Modifier
                            .clickable(
                                interactionSource = trailingSource,
                                indication = null,
                                role = Role.Button,
                                onClick = { modeMenuExpanded = !modeMenuExpanded },
                            )
                            .semantics { contentDescription = "Mode options" },
                    ),
                    shapes = SplitButtonDefaults.trailingButtonShapesFor(SplitButtonDefaults.ExtraSmallContainerHeight),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    ChevronDownGlyph(tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            },
        )

        DropdownMenu(
            expanded = modeMenuExpanded,
            onDismissRequest = { modeMenuExpanded = false },
            properties = PopupProperties(blurBehindRadius = 16.dp),
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mode,
                            fontWeight = if (mode == selectedMode || (mode == "Agentic" && selectedMode == "Agent Engine")) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == selectedMode || (mode == "Agentic" && selectedMode == "Agent Engine")) palette.signalGreen else MaterialTheme.colorScheme.onSurface,
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
        val page = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.85f)
            lineTo(w * 0.25f, h * 0.85f)
            close()
        }
        drawPath(page, color = tint, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(tint, Offset(w * 0.40f, h * 0.35f), Offset(w * 0.65f, h * 0.35f), strokeWidth = 1.1.dp.toPx(), cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.40f, h * 0.50f), Offset(w * 0.65f, h * 0.50f), strokeWidth = 1.1.dp.toPx(), cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.40f, h * 0.65f), Offset(w * 0.60f, h * 0.65f), strokeWidth = 1.1.dp.toPx(), cap = StrokeCap.Round)
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
        drawCircle(tint, radius = w * 0.40f, style = Stroke(width = 1.3.dp.toPx()))
        drawCircle(tint, radius = 0.9.dp.toPx(), center = Offset(w * 0.37f, h * 0.40f))
        drawCircle(tint, radius = 0.9.dp.toPx(), center = Offset(w * 0.63f, h * 0.40f))
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
        val capsule = Path().apply {
            moveTo(w * 0.38f, h * 0.25f)
            lineTo(w * 0.62f, h * 0.25f)
            lineTo(w * 0.62f, h * 0.55f)
            quadraticTo(w * 0.62f, h * 0.68f, w * 0.50f, h * 0.68f)
            quadraticTo(w * 0.38f, h * 0.68f, w * 0.38f, h * 0.55f)
            close()
        }
        drawPath(capsule, color = tint, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))
        val cradle = Path().apply {
            moveTo(w * 0.28f, h * 0.50f)
            quadraticTo(w * 0.28f, h * 0.78f, w * 0.50f, h * 0.78f)
            quadraticTo(w * 0.72f, h * 0.78f, w * 0.72f, h * 0.50f)
        }
        drawPath(cradle, color = tint, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))
        drawLine(tint, Offset(w * 0.50f, h * 0.78f), Offset(w * 0.50f, h * 0.90f), strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round)
    }
}

/** Draws telephone receiver Voice Call glyph. */
@Composable
private fun VoiceCallGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(17.dp)) {
        val w = size.width
        val h = size.height
        val phone = Path().apply {
            moveTo(w * 0.28f, h * 0.32f)
            lineTo(w * 0.38f, h * 0.22f)
            lineTo(w * 0.50f, h * 0.34f)
            lineTo(w * 0.44f, h * 0.42f)
            quadraticTo(w * 0.48f, h * 0.54f, w * 0.58f, h * 0.64f)
            lineTo(w * 0.66f, h * 0.58f)
            lineTo(w * 0.78f, h * 0.70f)
            lineTo(w * 0.68f, h * 0.80f)
            quadraticTo(w * 0.52f, h * 0.85f, w * 0.34f, h * 0.67f)
            quadraticTo(w * 0.16f, h * 0.49f, w * 0.28f, h * 0.32f)
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

@Preview(name = "Teamily Chat Bar — 363dp Light", widthDp = 363, showBackground = true)
@Composable
private fun TeamilyChatBar363dpLightPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(12.dp)) {
        TeamilyAgentChatBar(value = "")
    }
}

@Preview(name = "Teamily Chat Bar — 363dp Dark", widthDp = 363, showBackground = true)
@Composable
private fun TeamilyChatBar363dpDarkPreview() = DuaGenAITheme(darkTheme = true) {
    Box(Modifier.padding(12.dp)) {
        TeamilyAgentChatBar(value = "")
    }
}

@Preview(name = "Teamily Chat Bar — 363dp Typing", widthDp = 363, showBackground = true)
@Composable
private fun TeamilyChatBar363dpTypingPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(12.dp)) {
        TeamilyAgentChatBar(value = "P1+P2+P3 ek hi PR me fix karo")
    }
}

@Preview(name = "Teamily Chat Bar — 363dp Streaming", widthDp = 363, showBackground = true)
@Composable
private fun TeamilyChatBar363dpStreamingPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(12.dp)) {
        TeamilyAgentChatBar(value = "", sending = true, onStop = {})
    }
}
