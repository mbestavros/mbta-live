package com.example.data.api

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MbtaJsonSerializationTest {

  private val moshi = MbtaApiClient.moshi

  @Test
  fun `test stop attributes with null coordinates does not throw JsonDataException`() {
    // Regression test for: "expected a double but was NULL at path $.data[20].attributes.latitude"
    val json = """
      {
        "id": "stop-null-coord",
        "type": "stop",
        "attributes": {
          "name": "Virtual MBTA Stop",
          "latitude": null,
          "longitude": null,
          "municipality": "Boston",
          "location_type": 0,
          "wheelchair_boarding": 1
        }
      }
    """.trimIndent()

    val adapter = moshi.adapter(StopResource::class.java)
    val stop = adapter.fromJson(json)

    assertNotNull(stop)
    assertEquals("stop-null-coord", stop!!.id)
    assertEquals("Virtual MBTA Stop", stop.attributes.name)
    assertNull(stop.attributes.latitude)
    assertNull(stop.attributes.longitude)
    assertEquals("Boston", stop.attributes.municipality)
  }

  @Test
  fun `test stop attributes with valid coordinates parses correctly`() {
    val json = """
      {
        "id": "place-pktrm",
        "type": "stop",
        "attributes": {
          "name": "Park Street",
          "latitude": 42.356395,
          "longitude": -71.062424,
          "municipality": "Boston",
          "location_type": 1,
          "wheelchair_boarding": 1
        }
      }
    """.trimIndent()

    val adapter = moshi.adapter(StopResource::class.java)
    val stop = adapter.fromJson(json)

    assertNotNull(stop)
    assertEquals("place-pktrm", stop!!.id)
    assertEquals("Park Street", stop.attributes.name)
    assertEquals(42.356395, stop.attributes.latitude!!, 0.0001)
    assertEquals(-71.062424, stop.attributes.longitude!!, 0.0001)
    assertEquals(1, stop.attributes.locationType)
  }

  @Test
  fun `test envelope parsing with multiple stops including missing coordinates`() {
    val json = """
      {
        "data": [
          {
            "id": "stop-1",
            "type": "stop",
            "attributes": {
              "name": "Station 1",
              "latitude": 42.35,
              "longitude": -71.06
            }
          },
          {
            "id": "stop-2",
            "type": "stop",
            "attributes": {
              "name": "Station 2 (Virtual)",
              "latitude": null,
              "longitude": null
            }
          }
        ]
      }
    """.trimIndent()

    val type = Types.newParameterizedType(JsonApiEnvelope::class.java, StopResource::class.java)
    val adapter = moshi.adapter<JsonApiEnvelope<StopResource>>(type)
    val envelope = adapter.fromJson(json)

    assertNotNull(envelope)
    assertEquals(2, envelope!!.data.size)
    assertNotNull(envelope.data[0].attributes.latitude)
    assertNull(envelope.data[1].attributes.latitude)
  }

  @Test
  fun `test route resource serialization and direction names`() {
    val json = """
      {
        "id": "Red",
        "type": "route",
        "attributes": {
          "color": "DA291C",
          "description": "Rapid Transit",
          "direction_destinations": ["Ashmont / Braintree", "Alewife"],
          "direction_names": ["Southbound", "Northbound"],
          "long_name": "Red Line",
          "short_name": "",
          "text_color": "FFFFFF",
          "type": 1
        }
      }
    """.trimIndent()

    val adapter = moshi.adapter(RouteResource::class.java)
    val route = adapter.fromJson(json)

    assertNotNull(route)
    assertEquals("Red", route!!.id)
    assertEquals("DA291C", route.attributes.color)
    assertEquals(2, route.attributes.directionDestinations?.size)
    assertEquals("Ashmont / Braintree", route.attributes.directionDestinations?.get(0))
  }

  @Test
  fun `test prediction resource with relationships`() {
    val json = """
      {
        "id": "prediction-12345",
        "type": "prediction",
        "attributes": {
          "arrival_time": "2026-10-28T14:35:00-04:00",
          "departure_time": "2026-10-28T14:36:00-04:00",
          "direction_id": 0,
          "status": "On time",
          "stop_sequence": 15
        },
        "relationships": {
          "route": { "data": { "id": "Red", "type": "route" } },
          "stop": { "data": { "id": "place-pktrm", "type": "stop" } },
          "trip": { "data": { "id": "trip-999", "type": "trip" } },
          "vehicle": { "data": { "id": "vehicle-1801", "type": "vehicle" } }
        }
      }
    """.trimIndent()

    val adapter = moshi.adapter(PredictionResource::class.java)
    val prediction = adapter.fromJson(json)

    assertNotNull(prediction)
    assertEquals("prediction-12345", prediction!!.id)
    assertEquals("Red", prediction.relationships?.route?.data?.id)
    assertEquals("place-pktrm", prediction.relationships?.stop?.data?.id)
    assertEquals("vehicle-1801", prediction.relationships?.vehicle?.data?.id)
  }

  @Test
  fun `test seed stop item serialization`() {
    val json = """
      [
        {
          "stopId": "place-pktrm",
          "name": "Park Street",
          "latitude": 42.356395,
          "longitude": -71.062424,
          "municipality": "Boston",
          "locationType": 1,
          "wheelchairBoarding": 1
        }
      ]
    """.trimIndent()

    val type = Types.newParameterizedType(List::class.java, SeedStopItem::class.java)
    val adapter = moshi.adapter<List<SeedStopItem>>(type)
    val items = adapter.fromJson(json)

    assertNotNull(items)
    assertEquals(1, items!!.size)
    assertEquals("place-pktrm", items[0].stopId)
    assertEquals("Park Street", items[0].name)
    assertEquals(42.356395, items[0].latitude, 0.0001)
  }
}
