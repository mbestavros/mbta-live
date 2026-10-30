package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransitRepositoryConcurrencyTest {

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

  @Test
  fun testConcurrentGetRoutesExecutesSafelyWithMutex() = runTest(testDispatcher) {
    // Launch 10 concurrent requests to getRoutes on a cold repository
    val deferredResults = (1..10).map {
      async {
        repository.getRoutes(forceRefresh = false)
      }
    }

    val allResults = deferredResults.awaitAll()
    assertEquals(10, allResults.size)

    // All results must be identical and valid
    val firstResult = allResults[0]
    assertTrue(firstResult.isNotEmpty())
    allResults.forEach { result ->
      assertEquals(firstResult.size, result.size)
      assertEquals(firstResult.map { it.id }, result.map { it.id })
    }
  }

  @Test
  fun testOfflineFallbackRoutesAreCached() = runTest(testDispatcher) {
    val routes = repository.getOfflineFallbackRoutes()
    assertTrue(routes.isNotEmpty())

    val redLine = routes.find { it.id == "Red" }
    assertNotNull(redLine)
    assertEquals("Red Line", redLine!!.shortName)

    val orangeLine = routes.find { it.id == "Orange" }
    assertNotNull(orangeLine)
    assertEquals("Orange Line", orangeLine!!.shortName)
  }
}
