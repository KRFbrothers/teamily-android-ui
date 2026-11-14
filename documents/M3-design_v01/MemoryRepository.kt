package ai.duagenai.design.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
    data class Error(val message: String) : MemoryUiState
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
    const val BASE_URL: String = ApiConfig.BASE_URL

    /**
     * Cloud snapshot on the Node API. Notes are `snapshot.memory`, up to 10
     * strings of 200 characters. A JSON array body is still accepted so the
     * design-kit parser test can inject its sample list.
     */
    const val MEMORY_PATH: String = "/api/cloud"

    /** The full list endpoint. */
    val memoriesUrl: String get() = BASE_URL + MEMORY_PATH
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

    /** Inserts [item] at the top of the list. Throws on failure. */
    override suspend fun add(item: MemoryItem)

    /** Removes the memory with [id]. Throws on failure. */
    override suspend fun delete(id: String)
}

/**
 * The demo memories the in-memory source seeds itself with.
 *
 * This is a design kit, so the entries are realistic sample data rather than a
 * live store — the same choice `sampleChats` and `sampleAgents` make. They are
 * written to the product model: things Dua and Agent would actually remember
 * about this user.
 */
internal val sampleMemories = listOf(
    MemoryItem(
        id = "mem_launch_q3",
        title = "Q3 launch is the current priority",
        summary = "Deck, competitor research and invoice reconciliation are split across three agents.",
        timestamp = "2 din pehle",
        category = "Project",
        // Has a URL: the long-press "Open" hands it to the shared UrlOpener.
        url = "https://static.teamily.ai/sites/demo/q3-launch-brief/index.html",
    ),
    MemoryItem(
        id = "mem_brand_green",
        title = "Brand green is #087C45",
        summary = "Prefers the Dua Gen AI green with the mint accent — never purple or blue.",
        timestamp = "4 din pehle",
        category = "Preference",
        // Deliberately has NO url: it is the memory that makes the long-press
        // "Open" fall back to the detail dialog, so that path is reachable and
        // testable rather than theoretical.
        url = null,
    ),
    MemoryItem(
        id = "mem_deck_style",
        title = "Decks: 12 slides, brand theme",
        summary = "Slides Assistant should always export with the brand theme applied.",
        timestamp = "1 hafta pehle",
        category = "Preference",
        url = "https://static.teamily.ai/sites/demo/brand-theme-guide.pdf",
    ),
    MemoryItem(
        id = "mem_market_brief",
        title = "Competitor brief needs cited sources",
        summary = "Market Research must cite every claim; eight sources was the last accepted count.",
        timestamp = "1 hafta pehle",
        category = "Project",
        url = "https://static.teamily.ai/sites/demo/competitor-brief.pdf",
    ),
    MemoryItem(
        id = "mem_invoice_2211",
        title = "Invoice #2211 flagged for review",
        summary = "Accounting Assistant is waiting on a human decision before it reconciles this one.",
        timestamp = "2 hafte pehle",
        category = "Finance",
        url = "https://static.teamily.ai/sites/demo/invoice-2211.pdf",
    ),
)

/**
 * The in-memory data source — the demo set, and the default.
 *
 * @param seed the initial list. Defaults to [sampleMemories]; a test can pass
 *   `emptyList()` to exercise the empty state.
 * @param failOnLoad when true, [fetch] throws — the hook the error-state test
 *   uses.
 * @param loadDelayMs how long [fetch] takes. Non-zero so the Loading state is
 *   observable; a test can pass 0 to skip the wait.
 */
class InMemoryMemoryDataSource(
    seed: List<MemoryItem> = sampleMemories,
    private val failOnLoad: Boolean = false,
    private val loadDelayMs: Long = 350L,
) : MemoryDataSource {

    private val items: MutableList<MemoryItem> = seed.toMutableList()

    override suspend fun fetch(): List<MemoryItem> {
        if (loadDelayMs > 0) delay(loadDelayMs)
        if (failOnLoad) throw IllegalStateException("Memories load nahi ho paye.")
        return items.toList()
    }

    override suspend fun add(item: MemoryItem) {
        items.add(0, item)
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

    /** Notes loaded from `snapshot.memory`, kept so add/delete can write them back. */
    private var cloudRevision: Long = 0
    private var cloudOwnerId: String = ""
    private var cloudSnapshot: JSONObject? = null
    private val cloudNotes = mutableListOf<Pair<String, String>>()

    override suspend fun fetch(): List<MemoryItem> = withContext(Dispatchers.IO) {
        val body = client.request("GET", baseUrl, label = "Memory request").trim()
        if (body.startsWith("[")) {
            cloudSnapshot = null
            cloudNotes.clear()
            parseMemories(body)
        } else {
            parseCloud(body)
        }
    }

    override suspend fun add(item: MemoryItem) = withContext(Dispatchers.IO) {
        val snap = cloudSnapshot
        if (snap == null) {
            val payload = """
                {"id":"${item.id}","title":"${item.title}","summary":"${item.summary}",
                 "timestamp":"${item.timestamp}","category":"${item.category}",
                 "url":${item.url?.let { "\"$it\"" } ?: "null"}}
            """.trimIndent()
            client.request("POST", baseUrl, payload, label = "Memory request")
            return@withContext
        }
        val note = memoryNote(item)
        if (cloudNotes.size >= 10) {
            throw IllegalStateException("Cloud memory limit is 10 notes.")
        }
        val next = ArrayList(cloudNotes)
        next.add(0, item.id to note)
        writeCloud(next)
        cloudNotes.clear()
        cloudNotes.addAll(next)
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        if (cloudSnapshot == null) {
            client.request("DELETE", "$baseUrl/$id", label = "Memory request")
            return@withContext
        }
        if (cloudNotes.none { it.first == id }) throw IllegalStateException("Unknown memory note.")
        val next = cloudNotes.filterNot { it.first == id }
        writeCloud(next)
        cloudNotes.clear()
        cloudNotes.addAll(next)
    }

    /** Parses the design-kit list payload. Throws on malformed JSON. */
    private fun parseMemories(body: String): List<MemoryItem> {
        val array = JSONArray(body)
        val out = ArrayList<MemoryItem>(array.length())
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            out += MemoryItem(
                id = o.getString("id"),
                title = o.getString("title"),
                summary = o.optString("summary", ""),
                timestamp = o.optString("timestamp", ""),
                category = o.optString("category", ""),
                // A blank or absent url is null, not an empty string: the screen
                // branches on null to pick the detail-dialog fallback, so an
                // empty string would slip past that check and open nothing.
                url = o.optString("url", "").takeIf { it.isNotBlank() },
            )
        }
        return out
    }

    /** `GET /api/cloud` — notes are plain strings on `snapshot.memory`. */
    private fun parseCloud(body: String): List<MemoryItem> {
        val root = JSONObject(body)
        cloudRevision = root.optLong("revision", 0L)
        cloudOwnerId = root.optString("ownerId")
        val snap = root.getJSONObject("snapshot")
        cloudSnapshot = snap
        val notes = snap.optJSONArray("memory") ?: JSONArray()
        cloudNotes.clear()
        val out = ArrayList<MemoryItem>(notes.length())
        for (i in 0 until notes.length()) {
            val note = notes.getString(i)
            val id = "memory:$i"
            cloudNotes += id to note
            out += MemoryItem(
                id = id,
                title = note,
                summary = note,
                timestamp = "",
                category = "Memory",
            )
        }
        return out
    }

    private fun memoryNote(item: MemoryItem): String {
        val note = item.summary.ifBlank { item.title }.trim()
        if (note.isEmpty() || note.length > 200) {
            throw IllegalStateException("Memory notes are 200 characters or shorter.")
        }
        return note
    }

    /** Writes the same snapshot back with only the memory notes changed. */
    private fun writeCloud(notes: List<Pair<String, String>>) {
        if (cloudOwnerId.isBlank()) throw IllegalStateException("Cloud account is missing.")
        val snap = JSONObject(cloudSnapshot.toString())
        val memory = JSONArray()
        for ((_, note) in notes) memory.put(note)
        snap.put("memory", memory)
        val body = JSONObject()
            .put("expectedRevision", cloudRevision)
            .put("expectedAccountId", cloudOwnerId)
            .put("snapshot", snap)
        val saved = JSONObject(client.request("PUT", baseUrl, body.toString(), label = "Memory request"))
        if (saved.has("revision")) cloudRevision = saved.getLong("revision")
        cloudSnapshot = snap
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
 *   in-memory demo source, so the kit runs with no backend.
 * @param seed / @param failOnLoad / @param loadDelayMs configure the in-memory
 *   source when [dataSource] is left at its default.
 */
class MemoryRepository(
    dataSource: MemoryDataSource? = null,
    seed: List<MemoryItem> = sampleMemories,
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
    override fun errorState(message: String): MemoryUiState =
        MemoryUiState.Error(message)

    /** The loaded list inside [state], or null while loading / in error. */
    override fun itemsOf(state: MemoryUiState): List<MemoryItem>? =
        (state as? MemoryUiState.Loaded)?.memories

    /** The message used when an exception carries none of its own. */
    override val errorFallback: String = "Memories load nahi ho paye."
}
