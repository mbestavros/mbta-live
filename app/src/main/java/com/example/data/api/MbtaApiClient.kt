package com.example.data.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe client for the MBTA V3 API.
 * Uses atomic headers for dynamic API key updates without connection pool churn,
 * caching headers for static route and stop data, and non-blocking architecture.
 */
object MbtaApiClient {
  private const val BASE_URL = "https://api-v3.mbta.com/"
  private const val CACHE_SIZE = 15L * 1024 * 1024 // 15 MB

  private val apiKeyRef = AtomicReference<String?>(null)

  var customApiKey: String?
    get() = apiKeyRef.get()
    set(value) {
      apiKeyRef.set(value)
    }

  @Volatile
  private var okHttpCache: Cache? = null

  private val authInterceptor = Interceptor { chain ->
    val original = chain.request()
    val builder = original.newBuilder()
      .header("Accept", "application/vnd.api+json")
      .header("User-Agent", "MbtaLive-AndroidApp/1.0")

    val key = apiKeyRef.get()
    if (!key.isNullOrBlank()) {
      builder.header("x-api-key", key)
    }

    chain.proceed(builder.build())
  }

  // Network cache interceptor: add Cache-Control max-age to cacheable MBTA catalog endpoints (routes, stops)
  private val cacheRewriteInterceptor = Interceptor { chain ->
    val request = chain.request()
    val response = chain.proceed(request)
    val path = request.url.encodedPath

    // If successfully returned routes or stops, cache on disk for 5 minutes
    if (response.isSuccessful && (path.contains("/routes") || path.contains("/stops"))) {
      response.newBuilder()
        .header("Cache-Control", "public, max-age=300")
        .build()
    } else {
      response
    }
  }

  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BASIC
  }

  val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  @Volatile
  private var currentClient: OkHttpClient = buildClient(null)

  @Volatile
  private var currentApiService: MbtaApiService = buildApiService(currentClient)

  val apiService: MbtaApiService
    get() = currentApiService

  fun init(context: Context) {
    if (okHttpCache == null) {
      synchronized(this) {
        if (okHttpCache == null) {
          try {
            val cacheDir = File(context.cacheDir, "mbta_http_cache")
            val newCache = Cache(cacheDir, CACHE_SIZE)
            okHttpCache = newCache
            val client = buildClient(newCache)
            currentClient = client
            currentApiService = buildApiService(client)
          } catch (_: Exception) {
            // Gracefully ignore file cache setup errors if storage restricted
          }
        }
      }
    }
  }

  /**
   * Helper to construct a test OkHttpClient instance without modifying global state.
   */
  fun createTestClient(customInterceptor: Interceptor? = null): OkHttpClient {
    val builder = OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(5, TimeUnit.SECONDS)

    if (customInterceptor != null) {
      builder.addInterceptor(customInterceptor)
    }
    return builder.build()
  }

  private fun buildClient(cache: Cache?): OkHttpClient {
    val builder = OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .addNetworkInterceptor(cacheRewriteInterceptor)
      .addInterceptor(loggingInterceptor)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)

    if (cache != null) {
      builder.cache(cache)
    }

    return builder.build()
  }

  private fun buildApiService(client: OkHttpClient): MbtaApiService {
    return Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(MbtaApiService::class.java)
  }
}
