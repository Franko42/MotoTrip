package com.mototrip.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mototrip.MainActivity
import com.mototrip.MotoTripApp
import com.mototrip.core.location.LocationFilter
import com.mototrip.core.location.RawLocationPoint
import com.mototrip.data.local.entity.TripEventEntity
import com.mototrip.data.local.entity.TripEventType
import com.mototrip.domain.model.TrackPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

class TripTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager

    private var activeTripId: String? = null
    private var isTrackingPaused: Boolean = false
    private var lastRecordedPoint: RawLocationPoint? = null
    private var accumulatedDistanceMeters: Long = 0L

    private val pointsBatch = mutableListOf<TrackPoint>()
    private var lastLocationReceivedTime: Long = 0L
    private var isTrackingInterrupted: Boolean = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            val currentTime = System.currentTimeMillis()

            // Check if we recovered from interruption
            if (isTrackingInterrupted) {
                isTrackingInterrupted = false
                recordInterruptionEvent(TripEventType.TRACKING_RECOVERED, location.latitude, location.longitude)
            }
            lastLocationReceivedTime = currentTime

            val rawPoint = RawLocationPoint(
                timestamp = location.time.takeIf { it > 0 } ?: currentTime,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                speedMps = if (location.hasSpeed()) location.speed else null,
                altitudeMeters = if (location.hasAltitude()) location.altitude else null
            )

            val (shouldRecord, deltaDistance) = LocationFilter.evaluateConsecutivePoint(lastRecordedPoint, rawPoint)
            if (shouldRecord) {
                lastRecordedPoint = rawPoint
                accumulatedDistanceMeters += deltaDistance.toLong()

                val tripId = activeTripId ?: return
                val trackPoint = TrackPoint(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    timestamp = rawPoint.timestamp,
                    latitude = rawPoint.latitude,
                    longitude = rawPoint.longitude,
                    accuracyMeters = rawPoint.accuracyMeters,
                    speedMps = rawPoint.speedMps,
                    altitudeMeters = rawPoint.altitudeMeters
                )

                pointsBatch.add(trackPoint)

                // Flush batch if size reaches threshold
                if (pointsBatch.size >= BATCH_SIZE_THRESHOLD) {
                    flushPointsBatch()
                }

                updateNotification()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val tripId = intent.getStringExtra(EXTRA_TRIP_ID)

        when (action) {
            ACTION_START -> {
                if (tripId != null) {
                    activeTripId = tripId
                    isTrackingPaused = false
                    startForegroundServiceInternal()
                    startLocationUpdates()
                }
            }
            ACTION_PAUSE -> {
                isTrackingPaused = true
                stopLocationUpdates()
                flushPointsBatch()
                updateNotification()
            }
            ACTION_RESUME -> {
                isTrackingPaused = false
                startLocationUpdates()
                updateNotification()
            }
            ACTION_STOP -> {
                stopTrackingInternal()
            }
        }

        return START_STICKY
    }

    private fun startForegroundServiceInternal() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            stopSelf()
            return
        }

        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startLocationUpdates() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(2.0f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            lastLocationReceivedTime = System.currentTimeMillis()
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun stopTrackingInternal() {
        stopLocationUpdates()
        flushPointsBatch()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun flushPointsBatch() {
        val toSave = ArrayList(pointsBatch)
        pointsBatch.clear()

        val tripId = activeTripId ?: return
        val app = application as? MotoTripApp ?: return
        val currentDistance = accumulatedDistanceMeters

        serviceScope.launch {
            if (toSave.isNotEmpty()) {
                app.appContainer.tripRepository.recordTrackPoints(toSave)
            }
            // Update distance in trip
            val trip = app.appContainer.database.tripDao().getTripById(tripId)
            if (trip != null) {
                app.appContainer.database.tripDao().updateTrip(
                    trip.copy(
                        distanceMeters = maxOf(trip.distanceMeters, currentDistance),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun recordInterruptionEvent(eventType: TripEventType, lat: Double?, lon: Double?) {
        val tripId = activeTripId ?: return
        val app = application as? MotoTripApp ?: return

        serviceScope.launch {
            val event = TripEventEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                timestamp = System.currentTimeMillis(),
                eventType = eventType,
                latitude = lat,
                longitude = lon,
                details = if (eventType == TripEventType.TRACKING_INTERRUPTED) "Pérdida temporal de señal GPS" else "Señal GPS recuperada"
            )
            app.appContainer.database.tripEventDao().insertTripEvent(event)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val distanceKm = accumulatedDistanceMeters / 1000.0
        val title = if (isTrackingPaused) "MotoTrip - Viaje en pausa" else "MotoTrip - Registrando viaje"
        val text = String.format(java.util.Locale.getDefault(), "Distancia acumulada: %.1f km", distanceKm)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(contentIntent)
            .setOngoing(!isTrackingPaused)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Seguimiento GPS de MotoTrip",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente requerida durante el registro de viajes en moto"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        flushPointsBatch()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "mototrip_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.mototrip.action.START_TRACKING"
        const val ACTION_PAUSE = "com.mototrip.action.PAUSE_TRACKING"
        const val ACTION_RESUME = "com.mototrip.action.RESUME_TRACKING"
        const val ACTION_STOP = "com.mototrip.action.STOP_TRACKING"
        const val EXTRA_TRIP_ID = "extra_trip_id"

        const val BATCH_SIZE_THRESHOLD = 5

        fun startTracking(context: Context, tripId: String) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TRIP_ID, tripId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseTracking(context: Context, tripId: String) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_TRIP_ID, tripId)
            }
            context.startService(intent)
        }

        fun resumeTracking(context: Context, tripId: String) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_TRIP_ID, tripId)
            }
            context.startService(intent)
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
