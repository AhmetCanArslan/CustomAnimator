package com.arslan.customanimator.utils

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

object ConditionalDownload {

    private const val TIMEOUT_MS = 30_000

    class Response(val body: String?, val etag: String?)

    fun fetch(url: String, knownEtag: String?, maxBytes: Long, tag: String): Response? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                knownEtag?.let { setRequestProperty("If-None-Match", it) }
            }
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return Response(null, knownEtag)
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(tag, "Unexpected response: ${connection.responseCode}")
                return null
            }
            val etag = connection.getHeaderField("ETag")
            connection.inputStream.bufferedReader().use { reader ->
                val builder = StringBuilder()
                val buffer = CharArray(8192)
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    builder.append(buffer, 0, read)
                    if (builder.length > maxBytes) {
                        Log.w(tag, "Download exceeds the size limit")
                        return null
                    }
                }
                Response(builder.toString(), etag)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to download $url", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}
