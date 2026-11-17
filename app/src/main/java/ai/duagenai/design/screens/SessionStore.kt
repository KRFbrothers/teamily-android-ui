package ai.duagenai.design.screens

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/* ============================================================================
 *  The session — where the bearer token actually comes from
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  [TokenProvider] was a seam with nothing plugged into it. [TokenStore] held a
 *  plain `@Volatile var`, and NOTHING in `app/src/main` ever wrote to it: the
 *  only writers were tests. So `TokenStore.token` was always null, the nav host
 *  always chose the in-memory demo source, and [RemoteArtifactsDataSource] /
 *  [RemoteMemoryDataSource] were dead code at runtime. The app could not reach
 *  the live backend at all.
 *
 *  Two separate defects were hiding behind that one symptom:
 *
 *    1. NO WRITER. There was no sign-in path, so no token ever arrived.
 *    2. NO OBSERVER. `TokenStore.token` was a plain var, so even if something
 *       had written it, `remember(sessionToken)` in the nav host would not have
 *       recomposed — a plain var is not Compose state. Signing in would have
 *       left the app on the demo data until the next process start.
 *
 *  This file fixes both:
 *
 *    • [TokenStore.session] is a [StateFlow], so the nav host collects it with
 *      `collectAsStateWithLifecycle()` and a sign-in recomposes the graph.
 *    • [SessionBridge] is the seam a real auth provider plugs into. A host app
 *      that owns a Supabase (or any) session implements it once and calls
 *      [TokenStore.install]; the store mirrors the bridge's flow into its own.
 *
 *  WHAT IS DELIBERATELY NOT HERE
 *  -----------------------------
 *  No Supabase SDK, no OAuth flow, no token refresh. This module has no auth
 *  dependency and adding one is a product decision, not a review fix. What it
 *  does provide is the ONE thing that was missing: a real, observable place for
 *  a token to arrive, and a documented way to plug a provider in.
 *
 *  PERSISTENCE (sign-in survives an app restart)
 *  ---------------------------------------------
 *  [TokenStore.persistTo] mirrors the token into a small Preferences DataStore
 *  ([SessionPersistence]) and restores it once at startup. MainActivity calls
 *  it; tests that build the nav host directly never do, so they stay
 *  in-memory. The file lives in `noBackupFilesDir`, which Android never
 *  includes in cloud backup or device-to-device transfer, so the token does
 *  not leave the device. It is app-private but NOT encrypted, and there is no
 *  expiry policy on the client — the server's 401 is the expiry signal.
 * ==========================================================================*/

/**
 * Where a real session comes from.
 *
 * A host app that owns an auth session (Supabase, Firebase, a custom backend)
 * implements this once and hands it to [TokenStore.install]. The store then
 * mirrors [token] into its own [TokenStore.session], so every screen and every
 * request sees the same value.
 *
 * [token] is a [StateFlow] rather than a plain getter on purpose: a session can
 * change while the app is running (sign-in, sign-out, a refreshed token), and
 * the UI has to react to that. A getter would only ever be read at the moment
 * something asked.
 */
interface SessionBridge {
    /** The current bearer token, or null when signed out. */
    val token: StateFlow<String?>

    companion object {
        /**
         * The default: no auth provider is installed.
         *
         * The app then runs anonymously, which is the honest state for a build
         * with no auth dependency — the live service answers 401 and the UI says
         * so, instead of pretending to be signed in.
         */
        val None: SessionBridge = object : SessionBridge {
            override val token: StateFlow<String?> =
                MutableStateFlow<String?>(null).asStateFlow()
        }
    }
}

/**
 * The app-side holder for the current session token.
 *
 * [HttpClientProvider] reads [provider] on every request, so a sign-in only has
 * to write the token once and both features start sending it — no client
 * rebuild. The nav host collects [session], so the same write also flips the
 * repositories from the demo source to the remote one.
 *
 * Blank and whitespace-only values are normalised to null, so a half-filled
 * field can never produce an `Authorization: Bearer ` header.
 */
object TokenStore {

    private val _session = MutableStateFlow<String?>(null)

    /**
     * The current token as observable state.
     *
     * This is what the nav host collects. It is a [StateFlow] — not a plain var
     * — because Compose only recomposes on state it can observe, and the old
     * `@Volatile var` was invisible to it.
     */
    val session: StateFlow<String?> = _session.asStateFlow()

    /**
     * The same flow as [session].
     *
     * Account and chat screens collect this name. It is not a second store:
     * both properties are the one flow the setter writes, so a sign-in updates
     * every collector at once.
     */
    val state: StateFlow<String?> = session

    /**
     * The current bearer token, or null when signed out.
     *
     * Kept as a property so existing call sites and tests read the same value
     * they always did; the backing store is now [session].
     */
    var token: String?
        get() = _session.value
        // The Kotlin setter would otherwise be `setToken` on the JVM, which
        // is the same signature as [setToken] below. Call sites keep using
        // `TokenStore.token =`; only the bytecode name changes.
        @JvmName("assignToken")
        set(value) {
            _session.value = normalize(value)
        }

    /** True when a token is present. */
    val isSignedIn: Boolean get() = _session.value != null

    /** The provider [HttpClientProvider] hands to the shared client. */
    val provider: TokenProvider = TokenProvider { _session.value }

    /**
     * Sets the session token, or clears it with null.
     *
     * The one write path. A sign-in screen, a debug entry field and a host app's
     * bridge all end up here, so there is exactly one place the token can enter
     * the app.
     */
    fun setToken(value: String?) {
        token = value
    }

    /** Signs out: clears the token, so every request goes back to anonymous. */
    fun clear() {
        _session.value = null
    }

    /**
     * Mirrors [bridge]'s token into this store for as long as [scope] lives.
     *
     * A host app calls this once, at startup, with its own session provider:
     *
     * ```kotlin
     * TokenStore.install(SupabaseSessionBridge(client), lifecycleScope)
     * ```
     *
     * From then on a sign-in, a sign-out or a refreshed token in the host app
     * reaches the UI and the HTTP client without any further wiring.
     */
    fun install(bridge: SessionBridge, scope: CoroutineScope) {
        scope.launch {
            bridge.token.collect { setToken(it) }
        }
    }

    /** The one persistence job for this process (null until [persistTo]). */
    private var persistJob: Job? = null

    /**
     * Restores the saved token, then writes every change back, for the life of
     * the process. Sign-in (a non-null token) is saved; Sign out (null) removes
     * it. Safe to call more than once — while a mirror is running, further
     * calls do nothing (a cancelled [scope] lets a later call start a new one).
     *
     * A token that arrived before the saved one finished loading wins: the
     * saved value only fills an empty session.
     */
    @Synchronized
    fun persistTo(
        context: Context,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ) {
        if (persistJob?.isActive == true) return
        val persistence = SessionPersistence.get(context)
        persistJob = scope.launch {
            val saved = persistence.read()
            if (_session.value == null && saved != null) setToken(saved)
            _session.collect { persistence.write(it) }
        }
    }

    /** Trims a token and turns blank into null. */
    private fun normalize(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }
}

/**
 * The on-disk copy of the session token: one key in a Preferences DataStore.
 *
 * The file is created in `noBackupFilesDir` (not the default `files/datastore`)
 * so it is excluded from Auto Backup and device transfer without any backup
 * rules XML. One instance per process — DataStore must never open the same
 * file twice — so it is reached through [get].
 */
internal class SessionPersistence private constructor(
    /** Where the token lives: `noBackupFilesDir/dua_session.preferences_pb`. */
    val file: File,
) {
    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { file })
    private val tokenKey = stringPreferencesKey("session_token")

    /** The saved token, or null. */
    suspend fun read(): String? = store.data.first()[tokenKey]

    /** Saves [token]; null (signed out) removes it. */
    suspend fun write(token: String?) {
        store.edit { prefs ->
            if (token == null) prefs.remove(tokenKey) else prefs[tokenKey] = token
        }
    }

    companion object {
        /** File name inside `noBackupFilesDir`. */
        const val FILE_NAME = "dua_session.preferences_pb"

        @Volatile private var instance: SessionPersistence? = null

        fun get(context: Context): SessionPersistence =
            instance ?: synchronized(this) {
                instance ?: SessionPersistence(
                    File(context.applicationContext.noBackupFilesDir, FILE_NAME),
                ).also { instance = it }
            }
    }
}
