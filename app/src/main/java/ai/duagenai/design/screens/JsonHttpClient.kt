package ai.duagenai.design.screens

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

/* ============================================================================
 *  The shared HTTP + JSON transport
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  Artifacts and Memory each grew their own copy of the same request code: open
 *  an [HttpURLConnection], set the method, timeouts and headers, write the body,
 *  check the status, read the response. Two copies of one rule drift — a fix to
 *  the timeout in one place silently leaves the other behind, and the two
 *  endpoints end up behaving differently for no reason.
 *
 *  So the mechanics live here, once. [RemoteArtifactsDataSource] and
 *  [RemoteMemoryDataSource] keep only what is genuinely theirs — their endpoint
 *  and their JSON shape — and both call [JsonHttpClient.request].
 *
 *  TESTABILITY
 *  -----------
 *  The network call sits behind [HttpTransport], a one-method seam. The default
 *  is [UrlConnectionTransport] (the real thing, dependency-free); a test passes
 *  a fake transport and exercises a remote source end to end without a socket.
 *  No new library: [HttpURLConnection] and `org.json` are both on the platform.
 *
 *  WHAT WAS FIXED HERE (review blockers)
 *  -------------------------------------
 *   1. CANCELLATION WAS SWALLOWED. A `CancellationException` thrown while the
 *      request was in flight was caught by the generic failure path and turned
 *      into "the request failed". A cancelled coroutine is NOT a failed request:
 *      swallowing it breaks the caller's structured concurrency (a screen that
 *      navigated away keeps its spinner, and `Job.cancel()` stops working).
 *      [request] now rethrows it untouched.
 *   2. NO AUTH SEAM. Every request was anonymous, so pointing the app at a real
 *      service meant editing the transport. [TokenProvider] is that seam: the
 *      client asks for a token per request, and a test (or a real app) supplies
 *      one without touching the mechanics.
 *   3. ERRORS WERE STRINGS. A caller could not tell "retry will help" from
 *      "this will never work", so the UI could not offer the right action.
 *      [ApiError] carries the status, whether it is retryable, and a message
 *      that is safe to show.
 *
 *  WHERE request() ENDS AND requestOrThrow() BEGINS
 *  ------------------------------------------------
 *  Both entry points still work, and [request] is kept because it is the
 *  simplest thing to call and some callers genuinely only want the body.
 *
 *  The difference is the FAILURE TYPE, and it decides what the user ends up
 *  reading. [request] throws an [IllegalStateException] whose message is
 *  `"<label> failed: HTTP 500"`; the repository turns `e.message` into its Error
 *  state, so that raw string is what is rendered on screen. [requestOrThrow]
 *  throws an [ApiError] whose [ApiError.userMessage] is a curated sentence
 *  ("Artifacts request: the server is unavailable."), and — just as important —
 *  carries [ApiError.retryable], which is what lets the UI decide whether
 *  offering a Retry button is honest.
 *
 *   4. THE TOKEN WAS NEVER SENT. [TokenProvider] existed and [JsonHttpClient]
 *      could read it, but [HttpTransport.execute] had no token parameter, so
 *      [UrlConnectionTransport] never set an `Authorization` header: every
 *      request went out anonymous no matter what the provider returned. The
 *      seam is now wired end to end — the client asks for a token per request,
 *      hands it to the transport, and the transport sets `Authorization: Bearer`.
 *      [TokenStore] is the app-side holder a sign-in flow writes to.
 *
 *  Both remote data sources therefore call [requestOrThrow]; [request] remains
 *  for callers that only want the body.
 * ==========================================================================*/

/**
 * Thrown when the server answers with a non-2xx status.
 *
 * A distinct type so [JsonHttpClient] can turn it into the caller's labelled
 * message while letting a genuine network failure (an `IOException`) pass
 * through untouched — the two are different problems and should not be
 * flattened into one string.
 */
class HttpStatusException(
    val code: Int,
    /**
     * The response body the server sent with the failure, or null.
     *
     * WHY: the backend answers a failure with `{"error":"..."}` — a sentence
     * written for the user ("Sign in from Profile to use Artifacts."). Without
     * the body the app could only say "sign-in required", throwing away the one
     * message the server actually wanted shown. Defaulted to null so a caller
     * (and a test) can still throw a bare status.
     */
    val body: String? = null,
) : Exception("HTTP $code")

/**
 * A typed API failure.
 *
 * @param status the HTTP status, or null when the request never got a response
 *   (a DNS failure, a timeout, no connectivity).
 * @param retryable whether trying again could plausibly succeed. A 5xx or a
 *   network failure is retryable; a 4xx is not — retrying a 401 or a 422 just
 *   burns the user's battery.
 * @param userMessage a short, safe sentence for the UI. It never contains a
 *   stack trace, a URL or a token.
 */
data class ApiError(
    val status: Int?,
    val retryable: Boolean,
    val userMessage: String,
) : Exception(userMessage) {

    companion object {
        /**
         * Builds the error for a status code, with the feature's label.
         *
         * @param serverMessage the `error` field of the server's `{"error":"..."}`
         *   body, when it sent one. It is preferred over the curated sentence
         *   because it is the message the backend wrote for this exact failure
         *   ("Sign in from Profile to use Artifacts."); the curated sentence is
         *   the fallback for a body-less failure.
         */
        fun forStatus(code: Int, label: String, serverMessage: String? = null): ApiError = ApiError(
            status = code,
            // 5xx, 408 (timeout) and 429 (rate limit) are worth another try.
            retryable = code >= 500 || code == 408 || code == 429,
            userMessage = safeServerMessage(serverMessage) ?: when {
                code == 401 || code == 403 -> "$label: sign-in required."
                code == 404 -> "$label: not found."
                code == 429 -> "$label: too many requests, try again shortly."
                code >= 500 -> "$label: the server is unavailable."
                else -> "$label failed (HTTP $code)."
            },
        )

        /** Builds the error for a request that never reached the server. */
        fun forNetworkFailure(label: String): ApiError = ApiError(
            status = null,
            retryable = true,
            userMessage = "$label: no connection.",
        )

        /**
         * Builds the error for a 2xx response whose body is not the expected shape.
         *
         * A 200 carrying `{"error":"..."}` or a body with no list in it is a
         * server-side problem, not a network one: retrying is reasonable, and the
         * user must not be shown a raw JSON parse exception.
         */
        fun forMalformedResponse(label: String, serverMessage: String? = null): ApiError = ApiError(
            status = null,
            retryable = true,
            userMessage = safeServerMessage(serverMessage) ?: "$label: unexpected response.",
        )

        /**
         * Accepts a server message only when it is safe to render.
         *
         * A message is rejected when it is blank, absurdly long, or multi-line —
         * the three shapes that turn a snackbar into a wall of text or a stack
         * trace. Anything rejected falls back to the curated sentence.
         */
        internal fun safeServerMessage(raw: String?): String? {
            val text = raw?.trim() ?: return null
            if (text.isEmpty() || text.length > 200) return null
            if (text.any { it == '\n' || it == '\r' }) return null
            return text
        }
    }
}

/**
 * Supplies the bearer token for a request.
 *
 * The auth seam: the client asks for a token per request, so a real app can
 * plug in its session store and a test can plug in a constant — neither has to
 * touch the transport. Returning null means "send the request anonymously",
 * which is the correct behaviour for a public endpoint.
 */
fun interface TokenProvider {
    /** The current token, or null for an anonymous request. */
    fun token(): String?

    companion object {
        /** The default: no auth. The kit ships with no session. */
        val None: TokenProvider = TokenProvider { null }
    }
}

/**
 * The one-method seam between [JsonHttpClient] and the network.
 *
 * A `fun interface`, so a test can write `HttpTransport { method, url, body ->
 * ... }` inline. [execute] returns the response body and throws on any failure.
 */
fun interface HttpTransport {
    /**
     * Performs one request and returns the response body. Throws on failure.
     *
     * @param token the bearer token to send, or null for an anonymous request.
     *   It is a PARAMETER rather than something the transport fetches itself so
     *   that the client owns the "one token per request" rule and a fake
     *   transport can assert exactly which token a call carried.
     */
    fun execute(method: String, url: String, body: String?, token: String?): String

    /**
     * Performs one request and delivers streamed lines to [onLine].
     * Defaults to executing and splitting the response body by line sequence.
     */
    fun stream(
        method: String,
        url: String,
        body: String?,
        token: String?,
        onLine: (String) -> Unit,
    ) {
        val result = execute(method, url, body, token)
        result.lineSequence().forEach(onLine)
    }
}

/**
 * The default transport: a plain [HttpURLConnection].
 *
 * Deliberately dependency-free — no HTTP client library is added for this.
 * A non-2xx status throws [HttpStatusException]; a network failure throws
 * whatever the JDK throws.
 */
class UrlConnectionTransport(
    private val connectTimeoutMs: Int = 15_000,
    /**
     * How long to wait for the first byte of the response.
     *
     * 10s was too short for this backend. The service runs on Cloud Run, which
     * scales to zero: a cold start (container boot + JVM + route init) routinely
     * takes longer than 10s, and the request then failed with a read timeout
     * that looked like "no connection" to the user. 30s covers a cold start
     * while still failing in a time a person will wait.
     */
    private val readTimeoutMs: Int = 30_000,
) : HttpTransport {

    override fun execute(method: String, url: String, body: String?, token: String?): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            // dua-backend treats a client that sends this header, and no Origin,
            // as the Android app. Account sign-in then returns the refresh token
            // in JSON. HttpURLConnection does not send Origin, so this is the
            // marker the server needs. It is set on every call, including
            // anonymous ones, because the contract asks the native client for it.
            setRequestProperty("X-Dua-Native", "1")
            // The auth header. Without this line the whole TokenProvider seam
            // was decorative: the client read a token and then dropped it.
            sanitizeToken(token)?.let { setRequestProperty("Authorization", "Bearer $it") }
            // Do NOT follow redirects on an authenticated request.
            //
            // WHY: HttpURLConnection re-issues the request against the redirect
            // target, and a 3xx from a compromised or misconfigured endpoint
            // would then carry the bearer token to a host of the server's
            // choosing. A JSON API has no legitimate reason to redirect, so a
            // 3xx is reported as a failure instead of being followed.
            instanceFollowRedirects = false
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                // Read the ERROR stream, not the input stream: on a non-2xx the
                // body lives in errorStream, and it is where the backend puts
                // `{"error":"..."}`. Reading it here is what lets the UI show
                // the server's own sentence instead of a generic one.
                val errorBody = runCatching {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull()
                throw HttpStatusException(code, errorBody)
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    override fun stream(
        method: String,
        url: String,
        body: String?,
        token: String?,
        onLine: (String) -> Unit,
    ) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("X-Dua-Native", "1")
            sanitizeToken(token)?.let { setRequestProperty("Authorization", "Bearer $it") }
            instanceFollowRedirects = false
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                val errorBody = runCatching {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull()
                throw HttpStatusException(code, errorBody)
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    onLine(line)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Normalises a token before it becomes a header value.
     *
     * A token pasted from a file or an env var routinely carries a trailing
     * newline, and `setRequestProperty` rejects a value containing a line break
     * (on Android it can instead produce a malformed request line). Trimming
     * fixes the common case; a token with an EMBEDDED line break is not a token
     * at all — sending it would be header injection — so it is dropped and the
     * request goes out anonymous, where the server's 401 is the honest answer.
     */
    private fun sanitizeToken(token: String?): String? {
        val trimmed = token?.trim() ?: return null
        if (trimmed.isEmpty()) return null
        if (trimmed.any { it == '\r' || it == '\n' }) return null
        return trimmed
    }
}

/**
 * The shared HTTP client both remote data sources use.
 *
 * @param transport where the request actually goes. Defaults to the real
 *   [UrlConnectionTransport]; a test injects a fake.
 * @param label the feature name used in the failure message, so an error still
 *   says which call failed ("Artifacts request failed: HTTP 500") even though
 *   the mechanics are shared.
 * @param tokenProvider the auth seam. Defaults to [TokenProvider.None], so the
 *   kit's placeholder backend is called anonymously until a session exists.
 */
class JsonHttpClient(
    private val transport: HttpTransport = UrlConnectionTransport(),
    private val label: String = "Request",
    private val tokenProvider: TokenProvider = TokenProvider.None,
    /**
     * How many times a retryable failure is attempted, and how long to wait.
     *
     * Defaults to [RetryPolicy.Default]: three attempts for an IDEMPOTENT method
     * (GET / DELETE), one for a POST. Retrying a POST is not safe — the server
     * may already have created the row, and a second attempt would either
     * duplicate it or be rejected as a duplicate id.
     */
    private val retryPolicy: RetryPolicy = RetryPolicy.Default,
) {

    /**
     * Performs one request and returns the response body.
     *
     * Throws [IllegalStateException] with a labelled message on a non-2xx
     * status, and lets a network failure propagate as-is. The repository turns
     * either into its Error state, so a caller only has to report failure.
     *
     * Prefer [requestOrThrow] unless you really only want the body: this method's
     * failure message is the raw `"<label> failed: HTTP 500"` string, which is
     * what a caller that forwards `e.message` to the UI will end up showing.
     *
     * A [CancellationException] is ALWAYS rethrown untouched: a cancelled
     * coroutine is not a failed request, and swallowing it would break the
     * caller's structured concurrency.
     */
    fun request(
        method: String,
        url: String,
        body: String? = null,
        label: String? = null,
    ): String =
        try {
            transport.execute(method, url, body, authToken())
        } catch (e: CancellationException) {
            // Never convert cancellation into a failure. Rethrow as-is.
            throw e
        } catch (e: HttpStatusException) {
            throw IllegalStateException("${label ?: this.label} failed: HTTP ${e.code}")
        }

    /**
     * Like [request], but reports failure as a typed [ApiError].
     *
     * This is the entry point new code should use: the caller can branch on
     * [ApiError.retryable] to decide whether to offer a Retry action, and can
     * show [ApiError.userMessage] without leaking internals.
     */
    fun requestOrThrow(
        method: String,
        url: String,
        body: String? = null,
        label: String? = null,
    ): String {
        val effectiveLabel = label ?: this.label
        val attempts = retryPolicy.attemptsFor(method)
        var lastError: ApiError? = null

        for (attempt in 1..attempts) {
            try {
                return transport.execute(method, url, body, authToken())
            } catch (e: CancellationException) {
                // A cancelled coroutine is not a failed request. Never retry it.
                throw e
            } catch (e: HttpStatusException) {
                val error = ApiError.forStatus(e.code, effectiveLabel, errorMessageOf(e.body))
                lastError = error
                // A 4xx is not retryable: retrying a 401 or a 422 just burns the
                // user's battery and delays the honest message.
                if (!error.retryable || attempt == attempts) throw error
            } catch (e: IOException) {
                val error = ApiError.forNetworkFailure(effectiveLabel)
                lastError = error
                if (attempt == attempts) throw error
            }
            retryPolicy.backoffBeforeRetry(attempt)
        }

        // Unreachable in practice (the loop either returns or throws), but the
        // compiler needs a terminal statement.
        throw lastError ?: ApiError.forNetworkFailure(effectiveLabel)
    }

    /**
     * Performs a streaming request (SSE) delivering lines to [onLine].
     */
    fun stream(
        method: String,
        url: String,
        body: String? = null,
        label: String? = null,
        onLine: (String) -> Unit,
    ) {
        val effectiveLabel = label ?: this.label
        val attempts = retryPolicy.attemptsFor(method)
        var lastError: ApiError? = null

        for (attempt in 1..attempts) {
            try {
                transport.stream(method, url, body, authToken(), onLine)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpStatusException) {
                val error = ApiError.forStatus(e.code, effectiveLabel, errorMessageOf(e.body))
                lastError = error
                if (!error.retryable || attempt == attempts) throw error
            } catch (e: IOException) {
                val error = ApiError.forNetworkFailure(effectiveLabel)
                lastError = error
                if (attempt == attempts) throw error
            }
            retryPolicy.backoffBeforeRetry(attempt)
        }

        throw lastError ?: ApiError.forNetworkFailure(effectiveLabel)
    }

    /**
     * The bearer token for the next request, or null when anonymous.
     *
     * Called once per request by both [request] and [requestOrThrow], so a
     * provider that refreshes an expiring token is asked again on every call
     * rather than being frozen at construction time.
     *
     * A provider that throws is NOT caught here: a session store that cannot
     * produce a token is a real failure, and silently downgrading the request
     * to anonymous would turn it into a confusing 401 instead.
     */
    fun authToken(): String? = tokenProvider.token()
}

/**
 * How a retryable failure is retried.
 *
 * WHY THIS EXISTS
 * ---------------
 * The client had no retry at all: one transient 503 from a Cloud Run instance
 * that was still warming up, or one dropped connection on a flaky mobile link,
 * surfaced as a hard error the user had to dismiss and re-trigger by hand. The
 * backend is a scale-to-zero service, so a transient failure is the NORMAL case
 * on the first request after idle, not an exceptional one.
 *
 * Retrying is only safe for an IDEMPOTENT method. A GET or a DELETE can be
 * repeated without changing the outcome; a POST cannot — the server may already
 * have created the row, so a second attempt either duplicates it or is rejected
 * as a duplicate id. [attemptsFor] therefore returns 1 for a POST unless a
 * caller explicitly opts in.
 *
 * @param maxAttempts total attempts, including the first. 1 disables retry.
 * @param baseDelayMs the first backoff; it doubles per attempt (400, 800, …).
 * @param retryNonIdempotent when true, POST is retried too. Off by default.
 */
class RetryPolicy(
    val maxAttempts: Int = 3,
    val baseDelayMs: Long = 400L,
    val retryNonIdempotent: Boolean = false,
) {

    /** How many attempts [method] gets. */
    fun attemptsFor(method: String): Int =
        if (retryNonIdempotent || method.uppercase() in IDEMPOTENT) {
            maxAttempts.coerceAtLeast(1)
        } else {
            1
        }

    /**
     * Waits before the next attempt.
     *
     * A plain [Thread.sleep] is correct here: every caller runs inside
     * `withContext(Dispatchers.IO)`, so blocking this thread does not block the
     * UI. Exponential backoff (400ms, 800ms) keeps a burst of retries from
     * hammering a service that is already struggling.
     */
    fun backoffBeforeRetry(attempt: Int) {
        val delay = baseDelayMs shl (attempt - 1).coerceIn(0, 4)
        if (delay > 0) Thread.sleep(delay)
    }

    companion object {
        /** Methods that can be repeated without changing the outcome. */
        private val IDEMPOTENT = setOf("GET", "HEAD", "DELETE", "PUT", "OPTIONS")

        /** Three attempts for idempotent calls, one for a POST. */
        val Default = RetryPolicy()

        /** No retry at all — for a test that wants to count exactly one call. */
        val None = RetryPolicy(maxAttempts = 1)
    }
}

/**
 * Percent-encodes one URL PATH SEGMENT.
 *
 * WHY THIS EXISTS
 * ---------------
 * An id interpolated straight into a path (`"$baseUrl/$id"`) is not a value, it
 * is URL STRUCTURE: `art/1` becomes two segments, `art?x=1` becomes a query,
 * `art#frag` becomes a fragment, and a space produces an invalid request line.
 * Encoding makes the id one opaque segment, which is what the endpoint expects.
 *
 * `URLEncoder` is deliberately NOT used: it is form encoding, so it turns a
 * space into `+` (which a path reads literally) and leaves `*` alone. This
 * encodes everything except RFC 3986's unreserved set, in UTF-8.
 */
fun encodePathSegment(segment: String): String {
    val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    val out = StringBuilder(segment.length)
    for (byte in segment.toByteArray(Charsets.UTF_8)) {
        val ch = (byte.toInt() and 0xFF).toChar()
        if (ch in unreserved) {
            out.append(ch)
        } else {
            out.append('%')
            out.append("0123456789ABCDEF"[(byte.toInt() and 0xFF) shr 4])
            out.append("0123456789ABCDEF"[byte.toInt() and 0x0F])
        }
    }
    return out.toString()
}

/**
 * Extracts the `error` field from a `{"error":"..."}` body, or null.
 *
 * WHY THIS EXISTS
 * ---------------
 * The backend reports every failure as `{"error":"<sentence>"}` — a message it
 * wrote for the user. The app used to discard the body entirely and show a
 * generic "sign-in required", so the one useful sentence never reached the
 * screen. This reads it, and returns null for anything that is not that shape
 * (an HTML error page, an empty body, a bare string) so the caller falls back to
 * its curated message instead of showing markup.
 */
fun errorMessageOf(body: String?): String? {
    val text = body?.trim() ?: return null
    if (text.isEmpty() || !text.startsWith("{")) return null
    return runCatching {
        org.json.JSONObject(text).optString("error", "").takeIf { it.isNotBlank() }
    }.getOrNull()
}

/**
 * Unwraps a list from a JSON envelope.
 *
 * WHY THIS EXISTS
 * ---------------
 * The backend does not return a bare array. `GET /api/artifacts` answers
 * `{"artifacts":[...]}` and `GET /api/memories` answers `{"memories":[...]}`.
 * The app parsed the body with `JSONArray(body)`, which throws on an object — so
 * every successful response was reported as a parse failure and the list never
 * appeared.
 *
 * This accepts BOTH shapes, because a bare array is what the older contract (and
 * the existing tests) use, and a server that starts wrapping its payload must
 * not break the app:
 *
 *   • `{"<key>":[...]}`  → the array under [key]
 *   • `[...]`            → the array itself
 *
 * A body that is neither — including a 200 carrying `{"error":"..."}` — throws
 * an [ApiError] with a safe message, so the screen shows "unexpected response"
 * rather than a raw `JSONException`.
 *
 * @param body the raw response body.
 * @param key the envelope key ("artifacts" / "memories").
 * @param label the feature label used in the failure message.
 */
fun unwrapList(body: String, key: String, label: String): JSONArray {
    val text = body.trim()
    if (text.isEmpty()) throw ApiError.forMalformedResponse(label)
    return try {
        when {
            text.startsWith("[") -> JSONArray(text)
            text.startsWith("{") -> {
                val root = JSONObject(text)
                // A 200 that carries an error is still a failure, not an empty list.
                val serverError = root.optString("error", "")
                if (serverError.isNotBlank()) {
                    throw ApiError.forMalformedResponse(label, serverError)
                }
                root.optJSONArray(key)
                    ?: throw ApiError.forMalformedResponse(label)
            }
            else -> throw ApiError.forMalformedResponse(label)
        }
    } catch (e: ApiError) {
        throw e
    } catch (e: org.json.JSONException) {
        throw ApiError.forMalformedResponse(label)
    }
}

/**
 * The ONE [JsonHttpClient] the whole app shares.
 *
 * WHY A SINGLETON
 * ---------------
 * The two remote data sources used to each construct their own default
 * [JsonHttpClient] — two instances, two `HttpURLConnection` connection pools,
 * two copies of one timeout config. The client is a property of the APP, not of
 * a feature, so it lives in one place and both sources default to it.
 *
 * The per-request [JsonHttpClient.request] `label` argument is what keeps the
 * shared instance honest: one client, but an error still says which feature
 * failed ("Artifacts request failed: HTTP 500" vs "Memory request failed:
 * HTTP 500"). A caller that needs a bespoke instance (a test with a fake
 * transport) still passes its own — the injectable parameter is untouched.
 */
object HttpClientProvider {
    /**
     * The app-wide client. Label is generic; callers pass per-request labels.
     *
     * It reads [TokenStore] on every request, so a sign-in flow only has to set
     * the token once and both features start sending it — no client rebuild.
     */
    val client: JsonHttpClient = JsonHttpClient(
        label = "Request",
        tokenProvider = TokenStore.provider,
    )
}

/*
 * [TokenStore] used to live here as a plain `@Volatile var`.
 *
 * It moved to SessionStore.kt for two reasons, both of which were review
 * blockers: nothing in `app/src/main` ever WROTE to it (so the token was always
 * null and the remote sources were never constructed), and a plain var is not
 * Compose state (so a write would not have recomposed the nav host anyway).
 *
 * The store is now a StateFlow with a documented [SessionBridge] seam for a real
 * auth provider. See SessionStore.kt.
 */
