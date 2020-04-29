/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample

import com.google.gson.GsonBuilder
import com.rosberry.android.googlephotoprovider.CloudMediaApi
import io.reactivex.schedulers.Schedulers
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author Evgeniy Nagibin on 28/04/2020.
 */
object CloudApiProvider {

    private val timeoutTime = 60L
    private val baseUrl = "https://www.googleapis.com/oauth2/v4/"

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .apply {
                connectTimeout(timeoutTime, TimeUnit.SECONDS)
                readTimeout(timeoutTime, TimeUnit.SECONDS)
            }
            .build()
    }

    private val gson by lazy {
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
    }

    fun getCloudMediaApi(): CloudMediaApi {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .client(okHttpClient)
            .baseUrl(baseUrl)
            .build()
            .create(CloudMediaApi::class.java)
    }
}