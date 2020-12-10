/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.googlephotoprovider.network

import com.rosberry.android.googlephotoprovider.response.GetAccessTokenResponse
import org.json.JSONObject

/**
 * @author Evgeniy Nagibin on 08/12/2020.
 */
object ResponseMapper {
    fun mapAccessToken(jsonObject: JSONObject): GetAccessTokenResponse {
        return GetAccessTokenResponse(
                jsonObject.optString("access_token"),
                jsonObject.optString("expires_in")
        )
    }
}