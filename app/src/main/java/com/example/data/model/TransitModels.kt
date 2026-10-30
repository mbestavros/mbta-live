package com.example.data.model

import androidx.compose.runtime.Immutable

enum class TransitMode(val label: String) {
  ALL("All"),
  SUBWAY("Subway"),
  BUS("Bus"),
  COMMUTER_RAIL("Commuter Rail"),
  FERRY("Ferry")
}

@Immutable
data class MbtaRoute(
  val id: String,
  val type: Int, // 0: Light Rail, 1: Heavy Rail, 2: Commuter Rail, 3: Bus, 4: Ferry
  val shortName: String,
  val longName: String,
  val colorHex: String,
  val textColorHex: String = "FFFFFF",
  val directionNames: List<String> = listOf("Outbound", "Inbound"),
  val description: String = ""
) {
  val transitMode: TransitMode
    get() = when (type) {
      0, 1 -> TransitMode.SUBWAY
      2 -> TransitMode.COMMUTER_RAIL
      3 -> TransitMode.BUS
      4 -> TransitMode.FERRY
      else -> TransitMode.BUS
    }

  val displayName: String
    get() = if (shortName.isNotBlank()) shortName else longName
}

@Immutable
data class MbtaStop(
  val id: String,
  val name: String,
  val latitude: Double,
  val longitude: Double,
  val municipality: String = "Boston",
  val wheelchairBoarding: Int = 1,
  val parentStationId: String? = null,
  val locationType: Int = 0
)

@Immutable
data class MbtaPrediction(
  val id: String,
  val routeId: String,
  val stopId: String,
  val tripId: String?,
  val directionId: Int,
  val arrivalTimeIso: String?,
  val departureTimeIso: String?,
  val status: String?,
  val vehicleId: String?,
  val stopSequence: Int = 0,
  val countdownMinutes: Int = 0,
  val isLiveGps: Boolean = true
)

@Immutable
data class MbtaVehicle(
  val id: String,
  val routeId: String,
  val tripId: String?,
  val currentStatus: String?,
  val directionId: Int,
  val latitude: Double,
  val longitude: Double,
  val bearing: Double?,
  val label: String?
)

@Immutable
data class MbtaAlert(
  val id: String,
  val header: String,
  val effect: String,
  val severity: Int,
  val serviceEffect: String,
  val affectedRouteIds: List<String> = emptyList()
)

@Immutable
data class RouteDepartures(
  val route: MbtaRoute,
  val headsign: String,
  val directionId: Int,
  val predictions: List<MbtaPrediction>,
  val isLive: Boolean = true
)

@Immutable
data class DirectionDepartures(
  val directionId: Int,
  val headsign: String,
  val predictions: List<MbtaPrediction>,
  val isLive: Boolean = true
)

@Immutable
data class NearbyRouteItem(
  val id: String,
  val route: MbtaRoute,
  val nearestStop: MbtaStop,
  val distanceMiles: Double,
  val walkMinutes: Int,
  val directions: List<DirectionDepartures>
)

@Immutable
data class StationDepartures(
  val stop: MbtaStop,
  val distanceMiles: Double,
  val routeDepartures: List<RouteDepartures>
)

enum class LegType {
  WALK,
  TRANSIT,
  TRANSFER
}

@Immutable
data class ItineraryLeg(
  val legType: LegType,
  val routeId: String? = null,
  val routeName: String? = null,
  val routeColorHex: String? = null,
  val fromStopName: String,
  val toStopName: String,
  val durationMinutes: Int,
  val instructions: String,
  val stopsCount: Int = 0
)

@Immutable
data class Itinerary(
  val id: String,
  val fromName: String,
  val toName: String,
  val totalDurationMinutes: Int,
  val walkTimeMinutes: Int,
  val transferCount: Int,
  val legs: List<ItineraryLeg>
)

enum class TripStatus(val label: String) {
  WAITING("Waiting for departure"),
  ON_BOARD("On board"),
  TRANSFERRING("Transferring"),
  ARRIVED("Arrived")
}

@Immutable
data class LiveTripSession(
  val sessionId: String,
  val routeId: String,
  val routeName: String,
  val routeColorHex: String,
  val destination: String,
  val originStop: String,
  val targetArrivalEpochMs: Long,
  val durationMinutes: Int,
  val currentLegIndex: Int = 0,
  val totalLegs: Int = 1,
  val status: TripStatus = TripStatus.WAITING,
  val progressPercentage: Int = 0,
  val isOfflineEstimated: Boolean = false,
  val intermediateStops: List<String> = emptyList(),
  val currentStopName: String = originStop
)
