package com.example

import com.example.data.graph.BostonLandmarks
import com.example.data.graph.TransitGraphPathfinder
import com.example.data.location.GeocoderHelper
import com.example.data.model.LegType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun `pathfinder finds direct Red Line route from Harvard to South Station`() {
    val itinerary = TransitGraphPathfinder.findShortestPath(
      originLat = BostonLandmarks.HARVARD.latitude,
      originLng = BostonLandmarks.HARVARD.longitude,
      destLat = BostonLandmarks.SOUTH_STATION.latitude,
      destLng = BostonLandmarks.SOUTH_STATION.longitude,
      fromLabel = "Harvard",
      toLabel = "South Station"
    )

    assertNotNull(itinerary)
    assertTrue(itinerary.totalDurationMinutes > 0)
    val transitLegs = itinerary.legs.filter { it.legType == LegType.TRANSIT }
    assertTrue("Should have at least 1 transit leg", transitLegs.isNotEmpty())
    assertEquals("Red", transitLegs.first().routeId)
  }

  @Test
  fun `pathfinder finds multi-leg route with transfer from Harvard to North Station`() {
    val itinerary = TransitGraphPathfinder.findShortestPath(
      originLat = BostonLandmarks.HARVARD.latitude,
      originLng = BostonLandmarks.HARVARD.longitude,
      destLat = BostonLandmarks.NORTH_STATION.latitude,
      destLng = BostonLandmarks.NORTH_STATION.longitude,
      fromLabel = "Harvard",
      toLabel = "North Station"
    )

    assertNotNull(itinerary)
    assertTrue("Multi-line trip should have transfers", itinerary.transferCount >= 1)
  }

  @Test
  fun `closest station resolves correctly`() {
    val closest = TransitGraphPathfinder.getClosestStation(
      BostonLandmarks.PARK_STREET.latitude + 0.0001,
      BostonLandmarks.PARK_STREET.longitude + 0.0001
    )
    assertEquals("place-pktrm", closest.id)
  }

  @Test
  fun `pathfinder finds route to destination station`() {
    val destStop = BostonLandmarks.KENDALL_MIT
    val itinerary = TransitGraphPathfinder.findShortestPath(
      originLat = BostonLandmarks.PARK_STREET.latitude,
      originLng = BostonLandmarks.PARK_STREET.longitude,
      destLat = destStop.latitude,
      destLng = destStop.longitude,
      fromLabel = "Park Street",
      toLabel = destStop.name
    )
    assertNotNull(itinerary)
    assertTrue(itinerary.legs.isNotEmpty())
    val transitLegs = itinerary.legs.filter { it.legType == LegType.TRANSIT }
    assertTrue("Should have at least 1 transit leg", transitLegs.isNotEmpty())
    assertEquals("Red", transitLegs.first().routeId)
  }

  @Test
  fun `neighborhood label resolves correctly`() {
    val label = com.example.data.location.LocationHelper.getNeighborhoodLabel(
      BostonLandmarks.HARVARD.latitude,
      BostonLandmarks.HARVARD.longitude
    )
    assertTrue("Should indicate Harvard", label.contains("Harvard", ignoreCase = true))
  }

  @Test
  fun `moshi deserializes StopResource with null latitude without throwing exception`() {
    val json = """
      {
        "id": "place-parent-node",
        "type": "stop",
        "attributes": {
          "name": "Generic Terminal Pathway",
          "latitude": null,
          "longitude": null,
          "municipality": "Boston",
          "location_type": 3,
          "wheelchair_boarding": 1
        }
      }
    """.trimIndent()

    val moshi = com.squareup.moshi.Moshi.Builder()
      .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
      .build()

    val adapter = moshi.adapter(com.example.data.api.StopResource::class.java)
    val stopResource = adapter.fromJson(json)

    assertNotNull("StopResource should deserialize successfully", stopResource)
    assertEquals("place-parent-node", stopResource?.id)
    assertEquals("Generic Terminal Pathway", stopResource?.attributes?.name)
    org.junit.Assert.assertNull("Latitude should be null without throwing", stopResource?.attributes?.latitude)
    org.junit.Assert.assertNull("Longitude should be null without throwing", stopResource?.attributes?.longitude)
  }

  @Test
  fun `offline stops seed format parses valid coordinates`() {
    val json = """
      [
        {"stopId": "place-pktrm", "name": "Park Street", "latitude": 42.356395, "longitude": -71.062424, "municipality": "Boston", "locationType": 1, "wheelchairBoarding": 1},
        {"stopId": "place-sstat", "name": "South Station", "latitude": 42.352271, "longitude": -71.055242, "municipality": "Boston", "locationType": 1, "wheelchairBoarding": 1}
      ]
    """.trimIndent()

    val moshi = com.squareup.moshi.Moshi.Builder()
      .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
      .build()
    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.api.SeedStopItem::class.java)
    val adapter = moshi.adapter<List<com.example.data.api.SeedStopItem>>(type)
    val list = adapter.fromJson(json)

    assertNotNull(list)
    assertEquals(2, list?.size)

    val first = list?.get(0)
    assertEquals("place-pktrm", first?.stopId)
    assertEquals("Park Street", first?.name)
    assertTrue((first?.latitude ?: 0.0) > 42.0)
    assertTrue((first?.longitude ?: 0.0) < -70.0)
  }
}

