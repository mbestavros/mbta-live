package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_stops")
data class FavoriteStopEntity(
  @PrimaryKey val stopId: String,
  val stopName: String,
  val primaryLineName: String,
  val primaryLineColorHex: String,
  val latitude: Double = 42.356395,
  val longitude: Double = -71.062424,
  val savedEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_itineraries")
data class SavedItineraryEntity(
  @PrimaryKey val id: String,
  val fromName: String,
  val toName: String,
  val totalMinutes: Int,
  val transferCount: Int,
  val summaryText: String,
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
  @PrimaryKey val prefKey: String,
  val prefValue: String
)

@Entity(
  tableName = "cached_stops",
  indices = [
    Index(value = ["locationType", "name"]),
    Index(value = ["name"]),
    Index(value = ["municipality"])
  ]
)
data class CachedStopEntity(
  @PrimaryKey val stopId: String,
  val name: String,
  val latitude: Double,
  val longitude: Double,
  val municipality: String,
  val locationType: Int = 0,
  val wheelchairBoarding: Int = 1,
  val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)

@Dao
abstract class TransitDao {

  @Query("SELECT * FROM favorite_stops ORDER BY savedEpochMs DESC")
  abstract fun getAllFavoriteStops(): Flow<List<FavoriteStopEntity>>

  @Query("SELECT EXISTS(SELECT 1 FROM favorite_stops WHERE stopId = :stopId)")
  abstract fun isStopFavorited(stopId: String): Flow<Boolean>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insertFavoriteStop(stop: FavoriteStopEntity)

  @Query("DELETE FROM favorite_stops WHERE stopId = :stopId")
  abstract suspend fun deleteFavoriteStop(stopId: String)

  @Query("SELECT * FROM saved_itineraries ORDER BY timestamp DESC LIMIT 20")
  abstract fun getRecentItineraries(): Flow<List<SavedItineraryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insertItinerary(itinerary: SavedItineraryEntity)

  @Query("SELECT prefValue FROM user_preferences WHERE prefKey = :key LIMIT 1")
  abstract suspend fun getPreference(key: String): String?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun setPreference(pref: UserPreferenceEntity)

  // Cached MBTA Stops (Offline Sync & Universal Search)
  @Query("SELECT * FROM cached_stops WHERE name LIKE '%' || :query || '%' OR municipality LIKE '%' || :query || '%' ORDER BY (CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END) ASC, (locationType = 1) DESC, name ASC LIMIT :limit")
  abstract fun searchStops(query: String, limit: Int = 50): Flow<List<CachedStopEntity>>

  @Query("SELECT * FROM cached_stops WHERE name LIKE '%' || :query || '%' OR municipality LIKE '%' || :query || '%' ORDER BY (CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END) ASC, (locationType = 1) DESC, name ASC LIMIT :limit")
  abstract suspend fun searchStopsSync(query: String, limit: Int = 50): List<CachedStopEntity>

  @Query("SELECT * FROM cached_stops WHERE stopId = :stopId LIMIT 1")
  abstract suspend fun getStopById(stopId: String): CachedStopEntity?

  @Query("SELECT * FROM cached_stops WHERE stopId IN (:stopIds)")
  abstract suspend fun getStopsByIds(stopIds: List<String>): List<CachedStopEntity>

  @Query("SELECT * FROM cached_stops")
  abstract suspend fun getAllCachedStops(): List<CachedStopEntity>

  @Query("SELECT * FROM cached_stops WHERE locationType = 1")
  abstract suspend fun getAllCachedStations(): List<CachedStopEntity>

  @Query("SELECT COUNT(*) FROM cached_stops")
  abstract fun getCachedStopCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM cached_stops")
  abstract suspend fun getCachedStopCountSync(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insertAllStops(stops: List<CachedStopEntity>)

  @Transaction
  open suspend fun insertAllStopsInBatches(stops: List<CachedStopEntity>, chunkSize: Int = 500) {
    stops.chunked(chunkSize).forEach { chunk ->
      insertAllStops(chunk)
    }
  }

  @Query("DELETE FROM cached_stops")
  abstract suspend fun clearAllStops()
}

@Database(
  entities = [FavoriteStopEntity::class, SavedItineraryEntity::class, UserPreferenceEntity::class, CachedStopEntity::class],
  version = 2,
  exportSchema = false
)
abstract class TransitDatabase : RoomDatabase() {
  abstract fun transitDao(): TransitDao

  companion object {
    @Volatile
    private var INSTANCE: TransitDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
          CREATE TABLE IF NOT EXISTS `cached_stops` (
            `stopId` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `latitude` REAL NOT NULL,
            `longitude` REAL NOT NULL,
            `municipality` TEXT NOT NULL,
            `locationType` INTEGER NOT NULL DEFAULT 0,
            `wheelchairBoarding` INTEGER NOT NULL DEFAULT 1,
            `lastUpdatedEpochMs` INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(`stopId`)
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_stops_locationType_name` ON `cached_stops` (`locationType`, `name`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_stops_name` ON `cached_stops` (`name`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_stops_municipality` ON `cached_stops` (`municipality`)")
      }
    }

    fun getDatabase(context: Context): TransitDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          TransitDatabase::class.java,
          "mbta_transit.db"
        )
          .addMigrations(MIGRATION_1_2)
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
