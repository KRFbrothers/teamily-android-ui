package ai.duagenai.design.screens

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

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
}
