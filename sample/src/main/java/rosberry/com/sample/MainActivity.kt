/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.photos.library.v1.proto.MediaTypeFilter
import com.rosberry.android.googlephotoprovider.CloudMedia
import com.rosberry.android.googlephotoprovider.CloudMediaProvider
import com.rosberry.android.googlephotoprovider.model.CloudMediaPage
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.a_main.*
import rosberry.com.sample.converter.MediaConverter
import rosberry.com.sample.entity.Media
import rosberry.com.sample.entity.MediaPage
import rosberry.com.sample.tools.CloudApiProvider.getCloudMediaApi
import rosberry.com.sample.ui.EndlessScrollListener
import rosberry.com.sample.ui.MediaAdapter
import rosberry.com.sample.ui.MediaItemDecorator

class MainActivity : AppCompatActivity(), GooglePaginator.ViewController<Media> {

    companion object {
        private const val signInRequestCode = 60000
        private const val SPAN_COUNT = 3
    }

    private val cloudMediaProvider by lazy {
        CloudMediaProvider(this, getCloudMediaApi(), BuildConfig.OAUTH_СLIENT_ID, BuildConfig.OAUTH_СLIENT_SECRET)
    }

    private val mediaConverter by lazy {
        MediaConverter()
    }

    private val mediaAdapter by lazy {
        val itemWidth = (resources.displayMetrics.widthPixels - padding) / SPAN_COUNT
        MediaAdapter(itemWidth)
    }

    private val googlePaginator by lazy {
        GooglePaginator({ limit, token ->
            getMediaPageFromCloudSingle(
                    MediaTypeFilter.MediaType.ALL_MEDIA,
                    limit = limit,
                    nextPageToken = token
            )
        }, this)
    }

    private val endlessScrollListener by lazy {
        object : EndlessScrollListener(mediaList.layoutManager!!) {
            override fun onLoadMore() {
                googlePaginator.loadNewPage()
            }
        }
    }

    private val padding by lazy {
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
            .toInt()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_main)

        mediaList.addOnScrollListener(endlessScrollListener)
        mediaList.adapter = mediaAdapter
        mediaList.addItemDecoration(MediaItemDecorator(padding, SPAN_COUNT))

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
        cloudMediaProvider.handleSignInResult(data)
            .handleSignIn()
    }

    private fun getMediaPageFromCloudSingle(
            filter: MediaTypeFilter.MediaType,
            limit: Int,
            nextPageToken: String?
    ): Single<MediaPage> = cloudMediaProvider.getCloudMediaPage(filter, limit, nextPageToken)
        .map { cloudMediaPage -> cloudMediaPage.toMediaPage() }

    private fun CloudMediaPage.toMediaPage(): MediaPage {
        return MediaPage(this.mediaList.fromCloudToMediaList(), this.nextPageToken)
    }

    private fun List<CloudMedia>.fromCloudToMediaList(): List<Media> {
        return mediaConverter.convertCloudMedia(this)
    }

    @SuppressLint("CheckResult")
    private fun Completable.handleSignIn() {
        this.observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    {
                        Toast.makeText(this@MainActivity, "Successfully signed in", Toast.LENGTH_SHORT)
                            .show()
                        googlePaginator.restart()
                    },
                    { throwable ->
                        Toast.makeText(this@MainActivity, "Sign in result error: ${throwable.message}",
                                Toast.LENGTH_SHORT)
                            .show()
                    }
            )
    }

    override fun showError(show: Boolean) {
        mediaMessage.text = getString(R.string.waiting)
        mediaMessage.visibility = View.VISIBLE
        mediaList.visibility = View.GONE
    }

    override fun showEmptyView(show: Boolean) {
        mediaMessage.text = getString(R.string.error)
        mediaMessage.visibility = if (show) View.VISIBLE else View.GONE
        mediaList.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun showData(data: List<Media>) {
        mediaAdapter.showData(data)
        mediaMessage.visibility = View.GONE
        mediaList.visibility = View.VISIBLE
    }

    override fun showPageProgress(show: Boolean) {
    }

}
