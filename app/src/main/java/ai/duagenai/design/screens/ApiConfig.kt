package ai.duagenai.design.screens

import ai.duagenai.design.BuildConfig

/* ============================================================================
 *  The backend's address — ONE definition for the whole app.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  Artifacts and Memory each grew their own `BASE_URL` constant. They happened to
 *  hold the same string, but nothing enforced that: pointing the app at a real
 *  service meant editing two files, and forgetting one would leave half the app
 *  talking to the placeholder. A base URL is a property of the BACKEND, not of a
 *  feature, so it belongs in one place.
 *
 *  [ArtifactsConfig] and [MemoryConfig] both read [ApiConfig.BASE_URL]. Each
 *  keeps its own PATH (the two resources live at different endpoints), but the
 *  host is defined exactly once.
 *
 *  WHERE THE VALUE COMES FROM (build-type driven, not a literal)
 *  ------------------------------------------------------------
 *  The host is NOT written into this file. It arrives as a generated
 *  `BuildConfig.API_BASE_URL` field, which `app/build.gradle.kts` fills per
 *  build type from, in order of precedence:
 *
 *    1. the Gradle property `duaApiBaseUrlDebug` / `duaApiBaseUrlRelease`
 *    2. the Gradle property `duaApiBaseUrl`
 *    3. the environment variable `DUA_API_BASE_URL`
 *    4. the default in the build file (the live Cloud Run service)
 *
 *  So a debug build, a release build and a CI run can each point somewhere
 *  different without touching a single Kotlin file:
 *
 *      ./gradlew assembleDebug -PduaApiBaseUrl=http://10.0.2.2:8080
 *      DUA_API_BASE_URL=https://staging.example ./gradlew assembleRelease
 *
 *  [overrideBaseUrl] is the runtime seam on top of that: a test (or a debug
 *  settings screen) can repoint the app in-process without a rebuild.
 * ==========================================================================*/

/**
 * The one place the backend's address is resolved.
 *
 * A screen must never hardcode a URL, and neither should a repository: the host
 * is resolved here so every feature follows one rule.
 */
object ApiConfig {

    /*
     * THERE IS NO FALLBACK LITERAL HERE ANY MORE.
     *
     * This object used to carry its own copy of the live Cloud Run URL as
     * `FALLBACK_BASE_URL`, duplicating the default in `app/build.gradle.kts`. Two
     * literals for one host is a drift bug waiting to happen: change the build
     * file and the Kotlin fallback silently keeps pointing at the old service.
     *
     * The host now has exactly ONE definition — the `defaultApiBaseUrl` in
     * `app/build.gradle.kts` — which is baked into `BuildConfig.API_BASE_URL` per
     * build type. This object only normalises it and applies a runtime override.
     */

    /**
     * A runtime override, or null to use the build-time value.
     *
     * `@Volatile` because a debug screen (or a test) may set it from another
     * thread while requests are being built. Setting it to null restores the
     * build-time value.
     */
    @Volatile
    var overrideBaseUrl: String? = null

    /**
     * The API host, shared by every feature.
     *
     * Resolved per access rather than frozen in a `const`, so [overrideBaseUrl]
     * takes effect immediately. Always normalised: trimmed, and with no trailing
     * slash, so `BASE_URL + "/api/artifacts"` can never produce a double slash.
     *
     * A blank `BuildConfig.API_BASE_URL` is a BUILD misconfiguration, not a
     * runtime condition to paper over: the build file always supplies a value, so
     * an empty one means the generated field was not wired. It is surfaced as an
     * empty string rather than silently substituted with a hardcoded host, so the
     * failure is visible instead of masked.
     */
    val BASE_URL: String
        get() = normalize(overrideBaseUrl ?: BuildConfig.API_BASE_URL)

    /**
     * Builds a full endpoint URL from a path.
     *
     * @param path the endpoint path, with or without a leading slash.
     */
    fun url(path: String): String =
        BASE_URL + if (path.startsWith("/")) path else "/$path"

    /**
     * The last-resort engine host, used only when the generated field is blank.
     *
     * Dua Agent chat (`mode: "agent"`) goes here. Personal chat stays on
     * [BASE_URL]. It matches the build file's default so an empty
     * `ENGINE_BASE_URL` still reaches the real engine.
     */
    const val FALLBACK_ENGINE_BASE_URL: String =
        "https://dua-ai-engine-228305912525.us-central1.run.app"

    /**
     * A runtime override for the engine host, or null to use the build-time value.
     *
     * `@Volatile` for the same reason as [overrideBaseUrl]: a test may set it
     * from another thread while a request URL is being built.
     */
    @Volatile
    var overrideEngineBaseUrl: String? = null

    /**
     * The engine host, used by Dua Agent chat.
     *
     * Resolved per access, like [BASE_URL], and normalised the same way so
     * `engineUrl("/api/chat")` cannot produce a double slash.
     */
    val ENGINE_BASE_URL: String
        get() = normalize(
            overrideEngineBaseUrl ?: BuildConfig.ENGINE_BASE_URL.ifBlank { FALLBACK_ENGINE_BASE_URL },
        )

    /**
     * Builds a full engine URL from a path.
     *
     * @param path the endpoint path, with or without a leading slash.
     */
    fun engineUrl(path: String): String =
        ENGINE_BASE_URL + if (path.startsWith("/")) path else "/$path"

    /**
     * Trims a host and drops trailing slashes.
     *
     * WHY: a base URL pasted from a console routinely ends in `/`, and
     * `"https://host/" + "/api/artifacts"` is `https://host//api/artifacts` —
     * a different path, which some servers 404.
     */
    internal fun normalize(raw: String): String = raw.trim().trimEnd('/')
}
