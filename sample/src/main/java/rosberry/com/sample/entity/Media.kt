/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample.entity

import android.net.Uri

/**
 * @author Evgeniy Nagibin on 13/03/2020.
 */
data class Media(
        val id: String,
        val folderId: Long,
        val type: MediaType,
        val width: Int,
        val height: Int,
        val orientation: Int,
        val duration: Long,
        val dateModified: Long,
        val previewUri: Uri,
        val fullUri: Uri
)