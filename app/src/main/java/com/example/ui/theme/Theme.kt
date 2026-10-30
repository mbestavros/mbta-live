package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode {
  SYSTEM,
  LIGHT,
  DARK
}

private val DarkColorScheme =
  darkColorScheme(
    primary = AccentTeal,
    onPrimary = Color.Black,
    primaryContainer = AccentTealDark,
    onPrimaryContainer = Color.White,
    secondary = MbtaOrange,
    onSecondary = Color.Black,
    background = TransitDarkBg,
    onBackground = Color(0xFFF0F6FC),
    surface = TransitDarkSurface,
    onSurface = Color(0xFFF0F6FC),
    surfaceVariant = TransitDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC9D1D9),
    outline = TransitDarkBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AccentTealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCAF0F8),
    onPrimaryContainer = Color(0xFF03045E),
    secondary = MbtaOrange,
    onSecondary = Color.White,
    background = TransitLightBg,
    onBackground = Color(0xFF1F2328),
    surface = TransitLightSurface,
    onSurface = Color(0xFF1F2328),
    surfaceVariant = TransitLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF656D76),
    outline = TransitLightBorder
  )

@Composable
fun MbtaLiveTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.DARK -> true
    AppThemeMode.LIGHT -> false
  }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      isDark -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
