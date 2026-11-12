package ai.duagenai.design.screens

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
 *  [ArtifactsConfig] and [MemoryConfig] now both read [ApiConfig.BASE_URL]. Each
 *  keeps its own PATH (the two resources live at different endpoints), but the
 *  host is defined exactly once — change it here and both features follow.
 *
 *  The value is a clearly-named PLACEHOLDER: there is no live service behind it
 *  yet, which is why both repositories default to their in-memory data source.
 *  Replace this one constant and construct the repositories with their Remote
 *  data sources to go live.
 * ==========================================================================*/

/**
 * The one place the backend's address is defined.
 *
 * A screen must never hardcode a URL, and neither should a repository: the host
 * lives here so a developer pointing the app at a real service changes exactly
 * one constant.
 */
object ApiConfig {
    /**
     * The API host, shared by every feature.
     *
     * Placeholder on purpose: replace this with the real service's base URL and
     * construct the repositories with their Remote data sources to go live.
     */
    const val BASE_URL: String = "https://api.duagenai.example/v1"
}
