/*
 *
 *  * Copyright (c) 2019 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.googlephotoprovider

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.rosberry.android.googlephotoprovider.network.HttpHandler.fileName
import java.io.File

/**
 * @author Alexei Korshun on 2019-11-19.
 */
internal class Cache(context: Context) {

    val cacheDir: File = File(context.cacheDir, "google_photo")
        .apply { mkdir() }

    internal fun get(mediaId: String): Uri? =
            cacheDir.listFiles { _, name -> name.contains(mediaId.fileName()) }
                ?.getOrNull(0)
                ?.absolutePath
                ?.let { path -> Uri.parse(path) }

    internal fun put(mediaId: String, contentType: String, fileBytes: ByteArray): Uri {
        val extension: String = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
            ?: throw IllegalStateException("Unknown media content type.")

        return File(cacheDir, "${mediaId.fileName()}.$extension")
            .also { file -> file.writeBytes(fileBytes) }
            .let { file -> Uri.fromFile(file) }
    }
}