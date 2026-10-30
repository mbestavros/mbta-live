package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.MbtaApiClient
import com.example.data.graph.TransitGraphPathfinder
import com.example.data.location.GeocodedLocation
import com.example.data.location.GeocoderHelper
import com.example.data.location.LocationHelper
import com.example.data.model.DirectionDepartures
import com.example.data.model.Itinerary
import com.example.data.model.MbtaAlert
import com.example.data.model.MbtaRoute
import com.example.data.model.MbtaStop
import com.example.data.model.NearbyRouteItem
import com.example.data.model.StationDepartures
import com.example.data.model.TransitMode
import com.example.data.repository.StopSyncState
import com.example.data.repository.TransitRepository
import com.example.service.ActiveTrackingState
import com.example.service.LiveTrackingService
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NearbyUiState(
  val stations: List<StationDepartures> = emptyList(),
  val nearbyRoutes: List<NearbyRouteItem> = emptyList(),
  val isLoading: Boolean = false,
  val selectedMode: TransitMode = TransitMode.ALL,
  val currentStationName: String = "Park Street",
  val locationLabel: String = "Boston Common / Downtown",
  val isGpsActive: Boolean = false,
  val isLocationCustomized: Boolean = false,
  val error: String? = null,
  val isRateLimited: Boolean = false
)

data class TripPlannerUiState(
  val searchQuery: String = "",
  val searchSuggestions: List<GeocodedLocation> = emptyList(),
  val selectedDestination: GeocodedLocation? = null,
  val plannedItinerary: Itinerary? = null,
  val isSearching: Boolean = true,
  val isSheetOpen: Boolean = false,
  val isCalculating: Boolean = false,
  val errorMessage: String? = null
)

data class LinesUiState(
  val routes: List<MbtaRoute> = emptyList(),
  val alerts: List<MbtaAlert> = emptyList(),
  val selectedRoute: MbtaRoute? = null,
  val isLoading: Boolean = false
)

class TransitViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = TransitRepository(application)

  private val _nearbyState = MutableStateFlow(NearbyUiState())
  val nearbyState: StateFlow<NearbyUiState> = _nearbyState.asStateFlow()

  private val _tripPlannerState = MutableStateFlow(TripPlannerUiState())
  val tripPlannerState: StateFlow<TripPlannerUiState> = _tripPlannerState.asStateFlow()

  private val _linesState = MutableStateFlow(LinesUiState())
  val linesState: StateFlow<LinesUiState> = _linesState.asStateFlow()

  private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
  val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

  private val _userApiKey = MutableStateFlow("")
  val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

  val activeTracking: StateFlow<ActiveTrackingState> = LiveTrackingService.trackingState

  val favoriteStops = repository.favoriteStops.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  val stopSyncState: StateFlow<StopSyncState> = repository.stopSyncState
  val cachedStopCount: StateFlow<Int> = repository.cachedStopCount.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    0
  )

  private val _syncCooldownSeconds = MutableStateFlow(0)
  val syncCooldownSeconds: StateFlow<Int> = _syncCooldownSeconds.asStateFlow()

  private val _refreshCooldownSeconds = MutableStateFlow(0)
  val refreshCooldownSeconds: StateFlow<Int> = _refreshCooldownSeconds.asStateFlow()

  private var currentLat = LocationHelper.DEFAULT_BOSTON_LAT
  private var currentLng = LocationHelper.DEFAULT_BOSTON_LNG

  private var autoRefreshJob: Job? = null
  private var refreshDeparturesJob: Job? = null
  private var searchDebounceJob: Job? = null
  private var syncTimerJob: Job? = null
  private var refreshTimerJob: Job? = null

  init {
    viewModelScope.launch {
      repository.seedStopsIfEmpty()
      loadRoutes()
      refreshNearbyDepartures()
      repository.syncAllStops(forceRefresh = false)
    }
  }

  fun syncStopsNow() {
    if (_syncCooldownSeconds.value > 0) return
    _syncCooldownSeconds.value = 15
    viewModelScope.launch {
      repository.syncAllStops(forceRefresh = true)
    }
    syncTimerJob?.cancel()
    syncTimerJob = viewModelScope.launch {
      while (_syncCooldownSeconds.value > 0) {
        delay(1000)
        _syncCooldownSeconds.value -= 1
      }
    }
  }

  fun setThemeMode(mode: AppThemeMode) {
    _themeMode.value = mode
  }

  fun setCustomApiKey(key: String) {
    _userApiKey.value = key
    MbtaApiClient.customApiKey = key
    refreshNearbyDepartures()
  }

  fun refreshLocationAndNearby(context: android.content.Context, forceGps: Boolean = false) {
    viewModelScope.launch {
      val isCustom = _nearbyState.value.isLocationCustomized
      if (isCustom && !forceGps) {
        refreshNearbyDepartures()
        return@launch
      }

      _nearbyState.value = _nearbyState.value.copy(isLoading = true)
      val userLoc = LocationHelper.getDeviceLocation(context)

      // If forceGps is false and GPS is not active, do not overwrite if user previously selected a custom location
      if (!userLoc.isGpsActive && isCustom && !forceGps) {
        refreshNearbyDepartures()
        return@launch
      }

      currentLat = userLoc.latitude
      currentLng = userLoc.longitude
      _nearbyState.value = _nearbyState.value.copy(
        locationLabel = userLoc.displayName,
        isGpsActive = userLoc.isGpsActive,
        isLocationCustomized = if (forceGps) false else isCustom
      )
      refreshNearbyDepartures()
    }
  }

  fun setCustomLocation(lat: Double, lng: Double, label: String) {
    currentLat = lat
    currentLng = lng
    _nearbyState.value = _nearbyState.value.copy(
      locationLabel = label,
      isGpsActive = false,
      isLocationCustomized = true
    )
    refreshNearbyDepartures()
  }

  fun selectStationPreset(stop: MbtaStop) {
    currentLat = stop.latitude
    currentLng = stop.longitude
    _nearbyState.value = _nearbyState.value.copy(
      currentStationName = stop.name,
      locationLabel = "Near ${stop.name}, ${stop.municipality}",
      isGpsActive = false,
      isLocationCustomized = true
    )
    refreshNearbyDepartures()
  }

  fun setTransitMode(mode: TransitMode) {
    _nearbyState.value = _nearbyState.value.copy(selectedMode = mode)
    refreshNearbyDepartures()
  }

  /**
   * Manual refresh requested by user with 5-second cooldown to protect MBTA rate limits.
   */
  fun manualRefreshDepartures() {
    if (_refreshCooldownSeconds.value > 0) return
    _refreshCooldownSeconds.value = 5
    refreshTimerJob?.cancel()
    refreshTimerJob = viewModelScope.launch {
      while (_refreshCooldownSeconds.value > 0) {
        delay(1000)
        _refreshCooldownSeconds.value -= 1
      }
    }
    refreshNearbyDepartures()
  }

  fun refreshNearbyDepartures() {
    // Cancel superseded departure fetches to eliminate concurrency race conditions
    refreshDeparturesJob?.cancel()
    refreshDeparturesJob = viewModelScope.launch {
      _nearbyState.value = _nearbyState.value.copy(isLoading = true, error = null, isRateLimited = false)
      try {
        val targetMode = _nearbyState.value.selectedMode
        val nearbyRoutes = repository.getNearbyRouteItems(
          currentLat,
          currentLng,
          targetMode
        )
        val stations = repository.getNearbyStations(
          currentLat,
          currentLng,
          targetMode
        )
        val label = if (_nearbyState.value.locationLabel.isBlank()) {
          LocationHelper.getNeighborhoodLabel(currentLat, currentLng)
        } else {
          _nearbyState.value.locationLabel
        }
        _nearbyState.value = _nearbyState.value.copy(
          nearbyRoutes = nearbyRoutes,
          stations = stations,
          locationLabel = label,
          isLoading = false
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val isRateLimit = e.message?.contains("429") == true
        _nearbyState.value = _nearbyState.value.copy(
          isLoading = false,
          isRateLimited = isRateLimit,
          error = if (isRateLimit) "MBTA API rate limit reached. Using offline schedule." else (e.localizedMessage ?: "Failed to refresh departures")
        )
      }
    }
  }

  private fun loadRoutes() {
    viewModelScope.launch {
      _linesState.value = _linesState.value.copy(isLoading = true)
      val routes = repository.getRoutes()
      val alerts = repository.getAlertsForRoute()
      _linesState.value = _linesState.value.copy(
        routes = routes,
        alerts = alerts,
        isLoading = false
      )
    }
  }

  fun toggleFavorite(stop: MbtaStop, lineName: String, lineColor: String) {
    viewModelScope.launch {
      repository.toggleFavorite(stop, lineName, lineColor)
    }
  }

  fun removeFavorite(stopId: String) {
    viewModelScope.launch {
      repository.removeFavorite(stopId)
    }
  }

  // --- Search & Trip Planner ---

  fun openTripPlanner() {
    _tripPlannerState.value = _tripPlannerState.value.copy(
      isSheetOpen = true,
      isSearching = _tripPlannerState.value.plannedItinerary == null,
      errorMessage = null
    )
    loadDefaultSuggestions()
  }

  fun closeTripPlanner() {
    _tripPlannerState.value = _tripPlannerState.value.copy(isSheetOpen = false)
  }

  private fun loadDefaultSuggestions() {
    viewModelScope.launch {
      val suggestions = mutableListOf<GeocodedLocation>()
      // 1. Add user's favorited stations
      favoriteStops.value.forEach { fav ->
        suggestions.add(
          GeocodedLocation(
            label = fav.stopName,
            address = "Saved Favorite • ${fav.primaryLineName}",
            latitude = fav.latitude,
            longitude = fav.longitude,
            isLandmark = true
          )
        )
      }
      // 2. Add nearby cached stops from current location
      val nearbyStops = repository.searchCachedStops("", limit = 12)
      nearbyStops.sortedBy {
        TransitGraphPathfinder.calculateDistanceMiles(currentLat, currentLng, it.latitude, it.longitude)
      }.take(6).forEach { stop ->
        if (suggestions.none { it.label.equals(stop.name, ignoreCase = true) }) {
          suggestions.add(
            GeocodedLocation(
              label = stop.name,
              address = "Nearby MBTA Stop • ${stop.municipality}",
              latitude = stop.latitude,
              longitude = stop.longitude,
              isLandmark = true
            )
          )
        }
      }
      _tripPlannerState.value = _tripPlannerState.value.copy(searchSuggestions = suggestions)
    }
  }

  fun updateSearchQuery(query: String) {
    _tripPlannerState.value = _tripPlannerState.value.copy(
      searchQuery = query,
      isSearching = true,
      errorMessage = null
    )
    searchDebounceJob?.cancel()
    searchDebounceJob = viewModelScope.launch {
      if (query.isBlank()) {
        loadDefaultSuggestions()
      } else {
        val results = GeocoderHelper.geocodeQuery(getApplication(), query)
        _tripPlannerState.value = _tripPlannerState.value.copy(searchSuggestions = results)
      }
    }
  }

  fun selectDestination(dest: GeocodedLocation) {
    _tripPlannerState.value = _tripPlannerState.value.copy(
      selectedDestination = dest,
      searchQuery = dest.label,
      isSearching = false,
      isCalculating = true,
      errorMessage = null
    )
    planTrip(dest)
  }

  fun searchAddressDirectly(query: String) {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return
    searchDebounceJob?.cancel()
    searchDebounceJob = viewModelScope.launch {
      _tripPlannerState.value = _tripPlannerState.value.copy(
        isCalculating = true,
        isSearching = false,
        errorMessage = null
      )
      val location = GeocoderHelper.resolveLocation(getApplication(), trimmed)
      if (location != null) {
        selectDestination(location)
      } else {
        _tripPlannerState.value = _tripPlannerState.value.copy(
          isCalculating = false,
          isSearching = true,
          errorMessage = "Could not locate \"$trimmed\". Try an MBTA station name or full address."
        )
      }
    }
  }

  fun startNewSearch() {
    _tripPlannerState.value = _tripPlannerState.value.copy(
      isSearching = true,
      searchQuery = "",
      errorMessage = null
    )
  }

  fun planTripToStop(stop: MbtaStop) {
    val dest = GeocodedLocation(
      label = stop.name,
      address = "MBTA Station • ${stop.municipality}",
      latitude = stop.latitude,
      longitude = stop.longitude,
      isLandmark = true
    )
    _tripPlannerState.value = _tripPlannerState.value.copy(
      isSheetOpen = true,
      selectedDestination = dest,
      isSearching = false,
      isCalculating = true
    )
    planTrip(dest)
  }

  private fun planTrip(dest: GeocodedLocation) {
    viewModelScope.launch {
      val itinerary = repository.planItinerary(
        fromLat = currentLat,
        fromLng = currentLng,
        toLat = dest.latitude,
        toLng = dest.longitude,
        fromLabel = _nearbyState.value.currentStationName,
        toLabel = dest.label
      )
      _tripPlannerState.value = _tripPlannerState.value.copy(
        plannedItinerary = itinerary,
        isCalculating = false,
        isSearching = false
      )
    }
  }

  // --- Live Tracking ---

  fun startTrackingDeparture(
    routeName: String,
    routeColorHex: String,
    destination: String,
    originStop: String,
    countdownMinutes: Int
  ) {
    LiveTrackingService.startTracking(
      context = getApplication(),
      routeName = routeName,
      routeColorHex = routeColorHex,
      destination = destination,
      originStop = originStop,
      durationMinutes = countdownMinutes.coerceAtLeast(1)
    )
  }

  fun startTrackingItinerary(itinerary: Itinerary) {
    val firstTransitLeg = itinerary.legs.firstOrNull { it.legType != com.example.data.model.LegType.WALK }
      ?: itinerary.legs.firstOrNull()

    val routeName = firstTransitLeg?.routeName ?: "Boston Transit"
    val routeColor = firstTransitLeg?.routeColorHex ?: "00843D"
    val dest = itinerary.toName
    val origin = itinerary.fromName
    val duration = itinerary.totalDurationMinutes

    LiveTrackingService.startTracking(
      context = getApplication(),
      routeName = routeName,
      routeColorHex = routeColor,
      destination = dest,
      originStop = origin,
      durationMinutes = duration
    )
    closeTripPlanner()
  }

  fun stopLiveTracking() {
    LiveTrackingService.stopTracking(getApplication())
  }

  fun startAutoRefresh() {
    autoRefreshJob?.cancel()
    autoRefreshJob = viewModelScope.launch {
      while (isActive) {
        delay(45000) // Polite 45s periodic refresh to stay fresh and respect public rate limit
        refreshNearbyDepartures()
      }
    }
  }

  fun stopAutoRefresh() {
    autoRefreshJob?.cancel()
    autoRefreshJob = null
  }

  override fun onCleared() {
    super.onCleared()
    autoRefreshJob?.cancel()
    refreshDeparturesJob?.cancel()
    searchDebounceJob?.cancel()
    syncTimerJob?.cancel()
    refreshTimerJob?.cancel()
  }
}
