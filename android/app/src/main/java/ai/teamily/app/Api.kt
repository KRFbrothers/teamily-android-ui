package ai.teamily.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiException(message: String) : Exception(message)

/** Holds the JWT and talks to the Teamily FastAPI backend. */
class Api(context: Context) {
    private val prefs = context.getSharedPreferences("teamily", Context.MODE_PRIVATE)
    var token by mutableStateOf(prefs.getString(TOKEN_KEY, null))
        private set

    suspend fun register(email: String, name: String, password: String) {
        val body = JSONObject().put("email", email).put("name", name).put("password", password)
        saveToken(request("/auth/register", body.toString(), "application/json", auth = false))
    }

    suspend fun login(email: String, password: String) {
        val form = "username=${enc(email)}&password=${enc(password)}"
        saveToken(request("/auth/login", form, "application/x-www-form-urlencoded", auth = false))
    }

    suspend fun assist(prompt: String): String =
        request("/ai/assist", JSONObject().put("prompt", prompt).toString(), "application/json")
            .getString("answer")

    fun signOut() {
        prefs.edit().remove(TOKEN_KEY).apply()
        token = null
    }

    private fun saveToken(json: JSONObject) {
        val t = json.getString("access_token")
        prefs.edit().putString(TOKEN_KEY, t).apply()
        token = t
    }

    private suspend fun request(path: String, body: String, contentType: String, auth: Boolean = true): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 10_000
                conn.readTimeout = 60_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                if (auth) token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
                conn.outputStream.use { it.write(body.toByteArray()) }

                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code == 401 && auth) signOut()
                if (code !in 200..299) throw ApiException(errorDetail(text) ?: "Request failed ($code)")
                JSONObject(text)
            } catch (e: java.io.IOException) {
                throw ApiException("Can't reach the server. Is the backend running?")
            } finally {
                conn.disconnect()
            }
        }

    private fun errorDetail(text: String): String? =
        runCatching { JSONObject(text).optString("detail").takeIf { it.isNotBlank() } }.getOrNull()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    companion object {
        private const val TOKEN_KEY = "access-token"

        // 10.0.2.2 is the host machine from the Android emulator; change for a real device.
        const val BASE_URL = "http://10.0.2.2:8000"
    }
}
