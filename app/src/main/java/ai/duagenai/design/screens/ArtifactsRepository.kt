package ai.duagenai.design.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/* ============================================================================
 *  Artifacts — the data source
 *
 *  The Artifacts screen used to read a hardcoded `sampleArtifacts` list. It now
 *  reads from this repository instead, so the screen holds no data of its own.
 *
 *  WHY A REPOSITORY AND NOT A VIEW-MODEL
 *  -------------------------------------
 *  The kit has no view-model layer and no DI framework — every screen takes its
 *  data as a plain parameter. This repository keeps that shape: it is a plain
 *  class the nav host creates once and passes down, and it exposes its state as
 *  a [StateFlow] (kotlinx.coroutines, already a dependency) so the screen can
 *  collect it the same way it collects `CubeStore.selected`. No new library.
 *
 *  THE DATA SOURCE SEAM (backend wiring)
 *  -------------------------------------
 *  The repository no longer owns the data. It talks to an [ArtifactsDataSource],
 *  and there are two implementations:
 *
 *    • [InMemoryArtifactsDataSource] — the demo set, used by the preview and by
 *      tests. It is what the kit ships with.
 *    • [RemoteArtifactsDataSource]  — a real HTTP source. It reads the endpoint
 *      from [ArtifactsConfig.BASE_URL] (ONE place, never a literal in a screen)
 *      and parses the JSON the API returns.
 *
 *  Swapping the demo for the backend is therefore a one-line change at the
 *  construction site — the UI, the filter, the caption, pull-to-refresh, swipe
 *  and the context menu all keep working unchanged, because they only ever see
 *  [ArtifactsUiState].
 *
 *  THREE OPERATIONS, THREE STATES
 *  ------------------------------
 *    • [load]   -> Loading, then Loaded (or Error on failure)
 *    • [add]    -> prepends a new artifact to the loaded list
 *    • [delete] -> removes one by id
 *
 *  Every one of those goes THROUGH the data source, so a real backend stays in
 *  sync with what the screen shows.
 * ==========================================================================*/

/**
 * One generated artifact.
 *
 * Deliberately flat and immutable: it is what the screen renders, nothing more.
 * [type] is a short human label ("Image", "Document", "Webpage", "Video") that
 * doubles as the card's type indicator, so no icon font is needed for it.
 */
data class ArtifactItem(
    override val id: String,
    val title: String,
    val type: String,
    val timestamp: String,
    /**
     * The artifact's payload size in bytes.
     *
     * Added for audit fix #8: the preview caption must state the artifact's
     * NAME, TYPE and SIZE, so the size has to be data, not a string the screen
     * invents. [artifactSizeLabel] turns it into the caption's text.
     */
    val sizeBytes: Long,
    /**
     * The artifact's public URL, or null when it has none.
     *
     * Added for the context menu's "Open in new tab" action: the item is only
     * enabled when there is a URL to open, so the menu can never offer a tap
     * that does nothing. Null is a real state (an artifact that was never
     * published), not a missing value.
     */
    val url: String? = null,
) : Identifiable

/**
 * What the Artifacts screen can be showing.
 *
 * A sealed interface rather than a bag of booleans, so "loading AND loaded" is
 * not representable — the screen renders exactly one of these.
 */
sealed interface ArtifactsUiState {
    /** The first load is in flight. */
    object Loading : ArtifactsUiState

    /** The load finished. [artifacts] may be empty — that is the empty state. */
    data class Loaded(val artifacts: List<ArtifactItem>) : ArtifactsUiState

    /** The load failed. [message] is shown with a Retry action. */
    data class Error(val message: String) : ArtifactsUiState
}

/**
 * The one place the backend's address is defined.
 *
 * A screen must never hardcode a URL, and neither should the repository: the
 * endpoint lives here so a developer pointing the app at a real service changes
 * exactly one constant. The value is a clearly-named placeholder — there is no
 * live service behind it yet — which is why the default data source is the
 * in-memory one.
 */
object ArtifactsConfig {
    /**
     * The API host.
     *
     * NOT defined here: the host is a property of the backend, not of this
     * feature, so it lives in [ApiConfig] and both Artifacts and Memory read it.
     *
     * A `val`, not a `const val`: [ApiConfig.BASE_URL] is resolved per access
     * (build-type value, or a runtime override), so it cannot be a compile-time
     * constant.
     */
    val BASE_URL: String get() = ApiConfig.BASE_URL

    /**
     * The path the artifacts list is served from, relative to [BASE_URL].
     *
     * `/api/artifacts` — the live service mounts its routes under `/api`.
     */
    const val ARTIFACTS_PATH: String = "/api/artifacts"

    /** The full list endpoint. */
    val artifactsUrl: String get() = ApiConfig.url(ARTIFACTS_PATH)
}

/**
 * The seam between the repository and wherever the artifacts actually live.
 *
 * Every method is `suspend` and may throw: the repository turns a thrown
 * exception into [ArtifactsUiState.Error], so a data source only has to report
 * failure, not model it.
 */
interface ArtifactsDataSource : RemoteDataSource<ArtifactItem> {
    /** Fetches the full list. Throws on network / parse failure. */
    override suspend fun fetch(): List<ArtifactItem>

    /** Persists a new artifact. Throws on failure. */
    override suspend fun add(item: ArtifactItem)

    /** Removes an artifact by id. Throws on failure. */
    override suspend fun delete(id: String)
}

/**
 * The demo artifacts the in-memory source seeds itself with.
 *
 * This is a design kit, so the entries are realistic sample data rather than a
 * live store — the same choice `sampleMemories` and `sampleChats` make. They are
 * written to the product model: files, pages and decks Dua and Agent would
 * actually have produced for this user.
 */
val demoArtifacts: List<ArtifactItem> = listOf(
    ArtifactItem(
        id = "art_q3_deck",
        title = "Q3 Launch Deck",
        type = "Webpage",
        timestamp = "Aaj",
        sizeBytes = 2_411_724L,
        url = "https://static.teamily.ai/sites/demo/q3-launch-deck/index.html",
    ),
    ArtifactItem(
        id = "art_competitor_brief",
        title = "Competitor Brief",
        type = "Document",
        timestamp = "Aaj",
        sizeBytes = 412_160L,
        url = "https://static.teamily.ai/sites/demo/competitor-brief.pdf",
    ),
    ArtifactItem(
        id = "art_brand_hero",
        title = "Brand Hero Banner",
        type = "Image",
        timestamp = "2 din pehle",
        sizeBytes = 1_887_437L,
        url = "https://static.teamily.ai/sites/demo/brand-hero.png",
    ),
    ArtifactItem(
        id = "art_invoice_2211",
        title = "Invoice #2211",
        type = "Document",
        timestamp = "2 din pehle",
        sizeBytes = 96_256L,
        url = "https://static.teamily.ai/sites/demo/invoice-2211.pdf",
    ),
    ArtifactItem(
        id = "art_demo_reel",
        title = "Product Demo Reel",
        type = "Video",
        timestamp = "4 din pehle",
        sizeBytes = 18_350_080L,
        url = "https://static.teamily.ai/sites/demo/product-demo-reel.mp4",
    ),
    ArtifactItem(
        id = "art_landing_page",
        title = "Landing Page v2",
        type = "Webpage",
        timestamp = "1 hafta pehle",
        sizeBytes = 743_424L,
        url = "https://static.teamily.ai/sites/demo/landing-page-v2/index.html",
    ),
    ArtifactItem(
        id = "art_market_chart",
        title = "Market Share Chart",
        type = "Image",
        timestamp = "1 hafta pehle",
        sizeBytes = 204_800L,
        url = "https://static.teamily.ai/sites/demo/market-share-chart.png",
    ),
    // Deliberately has NO url: it is the artifact that makes the context menu's
    // disabled "Open in new tab" state reachable and testable.
    ArtifactItem(
        id = "art_meeting_notes",
        title = "Meeting Notes",
        type = "Document",
        timestamp = "2 hafte pehle",
        sizeBytes = 28_672L,
        url = null,
    ),
)

/**
 * `2411724` -> `"2.3 MB"` — the preview caption's size half.
 *
 * Lives here, beside the data it describes, so the caption cannot drift from the
 * [ArtifactItem.sizeBytes] it claims to show. Audit fix #8: the caption is built
 * from the model, never from a hardcoded string.
 */
internal fun artifactSizeLabel(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(java.util.Locale.US, "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(java.util.Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(java.util.Locale.US, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

/**
 * The in-memory data source — the demo set, and the default.
 *
 * It holds the list in a plain `MutableList`, so `add` / `delete` are real
 * mutations that a subsequent `fetch` reflects. That is what makes the undo
 * path honest: the artifact really is re-added to the source, not just to the
 * screen's copy.
 *
 * @param seed the initial list. Defaults to [demoArtifacts]; a test can pass
 *   `emptyList()` to exercise the empty state.
 * @param failOnLoad when true, [fetch] throws — the hook the error-state test
 *   uses.
 * @param loadDelayMs how long [fetch] takes. Non-zero so the Loading state is
 *   observable; a test can pass 0 to skip the wait.
 */
class InMemoryArtifactsDataSource(
    seed: List<ArtifactItem> = demoArtifacts,
    private val failOnLoad: Boolean = false,
    private val loadDelayMs: Long = 350L,
) : ArtifactsDataSource {

    private val items: MutableList<ArtifactItem> = seed.toMutableList()

    override suspend fun fetch(): List<ArtifactItem> {
        if (loadDelayMs > 0) delay(loadDelayMs)
        if (failOnLoad) throw IllegalStateException("Artifacts load nahi ho paye.")
        return items.toList()
    }

    override suspend fun add(item: ArtifactItem) {
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
 * what is genuinely its own: the endpoint ([ArtifactsConfig], never a literal
 * here) and the JSON shape. `org.json` is already on the platform, so wiring a
 * backend adds no library.
 *
 * The JSON shape it expects is the obvious one:
 * ```json
 * [ { "id": "...", "title": "...", "type": "Image",
 *     "timestamp": "Aaj", "sizeBytes": 1887437 } ]
 * ```
 * A non-2xx response, a network failure or malformed JSON all throw, which the
 * repository surfaces as [ArtifactsUiState.Error] with a Retry action.
 */
class RemoteArtifactsDataSource(
    private val baseUrl: String = ArtifactsConfig.artifactsUrl,
    /**
     * The shared HTTP client. Public read-only so a test can prove both remote
     * sources default to the ONE [HttpClientProvider] instance; still injectable
     * so a test can pass a fake-transport client.
     */
    val client: JsonHttpClient = HttpClientProvider.client,
) : ArtifactsDataSource {

    override suspend fun fetch(): List<ArtifactItem> = withContext(Dispatchers.IO) {
        // requestOrThrow, not request: it throws a typed [ApiError], so the
        // failure the repository puts in [ArtifactsUiState.Error] is the curated
        // userMessage ("Artifacts request: the server is unavailable.") instead
        // of the raw "Artifacts request failed: HTTP 500" string.
        val body = client.requestOrThrow("GET", baseUrl, label = "Artifacts request")
        parseArtifacts(body)
    }

    override suspend fun add(item: ArtifactItem) = withContext(Dispatchers.IO) {
        // Built with JSONObject, NOT string interpolation.
        //
        // WHY: a title like `He said "hi"` used to produce invalid JSON, and a
        // title like `x","id":"evil` could inject extra fields into the body.
        // JSONObject escapes every value, so the payload is always valid and a
        // value can never become structure.
        val payload = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("type", item.type)
            put("timestamp", item.timestamp)
            put("sizeBytes", item.sizeBytes)
            // JSONObject.NULL, not the string "null": a null url must be a JSON
            // null, not the four-character string.
            put("url", item.url ?: JSONObject.NULL)
        }.toString()
        // requestOrThrow so a refused write reaches WriteResult.Failure with a
        // typed ApiError, which BaseRemoteRepository.writeFailureMessage shows
        // verbatim; a plain request() would only carry a raw status string.
        client.requestOrThrow("POST", baseUrl, payload, label = "Artifacts request")
        Unit
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        // The id is a PATH SEGMENT, so it must be percent-encoded.
        //
        // WHY: an id containing `/`, `?`, `#` or a space used to change the URL's
        // structure — `art/1` became two segments, `art?x=1` became a query, and
        // a space produced an invalid request line. Encoding makes the id one
        // opaque segment, which is what the endpoint expects.
        client.requestOrThrow(
            "DELETE",
            "$baseUrl/${encodePathSegment(id)}",
            label = "Artifacts request",
        )
        Unit
    }

    /** Parses the list payload. Throws on malformed JSON. */
    private fun parseArtifacts(body: String): List<ArtifactItem> {
        // The live service wraps the list: `{"artifacts":[...]}`. A bare array
        // is still accepted (see [unwrapList]), and a 200 carrying
        // `{"error":"..."}` becomes an ApiError instead of an empty grid.
        val array = unwrapList(body, key = "artifacts", label = "Artifacts request")
        val out = ArrayList<ArtifactItem>(array.length())
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            out += ArtifactItem(
                id = o.getString("id"),
                title = o.getString("title"),
                type = o.getString("type"),
                timestamp = o.optString("timestamp", ""),
                sizeBytes = o.optLong("sizeBytes", 0L),
                url = o.optString("url", "").takeIf { it.isNotBlank() },
            )
        }
        return out
    }
}

/**
 * The Artifacts data source facade the screen talks to.
 *
 * It owns the [ArtifactsUiState] and the pull-to-refresh flag, and forwards
 * every operation to an [ArtifactsDataSource]. The screen never sees the data
 * source, so swapping [InMemoryArtifactsDataSource] for
 * [RemoteArtifactsDataSource] changes nothing above this class.
 *
 * @param dataSource where the artifacts actually come from. Defaults to the
 *   in-memory demo source, so the kit runs with no backend.
 * @param seed / @param failOnLoad / @param loadDelayMs are kept as convenience
 *   parameters for the previews and tests: when [dataSource] is left at its
 *   default they configure the in-memory source. Passing an explicit
 *   [dataSource] ignores them.
 */
class ArtifactsRepository(
    dataSource: ArtifactsDataSource? = null,
    seed: List<ArtifactItem> = demoArtifacts,
    failOnLoad: Boolean = false,
    loadDelayMs: Long = 350L,
) : BaseRemoteRepository<ArtifactItem, ArtifactsUiState>(
    source = dataSource ?: InMemoryArtifactsDataSource(
        seed = seed,
        failOnLoad = failOnLoad,
        loadDelayMs = loadDelayMs,
    ),
) {

    /** The state shown while the first load is in flight. */
    override fun loadingState(): ArtifactsUiState = ArtifactsUiState.Loading

    /** The state wrapping the fetched list (may be empty — the empty state). */
    override fun loadedState(items: List<ArtifactItem>): ArtifactsUiState =
        ArtifactsUiState.Loaded(items)

    /** The state wrapping a failure message, shown with a Retry action. */
    override fun errorState(message: String): ArtifactsUiState =
        ArtifactsUiState.Error(message)

    /** The loaded list inside [state], or null while loading / in error. */
    override fun itemsOf(state: ArtifactsUiState): List<ArtifactItem>? =
        (state as? ArtifactsUiState.Loaded)?.artifacts

    /** The message used when an exception carries none of its own. */
    override val errorFallback: String = "Artifacts load nahi ho paye."

    /** Shown when a new artifact could not be saved. */
    override val addFailureFallback: String = "Artifact save nahi ho paya."

    /** Shown when an artifact could not be deleted. */
    override val deleteFailureFallback: String = "Artifact delete nahi ho paya."
}
