package com.gokova.myanimelist.core.network.auth

import com.gokova.myanimelist.core.datastore.AuthPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor
    @Inject
    constructor(
        private val authPreferences: AuthPreferences,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()

            // Skip authorization for oauth2 endpoint
            if (request.url.encodedPath.contains("/oauth2/")) {
                return chain.proceed(request)
            }

            val accessToken = authPreferences.getAccessTokenSync()

            val newRequest =
                if (accessToken != null) {
                    request
                        .newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                } else {
                    request
                }

            return chain.proceed(newRequest)
        }
    }
