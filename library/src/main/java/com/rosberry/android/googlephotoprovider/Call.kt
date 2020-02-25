/*
 *
 *  * Copyright (c) 2019 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.googlephotoprovider

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

/**
 * @author Alexei Korshun on 2019-11-20.
 */
internal fun Call.enqueue(
        onResponse: (Response) -> Unit,
        onFailure: (Throwable) -> Unit
): Unit {
    this.enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            onFailure.invoke(e)
        }

        override fun onResponse(call: Call, response: Response) {
            onResponse.invoke(response)
        }
    })
}