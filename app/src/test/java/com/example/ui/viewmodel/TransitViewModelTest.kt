package com.example.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TransitMode
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransitViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var application: Application
  private lateinit var viewModel: TransitViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    application = ApplicationProvider.getApplicationContext()
    viewModel = TransitViewModel(application)
    viewModel.stopAutoRefresh()
  }

  @After
  fun tearDown() {
    viewModel.stopAutoRefresh()
    Dispatchers.resetMain()
  }

  @Test
  fun testInitialState() {
    val state = viewModel.nearbyState.value
    assertNotNull(state)
    assertEquals(TransitMode.ALL, state.selectedMode)
    assertFalse(state.isRateLimited)

    val tripState = viewModel.tripPlannerState.value
    assertFalse(tripState.isSheetOpen)

    val theme = viewModel.themeMode.value
    assertEquals(AppThemeMode.SYSTEM, theme)
  }

  @Test
  fun testSetTransitMode() {
    viewModel.setTransitMode(TransitMode.SUBWAY)
    assertEquals(TransitMode.SUBWAY, viewModel.nearbyState.value.selectedMode)

    viewModel.setTransitMode(TransitMode.BUS)
    assertEquals(TransitMode.BUS, viewModel.nearbyState.value.selectedMode)
  }

  @Test
  fun testSetThemeMode() {
    viewModel.setThemeMode(AppThemeMode.DARK)
    assertEquals(AppThemeMode.DARK, viewModel.themeMode.value)

    viewModel.setThemeMode(AppThemeMode.LIGHT)
    assertEquals(AppThemeMode.LIGHT, viewModel.themeMode.value)
  }

  @Test
  fun testSetCustomLocation() {
    viewModel.setCustomLocation(42.3601, -71.0589, "Government Center")
    assertEquals("Government Center", viewModel.nearbyState.value.locationLabel)
    assertFalse(viewModel.nearbyState.value.isGpsActive)
  }

  @Test
  fun testManualRefreshCooldown() = runTest(testDispatcher) {
    assertEquals(0, viewModel.refreshCooldownSeconds.value)

    viewModel.manualRefreshDepartures()
    assertEquals(5, viewModel.refreshCooldownSeconds.value)

    // Second immediate manual refresh should be ignored while on cooldown
    viewModel.manualRefreshDepartures()
    assertEquals(5, viewModel.refreshCooldownSeconds.value)
  }

  @Test
  fun testTripPlannerSheetOpenClose() {
    viewModel.openTripPlanner()
    assertTrue(viewModel.tripPlannerState.value.isSheetOpen)

    viewModel.closeTripPlanner()
    assertFalse(viewModel.tripPlannerState.value.isSheetOpen)
  }

  @Test
  fun testRapidConsecutiveRefreshesDoNotEmitCancellationError() = runTest(testDispatcher) {
    // Fire multiple rapid departure refreshes as happens during startup
    viewModel.refreshNearbyDepartures()
    viewModel.refreshNearbyDepartures()
    viewModel.refreshNearbyDepartures()

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.nearbyState.value
    // Assert error is not set to any coroutine cancellation error
    if (state.error != null) {
      assertFalse(state.error!!.contains("StandaloneCoroutine was cancelled", ignoreCase = true))
      assertFalse(state.error!!.contains("Job was cancelled", ignoreCase = true))
    }
  }
}
