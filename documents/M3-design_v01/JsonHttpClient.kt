package ai.duagenai.design.screens

import java.net.HttpURLConnection
import java.net.URL

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
 */
class JsonHttpClient(
    private val transport: HttpTransport = UrlConnectionTransport(),
    private val label: String = "Request",
) {

    /**
     * Performs one request and returns the response body.
     *
     * Throws [IllegalStateException] with a labelled message on a non-2xx
     * status, and lets a network failure propagate as-is. The repository turns
     * either into its Error state, so a caller only has to report failure.
     */
    fun request(
        method: String,
        url: String,
        body: String? = null,
        label: String? = null,
    ): String =
        try {
            transport.execute(method, url, body)
        } catch (e: HttpStatusException) {
            throw IllegalStateException("${label ?: this.label} failed: HTTP ${e.code}")
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
    /** The app-wide client. Label is generic; callers pass per-request labels. */
    val client: JsonHttpClient = JsonHttpClient(label = "Request")
}
