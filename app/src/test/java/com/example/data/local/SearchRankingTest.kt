package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SearchRankingTest {

  private lateinit var database: TransitDatabase
  private lateinit var dao: TransitDao

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, TransitDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    dao = database.transitDao()
  }

  @After
  fun closeDb() {
    database.close()
  }

  @Test
  fun testSearchPrioritizesPrefixMatchesAndStationHubs() = runTest {
    // 1. Prefix match + Station Hub (locationType = 1) -> Rank #1
    val parkStreetStation = CachedStopEntity(
      stopId = "stop-park-station",
      name = "Park Street",
      latitude = 42.356,
      longitude = -71.062,
      municipality = "Boston",
      locationType = 1
    )

    // 2. Prefix match + Bus Stop (locationType = 0) -> Rank #2
    val parkAveBus = CachedStopEntity(
      stopId = "stop-park-bus",
      name = "Park Avenue",
      latitude = 42.360,
      longitude = -71.070,
      municipality = "Boston",
      locationType = 0
    )

    // 3. Substring match + Station Hub (locationType = 1) -> Rank #3
    val fenwayParkStation = CachedStopEntity(
      stopId = "stop-fenway-station",
      name = "Fenway Park Station",
      latitude = 42.342,
      longitude = -71.098,
      municipality = "Boston",
      locationType = 1
    )

    // 4. Substring match + Bus Stop (locationType = 0) -> Rank #4
    val centralParkWayBus = CachedStopEntity(
      stopId = "stop-central-bus",
      name = "Central Park Way",
      latitude = 42.370,
      longitude = -71.100,
      municipality = "Cambridge",
      locationType = 0
    )

    dao.insertAllStopsInBatches(listOf(centralParkWayBus, parkAveBus, fenwayParkStation, parkStreetStation))

    val results = dao.searchStopsSync("Park", limit = 10)
    assertEquals(4, results.size)

    // Verify ordering
    assertEquals("Park Street", results[0].name)
    assertEquals(1, results[0].locationType)

    assertEquals("Park Avenue", results[1].name)
    assertEquals(0, results[1].locationType)

    assertEquals("Fenway Park Station", results[2].name)
    assertEquals(1, results[2].locationType)

    assertEquals("Central Park Way", results[3].name)
    assertEquals(0, results[3].locationType)
  }
}
