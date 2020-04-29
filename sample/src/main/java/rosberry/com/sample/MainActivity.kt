/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample

import android.app.Activity
import android.app.ProgressDialog.show
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.rosberry.android.googlephotoprovider.CloudMediaApi
import com.rosberry.android.googlephotoprovider.CloudMediaProvider
import com.rosberry.android.googlephotoprovider.exception.SignInError
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import rosberry.com.sample.CloudApiProvider.getCloudMediaApi

class MainActivity : AppCompatActivity() {

    companion object {
        private const val signInRequestCode = 60000
    }

    private val cloudMediaProvider by lazy {
        CloudMediaProvider(this, getCloudMediaApi(), BuildConfig.OAUTH_СLIENT_ID, BuildConfig.OAUTH_СLIENT_SECRET)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_main)

        cloudMediaProvider.checkAuthorization(
                onSignInRequired = { signInIntent ->
                    signInGoogle(signInIntent)
                },
                onSilentSignIn = { result ->
                    cloudMediaProvider.handleSignInResult(result)
                        .handleSignIn()
                },
                onAlreadySignedIn = {
                },
                onConnectionError = {
                }
        )
    }

    private fun signInGoogle(signInIntent: Intent) {
        startActivityForResult(signInIntent, signInRequestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && requestCode == signInRequestCode) {
            if (resultCode != Activity.RESULT_CANCELED) {
                onSignInGoogleResult(data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onSignInGoogleResult(data: Intent) {
        cloudMediaProvider.handleSignInResult(data).handleSignIn()
    }

    private fun Completable.handleSignIn() {
        val result = this.observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    {
                        Toast.makeText(this@MainActivity, "Successfully signed in", Toast.LENGTH_SHORT).show()
                    },
                    { throwable ->
                        Toast.makeText(this@MainActivity, "Sign in result error: ${throwable.message}", Toast.LENGTH_SHORT).show()
                    }
            )
    }

}
