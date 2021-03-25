# GooglePhotoProvider

GooglePhotoProvider is a library allows you to load all your Google Photos via Google Photos API. It wraps the loading photos process into a simple and easy to use interface.
## Features

- Helps you to download and manage all your Google Photos
- Allows you to download Google Photos page by page
- Google Sign-in is also integrated into this library
- The great sample guides you to integrate this library into your project step by step
- 
## Requirements

- Java 8+
- Android API 21+
- 
## Setup

At first, you have to configure your project by enabling the API via the Google API Console and setting up an OAuth 2.0 client ID. You can follow this [instructions](https://developers.google.com/photos/library/guides/get-started-java?hl=nl) for setting up your project.

Also, for using GooglePhotoProvider you have to add the dependency to Google Photos Java client library:
```gradle
dependencies {
     implementation 'com.google.photos.library:google-photos-library-client:ххх'
}
```

For managing Google Photos loading and caching you can use **CloudMediaProvider** class. It requires *OAUTH_СLIENT_ID* and *OAUTH_СLIENT_SECRET* as parameters. Also, you must provide an implementation of **CloudMediaApi** interface.

Before querying any Google Photos request you should sign in user to their Google account. By calling **checkAuthorization()** method you're able to retrieve the current accessToken for making any request to Google Photos API. There are four options available after receiving the authorization result.

  - *onSignInRequired* - user has never signed in before or has signed out / revoked access. You have to provide your own flow for getting current user account, check out our sample app for that.
  - *onSilentSignIn* - user has previously signed in, but the current *accessToken* is null or expired.
  - *onAlreadySignedIn()* - everything is good, you're able to make a request to Google Photos API
  - *onConnectionError()* - a connection error occurred

# Getting user photos
For retrieving user photos from Google Photos API you're able to use **getCloudMediaPage()** method from **CloudMediaProvider** class. As users might have a lot of photos in their gallery, it is highly recommended to use the pagination for getting photos from Google Photos API. You can pass any value to *limit* parameter that suits your needs. In the sample app, you can see our implementation of Google Paging library based on [this one](https://gitlab.com/terrakok/gitlab-client/blob/98167db3555012396e03027fb12b94b8ad8c923c/app/src/main/java/ru/terrakok/gitlabclient/presentation/global/Paginator.kt).

## About

<img src="https://github.com/rosberry/Foundation/blob/master/Assets/full_logo.png?raw=true" height="100" />

This project is owned and maintained by [Rosberry](http://rosberry.com). We build mobile apps for users worldwide 🌏.

Check out our [open source projects](https://github.com/rosberry), read [our blog](https://medium.com/@Rosberry) or give us a high-five on 🐦 [@rosberryapps](http://twitter.com/RosberryApps).

## License

**GooglePhotoProvider** is available under the MIT license. See the LICENSE file for more info.
