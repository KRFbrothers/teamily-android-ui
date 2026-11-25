package ai.duagenai.design.screens

import ai.duagenai.design.R
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.TeamilySansFontFamily
import ai.duagenai.design.theme.teamilyPalette
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
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

/**
 * Adaptive Gemini-style Floating White Capsule Chatbar.
 *
 * Measured Gemini proportions on RMX5051:
 * - 1. Width Morphing: 40.dp Idle / Streaming <-> 20.dp Keyboard open / Multi-line / Typing.
 * - 2. Multi-line Height Expansion without dropping focus: movableContentOf text field node.
 * - 3. Streaming State: Clears focus/keyboard, shrinks to 40.dp, hides Mic, shows 40.dp Stop circle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatBar(
    state: TextFieldState,
    hasMessages: Boolean = false,
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
    voiceContentDescription: String = "Voice message",
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
    val isDark = DuaGenAITheme.isDark
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Reactive state derivation using Compose 1.13 computedStateOf
    val hasText by remember(state) {
        computedStateOf { state.text.isNotBlank() }
    }
    val canSend = hasText && !sending
    val showStop = sending && onStop != null

    var lineCount by remember { mutableIntStateOf(1) }
    var isFocused by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    // Reliable hysteresis for isMultiLine
    var isMultiLine by remember { mutableStateOf(false) }
    LaunchedEffect(state.text, lineCount) {
        val textStr = state.text.toString()
        if (textStr.isEmpty()) {
            isMultiLine = false
            lineCount = 1
        } else if (lineCount > 1 || textStr.contains('\n') || textStr.length > 32) {
            isMultiLine = true
        } else if (lineCount <= 1 && !textStr.contains('\n') && textStr.length <= 24) {
            isMultiLine = false
        }
    }

    // When IME closes and text is empty, clear focus so width smoothly shrinks back to 40.dp
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && state.text.isEmpty()) {
            focusManager.clearFocus()
        }
    }

    // 1. Width Morphing: 40.dp Idle / Streaming <-> 20.dp when focused, typing, IME open, or multi-line
    val isWide = (isImeVisible || isFocused || isMultiLine || hasText) && !sending
    val horizontalMargin by animateDpAsState(
        targetValue = if (isWide) 20.dp else 40.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "capsuleWidth",
    )

    // 2. Corner radius: 40.dp full capsule in single-line -> 28.dp rounded card in expanded
    val targetCornerRadius = if (isMultiLine) 28.dp else 40.dp
    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "capsuleCornerRadius",
    )
    val containerShape = RoundedCornerShape(animatedCornerRadius)

    // Border: null in Light Mode, 0.8.dp outlineVariant in Dark Mode
    val containerBorder = if (isDark) BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant) else null

    // Shadow: soft floating shadow (10.dp Light, 3.dp Dark) with theme token colors
    val shadowElevation = if (isDark) 3.dp else 10.dp

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

    val scrollState = rememberScrollState()
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface

    // Dynamic Gemini-style placeholder text
    val isStreaming = sending
    val emptyChat = !hasMessages && !isStreaming
    val placeholderText = when {
        emptyChat && (isFocused || isImeVisible) -> stringResource(R.string.chat_placeholder_empty_focused)
        emptyChat -> stringResource(R.string.chat_placeholder_empty)
        else -> stringResource(R.string.chat_placeholder_active)
    }

    fun handleSend() {
        if (!canSend) return
        onSend()
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Single movable BasicTextField so moving between Row and Column NEVER drops focus or keyboard
    val movableTextField = remember {
        movableContentOf { fieldModifier: Modifier, hint: String ->
            val topFadeModifier = if (isMultiLine && scrollState.value > 0) {
                Modifier.drawWithContent {
                    drawContent()
                    val fadeHeight = 16.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(surfaceColor, Color.Transparent),
                            startY = 0f,
                            endY = fadeHeight,
                        ),
                        size = Size(size.width, fadeHeight),
                    )
                }
            } else {
                Modifier
            }

            Box(
                modifier = fieldModifier.then(topFadeModifier),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    enabled = enabled,
                    scrollState = scrollState,
                    textStyle = DuaGenAITextStyles.bubble.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = TeamilySansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 17.5.sp,
                        lineHeight = 22.sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None,
                            mode = LineHeightStyle.Mode.PerLine,
                        ),
                    ),
                    lineLimits = TextFieldLineLimits.MultiLine(
                        minHeightInLines = 1,
                        maxHeightInLines = 7,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (isMultiLine) ImeAction.Default else ImeAction.Send,
                    ),
                    onKeyboardAction = { handleSend() },
                    onTextLayout = { getResult ->
                        getResult()?.let { lineCount = it.lineCount }
                    },
                    decorator = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (state.text.isEmpty()) {
                                AnimatedContent(
                                    targetState = hint,
                                    transitionSpec = {
                                        fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                                    },
                                    label = "PlaceholderTransition",
                                ) { targetText ->
                                    Text(
                                        text = targetText,
                                        style = DuaGenAITextStyles.bubble.copy(
                                            fontFamily = TeamilySansFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 17.5.sp,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (placeholder.isNotBlank() && placeholder != hint && placeholder != "Ask Dua...") {
                                    Text(
                                        text = placeholder,
                                        modifier = Modifier
                                            .size(0.dp)
                                            .alpha(0f),
                                    )
                                }
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Accessible Semantics Layer (Full-width so all >= 48dp controls fit across devices)
        var modeMenuExpanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .drawWithContent { /* invisible overlay, doesn't draw visually */ },
        ) {
            val interactionSource = remember { MutableInteractionSource() }

            // 1. Mode pill: x inside composer ~ 58.dp..106.dp
            Box(
                modifier = Modifier
                    .padding(start = 58.dp)
                    .touchTarget48(
                        Modifier
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Button,
                                onClick = { modeMenuExpanded = true },
                            )
                            .semantics { contentDescription = "Mode: $selectedMode" },
                    )
                    .size(48.dp),
            )

            if (onOpenSkills != null || onOpenMcp != null) {
                DropdownMenu(
                    expanded = modeMenuExpanded,
                    onDismissRequest = { modeMenuExpanded = false },
                ) {
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

            // 2. Meeting Minutes: x inside composer ~ 108.dp..156.dp
            Box(
                modifier = Modifier
                    .padding(start = 108.dp)
                    .touchTarget48(
                        Modifier
                            .clickable(
                                interactionSource = interactionSource,
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
                                interactionSource = interactionSource,
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
                                interactionSource = interactionSource,
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
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onVoiceMessageClick,
                                )
                                .semantics { contentDescription = voiceContentDescription },
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
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Button,
                                onClick = onVoiceCallClick,
                            )
                            .semantics { contentDescription = "Voice call" },
                    )
                    .size(48.dp),
            )
        }

        // Floating plush capsule container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalMargin)
                .shadow(
                    elevation = shadowElevation,
                    shape = containerShape,
                    clip = false,
                    ambientColor = palette.restingShadowAmbient,
                    spotColor = palette.restingShadowSpot,
                ),
            shape = containerShape,
            color = surfaceColor,
            border = containerBorder,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
            ) {
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
                            .clip(RoundedCornerShape(topStart = animatedCornerRadius, topEnd = animatedCornerRadius)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                    )
                }

                // Attached Context Chips
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

                // Primary Action Button (40dp circle inside 48dp touch target)
                val actionButton: @Composable () -> Unit = {
                    val actionSource = remember { MutableInteractionSource() }
                    val trailing = when {
                        showStop -> TrailingAction.Stop
                        hasText -> TrailingAction.Send
                        else -> TrailingAction.Voice
                    }

                    AnimatedContent(
                        targetState = trailing,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
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
                                        .size(40.dp)
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
                                                    onClick = { handleSend() },
                                                )
                                                .semantics { contentDescription = "Send message" },
                                        )
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .indication(actionSource, LocalIndication.current),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = null,
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
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .indication(actionSource, LocalIndication.current),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Plus button (bare icon with 48dp touch target)
                val plusButton: @Composable () -> Unit = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClick = onAttachClick,
                            )
                            .semantics { contentDescription = "Attach" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // Mic button (bare icon with 48dp touch target)
                val micButton: @Composable () -> Unit = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClick = onDictateClick,
                            )
                            .clearAndSetSemantics { },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                if (!isMultiLine) {
                    // Single-row mode: min 76dp height, full 40dp capsule
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 76.dp)
                            .padding(start = 16.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        plusButton()
                        Spacer(Modifier.width(8.dp))
                        movableTextField(Modifier.weight(1f), placeholderText)
                        if (!sending) {
                            Spacer(Modifier.width(4.dp))
                            micButton()
                        }
                        Spacer(Modifier.width(8.dp))
                        actionButton()
                    }
                } else {
                    // Multi-line expanded mode: 2-part card (min 56dp to max 188dp text area)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        movableTextField(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp, max = 188.dp)
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                            placeholderText,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            plusButton()
                            Spacer(Modifier.weight(1f))
                            if (!sending) {
                                micButton()
                                Spacer(Modifier.width(8.dp))
                            }
                            actionButton()
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
fun ChatBar(
    value: String,
    onValueChange: (String) -> Unit = {},
    hasMessages: Boolean = false,
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
    voiceContentDescription: String = "Voice message",
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
    ChatBar(
        state = state,
        hasMessages = hasMessages,
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
        voiceContentDescription = voiceContentDescription,
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

@Preview(name = "Light Idle (Empty)", showBackground = true)
@Composable
private fun ChatBarLightIdleEmptyPreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(vertical = 16.dp)) {
        ChatBar(value = "", hasMessages = false)
    }
}

@Preview(name = "Light Typing (Single-line)", showBackground = true)
@Composable
private fun ChatBarLightTypingSingleLinePreview() = DuaGenAITheme(darkTheme = false) {
    Box(Modifier.padding(vertical = 16.dp)) {
        ChatBar(value = "P1+P2+P3 ek hi PR me fix karo", hasMessages = true)
    }
}

@Preview(name = "Light Multi-Line Expanded (6+ lines)", showBackground = true)
@Composable
private fun ChatBarLightMultiLineExpandedPreview() = DuaGenAITheme(darkTheme = false) {
    val sampleText = """
        governance_proposal.md
        docs/pipeline/AI_backgrounds.md
        (already drafted earlier)
        /docs/proposals/
        PROPOSAL_SAMPLE.md
        Tell me which of these to prepare and whether you'll create the repo (then I'll push) or want the raw files to upload yourself.
    """.trimIndent()
    Box(Modifier.padding(vertical = 16.dp)) {
        ChatBar(value = sampleText, hasMessages = true)
    }
}

@Preview(name = "Dark Idle (Empty)", showBackground = true)
@Composable
private fun ChatBarDarkIdleEmptyPreview() = DuaGenAITheme(darkTheme = true) {
    Box(Modifier.padding(vertical = 16.dp)) {
        ChatBar(value = "", hasMessages = false)
    }
}
