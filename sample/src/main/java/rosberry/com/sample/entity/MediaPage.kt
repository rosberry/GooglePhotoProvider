/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample.entity

/**
 * @author Evgeniy Nagibin on 29/04/2020.
 */
data class MediaPage(
        val mediaList: List<Media>,
        val nextPageToken: String?
)