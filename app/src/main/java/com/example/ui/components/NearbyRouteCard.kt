package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.DirectionDepartures
import com.example.data.model.NearbyRouteItem
import com.example.data.model.TransitMode
import com.example.ui.theme.TransitLiveGreen

@Composable
fun NearbyRouteCard(
  item: NearbyRouteItem,
  onTrackLive: (routeName: String, routeColorHex: String, destination: String, originStop: String, minutes: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val route = item.route
  val routeColor = parseColorFromHex(route.colorHex)
  val textColor = parseColorFromHex(route.textColorHex, Color.White)

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 7.dp)
      .testTag("nearby_route_card_${route.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Route Header: Color Badge + Mode Icon + Walking Distance Subtitle
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Route pill/badge with high contrast
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(routeColor)
              .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              val modeIcon = when (route.transitMode) {
                TransitMode.SUBWAY -> Icons.Default.DirectionsSubway
                TransitMode.BUS -> Icons.Default.DirectionsBus
                TransitMode.COMMUTER_RAIL -> Icons.Default.DirectionsRailway
                TransitMode.FERRY -> Icons.Default.DirectionsTransit
                else -> Icons.Default.DirectionsTransit
              }
              Icon(
                imageVector = modeIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = route.shortName.ifBlank { route.longName },
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
              )
            }
          }

          if (route.shortName.isNotBlank() && route.longName.isNotBlank() && route.shortName != route.longName) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = route.longName,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        // Walking distance pill
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.padding(start = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${item.walkMinutes} min (${String.format("%.1f", item.distanceMiles)} mi)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Boarding station hint with pin icon
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
      ) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Board at ${item.nearestStop.name}",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.8.dp
      )

      // Stacked Directions: Direction 0 and Direction 1
      item.directions.forEachIndexed { index, dir ->
        if (index > 0) {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 4.dp)
          )
        }
        DirectionRow(
          direction = dir,
          routeColor = routeColor,
          onTrackClick = { minutes ->
            onTrackLive(
              route.shortName.ifBlank { route.longName },
              route.colorHex,
              dir.headsign,
              item.nearestStop.name,
              minutes
            )
          }
        )
      }
    }
  }
}

@Composable
private fun DirectionRow(
  direction: DirectionDepartures,
  routeColor: Color,
  onTrackClick: (minutes: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val firstPred = direction.predictions.firstOrNull()
  val nextMinutes = firstPred?.countdownMinutes ?: 2
  val subsequentPreds = direction.predictions
    .drop(1)
    .filter { it.countdownMinutes > nextMinutes }
    .distinctBy { it.countdownMinutes }
    .take(2)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
      .testTag("direction_row_${direction.directionId}")
  ) {
    // 1. Destination Headsign + Live Beacon
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      if (direction.isLive) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(TransitLiveGreen)
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = direction.headsign,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 2. Chronological Departure Timeline (Left to Right: Next Arrival FIRST, then upcoming, then Track button)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      // Left Group: Next Arrival Chip -> Upcoming "Then" Badges
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.weight(1f, fill = false)
      ) {
        // Primary Next Departure Chip (Highlighted)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (nextMinutes <= 3) {
            TransitLiveGreen.copy(alpha = 0.15f)
          } else {
            routeColor.copy(alpha = 0.14f)
          },
          border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (nextMinutes <= 3) TransitLiveGreen else routeColor.copy(alpha = 0.5f)
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
          ) {
            if (direction.isLive) {
              Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = "Live GPS",
                tint = if (nextMinutes <= 3) TransitLiveGreen else routeColor,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
              text = if (nextMinutes <= 0) "Arriving" else "$nextMinutes min",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.sp,
              color = if (nextMinutes <= 3) TransitLiveGreen else routeColor
            )
          }
        }

        // Subsequent Departures in Chronological Order ("Then Xm", "Then Ym")
        if (subsequentPreds.isNotEmpty()) {
          subsequentPreds.forEach { p ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
              Text(
                text = "Then ${p.countdownMinutes}m",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Right: Track Live Button (minimum 48dp touch target)
      OutlinedButton(
        onClick = { onTrackClick(nextMinutes) },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 6.dp),
        modifier = Modifier
          .height(34.dp)
          .testTag("track_button_${direction.directionId}")
      ) {
        Icon(
          imageVector = Icons.Default.NotificationsActive,
          contentDescription = "Track Live Notification",
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Track",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
