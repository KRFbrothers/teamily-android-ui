
package ai.duagenai.design.screens

import ai.duagenai.design.R
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.TeamilyPalette
import ai.duagenai.design.theme.teamilyPalette
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.CapturedAnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
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

/**
 * Collapsed pill that grows into a drafting card.
 *
 * Empty and unfocused: one row, attach, the field, mic, and a voice circle.
 * Focused, drafting, sending, or showing chips: the field moves above a tool
 * row (mode, meeting minutes, emoji, mic, then Send or Stop).
 * Colours are [MaterialTheme] tokens. [onVoiceCallClick] stays on the signature
 * for existing callers; this bar does not draw a voice-call control.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TeamilyAgentChatBar(
    state: TextFieldState,
    onSend: () -> Unit = {},
    placeholder: String = "Ask Dua...",
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
    var fieldFocused by remember { mutableStateOf(false) }

    val hasText by remember(state) {
        computedStateOf { state.text.isNotBlank() }
    }
    val canSend = hasText && !sending && enabled
    val showStop = sending && onStop != null
    val expanded = fieldFocused || hasText || sending || contextChips.isNotEmpty()

    val field = remember {
        movableContentOf<ComposerFieldSpec> { spec ->
            ComposerInputField(spec)
        }
    }
    val fieldSpec = ComposerFieldSpec(
        state = state,
        placeholder = placeholder,
        enabled = enabled,
        expanded = expanded,
        canSend = canSend,
        onSend = onSend,
        onFocusChange = { fieldFocused = it },
    )
    val trailing = when {
        showStop -> TrailingAction.Stop
        hasText -> TrailingAction.Send
        else -> TrailingAction.Voice
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f),
        shape = if (expanded) RoundedCornerShape(32.dp) else RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (isDark) 2.dp else 6.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
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
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (chipText in contextChips) {
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
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = chipText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val removeChip = stringResource(R.string.composer_remove_chip, chipText)
                                    Box(
                                        modifier = Modifier
                                            .touchTarget48(
                                                Modifier
                                                    .clickable(
                                                        role = Role.Button,
                                                        onClick = { onDismissChip(chipText) },
                                                    )
                                                    .semantics { contentDescription = removeChip },
                                            )
                                            .size(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (expanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        field(fieldSpec)
                    }
                    ComposerToolRow(
                        expanded = true,
                        enabled = enabled,
                        selectedMode = selectedMode,
                        onModeChange = onModeChange,
                        onOpenSkills = onOpenSkills,
                        onOpenMcp = onOpenMcp,
                        palette = palette,
                        onAttachClick = onAttachClick,
                        onMeetingMinutesClick = onMeetingMinutesClick,
                        onEmojiClick = onEmojiClick,
                        onDictateClick = onDictateClick,
                        trailing = trailing,
                        canSend = canSend,
                        onSend = onSend,
                        onStop = onStop,
                        onVoiceMessageClick = onVoiceMessageClick,
                        field = null,
                    )
                } else {
                    ComposerToolRow(
                        expanded = false,
                        enabled = enabled,
                        selectedMode = selectedMode,
                        onModeChange = onModeChange,
                        onOpenSkills = onOpenSkills,
                        onOpenMcp = onOpenMcp,
                        palette = palette,
                        onAttachClick = onAttachClick,
                        onMeetingMinutesClick = onMeetingMinutesClick,
                        onEmojiClick = onEmojiClick,
                        onDictateClick = onDictateClick,
                        trailing = trailing,
                        canSend = canSend,
                        onSend = onSend,
                        onStop = onStop,
                        onVoiceMessageClick = onVoiceMessageClick,
                        field = { field(fieldSpec) },
                    )
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
    placeholder: String = "Ask Dua...",
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
    val latestValue = rememberUpdatedState(value)
    val latestOnValueChange = rememberUpdatedState(onValueChange)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .collect { newText ->
                if (newText != latestValue.value) {
                    latestOnValueChange.value(newText)
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

/** The chat bar's single trailing control. */
private enum class TrailingAction { Voice, Send, Stop }

private data class ComposerFieldSpec(
    val state: TextFieldState,
    val placeholder: String,
    val enabled: Boolean,
    val expanded: Boolean,
    val canSend: Boolean,
    val onSend: () -> Unit,
    val onFocusChange: (Boolean) -> Unit,
)

@Composable
private fun ComposerInputField(spec: ComposerFieldSpec) {
    val textStyle = DuaGenAITextStyles.bubble.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
            mode = LineHeightStyle.Mode.PerLine,
        ),
    )
    BasicTextField(
        state = spec.state,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { spec.onFocusChange(it.isFocused) },
        enabled = spec.enabled,
        textStyle = textStyle,
        lineLimits = if (spec.expanded) {
            TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = 4)
        } else {
            TextFieldLineLimits.SingleLine
        },
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            imeAction = if (spec.expanded) ImeAction.Default else ImeAction.Send,
        ),
        onKeyboardAction = { if (spec.canSend) spec.onSend() },
        decorator = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = if (spec.expanded) Alignment.TopStart else Alignment.CenterStart,
            ) {
                if (spec.state.text.isEmpty()) {
                    Text(
                        text = spec.placeholder,
                        style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun ComposerToolRow(
    expanded: Boolean,
    enabled: Boolean,
    selectedMode: String,
    onModeChange: (String) -> Unit,
    onOpenSkills: (() -> Unit)?,
    onOpenMcp: (() -> Unit)?,
    palette: TeamilyPalette,
    onAttachClick: () -> Unit,
    onMeetingMinutesClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onDictateClick: () -> Unit,
    trailing: TrailingAction,
    canSend: Boolean,
    onSend: () -> Unit,
    onStop: (() -> Unit)?,
    onVoiceMessageClick: () -> Unit,
    field: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val iconTint = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (expanded) {
                    Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                } else {
                    Modifier
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.SpaceBetween else Arrangement.Start,
    ) {
        ToolbarIconButton(
            contentDescription = stringResource(R.string.composer_attach),
            onClick = onAttachClick,
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        if (!expanded && field != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                field()
            }
        }
        if (expanded) {
            ComposerModeButton(
                selectedMode = selectedMode,
                enabled = enabled,
                onModeChange = onModeChange,
                onOpenSkills = onOpenSkills,
                onOpenMcp = onOpenMcp,
                palette = palette,
            )
            ToolbarIconButton(
                contentDescription = stringResource(R.string.composer_meeting_minutes),
                onClick = onMeetingMinutesClick,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoFixHigh,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
            ToolbarIconButton(
                contentDescription = stringResource(R.string.composer_emoji),
                onClick = onEmojiClick,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        ToolbarIconButton(
            contentDescription = stringResource(R.string.composer_dictate),
            onClick = onDictateClick,
            enabled = enabled,
        ) {
            OutlinedMicGlyph(
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        if (!expanded) Spacer(Modifier.width(8.dp))
        AnimatedContent(
            targetState = trailing,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "PrimaryActionTransition",
        ) { target ->
            when (target) {
                TrailingAction.Stop -> ComposerCircleButton(
                    contentDescription = stringResource(R.string.composer_stop),
                    container = MaterialTheme.colorScheme.errorContainer,
                    enabled = enabled,
                    onClick = { onStop?.invoke() },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                TrailingAction.Send -> ComposerCircleButton(
                    contentDescription = stringResource(R.string.composer_send),
                    container = MaterialTheme.colorScheme.primary,
                    enabled = enabled && canSend,
                    onClick = onSend,
                ) {
                    ArrowUpwardGlyph(
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                TrailingAction.Voice -> ComposerCircleButton(
                    contentDescription = stringResource(R.string.composer_voice_message),
                    container = MaterialTheme.colorScheme.primaryContainer,
                    enabled = enabled,
                    onClick = onVoiceMessageClick,
                ) {
                    VoiceBarsGlyph(
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerModeButton(
    selectedMode: String,
    enabled: Boolean,
    onModeChange: (String) -> Unit,
    onOpenSkills: (() -> Unit)?,
    onOpenMcp: (() -> Unit)?,
    palette: TeamilyPalette,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val modes = listOf(
        stringResource(R.string.composer_mode_auto),
        stringResource(R.string.composer_mode_instant),
        stringResource(R.string.composer_mode_agentic),
        stringResource(R.string.composer_mode_deep_research),
        stringResource(R.string.composer_mode_fast),
    )
    Box(modifier) {
        ToolbarIconButton(
            contentDescription = stringResource(R.string.composer_mode, selectedMode),
            onClick = { menuExpanded = true },
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Outlined.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            properties = PopupProperties(blurBehindRadius = 16.dp),
        ) {
            modes.forEach { mode ->
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
                        menuExpanded = false
                    },
                )
            }
            if (onOpenSkills != null || onOpenMcp != null) {
                HorizontalDivider(color = palette.border)
            }
            if (onOpenSkills != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.composer_skills)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Build, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onOpenSkills()
                    },
                )
            }
            if (onOpenMcp != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.composer_mcp)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Extension, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onOpenMcp()
                    },
                )
            }
        }
    }
}

@Composable
private fun ComposerCircleButton(
    contentDescription: String,
    container: Color,
    enabled: Boolean,
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
                        enabled = enabled,
                        interactionSource = source,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                    .semantics { this.contentDescription = contentDescription },
            )
            .size(40.dp)
            .clip(CircleShape)
            .background(container)
            .indication(source, LocalIndication.current),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** A 32dp toolbar glyph with a 48dp touch box ([touchTarget48]). */
@Composable
private fun ToolbarIconButton(
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val semanticsMod = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .touchTarget48(
                Modifier
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                        interactionSource = source,
                        indication = null,
                    )
                    .then(semanticsMod),
            )
            .size(32.dp)
            .clip(CircleShape)
            .indication(source, LocalIndication.current),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Draws crisp outlined microphone glyph. */
@Composable
private fun OutlinedMicGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        // mic capsule
        val capsule = Path().apply {
            moveTo(w * 0.38f, h * 0.20f)
            lineTo(w * 0.62f, h * 0.20f)
            lineTo(w * 0.62f, h * 0.52f)
            quadraticTo(w * 0.62f, h * 0.66f, w * 0.50f, h * 0.66f)
            quadraticTo(w * 0.38f, h * 0.66f, w * 0.38f, h * 0.52f)
            close()
        }
        drawPath(capsule, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
        // cradle
        val cradle = Path().apply {
            moveTo(w * 0.26f, h * 0.46f)
            quadraticTo(w * 0.26f, h * 0.76f, w * 0.50f, h * 0.76f)
            quadraticTo(w * 0.74f, h * 0.76f, w * 0.74f, h * 0.46f)
        }
        drawPath(cradle, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
        // stem & base
        drawLine(tint, Offset(w * 0.50f, h * 0.76f), Offset(w * 0.50f, h * 0.90f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.34f, h * 0.90f), Offset(w * 0.66f, h * 0.90f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** Draws crisp up-arrow ↑ Send glyph. */
@Composable
private fun ArrowUpwardGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.4.dp.toPx()
        // vertical stem
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.80f),
            end = Offset(w * 0.5f, h * 0.22f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        // arrow head
        val head = Path().apply {
            moveTo(w * 0.26f, h * 0.44f)
            lineTo(w * 0.5f, h * 0.20f)
            lineTo(w * 0.74f, h * 0.44f)
        }
        drawPath(head, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Two rounded bars, the resting voice control on the collapsed pill. */
@Composable
private fun VoiceBarsGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.4.dp.toPx()
        val gap = 3.dp.toPx()
        val cx = size.width / 2f
        drawLine(
            color = tint,
            start = Offset(cx - gap, size.height * 0.22f),
            end = Offset(cx - gap, size.height * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(cx + gap, size.height * 0.22f),
            end = Offset(cx + gap, size.height * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Preview(name = "Collapsed", showBackground = true, widthDp = 360)
@Composable
private fun SingleRowCapsuleEmptyPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(16.dp)) {
        TeamilyAgentChatBar(value = "")
    }
}

@Preview(name = "Expanded — With Text", showBackground = true, widthDp = 360)
@Composable
private fun SingleRowCapsuleTextPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(16.dp)) {
        TeamilyAgentChatBar(value = "Hi")
    }
}

@Preview(name = "Collapsed — Dark", showBackground = true, widthDp = 360)
@Composable
private fun SingleRowCapsuleDarkPreview() = DuaGenAITheme(darkTheme = true) {
    Box(Modifier.padding(16.dp)) {
        TeamilyAgentChatBar(value = "")
    }
}

@Preview(name = "Toolbar Icon Button", showBackground = true)
@Composable
private fun ToolbarIconButtonPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(16.dp)) {
        ToolbarIconButton(
            contentDescription = "Attach",
            onClick = {},
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
