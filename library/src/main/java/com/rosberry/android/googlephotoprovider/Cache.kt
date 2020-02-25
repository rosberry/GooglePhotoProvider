/*
 *
 *  * Copyright (c) 2019 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.googlephotoprovider

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import okhttp3.Response
import java.io.File
import java.util.UUID

/**
 * @author Alexei Korshun on 2019-11-19.
 */
internal class Cache(context: Context) {

    private val cacheDir: File = File(context.cacheDir, "google_photo")
        .apply { mkdir() }

    internal fun get(mediaId: String): Uri? =
            cacheDir.listFiles { _, name -> name.contains(mediaId.fileName()) }
                ?.getOrNull(0)
                ?.absolutePath
                ?.let { path -> Uri.parse(path) }

    internal fun put(mediaId: String, response: Response): Uri {
        val fileBytes = response.body()!!.byteStream()
            .readBytes()

        val extension: String = MimeTypeMap.getSingleton().getExtensionFromMimeType(response.header("content-type"))
            ?: throw IllegalStateException("Unknown media content type.")

        return File(cacheDir, "${mediaId.fileName()}.$extension")
            .also { file -> file.writeBytes(fileBytes) }
            .let { file -> Uri.fromFile(file) }
    }

    private fun String.fileName(): String = this.byteInputStream()
        .readBytes()
        .let { bytes -> UUID.nameUUIDFromBytes(bytes) }
        .toString()
}