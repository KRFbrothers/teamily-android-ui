package ai.duagenai.design.screens

import ai.duagenai.design.theme.DuaGenAITheme
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ChatBarTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun imeSendAction_sendsMessageAfterTypingIntoInitiallyEmptyChatBar() {
        var sentText: String? = null
        compose.setContent {
            val state = rememberTextFieldState("")
            DuaGenAITheme(darkTheme = false) {
                ChatBar(
                    state = state,
                    onSend = { sentText = state.text.toString() },
                )
            }
        }

        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("Hello Dua")
        compose.waitForIdle()
        field.performImeAction()
        compose.waitForIdle()

        assertEquals("Hello Dua", sentText)
    }

    @Test
    fun stringOverload_propagatesClearAndExternalResetAndLatestOnSend() {
        var text by mutableStateOf("")
        var lastSent = ""
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                ChatBar(
                    value = text,
                    onValueChange = { text = it },
                    onSend = {
                        lastSent = text
                        text = ""
                    },
                )
            }
        }

        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("Draft")
        compose.waitForIdle()
        assertEquals("Draft", text)

        field.performTextClearance()
        compose.waitForIdle()
        assertEquals("", text)

        field.performTextInput("Second message")
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Send message").performClick()
        compose.waitForIdle()

        assertEquals("Second message", lastSent)
        assertEquals("", text)
    }

    @Test
    fun tappingUpperCapsuleArea_focusesTextFieldWithoutTriggeringPhantomButtons() {
        var attachCount = 0
        var dictateCount = 0
        var voiceCount = 0
        compose.setContent {
            val state = rememberTextFieldState("")
            DuaGenAITheme(darkTheme = false) {
                ChatBar(
                    state = state,
                    placeholder = "Message David Park\u2026",
                    onAttachClick = { attachCount++ },
                    onDictateClick = { dictateCount++ },
                    onVoiceMessageClick = { voiceCount++ },
                )
            }
        }

        val fieldNode = compose.onNode(hasSetTextAction())
        val bounds = fieldNode.fetchSemanticsNode().boundsInRoot
        // Tap near the top edge of the text field box (inside the capsule)
        fieldNode.performTouchInput {
            click(Offset(bounds.width * 0.5f, 4f))
        }
        compose.waitForIdle()

        fieldNode.assertIsFocused()
        assertEquals(0, attachCount)
        assertEquals(0, dictateCount)
        assertEquals(0, voiceCount)
    }

    @Test
    fun placeholder_displaysCallerPlaceholderVisiblyWithNonZeroBounds() {
        compose.setContent {
            val state = rememberTextFieldState("")
            DuaGenAITheme(darkTheme = false) {
                ChatBar(
                    state = state,
                    placeholder = "Message David Park\u2026",
                )
            }
        }

        val node = compose.onNodeWithText("Message David Park\u2026")
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue("Placeholder width must be visible", bounds.width > 0f)
        assertTrue("Placeholder height must be visible", bounds.height > 0f)
    }

    @Test
    fun multiLineText_expandsAndStillSendsViaButton() {
        var sentText: String? = null
        compose.setContent {
            val state = rememberTextFieldState("")
            DuaGenAITheme(darkTheme = false) {
                ChatBar(
                    state = state,
                    onSend = { sentText = state.text.toString() },
                )
            }
        }

        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("First line\nSecond line\nThird line")
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Send message").performClick()
        compose.waitForIdle()

        assertEquals("First line\nSecond line\nThird line", sentText)
    }

    @Test
    fun contextChipDismiss_hasMinimum48dpTouchTargetAndCallsCallback() {
        var dismissed: String? = null
        compose.setContent {
            val state = rememberTextFieldState("")
            DuaGenAITheme(darkTheme = false) {
                ChatBar(
                    state = state,
                    contextChips = listOf("Memory"),
                    onDismissChip = { dismissed = it },
                )
            }
        }

        val min = 48f * compose.density.density - 1f
        val close = compose.onNodeWithContentDescription("Remove Memory")
        val bounds = close.fetchSemanticsNode().boundsInRoot
        assertTrue("Dismiss touch width >= 48dp", bounds.width >= min)
        assertTrue("Dismiss touch height >= 48dp", bounds.height >= min)

        close.performClick()
        compose.waitForIdle()
        assertEquals("Memory", dismissed)
    }
}
