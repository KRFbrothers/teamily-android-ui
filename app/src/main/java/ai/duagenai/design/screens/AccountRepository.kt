package ai.duagenai.design.screens

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/* ============================================================================
 *  Account sign-in and sign-out.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  Artifacts, Memory and chat all read [TokenStore]. Nothing in the kit wrote
 *  it, so the live backend was unreachable from the UI. This is the writer:
 *  email sign-in stores `session.accessToken`, and sign-out clears it.
 *
 *  The HTTP mechanics stay in [JsonHttpClient]. This class only knows the two
 *  account endpoints and the session JSON shape from dua-backend's contract.
 * ==========================================================================*/

/**
 * The account endpoints: email sign-in and sign-out.
 *
 * @param client the shared HTTP client. A test injects one with a fake
 *   transport; the default sends the session token [TokenStore] holds.
 */
class RemoteAccountDataSource(
    private val client: JsonHttpClient = HttpClientProvider.client,
) {

    /**
     * Signs in with email and password and stores the session access token.
     *
     * @return the access token that was stored. Blank and whitespace-only tokens
     *   are refused: [TokenStore] would normalise them to null, which is not a
     *   successful sign-in.
     */
    suspend fun emailLogin(email: String, password: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .toString()
        val body = client.requestOrThrow(
            method = "POST",
            url = ApiConfig.url("/api/account/email-login"),
            body = payload,
            label = "Sign in",
        )
        val accessToken = try {
            JSONObject(body)
                .optJSONObject("session")
                ?.optString("accessToken")
                ?.trim()
                .orEmpty()
        } catch (e: org.json.JSONException) {
            throw ApiError.forMalformedResponse("Sign in")
        }
        if (accessToken.isEmpty()) {
            throw ApiError.forMalformedResponse("Sign in")
        }
        TokenStore.token = accessToken
        TokenStore.token ?: accessToken
    }

    /**
     * Signs out.
     *
     * The server call is best-effort: a network failure still clears the local
     * session, because leaving a token in memory after the user asked to sign
     * out would keep sending it. Cancellation is not a network failure and is
     * rethrown after the local session is cleared, so a leaving screen still
     * ends the session and structured concurrency still works.
     */
    suspend fun logout() {
        var cancelled: CancellationException? = null
        try {
            withContext(Dispatchers.IO) {
                client.requestOrThrow(
                    method = "POST",
                    url = ApiConfig.url("/api/account/logout"),
                    body = "{}",
                    label = "Sign out",
                )
            }
        } catch (e: CancellationException) {
            cancelled = e
        } catch (_: Exception) {
            // The local session is cleared below either way.
        } finally {
            TokenStore.token = null
        }
        val toRethrow = cancelled
        if (toRethrow != null) throw toRethrow
    }
}
