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
 *  keeps its own PATH, but the host is defined exactly once.
 *
 *  This kit talks to the standalone Dua backend (KRFbrothers/dua-backend):
 *    • [API_BASE_URL]  — Node API. Personal chat, files, cloud memory, account.
 *    • [ENGINE_BASE_URL] — Python engine. Dua Agent chat (`mode: "agent"`).
 *  Android sends no Origin header. Set [accessToken] to the Supabase access
 *  token before [DuaGenAINavHost] is shown. Leave it blank and the kit stays on
 *  its in-memory demo data, which is what the previews and UI tests use.
 *
 *  Local defaults match the backend README: API on port 3000, engine on 8080.
 *  An emulator reaches the computer at 10.0.2.2 instead of 127.0.0.1. Cleartext
 *  HTTP needs `android:usesCleartextTraffic="true"` on a debug manifest.
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
     * Node API origin. Personal Agent chat is `POST /api/chat` here.
     * Files are `GET /api/files/list`. Memory notes are inside `GET /api/cloud`.
     */
    const val API_BASE_URL: String = "http://127.0.0.1:3000"

    /**
     * Python engine origin. Dua Agent chat is `POST /api/chat` here with
     * `"mode": "agent"` and `"stream": true`. Skill ids only; the server
     * attaches the skill text.
     */
    const val ENGINE_BASE_URL: String = "http://127.0.0.1:8080"

    /**
     * The API host shared by Artifacts and Memory.
     *
     * Kept as the name those configs already read, so a feature does not grow
     * its own copy of the host.
     */
    const val BASE_URL: String = API_BASE_URL

    /**
     * Supabase access token from `POST /api/account/email-login` (or Google
     * token exchange). Empty means the kit does not call the network.
     */
    var accessToken: String = ""

    /**
     * Optional `X-Dua-App-Token`. Leave empty unless the engine has
     * `DUA_APP_TOKEN` set. Never commit a real token into this file.
     */
    var appToken: String = ""
}
