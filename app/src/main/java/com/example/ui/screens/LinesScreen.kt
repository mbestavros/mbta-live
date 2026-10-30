package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MbtaRoute
import com.example.ui.components.TransitLineBadge
import com.example.ui.viewmodel.LinesUiState

@Composable
fun LinesScreen(
  state: LinesUiState,
  onRouteClick: (MbtaRoute) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    modifier = modifier
      .fillMaxSize()
      .testTag("lines_screen")
  ) {
    // Service Alerts section
    if (state.alerts.isNotEmpty()) {
      item(key = "alerts_header") {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFFFFF3CD),
          contentColor = Color(0xFF664D03),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = Color(0xFF997404),
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "${state.alerts.size} Active MBTA Service Alerts",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
              Text(
                text = state.alerts.firstOrNull()?.header ?: "Check line schedules for details.",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }

    item(key = "subway_header") {
      Text(
        text = "RAPID TRANSIT & SUBWAY",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
      )
    }

    val subwayRoutes = state.routes.filter { it.type == 0 || it.type == 1 }
    items(subwayRoutes, key = { it.id }) { route ->
      LineItemCard(route = route, onClick = { onRouteClick(route) })
    }

    item(key = "bus_header") {
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "KEY BUS ROUTES & SILVER LINE",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
      )
    }

    val busRoutes = state.routes.filter { it.type == 3 }
    items(busRoutes.take(12), key = { it.id }) { route ->
      LineItemCard(route = route, onClick = { onRouteClick(route) })
    }

    item(key = "commuter_header") {
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "COMMUTER RAIL & FERRY",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
      )
    }

    val railAndFerry = state.routes.filter { it.type == 2 || it.type == 4 }
    items(railAndFerry.take(8), key = { it.id }) { route ->
      LineItemCard(route = route, onClick = { onRouteClick(route) })
    }

    item(key = "bottom_space") {
      Spacer(modifier = Modifier.height(80.dp))
    }
  }
}

@Composable
fun LineItemCard(
  route: MbtaRoute,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clickable(onClick = onClick)
      .testTag("line_item_${route.id}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        TransitLineBadge(
          name = route.displayName,
          colorHex = route.colorHex,
          textColorHex = route.textColorHex
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = route.longName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = route.description.ifBlank { "Regular Service" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
      )
    }
  }
}
