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
    val sizeBytes: Long,
    val url: String? = null,
) : Identifiable

data class ArtifactVersionItem(
    override val id: String,
    val artifactId: String,
    val versionNum: Int,
    val content: String,
    val promptNote: String? = null,
    val createdAt: String = "",
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
    ) : ArtifactsUiState
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

    /**
     * Persists a new artifact and returns what the SERVER stored.
     *
     * Returning the stored item (not Unit) is the fix for a real data-loss bug:
     * the POST response was discarded, so the server-assigned `id` and
     * `timestamp` were thrown away and the screen kept showing the client's
     * guess. A later DELETE then used an id the server had never issued.
     */
    override suspend fun add(item: ArtifactItem): ArtifactItem

    /** Removes an artifact by id. Throws on failure. */
    override suspend fun delete(id: String)

    suspend fun fetchVersions(artifactId: String): List<ArtifactVersionItem> = emptyList()

    suspend fun reviseArtifact(artifactId: String, instruction: String): Pair<ArtifactItem, ArtifactVersionItem>? = null
}

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
 * @param seed the initial list. Defaults to EMPTY: a production build must never
 *   ship fake rows. [demoArtifacts] is a preview/test fixture and is passed
 *   explicitly by the `@Preview` functions and the tests that need data.
 * @param failOnLoad when true, [fetch] throws — the hook the error-state test
 *   uses.
 * @param loadDelayMs how long [fetch] takes. Non-zero so the Loading state is
 *   observable; a test can pass 0 to skip the wait.
 */
class InMemoryArtifactsDataSource(
    seed: List<ArtifactItem> = emptyList(),
    private val failOnLoad: Boolean = false,
    private val loadDelayMs: Long = 350L,
) : ArtifactsDataSource {

    private val items: MutableList<ArtifactItem> = seed.toMutableList()
    private val versions = mutableMapOf<String, MutableList<ArtifactVersionItem>>()

    override suspend fun fetch(): List<ArtifactItem> {
        if (loadDelayMs > 0) delay(loadDelayMs)
        if (failOnLoad) throw IllegalStateException("Artifacts load nahi ho paye.")
        return items.toList()
    }

    override suspend fun add(item: ArtifactItem): ArtifactItem {
        items.add(0, item)
        return item
    }

    override suspend fun delete(id: String) {
        items.removeAll { it.id == id }
        versions.remove(id)
    }

    override suspend fun fetchVersions(artifactId: String): List<ArtifactVersionItem> =
        versions[artifactId]?.toList().orEmpty()

    override suspend fun reviseArtifact(artifactId: String, instruction: String): Pair<ArtifactItem, ArtifactVersionItem>? {
        val artifact = items.firstOrNull { it.id == artifactId } ?: return null
        val list = versions.getOrPut(artifactId) { mutableListOf() }
        val nextVersionNum = list.size + 1
        val version = ArtifactVersionItem(
            id = java.util.UUID.randomUUID().toString(),
            artifactId = artifactId,
            versionNum = nextVersionNum,
            content = "Revised: $instruction",
            promptNote = instruction,
            createdAt = nowLabel(),
        )
        list.add(0, version)
        return artifact to version
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

    override suspend fun add(item: ArtifactItem): ArtifactItem = withContext(Dispatchers.IO) {
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
        val response = client.requestOrThrow("POST", baseUrl, payload, label = "Artifacts request")
        // The response is the SERVER's copy of the row, and it is what the list
        // must show: the server may have assigned a different id, normalised the
        // timestamp, or filled in a url. Discarding it (the old behaviour) meant
        // the screen kept a client-side guess and a later DELETE used an id the
        // server had never issued.
        parseArtifactOrFallback(response, item)
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

    override suspend fun fetchVersions(artifactId: String): List<ArtifactVersionItem> = withContext(Dispatchers.IO) {
        val body = client.requestOrThrow(
            "GET",
            "$baseUrl/${encodePathSegment(artifactId)}/versions",
            label = "Artifact versions request",
        )
        parseArtifactVersions(body, artifactId)
    }

    override suspend fun reviseArtifact(
        artifactId: String,
        instruction: String,
    ): Pair<ArtifactItem, ArtifactVersionItem>? = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("instruction", instruction)
        }.toString()
        val response = client.requestOrThrow(
            "POST",
            "$baseUrl/${encodePathSegment(artifactId)}/revise",
            payload,
            label = "Artifact revision request",
        )
        parseArtifactRevision(response, artifactId)
    }

    /** Parses the list payload. Throws on malformed JSON. */
    private fun parseArtifacts(body: String): List<ArtifactItem> {
        // The live service wraps the list: `{"artifacts":[...]}`. A bare array
        // is still accepted (see [unwrapList]), and a 200 carrying
        // `{"error":"..."}` becomes an ApiError instead of an empty grid.
        val array = unwrapList(body, key = "artifacts", label = "Artifacts request")
        val out = ArrayList<ArtifactItem>(array.length())
        for (i in 0 until array.length()) {
            // PER-ITEM ISOLATION.
            //
            // This used to be `array.getJSONObject(i)` + `o.getString("id")`
            // with no guard, so ONE malformed row (a missing `type`, a null
            // `title`, a non-object element) threw a JSONException that
            // propagated out of the whole parse and replaced the ENTIRE list
            // with an error screen. A single bad row must cost one row, not the
            // page.
            //
            // A row is skipped when it has no usable id (it cannot be addressed
            // for delete) or is not an object at all. Everything else is
            // defaulted, so a partial row still renders.
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id", "").takeIf { it.isNotBlank() } ?: continue
            out += ArtifactItem(
                id = id,
                title = o.optString("title", "").ifBlank { "Untitled artifact" },
                type = o.optString("type", "").ifBlank { "File" },
                timestamp = o.optString("timestamp", ""),
                sizeBytes = o.optLong("sizeBytes", 0L),
                url = o.optString("url", "").takeIf { it.isNotBlank() },
            )
        }
        return out
    }

    /**
     * Parses the POST response into the stored item, falling back to [sent].
     *
     * The server may answer with the created object directly, or wrapped in an
     * envelope (`{"artifact":{...}}`). Both are accepted. If the body is not a
     * usable object the item the client sent is returned unchanged — a write
     * that SUCCEEDED must not be reported as a failure just because the response
     * shape was unexpected.
     */
    private fun parseArtifactOrFallback(body: String, sent: ArtifactItem): ArtifactItem {
        val text = body.trim()
        if (!text.startsWith("{")) return sent
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return sent
        val o = root.optJSONObject("artifact") ?: root
        val id = o.optString("id", "").takeIf { it.isNotBlank() } ?: return sent
        return ArtifactItem(
            id = id,
            title = o.optString("title", "").ifBlank { sent.title },
            type = o.optString("type", "").ifBlank { sent.type },
            timestamp = o.optString("timestamp", "").ifBlank { sent.timestamp },
            sizeBytes = o.optLong("sizeBytes", sent.sizeBytes),
            url = o.optString("url", "").takeIf { it.isNotBlank() } ?: sent.url,
        )
    }

    private fun parseArtifactVersions(body: String, fallbackArtifactId: String): List<ArtifactVersionItem> {
        val array = unwrapList(body, key = "versions", label = "Artifact versions request")
        val out = ArrayList<ArtifactVersionItem>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id", "").takeIf { it.isNotBlank() } ?: continue
            out += ArtifactVersionItem(
                id = id,
                artifactId = o.optString("artifactId", o.optString("artifact_id", fallbackArtifactId)),
                versionNum = o.optInt("versionNum", o.optInt("version_num", 1)),
                content = o.optString("content", ""),
                promptNote = o.optString("promptNote", o.optString("prompt_note", "")).takeIf { it.isNotBlank() },
                createdAt = o.optString("createdAt", o.optString("created_at", "")),
            )
        }
        return out
    }

    private fun parseArtifactRevision(body: String, fallbackArtifactId: String): Pair<ArtifactItem, ArtifactVersionItem>? {
        val text = body.trim()
        if (!text.startsWith("{")) return null
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val artObj = root.optJSONObject("artifact")
        val verObj = root.optJSONObject("version") ?: return null
        val artifact = if (artObj != null) {
            val id = artObj.optString("id", fallbackArtifactId)
            ArtifactItem(
                id = id,
                title = artObj.optString("title", "Untitled artifact"),
                type = artObj.optString("type", "File"),
                timestamp = artObj.optString("timestamp", ""),
                sizeBytes = artObj.optLong("sizeBytes", 0L),
                url = artObj.optString("url", "").takeIf { it.isNotBlank() },
            )
        } else {
            ArtifactItem(id = fallbackArtifactId, title = "Artifact", type = "File", timestamp = "", sizeBytes = 0L)
        }
        val verId = verObj.optString("id", "").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
        val version = ArtifactVersionItem(
            id = verId,
            artifactId = verObj.optString("artifactId", verObj.optString("artifact_id", fallbackArtifactId)),
            versionNum = verObj.optInt("versionNum", verObj.optInt("version_num", 1)),
            content = verObj.optString("content", ""),
            promptNote = verObj.optString("promptNote", verObj.optString("prompt_note", "")).takeIf { it.isNotBlank() },
            createdAt = verObj.optString("createdAt", verObj.optString("created_at", "")),
        )
        return artifact to version
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
 *   in-memory source, which starts EMPTY — a production build must never ship
 *   fake rows.
 * @param seed / @param failOnLoad / @param loadDelayMs are kept as convenience
 *   parameters for the previews and tests: when [dataSource] is left at its
 *   default they configure the in-memory source. Passing an explicit
 *   [dataSource] ignores them.
 */
class ArtifactsRepository(
    dataSource: ArtifactsDataSource? = null,
    seed: List<ArtifactItem> = emptyList(),
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
    override fun errorState(message: String, retryable: Boolean): ArtifactsUiState =
        ArtifactsUiState.Error(message, retryable)

    /** The loaded list inside [state], or null while loading / in error. */
    override fun itemsOf(state: ArtifactsUiState): List<ArtifactItem>? =
        (state as? ArtifactsUiState.Loaded)?.artifacts

    /** A copy of [item] carrying [id] — the undo path's new identity. */
    override fun withId(item: ArtifactItem, id: String): ArtifactItem = item.copy(id = id)

    /** The message used when an exception carries none of its own. */
    override val errorFallback: String = "Artifacts load nahi ho paye."

    /** Shown when a new artifact could not be saved. */
    override val addFailureFallback: String = "Artifact save nahi ho paya."

    /** Shown when an artifact could not be deleted. */
    override val deleteFailureFallback: String = "Artifact delete nahi ho paya."

    /** Fetches version history for an artifact. */
    suspend fun fetchVersions(artifactId: String): List<ArtifactVersionItem> =
        (source as? ArtifactsDataSource)?.fetchVersions(artifactId) ?: emptyList()

    /** Submits a revision prompt instruction on an artifact. */
    suspend fun reviseArtifact(artifactId: String, instruction: String): Pair<ArtifactItem, ArtifactVersionItem>? {
        val result = (source as? ArtifactsDataSource)?.reviseArtifact(artifactId, instruction) ?: return null
        load()
        return result
    }
}
