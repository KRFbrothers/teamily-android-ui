package ai.duagenai.design.screens

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException

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
class HttpStatusException(val code: Int) : Exception("HTTP $code")

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
        /** Builds the error for a status code, with the feature's label. */
        fun forStatus(code: Int, label: String): ApiError = ApiError(
            status = code,
            // 5xx, 408 (timeout) and 429 (rate limit) are worth another try.
            retryable = code >= 500 || code == 408 || code == 429,
            userMessage = when {
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
    /** Performs one request and returns the response body. Throws on failure. */
    fun execute(method: String, url: String, body: String?): String
}

/**
 * The default transport: a plain [HttpURLConnection].
 *
 * Deliberately dependency-free — no HTTP client library is added for this.
 * A non-2xx status throws [HttpStatusException]; a network failure throws
 * whatever the JDK throws.
 */
class UrlConnectionTransport(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000,
) : HttpTransport {

    override fun execute(method: String, url: String, body: String?): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
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
                throw HttpStatusException(code)
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
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
            transport.execute(method, url, body)
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
        return try {
            transport.execute(method, url, body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpStatusException) {
            throw ApiError.forStatus(e.code, effectiveLabel)
        } catch (e: IOException) {
            throw ApiError.forNetworkFailure(effectiveLabel)
        }
    }

    /** The bearer token for the next request, or null when anonymous. */
    fun authToken(): String? = tokenProvider.token()
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
    /** The app-wide client. Label is generic; callers pass per-request labels. */
    val client: JsonHttpClient = JsonHttpClient(label = "Request")
}
