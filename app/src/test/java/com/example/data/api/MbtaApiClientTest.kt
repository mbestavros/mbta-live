package com.example.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MbtaApiClientTest {

  @Before
  fun setUp() {
    MbtaApiClient.customApiKey = null
  }

  @After
  fun tearDown() {
    MbtaApiClient.customApiKey = null
  }

  @Test
  fun `test dynamic API key is correctly injected into request headers`() {
    var capturedHeaderValue: String? = null

    // Interceptor to inspect request
    val mockNetworkInterceptor = Interceptor { chain ->
      val req = chain.request()
      capturedHeaderValue = req.header("x-api-key")
      Response.Builder()
        .request(req)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("{}".toResponseBody("application/json".toMediaType()))
        .build()
    }

    val client = MbtaApiClient.createTestClient(mockNetworkInterceptor)

    // 1. Without custom API key
    val req1 = Request.Builder().url("https://api-v3.mbta.com/routes").build()
    client.newCall(req1).execute().close()
    assertNull(capturedHeaderValue)

    // 2. Set dynamic custom API key
    MbtaApiClient.customApiKey = "TEST-API-KEY-12345"
    val req2 = Request.Builder().url("https://api-v3.mbta.com/routes").build()
    client.newCall(req2).execute().close()
    assertEquals("TEST-API-KEY-12345", capturedHeaderValue)

    // 3. Reset API key
    MbtaApiClient.customApiKey = null
    val req3 = Request.Builder().url("https://api-v3.mbta.com/routes").build()
    client.newCall(req3).execute().close()
    assertNull(capturedHeaderValue)
  }

  @Test
  fun `test default Accept and User-Agent headers are always injected`() {
    var acceptHeader: String? = null
    var userAgentHeader: String? = null

    val mockNetworkInterceptor = Interceptor { chain ->
      val req = chain.request()
      acceptHeader = req.header("Accept")
      userAgentHeader = req.header("User-Agent")
      Response.Builder()
        .request(req)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("{}".toResponseBody("application/json".toMediaType()))
        .build()
    }

    val client = MbtaApiClient.createTestClient(mockNetworkInterceptor)
    val req = Request.Builder().url("https://api-v3.mbta.com/routes").build()
    client.newCall(req).execute().close()

    assertEquals("application/vnd.api+json", acceptHeader)
    assertEquals("MbtaLive-AndroidApp/1.0", userAgentHeader)
  }
}
