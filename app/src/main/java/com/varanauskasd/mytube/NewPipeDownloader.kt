package com.varanauskasd.mytube

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Minimal [Downloader] NewPipeExtractor needs to perform its own HTTP calls. */
class NewPipeDownloader : Downloader() {

    override fun execute(request: Request): Response {
        val connection = URL(request.url()).openConnection() as HttpURLConnection
        connection.requestMethod = request.httpMethod()
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true

        request.headers().forEach { (name, values) ->
            values.forEach { value -> connection.addRequestProperty(name, value) }
        }
        if (connection.getRequestProperty("User-Agent") == null) {
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
            )
        }
        // Pre-accept the EU/UK cookie-consent interstitial (consent.youtube.com),
        // otherwise GB/EU IPs get a redirect page with no ytInitialData to parse.
        if (request.url().contains("youtube.com") && connection.getRequestProperty("Cookie") == null) {
            connection.setRequestProperty("Cookie", "SOCS=CAISAiAD; CONSENT=YES+1")
        }

        request.dataToSend()?.let { body ->
            connection.doOutput = true
            connection.outputStream.use { it.write(body) }
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..399) connection.inputStream else connection.errorStream
        val bodyBytes = stream?.use { it.readAllBytesCompat() } ?: ByteArray(0)

        return Response(
            responseCode,
            connection.responseMessage ?: "",
            connection.headerFields,
            String(bodyBytes, Charsets.UTF_8),
            connection.url.toString(),
        )
    }
}

private fun InputStream.readAllBytesCompat(): ByteArray {
    val buffer = ByteArrayOutputStream()
    val chunk = ByteArray(8192)
    while (true) {
        val read = this.read(chunk)
        if (read == -1) break
        buffer.write(chunk, 0, read)
    }
    return buffer.toByteArray()
}
