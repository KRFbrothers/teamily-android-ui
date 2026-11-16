package ai.duagenai.design.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyShapes

/* ============================================================================
 *  Signed-out read-only — the v25 state from m3-prototype_v25.
 *
 *  The prototype does not invent a new page. After Sign out is confirmed, the
 *  phone that is already on screen gets a veil (`rgba(10, 10, 12, .42)` plus a
 *  1.5px blur) and a bottom bar. The bar is the only control a finger or a
 *  screen reader can reach. "Sign back in" clears the state.
 *
 *  This file is that state as a stateless overlay. DuaGenAINavHost wraps the
 *  drawer in it. When [SignedOutUiState.readOnly] is false, only [content]
 *  is drawn, so the five tabs stay as they are.
 *
 *  The HTML bar says the prototype cannot really end a session. This app can,
 *  so the default sentence says the device is read-only until sign-in. The
 *  layout (inset, radius, inverse ink bar, pill action) follows the prototype.
 * ==========================================================================*/

/**
 * What the signed-out bar shows.
 *
 * Pass a new instance from a ViewModel later. The screen does not keep its own
 * copy of this.
 */
data class SignedOutUiState(
    /** When false, [SignedOutReadOnlyScreen] draws [content] with no veil. */
    val readOnly: Boolean = true,
    /** The bar's status line. Announced as a polite live region. */
    val message: String = "Signed out. This device is read-only until you sign back in.",
    /** Label of the only enabled control. */
    val signBackInLabel: String = "Sign back in",
)

@Composable
fun SignedOutReadOnlyScreen(
    state: SignedOutUiState,
    onSignBackIn: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = { SignedOutBackdrop() },
) {
    val spacing = LocalDuaGenAISpacing.current
    val focusManager = LocalFocusManager.current

    // A field that was focused before sign-out must not keep the keyboard.
    LaunchedEffect(state.readOnly) {
        if (state.readOnly) focusManager.clearFocus(force = true)
    }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (state.readOnly) {
                        // 1.5px in the prototype. Drop the subtree from the
                        // accessibility tree so TalkBack cannot land on a
                        // control the veil has locked.
                        Modifier.blur(1.5.dp).clearAndSetSemantics {}
                    } else {
                        Modifier
                    },
                ),
        ) {
            content()
        }

        if (state.readOnly) {
            Box(
                Modifier
                    .matchParentSize()
                    .clearAndSetSemantics {}
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
            )
            SignedOutBar(
                state = state,
                onSignBackIn = onSignBackIn,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(spacing.gutter),
            )
        }
    }
}

@Composable
private fun SignedOutBar(
    state: SignedOutUiState,
    onSignBackIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    // Light: ink bar (#182B23) on canvas text. Dark: the inverse pair.
    // Those are inverseSurface / inverseOnSurface in DuaGenAITheme.
    Surface(
        modifier = modifier,
        shape = TeamilyShapes.lg,
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.gutter, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = state.message,
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = DuaGenAITextStyles.button,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
            SignBackInButton(
                label = state.signBackInLabel,
                onClick = onSignBackIn,
            )
        }
    }
}

@Composable
private fun SignBackInButton(label: String, onClick: () -> Unit) {
    val spacing = LocalDuaGenAISpacing.current
    val interaction = remember { MutableInteractionSource() }
    // Drawn at the prototype's 6×12 padding. The touch box is 48dp and does
    // not grow the row — same helper the rest of the kit uses.
    Text(
        text = label,
        modifier = Modifier
            .touchTarget48(
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
            )
            .clip(DuaGenAITheme.shapes.pill)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DuaGenAITheme.shapes.pill)
            .indication(interaction, LocalIndication.current)
            .padding(horizontal = spacing.md, vertical = 6.dp),
        style = DuaGenAITextStyles.meta.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * Stand-in under the veil when a caller has not passed the real screen.
 *
 * The preview passes [HomeScreen] instead, so Studio shows the lock over the
 * existing Agent home.
 */
@Composable
private fun SignedOutBackdrop() {
    val spacing = LocalDuaGenAISpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = spacing.gutter, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = "Dua Gen AI",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Chats, agents, and settings stay on screen. Nothing here accepts input.",
            style = DuaGenAITextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LockedRow(title = "Chats", subtitle = "Your conversations")
        LockedRow(title = "Agents", subtitle = "Agents you manage")
    }
}

@Composable
private fun LockedRow(title: String, subtitle: String) {
    val spacing = LocalDuaGenAISpacing.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DuaGenAITheme.shapes.card,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, DuaGenAITheme.colors.hairline),
    ) {
        Column(Modifier.padding(spacing.lg)) {
            Text(
                text = title,
                style = DuaGenAITextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = DuaGenAITextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Signed out — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignedOutLight() = DuaGenAITheme(darkTheme = false) {
    SignedOutReadOnlyScreen(
        state = SignedOutUiState(),
        onSignBackIn = {},
    ) {
        HomeScreen(mode = HomeMode.Agent)
    }
}

@Preview(name = "Signed out — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignedOutDark() = DuaGenAITheme(darkTheme = true) {
    SignedOutReadOnlyScreen(
        state = SignedOutUiState(),
        onSignBackIn = {},
    ) {
        HomeScreen(mode = HomeMode.Agent)
    }
}
