package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface MbtaApiService {

  @GET("routes")
  suspend fun getRoutes(
    @Query("filter[type]") type: String? = "0,1,2,3,4"
  ): JsonApiEnvelope<RouteResource>

  @GET("stops")
  suspend fun getAllStops(
    @Query("filter[route_type]") routeType: String? = null,
    @Query("filter[location_type]") locationType: String? = "0,1"
  ): JsonApiEnvelope<StopResource>

  @GET("stops")
  suspend fun getStopsNearby(
    @Query("filter[latitude]") latitude: Double,
    @Query("filter[longitude]") longitude: Double,
    @Query("filter[radius]") radius: Double = 0.02, // ~1.4 miles
    @Query("sort") sort: String = "distance"
  ): JsonApiEnvelope<StopResource>

  @GET("stops")
  suspend fun getStopsForRoute(
    @Query("filter[route]") routeId: String
  ): JsonApiEnvelope<StopResource>

  @GET("predictions")
  suspend fun getPredictionsForStops(
    @Query("filter[stop]") stopIds: String,
    @Query("include") include: String = "trip,vehicle",
    @Query("sort") sort: String = "time"
  ): JsonApiEnvelope<PredictionResource>

  @GET("predictions")
  suspend fun getPredictionsForRoute(
    @Query("filter[route]") routeId: String,
    @Query("include") include: String = "trip,vehicle",
    @Query("sort") sort: String = "time"
  ): JsonApiEnvelope<PredictionResource>

  @GET("vehicles")
  suspend fun getVehiclesForRoute(
    @Query("filter[route]") routeId: String
  ): JsonApiEnvelope<VehicleResource>

  @GET("alerts")
  suspend fun getAlerts(
    @Query("filter[datetime]") datetime: String = "NOW",
    @Query("filter[route]") routeId: String? = null
  ): JsonApiEnvelope<AlertResource>
}
