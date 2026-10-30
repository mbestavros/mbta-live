package com.example.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class UserLocation(
  val latitude: Double,
  val longitude: Double,
  val displayName: String,
  val isGpsActive: Boolean
)

object LocationHelper {

  // Boston City Hall / Government Center coordinates as default center
  const val DEFAULT_BOSTON_LAT = 42.356395
  const val DEFAULT_BOSTON_LNG = -71.062424

  fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
  }

  suspend fun getDeviceLocation(context: Context): UserLocation = withContext(Dispatchers.IO) {
    if (!hasLocationPermission(context)) {
      return@withContext UserLocation(
        latitude = DEFAULT_BOSTON_LAT,
        longitude = DEFAULT_BOSTON_LNG,
        displayName = "Boston Common / Downtown",
        isGpsActive = false
      )
    }

    try {
      val fusedClient = LocationServices.getFusedLocationProviderClient(context)
      val location: Location? = try {
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
          ?: fusedClient.lastLocation.await()
      } catch (_: Exception) {
        fusedClient.lastLocation.await()
      }

      if (location != null) {
        val label = getNeighborhoodLabel(location.latitude, location.longitude, context)
        UserLocation(
          latitude = location.latitude,
          longitude = location.longitude,
          displayName = label,
          isGpsActive = true
        )
      } else {
        UserLocation(
          latitude = DEFAULT_BOSTON_LAT,
          longitude = DEFAULT_BOSTON_LNG,
          displayName = "Boston Common / Downtown",
          isGpsActive = false
        )
      }
    } catch (_: Exception) {
      UserLocation(
        latitude = DEFAULT_BOSTON_LAT,
        longitude = DEFAULT_BOSTON_LNG,
        displayName = "Boston Common / Downtown",
        isGpsActive = false
      )
    }
  }

  fun getNeighborhoodLabel(lat: Double, lng: Double, context: Context? = null): String {
    if (context != null && Geocoder.isPresent()) {
      try {
        val geocoder = Geocoder(context, Locale.US)
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        val address = addresses?.firstOrNull()
        if (address != null) {
          val sub = address.subLocality ?: address.thoroughfare
          val city = address.locality ?: "Boston"
          return if (!sub.isNullOrBlank() && sub != city) "$sub, $city" else city
        }
      } catch (_: Exception) {}
    }

    // Proximity fallback for offline/test environments
    val closest = com.example.data.graph.BostonLandmarks.ALL_MAJOR_STOPS.minByOrNull {
      com.example.data.graph.TransitGraphPathfinder.calculateDistanceMiles(lat, lng, it.latitude, it.longitude)
    }
    if (closest != null) {
      val dist = com.example.data.graph.TransitGraphPathfinder.calculateDistanceMiles(lat, lng, closest.latitude, closest.longitude)
      if (dist < 2.0) {
        return "Near ${closest.name}, ${closest.municipality}"
      }
    }

    return "Greater Boston Area"
  }
}
