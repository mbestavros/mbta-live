package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransitDaoTest {

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
  fun testInsertAllStopsInBatchesAndCount() = runTest {
    val stops = (1..1200).map { i ->
      CachedStopEntity(
        stopId = "stop-$i",
        name = "MBTA Stop $i",
        latitude = 42.35 + (i * 0.0001),
        longitude = -71.06 + (i * 0.0001),
        municipality = if (i % 2 == 0) "Boston" else "Cambridge",
        locationType = if (i % 10 == 0) 1 else 0
      )
    }

    // Insert 1200 items in 500-sized transactional chunks
    dao.insertAllStopsInBatches(stops, chunkSize = 500)

    val count = dao.getCachedStopCountSync()
    assertEquals(1200, count)

    val flowCount = dao.getCachedStopCount().first()
    assertEquals(1200, flowCount)
  }

  @Test
  fun testSearchStopsPrioritizesStations() = runTest {
    val busStop = CachedStopEntity(
      stopId = "stop-bus-1",
      name = "Harvard Ave",
      latitude = 42.35,
      longitude = -71.13,
      municipality = "Boston",
      locationType = 0 // Bus stop
    )
    val subwayStation = CachedStopEntity(
      stopId = "place-harsq",
      name = "Harvard Square",
      latitude = 42.37,
      longitude = -71.11,
      municipality = "Cambridge",
      locationType = 1 // Station hub
    )

    dao.insertAllStopsInBatches(listOf(busStop, subwayStation))

    val searchResults = dao.searchStopsSync("Harvard", limit = 10)
    assertEquals(2, searchResults.size)
    // The station (locationType = 1) should be ordered first before bus stop
    assertEquals("Harvard Square", searchResults[0].name)
    assertEquals(1, searchResults[0].locationType)
    assertEquals("Harvard Ave", searchResults[1].name)
    assertEquals(0, searchResults[1].locationType)
  }

  @Test
  fun testFavoriteStopsCrud() = runTest {
    val fav = FavoriteStopEntity(
      stopId = "place-pktrm",
      stopName = "Park Street",
      primaryLineName = "Red Line",
      primaryLineColorHex = "DA291C",
      latitude = 42.356395,
      longitude = -71.062424
    )

    // Initial check: not favorited
    val isFavInitial = dao.isStopFavorited("place-pktrm").first()
    assertFalse(isFavInitial)

    // Insert favorite
    dao.insertFavoriteStop(fav)
    val isFavAfter = dao.isStopFavorited("place-pktrm").first()
    assertTrue(isFavAfter)

    val allFavs = dao.getAllFavoriteStops().first()
    assertEquals(1, allFavs.size)
    assertEquals("Park Street", allFavs[0].stopName)

    // Delete favorite
    dao.deleteFavoriteStop("place-pktrm")
    val isFavDeleted = dao.isStopFavorited("place-pktrm").first()
    assertFalse(isFavDeleted)
    val emptyFavs = dao.getAllFavoriteStops().first()
    assertTrue(emptyFavs.isEmpty())
  }

  @Test
  fun testUserPreferences() = runTest {
    dao.setPreference(UserPreferenceEntity("test_pref", "value_123"))
    val saved = dao.getPreference("test_pref")
    assertEquals("value_123", saved)
  }
}
