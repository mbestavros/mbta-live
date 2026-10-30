package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MbtaLiveTheme
import com.example.ui.viewmodel.TransitViewModel

class MainActivity : ComponentActivity() {

  private val transitViewModel: TransitViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val themeMode by transitViewModel.themeMode.collectAsState()

      MbtaLiveTheme(themeMode = themeMode, dynamicColor = true) {
        MainScreen(
          viewModel = transitViewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
