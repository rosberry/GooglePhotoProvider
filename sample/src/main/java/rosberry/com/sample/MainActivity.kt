/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.rosberry.android.googlephotoprovider.CloudMediaApi
import com.rosberry.android.googlephotoprovider.CloudMediaProvider

class MainActivity : AppCompatActivity() {

    private val cloudMediaProvider by lazy {
        CloudMediaProvider(this, , "", "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_main)


    }
}
