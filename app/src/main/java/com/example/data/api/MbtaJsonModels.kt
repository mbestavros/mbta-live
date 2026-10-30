package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonApiEnvelope<T>(
  @Json(name = "data") val data: List<T> = emptyList(),
  @Json(name = "included") val included: List<JsonApiResource>? = null
)

@JsonClass(generateAdapter = true)
data class JsonApiResource(
  @Json(name = "id") val id: String = "",
  @Json(name = "type") val type: String = "",
  @Json(name = "attributes") val attributes: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class RouteResource(
  @Json(name = "id") val id: String,
  @Json(name = "type") val type: String,
  @Json(name = "attributes") val attributes: RouteAttributes
)

@JsonClass(generateAdapter = true)
data class RouteAttributes(
  @Json(name = "color") val color: String? = "00843D",
  @Json(name = "description") val description: String? = null,
  @Json(name = "direction_destinations") val directionDestinations: List<String>? = null,
  @Json(name = "direction_names") val directionNames: List<String>? = null,
  @Json(name = "long_name") val longName: String? = null,
  @Json(name = "short_name") val shortName: String? = null,
  @Json(name = "text_color") val textColor: String? = "FFFFFF",
  @Json(name = "type") val type: Int = 3
)

@JsonClass(generateAdapter = true)
data class StopResource(
  @Json(name = "id") val id: String,
  @Json(name = "type") val type: String,
  @Json(name = "attributes") val attributes: StopAttributes
)

@JsonClass(generateAdapter = true)
data class StopAttributes(
  @Json(name = "name") val name: String = "",
  @Json(name = "latitude") val latitude: Double? = null,
  @Json(name = "longitude") val longitude: Double? = null,
  @Json(name = "municipality") val municipality: String? = "Boston",
  @Json(name = "wheelchair_boarding") val wheelchairBoarding: Int? = 1,
  @Json(name = "location_type") val locationType: Int? = 0
)

@JsonClass(generateAdapter = true)
data class PredictionResource(
  @Json(name = "id") val id: String,
  @Json(name = "type") val type: String,
  @Json(name = "attributes") val attributes: PredictionAttributes,
  @Json(name = "relationships") val relationships: PredictionRelationships? = null
)

@JsonClass(generateAdapter = true)
data class PredictionAttributes(
  @Json(name = "arrival_time") val arrivalTime: String? = null,
  @Json(name = "departure_time") val departureTime: String? = null,
  @Json(name = "direction_id") val directionId: Int = 0,
  @Json(name = "status") val status: String? = null,
  @Json(name = "stop_sequence") val stopSequence: Int? = 0
)

@JsonClass(generateAdapter = true)
data class PredictionRelationships(
  @Json(name = "route") val route: RelationshipItem? = null,
  @Json(name = "stop") val stop: RelationshipItem? = null,
  @Json(name = "trip") val trip: RelationshipItem? = null,
  @Json(name = "vehicle") val vehicle: RelationshipItem? = null
)

@JsonClass(generateAdapter = true)
data class RelationshipItem(
  @Json(name = "data") val data: ResourceIdentifier? = null
)

@JsonClass(generateAdapter = true)
data class ResourceIdentifier(
  @Json(name = "id") val id: String = "",
  @Json(name = "type") val type: String = ""
)

@JsonClass(generateAdapter = true)
data class VehicleResource(
  @Json(name = "id") val id: String,
  @Json(name = "type") val type: String,
  @Json(name = "attributes") val attributes: VehicleAttributes,
  @Json(name = "relationships") val relationships: PredictionRelationships? = null
)

@JsonClass(generateAdapter = true)
data class VehicleAttributes(
  @Json(name = "bearing") val bearing: Double? = null,
  @Json(name = "current_status") val currentStatus: String? = null,
  @Json(name = "direction_id") val directionId: Int = 0,
  @Json(name = "label") val label: String? = null,
  @Json(name = "latitude") val latitude: Double? = null,
  @Json(name = "longitude") val longitude: Double? = null,
  @Json(name = "speed") val speed: Double? = null
)

@JsonClass(generateAdapter = true)
data class AlertResource(
  @Json(name = "id") val id: String,
  @Json(name = "type") val type: String,
  @Json(name = "attributes") val attributes: AlertAttributes
)

@JsonClass(generateAdapter = true)
data class AlertAttributes(
  @Json(name = "effect") val effect: String? = "UNKNOWN",
  @Json(name = "header") val header: String? = null,
  @Json(name = "service_effect") val serviceEffect: String? = null,
  @Json(name = "severity") val severity: Int = 1
)

@JsonClass(generateAdapter = true)
data class SeedStopItem(
  @Json(name = "stopId") val stopId: String = "",
  @Json(name = "name") val name: String = "",
  @Json(name = "latitude") val latitude: Double = 0.0,
  @Json(name = "longitude") val longitude: Double = 0.0,
  @Json(name = "municipality") val municipality: String = "Boston",
  @Json(name = "locationType") val locationType: Int = 1,
  @Json(name = "wheelchairBoarding") val wheelchairBoarding: Int = 1
)

