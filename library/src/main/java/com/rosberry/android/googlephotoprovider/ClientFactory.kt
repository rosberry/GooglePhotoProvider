/*
 *
 *  * Copyright (c) 2019 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.googlephotoprovider

import okhttp3.OkHttpClient

/**
 * @author Alexei Korshun on 2019-11-20.
 */
class ClientFactory {

    private val emptyClient: OkHttpClient by lazy { OkHttpClient() }

    fun client(progressListener: ProgressListener?): OkHttpClient {
        return progressListener?.buildClient() ?: emptyClient
    }

    private fun ProgressListener.buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .also { clientBuilder ->
                clientBuilder.addNetworkInterceptor { chain ->
                    val originalResponse = chain.proceed(chain.request())
                    originalResponse.newBuilder()
                        .body(ProgressResponseBody(originalResponse.body(), this))
                        .build()
                }
            }
            .build()
    }
}