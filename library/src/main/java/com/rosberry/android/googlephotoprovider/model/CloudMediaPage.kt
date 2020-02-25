package com.rosberry.android.googlephotoprovider.model

import com.rosberry.android.googlephotoprovider.CloudMedia

/**
 * @author mmikhailov on 2019-11-05.
 */
data class CloudMediaPage(
        val mediaList: List<CloudMedia>,
        val nextPageToken: String?
)