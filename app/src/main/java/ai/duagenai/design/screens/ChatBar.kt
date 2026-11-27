package ai.duagenai.design.screens

import ai.duagenai.design.R
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.TeamilySansFontFamily
import ai.duagenai.design.theme.rememberReduceMotion
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GraphicEq
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Adaptive Gemini-style Floating Capsule ChatBar.
 *
 * - Single-row capsule (`+` | input | `Mic` | `Voice/Send/Stop`) expands cleanly
 *   into a multi-line card without dropping focus or keyboard via [movableContentOf].
 * - Width gently widens when focused, typing, multi-line, or IME is visible, and
 *   collapses cleanly during streaming with a wavy progress bar and Stop control.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatBar(
    state: TextFieldState,
    hasMessages: Boolean = false,
    onSend: () -> Unit = {},
    placeholder: String = "",
    enabled: Boolean = true,
    onAttachClick: () -> Unit = {},
    onDictateClick: () -> Unit = {},
    onVoiceMessageClick: () -> Unit = {},
    voiceContentDescription: String? = null,
    modifier: Modifier = Modifier,
    /** A reply is in flight. With [onStop] the trailing control becomes Stop. */
    sending: Boolean = false,
    onStop: (() -> Unit)? = null,
    contextChips: List<String> = emptyList(),
    onDismissChip: (String) -> Unit = {},
) {
    val palette = teamilyPalette()
    val isDark = DuaGenAITheme.isDark
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val attachCd = stringResource(R.string.chat_attach_cd)
    val dictateCd = stringResource(R.string.chat_dictate_cd)
    val sendCd = stringResource(R.string.chat_send_cd)
    val stopCd = stringResource(R.string.chat_stop_cd)
    val voiceCd = voiceContentDescription ?: stringResource(R.string.voice_note_cd)

    val hasText by remember(state) {
        computedStateOf { state.text.isNotBlank() }
    }
    val canSend = enabled && hasText && !sending
    val showStop = sending && onStop != null

    var lineCount by remember(state) { mutableIntStateOf(1) }
    var isFocused by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    // Flicker-free multi-line hysteresis: enter when wrapping (>1 line) or newline,
    // and only collapse back after backing below the entry length threshold.
    var isMultiLine by remember(state) { mutableStateOf(false) }
    var collapseThreshold by remember(state) { mutableIntStateOf(0) }
    val textStr = state.text.toString()
    val effectiveMultiLine = isMultiLine && textStr.isNotEmpty()
    LaunchedEffect(textStr, lineCount) {
        if (textStr.isEmpty()) {
            isMultiLine = false
            collapseThreshold = 0
            lineCount = 1
        } else if (!isMultiLine && (lineCount > 1 || textStr.contains('\n'))) {
            isMultiLine = true
            collapseThreshold = if (textStr.contains('\n')) 0 else (textStr.length - 6).coerceAtLeast(0)
        } else if (isMultiLine && lineCount <= 1 && !textStr.contains('\n') && textStr.length <= collapseThreshold) {
            isMultiLine = false
            collapseThreshold = 0
        }
    }

    // When IME closes and text is empty, clear focus so width smoothly shrinks back
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && state.text.isEmpty()) {
            focusManager.clearFocus()
        }
    }

    val isWide = (isImeVisible || isFocused || effectiveMultiLine || hasText) && !sending
    val horizontalMargin by animateDpAsState(
        targetValue = if (isWide) 12.dp else 20.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "capsuleWidth",
    )

    val targetCornerRadius = if (effectiveMultiLine) 28.dp else 40.dp
    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "capsuleCornerRadius",
    )
    val containerShape = RoundedCornerShape(animatedCornerRadius)
    val containerBorder = if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    val shadowElevation = if (isDark) 4.dp else 8.dp

    val circlePolygon = remember { PolygonShape.circle() }
    val starPolygon = remember {
        PolygonShape.star(
            numPoints = 8,
            innerRadiusRatio = 0.82f,
            outerRounding = CornerRounding.fraction(0.5f),
        )
    }
    val reduceMotion = rememberReduceMotion()
    val morphProgress by if (reduceMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "StopMorphTransition")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "StopMorphProgress",
        )
    }
    val morphShape = remember(circlePolygon, starPolygon) {
        MorphPolygonShape(circlePolygon, starPolygon) { morphProgress }
    }

    val scrollState = rememberScrollState()
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface

    val resolvedPlaceholder = when {
        placeholder.isNotBlank() -> placeholder
        !hasMessages && !sending && (isFocused || isImeVisible) -> stringResource(R.string.chat_placeholder_empty_focused)
        !hasMessages && !sending -> stringResource(R.string.chat_placeholder_empty)
        else -> stringResource(R.string.chat_placeholder_active)
    }

    fun handleSend() {
        if (!canSend) return
        onSend()
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Keep latest parameters visible inside the remembered movableContentOf slot
    val currentEnabled by rememberUpdatedState(enabled)
    val currentSurfaceColor by rememberUpdatedState(surfaceColor)
    val currentMultiLine by rememberUpdatedState(effectiveMultiLine)
    val currentHandleSend by rememberUpdatedState { handleSend() }

    val movableTextField = remember(state) {
        movableContentOf { fieldModifier: Modifier, hint: String ->
            val topFadeModifier = if (currentMultiLine && scrollState.value > 0) {
                Modifier.drawWithContent {
                    drawContent()
                    val fadeHeight = 16.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(currentSurfaceColor, Color.Transparent),
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
                contentAlignment = if (currentMultiLine) Alignment.TopStart else Alignment.CenterStart,
            ) {
                BasicTextField(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    enabled = currentEnabled,
                    scrollState = scrollState,
                    textStyle = DuaGenAITextStyles.bubble.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = TeamilySansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
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
                        imeAction = if (currentMultiLine) ImeAction.Default else ImeAction.Send,
                    ),
                    onKeyboardAction = { currentHandleSend() },
                    onTextLayout = { getResult ->
                        getResult()?.let { lineCount = it.lineCount }
                    },
                    decorator = { innerTextField ->
                        // Full-height Box inside decorator ensures taps anywhere vertically in the
                        // capsule hit BasicTextField directly and place focus/cursor properly.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            contentAlignment = if (currentMultiLine) Alignment.TopStart else Alignment.CenterStart,
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
                                            fontSize = 16.sp,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
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

    Surface(
        modifier = modifier
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

            CapturedAnimatedVisibility(
                visible = contextChips.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    contextChips.forEach { chipText ->
                        val removeCd = stringResource(R.string.chat_remove_chip_cd, chipText)
                        Surface(
                            onClick = { onDismissChip(chipText) },
                            enabled = enabled,
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.semantics { contentDescription = removeCd },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = actionSource,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = { onStop?.invoke() },
                                    )
                                    .semantics { contentDescription = stopCd },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
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
                        }
                        TrailingAction.Send -> {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = canSend,
                                        interactionSource = actionSource,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = { handleSend() },
                                    )
                                    .semantics { contentDescription = sendCd },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
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
                        }
                        TrailingAction.Voice -> {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = enabled,
                                        interactionSource = actionSource,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = onVoiceMessageClick,
                                    )
                                    .semantics { contentDescription = voiceCd },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
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
            }

            val plusButton: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onAttachClick,
                        )
                        .semantics { contentDescription = attachCd },
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

            val micButton: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onDictateClick,
                        )
                        .semantics { contentDescription = dictateCd },
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

            if (!effectiveMultiLine) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    plusButton()
                    Spacer(Modifier.width(4.dp))
                    movableTextField(Modifier.weight(1f), resolvedPlaceholder)
                    if (!sending) {
                        micButton()
                    }
                    actionButton()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    movableTextField(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp, max = 188.dp)
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                        resolvedPlaceholder,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        plusButton()
                        Spacer(Modifier.weight(1f))
                        if (!sending) {
                            micButton()
                        }
                        actionButton()
                    }
                }
            }
        }
    }
}

/**
 * String-controlled overload for callers holding external string state and previews.
 */
@Composable
fun ChatBar(
    value: String,
    onValueChange: (String) -> Unit = {},
    hasMessages: Boolean = false,
    onSend: () -> Unit = {},
    placeholder: String = "",
    enabled: Boolean = true,
    onAttachClick: () -> Unit = {},
    onDictateClick: () -> Unit = {},
    onVoiceMessageClick: () -> Unit = {},
    voiceContentDescription: String? = null,
    modifier: Modifier = Modifier,
    sending: Boolean = false,
    onStop: (() -> Unit)? = null,
    contextChips: List<String> = emptyList(),
    onDismissChip: (String) -> Unit = {},
) {
    val state = rememberTextFieldState(initialText = value)
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    var lastEmittedValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value != lastEmittedValue && state.text.toString() != value) {
            state.edit { replace(0, length, value) }
        }
        lastEmittedValue = value
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .collect { newText ->
                lastEmittedValue = newText
                if (newText != currentValue) {
                    currentOnValueChange(newText)
                }
            }
    }
    ChatBar(
        state = state,
        hasMessages = hasMessages,
        onSend = onSend,
        placeholder = placeholder,
        enabled = enabled,
        onAttachClick = onAttachClick,
        onDictateClick = onDictateClick,
        onVoiceMessageClick = onVoiceMessageClick,
        voiceContentDescription = voiceContentDescription,
        modifier = modifier,
        sending = sending,
        onStop = onStop,
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
