package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ActiveTripSheet
import com.example.ui.components.NowTrackingMiniCard
import com.example.ui.viewmodel.TransitViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle

enum class MainNavTab {
  NEARBY,
  LINES,
  FAVORITES
}

@Composable
fun MainScreen(
  viewModel: TransitViewModel,
  modifier: Modifier = Modifier
) {
  var currentTab by remember { mutableStateOf(MainNavTab.NEARBY) }
  var showSettings by remember { mutableStateOf(false) }
  var showActiveTripSheet by remember { mutableStateOf(false) }

  val nearbyState by viewModel.nearbyState.collectAsStateWithLifecycle()
  val tripPlannerState by viewModel.tripPlannerState.collectAsStateWithLifecycle()
  val linesState by viewModel.linesState.collectAsStateWithLifecycle()
  val activeTracking by viewModel.activeTracking.collectAsStateWithLifecycle()
  val favoriteStops by viewModel.favoriteStops.collectAsStateWithLifecycle()
  val stopSyncState by viewModel.stopSyncState.collectAsStateWithLifecycle()
  val cachedStopCount by viewModel.cachedStopCount.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val apiKey by viewModel.userApiKey.collectAsStateWithLifecycle()
  val syncCooldownSeconds by viewModel.syncCooldownSeconds.collectAsStateWithLifecycle()
  val refreshCooldownSeconds by viewModel.refreshCooldownSeconds.collectAsStateWithLifecycle()

  val context = androidx.compose.ui.platform.LocalContext.current

  // Notification permission launcher for Android 13+
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { /* granted / denied handled gracefully */ }

  // Location permission launcher for auto-detecting real device GPS
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { _ ->
    viewModel.refreshLocationAndNearby(context, forceGps = true)
  }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    locationPermissionLauncher.launch(
      arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
      )
    )
    viewModel.refreshLocationAndNearby(context)
    viewModel.startAutoRefresh()
  }

  Scaffold(
    topBar = {
      // Top Material 3 Search Bar ("Where to? Search address or station...") + Settings
      Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          // Expressive Search Anchor Bar
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .clickable { viewModel.openTripPlanner() }
              .padding(horizontal = 16.dp)
              .testTag("top_search_bar_trigger")
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Where to? Search address or stop...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(
            onClick = { showSettings = true },
            modifier = Modifier.testTag("settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings and Hub Selector",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    },
    bottomBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
      ) {
        // Docked "Now Tracking" Live Mini-Card
        NowTrackingMiniCard(
          state = activeTracking,
          onCardClick = { showActiveTripSheet = true },
          onStopTracking = { viewModel.stopLiveTracking() }
        )

        // Decluttered 3-Tab Bottom NavigationBar
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 3.dp,
          modifier = Modifier.testTag("bottom_navigation_bar")
        ) {
          NavigationBarItem(
            selected = currentTab == MainNavTab.NEARBY,
            onClick = { currentTab = MainNavTab.NEARBY },
            icon = {
              Icon(
                imageVector = if (currentTab == MainNavTab.NEARBY) Icons.Filled.DirectionsTransit else Icons.Outlined.DirectionsTransit,
                contentDescription = "Nearby"
              )
            },
            label = { Text("Nearby", fontWeight = if (currentTab == MainNavTab.NEARBY) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.testTag("tab_nearby")
          )

          NavigationBarItem(
            selected = currentTab == MainNavTab.LINES,
            onClick = { currentTab = MainNavTab.LINES },
            icon = {
              Icon(
                imageVector = if (currentTab == MainNavTab.LINES) Icons.Filled.Map else Icons.Outlined.Map,
                contentDescription = "Lines"
              )
            },
            label = { Text("Lines", fontWeight = if (currentTab == MainNavTab.LINES) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.testTag("tab_lines")
          )

          NavigationBarItem(
            selected = currentTab == MainNavTab.FAVORITES,
            onClick = { currentTab = MainNavTab.FAVORITES },
            icon = {
              Icon(
                imageVector = if (currentTab == MainNavTab.FAVORITES) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favorites"
              )
            },
            label = { Text("Favorites", fontWeight = if (currentTab == MainNavTab.FAVORITES) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.testTag("tab_favorites")
          )
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (currentTab) {
        MainNavTab.NEARBY -> {
          NearbyScreen(
            state = nearbyState,
            refreshCooldownSeconds = refreshCooldownSeconds,
            onModeSelected = { viewModel.setTransitMode(it) },
            onRefresh = { viewModel.manualRefreshDepartures() },
            onGpsRecenter = { viewModel.refreshLocationAndNearby(context, forceGps = true) },
            onCustomLocationSelected = { lat, lng, label ->
              viewModel.setCustomLocation(lat, lng, label)
            },
            onTrackLive = { routeName, routeColorHex, dest, origin, mins ->
              viewModel.startTrackingDeparture(routeName, routeColorHex, dest, origin, mins)
            }
          )
        }
        MainNavTab.LINES -> {
          LinesScreen(
            state = linesState,
            onRouteClick = { route ->
              // Trigger route-specific view or planner
              viewModel.openTripPlanner()
            }
          )
        }
        MainNavTab.FAVORITES -> {
          FavoritesScreen(
            favorites = favoriteStops,
            onRemoveFavorite = { viewModel.removeFavorite(it) },
            onSelectStation = { stop ->
              viewModel.selectStationPreset(stop)
              currentTab = MainNavTab.NEARBY
            }
          )
        }
      }
    }
  }

  // Active Trip Bottom Sheet modal
  if (showActiveTripSheet) {
    ActiveTripSheet(
      state = activeTracking,
      onDismiss = { showActiveTripSheet = false },
      onStopTracking = {
        viewModel.stopLiveTracking()
        showActiveTripSheet = false
      }
    )
  }

  // Trip Planner & Address Search Sheet modal
  if (tripPlannerState.isSheetOpen) {
    TripPlannerSheet(
      state = tripPlannerState,
      currentOriginName = nearbyState.currentStationName,
      onQueryChange = { viewModel.updateSearchQuery(it) },
      onDestinationSelected = { dest ->
        viewModel.selectDestination(dest)
      },
      onDirectSearchSubmit = { query ->
        viewModel.searchAddressDirectly(query)
      },
      onStartNewSearch = {
        viewModel.startNewSearch()
      },
      onStartTrackingItinerary = { itin ->
        viewModel.startTrackingItinerary(itin)
      },
      onDismiss = { viewModel.closeTripPlanner() }
    )
  }

  // Settings & Offline Database Dialog
  if (showSettings) {
    SettingsDialog(
      currentTheme = themeMode,
      cachedStopCount = cachedStopCount,
      syncState = stopSyncState,
      currentApiKey = apiKey,
      syncCooldownSeconds = syncCooldownSeconds,
      onThemeSelected = { viewModel.setThemeMode(it) },
      onSyncStops = { viewModel.syncStopsNow() },
      onApiKeySaved = { key ->
        viewModel.setCustomApiKey(key)
        showSettings = false
      },
      onDismiss = { showSettings = false }
    )
  }
}
