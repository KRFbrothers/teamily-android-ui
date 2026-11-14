package ai.duagenai.design.screens

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    /** Persists a new item. Throws on failure. */
    suspend fun add(item: T)

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
    private val source: RemoteDataSource<T>,
) {

    // ---- the three state constructors plus the list accessor: the ONLY
    // ---- things a concrete repository must supply.

    /** The state shown while the first load is in flight. */
    protected abstract fun loadingState(): S

    /** The state wrapping a finished list (may be empty — that is the empty state). */
    protected abstract fun loadedState(items: List<T>): S

    /** The state wrapping a failure message, shown with a Retry action. */
    protected abstract fun errorState(message: String): S

    /** The Loaded list inside [state], or null when the state is Loading / Error. */
    protected abstract fun itemsOf(state: S): List<T>?

    /** The message used when an exception carries none of its own. */
    protected abstract val errorFallback: String

    // ---- shared state. `loadingState()` returns a bare object singleton in
    // ---- both concrete repositories, so calling it here — before the subclass
    // ---- has finished constructing — is safe. A subclass must never read its
    // ---- own fields from the state constructors.

    private val _state = MutableStateFlow(loadingState())

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
            errorState(e.message ?: errorFallback)
        }
    }

    /**
     * Re-runs [load] for a pull-to-refresh.
     *
     * Deliberately does NOT set [loadingState]: the whole point of pull-to-refresh
     * is that the existing content stays visible under the indicator.
     * [isRefreshing] carries the in-flight signal instead, and it is cleared in
     * a `finally` so a failed refresh cannot leave the spinner stuck.
     */
    suspend fun refresh() {
        _isRefreshing.value = true
        try {
            load(showLoading = false)
        } finally {
            _isRefreshing.value = false
        }
    }

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
            source.add(item)
            _state.value = loadedState(listOf(item) + items)
            _lastWriteError.value = null
            WriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = e.message ?: errorFallback
            _lastWriteError.value = message
            WriteResult.Failure(message)
        }
    }

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
            val message = e.message ?: errorFallback
            _lastWriteError.value = message
            WriteResult.Failure(message)
        }
    }
}
