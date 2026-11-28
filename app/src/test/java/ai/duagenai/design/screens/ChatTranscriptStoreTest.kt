package ai.duagenai.design.screens

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The agent reply parser.
 *
 * The servers stream `data: {"type":"text","delta":"..."}` — `delta` is the
 * field name in the backend's wire format (`apps/api/src/core/sse.mjs`) and in
 * the engine (`app.py`). The parser used to read `text` only, so every streamed
 * answer came back as an empty string with no error. These tests pin the real
 * field name so that regression cannot come back silently.
 */
@RunWith(AndroidJUnit4::class)
// Robolectric 4.12.2 has no SDK 36 runtime; without this pin it would pick
// targetSdk (36) and fail. Same pin as DuaGenAINavHostTest.
@Config(sdk = [34])
class ChatTranscriptStoreTest {

    @Test
    fun `reads delta from a streamed text event`() {
        val body = "event: text\ndata: {\"type\":\"text\",\"delta\":\"Namaste\"}\n\n"
        assertEquals("Namaste", parseAgentReply(body))
    }

    @Test
    fun `concatenates every delta in order`() {
        val body = buildString {
            append("event: text\ndata: {\"type\":\"text\",\"delta\":\"Dua \"}\n\n")
            append("event: text\ndata: {\"type\":\"text\",\"delta\":\"Gen \"}\n\n")
            append("event: text\ndata: {\"type\":\"text\",\"delta\":\"AI\"}\n\n")
        }
        assertEquals("Dua Gen AI", parseAgentReply(body))
    }

    @Test
    fun `ignores thinking events and keeps only the answer`() {
        val body = buildString {
            append("event: thinking\ndata: {\"type\":\"thinking\",\"delta\":\"soch raha hoon\"}\n\n")
            append("event: text\ndata: {\"type\":\"text\",\"delta\":\"Jawab\"}\n\n")
        }
        assertEquals("Jawab", parseAgentReply(body))
    }

    @Test
    fun `still accepts the older text key`() {
        val body = "event: text\ndata: {\"type\":\"text\",\"text\":\"purana server\"}\n\n"
        assertEquals("purana server", parseAgentReply(body))
    }

    @Test
    fun `skips heartbeats and the done marker`() {
        val body = buildString {
            append(": heartbeat\n\n")
            append("event: text\ndata: {\"type\":\"text\",\"delta\":\"ok\"}\n\n")
            append("data: [DONE]\n\n")
        }
        assertEquals("ok", parseAgentReply(body))
    }

    @Test
    fun `falls back to a plain json body when there is no data line`() {
        assertEquals("plain", parseAgentReply("{\"text\":\"plain\"}"))
    }

    @Test
    fun `chat markdown turns stars into bold bullets and code`() {
        val styled = formatChatMarkdown("* **Heading:** use `code`\n- next")

        assertEquals("• Heading: use code\n• next", styled.text)
        assertFalse(styled.text.contains("*"))
        assertFalse(styled.text.contains("`"))
        val bold = styled.spanStyles.first { it.item.fontWeight == FontWeight.Bold }
        assertEquals("Heading:", styled.text.substring(bold.start, bold.end))
        val code = styled.spanStyles.first { it.item.fontFamily == FontFamily.Monospace }
        assertEquals("code", styled.text.substring(code.start, code.end))
    }

    @Test
    fun `chat markdown leaves ordinary text alone`() {
        val styled = formatChatMarkdown("Hello — 2 * 3")
        assertEquals("Hello — 2 * 3", styled.text)
        assertTrue(styled.spanStyles.isEmpty())
    }

    @Test
    fun `a read timeout is not an offline failure`() {
        val timeout = HttpTransport { _, _, _, _ -> throw SocketTimeoutException("Read timed out") }
        val timedOut = runCatching {
            JsonHttpClient(transport = timeout, label = "Chat request")
                .requestOrThrow("POST", "https://api.example/api/chat", "{}")
        }.exceptionOrNull() as ApiError

        assertTrue(timedOut.timedOut)
        assertTrue(timedOut.retryable)
        assertEquals(null, timedOut.status)
        assertEquals("Chat request: the response took too long.", timedOut.userMessage)
        assertFalse(timedOut.userMessage.contains("Read timed out"))

        val offline = HttpTransport { _, _, _, _ -> throw java.io.IOException("no route to host") }
        val disconnected = runCatching {
            JsonHttpClient(transport = offline, label = "Chat request")
                .requestOrThrow("POST", "https://api.example/api/chat", "{}")
        }.exceptionOrNull() as ApiError

        assertFalse(disconnected.timedOut)
        assertEquals("Chat request: no connection.", disconnected.userMessage)
        assertEquals(180_000, CHAT_READ_TIMEOUT_MS)
    }

    @Test
    fun `a streamed read timeout uses the same timeout error`() {
        val timeout = HttpTransport { _, _, _, _ -> throw SocketTimeoutException("timeout") }
        val error = runCatching {
            JsonHttpClient(transport = timeout, label = "Chat request")
                .stream("POST", "https://api.example/api/chat", "{}") { }
        }.exceptionOrNull() as ApiError

        assertTrue(error.timedOut)
        assertEquals("Chat request: the response took too long.", error.userMessage)
    }

    /**
     * Cancelling a stream must not send the message a second time.
     *
     * `streamMessage` falls back to a non-streaming POST when the stream fails
     * before any chunk arrives. A `CancellationException` is not a failure — it
     * means the caller went away (screen left, scope cleared) — so it has to be
     * rethrown before that fallback. Without the guard the user's abandoned
     * message was posted again, and the thread showed a duplicate reply.
     */
    @Test
    fun `a cancelled stream is rethrown and never falls back to a second post`() {
        var posts = 0
        val cancelling = HttpTransport { _, _, _, _ ->
            posts++
            throw CancellationException("scope cleared")
        }
        val source = RemoteChatDataSource(
            client = JsonHttpClient(transport = cancelling, label = "Chat request"),
        )

        val thrown = runCatching {
            runBlocking {
                source.streamMessage(chatId = "chat_1", text = "hello", skills = emptySet())
            }
        }.exceptionOrNull()

        assertTrue(
            "the CancellationException must reach the caller, got $thrown",
            thrown is CancellationException,
        )
        assertEquals("exactly one POST — no fallback retry", 1, posts)
    }

    /**
     * The same guard on the non-cancellation path: a real failure before any
     * chunk still falls back, so the fix above did not disable the fallback.
     */
    @Test
    fun `a genuine stream failure still falls back to one non-streaming post`() {
        var posts = 0
        val failing = HttpTransport { _, _, _, _ ->
            posts++
            if (posts == 1) throw java.io.IOException("stream broke")
            "{\"text\":\"fallback answer\"}"
        }
        val source = RemoteChatDataSource(
            client = JsonHttpClient(transport = failing, label = "Chat request"),
        )

        val reply = runBlocking {
            source.streamMessage(chatId = "chat_1", text = "hello", skills = emptySet())
        }

        assertEquals("fallback answer", reply)
        assertEquals("stream POST plus one fallback POST", 2, posts)
    }
}
