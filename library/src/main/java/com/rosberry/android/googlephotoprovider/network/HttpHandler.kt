/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.googlephotoprovider.network

import android.accounts.NetworkErrorException
import android.net.Uri
import android.util.Log
import com.rosberry.android.googlephotoprovider.ProgressListener
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

/**
 * @author Evgeniy Nagibin on 07/12/2020.
 */

object HttpHandler {

    private const val CONNECT_TIMEOUT = 3000
    private const val READ_TIMEOUT = 3000
    private const val BUFFER_SIZE = 4096
    private const val POST = "POST"

    private val TAG = this::class.java.simpleName

    private const val baseUrl = "https://www.googleapis.com/oauth2/v4/"

    fun <T> post(
            endpoint: String,
            params: HashMap<String, out Any>? = null,
            mapper: (JSONObject) -> T,
            success: ((T) -> Unit)? = null,
            error: ((Throwable) -> Unit)? = null,
            complete: () -> Unit
    ) {

        val url = URL(baseUrl.plus(endpoint))
        val conn: HttpURLConnection = (url.openConnection() as HttpURLConnection)
            .apply {
                requestMethod = POST
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                doOutput = true
                setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                )
            }
        params?.let {
            val os: OutputStream = conn.outputStream
            BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                .apply {
                    write(generateParams(params))
                    flush()
                    close()
                }
            os.close()
        }

        val rc = conn.responseCode
        if (rc != HttpURLConnection.HTTP_OK) {
            error?.invoke(NetworkErrorException("Exception: $rc"))
            complete()
        }
        val inp: InputStream = BufferedInputStream(conn.inputStream)
        val resp: String = inp.bufferedReader(UTF_8).use { it.readText() }
        success?.invoke(mapper.invoke(JSONObject(resp)))
        complete.invoke()
    }

    private fun generateParams(params: HashMap<String, out Any>): String {
        val builder = StringBuilder()
        var first = true
        params.forEach { entry ->
            if (first) {
                builder.append("${entry.key}=${entry.value}")
            } else {
                builder.append("&${entry.key}=${entry.value}")
            }
            first = false
        }
        return builder.toString()
    }

    @Throws(IOException::class)
    fun downloadFile(
            uri: Uri,
            fileName: String,
            progressListener: ProgressListener? = null,
            success: ((ResponseModel) -> Unit)? = null,
            error: ((Throwable) -> Unit)? = null
    ) {
        val url = URL(uri.toString())
        val conn = url.openConnection() as HttpURLConnection
        val responseCode = conn.responseCode

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val disposition = conn.getHeaderField("Content-Disposition")
            val contentType = conn.contentType
            val contentLength = conn.contentLength

            Log.d(TAG, "Content-Type = $contentType")
            Log.d(TAG, "Content-Disposition = $disposition")
            Log.d(TAG, "Content-Length = $contentLength")
            Log.d(TAG, "fileName = $fileName")
            val inputStream = conn.inputStream
            val bytes = conn.inputStream.readBytes().toList()
            success?.invoke(ResponseModel(contentType, bytes))
            inputStream.close()
            Log.d(TAG, "File downloaded")
        } else {
            error?.invoke(NetworkErrorException("Network error: $responseCode"))
        }
        conn.disconnect()
    }

    data class ResponseModel(val contentType: String, val bytes: List<Byte>)

    fun String.fileName(): String = this.byteInputStream()
        .readBytes()
        .let { bytes -> UUID.nameUUIDFromBytes(bytes) }
        .toString()
}