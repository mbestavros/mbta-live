package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.TripStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ActiveTrackingState(
  val isActive: Boolean = false,
  val routeName: String = "",
  val routeColorHex: String = "00843D",
  val destination: String = "",
  val originStop: String = "",
  val currentStop: String = "",
  val remainingMinutes: Int = 0,
  val progressPercentage: Int = 0,
  val status: TripStatus = TripStatus.WAITING,
  val isTunnelOffline: Boolean = false
)

class LiveTrackingService : Service() {

  companion object {
    // New promoted channel ID with HIGH importance for status bar chips and lock screen promotion
    const val CHANNEL_ID = "mbta_live_promoted_v3"
    const val NOTIFICATION_ID = 1001

    const val ACTION_START_TRACKING = "com.example.service.START_TRACKING"
    const val ACTION_STOP_TRACKING = "com.example.service.STOP_TRACKING"

    const val EXTRA_ROUTE_NAME = "extra_route_name"
    const val EXTRA_ROUTE_COLOR = "extra_route_color"
    const val EXTRA_DESTINATION = "extra_destination"
    const val EXTRA_ORIGIN_STOP = "extra_origin_stop"
    const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
    const val EXTRA_TARGET_EPOCH_MS = "extra_target_epoch_ms"

    private val _trackingState = MutableStateFlow(ActiveTrackingState())
    val trackingState = _trackingState.asStateFlow()

    fun startTracking(
      context: Context,
      routeName: String,
      routeColorHex: String,
      destination: String,
      originStop: String,
      durationMinutes: Int
    ) {
      val intent = Intent(context, LiveTrackingService::class.java).apply {
        action = ACTION_START_TRACKING
        putExtra(EXTRA_ROUTE_NAME, routeName)
        putExtra(EXTRA_ROUTE_COLOR, routeColorHex)
        putExtra(EXTRA_DESTINATION, destination)
        putExtra(EXTRA_ORIGIN_STOP, originStop)
        putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
        putExtra(EXTRA_TARGET_EPOCH_MS, System.currentTimeMillis() + durationMinutes * 60 * 1000L)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stopTracking(context: Context) {
      val intent = Intent(context, LiveTrackingService::class.java).apply {
        action = ACTION_STOP_TRACKING
      }
      context.startService(intent)
    }
  }

  private val serviceJob = Job()
  private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
  private var tickerJob: Job? = null

  private var routeName = "Red Line"
  private var routeColorHex = "DA291C"
  private var destination = "Ashmont/Braintree"
  private var originStop = "Park Street"
  private var totalDurationMinutes = 8
  private var targetEpochMs = System.currentTimeMillis() + 8 * 60 * 1000L

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START_TRACKING -> {
        routeName = intent.getStringExtra(EXTRA_ROUTE_NAME) ?: "MBTA Transit"
        routeColorHex = intent.getStringExtra(EXTRA_ROUTE_COLOR) ?: "00843D"
        destination = intent.getStringExtra(EXTRA_DESTINATION) ?: "Inbound"
        originStop = intent.getStringExtra(EXTRA_ORIGIN_STOP) ?: "Station"
        totalDurationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 8).coerceAtLeast(1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          startForeground(
            NOTIFICATION_ID,
            buildLiveNotification(),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
          )
        } else {
          startForeground(NOTIFICATION_ID, buildLiveNotification())
        }
        startDeadReckoningLoop()
      }
      ACTION_STOP_TRACKING -> {
        stopTrackingSession()
      }
    }
    return START_NOT_STICKY
  }

  private fun startDeadReckoningLoop() {
    tickerJob?.cancel()
    tickerJob = serviceScope.launch {
      while (isActive) {
        val now = System.currentTimeMillis()
        val remainingMs = targetEpochMs - now
        val remainingMins = (remainingMs / (60 * 1000L)).toInt().coerceAtLeast(0)

        val totalMs = totalDurationMinutes * 60 * 1000L
        val elapsedMs = (totalMs - remainingMs).coerceAtLeast(0)
        val progress = if (totalMs > 0) ((elapsedMs.toDouble() / totalMs) * 100).toInt().coerceIn(0, 100) else 100

        val currentStatus = when {
          progress >= 100 -> TripStatus.ARRIVED
          progress > 20 -> TripStatus.ON_BOARD
          else -> TripStatus.WAITING
        }

        _trackingState.value = ActiveTrackingState(
          isActive = true,
          routeName = routeName,
          routeColorHex = routeColorHex,
          destination = destination,
          originStop = originStop,
          currentStop = if (progress > 50) destination else originStop,
          remainingMinutes = remainingMins,
          progressPercentage = progress,
          status = currentStatus,
          isTunnelOffline = false
        )

        updateNotification()

        if (progress >= 100 && remainingMins <= 0) {
          delay(15000) // Keep "Arrived" state on lock screen for 15s then dismiss
          stopTrackingSession()
          break
        }

        delay(1000) // 1s local ticker for smooth countdown & progress bar
      }
    }
  }

  private fun updateNotification() {
    val notification = buildLiveNotification()
    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  private fun buildLiveNotification(): Notification {
    val state = _trackingState.value
    val title = "$routeName to ${state.destination}"
    val body = if (state.remainingMinutes > 0) {
      "${state.status.label} • Arriving in ${state.remainingMinutes} min"
    } else {
      "Arriving now at ${state.destination}"
    }

    val chipText = if (state.remainingMinutes > 0) "${state.remainingMinutes}m" else "NOW"

    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openPendingIntent = PendingIntent.getActivity(
      this, 0, openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = Intent(this, LiveTrackingService::class.java).apply {
      action = ACTION_STOP_TRACKING
    }
    val stopPendingIntent = PendingIntent.getService(
      this, 1, stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val routeColorInt = parseColorHex(state.routeColorHex)
    val routeBadgeBitmap = createRouteBadgeBitmap(state.routeName, routeColorInt)

    // Android 16 (API 36+) native Promoted Ongoing Live Update
    if (Build.VERSION.SDK_INT >= 36) {
      try {
        val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
        val progressStyle = progressStyleClass.getConstructor().newInstance()

        // Set total progress units
        progressStyleClass.getMethod("setProgress", Int::class.javaPrimitiveType)
          .invoke(progressStyle, 100)

        val builder = Notification.Builder(this, CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_transit_notification)
          .setContentTitle(title)
          .setContentText(body)
          .setSubText(chipText)
          .setOngoing(true)
          .setColor(routeColorInt)
          .setVisibility(Notification.VISIBILITY_PUBLIC)
          .setCategory(Notification.CATEGORY_NAVIGATION)
          .setContentIntent(openPendingIntent)
          .setLargeIcon(routeBadgeBitmap)
          .setOnlyAlertOnce(true)
          .addAction(
            Notification.Action.Builder(null, "Stop Tracking", stopPendingIntent).build()
          )

        // Request Promoted Ongoing (creates the status bar chip & locks expanded card in shade/lockscreen)
        try {
          Notification.Builder::class.java.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
            .invoke(builder, true)
        } catch (_: Exception) {
          builder.extras.putBoolean("android.requestPromotedOngoing", true)
        }

        // Set ProgressStyle
        Notification.Builder::class.java.getMethod("setStyle", Class.forName("android.app.Notification\$Style"))
          .invoke(builder, progressStyle)

        return builder.build()
      } catch (_: Exception) {
        // Fall back to NotificationCompat
      }
    }

    // High-priority persistent notification for Android 15 and earlier
    // CATEGORY_NAVIGATION + HIGH importance promotes this to top of shade, status bar chip, and lock screen card
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_transit_notification)
      .setContentTitle(title)
      .setContentText(body)
      .setSubText(chipText)
      .setColor(routeColorInt)
      .setProgress(100, state.progressPercentage, false)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
      .setContentIntent(openPendingIntent)
      .setLargeIcon(routeBadgeBitmap)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .setBigContentTitle(title)
          .bigText("$body\nFrom: ${state.originStop} • To: ${state.destination}")
          .setSummaryText(chipText)
      )
      .addAction(0, "Stop Tracking", stopPendingIntent)
      .addAction(0, "View Route", openPendingIntent)
      .build()
  }

  private fun stopTrackingSession() {
    tickerJob?.cancel()
    _trackingState.value = ActiveTrackingState(isActive = false)
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

      // Clean up any old low-priority channels
      try {
        notificationManager.deleteNotificationChannel("mbta_live_tracking_channel")
      } catch (_: Exception) {}

      val channel = NotificationChannel(
        CHANNEL_ID,
        "Live Transit & Navigation",
        NotificationManager.IMPORTANCE_HIGH // HIGH importance guarantees status bar chip, top shade placement, and lock screen promotion
      ).apply {
        description = "Shows glanceable live transit countdowns on status bar and lock screen"
        setShowBadge(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        enableVibration(false)
        setSound(null, null)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun parseColorHex(hex: String): Int {
    return try {
      val clean = hex.removePrefix("#")
      AndroidColor.parseColor("#$clean")
    } catch (_: Exception) {
      AndroidColor.parseColor("#00843D")
    }
  }

  private fun createRouteBadgeBitmap(name: String, color: Int): Bitmap {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.color = color
      style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.color = AndroidColor.WHITE
      textSize = 34f
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
    }
    val label = name.take(3).uppercase()
    val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(label, size / 2f, yPos, textPaint)
    return bitmap
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceJob.cancel()
  }
}
