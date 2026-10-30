package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.graph.TransitGraphPathfinder
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
class TransitRepositoryTest {

  private lateinit var repository: TransitRepository
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext<Context>()
    repository = TransitRepository(context)
  }

  @Test
  fun testOfflineFallbackRoutesContainsCoreLines() {
    val routes = repository.getOfflineFallbackRoutes()
    assertTrue(routes.isNotEmpty())

    val redLine = routes.find { it.id == "Red" }
    assertNotNull(redLine)
    assertEquals("Red Line", redLine!!.shortName)
    assertEquals("DA291C", redLine.colorHex)

    val orangeLine = routes.find { it.id == "Orange" }
    assertNotNull(orangeLine)
    assertEquals("ED8B00", orangeLine!!.colorHex)

    val blueLine = routes.find { it.id == "Blue" }
    assertNotNull(blueLine)
    assertEquals("003DA5", blueLine!!.colorHex)
  }

  @Test
  fun testPathfinderGeneratesValidItinerary() {
    // Park Street: 42.356395, -71.062424
    // Harvard Square: 42.373362, -71.118956
    val itinerary = repository.planItinerary(
      fromLat = 42.356395,
      fromLng = -71.062424,
      toLat = 42.373362,
      toLng = -71.118956,
      fromLabel = "Park Street",
      toLabel = "Harvard Square"
    )

    assertNotNull(itinerary)
    assertTrue(itinerary.totalDurationMinutes > 0)
    assertTrue(itinerary.legs.isNotEmpty())
    assertEquals("Park Street", itinerary.fromName)
    assertEquals("Harvard Square", itinerary.toName)
  }

  @Test
  fun testCalculateDistanceMiles() {
    // Distance between Boston Common and Harvard Square is ~3.1 miles
    val dist = TransitGraphPathfinder.calculateDistanceMiles(
      42.356395, -71.062424,
      42.373362, -71.118956
    )
    assertTrue(dist in 2.5..4.0)
  }

  @Test
  fun testSeedStopsIfEmptySeedsCachedStops() = runTest {
    repository.seedStopsIfEmpty()
    val stops = repository.searchCachedStops("Park", limit = 5)
    // Seed stops asset contains Park Street
    val park = stops.find { it.name.contains("Park Street", ignoreCase = true) }
    assertNotNull(park)
  }
}
