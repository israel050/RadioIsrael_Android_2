package il.co.radioapp.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches now-playing info from KCM.fm.
 * Endpoint: GET https://kcm.fm/Home/LiveJ/{numericId}
 * Response:  {"status":1,"item":{..."playing":"אמן - שיר"...}}
 */
object KcmNowPlayingFetcher {

    suspend fun fetch(stationId: String): String? = withContext(Dispatchers.IO) {
        val numId = stationId.removePrefix("kcm-").trim()
        if (numId.isBlank()) return@withContext null
        runCatching { fetchFromApi(numId) }.getOrNull()
    }

    private fun fetchFromApi(numId: String): String? {
        val url = "https://kcm.fm/Home/LiveJ/$numId"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout    = 8_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "RadioIsraelApp/3.0")
                connect()
            }
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            parseResponse(body)
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseResponse(json: String): String? {
        return try {
            val root = JSONObject(json)
            if (root.optInt("status") != 1) return null
            val playing = root.getJSONObject("item")
                .optString("playing")
                .trim()
                .ifBlank { return null }
            // Strip trailing " - LIVE" suffix (case-insensitive) without regex
            val liveSuffix = " - LIVE"
            if (playing.endsWith(liveSuffix, ignoreCase = true))
                playing.dropLast(liveSuffix.length).trim()
            else
                playing
        } catch (_: Exception) { null }
    }
}



