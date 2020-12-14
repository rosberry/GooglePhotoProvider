/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.googlephotoprovider

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.Credentials
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.UserCredentials
import com.google.photos.library.v1.PhotosLibraryClient
import com.google.photos.library.v1.PhotosLibrarySettings
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient
import com.google.photos.library.v1.proto.BatchGetMediaItemsResponse
import com.google.photos.library.v1.proto.Filters
import com.google.photos.library.v1.proto.MediaTypeFilter
import com.google.photos.library.v1.proto.SearchMediaItemsRequest
import com.google.photos.types.proto.MediaItem
import com.google.photos.types.proto.VideoProcessingStatus
import com.rosberry.android.googlephotoprovider.exception.PhotoLibraryCreationError
import com.rosberry.android.googlephotoprovider.exception.SignInError
import com.rosberry.android.googlephotoprovider.exception.TokenInvalidError
import com.rosberry.android.googlephotoprovider.model.CloudMediaPage
import com.rosberry.android.googlephotoprovider.network.HttpHandler
import com.rosberry.android.googlephotoprovider.network.HttpHandler.fileName
import com.rosberry.android.googlephotoprovider.response.GetAccessTokenResponse
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.Single
import io.reactivex.SingleEmitter
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

typealias CloudMedia = MediaItem

/**
 * @author mmikhailov on 2019-11-01.
 */
class CloudMediaProvider(
        private val context: Context,
        private val clientId: String,
        private val clientSecret: String
) {

    private val tag = "GooglePhotoProvider"
    private val cache = Cache(context)
    private val fallbackExpirationTimeMs: Long = 60 * 1000

    private var photosClient: PhotosLibraryClient? = null
    private var accessToken: String? = null
    private var tokenExpiresAt: Long? = null
    private var lastSearchMediaItemsReq: SearchMediaItemsRequest? = null

    fun getCloudMediaPage(
            filterMode: MediaTypeFilter.MediaType = MediaTypeFilter.MediaType.ALL_MEDIA,
            limit: Int,
            startPageToken: String?
    ): Single<CloudMediaPage> {
        return if (accessToken != null && isTokenExpired().not()) {
            loadCloudPage(filterMode, limit, startPageToken)
        } else if (GoogleSignIn.getLastSignedInAccount(context) != null) {
            silentSignInAndLoadPage(filterMode, limit, startPageToken)
        } else {
            Single.error(TokenInvalidError())
        }
    }

    fun getCloudMediaListByIds(ids: List<String>) =
            Single.create<List<CloudMedia>> { emitter ->
                if (accessToken == null || isTokenExpired()) {
                    emitter.onError(TokenInvalidError())
                } else {
                    val response = photosLibraryClient(accessToken!!).batchGetMediaItems(
                            ids
                    )
                    emitter.onSuccess(response.toCloudMediaList())
                }
            }

    fun handleSignInResult(signInData: Intent) = Completable.defer {
        val result: GoogleSignInResult? = Auth.GoogleSignInApi.getSignInResultFromIntent(
                signInData
        )
        if (result?.isSuccess == true) {
            return@defer handleSignInResult(result.signInAccount!!.serverAuthCode!!)
        } else {
            val message = result?.status?.statusCode?.let {
                CommonStatusCodes.getStatusCodeString(it)
            } ?: ""
            return@defer Completable.error(SignInError(message))
        }
    }

    fun handleSignInResult(authCode: String): Completable {
        return Completable.create { emitter ->
            val params = hashMapOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "code" to authCode,
                    "grant_type" to "authorization_code"
            )

            HttpHandler.post(
                    endpoint = "token",
                    params = params,
                    mapper = { jsonObject ->
                        jsonObject.mapAccessToken()
                    },
                    success = { accessTokenResponse: GetAccessTokenResponse ->
                        this@CloudMediaProvider.accessToken = accessTokenResponse.accessToken
                        this@CloudMediaProvider.tokenExpiresAt = accessTokenResponse.expires.calculateTokenExpiration()
                    },
                    error = { t -> emitter.passEvent { emitter.onError(t) } },
                    complete = { emitter.passEvent { emitter.onComplete() } }
            )
        }
    }

    fun checkAuthorization(
            onSignInRequired: (Intent) -> Unit,
            onSilentSignIn: (String) -> Unit,
            onAlreadySignedIn: () -> Unit,
            onConnectionError: () -> Unit
    ) {
        if (accessToken != null && isTokenExpired().not()) {
            onAlreadySignedIn()
        } else if (GoogleSignIn.getLastSignedInAccount(context) != null) {
            silentSignIn(onSilentSignIn, onConnectionError)
        } else {
            photosClient?.shutdown()
            photosClient?.awaitTermination(3, TimeUnit.SECONDS)
            photosClient = null
            accessToken = null

            val intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient())
            onSignInRequired(intent)
        }
    }

    /**
     * Returns a Single, which emits the local uri of the downloaded file.
     * If the downloaded file exists in cache storage, method returns the uri of the cached file.
     */
    fun downloadMedia(
            mediaId: String,
            uri: Uri,
            progressListener: ProgressListener? = null
    ): Single<Uri> {
        return Single.create<Uri> { emitter ->
            cache.get(mediaId)
                ?.run { emitter.onSuccess(this) }
                ?: downloadFile(mediaId, uri, progressListener,
                        success = { response ->
                            val uri = cache.put(
                                    mediaId,
                                    response.contentType,
                                    response.bytes.toByteArray()
                            )
                            emitter.passEvent {
                                emitter.onSuccess(uri)
                            }
                        },
                        error = { throwable ->
                            emitter.passEvent { emitter.onError(throwable) }
                        })

        }
    }

    private fun downloadFile(
            mediaId: String,
            uri: Uri,
            progressListener: ProgressListener?,
            success: (HttpHandler.ResponseModel) -> Unit,
            error: (Throwable) -> Unit
    ) {
        HttpHandler.downloadFile(
                uri,
                mediaId.fileName(),
                progressListener,
                success = { response ->
                    success.invoke(response)
                },
                error = { throwable ->
                    error.invoke(throwable)
                }
        )
    }

    private fun silentSignIn(
            onSilentSignIn: (String) -> Unit,
            onConnectionError: () -> Unit
    ) {
        val client = googleApiClient()
        client.registerConnectionCallbacks(object :
                GoogleApiClient.ConnectionCallbacks {

            override fun onConnected(p0: Bundle?) {
                val googleSignInResult = Auth.GoogleSignInApi.silentSignIn(
                        client
                )
                if (googleSignInResult.isDone) {
                    checkSilentSignInResult(
                            googleSignInResult.get(),
                            onSilentSignIn,
                            onConnectionError,
                            client
                    )
                } else {
                    googleSignInResult.setResultCallback { result ->
                        checkSilentSignInResult(
                                result,
                                onSilentSignIn,
                                onConnectionError,
                                client
                        )
                    }
                }

                client.unregisterConnectionCallbacks(this)
            }

            override fun onConnectionSuspended(p0: Int) {
                client.unregisterConnectionCallbacks(this)
            }
        })
        client.connect()
    }

    private fun checkSilentSignInResult(
            result: GoogleSignInResult,
            onSilentSignIn: (String) -> Unit,
            onConnectionError: () -> Unit,
            client: GoogleApiClient
    ) {
        val authCode = result.signInAccount?.serverAuthCode
        if (authCode != null) {
            onSilentSignIn.invoke(authCode)
        } else {
            if (result.status.statusCode != CommonStatusCodes.NETWORK_ERROR) {
                Auth.GoogleSignInApi.signOut(client)
            }
            onConnectionError.invoke()
        }
    }

    private fun googleApiClient(): GoogleApiClient {

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/photoslibrary.readonly"))
            .requestServerAuthCode(clientId)
            .requestEmail()
            .build()

        return GoogleApiClient.Builder(context)
            .addApi(Auth.GOOGLE_SIGN_IN_API, options)
            .build()
    }

    private fun photosLibraryClient(token: String): PhotosLibraryClient {
        if (photosClient?.isTerminated == false) {
            Log.d(tag, "photosLibraryClient::photo client is good")
            return photosClient!!
        }

        if (photosClient?.isShutdown == true) {
            Log.d(tag, "photosLibraryClient::photo client is shutting down now")
            photosClient!!.awaitTermination(3, TimeUnit.SECONDS)
            photosClient = null
            Log.d(tag, "photosLibraryClient::photo client was released")
        }

        val settings: PhotosLibrarySettings

        try {
            settings = PhotosLibrarySettings.newBuilder()
                .setCredentialsProvider(
                        FixedCredentialsProvider.create(
                                getUserCredentials(token)
                        )
                )
                .build()
        } catch (e: IOException) {
            throw PhotoLibraryCreationError(e)
        }

        return try {
            PhotosLibraryClient.initialize(settings)
                .also {
                    photosClient = it
                    Log.d(
                            tag,
                            "photosLibraryClient::photo client was initialized"
                    )
                }
        } catch (e: IOException) {
            throw PhotoLibraryCreationError(e)
        }
    }

    private fun getUserCredentials(token: String): Credentials {
        val accessToken = AccessToken(token, null)
        return UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setAccessToken(accessToken)
            .build()
    }

    private fun isTokenExpired(): Boolean =
            tokenExpiresAt == null || tokenExpiresAt!! < SystemClock.elapsedRealtime()

    private fun loadCloudPage(
            filterMode: MediaTypeFilter.MediaType,
            limit: Int,
            startPageToken: String?
    ): Single<CloudMediaPage> {
        return Single.create<CloudMediaPage> { emitter ->
            val mediaTypeFilter = MediaTypeFilter.newBuilder()
                .addMediaTypes(filterMode)
                .build()

            val filter = Filters.newBuilder()
                .setMediaTypeFilter(mediaTypeFilter)
                .build()

            val cloudMediaList = mutableListOf<CloudMedia>()
            var nextPageToken = if (isSearchMediaItemsRequestValid(limit)) startPageToken
                ?: ""
            else ""

            Log.d(tag, "loadCloudMedia::query with token: $nextPageToken")

            do {
                val listMediaReq = SearchMediaItemsRequest.newBuilder()
                    .setPageSize(limit)
                    .setFilters(filter)
                    .setPageToken(nextPageToken)
                    .build()

                this.lastSearchMediaItemsReq = listMediaReq

                val mediaItemsPagedResponse = photosLibraryClient(accessToken!!)
                    .searchMediaItems(listMediaReq)

                cloudMediaList.addAll(mediaItemsPagedResponse.toCloudMediaList())
                nextPageToken = mediaItemsPagedResponse.nextPageToken
            } while (cloudMediaList.size < limit && nextPageToken.isNotBlank())

            emitter.passEvent {
                emitter.onSuccess(CloudMediaPage(cloudMediaList, nextPageToken))
            }
        }
    }

    private fun silentSignInAndLoadPage(
            filterMode: MediaTypeFilter.MediaType,
            limit: Int,
            startPageToken: String?
    ): Single<CloudMediaPage> {
        return Single.create<String> { emitter ->
            silentSignIn(
                    { authCode -> emitter.onSuccess(authCode) },
                    { emitter.onError(Exception()) }
            )
        }
            .flatMapCompletable { authCode -> handleSignInResult(authCode) }
            .andThen(loadCloudPage(filterMode, limit, startPageToken))
    }

    private fun String.calculateTokenExpiration(): Long =
            SystemClock.elapsedRealtime() + this.castToMs()

    private fun String.castToMs(): Long =
            this.toLongOrNull()?.times(1000) ?: fallbackExpirationTimeMs

    private fun isSearchMediaItemsRequestValid(queriedSize: Int) =
            lastSearchMediaItemsReq?.pageSize == queriedSize

    private fun CloudMedia.isValidItem(): Boolean {
        return this.mediaMetadata.hasVideo()
                && this.mediaMetadata.video.status == VideoProcessingStatus.READY
                || this.mediaMetadata.hasPhoto()
    }

    private fun InternalPhotosLibraryClient.SearchMediaItemsPagedResponse.toCloudMediaList(): List<CloudMedia> {
        return page.values.filter { mediaItem -> mediaItem.isValidItem() }
    }

    private fun BatchGetMediaItemsResponse.toCloudMediaList(): List<CloudMedia> {
        return mediaItemResultsList.map { it.mediaItem }
            .filter { mediaItem -> mediaItem != MediaItem.getDefaultInstance() && mediaItem.isValidItem() }
    }

    fun JSONObject.mapAccessToken(): GetAccessTokenResponse {
        return GetAccessTokenResponse(
                optString("access_token"),
                optString("expires_in")
        )
    }

    private fun CompletableEmitter.passEvent(action: () -> Unit) {
        if (!this.isDisposed) {
            action.invoke()
        }
    }

    private fun <T> SingleEmitter<T>.passEvent(action: () -> Unit) {
        if (!this.isDisposed) {
            action.invoke()
        }
    }
}
