/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample.tools

import com.rosberry.android.googlephotoprovider.CloudMedia
import rosberry.com.sample.entity.MediaType

/**
 * @author Evgeniy Nagibin on 13/03/2020.
 */
object MediaUtil {

    fun String.toMediaType(): MediaType =
            when {
                this.startsWith("video") -> MediaType.VIDEO
                this.startsWith("image") -> MediaType.PHOTO
                else -> MediaType.PHOTO
            }

    fun CloudMedia.asMediaType(): MediaType = mimeType.toMediaType()
}