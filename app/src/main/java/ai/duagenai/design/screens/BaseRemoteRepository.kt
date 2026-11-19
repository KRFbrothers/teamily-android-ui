package ai.duagenai.design.screens

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/* ============================================================================
 *  The shared repository engine — load / refresh / add / delete, once.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  ArtifactsRepository and MemoryRepository used to each carry their own copy
 *  of the same orchestration: a MutableStateFlow for the UI state, a
 *  pull-to-refresh flag, a load count, a load() with Loading -> Loaded / Error,
 *  a refresh() that re-runs load() WITHOUT blanking the loaded list, and an
 *  add() / delete() that forward to the data source and only then update the
 *  list. Two copies of one rule drift — a fix in one place silently leaves the
 *  other behind.
 *
 *  So the engine lives here, once. [BaseRemoteRepository] is generic over the
 *  ITEM type and the UI state type, so both repositories inherit the exact
 *  same behaviour while keeping their own public state shapes
 *  ([ArtifactsUiState] / [MemoryUiState]) — the screens and the tests never
 *  change.
 *
 *  THE CONTRACT
 *  ------------
 *  A repository "owns" a UI state (Loading / Loaded / Error) and forwards
 *  every operation to a [RemoteDataSource]:
 *
 *    • load()    — Loading, then Loaded (or Error on failure)
 *    • refresh() — re-runs load() WITHOUT Loading, so the list stays visible
 *                  under the pull-to-refresh indicator
 *    • add()     — prepends an item THROUGH the source, then updates the list
 *    • delete()  — removes by id THROUGH the source, then updates the list
 *
 *  The concrete class implements the three state constructors and the data
 *  source — the only things that genuinely differ between Artifacts and Memory.
 *  Error handling, retry, the refresh flag and the load count are all shared.
 *
 *  No new library: kotlinx.coroutines Flow is already a dependency, and the
 *  HTTP mechanics stay in the shared [JsonHttpClient] (one instance, see
 *  [HttpClientProvider]).
 *
 *  WHAT WAS FIXED HERE (review blockers)
 *  -------------------------------------
 *   1. CANCELLATION WAS SWALLOWED. `catch (e: Exception)` also caught
 *      `CancellationException`, so a cancelled load was reported as an Error
 *      state and the coroutine's cancellation was silently absorbed. Every
 *      catch now rethrows it first.
 *   2. FAILED WRITES WERE SILENT. `add` / `delete` caught the exception and
 *      `return`ed, so the screen showed the item as deleted while the server
 *      still had it — the worst possible outcome, because the user believes the
 *      data is gone. They now return a [WriteResult] and publish the failure on
 *      [lastWriteError], so the screen can say "couldn't delete" and keep the
 *      row.
 * ==========================================================================*/

/**
 * The one thing every list item needs for [BaseRemoteRepository.delete].
 */
interface Identifiable {
    val id: String
}

/**
 * The generic data-source contract a repository talks to.
 *
 * Every method is `suspend` and may throw: the repository turns a thrown
 * exception into its Error state, so a data source only has to report failure,
 * not model it. A concrete source keeps its own feature interface (e.g.
 * [ArtifactsDataSource] extends this, adding nothing) so the UI code and the
 * tests keep referring to the feature name.
 */
interface RemoteDataSource<T : Identifiable> {
    /** Fetches the full list. Throws on network / parse failure. */
    suspend fun fetch(): List<T>

    /**
     * Persists a new item and returns what the SOURCE stored.
     *
     * Returning the stored item rather than Unit is deliberate: a real backend
     * assigns its own id and timestamp, and the list must show those, not the
     * client's guess. An in-memory source simply returns [item] unchanged.
     */
    suspend fun add(item: T): T

    /** Removes an item by id. Throws on failure. */
    suspend fun delete(id: String)
}

/**
 * The outcome of a write.
 *
 * A write is not a fire-and-forget: the caller has to know whether the server
 * accepted it, because the alternative is a screen that lies about what was
 * saved.
 */
sealed interface WriteResult {
    /** The source accepted the write and the list was updated. */
    data object Success : WriteResult

    /**
     * The source rejected the write. The list is UNCHANGED, and [message] is
     * safe to show.
     */
    data class Failure(val message: String) : WriteResult
}

/**
 * The shared load / refresh / add / delete engine both repositories use.
 *
 * @param T the item type ([ArtifactItem] or [MemoryItem]).
 * @param S the UI state type ([ArtifactsUiState] or [MemoryUiState]) — a sealed
 *   Loading / Loaded / Error shape.
 * @param source where items actually come from. [RemoteArtifactsDataSource] and
 *   [RemoteMemoryDataSource] route through the shared [HttpClientProvider]
 *   client by default; the in-memory sources are the demo defaults.
 */
abstract class BaseRemoteRepository<T : Identifiable, S>(
    protected val source: RemoteDataSource<T>,
) {

    // ---- the three state constructors plus the list accessor: the ONLY
    // ---- things a concrete repository must supply.

    /** The state shown while the first load is in flight. */
    protected abstract fun loadingState(): S

    /** The state wrapping a finished list (may be empty — that is the empty state). */
    protected abstract fun loadedState(items: List<T>): S

    /** The state wrapping a failure message, shown with a Retry action. */
    protected abstract fun errorState(message: String, retryable: Boolean): S

    /** The Loaded list inside [state], or null when the state is Loading / Error. */
    protected abstract fun itemsOf(state: S): List<T>?

    /**
     * Returns a copy of [item] carrying [id].
     *
     * Used by the undo path: a restore must not re-POST the id the server just
     * deleted, because a backend that keeps a tombstone (or enforces unique ids)
     * rejects it and the undo silently fails. A fresh id makes the restore a
     * genuine new row.
     */
    protected abstract fun withId(item: T, id: String): T

    /** The message used when an exception carries none of its own. */
    protected abstract val errorFallback: String

    /**
     * The message shown when an ADD fails and the exception is not an [ApiError].
     *
     * A write failure is shown to the user, so it must be a sentence this app
     * wrote — never `e.toString()`, which leaks class names, URLs and stack
     * frames into a snackbar. A concrete repository overrides this with its own
     * wording; the default is the load fallback so a new repository still gets a
     * safe message without extra work.
     */
    protected open val addFailureFallback: String get() = errorFallback

    /** The message shown when a DELETE fails and the exception is not an [ApiError]. */
    protected open val deleteFailureFallback: String get() = errorFallback

    /**
     * Turns a write exception into a sentence that is safe to show.
     *
     * [ApiError] already carries a curated [ApiError.userMessage], so it is used
     * as-is. Anything else (an `IOException`, a parse failure, a bug) is replaced
     * by the feature's own fallback: the user gets "couldn't delete" instead of
     * `java.net.UnknownHostException: api.example.com`.
     */
    protected open fun writeFailureMessage(e: Exception, deleting: Boolean): String =
        when (e) {
            is ApiError -> e.userMessage
            else -> if (deleting) deleteFailureFallback else addFailureFallback
        }

    /**
     * Turns a LOAD exception into a sentence that is safe to show.
     *
     * WHY THIS EXISTS
     * ---------------
     * [load] used to do `errorState(e.message ?: errorFallback)`. That is the
     * exact thing this file's own contract forbids: `e.message` on a
     * `JSONException` is `"No value for type"`, on an `UnknownHostException` it
     * is `"Unable to resolve host \"dua-api…\": No address associated with
     * hostname"`, and on a `SocketTimeoutException` it is `"Read timed out"`.
     * None of those is a sentence for a user, and the hostname one leaks the
     * backend's address onto the screen.
     *
     * [ApiError] already carries a curated [ApiError.userMessage], so it is used
     * as-is. Anything else is replaced by the feature's own fallback.
     */
    protected open fun loadFailureMessage(e: Exception): String =
        when (e) {
            is ApiError -> e.userMessage
            else -> errorFallback
        }

    /**
     * Whether a load failure is worth retrying.
     *
     * [ApiError] already knows (a 5xx or a network failure is retryable, a 401 is
     * not). Anything else — a parse bug, an unexpected exception — is treated as
     * retryable, because the honest default for an unknown failure is "try
     * again", not "this will never work".
     */
    protected open fun isRetryable(e: Exception): Boolean =
        (e as? ApiError)?.retryable ?: true

    // ---- shared state. `loadingState()` returns a bare object singleton in
    // ---- both concrete repositories, so calling it here — before the subclass
    // ---- has finished constructing — is safe. A subclass must never read its
    // ---- own fields from the state constructors.

    protected val _state = MutableStateFlow(loadingState())

    /** The current state. The screen collects this; it never reads the source. */
    val state: StateFlow<S> = _state.asStateFlow()

    /**
     * True while a pull-to-refresh is in flight.
     *
     * Deliberately SEPARATE from [state]: a refresh must not blank the list, so
     * it cannot go through the Loading state. The screen keeps drawing the
     * loaded list and shows the refresh indicator from this flag instead.
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * The last write failure, or null when the last write succeeded.
     *
     * The screen collects this to show a snackbar. It is separate from [state]
     * on purpose: a failed delete must NOT replace the list with an Error
     * screen — the list is still valid, only the write failed.
     */
    private val _lastWriteError = MutableStateFlow<String?>(null)
    val lastWriteError: StateFlow<String?> = _lastWriteError.asStateFlow()

    /** Clears [lastWriteError] once the screen has shown it. */
    fun clearWriteError() {
        _lastWriteError.value = null
    }

    /**
     * How many times [load] has run.
     *
     * The pull-to-refresh tests read this: it is the observable proof that the
     * gesture reached the repository, rather than only spinning an indicator.
     */
    var loadCount: Int = 0
        private set

    /**
     * Loads the items from the data source.
     *
     * Emits [loadingState] first (unless [showLoading] is false), then Loaded
     * (or Error). Safe to call again — that is what the Retry action does, and
     * what [refresh] calls with `showLoading = false` so the list stays on
     * screen while the indicator spins.
     */
    suspend fun load(showLoading: Boolean = true) {
        loadCount++
        if (showLoading) _state.value = loadingState()
        _state.value = try {
            loadedState(source.fetch())
        } catch (e: CancellationException) {
            // A cancelled load is not a failure: rethrow so the caller's
            // structured concurrency keeps working, and leave the state alone.
            throw e
        } catch (e: Exception) {
            // NEVER `e.message`: see [loadFailureMessage]. A raw JSONException or
            // UnknownHostException message is not a sentence for a user, and the
            // hostname one leaks the backend's address onto the screen.
            errorState(loadFailureMessage(e), isRetryable(e))
        }
    }

    /**
     * Re-runs the fetch for a pull-to-refresh, WITHOUT ever blanking the list.
     *
     * WHY THIS IS NOT `load(showLoading = false)`
     * -------------------------------------------
     * It used to be exactly that, and it was wrong in the one case that matters.
     * `load()` ends in `errorState(...)` on failure, so a refresh that failed
     * REPLACED the loaded list with a full-screen Error — the list the user was
     * looking at vanished because a background re-fetch failed. The comment on
     * this method claimed the opposite ("the list stays visible"), which is how
     * the bug survived review.
     *
     * The correct behaviour: a refresh is an OPTIMISTIC re-fetch of data that is
     * already on screen. If it fails, the data on screen is still valid — it is
     * just not any newer. So the state is left untouched and the failure is
     * published on [lastWriteError], which the screen already shows as a
     * snackbar. Only a refresh with NO loaded list (i.e. the first load failed
     * and the user pulled to retry) falls back to the Error state, because there
     * is nothing to preserve.
     */
    suspend fun refresh() {
        _isRefreshing.value = true
        try {
            loadCount++
            val fresh = try {
                source.fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = loadFailureMessage(e)
                // Keep whatever is on screen. Only surface a full Error when
                // there is no list to keep.
                if (itemsOf(_state.value) == null) {
                    _state.value = errorState(message, isRetryable(e))
                } else {
                    _lastWriteError.value = message
                }
                return
            }
            _state.value = loadedState(fresh)
            _lastWriteError.value = null
        } finally {
            _isRefreshing.value = false
        }
    }

    /**
     * A fresh id for a restored row.
     *
     * Deliberately NOT the deleted id: see [withId]. The suffix keeps the
     * original id readable in a log while making the value unique.
     */
    protected open fun freshIdFor(originalId: String): String =
        "${originalId}_restored_${System.currentTimeMillis()}"

    /**
     * Adds [item] to the top of the list, through the data source.
     *
     * This is what an undo snackbar calls, so a restored item really is re-added
     * to the source rather than only to the screen's copy. A no-op while loading
     * or in error, so a stray add cannot invent a list that was never loaded.
     *
     * A failed write leaves the list as it was AND reports the failure — the
     * honest result, and the one the screen can act on.
     */
    suspend fun add(item: T): WriteResult {
        val current = _state.value
        val items = itemsOf(current) ?: return WriteResult.Failure(errorFallback)
        return try {
            // The source returns what it actually stored. A real backend assigns
            // its own id / timestamp, so the list must show THAT row — keeping
            // the client's guess meant a later DELETE used an id the server had
            // never issued.
            val stored = source.add(item)
            _state.value = loadedState(listOf(stored) + items)
            _lastWriteError.value = null
            WriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = writeFailureMessage(e, deleting = false)
            _lastWriteError.value = message
            WriteResult.Failure(message)
        }
    }

    /**
     * Restores a deleted [item] as a NEW row, for the undo snackbar.
     *
     * WHY NOT `add(item)`
     * -------------------
     * The undo used to call `add(item)` with the SAME id the server had just
     * deleted. A backend that keeps a tombstone, or that enforces a unique id,
     * rejects that POST — so the undo failed and the row stayed gone, which is
     * the one outcome the snackbar promised would not happen.
     *
     * A restore is a new row, so it gets a new id. The content is preserved; only
     * the identity changes, which is exactly what "put it back" means to a
     * server that has already forgotten the old one.
     */
    suspend fun restore(item: T): WriteResult = add(withId(item, freshIdFor(item.id)))

    /**
     * Removes the item with [id], through the data source.
     *
     * A no-op while loading or in error, and a no-op for an unknown id.
     *
     * The list is only updated AFTER the source accepted the delete. If the
     * source refuses, the row stays on screen and the failure is reported —
     * showing it as deleted while the server still has it would be a lie the
     * user cannot see.
     */
    suspend fun delete(id: String): WriteResult {
        val current = _state.value
        val items = itemsOf(current) ?: return WriteResult.Failure(errorFallback)
        return try {
            source.delete(id)
            _state.value = loadedState(items.filterNot { it.id == id })
            _lastWriteError.value = null
            WriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = writeFailureMessage(e, deleting = true)
            _lastWriteError.value = message
            WriteResult.Failure(message)
        }
    }
}

/* ============================================================================
 *  Conversations & Chat Messages
 * ==========================================================================*/

data class ConversationItem(
    override val id: String,
    val title: String,
    val agentId: String = "dua-agent",
    val pinned: Boolean = false,
    val isGroup: Boolean = false,
    val lastMessagePreview: String = "",
    val lastMessageAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
) : Identifiable {
    fun toChatRow(): ChatRow = ChatRow(chatId = id, name = title)
}

data class ConversationAgentItem(
    val conversationId: String,
    val agentId: String,
    val addedAt: String = "",
)

sealed interface ConversationsUiState {
    object Loading : ConversationsUiState
    data class Loaded(val conversations: List<ConversationItem>) : ConversationsUiState
    data class Error(val message: String, val retryable: Boolean = true) : ConversationsUiState
}

interface ConversationsDataSource : RemoteDataSource<ConversationItem> {
    suspend fun fetch(query: String? = null): List<ConversationItem>
    suspend fun togglePin(id: String, pinned: Boolean): ConversationItem
    suspend fun fetchMessages(id: String, limit: Int = 200, before: String? = null): List<SavedMessage>
    suspend fun fetchAgents(id: String): List<ConversationAgentItem>
    suspend fun addAgent(id: String, agentId: String): ConversationAgentItem
    suspend fun removeAgent(id: String, agentId: String)
}

class InMemoryConversationsDataSource(
    seed: List<ConversationItem> = emptyList(),
) : ConversationsDataSource {
    private val items = seed.toMutableList()
    private val messages = mutableMapOf<String, MutableList<SavedMessage>>()
    private val agents = mutableMapOf<String, MutableList<ConversationAgentItem>>()

    override suspend fun fetch(): List<ConversationItem> = items.toList()
    override suspend fun fetch(query: String?): List<ConversationItem> =
        if (query.isNullOrBlank()) items.toList() else items.filter { it.title.contains(query, ignoreCase = true) }

    override suspend fun add(item: ConversationItem): ConversationItem {
        items.removeAll { it.id == item.id }
        items.add(0, item)
        return item
    }

    override suspend fun delete(id: String) {
        items.removeAll { it.id == id }
        messages.remove(id)
        agents.remove(id)
    }

    override suspend fun togglePin(id: String, pinned: Boolean): ConversationItem {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updated = items[index].copy(pinned = pinned)
            items[index] = updated
            return updated
        }
        val fallback = ConversationItem(id = id, title = "Chat", pinned = pinned)
        items.add(0, fallback)
        return fallback
    }

    override suspend fun fetchMessages(id: String, limit: Int, before: String?): List<SavedMessage> =
        messages[id]?.toList().orEmpty()

    override suspend fun fetchAgents(id: String): List<ConversationAgentItem> =
        agents[id]?.toList().orEmpty()

    override suspend fun addAgent(id: String, agentId: String): ConversationAgentItem {
        val agent = ConversationAgentItem(id, agentId, nowLabel())
        agents.getOrPut(id) { mutableListOf() }.add(agent)
        return agent
    }

    override suspend fun removeAgent(id: String, agentId: String) {
        agents[id]?.removeAll { it.agentId == agentId }
    }
}

class RemoteConversationsDataSource(
    private val client: JsonHttpClient = HttpClientProvider.client,
) : ConversationsDataSource {
    override suspend fun fetch(): List<ConversationItem> = fetch(null)

    override suspend fun fetch(query: String?): List<ConversationItem> = withContext(Dispatchers.IO) {
        val path = if (!query.isNullOrBlank()) {
            "/api/conversations?q=${encodePathSegment(query)}&limit=100"
        } else {
            "/api/conversations?limit=100"
        }
        val body = client.requestOrThrow("GET", ApiConfig.url(path), label = "Conversations request")
        parseConversations(body)
    }

    override suspend fun add(item: ConversationItem): ConversationItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            if (item.id.isNotBlank() && !item.id.startsWith("chat_")) put("id", item.id)
            put("title", item.title)
            put("agentId", item.agentId)
        }.toString()
        val body = client.requestOrThrow("POST", ApiConfig.url("/api/conversations"), payload, label = "Create conversation")
        parseSingleConversation(body) ?: item
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        client.requestOrThrow("DELETE", ApiConfig.url("/api/conversations/${encodePathSegment(id)}"), label = "Delete conversation")
        Unit
    }

    override suspend fun togglePin(id: String, pinned: Boolean): ConversationItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("pinned", pinned).toString()
        val body = client.requestOrThrow("PUT", ApiConfig.url("/api/conversations/${encodePathSegment(id)}/pin"), payload, label = "Pin conversation")
        parseSingleConversation(body) ?: ConversationItem(id = id, title = "Chat", pinned = pinned)
    }

    override suspend fun fetchMessages(id: String, limit: Int, before: String?): List<SavedMessage> = withContext(Dispatchers.IO) {
        val query = buildString {
            append("?limit=").append(limit)
            if (!before.isNullOrBlank()) append("&before=").append(encodePathSegment(before))
        }
        val body = client.requestOrThrow("GET", ApiConfig.url("/api/conversations/${encodePathSegment(id)}/messages$query"), label = "Conversation messages")
        parseMessages(body)
    }

    override suspend fun fetchAgents(id: String): List<ConversationAgentItem> = withContext(Dispatchers.IO) {
        val body = client.requestOrThrow("GET", ApiConfig.url("/api/conversations/${encodePathSegment(id)}/agents"), label = "Conversation agents")
        parseConversationAgents(body)
    }

    override suspend fun addAgent(id: String, agentId: String): ConversationAgentItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("agentId", agentId).toString()
        val body = client.requestOrThrow("POST", ApiConfig.url("/api/conversations/${encodePathSegment(id)}/agents"), payload, label = "Add conversation agent")
        parseSingleConversationAgent(body) ?: ConversationAgentItem(id, agentId)
    }

    override suspend fun removeAgent(id: String, agentId: String) = withContext(Dispatchers.IO) {
        client.requestOrThrow("DELETE", ApiConfig.url("/api/conversations/${encodePathSegment(id)}/agents/${encodePathSegment(agentId)}"), label = "Remove conversation agent")
        Unit
    }
}

fun parseConversations(body: String): List<ConversationItem> {
    val arr = unwrapList(body, "conversations", "Conversations")
    val list = ArrayList<ConversationItem>(arr.length())
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        list += ConversationItem(
            id = obj.optString("id"),
            title = obj.optString("title", "New chat"),
            agentId = obj.optString("agentId", "dua-agent"),
            pinned = obj.optBoolean("pinned", false),
            lastMessagePreview = obj.optString("lastMessagePreview", ""),
            lastMessageAt = obj.optString("lastMessageAt", ""),
            createdAt = obj.optString("createdAt", ""),
        )
    }
    return list
}

fun parseSingleConversation(body: String): ConversationItem? = runCatching {
    val root = JSONObject(body)
    val obj = root.optJSONObject("conversation") ?: root
    ConversationItem(
        id = obj.optString("id"),
        title = obj.optString("title", "New chat"),
        agentId = obj.optString("agentId", "dua-agent"),
        pinned = obj.optBoolean("pinned", false),
        lastMessagePreview = obj.optString("lastMessagePreview", ""),
        lastMessageAt = obj.optString("lastMessageAt", ""),
        createdAt = obj.optString("createdAt", ""),
    )
}.getOrNull()

fun parseMessages(body: String): List<SavedMessage> {
    val arr = unwrapList(body, "messages", "Messages")
    val list = ArrayList<SavedMessage>(arr.length())
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val role = obj.optString("role", "user")
        val content = obj.optString("content", "")
        val createdAt = obj.optString("createdAt", "")
        val agentId = obj.optString("agentId").takeIf { it.isNotBlank() && it != "null" }
        val agentName = when (agentId) {
            "dua-agent" -> "Dua Agent"
            "personal-agent" -> "Personal Agent"
            else -> agentId
        }
        val who = if (role.equals("user", ignoreCase = true)) SavedMessage.USER else SavedMessage.AGENT
        list += SavedMessage(
            who = who,
            text = content,
            time = formatIsoTime(createdAt),
            agentId = agentId,
            agentName = agentName,
        )
    }
    return list
}

fun parseConversationAgents(body: String): List<ConversationAgentItem> {
    val arr = unwrapList(body, "agents", "Conversation agents")
    val list = ArrayList<ConversationAgentItem>(arr.length())
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        list += ConversationAgentItem(
            conversationId = obj.optString("conversationId"),
            agentId = obj.optString("agentId"),
            addedAt = obj.optString("addedAt"),
        )
    }
    return list
}

fun parseSingleConversationAgent(body: String): ConversationAgentItem? = runCatching {
    val root = JSONObject(body)
    val obj = root.optJSONObject("agent") ?: root
    ConversationAgentItem(
        conversationId = obj.optString("conversationId"),
        agentId = obj.optString("agentId"),
        addedAt = obj.optString("addedAt"),
    )
}.getOrNull()

internal fun formatIsoTime(iso: String): String {
    if (iso.isBlank()) return nowLabel()
    return try {
        if (iso.contains("T") && iso.length >= 16) {
            iso.substringAfter("T").take(5)
        } else {
            iso
        }
    } catch (_: Exception) {
        nowLabel()
    }
}

class ConversationsRepository(
    dataSource: ConversationsDataSource? = null,
) : BaseRemoteRepository<ConversationItem, ConversationsUiState>(
    source = dataSource ?: (if (TokenStore.token != null) RemoteConversationsDataSource() else InMemoryConversationsDataSource()),
) {
    override fun loadingState(): ConversationsUiState = ConversationsUiState.Loading
    override fun loadedState(items: List<ConversationItem>): ConversationsUiState = ConversationsUiState.Loaded(items)
    override fun errorState(message: String, retryable: Boolean): ConversationsUiState = ConversationsUiState.Error(message, retryable)
    override fun itemsOf(state: ConversationsUiState): List<ConversationItem>? = (state as? ConversationsUiState.Loaded)?.conversations
    override fun withId(item: ConversationItem, id: String): ConversationItem = item.copy(id = id)
    override val errorFallback: String = "Conversations load nahi ho paye."

    val conversationsSource: ConversationsDataSource get() = source as ConversationsDataSource

    suspend fun filter(query: String?): List<ConversationItem> {
        return conversationsSource.fetch(query)
    }

    suspend fun createConversation(title: String, agentId: String = "dua-agent", id: String? = null): ConversationItem {
        val conv = ConversationItem(
            id = id ?: java.util.UUID.randomUUID().toString(),
            title = title,
            agentId = agentId,
            createdAt = nowLabel(),
        )
        conversationsSource.add(conv)
        val current = (state.value as? ConversationsUiState.Loaded)?.conversations.orEmpty()
        _state.value = ConversationsUiState.Loaded(listOf(conv) + current)
        return conv
    }

    suspend fun togglePin(id: String, pinned: Boolean): WriteResult {
        return try {
            val updated = conversationsSource.togglePin(id, pinned)
            val current = (state.value as? ConversationsUiState.Loaded)?.conversations.orEmpty()
            val next = current.map { if (it.id == id) updated else it }
            _state.value = ConversationsUiState.Loaded(next)
            WriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WriteResult.Failure(e.message ?: "Failed to update pin.")
        }
    }

    suspend fun fetchMessages(id: String): List<SavedMessage> = conversationsSource.fetchMessages(id)
    suspend fun fetchAgents(id: String): List<ConversationAgentItem> = conversationsSource.fetchAgents(id)
    suspend fun addAgent(id: String, agentId: String): ConversationAgentItem = conversationsSource.addAgent(id, agentId)
    suspend fun removeAgent(id: String, agentId: String) = conversationsSource.removeAgent(id, agentId)
}

/* ============================================================================
 *  Agents, Instructions, & Triggers
 * ==========================================================================*/

data class AgentItem(
    override val id: String,
    val name: String,
    val description: String = "",
    val mode: String = "auto",
    val systemPrompt: String = "",
    val model: String? = null,
    val pinned: Boolean = false,
    val isOfficial: Boolean = false,
    val icon: String? = null,
    val color: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
) : Identifiable

data class AgentInstructionItem(
    override val id: String,
    val agentId: String,
    val instruction: String,
    val isActive: Boolean = true,
    val createdAt: String = "",
) : Identifiable

data class AgentTriggerItem(
    override val id: String,
    val agentId: String,
    val conversationId: String? = null,
    val triggerType: String = "schedule",
    val cronExpr: String? = null,
    val prompt: String = "",
    val enabled: Boolean = true,
    val lastRunAt: String? = null,
    val createdAt: String = "",
) : Identifiable

sealed interface AgentsUiState {
    object Loading : AgentsUiState
    data class Loaded(val agents: List<AgentItem>) : AgentsUiState
    data class Error(val message: String, val retryable: Boolean = true) : AgentsUiState
}

interface AgentsDataSource : RemoteDataSource<AgentItem> {
    suspend fun update(item: AgentItem): AgentItem
    suspend fun generatePrompt(description: String, name: String = "", mode: String = "auto"): String
    suspend fun fetchInstructions(agentId: String): List<AgentInstructionItem>
    suspend fun addInstruction(agentId: String, instruction: String, isActive: Boolean = true): AgentInstructionItem
    suspend fun updateInstruction(agentId: String, instructionId: String, instruction: String, isActive: Boolean): AgentInstructionItem
    suspend fun deleteInstruction(agentId: String, instructionId: String)
    suspend fun fetchTriggers(agentId: String): List<AgentTriggerItem>
    suspend fun addTrigger(agentId: String, triggerType: String, cronExpr: String?, prompt: String, conversationId: String?, enabled: Boolean): AgentTriggerItem
    suspend fun updateTrigger(agentId: String, triggerId: String, triggerType: String, cronExpr: String?, prompt: String, conversationId: String?, enabled: Boolean): AgentTriggerItem
    suspend fun deleteTrigger(agentId: String, triggerId: String)
    suspend fun runTrigger(agentId: String, triggerId: String): AgentTriggerItem
}

class InMemoryAgentsDataSource(
    seed: List<AgentItem> = emptyList(),
) : AgentsDataSource {
    private val items = seed.toMutableList()
    private val instructions = mutableMapOf<String, MutableList<AgentInstructionItem>>()
    private val triggers = mutableMapOf<String, MutableList<AgentTriggerItem>>()

    override suspend fun fetch(): List<AgentItem> = items.toList()
    override suspend fun add(item: AgentItem): AgentItem {
        items.removeAll { it.id == item.id }
        items.add(item)
        return item
    }
    override suspend fun update(item: AgentItem): AgentItem {
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) items[idx] = item else items.add(item)
        return item
    }
    override suspend fun delete(id: String) {
        if (id == "personal-agent" || id == "dua-agent") {
            throw IllegalStateException("Official agents cannot be deleted.")
        }
        items.removeAll { it.id == id }
    }
    override suspend fun generatePrompt(description: String, name: String, mode: String): String =
        "You are $name, a specialized assistant designed to $description."
    override suspend fun fetchInstructions(agentId: String): List<AgentInstructionItem> =
        instructions[agentId]?.toList().orEmpty()
    override suspend fun addInstruction(agentId: String, instruction: String, isActive: Boolean): AgentInstructionItem {
        val item = AgentInstructionItem(java.util.UUID.randomUUID().toString(), agentId, instruction, isActive)
        instructions.getOrPut(agentId) { mutableListOf() }.add(item)
        return item
    }
    override suspend fun updateInstruction(agentId: String, instructionId: String, instruction: String, isActive: Boolean): AgentInstructionItem {
        val list = instructions.getOrPut(agentId) { mutableListOf() }
        val idx = list.indexOfFirst { it.id == instructionId }
        val updated = AgentInstructionItem(instructionId, agentId, instruction, isActive)
        if (idx >= 0) list[idx] = updated else list.add(updated)
        return updated
    }
    override suspend fun deleteInstruction(agentId: String, instructionId: String) {
        instructions[agentId]?.removeAll { it.id == instructionId }
    }
    override suspend fun fetchTriggers(agentId: String): List<AgentTriggerItem> =
        triggers[agentId]?.toList().orEmpty()
    override suspend fun addTrigger(agentId: String, triggerType: String, cronExpr: String?, prompt: String, conversationId: String?, enabled: Boolean): AgentTriggerItem {
        val item = AgentTriggerItem(java.util.UUID.randomUUID().toString(), agentId, conversationId, triggerType, cronExpr, prompt, enabled)
        triggers.getOrPut(agentId) { mutableListOf() }.add(item)
        return item
    }
    override suspend fun updateTrigger(agentId: String, triggerId: String, triggerType: String, cronExpr: String?, prompt: String, conversationId: String?, enabled: Boolean): AgentTriggerItem {
        val list = triggers.getOrPut(agentId) { mutableListOf() }
        val idx = list.indexOfFirst { it.id == triggerId }
        val updated = AgentTriggerItem(triggerId, agentId, conversationId, triggerType, cronExpr, prompt, enabled)
        if (idx >= 0) list[idx] = updated else list.add(updated)
        return updated
    }
    override suspend fun deleteTrigger(agentId: String, triggerId: String) {
        triggers[agentId]?.removeAll { it.id == triggerId }
    }
    override suspend fun runTrigger(agentId: String, triggerId: String): AgentTriggerItem {
        val list = triggers.getOrPut(agentId) { mutableListOf() }
        val item = list.firstOrNull { it.id == triggerId } ?: AgentTriggerItem(triggerId, agentId, prompt = "Run")
        val updated = item.copy(lastRunAt = nowLabel())
        val idx = list.indexOfFirst { it.id == triggerId }
        if (idx >= 0) list[idx] = updated
        return updated
    }
}

class RemoteAgentsDataSource(
    private val client: JsonHttpClient = HttpClientProvider.client,
) : AgentsDataSource {
    override suspend fun fetch(): List<AgentItem> = withContext(Dispatchers.IO) {
        val body = client.requestOrThrow("GET", ApiConfig.url("/api/agents"), label = "Agents request")
        parseAgents(body)
    }

    override suspend fun add(item: AgentItem): AgentItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            if (item.id.isNotBlank()) put("id", item.id)
            put("name", item.name)
            put("description", item.description)
            put("mode", item.mode)
            put("systemPrompt", item.systemPrompt)
            put("pinned", item.pinned)
        }.toString()
        val body = client.requestOrThrow("POST", ApiConfig.url("/api/agents"), payload, label = "Create agent")
        parseSingleAgent(body) ?: item
    }

    override suspend fun update(item: AgentItem): AgentItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("name", item.name)
            put("description", item.description)
            put("mode", item.mode)
            put("systemPrompt", item.systemPrompt)
            put("pinned", item.pinned)
        }.toString()
        val body = client.requestOrThrow("PUT", ApiConfig.url("/api/agents/${encodePathSegment(item.id)}"), payload, label = "Update agent")
        parseSingleAgent(body) ?: item
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        client.requestOrThrow("DELETE", ApiConfig.url("/api/agents/${encodePathSegment(id)}"), label = "Delete agent")
        Unit
    }

    override suspend fun generatePrompt(description: String, name: String, mode: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("description", description).put("name", name).put("mode", mode).toString()
        val body = client.requestOrThrow("POST", ApiConfig.url("/api/agents/generate-prompt"), payload, label = "Generate agent prompt")
        JSONObject(body).optString("systemPrompt")
    }

    override suspend fun fetchInstructions(agentId: String): List<AgentInstructionItem> = withContext(Dispatchers.IO) {
        val body = client.requestOrThrow("GET", ApiConfig.url("/api/agents/${encodePathSegment(agentId)}/instructions"), label = "Agent instructions")
        parseInstructions(body)
    }

    override suspend fun addInstruction(agentId: String, instruction: String, isActive: Boolean): AgentInstructionItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("instruction", instruction).put("isActive", isActive).toString()
        val body = client.requestOrThrow("POST", ApiConfig.url("/api/agents/${encodePathSegment(agentId)}/instructions"), payload, label = "Add instruction")
        parseSingleInstruction(body) ?: AgentInstructionItem(java.util.UUID.randomUUID().toString(), agentId, instruction, isActive)
    }

    override suspend fun updateInstruction(agentId: String, instructionId: String, instruction: String, isActive: Boolean): AgentInstructionItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("instruction", instruction).put("isActive", isActive).toString()
        val body = client.requestOrThrow("PUT", ApiConfig.url("/api/agents/${encodePathSegment(agentId)}/instructions/${encodePathSegment(instructionId)}"), payload, label = "Update instruction")
        parseSingleInstruction(body) ?: AgentInstructionItem(instructionId, agentId, instruction, isActive)
    }

    override suspend fun deleteInstruction(agentId: String, instructionId: String) = withContext(Dispatchers.IO) {
        client.requestOrThrow("DELETE", ApiConfig.url("/api/agents/${encodePathSegment(agentId)}/instructions/${encodePathSegment(instructionId)}"), label = "Delete instruction")
        Unit
    }

    override suspend fun fetchTriggers(agentId: String): List<AgentTriggerItem> = withContext(Dispatchers.IO) {
        val body = client.requestOrThrow("GET", ApiConfig.url("/api/agents/${encodePathSegment(agentId)}/triggers"), label = "Agent triggers")
        parseTriggers(body)
    }

    override suspend fun addTrigger(agentId: String, triggerType: String, cronExpr: String?, prompt: String, conversationId: String?, enabled: Boolean): AgentTriggerItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("triggerType", triggerType)
            cronExpr?.let { put("cronExpr", it) }
            put("prompt", prompt)
            conversationId?.let { put("conversationId", it) }
            put("enabled", enabled)
        }.toString()
        val body = client.requestOrThrow("POST", ApiConfig.url("/api/agents/${encodePathSegment(agentId)}/triggers"), payload, label = "Add trigger")
        parseSingleTrigger(body) ?: AgentTriggerItem(java.util.UUID.randomUUID().toString(), agentId, conversationId, triggerType, cronExpr, prompt, enabled)
    }

    override suspend fun updateTrigger(agentId: String, triggerId: String, triggerType: String, cronExpr: String?, prompt: String, conversationId: String?, enabled: Boolean): AgentTriggerItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("triggerType", triggerType)
            cronExpr?.let { put("cronExpr", it) }
            put("prompt", prompt)
            conversationId?.let { put("conversationId", it) }
            put("enabled", enabled)
        }.toString()
        val body = client.requestOrThrow("PUT", ApiConfig.url("/api/agents/${encodePathSegment(agentId)}/triggers/${encodePathSegment(triggerId)}"), payload, label = "Update trigger")
        parseSingleTrigger(body) ?: AgentTriggerItem(triggerId, agentId, conversationId, triggerType, cronExpr, prompt, enabled)
    }

    override suspend fun deleteTrigger(agentId: String, triggerId: String) = withContext(Dispatchers.IO) {
        client.requestOrThrow("DELETE", ApiConfig.url("/api/agents/${encodePathSegment(agentId)}/triggers/${encodePathSegment(triggerId)}"), label = "Delete trigger")
        Unit
    }

    override suspend fun runTrigger(agentId: String, triggerId: String): AgentTriggerItem = withContext(Dispatchers.IO) {
        val body = client.requestOrThrow("POST", ApiConfig.url("/api/agents/${encodePathSegment(agentId)}/triggers/${encodePathSegment(triggerId)}/run"), "{}", label = "Run trigger")
        parseSingleTrigger(body) ?: AgentTriggerItem(triggerId, agentId, prompt = "Run")
    }
}

fun parseAgents(body: String): List<AgentItem> {
    val arr = unwrapList(body, "agents", "Agents")
    val list = ArrayList<AgentItem>(arr.length())
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        list += AgentItem(
            id = obj.optString("id"),
            name = obj.optString("name"),
            description = obj.optString("description", ""),
            mode = obj.optString("mode", "auto"),
            systemPrompt = obj.optString("systemPrompt", ""),
            model = obj.optString("model").takeIf { it.isNotBlank() && it != "null" },
            pinned = obj.optBoolean("pinned", false),
            isOfficial = obj.optBoolean("isOfficial", false),
            createdAt = obj.optString("createdAt", ""),
            updatedAt = obj.optString("updatedAt", ""),
        )
    }
    return list
}

fun parseSingleAgent(body: String): AgentItem? = runCatching {
    val root = JSONObject(body)
    val obj = root.optJSONObject("agent") ?: root
    AgentItem(
        id = obj.optString("id"),
        name = obj.optString("name"),
        description = obj.optString("description", ""),
        mode = obj.optString("mode", "auto"),
        systemPrompt = obj.optString("systemPrompt", ""),
        model = obj.optString("model").takeIf { it.isNotBlank() && it != "null" },
        pinned = obj.optBoolean("pinned", false),
        isOfficial = obj.optBoolean("isOfficial", false),
        createdAt = obj.optString("createdAt", ""),
        updatedAt = obj.optString("updatedAt", ""),
    )
}.getOrNull()

fun parseInstructions(body: String): List<AgentInstructionItem> {
    val arr = unwrapList(body, "instructions", "Instructions")
    val list = ArrayList<AgentInstructionItem>(arr.length())
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        list += AgentInstructionItem(
            id = obj.optString("id"),
            agentId = obj.optString("agentId"),
            instruction = obj.optString("instruction"),
            isActive = obj.optBoolean("isActive", true),
            createdAt = obj.optString("createdAt", ""),
        )
    }
    return list
}

fun parseSingleInstruction(body: String): AgentInstructionItem? = runCatching {
    val root = JSONObject(body)
    val obj = root.optJSONObject("instruction") ?: root
    AgentInstructionItem(
        id = obj.optString("id"),
        agentId = obj.optString("agentId"),
        instruction = obj.optString("instruction"),
        isActive = obj.optBoolean("isActive", true),
        createdAt = obj.optString("createdAt", ""),
    )
}.getOrNull()

fun parseTriggers(body: String): List<AgentTriggerItem> {
    val arr = unwrapList(body, "triggers", "Triggers")
    val list = ArrayList<AgentTriggerItem>(arr.length())
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        list += AgentTriggerItem(
            id = obj.optString("id"),
            agentId = obj.optString("agentId"),
            conversationId = obj.optString("conversationId").takeIf { it.isNotBlank() && it != "null" },
            triggerType = obj.optString("triggerType", "schedule"),
            cronExpr = obj.optString("cronExpr").takeIf { it.isNotBlank() && it != "null" },
            prompt = obj.optString("prompt"),
            enabled = obj.optBoolean("enabled", true),
            lastRunAt = obj.optString("lastRunAt").takeIf { it.isNotBlank() && it != "null" },
            createdAt = obj.optString("createdAt", ""),
        )
    }
    return list
}

fun parseSingleTrigger(body: String): AgentTriggerItem? = runCatching {
    val root = JSONObject(body)
    val obj = root.optJSONObject("trigger") ?: root
    AgentTriggerItem(
        id = obj.optString("id"),
        agentId = obj.optString("agentId"),
        conversationId = obj.optString("conversationId").takeIf { it.isNotBlank() && it != "null" },
        triggerType = obj.optString("triggerType", "schedule"),
        cronExpr = obj.optString("cronExpr").takeIf { it.isNotBlank() && it != "null" },
        prompt = obj.optString("prompt"),
        enabled = obj.optBoolean("enabled", true),
        lastRunAt = obj.optString("lastRunAt").takeIf { it.isNotBlank() && it != "null" },
        createdAt = obj.optString("createdAt", ""),
    )
}.getOrNull()

class AgentsRepository(
    dataSource: AgentsDataSource? = null,
) : BaseRemoteRepository<AgentItem, AgentsUiState>(
    source = dataSource ?: (if (TokenStore.token != null) RemoteAgentsDataSource() else InMemoryAgentsDataSource()),
) {
    override fun loadingState(): AgentsUiState = AgentsUiState.Loading
    override fun loadedState(items: List<AgentItem>): AgentsUiState = AgentsUiState.Loaded(items)
    override fun errorState(message: String, retryable: Boolean): AgentsUiState = AgentsUiState.Error(message, retryable)
    override fun itemsOf(state: AgentsUiState): List<AgentItem>? = (state as? AgentsUiState.Loaded)?.agents
    override fun withId(item: AgentItem, id: String): AgentItem = item.copy(id = id)
    override val errorFallback: String = "Agents load nahi ho paye."

    val agentsSource: AgentsDataSource get() = source as AgentsDataSource

    suspend fun update(item: AgentItem): WriteResult = try {
        val updated = agentsSource.update(item)
        val current = (state.value as? AgentsUiState.Loaded)?.agents.orEmpty()
        val next = current.map { if (it.id == item.id) updated else it }
        _state.value = AgentsUiState.Loaded(next)
        WriteResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        WriteResult.Failure(e.message ?: "Failed to update agent.")
    }

    suspend fun generatePrompt(description: String, name: String = "", mode: String = "auto"): String =
        agentsSource.generatePrompt(description, name, mode)

    suspend fun fetchInstructions(agentId: String): List<AgentInstructionItem> = agentsSource.fetchInstructions(agentId)
    suspend fun addInstruction(agentId: String, instruction: String, isActive: Boolean = true): AgentInstructionItem =
        agentsSource.addInstruction(agentId, instruction, isActive)
    suspend fun updateInstruction(agentId: String, instructionId: String, instruction: String, isActive: Boolean): AgentInstructionItem =
        agentsSource.updateInstruction(agentId, instructionId, instruction, isActive)
    suspend fun deleteInstruction(agentId: String, instructionId: String) =
        agentsSource.deleteInstruction(agentId, instructionId)

    suspend fun fetchTriggers(agentId: String): List<AgentTriggerItem> = agentsSource.fetchTriggers(agentId)
    suspend fun addTrigger(agentId: String, triggerType: String, cronExpr: String?, prompt: String, conversationId: String?, enabled: Boolean): AgentTriggerItem =
        agentsSource.addTrigger(agentId, triggerType, cronExpr, prompt, conversationId, enabled)
    suspend fun updateTrigger(agentId: String, triggerId: String, triggerType: String, cronExpr: String?, prompt: String, conversationId: String?, enabled: Boolean): AgentTriggerItem =
        agentsSource.updateTrigger(agentId, triggerId, triggerType, cronExpr, prompt, conversationId, enabled)
    suspend fun deleteTrigger(agentId: String, triggerId: String) =
        agentsSource.deleteTrigger(agentId, triggerId)
    suspend fun runTrigger(agentId: String, triggerId: String): AgentTriggerItem =
        agentsSource.runTrigger(agentId, triggerId)
}

/* ============================================================================
 *  Skills
 * ==========================================================================*/

data class SkillItem(
    override val id: String,
    val agentId: String = "dua-agent",
    val title: String,
    val summary: String = "",
    val body: String = "",
    val enabled: Boolean = true,
    val isStarter: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) : Identifiable {
    val name: String get() = title
    val description: String get() = summary
}

sealed interface SkillsUiState {
    object Loading : SkillsUiState
    data class Loaded(val skills: List<SkillItem>) : SkillsUiState
    data class Error(val message: String, val retryable: Boolean = true) : SkillsUiState
}

interface SkillsDataSource : RemoteDataSource<SkillItem> {
    suspend fun fetch(agentId: String? = null): List<SkillItem>
    suspend fun update(item: SkillItem): SkillItem
    suspend fun generateSkill(description: String, title: String = ""): SkillItem
}

class InMemorySkillsDataSource(
    seed: List<SkillItem> = DuaAgentSkills.all.map {
        SkillItem(
            id = it.id,
            title = it.title,
            summary = it.summary,
            enabled = true,
            isStarter = true,
        )
    },
) : SkillsDataSource {
    private val items = seed.toMutableList()

    override suspend fun fetch(): List<SkillItem> = items.toList()
    override suspend fun fetch(agentId: String?): List<SkillItem> =
        if (agentId.isNullOrBlank()) items.toList() else items.filter { it.agentId == agentId }

    override suspend fun add(item: SkillItem): SkillItem {
        items.removeAll { it.id == item.id }
        items.add(item)
        return item
    }

    override suspend fun update(item: SkillItem): SkillItem {
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) items[idx] = item else items.add(item)
        return item
    }

    override suspend fun delete(id: String) {
        val skill = items.firstOrNull { it.id == id }
        if (skill?.isStarter == true) throw IllegalStateException("Starter skills cannot be deleted.")
        items.removeAll { it.id == id }
    }

    override suspend fun generateSkill(description: String, title: String): SkillItem =
        SkillItem(
            id = if (title.isNotBlank()) title.lowercase().replace(" ", "-") else "custom-skill",
            title = title.ifBlank { "Custom Skill" },
            summary = description.take(60),
            body = "Instructions for $description",
        )
}

class RemoteSkillsDataSource(
    private val client: JsonHttpClient = HttpClientProvider.client,
) : SkillsDataSource {
    override suspend fun fetch(): List<SkillItem> = fetch(null)

    override suspend fun fetch(agentId: String?): List<SkillItem> = withContext(Dispatchers.IO) {
        val path = if (!agentId.isNullOrBlank()) "/api/skills?agent_id=${encodePathSegment(agentId)}" else "/api/skills"
        val body = client.requestOrThrow("GET", ApiConfig.url(path), label = "Skills request")
        parseSkills(body)
    }

    override suspend fun add(item: SkillItem): SkillItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            if (item.id.isNotBlank()) put("id", item.id)
            put("agentId", item.agentId)
            put("title", item.title)
            put("summary", item.summary)
            put("body", item.body)
            put("enabled", item.enabled)
        }.toString()
        val body = client.requestOrThrow("POST", ApiConfig.url("/api/skills"), payload, label = "Create skill")
        parseSingleSkill(body) ?: item
    }

    override suspend fun update(item: SkillItem): SkillItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("title", item.title)
            put("summary", item.summary)
            put("body", item.body)
            put("enabled", item.enabled)
            put("agentId", item.agentId)
        }.toString()
        val body = client.requestOrThrow("PUT", ApiConfig.url("/api/skills/${encodePathSegment(item.id)}"), payload, label = "Update skill")
        parseSingleSkill(body) ?: item
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        client.requestOrThrow("DELETE", ApiConfig.url("/api/skills/${encodePathSegment(id)}"), label = "Delete skill")
        Unit
    }

    override suspend fun generateSkill(description: String, title: String): SkillItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("description", description).put("title", title).toString()
        val body = client.requestOrThrow("POST", ApiConfig.url("/api/skills/generate"), payload, label = "Generate skill")
        parseSingleSkill(body) ?: SkillItem(id = "custom-skill", title = title.ifBlank { "Custom Skill" }, summary = description)
    }
}

fun parseSkills(body: String): List<SkillItem> {
    val arr = unwrapList(body, "skills", "Skills")
    val list = ArrayList<SkillItem>(arr.length())
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        list += SkillItem(
            id = obj.optString("id"),
            agentId = obj.optString("agentId", "dua-agent"),
            title = obj.optString("title"),
            summary = obj.optString("summary", ""),
            body = obj.optString("body", ""),
            enabled = obj.optBoolean("enabled", true),
            isStarter = obj.optBoolean("isStarter", false),
            createdAt = obj.optString("createdAt").takeIf { it.isNotBlank() && it != "null" },
            updatedAt = obj.optString("updatedAt").takeIf { it.isNotBlank() && it != "null" },
        )
    }
    return list
}

fun parseSingleSkill(body: String): SkillItem? = runCatching {
    val root = JSONObject(body)
    val obj = root.optJSONObject("skill") ?: root
    SkillItem(
        id = obj.optString("id"),
        agentId = obj.optString("agentId", "dua-agent"),
        title = obj.optString("title"),
        summary = obj.optString("summary", ""),
        body = obj.optString("body", ""),
        enabled = obj.optBoolean("enabled", true),
        isStarter = obj.optBoolean("isStarter", false),
        createdAt = obj.optString("createdAt").takeIf { it.isNotBlank() && it != "null" },
        updatedAt = obj.optString("updatedAt").takeIf { it.isNotBlank() && it != "null" },
    )
}.getOrNull()

class SkillsRepository(
    dataSource: SkillsDataSource? = null,
) : BaseRemoteRepository<SkillItem, SkillsUiState>(
    source = dataSource ?: (if (TokenStore.token != null) RemoteSkillsDataSource() else InMemorySkillsDataSource()),
) {
    override fun loadingState(): SkillsUiState = SkillsUiState.Loading
    override fun loadedState(items: List<SkillItem>): SkillsUiState = SkillsUiState.Loaded(items)
    override fun errorState(message: String, retryable: Boolean): SkillsUiState = SkillsUiState.Error(message, retryable)
    override fun itemsOf(state: SkillsUiState): List<SkillItem>? = (state as? SkillsUiState.Loaded)?.skills
    override fun withId(item: SkillItem, id: String): SkillItem = item.copy(id = id)
    override val errorFallback: String = "Skills load nahi ho paye."

    val skillsSource: SkillsDataSource get() = source as SkillsDataSource

    suspend fun fetchForAgent(agentId: String): List<SkillItem> = skillsSource.fetch(agentId)

    suspend fun update(item: SkillItem): WriteResult = try {
        val updated = skillsSource.update(item)
        val current = (state.value as? SkillsUiState.Loaded)?.skills.orEmpty()
        val next = current.map { if (it.id == item.id) updated else it }
        _state.value = SkillsUiState.Loaded(next)
        WriteResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        WriteResult.Failure(e.message ?: "Failed to update skill.")
    }

    suspend fun generateSkill(description: String, title: String = ""): SkillItem =
        skillsSource.generateSkill(description, title)
}
