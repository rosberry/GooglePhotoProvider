package com.rosberry.android.googlephotoprovider.response

import com.google.gson.annotations.SerializedName

data class GetAccessTokenResponse(
        @SerializedName("access_token")
        val accessToken: String,
        @SerializedName("expires_in")
        val expires: String
)