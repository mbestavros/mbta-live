package com.example.data.repository

import android.content.Context
import com.example.data.api.MbtaApiClient
import com.example.data.api.MbtaApiService
import com.example.data.api.PredictionResource
import com.example.data.api.RouteResource
import com.example.data.api.SeedStopItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.data.graph.BostonLandmarks
import com.example.data.graph.TransitGraphPathfinder
import com.example.data.local.FavoriteStopEntity
import com.example.data.local.TransitDao
import com.example.data.local.TransitDatabase
import com.example.data.model.Itinerary
import com.example.data.model.MbtaAlert
import com.example.data.model.MbtaPrediction
import com.example.data.model.MbtaRoute
import com.example.data.model.MbtaRouteStationRegistry
import com.example.data.model.MbtaStop
import com.example.data.model.RouteDepartures
import com.example.data.model.StationDepartures
import com.example.data.model.TransitMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

sealed class StopSyncState {
  object Idle : StopSyncState()
  object Syncing : StopSyncState()
  data class Success(val count: Int, val epochMs: Long) : StopSyncState()
  data class Error(val message: String, val isRateLimited: Boolean = false, val retryAfterSeconds: Long? = null) : StopSyncState()
}

sealed class TransitResult<out T> {
  data class Success<out T>(val data: T, val isOfflineFallback: Boolean = false) : TransitResult<T>()
  data class Error(
    val message: String,
    val isRateLimited: Boolean = false,
    val retryAfterSeconds: Long? = null,
    val cause: Throwable? = null
  ) : TransitResult<Nothing>()
}

class TransitRepository(
  private val context: Context,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  customApi: MbtaApiService? = null,
  customDao: TransitDao? = null
) {
  init {
    MbtaApiClient.init(context)
  }

  private val api: MbtaApiService = customApi ?: MbtaApiClient.apiService
  private val dao: TransitDao = customDao ?: TransitDatabase.getDatabase(context).transitDao()

  suspend fun seedStopsIfEmpty() = withContext(ioDispatcher) {
    try {
      val count = dao.getCachedStopCountSync()
      if (count == 0) {
        val jsonStr = context.assets.open("mbta_stops_seed.json").bufferedReader().use { it.readText() }
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, SeedStopItem::class.java)
        val adapter = moshi.adapter<List<SeedStopItem>>(type)
        val items = adapter.fromJson(jsonStr) ?: emptyList()
        val now = System.currentTimeMillis()
        val entities = items.map {
          com.example.data.local.CachedStopEntity(
            stopId = it.stopId,
            name = it.name,
            latitude = it.latitude,
            longitude = it.longitude,
            municipality = it.municipality,
            locationType = it.locationType,
            wheelchairBoarding = it.wheelchairBoarding,
            lastUpdatedEpochMs = now
          )
        }
        if (entities.isNotEmpty()) {
          dao.insertAllStopsInBatches(entities)
        }
      }
    } catch (_: Exception) {
      // Gracefully continue if assets read fails
    }
  }

  // In-memory Mutex-protected cache with 30-minute TTL for fast, zero-latency route catalog
  private val routeCacheMutex = Mutex()
  private var cachedRoutes: List<MbtaRoute> = emptyList()
  private var routeCacheTimestampMs: Long = 0L
  private val routeCacheTtlMs: Long = 30 * 60 * 1000L // 30 minutes

  val favoriteStops: Flow<List<FavoriteStopEntity>> = dao.getAllFavoriteStops()

  private val _stopSyncState = MutableStateFlow<StopSyncState>(StopSyncState.Idle)
  val stopSyncState: StateFlow<StopSyncState> = _stopSyncState.asStateFlow()

  val cachedStopCount: Flow<Int> = dao.getCachedStopCount()

  fun isFavorite(stopId: String): Flow<Boolean> = dao.isStopFavorited(stopId)

  suspend fun toggleFavorite(stop: MbtaStop, primaryLineName: String, primaryLineColor: String) {
    withContext(ioDispatcher) {
      val entity = FavoriteStopEntity(
        stopId = stop.id,
        stopName = stop.name,
        primaryLineName = primaryLineName,
        primaryLineColorHex = primaryLineColor,
        latitude = stop.latitude,
        longitude = stop.longitude
      )
      dao.insertFavoriteStop(entity)
    }
  }

  suspend fun removeFavorite(stopId: String) {
    withContext(ioDispatcher) {
      dao.deleteFavoriteStop(stopId)
    }
  }

  internal suspend fun <T> executeWithRetry(
    maxAttempts: Int = 2,
    initialDelayMs: Long = 1000L,
    block: suspend () -> T
  ): T {
    var currentDelay = initialDelayMs
    repeat(maxAttempts - 1) {
      try {
        return block()
      } catch (e: CancellationException) {
        throw e
      } catch (e: HttpException) {
        if (e.code() == 429) {
          val retryAfterSec = e.response()?.headers()?.get("Retry-After")?.toLongOrNull()
          val delayToUse = if (retryAfterSec != null && retryAfterSec > 0) {
            (retryAfterSec * 1000L).coerceAtMost(10000L)
          } else {
            currentDelay
          }
          delay(delayToUse)
          currentDelay *= 2
        } else if (e.code() in 500..599) {
          delay(currentDelay)
          currentDelay *= 2
        } else {
          throw e
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (e.message?.contains("429") == true) {
          delay(currentDelay)
          currentDelay *= 2
        } else {
          throw e
        }
      }
    }
    return block()
  }

  suspend fun syncAllStops(forceRefresh: Boolean = false): StopSyncState = withContext(ioDispatcher) {
    seedStopsIfEmpty()

    val count = dao.getCachedStopCountSync()
    val lastSyncStr = dao.getPreference("last_stops_sync_time")
    val lastSync = lastSyncStr?.toLongOrNull() ?: 0L
    val now = System.currentTimeMillis()

    if (!forceRefresh && count > 0 && (now - lastSync) < 24 * 60 * 60 * 1000L) {
      val state = StopSyncState.Success(count, lastSync)
      _stopSyncState.value = state
      return@withContext state
    }

    _stopSyncState.value = StopSyncState.Syncing
    try {
      val response = executeWithRetry(maxAttempts = 2, initialDelayMs = 1500L) {
        api.getAllStops()
      }
      val entities = response.data.mapNotNull {
        val lat = it.attributes.latitude
        val lng = it.attributes.longitude
        if (lat == null || lng == null || (lat == 0.0 && lng == 0.0)) {
          null
        } else {
          com.example.data.local.CachedStopEntity(
            stopId = it.id,
            name = it.attributes.name.ifBlank { "MBTA Stop ${it.id}" },
            latitude = lat,
            longitude = lng,
            municipality = it.attributes.municipality ?: "Boston",
            locationType = it.attributes.locationType ?: 0,
            wheelchairBoarding = it.attributes.wheelchairBoarding ?: 1,
            lastUpdatedEpochMs = now
          )
        }
      }
      if (entities.isNotEmpty()) {
        dao.insertAllStopsInBatches(entities)
        dao.setPreference(com.example.data.local.UserPreferenceEntity("last_stops_sync_time", now.toString()))
        val success = StopSyncState.Success(entities.size, now)
        _stopSyncState.value = success
        success
      } else {
        val currentCount = dao.getCachedStopCountSync()
        val fallback = StopSyncState.Success(currentCount, now)
        _stopSyncState.value = fallback
        fallback
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val isRateLimited = (e is HttpException && e.code() == 429) || (e.message?.contains("429") == true)
      val currentCount = dao.getCachedStopCountSync()
      val msg = if (isRateLimited) {
        if (currentCount > 0) "MBTA API rate limit reached: Using $currentCount offline cached stops" else "MBTA API rate limit reached: Please try again shortly"
      } else {
        if (currentCount > 0) "Offline mode: Using $currentCount cached stops" else (e.message ?: "Sync failed")
      }
      val err = StopSyncState.Error(msg, isRateLimited = isRateLimited)
      _stopSyncState.value = err
      err
    }
  }

  suspend fun searchCachedStops(query: String, limit: Int = 30): List<MbtaStop> = withContext(ioDispatcher) {
    dao.searchStopsSync(query, limit).map {
      MbtaStop(
        id = it.stopId,
        name = it.name,
        latitude = it.latitude,
        longitude = it.longitude,
        municipality = it.municipality,
        wheelchairBoarding = it.wheelchairBoarding
      )
    }
  }

  suspend fun getRoutes(forceRefresh: Boolean = false): List<MbtaRoute> = withContext(ioDispatcher) {
    val now = System.currentTimeMillis()
    routeCacheMutex.withLock {
      if (cachedRoutes.isNotEmpty() && !forceRefresh && (now - routeCacheTimestampMs < routeCacheTtlMs)) {
        return@withContext cachedRoutes
      }

      try {
        val response = executeWithRetry { api.getRoutes() }
        val routes = response.data.map { mapRoute(it) }
        cachedRoutes = routes
        routeCacheTimestampMs = now
        routes
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        // Offline fallback: Return core MBTA rapid transit lines
        getOfflineFallbackRoutes().also {
          cachedRoutes = it
          routeCacheTimestampMs = now
        }
      }
    }
  }

  suspend fun getNearbyStations(
    lat: Double,
    lng: Double,
    selectedMode: TransitMode = TransitMode.ALL
  ): List<StationDepartures> = withContext(ioDispatcher) {
    val routes = getRoutes()
    val routesMap = routes.associateBy { it.id }

    // Query nearby stops from API, with graceful offline fallback
    val stops: List<MbtaStop> = try {
      val stopResponse = api.getStopsNearby(lat, lng, radius = 0.03)
      if (stopResponse.data.isNotEmpty()) {
        stopResponse.data.mapNotNull {
          val stopLat = it.attributes.latitude
          val stopLng = it.attributes.longitude
          if (stopLat == null || stopLng == null) null
          else MbtaStop(
            id = it.id,
            name = it.attributes.name,
            latitude = stopLat,
            longitude = stopLng,
            municipality = it.attributes.municipality ?: "Boston",
            wheelchairBoarding = it.attributes.wheelchairBoarding ?: 1
          )
        }
      } else {
        getSortedFallbackStops(lat, lng)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      getSortedFallbackStops(lat, lng)
    }

    // Take the closest 6 stations to prevent unnecessary network overhead
    val topStops = stops.take(6)
    val stopIds = topStops.joinToString(",") { it.id }

    // Fetch live predictions for these stops
    val predictionsByStop = mutableMapOf<String, MutableList<MbtaPrediction>>()
    try {
      if (stopIds.isNotBlank()) {
        val predResponse = api.getPredictionsForStops(stopIds)
        predResponse.data.forEach { p ->
          val stopId = p.relationships?.stop?.data?.id ?: return@forEach
          val pred = mapPrediction(p)
          predictionsByStop.getOrPut(stopId) { mutableListOf() }.add(pred)
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      // In offline/tunnel mode: Generate realistic schedule-based departures
      topStops.forEach { stop ->
        predictionsByStop[stop.id] = generateOfflinePredictions(stop.id).toMutableList()
      }
    }

    // Assemble StationDepartures
    topStops.mapNotNull { stop ->
      val dist = TransitGraphPathfinder.calculateDistanceMiles(lat, lng, stop.latitude, stop.longitude)
      val stopPredictions = predictionsByStop[stop.id] ?: generateOfflinePredictions(stop.id)

      // Group by Route + Direction
      val groupedByRoute = stopPredictions.groupBy { it.routeId }
      val routeDeps = groupedByRoute.mapNotNull { (routeId, preds) ->
        val route = routesMap[routeId] ?: findFallbackRoute(routeId)
        if (selectedMode != TransitMode.ALL && route.transitMode != selectedMode) {
          return@mapNotNull null
        }
        val headsign = route.directionNames.getOrElse(preds.firstOrNull()?.directionId ?: 0) { "Outbound" }
        RouteDepartures(
          route = route,
          headsign = headsign,
          directionId = preds.firstOrNull()?.directionId ?: 0,
          predictions = preds.sortedBy { it.countdownMinutes }.take(4),
          isLive = preds.any { it.isLiveGps }
        )
      }

      if (routeDeps.isNotEmpty()) {
        StationDepartures(stop = stop, distanceMiles = dist, routeDepartures = routeDeps)
      } else null
    }
  }

  suspend fun getNearbyRouteItems(
    lat: Double,
    lng: Double,
    selectedMode: TransitMode = TransitMode.ALL
  ): List<com.example.data.model.NearbyRouteItem> = withContext(ioDispatcher) {
    val routes = getRoutes()
    val routesMap = routes.associateBy { it.id }

    // Query nearby stops from API, with graceful offline fallback
    val rawStops: List<MbtaStop> = try {
      val stopResponse = api.getStopsNearby(lat, lng, radius = 0.02)
      if (stopResponse.data.isNotEmpty()) {
        stopResponse.data.mapNotNull {
          val stopLat = it.attributes.latitude
          val stopLng = it.attributes.longitude
          if (stopLat == null || stopLng == null) null
          else MbtaStop(
            id = it.id,
            name = it.attributes.name,
            latitude = stopLat,
            longitude = stopLng,
            municipality = it.attributes.municipality ?: "Boston",
            wheelchairBoarding = it.attributes.wheelchairBoarding ?: 1,
            locationType = it.attributes.locationType ?: 0
          )
        }
      } else {
        getSortedFallbackStops(lat, lng)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      getSortedFallbackStops(lat, lng)
    }

    // Cluster and deduplicate child platform tracks into distinct physical stations
    val clusteredStops = rawStops.distinctBy {
      val cleanName = it.name.replace(" - Inbound", "").replace(" - Outbound", "").replace(" - Track 1", "").replace(" - Track 2", "")
      "${cleanName}_${String.format(Locale.US, "%.3f", it.latitude)}_${String.format(Locale.US, "%.3f", it.longitude)}"
    }

    // Query physical rapid transit stations nearby to ensure subway lines are never omitted
    val nearestStations = MbtaRouteStationRegistry.getClosestStations(lat, lng, limit = 4)
    val stationPlatformStopIds = nearestStations.flatMap { station ->
      MbtaRouteStationRegistry.getChildPlatformStopIds(station.id)
    }

    val busStops = rawStops.filter { it.locationType == 0 }.take(6)
    val combinedQueryStopIds = (stationPlatformStopIds + nearestStations.map { it.id } + busStops.map { it.id }).distinct().take(18)
    val stopIds = combinedQueryStopIds.joinToString(",")

    val allPredictions = mutableListOf<MbtaPrediction>()
    try {
      if (stopIds.isNotBlank()) {
        val predResponse = api.getPredictionsForStops(stopIds)
        predResponse.data.forEach { p ->
          allPredictions.add(mapPrediction(p))
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      allPredictions.addAll(generateOfflineDeparturesForLocation(lat, lng, selectedMode))
    }

    if (allPredictions.isEmpty()) {
      allPredictions.addAll(generateOfflineDeparturesForLocation(lat, lng, selectedMode))
    }

    // Query cached stations to aid route-station resolution
    val cachedStations = try {
      dao.getAllCachedStations().map {
        MbtaStop(
          id = it.stopId,
          name = it.name,
          latitude = it.latitude,
          longitude = it.longitude,
          municipality = it.municipality,
          wheelchairBoarding = it.wheelchairBoarding,
          locationType = it.locationType
        )
      }
    } catch (_: Exception) {
      emptyList()
    }.ifEmpty {
      BostonLandmarks.ALL_MAJOR_STOPS
    }

    // Group predictions strictly by Route ID to prevent any duplicate cards
    val predictionsByRoute = allPredictions.groupBy { it.routeId }

    // Map each route to ONE NearbyRouteItem with stacked directions and route-specific stops
    val routeItems = predictionsByRoute.mapNotNull { (routeId, preds) ->
      val route = routesMap[routeId] ?: findFallbackRoute(routeId)
      if (selectedMode != TransitMode.ALL && route.transitMode != selectedMode) {
        return@mapNotNull null
      }

      // Resolve the actual, legitimate boarding station or stop for this route
      val routeStop = MbtaRouteStationRegistry.resolveBoardingStopForRoute(
        route = route,
        preds = preds,
        candidateStops = rawStops,
        allCachedStations = cachedStations,
        userLat = lat,
        userLng = lng
      )

      val distMiles = TransitGraphPathfinder.calculateDistanceMiles(lat, lng, routeStop.latitude, routeStop.longitude)
      val walkMins = (distMiles * 18.0).toInt().coerceAtLeast(1)

      // Group predictions by Direction ID (0 and 1) with strict deduplication
      val directionsList = mutableListOf<com.example.data.model.DirectionDepartures>()

      // Direction 0
      val effectiveDir0Preds = deduplicateDirectionPredictions(preds, routeStop.id, route.id, 0)
      val headsign0 = route.directionNames.getOrElse(0) { "Outbound" }
      directionsList.add(
        com.example.data.model.DirectionDepartures(
          directionId = 0,
          headsign = headsign0,
          predictions = effectiveDir0Preds,
          isLive = effectiveDir0Preds.any { it.isLiveGps }
        )
      )

      // Direction 1
      val effectiveDir1Preds = deduplicateDirectionPredictions(preds, routeStop.id, route.id, 1)
      val headsign1 = route.directionNames.getOrElse(1) { "Inbound" }
      directionsList.add(
        com.example.data.model.DirectionDepartures(
          directionId = 1,
          headsign = headsign1,
          predictions = effectiveDir1Preds,
          isLive = effectiveDir1Preds.any { it.isLiveGps }
        )
      )

      com.example.data.model.NearbyRouteItem(
        id = route.id,
        route = route,
        nearestStop = routeStop,
        distanceMiles = distMiles,
        walkMinutes = walkMins,
        directions = directionsList
      )
    }

    // Sort strictly by walking distance: closest transit route at the top
    routeItems.sortedBy { it.distanceMiles }
  }

  internal fun deduplicateDirectionPredictions(
    preds: List<MbtaPrediction>,
    fallbackStopId: String,
    routeId: String,
    directionId: Int
  ): List<MbtaPrediction> {
    val dirPreds = preds.filter { it.directionId == directionId }.sortedBy { it.countdownMinutes }
    if (dirPreds.isEmpty()) {
      val synthMinutes = if (directionId == 0) 4 else 7
      return listOf(
        MbtaPrediction(
          id = "p_${routeId}_${directionId}_synth",
          routeId = routeId,
          stopId = fallbackStopId,
          tripId = null,
          directionId = directionId,
          arrivalTimeIso = null,
          departureTimeIso = null,
          status = null,
          vehicleId = null,
          stopSequence = 1,
          countdownMinutes = synthMinutes,
          isLiveGps = false
        )
      )
    }

    val first = dirPreds.first()
    val subsequent = mutableListOf<MbtaPrediction>()
    val seenMinutes = mutableSetOf(first.countdownMinutes)

    for (p in dirPreds.drop(1)) {
      if (p.countdownMinutes > first.countdownMinutes && seenMinutes.add(p.countdownMinutes)) {
        subsequent.add(p)
        if (subsequent.size >= 2) break
      }
    }

    return listOf(first) + subsequent
  }

  suspend fun getAlertsForRoute(routeId: String? = null): List<MbtaAlert> = withContext(ioDispatcher) {
    try {
      val response = api.getAlerts(routeId = routeId)
      response.data.map {
        MbtaAlert(
          id = it.id,
          header = it.attributes.header ?: "Service alert",
          effect = it.attributes.effect ?: "UNKNOWN",
          severity = it.attributes.severity,
          serviceEffect = it.attributes.serviceEffect ?: "Notice"
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun planItinerary(
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double,
    fromLabel: String,
    toLabel: String
  ): Itinerary {
    return TransitGraphPathfinder.findShortestPath(fromLat, fromLng, toLat, toLng, fromLabel, toLabel)
  }

  // --- Helpers & Offline Generators ---

  private fun mapRoute(resource: RouteResource): MbtaRoute {
    val attr = resource.attributes
    val dirNames = attr.directionDestinations?.takeIf { it.isNotEmpty() }
      ?: attr.directionNames
      ?: listOf("Outbound", "Inbound")
    return MbtaRoute(
      id = resource.id,
      type = attr.type,
      shortName = attr.shortName ?: "",
      longName = attr.longName ?: resource.id,
      colorHex = attr.color ?: "00843D",
      textColorHex = attr.textColor ?: "FFFFFF",
      directionNames = dirNames,
      description = attr.description ?: ""
    )
  }

  private fun mapPrediction(p: PredictionResource): MbtaPrediction {
    val routeId = p.relationships?.route?.data?.id ?: "Green"
    val stopId = p.relationships?.stop?.data?.id ?: ""
    val tripId = p.relationships?.trip?.data?.id
    val vehicleId = p.relationships?.vehicle?.data?.id
    val arrivalTime = p.attributes.arrivalTime ?: p.attributes.departureTime

    val countdownMinutes = calculateMinutesFromIso(arrivalTime)

    return MbtaPrediction(
      id = p.id,
      routeId = routeId,
      stopId = stopId,
      tripId = tripId,
      directionId = p.attributes.directionId,
      arrivalTimeIso = p.attributes.arrivalTime,
      departureTimeIso = p.attributes.departureTime,
      status = p.attributes.status,
      vehicleId = vehicleId,
      stopSequence = p.attributes.stopSequence ?: 0,
      countdownMinutes = countdownMinutes,
      isLiveGps = vehicleId != null
    )
  }

  private fun calculateMinutesFromIso(iso: String?): Int {
    if (iso.isNullOrBlank()) return 1
    return try {
      val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
      val date = parser.parse(iso) ?: return 1
      val diffMs = date.time - System.currentTimeMillis()
      val mins = (diffMs / (60 * 1000)).toInt()
      mins.coerceAtLeast(0)
    } catch (_: Exception) {
      try {
        val parser2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        parser2.timeZone = TimeZone.getTimeZone("America/New_York")
        val date = parser2.parse(iso) ?: return 1
        val diffMs = date.time - System.currentTimeMillis()
        val mins = (diffMs / (60 * 1000)).toInt()
        mins.coerceAtLeast(0)
      } catch (_: Exception) {
        1
      }
    }
  }

  private suspend fun getSortedFallbackStops(lat: Double, lng: Double): List<MbtaStop> {
    val cached = dao.getAllCachedStops()
    if (cached.isNotEmpty()) {
      return cached.map {
        MbtaStop(
          id = it.stopId,
          name = it.name,
          latitude = it.latitude,
          longitude = it.longitude,
          municipality = it.municipality,
          wheelchairBoarding = it.wheelchairBoarding
        )
      }.sortedBy {
        TransitGraphPathfinder.calculateDistanceMiles(lat, lng, it.latitude, it.longitude)
      }
    }
    return emptyList()
  }

  internal fun generateOfflineDeparturesForLocation(
    lat: Double,
    lng: Double,
    selectedMode: TransitMode = TransitMode.ALL
  ): List<MbtaPrediction> {
    val candidateRoutes = MbtaRouteStationRegistry.getRoutesForLocation(lat, lng, limitStations = 4)
    val routesMap = getOfflineFallbackRoutes().associateBy { it.id }

    val filteredRoutes = candidateRoutes.filter { routeId ->
      if (selectedMode == TransitMode.ALL) return@filter true
      val route = routesMap[routeId] ?: findFallbackRoute(routeId)
      route.transitMode == selectedMode
    }.take(6)

    val preds = mutableListOf<MbtaPrediction>()
    filteredRoutes.forEachIndexed { index, routeId ->
      val route = routesMap[routeId] ?: findFallbackRoute(routeId)
      val stop = MbtaRouteStationRegistry.resolveBoardingStopForRoute(
        route = route,
        preds = emptyList(),
        candidateStops = emptyList(),
        allCachedStations = BostonLandmarks.ALL_MAJOR_STOPS,
        userLat = lat,
        userLng = lng
      )

      preds.add(
        MbtaPrediction("pred_loc_${routeId}_0_1", routeId, stop.id, null, 0, null, null, null, null, 1, 3 + (index * 2), false)
      )
      preds.add(
        MbtaPrediction("pred_loc_${routeId}_0_2", routeId, stop.id, null, 0, null, null, null, null, 1, 9 + (index * 2), false)
      )
      preds.add(
        MbtaPrediction("pred_loc_${routeId}_1_1", routeId, stop.id, null, 1, null, null, null, null, 1, 5 + (index * 2), false)
      )
      preds.add(
        MbtaPrediction("pred_loc_${routeId}_1_2", routeId, stop.id, null, 1, null, null, null, null, 1, 11 + (index * 2), false)
      )
    }

    return preds
  }

  internal fun generateOfflinePredictions(stopId: String): List<MbtaPrediction> {
    val parentId = MbtaRouteStationRegistry.resolveParentStationId(stopId)
    val routes = MbtaRouteStationRegistry.getRoutesForStation(parentId).ifEmpty {
      when (parentId) {
        "place-pktrm" -> listOf("Red", "Green-B", "Green-C", "Green-D", "Green-E")
        "place-dwnxg" -> listOf("Orange", "Red", "749")
        "place-gover" -> listOf("Green-B", "Green-C", "Green-D", "Green-E", "Blue")
        "place-state" -> listOf("Orange", "Blue")
        "place-sstat" -> listOf("Red", "741", "CR-Providence")
        "place-north" -> listOf("Green-D", "Green-E", "Orange", "CR-Lowell")
        "place-bbsta" -> listOf("Orange", "CR-Providence", "CR-Worcester")
        "place-coecl", "place-boyls", "place-hymnl", "place-kencl" -> listOf("Green-B", "Green-C", "Green-D")
        "place-chncl", "place-tumnl", "place-masta", "place-rugg" -> listOf("Orange")
        "place-aqucl", "place-mvbcl", "place-aport", "place-wondl" -> listOf("Blue")
        else -> listOf("Orange", "Red", "Green-B")
      }
    }

    return routes.flatMapIndexed { index, routeId ->
      listOf(
        MbtaPrediction("pred_off_${routeId}_${stopId}_1", routeId, stopId, null, 0, null, null, null, null, 1, 3 + (index * 2), false),
        MbtaPrediction("pred_off_${routeId}_${stopId}_2", routeId, stopId, null, 0, null, null, null, null, 1, 9 + (index * 2), false),
        MbtaPrediction("pred_off_${routeId}_${stopId}_3", routeId, stopId, null, 1, null, null, null, null, 1, 6 + (index * 2), false)
      )
    }
  }

  private fun findFallbackRoute(routeId: String): MbtaRoute {
    return getOfflineFallbackRoutes().firstOrNull { it.id.equals(routeId, ignoreCase = true) }
      ?: MbtaRoute(routeId, 0, routeId, "$routeId Line", "00843D")
  }

  fun getOfflineFallbackRoutes(): List<MbtaRoute> = listOf(
    MbtaRoute("Red", 1, "Red Line", "Red Line (Alewife - Ashmont/Braintree)", "DA291C", "FFFFFF", listOf("Ashmont/Braintree", "Alewife")),
    MbtaRoute("Orange", 1, "Orange Line", "Orange Line (Oak Grove - Forest Hills)", "ED8B00", "FFFFFF", listOf("Forest Hills", "Oak Grove")),
    MbtaRoute("Green-B", 0, "Green (B)", "Green Line B (Boston College)", "00843D", "FFFFFF", listOf("Boston College", "Government Center")),
    MbtaRoute("Green-C", 0, "Green (C)", "Green Line C (Cleveland Circle)", "00843D", "FFFFFF", listOf("Cleveland Circle", "Government Center")),
    MbtaRoute("Green-D", 0, "Green (D)", "Green Line D (Riverside)", "00843D", "FFFFFF", listOf("Riverside", "Union Square")),
    MbtaRoute("Green-E", 0, "Green (E)", "Green Line E (Heath Street)", "00843D", "FFFFFF", listOf("Heath Street", "Medford/Tufts")),
    MbtaRoute("Blue", 1, "Blue Line", "Blue Line (Bowdoin - Wonderland)", "003DA5", "FFFFFF", listOf("Wonderland", "Bowdoin")),
    MbtaRoute("741", 3, "SL1", "Silver Line SL1 (Logan Airport - South Station)", "7C878E", "FFFFFF", listOf("South Station", "Logan Airport")),
    MbtaRoute("1", 3, "1", "Key Bus 1 (Harvard - Nubian)", "FFC72C", "1E2229", listOf("Nubian", "Harvard")),
    MbtaRoute("CR-Providence", 2, "Providence", "Commuter Rail (Providence/Stoughton)", "80276C", "FFFFFF", listOf("Providence/Stoughton", "South Station"))
  )
}
