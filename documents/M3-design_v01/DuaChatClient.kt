package ai.duagenai.design.screens

import org.json.JSONArray
import org.json.JSONObject

/* ============================================================================
 *  Chat calls for the two pinned roles.
 *
 *  Personal Agent goes to the Node API (`API_BASE_URL` + `/api/chat`) with
 *  `mode: "auto"` and `stream: false`. That route does not accept skills.
 *
 *  Dua Agent (the thread whose id is [AGENT_CHAT_ID]) goes to the Python
 *  engine (`ENGINE_BASE_URL` + `/api/chat`) with `mode: "agent"` and
 *  `stream: true`. The client sends skill ids only, in catalog order.
 *  The server appends the Super Agent prompt and the skill bodies.
 *
 *  Cookie routes `/api/web/chat` and `/api/web/agent/action` are not used.
 * ==========================================================================*/

/**
 * One blocking chat call. Call it off the main thread.
 *
 * A blank [ApiConfig.accessToken] is refused here so a screen can keep its
 * offline demo reply instead of hitting a server that will 401.
 */
object DuaChatClient {
    private val client = JsonHttpClient(
        transport = UrlConnectionTransport(
            connectTimeoutMs = 15_000,
            readTimeoutMs = 120_000,
        ),
        label = "Chat request",
    )

    fun reply(agent: Boolean, history: List<SavedMessage>, skillIds: Set<String>): String {
        check(ApiConfig.accessToken.isNotBlank()) { "Sign in before sending a message." }
        val messages = JSONArray()
        for (message in history) {
            if (message.text.isBlank()) continue
            messages.put(
                JSONObject()
                    .put("role", if (message.who == SavedMessage.USER) "user" else "model")
                    .put("text", message.text),
            )
        }
        val body = JSONObject()
            .put("model", "Dua")
            .put("messages", messages)
            .put("stream", agent)
        if (agent) {
            body.put("mode", "agent")
            val skills = JSONArray()
            for (id in DuaAgentSkills.ids) {
                if (id in skillIds) skills.put(id)
            }
            body.put("skills", skills)
        } else {
            body.put("mode", "auto")
        }
        val url = (if (agent) ApiConfig.ENGINE_BASE_URL else ApiConfig.API_BASE_URL) + "/api/chat"
        return replyText(client.request("POST", url, body.toString(), label = "Chat request"))
    }
}

/**
 * Reads either a JSON chat object (`text`) or an SSE body (`type: text`, `delta`).
 * An error with no text is thrown. A partial answer is kept when the stream
 * also ends in an error event.
 */
internal fun replyText(body: String): String {
    val trimmed = body.trim()
    if (trimmed.startsWith("{")) {
        val obj = JSONObject(trimmed)
        val error = obj.optString("error")
        if (error.isNotBlank() && !obj.has("text")) throw IllegalStateException(error)
        return obj.optString("text")
    }
    val deltas = StringBuilder()
    var event = ""
    var error: String? = null
    for (raw in body.split('\n')) {
        val line = raw.trimEnd('\r')
        when {
            line.startsWith("event:") -> event = line.substring(6).trim()
            line.startsWith("data:") -> {
                val data = line.substring(5).trim()
                if (data.startsWith("{")) {
                    val obj = JSONObject(data)
                    val type = obj.optString("type", event)
                    if (type == "text") deltas.append(obj.optString("delta", obj.optString("text")))
                    if (type == "error") {
                        error = obj.optString("message", obj.optString("error", "Request failed."))
                    }
                }
                event = ""
            }
            line.isEmpty() -> event = ""
        }
    }
    if (deltas.isEmpty() && !error.isNullOrBlank()) throw IllegalStateException(error)
    return deltas.toString()
}
