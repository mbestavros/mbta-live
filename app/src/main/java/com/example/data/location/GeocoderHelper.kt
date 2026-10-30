package com.example.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class GeocodedLocation(
  val label: String,
  val address: String,
  val latitude: Double,
  val longitude: Double,
  val isLandmark: Boolean = false
)

object GeocoderHelper {

  // Greater Boston bounding coordinates to prioritize local search results
  private const val BOSTON_LOWER_LEFT_LAT = 42.15
  private const val BOSTON_LOWER_LEFT_LNG = -71.30
  private const val BOSTON_UPPER_RIGHT_LAT = 42.50
  private const val BOSTON_UPPER_RIGHT_LNG = -70.90

  /**
   * Search for locations matching the user's query.
   * 1. Matches against known MBTA stations and Boston transit landmarks.
   * 2. Queries standard Android Geocoder with Greater Boston bounding priority.
   * 3. Falls back to unbounded Android Geocoder for any general address.
   * 4. If offline or geocoder returns no results, returns matching MBTA stations/landmarks.
   */
  suspend fun geocodeQuery(context: Context, query: String): List<GeocodedLocation> = withContext(Dispatchers.IO) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return@withContext emptyList()

    val results = mutableListOf<GeocodedLocation>()

    // 1. Check cached MBTA stops from local database for instant, offline transit matching
    val matchedStops = matchCachedStops(context, trimmed)
    results.addAll(matchedStops)

    // 2. Query built-in Android Geocoder
    val geocoderResults = queryAndroidGeocoder(context, trimmed)
    results.addAll(geocoderResults)

    // 3. Deduplicate by coordinates (rounded to 4 decimal places, ~11 meters)
    results.distinctBy { "${String.format(Locale.US, "%.4f", it.latitude)}_${String.format(Locale.US, "%.4f", it.longitude)}" }
  }

  suspend fun resolveLocation(context: Context, query: String): GeocodedLocation? = withContext(Dispatchers.IO) {
    val list = geocodeQuery(context, query)
    list.firstOrNull()
  }

  private suspend fun matchCachedStops(context: Context, query: String): List<GeocodedLocation> {
    val qLower = query.lowercase().trim()
    if (qLower.length < 2) return emptyList()

    return try {
      val dao = com.example.data.local.TransitDatabase.getDatabase(context).transitDao()
      val stops = dao.searchStopsSync(qLower, limit = 20)
      stops.map { stop ->
        val modeDesc = if (stop.locationType == 1) "MBTA Station" else "MBTA Stop"
        GeocodedLocation(
          label = stop.name,
          address = "$modeDesc • ${stop.municipality}",
          latitude = stop.latitude,
          longitude = stop.longitude,
          isLandmark = true
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun queryAndroidGeocoder(context: Context, query: String): List<GeocodedLocation> {
    if (!Geocoder.isPresent()) return emptyList()

    return try {
      val geocoder = Geocoder(context, Locale.US)

      // Query with Greater Boston bounding box first to prioritize local addresses
      @Suppress("DEPRECATION")
      val localAddresses = try {
        geocoder.getFromLocationName(
          query,
          5,
          BOSTON_LOWER_LEFT_LAT,
          BOSTON_LOWER_LEFT_LNG,
          BOSTON_UPPER_RIGHT_LAT,
          BOSTON_UPPER_RIGHT_LNG
        )
      } catch (_: Exception) {
        null
      }

      val addresses = if (!localAddresses.isNullOrEmpty()) {
        localAddresses
      } else {
        // Fallback to unbounded search for general addresses
        @Suppress("DEPRECATION")
        geocoder.getFromLocationName(query, 5) ?: emptyList()
      }

      addresses.mapNotNull { addr ->
        if (!addr.hasLatitude() || !addr.hasLongitude()) return@mapNotNull null

        val label = when {
          !addr.featureName.isNullOrBlank() && addr.featureName != addr.subThoroughfare -> addr.featureName
          !addr.thoroughfare.isNullOrBlank() && !addr.subThoroughfare.isNullOrBlank() -> "${addr.subThoroughfare} ${addr.thoroughfare}"
          !addr.thoroughfare.isNullOrBlank() -> addr.thoroughfare
          else -> addr.getAddressLine(0)?.split(",")?.firstOrNull()?.trim() ?: query
        }

        GeocodedLocation(
          label = label,
          address = formatAddress(addr),
          latitude = addr.latitude,
          longitude = addr.longitude,
          isLandmark = false
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun formatAddress(address: Address): String {
    val sb = StringBuilder()
    for (i in 0..address.maxAddressLineIndex) {
      val line = address.getAddressLine(i)
      if (!line.isNullOrBlank()) {
        if (sb.isNotEmpty()) sb.append(", ")
        sb.append(line)
      }
    }
    if (sb.isNotEmpty()) return sb.toString()

    val parts = listOfNotNull(
      address.locality ?: address.subAdminArea,
      address.adminArea,
      address.postalCode
    ).filter { it.isNotBlank() }

    return if (parts.isNotEmpty()) parts.joinToString(", ") else "Massachusetts"
  }
}
