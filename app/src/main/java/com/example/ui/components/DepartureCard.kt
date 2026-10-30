package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.model.RouteDepartures

@Composable
fun DepartureCard(
  routeDeparture: RouteDepartures,
  onTrackLiveClick: (countdownMinutes: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val route = routeDeparture.route
  val predictions = routeDeparture.predictions
  val firstPred = predictions.firstOrNull()
  val nextMins = firstPred?.countdownMinutes ?: 1

  val routeColor = parseColorFromHex(route.colorHex)

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .testTag("departure_card_${route.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Top row: Line Badge, Headsign, Live Beacon
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
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
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = routeDeparture.headsign,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = route.description.ifBlank { "MBTA Service" },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        LiveBeaconIndicator(isLive = routeDeparture.isLive)
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Bottom row: Countdown Display & Track Live Action
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Departures breakdown
        Row(verticalAlignment = Alignment.Bottom) {
          val countdownLabel = if (nextMins <= 0) "ARRIVING" else "$nextMins MIN"
          Text(
            text = countdownLabel,
            fontSize = if (nextMins <= 0) 18.sp else 22.sp,
            fontWeight = FontWeight.Black,
            color = if (nextMins <= 1) Color(0xFFE63946) else MaterialTheme.colorScheme.onSurface
          )

          // Subsequent departures
          if (predictions.size > 1) {
            Spacer(modifier = Modifier.width(10.dp))
            predictions.drop(1).take(2).forEach { p ->
              Box(
                modifier = Modifier
                  .padding(end = 6.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(horizontal = 8.dp, vertical = 3.dp)
              ) {
                Text(
                  text = "${p.countdownMinutes}m",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        // Track Live Button (launches Android 16 Live Lock Screen notification)
        Button(
          onClick = { onTrackLiveClick(nextMins) },
          colors = ButtonDefaults.buttonColors(
            containerColor = routeColor,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .height(40.dp)
            .testTag("track_live_button_${route.id}")
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = "Track Live on Lock Screen",
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Track Live",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
