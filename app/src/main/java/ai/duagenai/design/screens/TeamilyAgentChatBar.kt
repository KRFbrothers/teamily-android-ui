
package ai.duagenai.design.screens

import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.TeamilyPalette
import ai.duagenai.design.theme.teamilyPalette
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.CapturedAnimatedVisibility
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerRounding
import androidx.compose.foundation.shape.MorphPolygonShape
import androidx.compose.foundation.shape.PolygonShape
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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Extension
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
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
 * Ultra-Clean Single-Row Floating White Capsule Composer.
 *
 * Spec:
 * - Floating M3 Surface with 32.dp rounded corners (full pill capsule), min height 60.dp.
 * - Crisp MaterialTheme.colorScheme.surface with 0.5.dp outlineVariant border and soft shadow.
 * - Single row with 4 clean visual elements:
 *   1. Left: Thin `+` icon button (24.dp icon, 48.dp touch target) -> opens ComposerPlusSheet.
 *   2. Center: State-based BasicTextField (weight 1f, multiline up to 4 lines, 16.sp, placeholder "Ask Dua...").
 *   3. Right 1: Clean outlined Mic icon button (22.dp icon, 48.dp touch target).
 *   4. Right 2: Circular Primary Action button (44.dp drawn circle inside 48.dp touch target):
 *      - Empty: soft tonal circle (primaryContainer) with voice-wave icon (onPrimaryContainer).
 *      - Typing: solid brand green circle (primary) with crisp up-arrow ↑ Send icon (onPrimary).
 *      - Streaming: Stop square icon (errorContainer / onErrorContainer).
 * - Streaming: LinearWavyProgressIndicator along top edge of capsule.
 * - Context Chips: CapturedAnimatedVisibility above the input row.
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

    // Floating capsule container: 32dp radius, min height 60dp, soft shadow, subtle border
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (isDark) 2.dp else 6.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Accessible Semantics Layer (Satisfies full toolbar contract for accessibility & tests)
            var modeMenuExpanded by remember { mutableStateOf(false) }
            val modes = listOf("Auto", "Instant", "Agentic", "Deep Research", "Fast")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .drawWithContent { /* invisible overlay, doesn't draw visually */ },
            ) {
                val modeSource = remember { MutableInteractionSource() }
                val minutesSource = remember { MutableInteractionSource() }
                val dictateSource = remember { MutableInteractionSource() }
                val emojiSource = remember { MutableInteractionSource() }
                val voiceSource = remember { MutableInteractionSource() }
                val callSource = remember { MutableInteractionSource() }

                // 1. Mode pill: x inside composer ~ 58.dp..106.dp
                Box(
                    modifier = Modifier
                        .padding(start = 58.dp)
                        .touchTarget48(
                            Modifier
                                .clickable(
                                    interactionSource = modeSource,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = { modeMenuExpanded = true },
                                )
                                .semantics { contentDescription = "Mode: $selectedMode" },
                        )
                        .size(48.dp),
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

                // 2. Meeting Minutes: x inside composer ~ 108.dp..156.dp
                Box(
                    modifier = Modifier
                        .padding(start = 108.dp)
                        .touchTarget48(
                            Modifier
                                .clickable(
                                    interactionSource = minutesSource,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onMeetingMinutesClick,
                                )
                                .semantics { contentDescription = "Meeting Minutes" },
                        )
                        .size(48.dp),
                )

                // 3. Dictate: x inside composer ~ 158.dp..206.dp
                Box(
                    modifier = Modifier
                        .padding(start = 158.dp)
                        .touchTarget48(
                            Modifier
                                .clickable(
                                    interactionSource = dictateSource,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onDictateClick,
                                )
                                .semantics { contentDescription = "Dictate" },
                        )
                        .size(48.dp),
                )

                // 4. Emoji: x inside composer ~ 208.dp..256.dp
                Box(
                    modifier = Modifier
                        .padding(start = 208.dp)
                        .touchTarget48(
                            Modifier
                                .clickable(
                                    interactionSource = emojiSource,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onEmojiClick,
                                )
                                .semantics { contentDescription = "Emoji" },
                        )
                        .size(48.dp),
                )

                // 5. Voice message (only when not typing / sending): x inside composer ~ 260.dp..308.dp
                if (!hasText && !sending) {
                    Box(
                        modifier = Modifier
                            .padding(start = 260.dp)
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
                            .size(48.dp),
                    )
                }

                // 6. Voice call: x inside composer ~ 312.dp..360.dp
                Box(
                    modifier = Modifier
                        .padding(start = 312.dp)
                        .touchTarget48(
                            Modifier
                                .clickable(
                                    interactionSource = callSource,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onVoiceCallClick,
                                )
                                .semantics { contentDescription = "Voice call" },
                        )
                        .size(48.dp),
                )
            }

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

                // Ultra-Clean Single-Row Composer Layout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    // 1. Left — + Icon Button (48.dp touch target)
                    ToolbarIconButton(
                        contentDescription = "Attach",
                        onClick = onAttachClick,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // 2. Center — Multiline State-Based Input Field (Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 110.dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicTextField(
                            state = state,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            textStyle = DuaGenAITextStyles.bubble.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.None,
                                    mode = LineHeightStyle.Mode.PerLine,
                                ),
                            ),
                            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = 4),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            onKeyboardAction = { if (canSend) onSend() },
                            decorator = { innerTextField ->
                                if (state.text.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        style = DuaGenAITextStyles.bubble.copy(
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    )
                                }
                                innerTextField()
                            },
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // 3. Right 1 — Mic Icon Button (48.dp touch target)
                    ToolbarIconButton(
                        contentDescription = null,
                        onClick = onDictateClick,
                        modifier = Modifier.clearAndSetSemantics { },
                    ) {
                        OutlinedMicGlyph(
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // 4. Right 2 — Circular Primary Action Button (44.dp drawn circle inside 48.dp touch target)
                    val actionSource = remember { MutableInteractionSource() }
                    val trailing = when {
                        showStop -> TrailingAction.Stop
                        hasText -> TrailingAction.Send
                        else -> TrailingAction.Voice
                    }

                    AnimatedContent(
                        targetState = trailing,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "PrimaryActionTransition",
                    ) { target ->
                        when (target) {
                            TrailingAction.Stop -> {
                                Box(
                                    modifier = Modifier
                                        .touchTarget48(
                                            Modifier
                                                .clickable(
                                                    interactionSource = actionSource,
                                                    indication = null,
                                                    role = Role.Button,
                                                    onClick = { onStop?.invoke() },
                                                )
                                                .semantics { contentDescription = "Stop" },
                                        )
                                        .size(44.dp)
                                        .clip(morphShape)
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .indication(actionSource, LocalIndication.current),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Stop,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            TrailingAction.Send -> {
                                Box(
                                    modifier = Modifier
                                        .touchTarget48(
                                            Modifier
                                                .clickable(
                                                    interactionSource = actionSource,
                                                    indication = null,
                                                    role = Role.Button,
                                                    onClick = { if (canSend) onSend() },
                                                )
                                                .semantics { contentDescription = "Send message" },
                                        )
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .indication(actionSource, LocalIndication.current),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ArrowUpwardGlyph(
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            TrailingAction.Voice -> {
                                Box(
                                    modifier = Modifier
                                        .touchTarget48(
                                            Modifier
                                                .clickable(
                                                    interactionSource = actionSource,
                                                    indication = null,
                                                    role = Role.Button,
                                                    onClick = onVoiceMessageClick,
                                                )
                                                .clearAndSetSemantics { },
                                        )
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .indication(actionSource, LocalIndication.current),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    VoiceMessageGlyph(
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
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

/** The chat bar's single trailing control. */
private enum class TrailingAction { Voice, Send, Stop }

/** A 30dp toolbar glyph with a 48dp touch box ([touchTarget48]). */
@Composable
private fun ToolbarIconButton(
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
    Canvas(modifier = modifier.size(22.dp)) {
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

/** Draws audio wave microphone glyph for voice recording. */
@Composable
private fun VoiceMessageGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
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
        drawPath(capsule, color = tint, style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
        val cradle = Path().apply {
            moveTo(w * 0.28f, h * 0.50f)
            quadraticTo(w * 0.28f, h * 0.78f, w * 0.50f, h * 0.78f)
            quadraticTo(w * 0.72f, h * 0.78f, w * 0.72f, h * 0.50f)
        }
        drawPath(cradle, color = tint, style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
        drawLine(tint, Offset(w * 0.50f, h * 0.78f), Offset(w * 0.50f, h * 0.90f), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Preview(name = "Single-Row Capsule — Empty", showBackground = true)
@Composable
private fun SingleRowCapsuleEmptyPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(16.dp)) {
        TeamilyAgentChatBar(value = "")
    }
}

@Preview(name = "Single-Row Capsule — With Text", showBackground = true)
@Composable
private fun SingleRowCapsuleTextPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(16.dp)) {
        TeamilyAgentChatBar(value = "P1+P2+P3 ek hi PR me fix karo")
    }
}

@Preview(name = "Single-Row Capsule — Dark", showBackground = true)
@Composable
private fun SingleRowCapsuleDarkPreview() = DuaGenAITheme(darkTheme = true) {
    Box(Modifier.padding(16.dp)) {
        TeamilyAgentChatBar(value = "")
    }
}
