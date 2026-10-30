package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.model.TransitMode
import com.example.ui.screens.NearbyScreen
import com.example.ui.theme.MbtaLiveTheme
import com.example.ui.viewmodel.NearbyUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ComposeUiRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun testNearbyScreenHeaderAndRefreshButtonRender() {
    val state = NearbyUiState(
      locationLabel = "Boston Common / Downtown",
      isGpsActive = true
    )

    composeTestRule.setContent {
      MbtaLiveTheme {
        NearbyScreen(
          state = state,
          refreshCooldownSeconds = 0,
          onModeSelected = {},
          onRefresh = {},
          onGpsRecenter = {},
          onCustomLocationSelected = { _, _, _ -> },
          onTrackLive = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nearby_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("location_header_chip").assertIsDisplayed()
    composeTestRule.onNodeWithTag("refresh_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("gps_recenter_button").assertIsDisplayed()
    composeTestRule.onNodeWithText("Boston Common / Downtown").assertIsDisplayed()
  }

  @Test
  fun testNearbyScreenRateLimitErrorBannerDisplayed() {
    val state = NearbyUiState(
      error = "MBTA API rate limit reached: Using offline cached stops",
      isRateLimited = true
    )

    composeTestRule.setContent {
      MbtaLiveTheme {
        NearbyScreen(
          state = state,
          refreshCooldownSeconds = 4,
          onModeSelected = {},
          onRefresh = {},
          onGpsRecenter = {},
          onCustomLocationSelected = { _, _, _ -> },
          onTrackLive = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("rate_limit_error_banner").assertIsDisplayed()
    composeTestRule.onNodeWithText("MBTA API rate limit reached: Using offline cached stops").assertIsDisplayed()
    // Cooldown text on the button
    composeTestRule.onNodeWithText("4s").assertIsDisplayed()
  }

  @Test
  fun testTransitModeFilterChipsRender() {
    val state = NearbyUiState(selectedMode = TransitMode.SUBWAY)

    composeTestRule.setContent {
      MbtaLiveTheme {
        NearbyScreen(
          state = state,
          refreshCooldownSeconds = 0,
          onModeSelected = {},
          onRefresh = {},
          onGpsRecenter = {},
          onCustomLocationSelected = { _, _, _ -> },
          onTrackLive = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("filter_chip_ALL").assertIsDisplayed()
    composeTestRule.onNodeWithTag("filter_chip_SUBWAY").assertIsDisplayed()
    composeTestRule.onNodeWithTag("filter_chip_BUS").assertIsDisplayed()
  }
}
