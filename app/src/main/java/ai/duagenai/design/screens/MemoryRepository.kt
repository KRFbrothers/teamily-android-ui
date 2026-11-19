package ai.duagenai.design.screens

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/* ============================================================================
 *  Memory — the data source
 *
 *  The Memory screen used to take a plain `List<MemoryItem>` parameter. It now
 *  reads from this repository instead, so the screen holds no data of its own
 *  and pull-to-refresh has a `load()` to re-run.
 *
 *  WHY A REPOSITORY AND NOT A VIEW-MODEL
 *  -------------------------------------
 *  Exactly the same reasoning as [ArtifactsRepository]: the kit has no
 *  view-model layer and no DI framework, so this is a plain class the nav host
 *  creates once and passes down, exposing its state as a [StateFlow]
 *  (kotlinx.coroutines, already a dependency). No new library.
 *
 *  THE DATA SOURCE SEAM
 *  --------------------
 *  The repository talks to a [MemoryDataSource], and there are two
 *  implementations — [InMemoryMemoryDataSource] (the demo set, the default) and
 *  [RemoteMemoryDataSource] (a real HTTP source reading its endpoint from
 *  [MemoryConfig]). Swapping the demo for a backend is a one-line change at the
 *  construction site; the screen only ever sees [MemoryUiState].
 *
 *  PULL-TO-REFRESH
 *  ---------------
 *  [refresh] re-runs [load] WITHOUT going through [MemoryUiState.Loading], so
 *  the list stays on screen under the indicator — the same contract
 *  [ArtifactsRepository.refresh] has. [isRefreshing] carries the in-flight
 *  signal, and [loadCount] is the observable proof the gesture reached here.
 * ==========================================================================*/

/**
 * One remembered fact.
 *
 * Deliberately flat and immutable: it is what the screen renders, nothing more.
 * [category] is a short human label ("Preference", "Project", …) that doubles as
 * the card's category indicator, so no icon font is needed for it.
 */
data class MemoryItem(
    override val id: String,
    val title: String,
    val summary: String,
    val timestamp: String,
    val category: String,
    /**
     * The memory's source URL, or null when it has none.
     *
     * The long-press "Open" action hands it to the shared [UrlOpener]; with no
     * URL it falls back to the detail dialog, so a tap is never a silent no-op.
     * Null is a real state (a memory that has no link), not a missing value.
     */
    val url: String? = null,
    val scope: String = "global",
    val sourceConversationId: String? = null,
) : Identifiable

/**
 * What the Memory screen can be showing.
 *
 * A sealed interface rather than a bag of booleans, so "loading AND loaded" is
 * not representable — the screen renders exactly one of these.
 */
sealed interface MemoryUiState {
    /** The first load is in flight. */
    object Loading : MemoryUiState

    /** The load finished. [memories] may be empty — that is the empty state. */
    data class Loaded(val memories: List<MemoryItem>) : MemoryUiState

    /** The load failed. [message] is shown with a Retry action. */
    data class Error(
        val message: String,
        /**
         * Whether trying again could plausibly succeed.
         *
         * A 401/403 is false: the request goes out anonymous again and fails
         * identically, so offering "Retry" for it was a dead button. The screen
         * shows "Sign in" instead.
         */
        val retryable: Boolean = true,
    ) : MemoryUiState
}

/**
 * The one place the memory backend's address is defined.
 *
 * A screen must never hardcode a URL, and neither should the repository: the
 * endpoint lives here so a developer pointing the app at a real service changes
 * exactly one constant. The value is a clearly-named placeholder — there is no
 * live service behind it yet — which is why the default data source is the
 * in-memory one.
 */
object MemoryConfig {
    /**
     * The API host.
     *
     * NOT defined here any more: the host is a property of the backend, not of
     * this feature, so it lives in [ApiConfig] and both Memory and Artifacts read
     * it. Pointing the app at a real service is one edit there, not one per
     * feature.
     */
    val BASE_URL: String get() = ApiConfig.BASE_URL

    /**
     * The path the memory list is served from, relative to [BASE_URL].
     *
     * `/api/memories` — the live service mounts its routes under `/api`.
     */
    const val MEMORY_PATH: String = "/api/memories"

    /** The full list endpoint. */
    val memoriesUrl: String get() = ApiConfig.url(MEMORY_PATH)
}

/**
 * The seam between the repository and wherever the memories actually live.
 *
 * Every method is `suspend` and may throw: the repository turns a thrown
 * exception into [MemoryUiState.Error], so a data source only has to report
 * failure, not model it.
 */
interface MemoryDataSource : RemoteDataSource<MemoryItem> {
    /** Fetches the full list. Throws on network / parse failure. */
    override suspend fun fetch(): List<MemoryItem>

    suspend fun fetch(scope: String? = null): List<MemoryItem> = fetch()

    /**
     * Inserts [item] and returns what the SERVER stored.
     *
     * Returning the stored item (not Unit) is the fix for a real data-loss bug:
     * the POST response was discarded, so the server-assigned `id` and
     * `timestamp` were thrown away and the screen kept showing the client's
     * guess. A later DELETE then used an id the server had never issued.
     */
    override suspend fun add(item: MemoryItem): MemoryItem

    /** Removes the memory with [id]. Throws on failure. */
    override suspend fun delete(id: String)

    suspend fun updateScope(id: String, scope: String, sourceConversationId: String? = null): MemoryItem =
        MemoryItem(id, "Memory", "", "", "Preference", scope = scope, sourceConversationId = sourceConversationId)
}

/**
 * The in-memory data source — the demo set, and the default.
 *
 * @param seed the initial list. Defaults to EMPTY: a production build must never
 *   ship fake rows. [sampleMemories] is a preview/test fixture and is passed
 *   explicitly by the `@Preview` functions and the tests that need data.
 * @param failOnLoad when true, [fetch] throws — the hook the error-state test
 *   uses.
 * @param loadDelayMs how long [fetch] takes. Non-zero so the Loading state is
 *   observable; a test can pass 0 to skip the wait.
 */
class InMemoryMemoryDataSource(
    seed: List<MemoryItem> = emptyList(),
    private val failOnLoad: Boolean = false,
    private val loadDelayMs: Long = 350L,
) : MemoryDataSource {

    private val items: MutableList<MemoryItem> = seed.toMutableList()

    override suspend fun fetch(): List<MemoryItem> {
        if (loadDelayMs > 0) delay(loadDelayMs)
        if (failOnLoad) throw IllegalStateException("Memories load nahi ho paye.")
        return items.toList()
    }

    override suspend fun add(item: MemoryItem): MemoryItem {
        items.add(0, item)
        return item
    }

    override suspend fun delete(id: String) {
        items.removeAll { it.id == id }
    }
}

/**
 * The real HTTP data source.
 *
 * The HTTP mechanics live in the shared [JsonHttpClient] — this class keeps only
 * what is genuinely its own: the endpoint ([MemoryConfig], never a literal here)
 * and the JSON shape. `org.json` is already on the platform, so wiring a backend
 * adds no library.
 */
class RemoteMemoryDataSource(
    private val baseUrl: String = MemoryConfig.memoriesUrl,
    /**
     * The shared HTTP client. Public read-only so a test can prove both remote
     * sources default to the ONE [HttpClientProvider] instance; still injectable
     * so a test can pass a fake-transport client.
     */
    val client: JsonHttpClient = HttpClientProvider.client,
) : MemoryDataSource {

    override suspend fun fetch(): List<MemoryItem> = fetch(null)

    override suspend fun fetch(scope: String?): List<MemoryItem> = withContext(Dispatchers.IO) {
        val url = if (!scope.isNullOrBlank()) {
            "$baseUrl?scope=${encodePathSegment(scope)}"
        } else {
            baseUrl
        }
        parseMemories(client.requestOrThrow("GET", url, label = "Memory request"))
    }

    override suspend fun updateScope(id: String, scope: String, sourceConversationId: String?): MemoryItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("scope", scope)
            if (sourceConversationId != null) {
                put("sourceConversationId", sourceConversationId)
            } else {
                put("sourceConversationId", JSONObject.NULL)
            }
        }.toString()
        val response = client.requestOrThrow("PUT", "$baseUrl/${encodePathSegment(id)}", payload, label = "Memory scope update")
        parseMemoryOrFallback(response, MemoryItem(id, "Memory", "", "", "Preference", scope = scope, sourceConversationId = sourceConversationId))
    }

    override suspend fun add(item: MemoryItem): MemoryItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("summary", item.summary)
            put("timestamp", item.timestamp)
            put("category", item.category)
            put("url", item.url ?: JSONObject.NULL)
            put("scope", item.scope)
            if (item.sourceConversationId != null) {
                put("sourceConversationId", item.sourceConversationId)
            }
        }.toString()
        val response = client.requestOrThrow("POST", baseUrl, payload, label = "Memory request")
        parseMemoryOrFallback(response, item)
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        client.requestOrThrow(
            "DELETE",
            "$baseUrl/${encodePathSegment(id)}",
            label = "Memory request",
        )
        Unit
    }

    /** Parses the list payload. Throws on malformed JSON. */
    private fun parseMemories(body: String): List<MemoryItem> {
        val array = unwrapList(body, key = "memories", label = "Memory request")
        val out = ArrayList<MemoryItem>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id", "").takeIf { it.isNotBlank() } ?: continue
            out += MemoryItem(
                id = id,
                title = o.optString("title", "").ifBlank { "Untitled memory" },
                summary = o.optString("summary", ""),
                timestamp = o.optString("timestamp", ""),
                category = o.optString("category", "").ifBlank { "Note" },
                url = o.optString("url", "").takeIf { it.isNotBlank() },
                scope = o.optString("scope", "global"),
                sourceConversationId = o.optString("sourceConversationId").takeIf { it.isNotBlank() && it != "null" },
            )
        }
        return out
    }

    /**
     * Parses the POST response into the stored item, falling back to [sent].
     */
    private fun parseMemoryOrFallback(body: String, sent: MemoryItem): MemoryItem {
        val text = body.trim()
        if (!text.startsWith("{")) return sent
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return sent
        val o = root.optJSONObject("memory") ?: root
        val id = o.optString("id", "").takeIf { it.isNotBlank() } ?: return sent
        return MemoryItem(
            id = id,
            title = o.optString("title", "").ifBlank { sent.title },
            summary = o.optString("summary", "").ifBlank { sent.summary },
            timestamp = o.optString("timestamp", "").ifBlank { sent.timestamp },
            category = o.optString("category", "").ifBlank { sent.category },
            url = o.optString("url", "").takeIf { it.isNotBlank() } ?: sent.url,
            scope = o.optString("scope", sent.scope),
            sourceConversationId = o.optString("sourceConversationId").takeIf { it.isNotBlank() && it != "null" } ?: sent.sourceConversationId,
        )
    }
}

/**
 * The Memory data source facade the screen talks to.
 *
 * It owns the [MemoryUiState] and the pull-to-refresh flag, and forwards every
 * load to a [MemoryDataSource]. The screen never sees the data source, so
 * swapping [InMemoryMemoryDataSource] for [RemoteMemoryDataSource] changes
 * nothing above this class.
 *
 * @param dataSource where the memories actually come from. Defaults to the
 *   in-memory source, which starts EMPTY — a production build must never ship
 *   fake rows.
 * @param seed / @param failOnLoad / @param loadDelayMs configure the in-memory
 *   source when [dataSource] is left at its default.
 */
class MemoryRepository(
    dataSource: MemoryDataSource? = null,
    seed: List<MemoryItem> = emptyList(),
    failOnLoad: Boolean = false,
    loadDelayMs: Long = 350L,
) : BaseRemoteRepository<MemoryItem, MemoryUiState>(
    source = dataSource ?: InMemoryMemoryDataSource(
        seed = seed,
        failOnLoad = failOnLoad,
        loadDelayMs = loadDelayMs,
    ),
) {

    /** The state shown while the first load is in flight. */
    override fun loadingState(): MemoryUiState = MemoryUiState.Loading

    /** The state wrapping the fetched list (may be empty — the empty state). */
    override fun loadedState(items: List<MemoryItem>): MemoryUiState =
        MemoryUiState.Loaded(items)

    /** The state wrapping a failure message, shown with a Retry action. */
    override fun errorState(message: String, retryable: Boolean): MemoryUiState =
        MemoryUiState.Error(message, retryable)

    /** The loaded list inside [state], or null while loading / in error. */
    override fun itemsOf(state: MemoryUiState): List<MemoryItem>? =
        (state as? MemoryUiState.Loaded)?.memories

    /** A copy of [item] carrying [id] — the undo path's new identity. */
    override fun withId(item: MemoryItem, id: String): MemoryItem = item.copy(id = id)

    /** The message used when an exception carries none of its own. */
    override val errorFallback: String = "Memories load nahi ho paye."

    /** Shown when a new memory could not be saved. */
    override val addFailureFallback: String = "Memory save nahi ho payi."

    /** Shown when a memory could not be deleted. */
    override val deleteFailureFallback: String = "Memory delete nahi ho payi."

    suspend fun toggleScope(id: String, currentScope: String): WriteResult {
        val newScope = if (currentScope.startsWith("group")) "global" else "group"
        return try {
            val updated = (source as? RemoteMemoryDataSource)?.updateScope(id, newScope)
                ?: (source as? MemoryDataSource)?.updateScope(id, newScope)
            if (updated != null) {
                val current = (state.value as? MemoryUiState.Loaded)?.memories.orEmpty()
                val next = current.map { if (it.id == id) updated else it }
                _state.value = MemoryUiState.Loaded(next)
            }
            WriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WriteResult.Failure(e.message ?: "Failed to update memory scope.")
        }
    }
}
