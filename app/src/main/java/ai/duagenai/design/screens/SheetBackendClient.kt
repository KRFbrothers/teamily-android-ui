package ai.duagenai.design.screens

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class SheetUiComponent(
    val componentId: String,
    val visible: Boolean,
    val title: String,
    val subtitle: String,
    val bgHex: String,
    val order: Int,
)

/**
 * Dependency-free client for the Master Google Apps Script backend.
 *
 * Uses a dedicated HttpURLConnection with `instanceFollowRedirects = true`
 * because Google Apps Script Web Apps always respond with a 302 redirect
 * to `script.googleusercontent.com`, whereas [UrlConnectionTransport]
 * disables redirects to protect Bearer tokens.
 */
object SheetBackendClient {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 25_000

    /**
     * Fetches dynamic UI components from the `ui_config` tab.
     */
    suspend fun fetchUiConfig(): Result<List<SheetUiComponent>> = withContext(Dispatchers.IO) {
        runCatching {
            val root = getTab("ui_config")
            val items = root.optJSONArray("items") ?: JSONArray()
            buildList {
                for (i in 0 until items.length()) {
                    val obj = items.optJSONObject(i) ?: continue
                    add(
                        SheetUiComponent(
                            componentId = obj.optString("component_id", ""),
                            visible = obj.optBoolean("visible", false),
                            title = obj.optString("title", ""),
                            subtitle = obj.optString("subtitle", ""),
                            bgHex = obj.optString("bg_hex", ""),
                            order = obj.optInt("order", i),
                        ),
                    )
                }
            }.filter { it.componentId.isNotBlank() && it.visible }
                .sortedBy { it.order }
        }
    }

    /**
     * Generic GET for any tab in the Master Apps Script (`ui_config`, `agents`,
     * `sessions`, `messages`, `memories`, `artifacts`, `settings`, or `all`).
     */
    suspend fun getTab(
        tab: String,
        filterCol: String? = null,
        filterVal: String? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = ApiConfig.SHEET_WEB_APP_URL
        require(baseUrl.isNotBlank()) { "SHEET_WEB_APP_URL is blank" }

        val query = buildString {
            append("?key=").append(encodeQuery(ApiConfig.sheetSecretKey))
            append("&tab=").append(encodeQuery(tab))
            if (!filterCol.isNullOrBlank() && filterVal != null) {
                append("&filter_col=").append(encodeQuery(filterCol))
                append("&filter_val=").append(encodeQuery(filterVal))
            }
        }

        parseOk(executeRequest(method = "GET", url = baseUrl + query, body = null), "Sheet request failed")
    }

    /**
     * Generic POST to append, upsert, or delete a row in any Sheet tab.
     */
    suspend fun postRow(
        tab: String,
        action: String, // "append" | "upsert" | "delete"
        row: JSONObject,
        idCol: String? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = ApiConfig.SHEET_WEB_APP_URL
        require(baseUrl.isNotBlank()) { "SHEET_WEB_APP_URL is blank" }

        val payload = JSONObject().apply {
            put("key", ApiConfig.sheetSecretKey)
            put("tab", tab)
            put("action", action)
            if (!idCol.isNullOrBlank()) put("id_col", idCol)
            put("row", row)
        }

        parseOk(executeRequest(method = "POST", url = baseUrl, body = payload.toString()), "Sheet write failed")
    }

    /**
     * Parses the script's `{"ok":..., "error":...}` envelope.
     *
     * A non-JSON body (usually an HTML login or error page) means the URL is wrong
     * or the script is not deployed with access "Anyone"; surface that clearly
     * instead of a raw JSON parse exception.
     */
    private fun parseOk(raw: String, fallbackError: String): JSONObject {
        val json = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            throw IllegalStateException(
                "Sheet backend returned non-JSON. Check SHEET_WEB_APP_URL and that the script is deployed for \"Anyone\".",
            )
        }
        if (!json.optBoolean("ok", false)) {
            throw IllegalStateException(json.optString("error", fallbackError))
        }
        return json
    }

    private fun executeRequest(method: String, url: String, body: String?): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
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
                throw IOException("Sheet HTTP $code")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, "UTF-8")
}
