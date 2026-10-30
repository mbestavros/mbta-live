package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NetworkResilienceTest {

  private lateinit var context: Context
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: TransitRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    repository = TransitRepository(
      context = context,
      ioDispatcher = testDispatcher
    )
  }

  private fun createHttpException(code: Int, message: String, headers: Headers = Headers.headersOf()): HttpException {
    val errorBody = "{\"error\": \"$message\"}".toResponseBody("application/json".toMediaType())
    val response = Response.error<Any>(code, errorBody)
    val rawResponse = response.raw().newBuilder().headers(headers).build()
    val responseWithHeaders = Response.error<Any>(errorBody, rawResponse)
    return HttpException(responseWithHeaders)
  }

  @Test
  fun testExecuteWithRetrySucceedsOnFirstAttempt() = runTest(testDispatcher) {
    var callCount = 0
    val result = repository.executeWithRetry(maxAttempts = 3, initialDelayMs = 100) {
      callCount++
      "Success Data"
    }

    assertEquals(1, callCount)
    assertEquals("Success Data", result)
  }

  @Test
  fun testExecuteWithRetryRecoversFromTransientHttp500() = runTest(testDispatcher) {
    var callCount = 0
    val result = repository.executeWithRetry(maxAttempts = 3, initialDelayMs = 100) {
      callCount++
      if (callCount == 1) {
        throw createHttpException(500, "Internal Server Error")
      }
      "Recovered From 500"
    }

    assertEquals(2, callCount)
    assertEquals("Recovered From 500", result)
  }

  @Test
  fun testExecuteWithRetryRecoversFromTransientHttp429WithRetryAfter() = runTest(testDispatcher) {
    var callCount = 0
    val headers = Headers.headersOf("Retry-After", "2")
    val result = repository.executeWithRetry(maxAttempts = 2, initialDelayMs = 50) {
      callCount++
      if (callCount == 1) {
        throw createHttpException(429, "Too Many Requests", headers)
      }
      "Recovered From 429"
    }

    assertEquals(2, callCount)
    assertEquals("Recovered From 429", result)
  }

  @Test
  fun testExecuteWithRetryFailsWhenHttp503ExceedsMaxAttempts() = runTest(testDispatcher) {
    var callCount = 0
    try {
      repository.executeWithRetry(maxAttempts = 2, initialDelayMs = 50) {
        callCount++
        throw createHttpException(503, "Service Unavailable")
      }
      fail("Expected HttpException 503 to be thrown")
    } catch (e: HttpException) {
      assertEquals(503, e.code())
      assertEquals(2, callCount)
    }
  }

  @Test
  fun testExecuteWithRetryDoesNotRetryNonRetryableHttp404() = runTest(testDispatcher) {
    var callCount = 0
    try {
      repository.executeWithRetry(maxAttempts = 3, initialDelayMs = 100) {
        callCount++
        throw createHttpException(404, "Not Found")
      }
      fail("Expected HttpException 404 to be thrown immediately")
    } catch (e: HttpException) {
      assertEquals(404, e.code())
      // Non-retryable error must fail fast on attempt 1
      assertEquals(1, callCount)
    }
  }

  @Test
  fun testExecuteWithRetryNeverSwallowsCancellationException() = runTest(testDispatcher) {
    var callCount = 0
    try {
      repository.executeWithRetry(maxAttempts = 3, initialDelayMs = 100) {
        callCount++
        throw CancellationException("Coroutine Cancelled")
      }
      fail("Expected CancellationException to be rethrown immediately")
    } catch (e: CancellationException) {
      assertEquals(1, callCount)
      assertEquals("Coroutine Cancelled", e.message)
    }
  }

  @Test
  fun testTransitResultRateLimitedModelCarriesRetryAfterSeconds() {
    val rateLimitResult = TransitResult.Error(
      message = "MBTA API rate limit reached",
      isRateLimited = true,
      retryAfterSeconds = 15L
    )

    assertTrue(rateLimitResult.isRateLimited)
    assertEquals(15L, rateLimitResult.retryAfterSeconds)
    assertEquals("MBTA API rate limit reached", rateLimitResult.message)

    val regularError = TransitResult.Error(
      message = "Network Timeout",
      isRateLimited = false
    )
    assertNull(regularError.retryAfterSeconds)
  }
}
