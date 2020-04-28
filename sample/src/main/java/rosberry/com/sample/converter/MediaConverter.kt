/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample.converter

import android.net.Uri
import com.rosberry.android.googlephotoprovider.CloudMedia
import rosberry.com.sample.data.Constant
import rosberry.com.sample.entity.Media
import rosberry.com.sample.entity.MediaType
import rosberry.com.sample.tools.MediaUtil.asMediaType

/**
 * @author Evgeniy Nagibin on 13/03/2020.
 */
class MediaConverter {
    fun convertCloudMedia(cloudMedia: CloudMedia): Media {
        with(cloudMedia) {
            val mediaType = cloudMedia.asMediaType()
            val width = mediaMetadata.width.toInt()
            val height = mediaMetadata.height.toInt()
            val previewUri = Uri.parse(baseUrl.toPreviewPath(mediaType))
            val fullUri = Uri.parse(baseUrl.toFullPath(width, height, mediaType))

            return Media(
                    Constant.GOOGLE_MEDIA_ITEM_ID_PREFIX.plus(id),
                    Constant.GOOGLE_FOLDER_ID,
                    mediaType,
                    width,
                    height,
                    -1,
                    -1,
                    0,
                    previewUri,
                    fullUri
            )
        }
    }

    private fun String.toPreviewPath(mediaType: MediaType): String =
            if (mediaType == MediaType.VIDEO) {
                this.plus("=w")
                    .plus(Constant.THUMBNAIL_MAX_SIZE)
                    .plus("-h")
                    .plus(Constant.THUMBNAIL_MAX_SIZE)
                    .plus("-d")
            } else this

    private fun String.toFullPath(width: Int, height: Int, mediaType: MediaType): String =
            when (mediaType) {
                MediaType.PHOTO -> {
                    this.plus("=w")
                        .plus(if (width > Constant.PROJECT_RENDER_WIDTH) Constant.PROJECT_RENDER_WIDTH else width)
                        .plus("-h")
                        .plus(if (width > Constant.PROJECT_RENDER_HEIGHT) Constant.PROJECT_RENDER_HEIGHT else height)
                }
                MediaType.VIDEO -> this.plus("=dv")
                else -> this
            }

}