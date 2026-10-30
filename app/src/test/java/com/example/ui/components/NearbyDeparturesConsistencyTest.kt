package com.example.ui.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MbtaPrediction
import com.example.data.repository.TransitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NearbyDeparturesConsistencyTest {

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

  private fun makePrediction(id: String, directionId: Int, countdownMinutes: Int, stopId: String = "stop-test"): MbtaPrediction {
    return MbtaPrediction(
      id = id,
      routeId = "Red",
      stopId = stopId,
      tripId = "trip-$id",
      directionId = directionId,
      arrivalTimeIso = null,
      departureTimeIso = null,
      status = null,
      vehicleId = null,
      stopSequence = 1,
      countdownMinutes = countdownMinutes,
      isLiveGps = true
    )
  }

  @Test
  fun testDeduplicationEliminatesNextArrivalFromUpcomingList() {
    // Scenario: API returns multiple predictions with duplicate countdowns (e.g. child platforms)
    val rawPreds = listOf(
      makePrediction("p1", 0, 3),
      makePrediction("p2", 0, 3), // duplicate of next arrival
      makePrediction("p3", 0, 7),
      makePrediction("p4", 0, 12),
      makePrediction("p5", 0, 12), // duplicate upcoming arrival
      makePrediction("p6", 0, 18)
    )

    val deduplicated = repository.deduplicateDirectionPredictions(
      preds = rawPreds,
      fallbackStopId = "place-pktrm",
      routeId = "Red",
      directionId = 0
    )

    // Must have 1 primary + at most 2 subsequent = 3 total
    assertEquals(3, deduplicated.size)

    val nextArrival = deduplicated.first()
    assertEquals(3, nextArrival.countdownMinutes)

    val upcoming = deduplicated.drop(1)
    assertEquals(2, upcoming.size)

    // Verify subsequent departure minutes are strictly greater and distinct
    assertEquals(7, upcoming[0].countdownMinutes)
    assertEquals(12, upcoming[1].countdownMinutes)

    // Critical assertion: Next arrival time (3) is NOT present in upcoming times
    assertFalse(upcoming.any { it.countdownMinutes == nextArrival.countdownMinutes })

    // Verify all departure minutes in the list are strictly strictly increasing
    assertTrue(deduplicated[0].countdownMinutes < deduplicated[1].countdownMinutes)
    assertTrue(deduplicated[1].countdownMinutes < deduplicated[2].countdownMinutes)
  }

  @Test
  fun testIdenticalCountdownPredictionsYieldSingleNextArrivalWithoutLaterDuplicates() {
    val rawPreds = listOf(
      makePrediction("p1", 1, 5),
      makePrediction("p2", 1, 5),
      makePrediction("p3", 1, 5)
    )

    val deduplicated = repository.deduplicateDirectionPredictions(
      preds = rawPreds,
      fallbackStopId = "place-pktrm",
      routeId = "Red",
      directionId = 1
    )

    assertEquals(1, deduplicated.size)
    assertEquals(5, deduplicated.first().countdownMinutes)
  }

  @Test
  fun testSinglePredictionProducesNoSubsequentItems() {
    val rawPreds = listOf(
      makePrediction("p1", 0, 8)
    )

    val deduplicated = repository.deduplicateDirectionPredictions(
      preds = rawPreds,
      fallbackStopId = "place-pktrm",
      routeId = "Red",
      directionId = 0
    )

    assertEquals(1, deduplicated.size)
    assertEquals(8, deduplicated.first().countdownMinutes)
  }

  @Test
  fun testEmptyPredictionsYieldFallbackSynthesis() {
    val deduplicated = repository.deduplicateDirectionPredictions(
      preds = emptyList(),
      fallbackStopId = "place-pktrm",
      routeId = "Red",
      directionId = 0
    )

    assertEquals(1, deduplicated.size)
    assertEquals(4, deduplicated.first().countdownMinutes)
  }

  @Test
  fun testRouteSpecificBoardingStopResolutionNearParkStreet() {
    // User is located right at Boston Common / Park Street
    val userLat = 42.356395
    val userLng = -71.062424

    val redRoute = com.example.data.model.MbtaRoute("Red", 1, "Red Line", "Red Line", "DA291C")
    val orangeRoute = com.example.data.model.MbtaRoute("Orange", 1, "Orange Line", "Orange Line", "ED8B00")
    val blueRoute = com.example.data.model.MbtaRoute("Blue", 1, "Blue Line", "Blue Line", "003DA5")
    val greenRoute = com.example.data.model.MbtaRoute("Green-B", 0, "Green (B)", "Green Line B", "00843D")
    val crRoute = com.example.data.model.MbtaRoute("CR-Providence", 2, "Providence", "Providence/Stoughton", "80276C")

    // Candidate stops from nearby query
    val candidates = listOf(
      com.example.data.graph.BostonLandmarks.PARK_STREET,
      com.example.data.graph.BostonLandmarks.DOWNTOWN_CROSSING,
      com.example.data.graph.BostonLandmarks.GOVERNMENT_CENTER,
      com.example.data.graph.BostonLandmarks.STATE,
      com.example.data.graph.BostonLandmarks.SOUTH_STATION
    )

    // Red Line can board at Park Street
    val redStop = com.example.data.model.MbtaRouteStationRegistry.resolveBoardingStopForRoute(
      route = redRoute,
      preds = emptyList(),
      candidateStops = candidates,
      allCachedStations = candidates,
      userLat = userLat,
      userLng = userLng
    )
    assertEquals("Park Street", redStop.name)

    // Green Line can board at Park Street
    val greenStop = com.example.data.model.MbtaRouteStationRegistry.resolveBoardingStopForRoute(
      route = greenRoute,
      preds = emptyList(),
      candidateStops = candidates,
      allCachedStations = candidates,
      userLat = userLat,
      userLng = userLng
    )
    assertEquals("Park Street", greenStop.name)

    // Orange Line DOES NOT stop at Park Street; must resolve to Downtown Crossing!
    val orangeStop = com.example.data.model.MbtaRouteStationRegistry.resolveBoardingStopForRoute(
      route = orangeRoute,
      preds = emptyList(),
      candidateStops = candidates,
      allCachedStations = candidates,
      userLat = userLat,
      userLng = userLng
    )
    assertEquals("Downtown Crossing", orangeStop.name)
    assertFalse(orangeStop.name == "Park Street")

    // Blue Line DOES NOT stop at Park Street; must resolve to Government Center or State!
    val blueStop = com.example.data.model.MbtaRouteStationRegistry.resolveBoardingStopForRoute(
      route = blueRoute,
      preds = emptyList(),
      candidateStops = candidates,
      allCachedStations = candidates,
      userLat = userLat,
      userLng = userLng
    )
    assertTrue(blueStop.name == "Government Center" || blueStop.name == "State")
    assertFalse(blueStop.name == "Park Street")

    // Commuter Rail DOES NOT stop at Park Street; must resolve to South Station!
    val crStop = com.example.data.model.MbtaRouteStationRegistry.resolveBoardingStopForRoute(
      route = crRoute,
      preds = emptyList(),
      candidateStops = candidates,
      allCachedStations = candidates,
      userLat = userLat,
      userLng = userLng
    )
    assertEquals("South Station", crStop.name)
    assertFalse(crStop.name == "Park Street")
  }

  @Test
  fun testChildPlatformResolutionMapsToParentStation() {
    val orangeRoute = com.example.data.model.MbtaRoute("Orange", 1, "Orange Line", "Orange Line", "ED8B00")
    val candidates = listOf(
      com.example.data.graph.BostonLandmarks.PARK_STREET,
      com.example.data.graph.BostonLandmarks.DOWNTOWN_CROSSING
    )

    // Child platform 70014 is Downtown Crossing Orange Line northbound
    val predWithChildPlatform = listOf(
      MbtaPrediction(
        id = "pred-dtx-orange",
        routeId = "Orange",
        stopId = "70014",
        tripId = "trip-1",
        directionId = 0,
        arrivalTimeIso = null,
        departureTimeIso = null,
        status = null,
        vehicleId = "veh-1",
        stopSequence = 1,
        countdownMinutes = 4,
        isLiveGps = true
      )
    )

    val resolved = com.example.data.model.MbtaRouteStationRegistry.resolveBoardingStopForRoute(
      route = orangeRoute,
      preds = predWithChildPlatform,
      candidateStops = candidates,
      allCachedStations = candidates,
      userLat = 42.356395,
      userLng = -71.062424
    )

    assertEquals("Downtown Crossing", resolved.name)
    assertEquals("place-dwnxg", resolved.id)
  }

  @Test
  fun testGenerateOfflinePredictionsIncludesLegitimateLinesPerStation() {
    // Park Street serves Red and Green lines
    val parkStPreds = repository.generateOfflinePredictions("place-pktrm")
    val parkStRoutes = parkStPreds.map { it.routeId }.distinct()
    assertTrue(parkStRoutes.contains("Red"))
    assertTrue(parkStRoutes.any { it.startsWith("Green") })
    // Must NOT contain Orange or Blue at Park Street
    assertFalse(parkStRoutes.contains("Orange"))
    assertFalse(parkStRoutes.contains("Blue"))

    // Downtown Crossing serves Orange and Red
    val dtxPreds = repository.generateOfflinePredictions("place-dwnxg")
    val dtxRoutes = dtxPreds.map { it.routeId }.distinct()
    assertTrue(dtxRoutes.contains("Orange"))
    assertTrue(dtxRoutes.contains("Red"))
    // Must NOT contain Blue at Downtown Crossing
    assertFalse(dtxRoutes.contains("Blue"))
  }

  @Test
  fun testGenerateOfflineDeparturesForBackBayIncludesOrangeAndCommuterRailNotJustRed() {
    // Back Bay Station: lat 42.34735, lng -71.075727
    val preds = repository.generateOfflineDeparturesForLocation(42.34735, -71.075727)
    val routes = preds.map { it.routeId }.distinct()

    assertTrue("Back Bay must include Orange Line", routes.contains("Orange"))
    assertTrue(
      "Back Bay must include Commuter Rail or Green Line branches",
      routes.contains("CR-Providence") || routes.any { it.startsWith("Green") }
    )
    assertFalse("Must never collapse to only the Red Line at Back Bay", routes.size == 1 && routes.first() == "Red")
  }

  @Test
  fun testGenerateOfflineDeparturesForAirportIncludesBlueLine() {
    // Airport Station in East Boston: lat 42.374262, lng -71.030395
    val preds = repository.generateOfflineDeparturesForLocation(42.374262, -71.030395)
    val routes = preds.map { it.routeId }.distinct()

    assertTrue("Airport must include Blue Line", routes.contains("Blue"))
    assertFalse("Must never collapse to only Red Line in East Boston", routes.size == 1 && routes.first() == "Red")
  }

  @Test
  fun testGenerateOfflineDeparturesForKenmoreIncludesGreenBranches() {
    // Kenmore Station: lat 42.348949, lng -71.095169
    val preds = repository.generateOfflineDeparturesForLocation(42.348949, -71.095169)
    val routes = preds.map { it.routeId }.distinct()

    assertTrue("Kenmore must include Green Line branches", routes.any { it.startsWith("Green") })
    assertFalse("Must never collapse to only Red Line at Kenmore", routes.size == 1 && routes.first() == "Red")
  }

  @Test
  fun testUnknownStopOfflinePredictionsDoesNotDefaultToRedOnly() {
    // Unmapped/bus stop ID
    val preds = repository.generateOfflinePredictions("99999")
    val routes = preds.map { it.routeId }.distinct()

    assertTrue("Unknown stops should return multi-route fallback, not just Red", routes.size > 1)
    assertFalse("Must not be Red-only", routes.size == 1 && routes.first() == "Red")
  }

  @Test
  fun testGetRoutesForLocationDiscoversNearbyPhysicalLines() {
    // Back Bay: Orange
    val backBayRoutes = com.example.data.model.MbtaRouteStationRegistry.getRoutesForLocation(42.34735, -71.075727)
    assertTrue("Back Bay routes must include Orange", backBayRoutes.contains("Orange"))

    // Airport: Blue
    val airportRoutes = com.example.data.model.MbtaRouteStationRegistry.getRoutesForLocation(42.374262, -71.030395)
    assertTrue("Airport routes must include Blue", airportRoutes.contains("Blue"))

    // Harvard: Red
    val harvardRoutes = com.example.data.model.MbtaRouteStationRegistry.getRoutesForLocation(42.373362, -71.118956)
    assertTrue("Harvard routes must include Red", harvardRoutes.contains("Red"))
  }
}
