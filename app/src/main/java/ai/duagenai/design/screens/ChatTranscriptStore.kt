package ai.duagenai.design.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/* ============================================================================
 *  ChatTranscriptStore — where a thread's messages LIVE.
 *
 *  WHY THIS FILE EXISTS (audit blocker: "chat transcript in rememberSaveable")
 *  ------------------------------------------------------------------------
 *  `ChatScreen` used to hold the whole message list in `rememberSaveable`. That
 *  puts EVERY message of EVERY visited thread into the Activity's saved-instance
 *  Bundle. A Bundle is a Binder transaction with a ~1 MB budget: a long
 *  conversation — or simply a few threads' worth — overflows it and the process
 *  dies with `TransactionTooLargeException` while backgrounding, which the user
 *  sees as "the app crashes when I switch away".
 *
 *  A transcript is APPLICATION STATE, not UI state. It belongs in something that
 *  is RETAINED across a configuration change and is never serialised into the
 *  Bundle. A [ViewModel] is exactly that: it survives rotation and theme
 *  changes, it is cleared when the host really goes away, and nothing in it is
 *  written to the Bundle.
 *
 *  WHAT STAYS IN rememberSaveable
 *  ------------------------------
 *  Only the DRAFT — one short string per thread. That is the state a user would
 *  actually be upset to lose on rotation, and it is small enough that the Bundle
 *  budget is a non-issue. The typing indicator is transient, so it is plain
 *  `remember`: it must not come back after a rotation, because the coroutine
 *  that would clear it died with the old composition.
 *
 *  THREAD SAFETY
 *  -------------
 *  Every mutation goes through a `MutableStateFlow` update, so two threads
 *  (Agent's and a chat's) appending at once cannot lose a message.
 * ==========================================================================*/

/**
 * The message store for every open thread, keyed by chat id.
 *
 * Created once by the nav host (`viewModel()`) and passed down, so all threads
 * share one retained instance — the same shape as the repositories.
 */
class ChatTranscriptStore : ViewModel() {

    private val _threads = MutableStateFlow<Map<String, List<SavedMessage>>>(emptyMap())

    /** Every thread's transcript. Read by the screen; written only here. */
    val threads: StateFlow<Map<String, List<SavedMessage>>> = _threads.asStateFlow()

    /**
     * The messages of [chatId], seeded with [seed] the first time the thread is
     * opened.
     *
     * Seeding happens on FIRST READ, not in the constructor: a thread the user
     * never opened costs nothing, and re-entering an open thread keeps whatever
     * was said in it rather than resetting to the sample history.
     */
    fun transcript(chatId: String, seed: List<SavedMessage>): List<SavedMessage> {
        _threads.value[chatId]?.let { return it }
        // `update` rather than a plain write: two composables can ask for the
        // same thread in the same frame, and only one seed may win.
        _threads.update { current -> if (current.containsKey(chatId)) current else current + (chatId to seed) }
        return _threads.value[chatId].orEmpty()
    }

    /**
     * Seeds [chatId] with [seed] if it has no transcript yet.
     *
     * Called from a `LaunchedEffect`, so the screen can render the seed on its
     * very first frame (a pure read) and only then commit it — mutating state
     * during composition is not allowed.
     */
    fun ensure(chatId: String, seed: List<SavedMessage>) {
        _threads.update { current ->
            if (current.containsKey(chatId)) current else current + (chatId to seed)
        }
    }

    /** Appends one message to [chatId]. */
    fun append(chatId: String, message: SavedMessage) {
        _threads.update { current ->
            val existing = current[chatId].orEmpty()
            current + (chatId to (existing + message))
        }
    }

    /**
     * Appends a chunk to the active streaming agent message for [agentId], or starts a new
     * message if the last message in the thread is from another role or a different agent.
     */
    fun appendOrUpdateStreaming(
        chatId: String,
        agentId: String?,
        agentName: String?,
        delta: String,
        time: String,
    ) {
        _threads.update { current ->
            val existing = current[chatId].orEmpty()
            val last = existing.lastOrNull()
            if (last != null && last.who == SavedMessage.AGENT && last.agentId == agentId) {
                val updatedLast = last.copy(text = last.text + delta)
                current + (chatId to (existing.dropLast(1) + updatedLast))
            } else {
                val newMsg = SavedMessage(
                    who = SavedMessage.AGENT,
                    text = delta,
                    time = time,
                    agentId = agentId,
                    agentName = agentName,
                )
                current + (chatId to (existing + newMsg))
            }
        }
    }

    /**
     * Empties a thread's transcript (the agent profile's "Clear Chat History").
     * The key stays, mapped to an empty list, so [ensure] does not re-seed
     * the sample conversation the next time the thread opens.
     */
    fun clear(chatId: String) {
        _threads.update { current -> current + (chatId to emptyList()) }
    }

    /** `MutableStateFlow.update` without pulling in the coroutines `update` ext. */
    private fun MutableStateFlow<Map<String, List<SavedMessage>>>.update(
        transform: (Map<String, List<SavedMessage>>) -> Map<String, List<SavedMessage>>,
    ) {
        while (true) {
            val current = value
            val next = transform(current)
            if (compareAndSet(current, next)) return
        }
    }
}

/* ============================================================================
 *  Live chat — one turn against dua-backend.
 *
 *  A normal thread goes to the Node API (`mode: "auto"`, `stream: false`).
 *  Dua Work ([AGENT_CHAT_ID] / [OFFICIAL_AGENT_ID]) goes to the Python engine
 *  (`mode: "agent"`, `stream: true`) with the enabled skill ids. The screen
 *  stays on the canned offline reply until a session token exists, unless a
 *  caller injects a source.
 * ==========================================================================*/

/**
 * Sends one user turn and returns the assistant text.
 *
 * @param skills the enabled Dua Work skill ids. A normal thread ignores them.
 */
interface ChatDataSource {
    suspend fun sendMessage(chatId: String, text: String, skills: Set<String>): String

    suspend fun streamMessage(
        chatId: String,
        text: String,
        skills: Set<String>,
        onChunk: (agentId: String?, agentName: String?, delta: String) -> Unit = { _, _, _ -> },
        onAgentDone: (agentId: String?, agentName: String?) -> Unit = { _, _ -> },
    ): String {
        val reply = sendMessage(chatId, text, skills)
        onChunk(null, null, reply)
        onAgentDone(null, null)
        return reply
    }
}

/**
 * The live `POST /api/chat` source.
 *
 * @param client where the request goes. Defaults to a client that reads
 *   [TokenStore] and waits [CHAT_READ_TIMEOUT_MS] (3 minutes). A long document
 *   or agent turn on Cloud Run outlives the old 2 minute read, and that
 *   timeout looked like a dead connection. A test injects a fake-transport client.
 */
class RemoteChatDataSource(
    private val client: JsonHttpClient = JsonHttpClient(
        transport = UrlConnectionTransport(readTimeoutMs = CHAT_READ_TIMEOUT_MS),
        label = "Chat request",
        tokenProvider = TokenStore.provider,
    ),
) : ChatDataSource {

    override suspend fun sendMessage(
        chatId: String,
        text: String,
        skills: Set<String>,
    ): String = withContext(Dispatchers.IO) {
        val isEngine = isOfficialAgentThread(chatId)
        val url = if (isEngine) ApiConfig.engineUrl("/api/chat") else ApiConfig.url("/api/chat")
        val payload = buildChatPayload(chatId, text, skills, stream = isEngine)
        val body = client.requestOrThrow("POST", url, payload.toString(), label = "Chat request")
        if (isEngine) parseAgentReply(body) else plainChatText(body)
    }

    override suspend fun streamMessage(
        chatId: String,
        text: String,
        skills: Set<String>,
        onChunk: (agentId: String?, agentName: String?, delta: String) -> Unit,
        onAgentDone: (agentId: String?, agentName: String?) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val isEngine = isOfficialAgentThread(chatId)
        val payload = buildChatPayload(chatId, text, skills, stream = true)
        val url = if (isEngine) ApiConfig.engineUrl("/api/chat") else ApiConfig.url("/api/chat")

        val accumulated = StringBuilder()
        var sawAnyChunk = false

        try {
            client.stream("POST", url, payload.toString(), label = "Chat request") { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val rawData = trimmed.removePrefix("data:").trim()
                    if (rawData.isNotEmpty() && rawData != "[DONE]") {
                        val obj = runCatching { JSONObject(rawData) }.getOrNull()
                        if (obj != null) {
                            val type = obj.optString("type")
                            val agentId = obj.optString("agentId").takeIf { it.isNotBlank() && it != "null" }
                            val agentName = obj.optString("agentName").takeIf { it.isNotBlank() && it != "null" }
                            when (type) {
                                "text" -> {
                                    val delta = obj.optString("delta").ifEmpty { obj.optString("text") }
                                    if (delta.isNotEmpty()) {
                                        sawAnyChunk = true
                                        accumulated.append(delta)
                                        onChunk(agentId, agentName, delta)
                                    }
                                }
                                "agent_done" -> {
                                    onAgentDone(agentId, agentName)
                                }
                                "done" -> {
                                    onAgentDone(agentId, agentName)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (!sawAnyChunk) {
                val nonStreamPayload = buildChatPayload(chatId, text, skills, stream = false)
                val body = client.requestOrThrow("POST", url, nonStreamPayload.toString(), label = "Chat request")
                val fallbackText = if (isEngine) parseAgentReply(body) else plainChatText(body)
                accumulated.append(fallbackText)
                onChunk(null, null, fallbackText)
                onAgentDone(null, null)
            } else {
                throw e
            }
        }

        accumulated.toString()
    }

    private fun buildChatPayload(
        chatId: String,
        text: String,
        skills: Set<String>,
        stream: Boolean,
    ): JSONObject {
        val isEngine = isOfficialAgentThread(chatId)
        val payload = JSONObject()
            .put("model", "Dua")
            .put("mode", if (isEngine) "agent" else "auto")
            .put("stream", stream)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("text", text),
                ),
            )

        if (!isEngine && !chatId.startsWith("chat_")) {
            payload.put("conversation_id", chatId)
        }

        if (isEngine) {
            payload.put("skills", skillIds(skills))
        }
        return payload
    }
}

/**
 * Skill ids in catalog order, then any extra id the caller enabled.
 *
 * The engine rejects unknown ids, and a stable order keeps the request body
 * comparable. Catalog order matches the contract's skill table.
 */
private fun skillIds(enabled: Set<String>): JSONArray {
    val ordered = ArrayList<String>(enabled.size)
    for (id in DuaAgentSkills.ids) {
        if (id in enabled) ordered += id
    }
    for (id in enabled) {
        if (id !in ordered) ordered += id
    }
    return JSONArray().apply { ordered.forEach { put(it) } }
}

/**
 * Reads an agent reply.
 *
 * SSE lines of the form `data: {"type":"text","delta":"..."}` are concatenated.
 * `delta` is the field the servers actually send (see the backend's
 * `apps/api/src/core/sse.mjs` wire-format comment and the engine's
 * `app.py` `sse({"type": "text", "delta": ...})`). `text` is still accepted as a
 * fallback so an older server that used that key keeps working.
 * A body with no `data:` line is treated as a plain `{"text":"..."}` JSON
 * response, which is what the engine returns when streaming did not happen.
 */
internal fun parseAgentReply(body: String): String {
    val chunks = StringBuilder()
    var sawData = false
    for (raw in body.lineSequence()) {
        val line = raw.trim()
        if (!line.startsWith("data:")) continue
        sawData = true
        val payload = line.removePrefix("data:").trim()
        if (payload.isEmpty() || payload == "[DONE]") continue
        val obj = runCatching { JSONObject(payload) }.getOrNull() ?: continue
        if (obj.optString("type") == "text") {
            // `delta` is what both servers send; `text` is the older key.
            val piece = obj.optString("delta").ifEmpty { obj.optString("text") }
            chunks.append(piece)
        }
    }
    if (!sawData) return plainChatText(body)
    return chunks.toString()
}

/** Reads `{"text":"..."}`. A body that is not that object is a malformed reply. */
private fun plainChatText(body: String): String {
    val trimmed = body.trim()
    if (trimmed.isEmpty()) throw ApiError.forMalformedResponse("Chat request")
    return try {
        JSONObject(trimmed).optString("text")
    } catch (e: org.json.JSONException) {
        throw ApiError.forMalformedResponse("Chat request")
    }
}
