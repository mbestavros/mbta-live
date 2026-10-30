package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FavoriteStopEntity
import com.example.data.model.MbtaStop
import com.example.ui.components.TransitLineBadge

@Composable
fun FavoritesScreen(
  favorites: List<FavoriteStopEntity>,
  onRemoveFavorite: (String) -> Unit,
  onSelectStation: (MbtaStop) -> Unit,
  modifier: Modifier = Modifier
) {
  if (favorites.isEmpty()) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = modifier
        .fillMaxSize()
        .padding(32.dp)
        .testTag("favorites_empty_screen")
    ) {
      Icon(
        imageVector = Icons.Default.StarOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(64.dp)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "No Starred Stations Yet",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Star any station or stop from the Nearby feed or Trip Planner for quick access to live departures and one-tap trip planning.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  } else {
    LazyColumn(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      modifier = modifier
        .fillMaxSize()
        .testTag("favorites_list_screen")
    ) {
      item(key = "fav_title") {
        Text(
          text = "STARRED STATIONS",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          letterSpacing = 1.sp,
          modifier = Modifier.padding(vertical = 8.dp)
        )
      }

      items(favorites, key = { it.stopId }) { fav ->
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable {
              val stop = MbtaStop(
                id = fav.stopId,
                name = fav.stopName,
                latitude = fav.latitude,
                longitude = fav.longitude
              )
              onSelectStation(stop)
            }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              TransitLineBadge(
                name = fav.primaryLineName,
                colorHex = fav.primaryLineColorHex
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = fav.stopName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Live Departures & Alerts Available",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { onRemoveFavorite(fav.stopId) },
                modifier = Modifier.testTag("remove_fav_${fav.stopId}")
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Remove favorite",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
