package il.co.radioapp.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

/**
 * Fetches ICY StreamTitle by manually following HTTP redirects,
 * re-sending "Icy-MetaData: 1" on every hop (Java's built-in redirect
 * following drops custom headers, causing the server to omit icy-metaint).
 */
object IcyMetadataFetcher {

    suspend fun fetch(streamUrl: String): String? = withContext(Dispatchers.IO) {
        if (streamUrl.contains(".m3u8") || streamUrl.contains(".m3u")) {
            return@withContext null
        }
        runCatching {
            fetchInternal(streamUrl)
        }.getOrNull()
    }

    private fun fetchInternal(streamUrl: String): String? {
        var currentUrl = streamUrl
        var conn: HttpURLConnection? = null

        for (hop in 0 until 5) {
            conn?.disconnect()
            conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout          = 8_000
                readTimeout             = 15_000
                instanceFollowRedirects = false          // manual redirect handling
                setRequestProperty("Icy-MetaData", "1") // must be sent on EVERY hop
                setRequestProperty("User-Agent", "RadioIsraelApp/3.0")
                setRequestProperty("Connection", "close")
                connect()
            }

            val code = conn!!.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location") ?: break
                currentUrl = if (location.startsWith("http")) location
                             else URL(URL(currentUrl), location).toString()
                continue                                  // follow redirect, keep headers
            }
            if (code != 200) break

            // We reached the real stream URL — look for icy-metaint
            val metaIntStr = conn.getHeaderField("icy-metaint")
                ?: conn.getHeaderField("Icy-MetaInt")
                ?: return null                           // stream doesn't carry ICY metadata

            val metaInt = metaIntStr.toIntOrNull() ?: return null
            val stream  = conn.inputStream

            // Skip the audio bytes preceding the first metadata block
            var skipped = 0L
            while (skipped < metaInt.toLong()) {
                val s = stream.skip(metaInt.toLong() - skipped)
                if (s <= 0L) break
                skipped += s
            }

            // Read metadata: 1 length byte, then length*16 bytes of data
            val lenByte = stream.read()
            if (lenByte <= 0) return null
            val metaLen = lenByte * 16
            if (metaLen == 0) return null

            val buf = ByteArray(metaLen)
            var total = 0
            while (total < metaLen) {
                val r = stream.read(buf, total, metaLen - total)
                if (r < 0) break
                total += r
            }

            // ICY streams may send Hebrew in Windows-1255 rather than UTF-8
            val metaStr = decodeIcyMetadata(buf)
            val match   = Regex("StreamTitle='([^']*)'").find(metaStr)
            val title   = match?.groupValues?.getOrNull(1)?.trim()
            conn.disconnect()
            return if (title.isNullOrBlank()) null else title
        }

        conn?.disconnect()
        return null
    }
    /** Try UTF-8; if it produces replacement chars (U+FFFD), fall back to Windows-1255. */
    private fun decodeIcyMetadata(buf: ByteArray): String {
        val utf8 = String(buf, Charsets.UTF_8).trimEnd('\u0000')
        if (!utf8.contains('\uFFFD')) return utf8
        return try {
            String(buf, Charset.forName("windows-1255")).trimEnd('\u0000')
        } catch (_: Exception) {
            String(buf, Charsets.ISO_8859_1).trimEnd('\u0000')
        }
    }
}



