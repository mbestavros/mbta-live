package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.service.ActiveTrackingState

@Composable
fun NowTrackingMiniCard(
  state: ActiveTrackingState,
  onCardClick: () -> Unit,
  onStopTracking: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = state.isActive,
    enter = slideInVertically(initialOffsetY = { it }),
    exit = slideOutVertically(targetOffsetY = { it }),
    modifier = modifier
  ) {
    val routeColor = parseColorFromHex(state.routeColorHex)

    Surface(
      shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onCardClick)
        .testTag("now_tracking_mini_card")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Progress bar reflecting lock screen progress
        LinearProgressIndicator(
          progress = { (state.progressPercentage / 100f).coerceIn(0f, 1f) },
          color = routeColor,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(routeColor),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.LockClock,
                contentDescription = "Lock Screen Live Update",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = state.routeName,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = routeColor
                )
                Text(
                  text = " • to ${state.destination}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Text(
                text = if (state.remainingMinutes > 0) {
                  "${state.status.label} • ${state.remainingMinutes} min remaining"
                } else {
                  "Arriving now"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onCardClick,
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Expand Trip Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            IconButton(
              onClick = onStopTracking,
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Stop Live Tracking",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
