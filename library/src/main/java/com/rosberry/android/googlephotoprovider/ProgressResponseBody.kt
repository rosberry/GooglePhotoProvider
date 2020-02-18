/*
 *
 *  * Copyright (c) 2019 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.googlephotoprovider

import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Okio
import okio.Source
import java.io.IOException

/**
 * @author Alexei Korshun on 2019-11-19.
 */
internal class ProgressResponseBody internal constructor(
        private val responseBody: ResponseBody?,
        private val progressListener: ProgressListener
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentType() = responseBody?.contentType()

    override fun contentLength() = responseBody?.contentLength() ?: -1

    override fun source(): BufferedSource =
            bufferedSource ?: Okio.buffer(source(responseBody!!.source()))
                .also { newBufferedSource -> bufferedSource = newBufferedSource }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead: Long = 0
            val contentLength: Long = contentLength() / 100

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += bytesRead
                    .takeIf { bytesRead != -1L }
                    ?: 0L
                progressListener.update(totalBytesRead / contentLength, bytesRead == -1L)
                return bytesRead
            }
        }
    }
}