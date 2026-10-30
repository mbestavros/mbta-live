package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.GeocodedLocation
import com.example.data.model.Itinerary
import com.example.data.model.ItineraryLeg
import com.example.data.model.LegType
import com.example.ui.components.TransitLineBadge
import com.example.ui.components.parseColorFromHex
import com.example.ui.viewmodel.TripPlannerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlannerSheet(
  state: TripPlannerUiState,
  currentOriginName: String,
  onQueryChange: (String) -> Unit,
  onDestinationSelected: (GeocodedLocation) -> Unit,
  onDirectSearchSubmit: (String) -> Unit,
  onStartNewSearch: () -> Unit,
  onStartTrackingItinerary: (Itinerary) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("trip_planner_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text(
            text = "Plan Trip & Routing",
            style = MaterialTheme.typography.titleLarge
          )
          Text(
            text = "From: $currentOriginName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // If viewing an itinerary, show Destination summary card with "Edit / Change" button
      if (!state.isSearching && state.plannedItinerary != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = state.selectedDestination?.label ?: state.searchQuery,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = state.selectedDestination?.address ?: "Greater Boston",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            OutlinedButton(
              onClick = onStartNewSearch,
              modifier = Modifier.testTag("change_destination_button")
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Change", fontSize = 12.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      } else {
        // Search Input Bar
        OutlinedTextField(
          value = state.searchQuery,
          onValueChange = onQueryChange,
          placeholder = { Text("Enter address, street, or destination...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (state.searchQuery.isNotEmpty()) {
              IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "Clear search")
              }
            }
          },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(
            onSearch = { onDirectSearchSubmit(state.searchQuery) }
          ),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("address_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Dynamic Suggestion Chips
        if (state.searchSuggestions.isNotEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            state.searchSuggestions.take(6).forEach { loc ->
              SuggestionChip(
                onClick = { onDestinationSelected(loc) },
                label = { Text(loc.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = {
                  Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                  )
                }
              )
            }
          }
        }

        if (state.errorMessage != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("search_error_banner")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
      }

      // Main content: Loading, Search Results, or Planned Itinerary
      if (state.isCalculating) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Finding optimal transit & transfer route...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else if (!state.isSearching && state.plannedItinerary != null) {
        val itin = state.plannedItinerary
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            // Itinerary summary row
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Text(
                  text = "${itin.totalDurationMinutes} MIN TOTAL",
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Black
                )
                Text(
                  text = "${itin.walkTimeMinutes} min walk • ${itin.transferCount} transfers",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              Button(
                onClick = { onStartTrackingItinerary(itin) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("start_journey_button")
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Journey", fontWeight = FontWeight.Bold)
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-leg sequence
            itin.legs.forEachIndexed { index, leg ->
              ItineraryLegRow(leg = leg, isLast = index == itin.legs.lastIndex)
            }
          }
        }
      } else {
        // Search suggestions list
        Text(
          text = if (state.searchQuery.isBlank()) "POPULAR DESTINATIONS" else "SEARCH RESULTS & CORRIDORS",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
        ) {
          // Direct search button for typed text
          if (state.searchQuery.isNotBlank()) {
            item(key = "direct_search_item") {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 8.dp)
                  .clickable { onDirectSearchSubmit(state.searchQuery) }
                  .testTag("direct_search_submit_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.DirectionsTransit,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = "Route to: \"${state.searchQuery.trim()}\"",
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                      text = "Tap to route to this address",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                  }
                }
              }
            }
          }

          items(state.searchSuggestions) { dest ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onDestinationSelected(dest) }
                .padding(vertical = 12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = dest.label,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = dest.address,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ItineraryLegRow(leg: ItineraryLeg, isLast: Boolean) {
  Row(modifier = Modifier.fillMaxWidth()) {
    // Indicator column
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(32.dp)
    ) {
      val iconBg = when (leg.legType) {
        LegType.WALK -> MaterialTheme.colorScheme.surfaceVariant
        LegType.TRANSFER -> Color(0xFFED8B00)
        LegType.TRANSIT -> parseColorFromHex(leg.routeColorHex ?: "00843D")
      }

      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(iconBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = when (leg.legType) {
            LegType.WALK -> Icons.Default.DirectionsWalk
            LegType.TRANSFER -> Icons.Default.SwapVert
            LegType.TRANSIT -> Icons.Default.ArrowForward
          },
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(14.dp)
        )
      }

      if (!isLast) {
        Box(
          modifier = Modifier
            .width(2.dp)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        )
      }
    }

    Spacer(modifier = Modifier.width(10.dp))

    // Content
    Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (leg.legType == LegType.TRANSIT && leg.routeName != null) {
          TransitLineBadge(
            name = leg.routeName,
            colorHex = leg.routeColorHex ?: "00843D"
          )
          Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
          text = "${leg.durationMinutes} min",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }
      Text(
        text = leg.instructions,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}
