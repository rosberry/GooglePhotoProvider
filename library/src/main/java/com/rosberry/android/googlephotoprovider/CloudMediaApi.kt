/*
 *
 *  * Copyright (c) 2018 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.googlephotoprovider

import com.rosberry.android.googlephotoprovider.response.GetAccessTokenResponse
import io.reactivex.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * @author Evgeniy Nagibin on 2019-06-18.
 */
//todo migrate to HttpUrlConnection
interface CloudMediaApi {

    @POST("token")
    @FormUrlEncoded
    fun accessToken(
              @Field("client_id") clientId: String,
              @Field("client_secret") clientSecret: String,
              @Field("code") authCode: String,
              @Field("grant_type") grantType: String = "authorization_code"
    ): Single<GetAccessTokenResponse>
}