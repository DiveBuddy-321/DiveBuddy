package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.BuildConfig
import com.cpen321.usermanagement.data.remote.interceptors.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.cpen321.usermanagement.common.Constants

object RetrofitClient {
    private const val BASE_URL = BuildConfig.API_BASE_URL
    private const val IMAGE_BASE_URL = BuildConfig.IMAGE_BASE_URL

    private var authToken: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = AuthInterceptor { authToken }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(Constants.NETWORK_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(Constants.NETWORK_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .writeTimeout(Constants.NETWORK_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authInterface: AuthInterface = retrofit.create(AuthInterface::class.java)
    val imageInterface: ImageInterface = retrofit.create(ImageInterface::class.java)
    val chatInterface: ChatInterface = retrofit.create(ChatInterface::class.java)
    val userInterface: UserInterface = retrofit.create(UserInterface::class.java)
    val buddyInterface: BuddyInterface = retrofit.create(BuddyInterface::class.java)
    val eventInterface: EventInterface = retrofit.create(EventInterface::class.java)
    val blockInterface: BlockInterface = retrofit.create(BlockInterface::class.java)

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun getPictureUri(picturePath: String): String {
        return if (picturePath.startsWith("uploads/")) {
            IMAGE_BASE_URL + picturePath
        } else {
            picturePath
        }
    }
}