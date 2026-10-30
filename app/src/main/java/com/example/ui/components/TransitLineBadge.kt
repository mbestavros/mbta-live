package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TransitLiveGreen

fun parseColorFromHex(hex: String, fallback: Color = Color(0xFF00843D)): Color {
  return try {
    val clean = hex.removePrefix("#")
    Color(android.graphics.Color.parseColor("#$clean"))
  } catch (_: Exception) {
    fallback
  }
}

@Composable
fun TransitLineBadge(
  name: String,
  colorHex: String,
  textColorHex: String = "FFFFFF",
  modifier: Modifier = Modifier
) {
  val bgColor = parseColorFromHex(colorHex)
  val textColor = parseColorFromHex(textColorHex, Color.White)

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(bgColor)
      .padding(horizontal = 10.dp, vertical = 5.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = name,
      color = textColor,
      fontSize = 13.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = 0.5.sp
    )
  }
}

@Composable
fun LiveBeaconIndicator(
  modifier: Modifier = Modifier,
  isLive: Boolean = true
) {
  if (!isLive) return

  val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
  val alpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
  )

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .alpha(alpha)
        .clip(CircleShape)
        .background(TransitLiveGreen)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = "LIVE",
      color = TransitLiveGreen,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.5.sp
    )
  }
}
