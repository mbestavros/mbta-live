package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.GeocodedLocation
import com.example.data.location.GeocoderHelper
import com.example.data.model.TransitMode
import com.example.ui.components.NearbyRouteCard
import com.example.ui.theme.TransitLiveGreen
import com.example.ui.viewmodel.NearbyUiState
import kotlinx.coroutines.launch

@Composable
fun NearbyScreen(
  state: NearbyUiState,
  refreshCooldownSeconds: Int = 0,
  onModeSelected: (TransitMode) -> Unit,
  onRefresh: () -> Unit,
  onGpsRecenter: () -> Unit,
  onCustomLocationSelected: (lat: Double, lng: Double, label: String) -> Unit,
  onTrackLive: (routeName: String, routeColorHex: String, destination: String, originStop: String, minutes: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var showLocationPicker by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("nearby_screen")
  ) {
    // Top Location Header Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        // Location chip / label
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { showLocationPicker = true }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("location_header_chip")
        ) {
          if (state.isGpsActive) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(TransitLiveGreen)
            )
          } else {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (state.isGpsActive) "CURRENT LOCATION" else "SELECTED AREA",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              color = if (state.isGpsActive) TransitLiveGreen else MaterialTheme.colorScheme.primary
            )
            Text(
              text = state.locationLabel,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Icon(
            imageVector = Icons.Default.EditLocationAlt,
            contentDescription = "Change Location",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Re-center GPS button
        IconButton(
          onClick = onGpsRecenter,
          modifier = Modifier.testTag("gps_recenter_button")
        ) {
          Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = "Re-center to GPS Location",
            tint = if (state.isGpsActive) TransitLiveGreen else MaterialTheme.colorScheme.primary
          )
        }

        // Refresh button with cooldown state
        IconButton(
          onClick = { if (refreshCooldownSeconds == 0) onRefresh() },
          enabled = refreshCooldownSeconds == 0 && !state.isLoading,
          modifier = Modifier.testTag("refresh_button")
        ) {
          if (state.isLoading) {
            CircularProgressIndicator(
              strokeWidth = 2.dp,
              modifier = Modifier.size(20.dp)
            )
          } else if (refreshCooldownSeconds > 0) {
            Text(
              text = "${refreshCooldownSeconds}s",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh Departures"
            )
          }
        }
      }
    }

    // Rate-limit or Error Alert Banner
    if (state.error != null) {
      Surface(
        color = if (state.isRateLimited) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .testTag("rate_limit_error_banner")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Text(
            text = state.error,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (state.isRateLimited) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Transit Mode Filter Chips Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      TransitMode.values().forEach { mode ->
        val selected = mode == state.selectedMode
        FilterChip(
          selected = selected,
          onClick = { onModeSelected(mode) },
          label = {
            Text(
              text = mode.label,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
          ),
          modifier = Modifier.testTag("filter_chip_${mode.name}")
        )
      }
    }

    // Route-Centric Departures Feed
    if (state.nearbyRoutes.isEmpty() && !state.isLoading) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.DirectionsTransit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(52.dp)
          )
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "No transit routes found nearby",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Try switching transit modes or search another Boston area location.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = onGpsRecenter,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("empty_recenter_button")
            ) {
              Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Use GPS")
            }
            TextButton(
              onClick = { showLocationPicker = true },
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Search Place")
            }
          }
        }
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
        modifier = Modifier
          .fillMaxSize()
          .testTag("nearby_routes_list")
      ) {
        items(
          items = state.nearbyRoutes,
          key = { it.id }
        ) { routeItem ->
          NearbyRouteCard(
            item = routeItem,
            onTrackLive = onTrackLive
          )
        }
      }
    }
  }

  // Location Search & Hub Picker Modal
  if (showLocationPicker) {
    LocationSearchDialog(
      currentLabel = state.locationLabel,
      onDismiss = { showLocationPicker = false },
      onLocationChosen = { lat, lng, label ->
        showLocationPicker = false
        onCustomLocationSelected(lat, lng, label)
      },
      onUseGps = {
        showLocationPicker = false
        onGpsRecenter()
      }
    )
  }
}

@Composable
private fun LocationSearchDialog(
  currentLabel: String,
  onDismiss: () -> Unit,
  onLocationChosen: (lat: Double, lng: Double, label: String) -> Unit,
  onUseGps: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var searchQuery by remember { mutableStateOf("") }
  var searchResults by remember { mutableStateOf<List<GeocodedLocation>>(emptyList()) }
  var isSearching by remember { mutableStateOf(false) }

  LaunchedEffect(searchQuery) {
    if (searchQuery.isBlank()) {
      coroutineScope.launch {
        try {
          val dao = com.example.data.local.TransitDatabase.getDatabase(context).transitDao()
          val stations = dao.getAllCachedStations()
          val sampleStops = if (stations.isNotEmpty()) stations.take(8) else dao.getAllCachedStops().take(8)
          searchResults = sampleStops.map {
            val typeDesc = if (it.locationType == 1) "MBTA Station" else "MBTA Stop"
            GeocodedLocation(
              label = it.name,
              address = "$typeDesc • ${it.municipality}",
              latitude = it.latitude,
              longitude = it.longitude,
              isLandmark = true
            )
          }
        } catch (_: Exception) {
          searchResults = emptyList()
        }
        isSearching = false
      }
    } else {
      isSearching = true
      coroutineScope.launch {
        val results = GeocoderHelper.geocodeQuery(context, searchQuery)
        searchResults = results
        isSearching = false
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Choose Location",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(380.dp)
      ) {
        // Search Input
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search address, stop, or place...") },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("location_dialog_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick GPS Option Button
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onUseGps() }
            .testTag("location_dialog_gps_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Use Current GPS Location",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "Detects live device coordinates",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(4.dp))

        // Results / Suggested Landmarks List
        if (isSearching) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
          ) {
            items(searchResults) { loc ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onLocationChosen(loc.latitude, loc.longitude, loc.label)
                  }
                  .padding(vertical = 10.dp, horizontal = 4.dp)
                  .testTag("location_result_${loc.label.replace(" ", "_")}")
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = loc.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                  )
                  if (loc.address.isNotBlank()) {
                    Text(
                      text = loc.address,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
              HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
